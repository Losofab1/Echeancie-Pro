/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Inject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.faces.FacesException;
import com.mycompany.jsf.util.JsfUtil;
import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.IllegalOrphanException;
import com.mycompany.jsf.util.PagingInfo;
import entities.Clients;
import com.mycompany.jsf.security.ClientProvisioningService;
import jakarta.annotation.Resource;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.model.SelectItem;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.UserTransaction;
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.UUID;

/**
 *
 * @author fabri
 */
@Named("clients")
@SessionScoped
public class ClientsController implements Serializable {

    private static final long serialVersionUID = 1L;

    public ClientsController() {
        pagingInfo = new PagingInfo();
        converter = new ClientsConverter();
    }
    private Clients clients = null;
    private String newPassword;
    private List<Clients> clientsItems = null;
    private transient ClientsJpaController jpaController = null;
    private ClientsConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;
    @Inject
    private ClientProvisioningService clientProvisioningService;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getClientsCount());
        }
        return pagingInfo;
    }

    public ClientsJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new ClientsJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getClientsItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findClientsEntities(), false);
    }

    public SelectItem[] getClientsItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findClientsEntities(), true,
                client -> client.getName() + " — " + client.getEmail());
    }

    public Clients getClients() {
        if (clients == null) {
            clients = (Clients) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentClients", converter, null);
        }
        if (clients == null) {
            clients = new Clients();
        }
        return clients;
    }

    public String listSetup() {
        reset(true);
        return "clients_list";
    }

    public String createSetup() {
        reset(false);
        clients = new Clients();
        clients.setCreatedAt(new Date());
        newPassword = null;
        return "clients_create";
    }

    public String create() {
        char[] password = newPassword == null ? null : newPassword.toCharArray();
        try {
            clientProvisioningService.createClientWithAccount(clients, password);
            JsfUtil.addSuccessMessage(
                    "Le client et son compte de connexion actif ont été créés.");
        } catch (IllegalArgumentException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            newPassword = null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("clients_detail");
    }

    public String editSetup() {
        return scalarSetup("clients_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        clients = (Clients) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentClients", converter, null);
        if (clients == null) {
            String requestClientsString = JsfUtil.getRequestParameter("jsfcrud.currentClients");
            JsfUtil.addErrorMessage("The clients with id " + requestClientsString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String clientsString = converter.getAsString(FacesContext.getCurrentInstance(), null, clients);
        String currentClientsString = JsfUtil.getRequestParameter("jsfcrud.currentClients");
        if (clientsString == null || clientsString.length() == 0 || !clientsString.equals(currentClientsString)) {
            String outcome = editSetup();
            if ("clients_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit clients. Try again.");
            }
            return outcome;
        }
        try {
            getJpaController().edit(clients);
            JsfUtil.addSuccessMessage("Clients was successfully updated.");
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
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentClients");
        try {
            UUID id = UUID.fromString(idAsString);
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("Clients was successfully deleted.");
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

    public List<Clients> getClientsItems() {
        if (clientsItems == null) {
            getPagingInfo();
            clientsItems = getJpaController().findClientsEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return clientsItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "clients_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "clients_list";
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
        clients = null;
        newPassword = null;
        clientsItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        Clients newClients = new Clients();
        String newClientsString = converter.getAsString(FacesContext.getCurrentInstance(), null, newClients);
        String clientsString = converter.getAsString(FacesContext.getCurrentInstance(), null, clients);
        if (!newClientsString.equals(clientsString)) {
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
