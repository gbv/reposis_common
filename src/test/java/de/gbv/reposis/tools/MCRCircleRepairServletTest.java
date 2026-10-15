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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MCRCircleRepairServletTest extends org.mycore.common.MCRTestCase {
    @Test
    public void rejectsUnauthorizedOrUnsafeRequestsBeforeAccessingRepository() throws Exception {
        var session = org.mycore.common.MCRSessionMgr.getCurrentSession();
        var original = session.getUserInformation();
        try {
            for (String scenario : new String[] { "guest", "getApply", "post", "editorPost" }) {
                session.setUserInformation(new org.mycore.common.MCRUserInformation() {
                    public String getUserID() {
                        return "test";
                    }

                    public boolean isUserInRole(String role) {
                        return !scenario.equals("guest")
                            && role.equals(scenario.equals("editorPost") ? "editor" : "admin");
                    }

                    public String getUserAttribute(String attribute) {
                        return null;
                    }
                });
                Map<String, Object> sessionAttributes = new HashMap<>();
                var httpSession = (jakarta.servlet.http.HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { jakarta.servlet.http.HttpSession.class }, (proxy, method, args) -> {
                        if ("getAttribute".equals(method.getName()))
                            return sessionAttributes.get(args[0]);
                        if ("setAttribute".equals(method.getName()))
                            sessionAttributes.put((String) args[0], args[1]);
                        return null;
                    });
                var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
                        case "getMethod" -> scenario.endsWith("Post") || scenario.equals("post") ? "POST" : "GET";
                        case "getParameter" -> scenario.equals("guest") ? "preview" : "apply";
                        case "getSession" -> httpSession;
                        default -> null;
                    });
                var servlet = new MCRCircleRepairServlet() {
                    protected MCRCircleRepairService.Reader repository() {
                        throw new AssertionError("Unauthorized request reached repository");
                    }
                };
                var failure = Assert.assertThrows(Exception.class,
                    () -> servlet.think(new MCRServletJob(request, null)));
                if (scenario.equals("guest")) {
                    Assert.assertTrue(failure instanceof org.mycore.access.MCRAccessException);
                } else {
                    Assert.assertEquals(scenario.equals("getApply") ? 400 : 405,
                        ((MCRCircleRepairServlet.RequestFailure) failure).status);
                }
            }
        } finally {
            session.setUserInformation(original);
        }
    }

    @Test
    public void rendersXsltWithApplicationLayout() throws Exception {
        var row = new MCRCircleRepairService.PreviewLink("test_mods_00000001", "test_mods_00000002",
            "otherVersion", 1, true, "Begründung mit <script> und & bleibt Text.");
        var group = new MCRCircleRepairService.RepairCase(java.util.List.of("test_mods_00000001"),
            java.util.List.of(row), true, "Vorschlag prüfen.");
        var xml = MCRCircleRepairServlet.previewXml(
            new MCRCircleRepairService.Preview(java.util.List.of(group), 2));
        // The application supplies MyCoReLayout; the module alone has no MIR layout dependency.
        var factory = javax.xml.transform.TransformerFactory.newDefaultInstance();
        factory.setURIResolver((href, base) -> {
            if (!"MyCoReLayout.xsl".equals(href)) {
                throw new javax.xml.transform.TransformerException("Unexpected include: " + href);
            }
            return new javax.xml.transform.stream.StreamSource(new java.io.StringReader("""
                <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:param name="WebApplicationBaseURL" select="'/mir/'"/>
                  <xsl:template match="/"><html><body><xsl:apply-templates/></body></html></xsl:template>
                </xsl:stylesheet>
                """));
        });
        var stylesheet = getClass().getResource("/xsl/circleRepair.xsl");
        var transformer = factory.newTransformer(new javax.xml.transform.stream.StreamSource(stylesheet.toString()));
        var output = new StringWriter();
        transformer.transform(new javax.xml.transform.stream.StreamSource(new java.io.StringReader(
            new org.jdom2.output.XMLOutputter().outputString(xml))),
            new javax.xml.transform.stream.StreamResult(output));
        String rendered = output.toString();
        Assert.assertTrue(rendered.contains("circle-repair"));
        Assert.assertTrue(org.jsoup.Jsoup.parse(rendered).select("button.apply-fix").isEmpty());
        Assert.assertFalse(rendered.contains("data-payload"));
        Assert.assertFalse(rendered.contains("@hash@"));
        Assert.assertTrue(rendered.contains("Entfernen prüfen")
            || org.jsoup.Jsoup.parse(rendered).text().contains("Entfernen prüfen"));
        Assert.assertFalse(rendered.contains("data-csrf"));
        Assert.assertFalse(rendered.contains("<script>"));
        Assert.assertTrue(rendered.contains("&lt;script&gt;"));
    }

    @Test
    public void requestErrorsUseContainerErrorPages() throws Exception {
        Map<String, Object> responseCalls = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> null);
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
                if ("getWriter".equals(method.getName()))
                    throw new AssertionError("Must not write own error body");
                if ("sendError".equals(method.getName()))
                    responseCalls.put("status", args[0]);
                if ("setHeader".equals(method.getName()))
                    responseCalls.put((String) args[0], args[1]);
                return null;
            });
        new MCRCircleRepairServlet().render(new MCRServletJob(request, response),
            new MCRCircleRepairServlet.RequestFailure(405, "Nur GET erlaubt"));
        Assert.assertEquals(405, responseCalls.get("status"));
        Assert.assertEquals("GET", responseCalls.get("Allow"));
    }

    @Test
    public void commitFailureNeverRendersSuccess() throws Exception {
        StringWriter body = new StringWriter();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(MCRCircleRepairServlet.class.getName() + ".result", Map.of("message", "success"));
        int[] status = { 200 };
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { HttpServletRequest.class },
            (proxy, method, args) -> "getAttribute".equals(method.getName()) ? attributes.get(args[0]) : null);
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
                if ("getWriter".equals(method.getName())) {
                    return new PrintWriter(body);
                }
                if ("setStatus".equals(method.getName())) {
                    status[0] = (int) args[0];
                }
                return null;
            });
        for (Exception failure : java.util.List.of(new Exception("simulated transaction commit failure"),
            org.mycore.access.MCRAccessException.missingPrivilege("Kreisverweise prüfen", "editor"))) {
            Assert.assertSame(failure, Assert.assertThrows(Exception.class,
                () -> new MCRCircleRepairServlet().render(new MCRServletJob(request, response), failure)));
            Assert.assertEquals(200, status[0]);
            Assert.assertEquals("", body.toString());
        }
    }
}
