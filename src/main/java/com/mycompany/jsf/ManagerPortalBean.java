package com.mycompany.jsf;

import com.mycompany.jsf.security.AuthenticatedUser;
import com.mycompany.jsf.security.AuthenticationBean;
import com.mycompany.jsf.security.ManagerPortalService;
import com.mycompany.jsf.security.ManagerPortalService.ManagerDashboard;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.model.SelectItem;

@Named("managerPortal")
@ViewScoped
public class ManagerPortalBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ManagerPortalBean.class.getName());

    @Inject
    private AuthenticationBean authenticationBean;
    @Inject
    private ManagerPortalService managerPortalService;

    private ManagerDashboard dashboard;
    private String agencyInstallmentId;
    private long agencyPaymentAmount;
    private LocalDate agencyPaymentDate = LocalDate.now();
    private String agencyPaymentChannel = "CASH";
    private String agencyPaymentReference;
    private String agencyPaymentNote;

    @PostConstruct
    public void initialize() {
        reloadDashboard();
    }

    public String approveRequest(UUID requestId, long agreedTotal) {
        return reviewRequest(requestId, true, agreedTotal);
    }

    public String rejectRequest(UUID requestId) {
        return reviewRequest(requestId, false, 0);
    }

    public String approvePayment(UUID groupId) {
        return reviewPayment(groupId, true, null);
    }

    public String rejectPayment(UUID groupId, String reason) {
        return reviewPayment(groupId, false, reason);
    }

    private String reviewPayment(UUID groupId, boolean approved, String reason) {
        try {
            managerPortalService.reviewPaymentGroup(
                    currentStaffId(), groupId, approved, reason);
            addMessage(FacesMessage.SEVERITY_INFO,
                    approved ? "Le paiement a été validé." : "Le paiement a été rejeté.");
            return redirectWithMessages();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
            return null;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to review a payment transaction.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Le paiement n’a pas pu être traité. Réessayez plus tard.");
            return null;
        }
    }

    public String recordAgencyPayment() {
        try {
            UUID selectedId = agencyInstallmentId == null || agencyInstallmentId.isBlank()
                    ? null : UUID.fromString(agencyInstallmentId);
            managerPortalService.recordAgencyPayment(
                    currentStaffId(), selectedId, agencyPaymentAmount, agencyPaymentDate,
                    agencyPaymentChannel, agencyPaymentReference, agencyPaymentNote);
            agencyInstallmentId = null;
            agencyPaymentAmount = 0;
            agencyPaymentDate = LocalDate.now();
            agencyPaymentChannel = "CASH";
            agencyPaymentReference = null;
            agencyPaymentNote = null;
            addMessage(FacesMessage.SEVERITY_INFO,
                    "Le paiement encaissé en agence a été enregistré et validé.");
            return redirectWithMessages();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
            return null;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to record an agency payment.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Le paiement n’a pas pu être enregistré. Réessayez plus tard.");
            return null;
        }
    }

    private String reviewRequest(UUID requestId, boolean approved, long agreedTotal) {
        try {
            managerPortalService.reviewPaymentRequest(
                    currentStaffId(), requestId, approved, agreedTotal);
            addMessage(FacesMessage.SEVERITY_INFO,
                    approved
                            ? "La demande a été approuvée et l’échéancier a été créé."
                            : "La demande a été rejetée.");
            return redirectWithMessages();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
            return null;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to review a client payment request.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "La demande n’a pas pu être traitée. Réessayez plus tard.");
            return null;
        }
    }

    private void reloadDashboard() {
        try {
            dashboard = managerPortalService.loadDashboard(currentStaffId());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load the manager portal.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "La file de traitement est momentanément indisponible.");
        }
    }

    private UUID currentStaffId() {
        AuthenticatedUser user = authenticationBean.getUser();
        if (user == null || user.isClient()
                || (!user.isAdmin() && !user.isManager())) {
            throw new SecurityException("Un compte gestionnaire actif est nécessaire.");
        }
        return user.getId();
    }

    private String redirectWithMessages() {
        FacesContext.getCurrentInstance().getExternalContext()
                .getFlash().setKeepMessages(true);
        return "/manager/dashboard.xhtml?faces-redirect=true";
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, null));
    }

    public ManagerDashboard getDashboard() {
        return dashboard;
    }

    public SelectItem[] getAgencyInstallmentSelectItems() {
        List<ManagerPortalService.AgencyInstallmentOption> installments = dashboard == null
                ? List.of() : dashboard.getAgencyInstallments();
        return installments.stream()
                .map(item -> new SelectItem(item.getId().toString(), item.getLabel()))
                .toArray(SelectItem[]::new);
    }

    public String getAgencyInstallmentId() { return agencyInstallmentId; }
    public void setAgencyInstallmentId(String agencyInstallmentId) {
        this.agencyInstallmentId = agencyInstallmentId;
    }
    public long getAgencyPaymentAmount() { return agencyPaymentAmount; }
    public void setAgencyPaymentAmount(long agencyPaymentAmount) {
        this.agencyPaymentAmount = agencyPaymentAmount;
    }
    public LocalDate getAgencyPaymentDate() { return agencyPaymentDate; }
    public void setAgencyPaymentDate(LocalDate agencyPaymentDate) {
        this.agencyPaymentDate = agencyPaymentDate;
    }
    public String getAgencyPaymentChannel() { return agencyPaymentChannel; }
    public void setAgencyPaymentChannel(String agencyPaymentChannel) {
        this.agencyPaymentChannel = agencyPaymentChannel;
    }
    public String getAgencyPaymentReference() { return agencyPaymentReference; }
    public void setAgencyPaymentReference(String agencyPaymentReference) {
        this.agencyPaymentReference = agencyPaymentReference;
    }
    public String getAgencyPaymentNote() { return agencyPaymentNote; }
    public void setAgencyPaymentNote(String agencyPaymentNote) {
        this.agencyPaymentNote = agencyPaymentNote;
    }
    public String getMaximumPaymentDate() { return LocalDate.now().toString(); }
}
