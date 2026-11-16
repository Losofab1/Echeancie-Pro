package com.mycompany.jsf;

import com.mycompany.jsf.security.AuthenticatedUser;
import com.mycompany.jsf.security.AuthenticationBean;
import com.mycompany.jsf.security.ProfileService;
import com.mycompany.jsf.security.ProfileService.ProfileData;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("profile")
@ViewScoped
public class ProfileBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ProfileBean.class.getName());

    @Inject
    private AuthenticationBean authenticationBean;
    @Inject
    private ProfileService profileService;

    private String name;
    private String email;
    private String phone;
    private String currentPassword;
    private String newPassword;
    private String passwordConfirmation;
    private boolean client;
    private String role;

    @PostConstruct
    public void initialize() {
        try {
            ProfileData profile = profileService.loadProfile(currentUserId());
            setProfile(profile);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load the authenticated user's profile.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Le profil n’a pas pu être chargé.", null);
        }
    }

    public String save() {
        char[] currentPasswordChars = toChars(currentPassword);
        char[] newPasswordChars = toChars(newPassword);
        char[] confirmationChars = toChars(passwordConfirmation);
        try {
            ProfileData updated = profileService.updateProfile(
                    currentUserId(), name, email, client ? phone : null,
                    currentPasswordChars, newPasswordChars, confirmationChars);
            setProfile(updated);
            authenticationBean.refreshProfile(updated.getName(), updated.getEmail());
            addMessage(FacesMessage.SEVERITY_INFO,
                    "Vos coordonnées ont été mises à jour.", null);
            FacesContext.getCurrentInstance().getExternalContext()
                    .getFlash().setKeepMessages(true);
            return "/profile.xhtml?faces-redirect=true";
        } catch (IllegalArgumentException | SecurityException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null);
            return null;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to update the authenticated user's profile.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Les coordonnées n’ont pas pu être enregistrées. Réessayez plus tard.", null);
            return null;
        } finally {
            clear(currentPasswordChars);
            clear(newPasswordChars);
            clear(confirmationChars);
            currentPassword = null;
            newPassword = null;
            passwordConfirmation = null;
        }
    }

    private void setProfile(ProfileData profile) {
        name = profile.getName();
        email = profile.getEmail();
        phone = profile.getPhone();
        client = profile.isClient();
        role = profile.getRole();
    }

    private java.util.UUID currentUserId() {
        AuthenticatedUser user = authenticationBean.getUser();
        if (user == null) {
            throw new SecurityException("Une session authentifiée est nécessaire.");
        }
        return user.getId();
    }

    private char[] toChars(String value) {
        return value == null ? null : value.toCharArray();
    }

    private void clear(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }
    public boolean isClient() { return client; }
    public String getRole() { return role; }
}
