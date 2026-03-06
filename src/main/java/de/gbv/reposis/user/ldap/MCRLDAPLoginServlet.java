package de.gbv.reposis.user.ldap;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.mycore.user2.login.MCRLogin;
import org.mycore.user2.login.MCRLoginServlet;
import org.xml.sax.SAXException;

import jakarta.servlet.ServletException;
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

    private boolean persistUser;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.persistUser = MCRConfiguration2.getBoolean(PROP_PREFIX + "PersistUser").orElse(false);
        } catch (MCRConfigurationException e) {
            throw new ServletException("Failed to initialize MCRLDAPLoginServlet", e);
        }
    }

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
        if (uid != null && realm != null) {
            MCRLDAPAuthService authService =
                MCRConfiguration2.<MCRLDAPAuthService>getSingleInstanceOf(
                        PROP_PREFIX + "AuthService." + realm + ".Class")
                    .orElseThrow(
                        () -> new MCRConfigurationException("No LDAP auth service configured for realm: " + realm));
            authService.init(realm);
            try {
                MCRUser user = authService.authenticate(uid, pwd);
                if (persistUser) {
                    if (!MCRUserManager.exists(uid, realm)) {
                        MCRUserManager.createUser(user);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Created User {}", user.getUserID());
                        }
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("User {} already exists", user.getUserID());
                        }
                    }
                }
                MCRSessionMgr.getCurrentSession().setUserInformation(user);
                req.changeSessionId();
                LOGGER.debug("user {} logged in successfully", user.getUserID());
                res.sendRedirect(res.encodeRedirectURL(getReturnURL(req)));
                return;
            } catch (MCRLDAPAuthenticationException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                loginForm.setLoginFailed(true);
            }
        }
        addFormFields(loginForm, job.getRequest().getParameter(REALM_URL_PARAMETER));
        getLayoutService().doLayout(req, res, new MCRJAXBContent<>(JAXBContext.newInstance(MCRLogin.class), loginForm));
    }
}
