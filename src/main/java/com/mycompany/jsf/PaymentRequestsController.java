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
import entities.PaymentRequests;
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
@Named("paymentRequests")
@SessionScoped
public class PaymentRequestsController implements Serializable {

    private static final long serialVersionUID = 1L;

    public PaymentRequestsController() {
        pagingInfo = new PagingInfo();
        converter = new PaymentRequestsConverter();
    }
    private PaymentRequests paymentRequests = null;
    private List<PaymentRequests> paymentRequestsItems = null;
    private transient PaymentRequestsJpaController jpaController = null;
    private PaymentRequestsConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getPaymentRequestsCount());
        }
        return pagingInfo;
    }

    public PaymentRequestsJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new PaymentRequestsJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getPaymentRequestsItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findPaymentRequestsEntities(), false);
    }

    public SelectItem[] getPaymentRequestsItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findPaymentRequestsEntities(), true);
    }

    public PaymentRequests getPaymentRequests() {
        if (paymentRequests == null) {
            paymentRequests = (PaymentRequests) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPaymentRequests", converter, null);
        }
        if (paymentRequests == null) {
            paymentRequests = new PaymentRequests();
        }
        return paymentRequests;
    }

    public String listSetup() {
        reset(true);
        return "paymentRequests_list";
    }

    public String createSetup() {
        reset(false);
        paymentRequests = new PaymentRequests();
        return "paymentRequests_create";
    }

    public String create() {
        try {
            getJpaController().create(paymentRequests);
            JsfUtil.addSuccessMessage("PaymentRequests was successfully created.");
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("paymentRequests_detail");
    }

    public String editSetup() {
        return scalarSetup("paymentRequests_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        paymentRequests = (PaymentRequests) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPaymentRequests", converter, null);
        if (paymentRequests != null && paymentRequests.getId() != null) {
            paymentRequests = getJpaController().findPaymentRequestsWithInstallments(paymentRequests.getId());
        }
        if (paymentRequests == null) {
            String requestPaymentRequestsString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentRequests");
            JsfUtil.addErrorMessage("The paymentRequests with id " + requestPaymentRequestsString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String paymentRequestsString = converter.getAsString(FacesContext.getCurrentInstance(), null, paymentRequests);
        String currentPaymentRequestsString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentRequests");
        if (paymentRequestsString == null || paymentRequestsString.length() == 0 || !paymentRequestsString.equals(currentPaymentRequestsString)) {
            String outcome = editSetup();
            if ("paymentRequests_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit paymentRequests. Try again.");
            }
            return outcome;
        }
        try {
            getJpaController().edit(paymentRequests);
            JsfUtil.addSuccessMessage("PaymentRequests was successfully updated.");
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
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentRequests");
        try {
            UUID id = UUID.fromString(idAsString);
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("PaymentRequests was successfully deleted.");
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

    public List<PaymentRequests> getPaymentRequestsItems() {
        if (paymentRequestsItems == null) {
            getPagingInfo();
            paymentRequestsItems = getJpaController().findPaymentRequestsEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return paymentRequestsItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "paymentRequests_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "paymentRequests_list";
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
        paymentRequests = null;
        paymentRequestsItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        PaymentRequests newPaymentRequests = new PaymentRequests();
        String newPaymentRequestsString = converter.getAsString(FacesContext.getCurrentInstance(), null, newPaymentRequests);
        String paymentRequestsString = converter.getAsString(FacesContext.getCurrentInstance(), null, paymentRequests);
        if (!newPaymentRequestsString.equals(paymentRequestsString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }
    
}
