package com.mycompany.jsf;

import com.mycompany.jsf.security.AuthenticatedUser;
import com.mycompany.jsf.security.AuthenticationBean;
import com.mycompany.jsf.security.ClientPortalService;
import com.mycompany.jsf.security.ClientPortalService.ClientDashboard;
import com.mycompany.jsf.security.ClientPortalService.InstallmentSummary;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("clientPortal")
@ViewScoped
public class ClientPortalBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ClientPortalBean.class.getName());

    @Inject
    private AuthenticationBean authenticationBean;
    @Inject
    private ClientPortalService clientPortalService;

    private ClientDashboard dashboard;
    private long requestTotal;
    private int requestInstallmentCount = 1;
    private List<ProposedInstallment> proposedInstallments = new ArrayList<>();
    private String requestNote;
    private String installmentId;
    private long paymentAmount;
    private LocalDate paymentDate = LocalDate.now();
    private String paymentChannel;
    private String paymentReference;
    private String paymentNote;
    private transient Part paymentProof;

    @PostConstruct
    public void initialize() {
        resizeProposedInstallments();
        reloadDashboard();
    }

    public String createPaymentRequest() {
        try {
            clientPortalService.createPaymentRequest(
                    currentUserId(), requestTotal, requestInstallmentCount,
                    getProposedDueDates(), requestNote);
            requestTotal = 0;
            requestInstallmentCount = 1;
            proposedInstallments.clear();
            resizeProposedInstallments();
            requestNote = null;
            addMessage(FacesMessage.SEVERITY_INFO,
                    "Votre demande a été transmise pour examen.", null);
            return redirectWithMessages();
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null);
            return null;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to submit a client payment request.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "La demande n’a pas pu être enregistrée. Réessayez plus tard.", null);
            return null;
        }
    }

    public void updateProposedInstallments(AjaxBehaviorEvent event) {
        resizeProposedInstallments();
    }

    private void resizeProposedInstallments() {
        while (proposedInstallments.size() < requestInstallmentCount) {
            proposedInstallments.add(new ProposedInstallment(
                    proposedInstallments.size() + 1));
        }
        while (proposedInstallments.size() > requestInstallmentCount) {
            proposedInstallments.remove(proposedInstallments.size() - 1);
        }
        for (int index = 0; index < proposedInstallments.size(); index++) {
            proposedInstallments.get(index).setNumber(index + 1);
        }
    }

    private List<LocalDate> getProposedDueDates() {
        return proposedInstallments.stream()
                .map(ProposedInstallment::getDueDate)
                .toList();
    }

    public String submitPayment() {
        try {
            UUID selectedId = installmentId == null || installmentId.isBlank()
                    ? null : UUID.fromString(installmentId);
            clientPortalService.submitPayment(
                    currentUserId(), selectedId, paymentAmount, paymentDate,
                    paymentChannel, paymentReference, paymentNote, paymentProof);
            paymentAmount = 0;
            paymentDate = LocalDate.now();
            installmentId = null;
            paymentChannel = null;
            paymentReference = null;
            paymentNote = null;
            paymentProof = null;
            addMessage(FacesMessage.SEVERITY_INFO,
                    "Votre paiement a été soumis pour validation.", null);
            return redirectWithMessages();
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null);
            return null;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to submit a client payment.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Le paiement n’a pas pu être enregistré. Réessayez plus tard.", null);
            return null;
        }
    }

    private void reloadDashboard() {
        AuthenticatedUser user = authenticationBean.getUser();
        if (user == null || !user.isClient()) {
            dashboard = null;
            return;
        }
        try {
            dashboard = clientPortalService.loadDashboard(user.getId());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to load the client portal.", e);
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Le portail client est momentanément indisponible.", null);
        }
    }

    private UUID currentUserId() {
        AuthenticatedUser user = authenticationBean.getUser();
        if (user == null || !user.isClient()) {
            throw new SecurityException("Une session client est nécessaire.");
        }
        return user.getId();
    }

    private String redirectWithMessages() {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getFlash().setKeepMessages(true);
        return context.getViewRoot().getViewId() + "?faces-redirect=true";
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }

    public SelectItem[] getInstallmentSelectItems() {
        List<InstallmentSummary> installments = dashboard == null
                ? List.of() : dashboard.getOpenInstallments();
        return installments.stream()
                .map(item -> new SelectItem(item.getId().toString(),
                        item.getScheduleReference() + " — échéance " + item.getNumber()
                        + " — solde " + item.getRemaining()))
                .toArray(SelectItem[]::new);
    }

    public ClientDashboard getDashboard() { return dashboard; }
    public long getRequestTotal() { return requestTotal; }
    public void setRequestTotal(long requestTotal) { this.requestTotal = requestTotal; }
    public int getRequestInstallmentCount() { return requestInstallmentCount; }
    public void setRequestInstallmentCount(int requestInstallmentCount) {
        this.requestInstallmentCount = requestInstallmentCount;
    }
    public String getRequestNote() { return requestNote; }
    public void setRequestNote(String requestNote) { this.requestNote = requestNote; }
    public List<ProposedInstallment> getProposedInstallments() {
        return proposedInstallments;
    }
    public int getMaximumInstallmentCount() {
        return ClientPortalService.MAX_INSTALLMENT_COUNT;
    }
    public String getMinimumProposedDate() {
        return LocalDate.now().toString();
    }
    public String getMaximumPaymentDate() {
        return LocalDate.now().toString();
    }
    public String getInstallmentId() { return installmentId; }
    public void setInstallmentId(String installmentId) { this.installmentId = installmentId; }
    public long getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(long paymentAmount) { this.paymentAmount = paymentAmount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getPaymentNote() { return paymentNote; }
    public void setPaymentNote(String paymentNote) { this.paymentNote = paymentNote; }
    public Part getPaymentProof() { return paymentProof; }
    public void setPaymentProof(Part paymentProof) { this.paymentProof = paymentProof; }

    public static final class ProposedInstallment implements Serializable {

        private static final long serialVersionUID = 1L;
        private int number;
        private LocalDate dueDate;

        private ProposedInstallment(int number) {
            this.number = number;
        }

        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    }
}
