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
import entities.FlywaySchemaHistory;
import jakarta.annotation.Resource;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.model.SelectItem;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.UserTransaction;
import java.util.List;

/**
 *
 * @author fabri
 */
@Named("flywaySchemaHistory")
@SessionScoped
public class FlywaySchemaHistoryController implements Serializable {

    private static final long serialVersionUID = 1L;

    public FlywaySchemaHistoryController() {
        pagingInfo = new PagingInfo();
        converter = new FlywaySchemaHistoryConverter();
    }
    private FlywaySchemaHistory flywaySchemaHistory = null;
    private List<FlywaySchemaHistory> flywaySchemaHistoryItems = null;
    private transient FlywaySchemaHistoryJpaController jpaController = null;
    private FlywaySchemaHistoryConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getFlywaySchemaHistoryCount());
        }
        return pagingInfo;
    }

    public FlywaySchemaHistoryJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new FlywaySchemaHistoryJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getFlywaySchemaHistoryItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findFlywaySchemaHistoryEntities(), false);
    }

    public SelectItem[] getFlywaySchemaHistoryItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findFlywaySchemaHistoryEntities(), true);
    }

    public FlywaySchemaHistory getFlywaySchemaHistory() {
        if (flywaySchemaHistory == null) {
            flywaySchemaHistory = (FlywaySchemaHistory) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentFlywaySchemaHistory", converter, null);
        }
        if (flywaySchemaHistory == null) {
            flywaySchemaHistory = new FlywaySchemaHistory();
        }
        return flywaySchemaHistory;
    }

    public String listSetup() {
        reset(true);
        return "flywaySchemaHistory_list";
    }

    public String createSetup() {
        reset(false);
        flywaySchemaHistory = new FlywaySchemaHistory();
        return "flywaySchemaHistory_create";
    }

    public String create() {
        try {
            getJpaController().create(flywaySchemaHistory);
            JsfUtil.addSuccessMessage("FlywaySchemaHistory was successfully created.");
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("flywaySchemaHistory_detail");
    }

    public String editSetup() {
        return scalarSetup("flywaySchemaHistory_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        flywaySchemaHistory = (FlywaySchemaHistory) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentFlywaySchemaHistory", converter, null);
        if (flywaySchemaHistory == null) {
            String requestFlywaySchemaHistoryString = JsfUtil.getRequestParameter("jsfcrud.currentFlywaySchemaHistory");
            JsfUtil.addErrorMessage("The flywaySchemaHistory with id " + requestFlywaySchemaHistoryString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String flywaySchemaHistoryString = converter.getAsString(FacesContext.getCurrentInstance(), null, flywaySchemaHistory);
        String currentFlywaySchemaHistoryString = JsfUtil.getRequestParameter("jsfcrud.currentFlywaySchemaHistory");
        if (flywaySchemaHistoryString == null || flywaySchemaHistoryString.length() == 0 || !flywaySchemaHistoryString.equals(currentFlywaySchemaHistoryString)) {
            String outcome = editSetup();
            if ("flywaySchemaHistory_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit flywaySchemaHistory. Try again.");
            }
            return outcome;
        }
        try {
            getJpaController().edit(flywaySchemaHistory);
            JsfUtil.addSuccessMessage("FlywaySchemaHistory was successfully updated.");
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
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentFlywaySchemaHistory");
        Integer id = java.lang.Integer.valueOf(idAsString);
        try {
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("FlywaySchemaHistory was successfully deleted.");
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

    public List<FlywaySchemaHistory> getFlywaySchemaHistoryItems() {
        if (flywaySchemaHistoryItems == null) {
            getPagingInfo();
            flywaySchemaHistoryItems = getJpaController().findFlywaySchemaHistoryEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return flywaySchemaHistoryItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "flywaySchemaHistory_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "flywaySchemaHistory_list";
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
        flywaySchemaHistory = null;
        flywaySchemaHistoryItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        FlywaySchemaHistory newFlywaySchemaHistory = new FlywaySchemaHistory();
        String newFlywaySchemaHistoryString = converter.getAsString(FacesContext.getCurrentInstance(), null, newFlywaySchemaHistory);
        String flywaySchemaHistoryString = converter.getAsString(FacesContext.getCurrentInstance(), null, flywaySchemaHistory);
        if (!newFlywaySchemaHistoryString.equals(flywaySchemaHistoryString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }
    
}
