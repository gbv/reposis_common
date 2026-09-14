/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.gbv.reposis.tools;

import java.util.concurrent.locks.ReentrantLock;

import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.servlet.http.HttpServletRequest;

/** Read-only overview for metadata editors. */
public class MCRCircleRepairServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;
    private static final String RESULT = MCRCircleRepairServlet.class.getName() + ".result";
    private static final String LOCKED = MCRCircleRepairServlet.class.getName() + ".locked";
    private static final ReentrantLock SCAN_LOCK = new ReentrantLock();

    @Override
    protected void think(MCRServletJob job) throws Exception {
        var request = job.getRequest();
        var user = MCRSessionMgr.getCurrentSession().getUserInformation();
        String[] roles = java.util.Arrays.stream(org.mycore.common.config.MCRConfiguration2
            .getString("MCR.CircleOverview.Roles").orElse("admin,editor").split(","))
            .map(String::trim).filter(role -> !role.isEmpty()).toArray(String[]::new);
        boolean allowed = java.util.Arrays.stream(roles).anyMatch(user::isUserInRole);
        if (!allowed) {
            throw org.mycore.access.MCRAccessException.missingPrivilege("Kreisverweise prüfen", roles);
        }
        if (!"GET".equals(request.getMethod())) {
            throw new RequestFailure(405,
                "Diese Übersicht ist ausschließlich lesend. Änderungen bitte im Editor vornehmen.");
        }
        String action = request.getParameter("action");
        if (action != null && !"preview".equals(action)) {
            throw new RequestFailure(400, "Diese Übersicht bietet keine Schreibaktionen an.");
        }
        acquire(request);
        request.setAttribute(RESULT, previewXml(MCRCircleRepairService.previewIndexed(repository(), indexedLinks())));
    }

    /** One scalar database query, without loading managed entities or individual XML objects. */
    protected java.util.List<MCRObjectLinkGraph.MCRObjectLink> indexedLinks() {
        return org.mycore.backend.jpa.MCREntityManagerProvider.getCurrentEntityManager()
            .createQuery("select l.key.mcrfrom, l.key.mcrto, l.key.mcrtype "
                + "from MCRLINKHREF l where l.key.mcrtype in ('parent', 'reference')", Object[].class)
            .getResultList().stream()
            .map(row -> new MCRObjectLinkGraph.MCRObjectLink((String) row[0], (String) row[1], (String) row[2]))
            .toList();
    }

    protected MCRCircleRepairService.Reader repository() {
        return new MCRCircleRepairService.Reader() {
            public byte[] read(String id) throws Exception {
                try (var input = org.mycore.datamodel.common.MCRXMLMetadataManager.instance()
                    .retrieveContent(org.mycore.datamodel.metadata.MCRObjectID.getInstance(id)).getInputStream()) {
                    return input.readAllBytes();
                }
            }
        };
    }

    private static void acquire(HttpServletRequest request) throws RequestFailure {
        if (!SCAN_LOCK.tryLock()) {
            throw new RequestFailure(409, "Eine Kreisprüfung läuft bereits. Bitte anschließend erneut versuchen.");
        }
        request.setAttribute(LOCKED, Boolean.TRUE);
    }

    @Override
    protected void render(MCRServletJob job, Exception failure) throws Exception {
        var request = job.getRequest();
        var response = job.getResponse();
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setCharacterEncoding("UTF-8");
        try {
            if (failure instanceof RequestFailure expected) {
                if (expected.status == 405) {
                    response.setHeader("Allow", "GET");
                }
                response.sendError(expected.status, expected.getMessage());
                return;
            }
            if (failure != null) {
                // Let MyCoRe handle access exceptions and unexpected failures with its normal error pages.
                throw failure;
            }
            if (request.getAttribute(RESULT) instanceof org.jdom2.Element page) {
                org.mycore.common.xml.MCRLayoutService.instance().doLayout(request, response,
                    new org.mycore.common.content.MCRJDOMContent(page));
            } else {
                throw new org.mycore.common.MCRException("Die Kreisprüfung hat keinen Bericht erzeugt.");
            }
        } finally {
            if (Boolean.TRUE.equals(request.getAttribute(LOCKED))) {
                SCAN_LOCK.unlock();
            }
        }
    }

    static org.jdom2.Element previewXml(MCRCircleRepairService.Preview preview) {
        var root = new org.jdom2.Element("circleRepair")
            .setAttribute("objects", Integer.toString(preview.objectCount()))
            .setAttribute("generated", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        for (var group : preview.cases()) {
            var element = new org.jdom2.Element("group").setAttribute("suggested", Boolean.toString(group.suggested()));
            element.addContent(new org.jdom2.Element("explanation").setText(group.explanation()));
            for (String id : group.members()) {
                element.addContent(new org.jdom2.Element("member").setText(id));
            }
            for (var link : group.links()) {
                element.addContent(new org.jdom2.Element("link").setAttribute("from", link.from())
                    .setAttribute("to", link.to()).setAttribute("relation", link.relation())
                    .setAttribute("position", Integer.toString(link.position()))
                    .setAttribute("remove", Boolean.toString(link.remove())).setText(link.reason()));
            }
            root.addContent(element);
        }
        return root;
    }

    static class RequestFailure extends Exception {
        private static final long serialVersionUID = 1L;
        final int status;

        RequestFailure(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
