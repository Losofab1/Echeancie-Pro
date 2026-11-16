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
import entities.PaymentSchedules;
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
@Named("paymentSchedules")
@SessionScoped
public class PaymentSchedulesController implements Serializable {

    private static final long serialVersionUID = 1L;

    public PaymentSchedulesController() {
        pagingInfo = new PagingInfo();
        converter = new PaymentSchedulesConverter();
    }
    private PaymentSchedules paymentSchedules = null;
    private List<PaymentSchedules> paymentSchedulesItems = null;
    private transient PaymentSchedulesJpaController jpaController = null;
    private PaymentSchedulesConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getPaymentSchedulesCount());
        }
        return pagingInfo;
    }

    public PaymentSchedulesJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new PaymentSchedulesJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getPaymentSchedulesItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findPaymentSchedulesEntities(), false);
    }

    public SelectItem[] getPaymentSchedulesItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findPaymentSchedulesEntities(), true,
                schedule -> schedule.getReference() + " — " + schedule.getTotal()
                + " (" + schedule.getConfirmationStatus() + ")");
    }

    public PaymentSchedules getPaymentSchedules() {
        if (paymentSchedules == null) {
            paymentSchedules = (PaymentSchedules) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPaymentSchedules", converter, null);
        }
        if (paymentSchedules == null) {
            paymentSchedules = new PaymentSchedules();
        }
        return paymentSchedules;
    }

    public String listSetup() {
        reset(true);
        return "paymentSchedules_list";
    }

    public String createSetup() {
        reset(false);
        paymentSchedules = new PaymentSchedules();
        return "paymentSchedules_create";
    }

    public String create() {
        try {
            getJpaController().create(paymentSchedules);
            JsfUtil.addSuccessMessage("PaymentSchedules was successfully created.");
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("paymentSchedules_detail");
    }

    public String editSetup() {
        return scalarSetup("paymentSchedules_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        paymentSchedules = (PaymentSchedules) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPaymentSchedules", converter, null);
        if (paymentSchedules != null && paymentSchedules.getId() != null) {
            paymentSchedules = getJpaController().findPaymentSchedulesWithInstallments(paymentSchedules.getId());
        }
        if (paymentSchedules == null) {
            String requestPaymentSchedulesString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentSchedules");
            JsfUtil.addErrorMessage("The paymentSchedules with id " + requestPaymentSchedulesString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String paymentSchedulesString = converter.getAsString(FacesContext.getCurrentInstance(), null, paymentSchedules);
        String currentPaymentSchedulesString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentSchedules");
        if (paymentSchedulesString == null || paymentSchedulesString.length() == 0 || !paymentSchedulesString.equals(currentPaymentSchedulesString)) {
            String outcome = editSetup();
            if ("paymentSchedules_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit paymentSchedules. Try again.");
            }
            return outcome;
        }
        try {
            getJpaController().edit(paymentSchedules);
            JsfUtil.addSuccessMessage("PaymentSchedules was successfully updated.");
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
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentSchedules");
        try {
            UUID id = UUID.fromString(idAsString);
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("PaymentSchedules was successfully deleted.");
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

    public List<PaymentSchedules> getPaymentSchedulesItems() {
        if (paymentSchedulesItems == null) {
            getPagingInfo();
            paymentSchedulesItems = getJpaController().findPaymentSchedulesEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return paymentSchedulesItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "paymentSchedules_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "paymentSchedules_list";
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
        paymentSchedules = null;
        paymentSchedulesItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        PaymentSchedules newPaymentSchedules = new PaymentSchedules();
        String newPaymentSchedulesString = converter.getAsString(FacesContext.getCurrentInstance(), null, newPaymentSchedules);
        String paymentSchedulesString = converter.getAsString(FacesContext.getCurrentInstance(), null, paymentSchedules);
        if (!newPaymentSchedulesString.equals(paymentSchedulesString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }
    
}
