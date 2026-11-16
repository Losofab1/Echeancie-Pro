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
import com.mycompany.jsf.security.AuthenticatedUser;
import com.mycompany.jsf.security.AuthenticationBean;
import com.mycompany.jsf.security.PaymentSubmissionStatus;
import entities.Users;
import jakarta.inject.Inject;
import entities.PaymentSubmissions;
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
import java.util.Locale;
import java.util.UUID;

/**
 *
 * @author fabri
 */
@Named("paymentSubmissions")
@SessionScoped
public class PaymentSubmissionsController implements Serializable {

    private static final long serialVersionUID = 1L;

    public PaymentSubmissionsController() {
        pagingInfo = new PagingInfo();
        converter = new PaymentSubmissionsConverter();
    }
    private PaymentSubmissions paymentSubmissions = null;
    private List<PaymentSubmissions> paymentSubmissionsItems = null;
    private transient PaymentSubmissionsJpaController jpaController = null;
    private PaymentSubmissionsConverter converter = null;
    private PagingInfo pagingInfo = null;
    private boolean managerReviewMode;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "my_persistence_unit")
    private EntityManagerFactory emf = null;
    @Inject
    private AuthenticationBean authenticationBean;

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().getPaymentSubmissionsCount());
        }
        return pagingInfo;
    }

    public PaymentSubmissionsJpaController getJpaController() {
        if (jpaController == null) {
            jpaController = new PaymentSubmissionsJpaController(utx, emf);
        }
        return jpaController;
    }

    public SelectItem[] getPaymentSubmissionsItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findPaymentSubmissionsEntities(), false);
    }

    public SelectItem[] getPaymentSubmissionsItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findPaymentSubmissionsEntities(), true);
    }

    public PaymentSubmissions getPaymentSubmissions() {
        if (paymentSubmissions == null) {
            paymentSubmissions = (PaymentSubmissions) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPaymentSubmissions", converter, null);
        }
        if (paymentSubmissions == null) {
            paymentSubmissions = new PaymentSubmissions();
        }
        return paymentSubmissions;
    }

    public String listSetup() {
        reset(true);
        return "paymentSubmissions_list";
    }

    public String createSetup() {
        reset(false);
        paymentSubmissions = new PaymentSubmissions();
        return "paymentSubmissions_create";
    }

    public String create() {
        try {
            getJpaController().create(paymentSubmissions);
            JsfUtil.addSuccessMessage("PaymentSubmissions was successfully created.");
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("paymentSubmissions_detail");
    }

    public String editSetup() {
        return scalarSetup("paymentSubmissions_edit");
    }

    public String reviewSetup(UUID submissionId) {
        managerReviewMode = false;
        AuthenticatedUser reviewer = authenticationBean.getUser();
        if (reviewer == null || (!reviewer.isAdmin() && !reviewer.isManager())) {
            JsfUtil.addErrorMessage("Seul un membre de l’équipe peut traiter un paiement.");
            return null;
        }
        paymentSubmissions = submissionId == null
                ? null : getJpaController().findPaymentSubmissions(submissionId);
        if (paymentSubmissions == null) {
            JsfUtil.addErrorMessage("Le paiement demandé n’existe plus.");
            return listSetup();
        }
        if (!PaymentSubmissionStatus.PENDING.equalsIgnoreCase(paymentSubmissions.getStatus())) {
            JsfUtil.addErrorMessage("Ce paiement a déjà été traité.");
            return listSetup();
        }
        if (paymentSubmissions.getPaymentGroupId() != null) {
            JsfUtil.addErrorMessage(
                    "Ce paiement réparti doit être traité depuis le tableau de bord gestionnaire.");
            return listSetup();
        }
        managerReviewMode = true;
        return "paymentSubmissions_edit";
    }

    private String scalarSetup(String destination) {
        reset(false);
        paymentSubmissions = (PaymentSubmissions) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPaymentSubmissions", converter, null);
        if (paymentSubmissions == null) {
            String requestPaymentSubmissionsString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentSubmissions");
            JsfUtil.addErrorMessage("The paymentSubmissions with id " + requestPaymentSubmissionsString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String paymentSubmissionsString = converter.getAsString(FacesContext.getCurrentInstance(), null, paymentSubmissions);
        String currentPaymentSubmissionsString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentSubmissions");
        if (paymentSubmissionsString == null || paymentSubmissionsString.length() == 0 || !paymentSubmissionsString.equals(currentPaymentSubmissionsString)) {
            String outcome = editSetup();
            if ("paymentSubmissions_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit paymentSubmissions. Try again.");
            }
            return outcome;
        }
        try {
            String status = paymentSubmissions.getStatus() == null
                    ? "" : paymentSubmissions.getStatus().trim().toUpperCase(Locale.ROOT);
            paymentSubmissions.setStatus(status);
            if (PaymentSubmissionStatus.PENDING.equals(status)) {
                paymentSubmissions.setReviewedAt(null);
                paymentSubmissions.setReviewedBy(null);
            } else {
                AuthenticatedUser reviewer = authenticationBean.getUser();
                if (reviewer == null || reviewer.isClient()) {
                    JsfUtil.addErrorMessage("Seul un membre de l’équipe peut traiter un paiement.");
                    return null;
                }
                paymentSubmissions.setReviewedAt(new Date());
                paymentSubmissions.setReviewedBy(new Users(reviewer.getId()));
            }
            getJpaController().edit(paymentSubmissions);
            JsfUtil.addSuccessMessage("Le paiement a été mis à jour.");
        } catch (NonexistentEntityException ne) {
            JsfUtil.addErrorMessage(ne.getLocalizedMessage());
            return listSetup();
        } catch (IllegalArgumentException | IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        } catch (Exception e) {
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        if (managerReviewMode) {
            FacesContext.getCurrentInstance().getExternalContext()
                    .getFlash().setKeepMessages(true);
            reset(false);
            return "/manager/dashboard.xhtml?faces-redirect=true";
        }
        return detailSetup();
    }

    public String destroy() {
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentPaymentSubmissions");
        try {
            UUID id = UUID.fromString(idAsString);
            getJpaController().destroy(id);
            JsfUtil.addSuccessMessage("PaymentSubmissions was successfully deleted.");
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

    public List<PaymentSubmissions> getPaymentSubmissionsItems() {
        if (paymentSubmissionsItems == null) {
            getPagingInfo();
            paymentSubmissionsItems = getJpaController().findPaymentSubmissionsEntities(pagingInfo.getBatchSize(), pagingInfo.getFirstItem());
        }
        return paymentSubmissionsItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "paymentSubmissions_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "paymentSubmissions_list";
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
        paymentSubmissions = null;
        paymentSubmissionsItems = null;
        managerReviewMode = false;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        PaymentSubmissions newPaymentSubmissions = new PaymentSubmissions();
        String newPaymentSubmissionsString = converter.getAsString(FacesContext.getCurrentInstance(), null, newPaymentSubmissions);
        String paymentSubmissionsString = converter.getAsString(FacesContext.getCurrentInstance(), null, paymentSubmissions);
        if (!newPaymentSubmissionsString.equals(paymentSubmissionsString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }

    public boolean isManagerReviewMode() {
        return managerReviewMode;
    }
    
}
