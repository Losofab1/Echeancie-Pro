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
import com.mycompany.jsf.util.PagingInfo;
import entities.EmailOutbox;
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

/**
 *
 * @author fabri
 */
@Named("emailOutbox")
@SessionScoped
public class EmailOutboxController implements Serializable {

    private static final long serialVersionUID = 1L;

    public EmailOutboxController() {
        pagingInfo = new PagingInfo();
        converter = new EmailOutboxConverter();
    }
    private EmailOutbox emailOutbox = null;
    private List<EmailOutbox> emailOutboxItems = null;
    private transient EmailOutboxJpaController jpaController = null;
    private EmailOutboxConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getEmailOutboxCount());
        }
        return pagingInfo;
    }

    public EmailOutboxJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new EmailOutboxJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getEmailOutboxItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findEmailOutboxEntities(), false);
    }

    public SelectItem[] getEmailOutboxItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findEmailOutboxEntities(), true);
    }

    public EmailOutbox getEmailOutbox() {
        if (emailOutbox == null) {
            emailOutbox = (EmailOutbox) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentEmailOutbox", converter, null);
        }
        if (emailOutbox == null) {
            emailOutbox = new EmailOutbox();
        }
        return emailOutbox;
    }

    public String listSetup() {
        reset(true);
        return "emailOutbox_list";
    }

    public String createSetup() {
        reset(false);
        emailOutbox = new EmailOutbox();
        return "emailOutbox_create";
    }

    public String create() {
        try {
            getJpaController().create(emailOutbox);
            JsfUtil.addSuccessMessage("EmailOutbox was successfully created.");
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("emailOutbox_detail");
    }

    public String editSetup() {
        return scalarSetup("emailOutbox_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        emailOutbox = (EmailOutbox) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentEmailOutbox", converter, null);
        if (emailOutbox == null) {
            String requestEmailOutboxString = JsfUtil.getRequestParameter("jsfcrud.currentEmailOutbox");
            JsfUtil.addErrorMessage("The emailOutbox with id " + requestEmailOutboxString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String emailOutboxString = converter.getAsString(FacesContext.getCurrentInstance(), null, emailOutbox);
        String currentEmailOutboxString = JsfUtil.getRequestParameter("jsfcrud.currentEmailOutbox");
        if (emailOutboxString == null || emailOutboxString.length() == 0 || !emailOutboxString.equals(currentEmailOutboxString)) {
            String outcome = editSetup();
            if ("emailOutbox_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit emailOutbox. Try again.");
            }
            return outcome;
        }
        try {
            getJpaController().edit(emailOutbox);
            JsfUtil.addSuccessMessage("EmailOutbox was successfully updated.");
        } catch (NonexistentEntityException ne) {
            JsfUtil.addErrorMessage(ne.getLocalizedMessage());
            return listSetup();
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return detailSetup();
    }

    public String destroy() {
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentEmailOutbox");
        try {
            UUID id = UUID.fromString(idAsString);
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("EmailOutbox was successfully deleted.");
        } catch (NonexistentEntityException ne) {
            JsfUtil.addErrorMessage(ne.getLocalizedMessage());
            return relatedOrListOutcome();
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

    public List<EmailOutbox> getEmailOutboxItems() {
        if (emailOutboxItems == null) {
            getPagingInfo();
            emailOutboxItems = getJpaController().findEmailOutboxEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return emailOutboxItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "emailOutbox_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "emailOutbox_list";
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
        emailOutbox = null;
        emailOutboxItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        EmailOutbox newEmailOutbox = new EmailOutbox();
        String newEmailOutboxString = converter.getAsString(FacesContext.getCurrentInstance(), null, newEmailOutbox);
        String emailOutboxString = converter.getAsString(FacesContext.getCurrentInstance(), null, emailOutbox);
        if (!newEmailOutboxString.equals(emailOutboxString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }
    
}
