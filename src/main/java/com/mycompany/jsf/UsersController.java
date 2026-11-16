/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.faces.FacesException;
import com.mycompany.jsf.util.JsfUtil;
import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.IllegalOrphanException;
import com.mycompany.jsf.util.PagingInfo;
import entities.Users;
import jakarta.annotation.Resource;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.model.SelectItem;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.UserTransaction;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import com.mycompany.jsf.security.PasswordHasher;
import com.mycompany.jsf.security.RoleNames;
import com.mycompany.jsf.security.AuthenticatedUser;
import com.mycompany.jsf.security.AuthenticationBean;
import jakarta.inject.Inject;

/**
 *
 * @author fabri
 */
@Named("users")
@SessionScoped
public class UsersController implements Serializable {

    private static final long serialVersionUID = 1L;

    public UsersController() {
        pagingInfo = new PagingInfo();
        converter = new UsersConverter();
    }
    private Users users = null;
    private String newPassword;
    private List<Users> usersItems = null;
    private UUID primaryAdministratorId;
    private boolean primaryAdministratorIdLoaded;
    private transient UsersJpaController jpaController = null;
    private UsersConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;
    @Inject
    private AuthenticationBean authenticationBean;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getUsersCount());
        }
        return pagingInfo;
    }

    public UsersJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new UsersJpaController(utx, emf);
        }
        return jpaController;
    }

    public UUID getPrimaryAdministratorId() {
        if (!primaryAdministratorIdLoaded) {
            primaryAdministratorId = getJpaController().findPrimaryAdministratorId();
            primaryAdministratorIdLoaded = true;
        }
        return primaryAdministratorId;
    }

    public SelectItem[] getUsersItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findUsersEntities(), false);
    }

    public SelectItem[] getUsersItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findUsersEntities(), true,
                user -> user.getName() + " — " + user.getEmail());
    }

    public Users getUsers() {
        if (users == null) {
            users = (Users) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentUsers", converter, null);
        }
        if (users == null) {
            users = new Users();
        }
        return users;
    }

    public String listSetup() {
        reset(true);
        return "users_list";
    }

    public String createSetup() {
        reset(false);
        users = new Users();
        users.setActive(true);
        newPassword = null;
        return "users_create";
    }

    public String create() {
        try {
            validateAccount(true);
            users.setRole(RoleNames.normalize(users.getRole()));
            users.setActive(true);
            users.setClientId(null);
            users.setPasswordHash(hashNewPassword());
            users.setCreatedAt(new Date());
            getJpaController().create(users);
            JsfUtil.addSuccessMessage("Le compte a été créé avec succès.");
        } catch (IllegalArgumentException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        } finally {
            newPassword = null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("users_detail");
    }

    public String editSetup() {
        return scalarSetup("users_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        users = (Users) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentUsers", converter, null);
        if (users != null && users.getId() != null) {
            users = getJpaController().findUsersWithRelations(users.getId());
        }
        if (users == null) {
            String requestUsersString = JsfUtil.getRequestParameter("jsfcrud.currentUsers");
            JsfUtil.addErrorMessage("The users with id " + requestUsersString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String usersString = converter.getAsString(FacesContext.getCurrentInstance(), null, users);
        String currentUsersString = JsfUtil.getRequestParameter("jsfcrud.currentUsers");
        if (usersString == null || usersString.length() == 0 || !usersString.equals(currentUsersString)) {
            String outcome = editSetup();
            if ("users_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit users. Try again.");
            }
            return outcome;
        }
        try {
            validateAccount(false);
            users.setRole(RoleNames.normalize(users.getRole()));
            if (!RoleNames.CLIENT.equals(users.getRole())) {
                users.setClientId(null);
            }
            if (newPassword != null && !newPassword.isBlank()) {
                users.setPasswordHash(hashNewPassword());
            }
            getJpaController().edit(users);
            JsfUtil.addSuccessMessage("Le compte a été mis à jour avec succès.");
        } catch (IllegalArgumentException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        } catch (IllegalOrphanException oe) {
            JsfUtil.addErrorMessages(oe.getMessages());
            return null;
        } catch (NonexistentEntityException ne) {
            JsfUtil.addErrorMessage(ne.getLocalizedMessage());
            return listSetup();
        } catch (IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        } finally {
            newPassword = null;
        }
        return detailSetup();
    }

    private void validateAccount(boolean requirePassword) {
        if (!RoleNames.isAllowed(users.getRole())) {
            throw new IllegalArgumentException(
                    "Le rôle doit être Administrateur, Gestionnaire ou Client.");
        }
        String normalizedName = users.getName() == null ? "" : users.getName().trim();
        String normalizedEmail = users.getEmail() == null
                ? "" : users.getEmail().trim().toLowerCase(Locale.ROOT);
        if (normalizedName.isEmpty() || normalizedName.length() > 120) {
            throw new IllegalArgumentException(
                    "Le nom est obligatoire et ne peut pas dépasser 120 caractères.");
        }
        if (normalizedEmail.length() > 254
                || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Saisissez une adresse courriel valide.");
        }
        users.setName(normalizedName);
        users.setEmail(normalizedEmail);
        if (getJpaController().emailExists(normalizedEmail, users.getId())) {
            throw new IllegalArgumentException(
                    "Cette adresse courriel est déjà utilisée par un autre compte.");
        }
        if (requirePassword && RoleNames.CLIENT.equals(RoleNames.normalize(users.getRole()))) {
            throw new IllegalArgumentException(
                    "Un compte CLIENT doit être créé avec sa fiche client.");
        }
        if (RoleNames.CLIENT.equals(RoleNames.normalize(users.getRole()))
                && users.getClientId() == null) {
            throw new IllegalArgumentException(
                    "Un compte CLIENT doit être lié à une fiche client.");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            if (requirePassword) {
                throw new IllegalArgumentException("Le mot de passe est obligatoire.");
            }
            return;
        }
        if (newPassword.length() < PasswordHasher.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins "
                    + PasswordHasher.MIN_PASSWORD_LENGTH + " caractères.");
        }
    }

    private String hashNewPassword() {
        char[] passwordChars = newPassword.toCharArray();
        try {
            return PasswordHasher.hash(passwordChars);
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    public boolean isCurrentUserPrimaryAdministrator() {
        AuthenticatedUser currentUser = authenticationBean.getUser();
        return currentUser != null
                && currentUser.isAdmin()
                && currentUser.getId().equals(getPrimaryAdministratorId());
    }

    public String deleteUnreferencedAccounts() {
        if (!isCurrentUserPrimaryAdministrator()) {
            JsfUtil.addErrorMessage(
                    "Seul l’administrateur principal peut supprimer les autres comptes.");
            return null;
        }
        try {
            UsersJpaController.AccountDeletionSummary result
                    = getJpaController().deleteUnreferencedAccountsExceptPrimary();
            JsfUtil.addSuccessMessage(
                    result.deleted() + " compte(s) supprimé(s); "
                    + result.retained() + " compte(s) conservé(s), car liés à l’historique.");
        } catch (IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String destroy() {
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentUsers");
        try {
            UUID id = UUID.fromString(idAsString);
            AuthenticatedUser currentUser = authenticationBean.getUser();
            if (currentUser != null && currentUser.getId().equals(id)) {
                JsfUtil.addErrorMessage("Vous ne pouvez pas désactiver votre propre compte.");
                return null;
            }
            getJpaController().deactivate(id);
            JsfUtil.addSuccessMessage(
                    "Compte désactivé; l’historique associé est conservé.");
        } catch (NonexistentEntityException ne) {
            JsfUtil.addErrorMessage(ne.getLocalizedMessage());
            return relatedOrListOutcome();
        } catch (IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return relatedOrListOutcome();
    }

    private String relatedOrListOutcome() {
        String relatedControllerOutcome = relatedControllerOutcome();
        if (relatedControllerOutcome != null) {
            return relatedControllerOutcome;
        }
        return listSetup();
    }

    public List<Users> getUsersItems() {
        if (usersItems == null) {
            getPagingInfo();
            usersItems = getJpaController().findUsersEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return usersItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "users_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "users_list";
    }

    private String relatedControllerOutcome() {
        String relatedControllerString = JsfUtil.getRequestParameter("jsfcrud.relatedController");
        String relatedControllerTypeString = JsfUtil.getRequestParameter("jsfcrud.relatedControllerType");
        if (relatedControllerString != null && relatedControllerTypeString != null) {
            FacesContext context = FacesContext.getCurrentInstance();
            Object relatedController = context.getApplication().getELResolver().getValue(context.getELContext(), null, relatedControllerString);
            try {
                Class<?> relatedControllerType = Class.forName(relatedControllerTypeString);
                Method detailSetupMethod = relatedControllerType.getMethod("detailSetup");
                return (String) detailSetupMethod.invoke(relatedController);
            } catch (ClassNotFoundException e) {
                throw new FacesException(e);
            } catch (NoSuchMethodException e) {
                throw new FacesException(e);
            } catch (IllegalAccessException e) {
                throw new FacesException(e);
            } catch (InvocationTargetException e) {
                throw new FacesException(e);
            }
        }
        return null;
    }

    private void reset(boolean resetFirstItem) {
        users = null;
        newPassword = null;
        usersItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        Users newUsers = new Users();
        String newUsersString = converter.getAsString(FacesContext.getCurrentInstance(), null, newUsers);
        String usersString = converter.getAsString(FacesContext.getCurrentInstance(), null, users);
        if (!newUsersString.equals(usersString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
}
