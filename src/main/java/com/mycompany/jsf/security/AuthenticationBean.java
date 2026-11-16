package com.mycompany.jsf.security;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

@Named("auth")
@SessionScoped
public class AuthenticationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
            Logger.getLogger(AuthenticationBean.class.getName());

    private String email;
    private String password;
    private AuthenticatedUser user;

    public String login() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            addLoginError(facesContext);
            return null;
        }

        char[] passwordChars = password.toCharArray();
        password = null;
        NamePasswordCallbackHandler callbackHandler =
                new NamePasswordCallbackHandler(email.trim(), passwordChars);
        LoginContext loginContext = null;
        boolean loggedIn = false;

        try {
            loginContext = new LoginContext(
                    EcheanJaasConfiguration.APPLICATION_NAME,
                    new Subject(),
                    callbackHandler,
                    new EcheanJaasConfiguration());
            loginContext.login();
            loggedIn = true;

            Subject subject = loginContext.getSubject();
            UserPrincipal userPrincipal = subject.getPrincipals(UserPrincipal.class)
                    .stream().findFirst().orElseThrow(LoginException::new);
            RolePrincipal rolePrincipal = subject.getPrincipals(RolePrincipal.class)
                    .stream().findFirst().orElseThrow(LoginException::new);
            user = new AuthenticatedUser(
                    userPrincipal.getId(),
                    userPrincipal.getClientId(),
                    userPrincipal.getEmail(),
                    userPrincipal.getDisplayName(),
                    rolePrincipal.getName());

            HttpServletRequest request = (HttpServletRequest) facesContext
                    .getExternalContext().getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) {
                request.changeSessionId();
            } else {
                session = request.getSession(true);
            }
            session.setAttribute(AuthenticatedUser.SESSION_ATTRIBUTE, user);

            try {
                loginContext.logout();
                loggedIn = false;
            } catch (LoginException ex) {
                LOGGER.log(Level.WARNING, "Unable to clear the JAAS login subject.", ex);
            }
            String destination = user.isClient()
                    ? "/client/dashboard.xhtml"
                    : user.isManager() ? "/manager/dashboard.xhtml" : "/welcomeJSF.xhtml";
            return destination + "?faces-redirect=true";
        } catch (LoginException ex) {
            if (loggedIn && loginContext != null) {
                try {
                    loginContext.logout();
                } catch (LoginException logoutException) {
                    ex.addSuppressed(logoutException);
                }
            }
            if (ex.getCause() != null) {
                LOGGER.log(Level.SEVERE, "JAAS authentication service failed.", ex);
            }
            user = null;
            addLoginError(facesContext);
            return null;
        } finally {
            callbackHandler.clear();
            Arrays.fill(passwordChars, '\0');
            password = null;
        }
    }

    public String logout() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        user = null;
        facesContext.getExternalContext().invalidateSession();
        facesContext.getExternalContext().redirect(
                facesContext.getExternalContext().getRequestContextPath() + "/login.xhtml");
        facesContext.responseComplete();
        return null;
    }

    private void addLoginError(FacesContext facesContext) {
        facesContext.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Identifiants invalides, compte inactif ou service indisponible.",
                null));
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AuthenticatedUser getUser() {
        if (user == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                Object sessionUser = facesContext.getExternalContext()
                        .getSessionMap().get(AuthenticatedUser.SESSION_ATTRIBUTE);
                if (sessionUser instanceof AuthenticatedUser authenticatedUser) {
                    user = authenticatedUser;
                }
            }
        }
        return user;
    }

    public void refreshProfile(String displayName, String email) {
        AuthenticatedUser currentUser = getUser();
        if (currentUser == null) {
            throw new IllegalStateException("Aucune session authentifiée.");
        }
        AuthenticatedUser updatedUser = new AuthenticatedUser(
                currentUser.getId(), currentUser.getClientId(),
                email, displayName, currentUser.getRole());
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
                .put(AuthenticatedUser.SESSION_ATTRIBUTE, updatedUser);
        user = updatedUser;
    }

    public boolean isAdmin() {
        AuthenticatedUser authenticatedUser = getUser();
        return authenticatedUser != null && authenticatedUser.isAdmin();
    }

    public boolean isManager() {
        AuthenticatedUser authenticatedUser = getUser();
        return authenticatedUser != null && authenticatedUser.isManager();
    }

    public boolean isClient() {
        AuthenticatedUser authenticatedUser = getUser();
        return authenticatedUser != null && authenticatedUser.isClient();
    }
}
