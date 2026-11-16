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
import entities.Installments;
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
@Named("installments")
@SessionScoped
public class InstallmentsController implements Serializable {

    private static final long serialVersionUID = 1L;

    public InstallmentsController() {
        pagingInfo = new PagingInfo();
        converter = new InstallmentsConverter();
    }
    private Installments installments = null;
    private List<Installments> installmentsItems = null;
    private transient InstallmentsJpaController jpaController = null;
    private InstallmentsConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getInstallmentsCount());
        }
        return pagingInfo;
    }

    public InstallmentsJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new InstallmentsJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getInstallmentsItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findInstallmentsEntities(), false);
    }

    public SelectItem[] getInstallmentsItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findInstallmentsEntities(), true);
    }

    public Installments getInstallments() {
        if (installments == null) {
            installments = (Installments) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentInstallments", converter, null);
        }
        if (installments == null) {
            installments = new Installments();
        }
        return installments;
    }

    public String listSetup() {
        reset(true);
        return "installments_list";
    }

    public String createSetup() {
        reset(false);
        installments = new Installments();
        return "installments_create";
    }

    public String create() {
        try {
            getJpaController().create(installments);
            JsfUtil.addSuccessMessage("Installments was successfully created.");
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("installments_detail");
    }

    public String editSetup() {
        return scalarSetup("installments_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        installments = (Installments) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentInstallments", converter, null);
        if (installments != null && installments.getId() != null) {
            installments = getJpaController().findInstallmentsWithSubmissions(installments.getId());
        }
        if (installments == null) {
            String requestInstallmentsString = JsfUtil.getRequestParameter("jsfcrud.currentInstallments");
            JsfUtil.addErrorMessage("The installments with id " + requestInstallmentsString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String installmentsString = converter.getAsString(FacesContext.getCurrentInstance(), null, installments);
        String currentInstallmentsString = JsfUtil.getRequestParameter("jsfcrud.currentInstallments");
        if (installmentsString == null || installmentsString.length() == 0 || !installmentsString.equals(currentInstallmentsString)) {
            String outcome = editSetup();
            if ("installments_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit installments. Try again.");
            }
            return outcome;
        }
        try {
            getJpaController().edit(installments);
            JsfUtil.addSuccessMessage("Installments was successfully updated.");
        } catch (IllegalOrphanException oe) {
            JsfUtil.addErrorMessages(oe.getMessages());
            return null;
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
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentInstallments");
        try {
            UUID id = UUID.fromString(idAsString);
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("Installments was successfully deleted.");
        } catch (IllegalOrphanException oe) {
            JsfUtil.addErrorMessages(oe.getMessages());
            return null;
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

    public List<Installments> getInstallmentsItems() {
        if (installmentsItems == null) {
            getPagingInfo();
            installmentsItems = getJpaController().findInstallmentsEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return installmentsItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "installments_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "installments_list";
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
        installments = null;
        installmentsItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        Installments newInstallments = new Installments();
        String newInstallmentsString = converter.getAsString(FacesContext.getCurrentInstance(), null, newInstallments);
        String installmentsString = converter.getAsString(FacesContext.getCurrentInstance(), null, installments);
        if (!newInstallmentsString.equals(installmentsString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }
    
}
