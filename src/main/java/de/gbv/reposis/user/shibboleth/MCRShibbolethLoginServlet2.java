package de.gbv.reposis.user.shibboleth;

/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.common.MCRISO8601Format;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * <p>This servlet is used to login a user via Shibboleth.</p>
 * <p>It uses {@link MCRShibbolethUserMapper} to map the Shibboleth attributes, stored in the HTTP request attributes,
 * to a {@link MCRUser} object.
 * The {@link MCRShibbolethUserActionFactory} is used to resolve a {@link MCRShibbolethUserMapper}.
 * </p>
 * <p>
 * If the property {@link #SHIBBOLETH_PERSIST_USER_PROPERTY} is set to true, the user is persisted in the user database.
 * If the User with the given ID already exists, the user in the Database is merged with the user from the Shibboleth
 * attributes using {@link MCRShibbolethUserMerger}.
 * The {@link MCRShibbolethUserActionFactory} is used to resolve a {@link MCRShibbolethUserMerger}.
 * </p>
 *
 * @author Sebastian Hofmann
 */
public class MCRShibbolethLoginServlet2 extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRShibbolethLoginServlet2.class);

    private static final String SHIBBOLETH_PERSIST_USER_PROPERTY = "MCR.User.Shibboleth.PersistUser";
    public static final Boolean PERSIST_USER
        = MCRConfiguration2.getBoolean(SHIBBOLETH_PERSIST_USER_PROPERTY).orElse(false);

    private static boolean handleUserExpired(MCRServletJob job, MCRUser existingUser) throws IOException {
        if (!existingUser.loginAllowed()) {
            String message;

            if (existingUser.isDisabled()) {
                message = "User " + existingUser.getUserID() + " was disabled!";
                LOGGER.warn(message);
            } else {
                message = "Password expired for user " + existingUser.getUserID() + " on " + MCRXMLFunctions
                    .getISODate(existingUser.getValidUntil(), MCRISO8601Format.COMPLETE_HH_MM_SS.toString());
                LOGGER.warn(message);
            }
            job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
            return true;
        }
        return false;
    }

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        MCRShibbolethUserMapper userMapper = MCRShibbolethUserActionFactory.getUserMapper();

        MCRUser mcrUser;
        try {
            mcrUser = userMapper.mapAttributes(req.getRemoteUser(), (name) -> (String) req.getAttribute(name));
        } catch (IllegalArgumentException e) {
            job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }

        if (PERSIST_USER) {
            MCRUser existingUser = MCRUserManager.getUser(mcrUser.getUserName(), mcrUser.getRealm());

            if (existingUser != null) {
                // user exists and attributes need to be updated
                if (MCRShibbolethUserActionFactory.getUserMerger().merge(existingUser, mcrUser)) {
                    MCRUserManager.updateUser(existingUser);
                }
                mcrUser = existingUser;

                if (handleUserExpired(job, existingUser)) {
                    return;
                }
            } else {
                // user does not exist
                MCRUserManager.createUser(mcrUser);

                handleNewUser(job, mcrUser);
            }
        }

        MCRSessionMgr.getCurrentSession().setUserInformation(mcrUser);
        // MCR-1154
        req.changeSessionId();
        res.sendRedirect(res.encodeRedirectURL(req.getParameter("url")));
    }

    private void handleNewUser(MCRServletJob job, MCRUser mcrUser) {
        MCRShibbolethUserActionFactory.getNewUserHandler().handleNewUser(mcrUser);
    }

}
