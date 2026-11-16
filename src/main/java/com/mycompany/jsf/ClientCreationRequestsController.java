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
import entities.ClientCreationRequests;
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
@Named("clientCreationRequests")
@SessionScoped
public class ClientCreationRequestsController implements Serializable {

    private static final long serialVersionUID = 1L;

    public ClientCreationRequestsController() {
        pagingInfo = new PagingInfo();
        converter = new ClientCreationRequestsConverter();
    }
    private ClientCreationRequests clientCreationRequests = null;
    private List<ClientCreationRequests> clientCreationRequestsItems = null;
    private transient ClientCreationRequestsJpaController jpaController = null;
    private ClientCreationRequestsConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getClientCreationRequestsCount());
        }
        return pagingInfo;
    }

    public ClientCreationRequestsJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new ClientCreationRequestsJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getClientCreationRequestsItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findClientCreationRequestsEntities(), false);
    }

    public SelectItem[] getClientCreationRequestsItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findClientCreationRequestsEntities(), true);
    }

    public ClientCreationRequests getClientCreationRequests() {
        if (clientCreationRequests == null) {
            clientCreationRequests = (ClientCreationRequests) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentClientCreationRequests", converter, null);
        }
        if (clientCreationRequests == null) {
            clientCreationRequests = new ClientCreationRequests();
        }
        return clientCreationRequests;
    }

    public String listSetup() {
        reset(true);
        return "clientCreationRequests_list";
    }

    public String createSetup() {
        reset(false);
        clientCreationRequests = new ClientCreationRequests();
        return "clientCreationRequests_create";
    }

    public String create() {
        try {
            getJpaController().create(clientCreationRequests);
            JsfUtil.addSuccessMessage("ClientCreationRequests was successfully created.");
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("clientCreationRequests_detail");
    }

    public String editSetup() {
        return scalarSetup("clientCreationRequests_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        clientCreationRequests = (ClientCreationRequests) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentClientCreationRequests", converter, null);
        if (clientCreationRequests == null) {
            String requestClientCreationRequestsString = JsfUtil.getRequestParameter("jsfcrud.currentClientCreationRequests");
            JsfUtil.addErrorMessage("The clientCreationRequests with id " + requestClientCreationRequestsString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String clientCreationRequestsString = converter.getAsString(FacesContext.getCurrentInstance(), null, clientCreationRequests);
        String currentClientCreationRequestsString = JsfUtil.getRequestParameter("jsfcrud.currentClientCreationRequests");
        if (clientCreationRequestsString == null || clientCreationRequestsString.length() == 0 || !clientCreationRequestsString.equals(currentClientCreationRequestsString)) {
            String outcome = editSetup();
            if ("clientCreationRequests_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit clientCreationRequests. Try again.");
            }
            return outcome;
        }
        try {
            getJpaController().edit(clientCreationRequests);
            JsfUtil.addSuccessMessage("ClientCreationRequests was successfully updated.");
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
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentClientCreationRequests");
        try {
            UUID id = UUID.fromString(idAsString);
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("ClientCreationRequests was successfully deleted.");
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

    public List<ClientCreationRequests> getClientCreationRequestsItems() {
        if (clientCreationRequestsItems == null) {
            getPagingInfo();
            clientCreationRequestsItems = getJpaController().findClientCreationRequestsEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return clientCreationRequestsItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "clientCreationRequests_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "clientCreationRequests_list";
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
        clientCreationRequests = null;
        clientCreationRequestsItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        ClientCreationRequests newClientCreationRequests = new ClientCreationRequests();
        String newClientCreationRequestsString = converter.getAsString(FacesContext.getCurrentInstance(), null, newClientCreationRequests);
        String clientCreationRequestsString = converter.getAsString(FacesContext.getCurrentInstance(), null, clientCreationRequests);
        if (!newClientCreationRequestsString.equals(clientCreationRequestsString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }
    
}
