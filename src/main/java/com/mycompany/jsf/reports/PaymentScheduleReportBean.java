package com.mycompany.jsf.reports;

import com.mycompany.jsf.security.AuthenticatedUser;
import com.mycompany.jsf.security.AuthenticationBean;
import com.mycompany.jsf.security.PaymentSubmissionStatus;
import com.mycompany.jsf.security.PaymentDisplayLabels;
import entities.Installments;
import entities.PaymentSubmissions;
import entities.PaymentSchedules;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

@Named("paymentScheduleReport")
@RequestScoped
public class PaymentScheduleReportBean {

    private static final Logger LOGGER = Logger.getLogger(PaymentScheduleReportBean.class.getName());
    private static final String REPORT_RESOURCE = "/reports/payment-schedule.jrxml";
    private static final String RECEIPT_RESOURCE = "/reports/payment-receipt.jrxml";

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager entityManager;
    @Inject
    private AuthenticationBean authenticationBean;

    public void downloadPdf() {
        download("pdf");
    }

    public void downloadExcel() {
        download("xlsx");
    }

    private void download(String format) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String scheduleId = facesContext.getExternalContext()
                .getRequestParameterMap().get("scheduleId");

        final UUID id;
        try {
            id = UUID.fromString(scheduleId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Identifiant d'échéancier invalide.", null));
            return;
        }

        try {
            PaymentSchedules schedule = findSchedule(id);
            if (schedule == null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, "L'échéancier demandé n'existe plus.", null));
                return;
            }
            requireScheduleAccess(schedule);

            byte[] content = generateReport(schedule, format);
            String extension = format.equals("pdf") ? "pdf" : "xlsx";
            String contentType = format.equals("pdf")
                    ? "application/pdf"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            var externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType(contentType);
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"echeancier_" + id + "." + extension + "\"");
            externalContext.setResponseContentLength(content.length);
            externalContext.getResponseOutputStream().write(content);
            externalContext.getResponseOutputStream().flush();
            facesContext.responseComplete();
        } catch (SecurityException ex) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Vous n’êtes pas autorisé à télécharger cet échéancier.", null));
        } catch (PersistenceException | JRException | IOException ex) {
            LOGGER.log(Level.SEVERE, "Impossible de générer le rapport de l'échéancier " + id, ex);
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "La génération du rapport a échoué. Consultez les journaux du serveur.", null));
        }
    }

    public void downloadReceiptPdf() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String submissionId = facesContext.getExternalContext()
                .getRequestParameterMap().get("submissionId");
        final UUID id;
        try {
            id = UUID.fromString(submissionId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Identifiant de paiement invalide.", null));
            return;
        }

        try {
            List<PaymentSubmissions> group = findApprovedReceiptGroup(id);
            if (group.isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Aucune quittance disponible pour ce paiement.", null));
                return;
            }
            PaymentSubmissions submission = group.get(0);
            PaymentSchedules schedule = submission.getInstallmentId().getScheduleId();
            requireScheduleAccess(schedule);
            byte[] content = generateReceipt(group, schedule);
            UUID receiptId = submission.getPaymentGroupId() == null
                    ? submission.getId() : submission.getPaymentGroupId();
            sendDownload(facesContext, content, "application/pdf",
                    "quittance_" + receiptId + ".pdf");
        } catch (SecurityException ex) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Vous n’êtes pas autorisé à télécharger cette quittance.", null));
        } catch (PersistenceException | JRException | IOException ex) {
            LOGGER.log(Level.SEVERE, "Impossible de générer la quittance du paiement " + id, ex);
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "La génération de la quittance a échoué. Consultez les journaux du serveur.", null));
        }
    }

    public void downloadPaymentProof() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String submissionId = facesContext.getExternalContext()
                .getRequestParameterMap().get("submissionId");
        final UUID id;
        try {
            id = UUID.fromString(submissionId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Identifiant de paiement invalide.", null));
            return;
        }
        try {
            PaymentSubmissions submission = findPaymentWithProof(id);
            if (submission == null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Aucun justificatif n’est disponible pour ce paiement.", null));
                return;
            }
            requireScheduleAccess(submission.getInstallmentId().getScheduleId());
            String filename = submission.getProofFileName().replaceAll("[\\r\\n\"]", "_");
            sendDownload(facesContext, submission.getProofData(),
                    submission.getProofContentType(), filename);
        } catch (SecurityException ex) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Vous n’êtes pas autorisé à consulter ce justificatif.", null));
        } catch (PersistenceException | IOException ex) {
            LOGGER.log(Level.SEVERE, "Impossible de télécharger le justificatif " + id, ex);
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Le téléchargement du justificatif a échoué.", null));
        }
    }

    private void sendDownload(FacesContext facesContext, byte[] content,
            String contentType, String filename) throws IOException {
        var externalContext = facesContext.getExternalContext();
        externalContext.responseReset();
        externalContext.setResponseContentType(contentType);
        externalContext.setResponseHeader(
                "Content-Disposition", "attachment; filename=\"" + filename + "\"");
        externalContext.setResponseHeader("X-Content-Type-Options", "nosniff");
        externalContext.setResponseContentLength(content.length);
        externalContext.getResponseOutputStream().write(content);
        externalContext.getResponseOutputStream().flush();
        facesContext.responseComplete();
    }

    private void requireScheduleAccess(PaymentSchedules schedule) {
        AuthenticatedUser user = authenticationBean.getUser();
        if (user == null) {
            throw new SecurityException("Une session authentifiée est nécessaire.");
        }
        if (user.isAdmin() || user.isManager()) {
            return;
        }
        if (user.isClient() && user.getClientId() != null
                && schedule.getClientId() != null
                && user.getClientId().equals(schedule.getClientId().getId())) {
            return;
        }
        throw new SecurityException("Accès interdit à cet échéancier.");
    }

    private PaymentSchedules findSchedule(UUID id) {
        List<PaymentSchedules> results = entityManager.createQuery(
                "SELECT DISTINCT s FROM PaymentSchedules s "
                + "LEFT JOIN FETCH s.installmentsList "
                + "LEFT JOIN FETCH s.clientId "
                + "LEFT JOIN FETCH s.managerId "
                + "WHERE s.id = :id", PaymentSchedules.class)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private List<PaymentSubmissions> findApprovedReceiptGroup(UUID id) {
        PaymentSubmissions selected = entityManager.find(PaymentSubmissions.class, id);
        if (selected == null) {
            return List.of();
        }
        UUID groupId = selected.getPaymentGroupId();
        List<PaymentSubmissions> results = entityManager.createQuery(
                "SELECT p FROM PaymentSubmissions p "
                + "JOIN FETCH p.installmentId "
                + "JOIN FETCH p.installmentId.scheduleId "
                + "JOIN FETCH p.installmentId.scheduleId.clientId "
                + "LEFT JOIN FETCH p.installmentId.scheduleId.managerId "
                + "LEFT JOIN FETCH p.reviewedBy "
                + (groupId == null ? "WHERE p.id = :id " : "WHERE p.paymentGroupId = :groupId ")
                + "ORDER BY p.installmentId.installmentNumber",
                PaymentSubmissions.class)
                .setParameter(groupId == null ? "id" : "groupId", groupId == null ? id : groupId)
                .getResultList();
        return results.isEmpty() || results.stream().anyMatch(payment
                -> !PaymentSubmissionStatus.APPROVED.equalsIgnoreCase(payment.getStatus()))
                ? List.of() : results;
    }

    private PaymentSubmissions findPaymentWithProof(UUID id) {
        List<PaymentSubmissions> results = entityManager.createQuery(
                "SELECT p FROM PaymentSubmissions p JOIN FETCH p.installmentId "
                + "JOIN FETCH p.installmentId.scheduleId JOIN FETCH p.installmentId.scheduleId.clientId "
                + "WHERE p.id = :id AND p.proofData IS NOT NULL",
                PaymentSubmissions.class)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private byte[] generateReport(PaymentSchedules schedule, String format)
            throws JRException, IOException {
        List<Installments> installments = schedule.getInstallmentsList() == null
                ? new ArrayList<>()
                : new ArrayList<>(schedule.getInstallmentsList());
        installments.sort(Comparator.comparingInt(Installments::getInstallmentNumber));
        List<Map<String, ?>> rows = new ArrayList<>();
        for (Installments installment : installments) {
            Map<String, Object> row = new HashMap<>();
            row.put("installmentNumber", installment.getInstallmentNumber());
            row.put("amount", installment.getAmount());
            row.put("dueDate", installment.getDueDate());
            row.put("paid", installment.getPaid());
            row.put("paidAt", installment.getPaidAt());
            row.put("channel", installment.getChannel());
            row.put("paymentReference", installment.getPaymentReference());
            rows.add(row);
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scheduleReference", schedule.getReference());
        parameters.put("scheduleTotal", schedule.getTotal());
        parameters.put("confirmationStatus", schedule.getConfirmationStatus());
        parameters.put("createdAt", schedule.getCreatedAt());
        parameters.put("clientName", schedule.getClientId().getName());
        parameters.put("clientEmail", schedule.getClientId().getEmail());
        parameters.put("clientPhone", schedule.getClientId().getPhone() == null
                ? "" : schedule.getClientId().getPhone());
        parameters.put("managerName", schedule.getManagerId() == null
                ? "" : schedule.getManagerId().getName());

        JasperReport report = compileReport(REPORT_RESOURCE);
        JasperPrint print = JasperFillManager.fillReport(
                report, parameters, new JRMapCollectionDataSource(rows));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (format.equals("pdf")) {
                net.sf.jasperreports.engine.JasperExportManager.exportReportToPdfStream(print, output);
            } else {
                JRXlsxExporter exporter = new JRXlsxExporter();
                exporter.setExporterInput(new SimpleExporterInput(print));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
                SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
                configuration.setOnePagePerSheet(false);
                configuration.setDetectCellType(true);
                exporter.setConfiguration(configuration);
                exporter.exportReport();
            }
            return output.toByteArray();
        }
    }

    private byte[] generateReceipt(List<PaymentSubmissions> payments, PaymentSchedules schedule)
            throws JRException, IOException {
        PaymentSubmissions submission = payments.get(0);
        StringBuilder allocationDetails = new StringBuilder();
        for (PaymentSubmissions payment : payments) {
            if (allocationDetails.length() > 0) {
                allocationDetails.append('\n');
            }
            allocationDetails.append("Échéance ")
                    .append(payment.getInstallmentId().getInstallmentNumber())
                    .append(" : ")
                    .append(NumberFormat.getIntegerInstance(Locale.FRANCE)
                            .format(payment.getAmount()));
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("receiptNumber", submission.getPaymentGroupId() == null
                ? submission.getId().toString() : submission.getPaymentGroupId().toString());
        parameters.put("clientName", schedule.getClientId().getName());
        parameters.put("clientEmail", schedule.getClientId().getEmail());
        parameters.put("scheduleReference", schedule.getReference());
        parameters.put("installmentNumber", submission.getInstallmentId().getInstallmentNumber());
        parameters.put("installmentAmount", submission.getAmount());
        parameters.put("amount", submission.getPaymentGroupAmount() == null
                ? payments.stream().mapToLong(PaymentSubmissions::getAmount).sum()
                : submission.getPaymentGroupAmount());
        parameters.put("creditAmount", submission.getCreditAmount());
        parameters.put("allocationDetails", allocationDetails.toString());
        parameters.put("channel", PaymentDisplayLabels.channel(submission.getChannel()));
        parameters.put("paymentReference", submission.getReference() == null
                ? "" : submission.getReference());
        parameters.put("paidAt", submission.getReviewedAt() == null
                ? submission.getSubmittedAt() : submission.getReviewedAt());
        parameters.put("paymentDate", submission.getPaymentDate() == null
                ? submission.getSubmittedAt() : submission.getPaymentDate());
        parameters.put("managerName", submission.getReviewedBy() != null
                ? submission.getReviewedBy().getName()
                : schedule.getManagerId() == null ? "" : schedule.getManagerId().getName());

        JasperPrint print = JasperFillManager.fillReport(
                compileReport(RECEIPT_RESOURCE), parameters, new JREmptyDataSource(1));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            net.sf.jasperreports.engine.JasperExportManager.exportReportToPdfStream(print, output);
            return output.toByteArray();
        }
    }

    private JasperReport compileReport(String resource) throws JRException, IOException {
        try (InputStream template = getClass().getResourceAsStream(resource)) {
            if (template == null) {
                throw new IOException("Template JasperReports introuvable : " + resource);
            }
            return JasperCompileManager.compileReport(template);
        }
    }
}
