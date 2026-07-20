package de.gbv.reposis.user.ldap;

import java.io.IOException;
import java.util.Date;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.mycore.user2.login.MCRLogin;
import org.mycore.user2.login.MCRLoginServlet;
import org.xml.sax.SAXException;

import de.gbv.reposis.user.MCRUserData;
import de.gbv.reposis.user.persistence.MCRUserPersistenceStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * Login servlet that authenticates users against an LDAP server.
 * <p>
 * Extends {@link MCRLoginServlet} and overrides {@link #presentLoginForm} to delegate
 * credential verification to a realm-specific {@link MCRLDAPAuthService}.
 */
public class MCRLDAPLoginServlet extends MCRLoginServlet {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROP_PREFIX = "MCRLDAPLoginServlet.";

    @Override
    protected void presentLoginForm(MCRServletJob job)
        throws IOException, TransformerException, SAXException, JAXBException {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (LOCAL_LOGIN_SECURE_ONLY && !req.isSecure()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, getErrorI18N("component.user2.login", "httpsOnly"));
            return;
        }
        String returnURL = getReturnURL(req);
        String formAction = req.getRequestURI();
        MCRLogin loginForm =
            new MCRLogin(MCRSessionMgr.getCurrentSession().getUserInformation(), returnURL, formAction);
        String uid = getProperty(req, "uid");
        String pwd = getProperty(req, "pwd");
        String realm = getProperty(req, "realm");

        if (StringUtils.isBlank(realm)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        MCRLDAPAuthService authService;
        MCRUserPersistenceStrategy persistenceStrategy;

        try {
            authService = getAuthService(realm);
            persistenceStrategy = getPersistenceStrategy(realm);
        } catch (MCRConfigurationException e) {
            LOGGER.error("Error while authenticating user", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (StringUtils.isNotBlank(uid)) {
            if (!"POST".equalsIgnoreCase(req.getMethod())) {
                res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            try {
                MCRUserData userData = authService.authenticate(uid, pwd);
                MCRUser currentUser = persistenceStrategy.apply(userData);
                if (!(currentUser instanceof MCRTransientUser)) {
                    currentUser.setLastLogin(new Date(MCRSessionMgr.getCurrentSession().getLoginTime()));
                    MCRUserManager.updateUser(currentUser);
                }
                MCRSessionMgr.getCurrentSession().setUserInformation(currentUser);
                req.changeSessionId();
                LOGGER.debug("user {} logged in successfully", userData.userId());
                res.sendRedirect(res.encodeRedirectURL(getReturnURL(req)));
                return;
            } catch (MCRLDAPAuthException e) {
                LOGGER.warn("Failed login attempt for uid: {}", uid);
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                loginForm.setLoginFailed(true);
            } catch (Exception e) {
                LOGGER.error("Error while authenticating user", e);
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        addFormFields(loginForm, job.getRequest().getParameter(REALM_URL_PARAMETER));
        getLayoutService().doLayout(req, res, new MCRJAXBContent<>(JAXBContext.newInstance(MCRLogin.class), loginForm));
    }

    private MCRUserPersistenceStrategy getPersistenceStrategy(String realm) {
        String config = PROP_PREFIX + "PersistenceStrategy." + realm + ".Class";
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRUserPersistenceStrategy.class, config);
    }

    private MCRLDAPAuthService getAuthService(String realm) {
        MCRLDAPAuthService authService = MCRConfiguration2.getSingleInstanceOf(MCRLDAPAuthService.class,
                PROP_PREFIX + "AuthService." + realm + ".Class")
            .orElseThrow(() -> new MCRConfigurationException("No LDAP auth service configured for realm: " + realm));
        authService.init(realm);
        return authService;
    }
}
