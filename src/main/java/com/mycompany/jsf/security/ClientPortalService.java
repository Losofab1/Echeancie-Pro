package com.mycompany.jsf.security;

import entities.Installments;
import entities.PaymentRequestInstallments;
import entities.PaymentRequests;
import entities.PaymentSubmissions;
import entities.Users;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.servlet.http.Part;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;

@ApplicationScoped
public class ClientPortalService {

    public static final int MAX_INSTALLMENT_COUNT = Short.MAX_VALUE;

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager entityManager;
    @Inject
    private PaymentTransactionService paymentTransactionService;

    @Transactional
    public ClientDashboard loadDashboard(UUID userId) {
        Users user = requireClient(userId);
        UUID clientId = user.getClientId().getId();
        List<PaymentRequests> requests = entityManager.createQuery(
                "SELECT DISTINCT p FROM PaymentRequests p "
                + "LEFT JOIN FETCH p.paymentRequestInstallmentsList "
                + "WHERE p.clientId.id = :clientId "
                + "ORDER BY p.createdAt DESC", PaymentRequests.class)
                .setParameter("clientId", clientId)
                .getResultList();
        return new ClientDashboard(
                user.getClientId().getCreditBalance(),
                requests,
                entityManager.createQuery(
                        "SELECT s.id, s.reference, s.total, s.confirmationStatus, s.createdAt "
                        + "FROM PaymentSchedules s WHERE s.clientId.id = :clientId "
                        + "ORDER BY s.createdAt DESC", Object[].class)
                        .setParameter("clientId", clientId)
                        .getResultList(),
                entityManager.createQuery(
                        "SELECT i.id, s.reference, i.installmentNumber, i.amount, i.dueDate, "
                        + "i.paid, COALESCE(SUM(CASE WHEN UPPER(p.status) IN :reservedStatuses "
                        + "THEN p.amount ELSE 0 END), 0) "
                        + "FROM Installments i JOIN i.scheduleId s "
                        + "LEFT JOIN i.paymentSubmissionsList p "
                        + "WHERE s.clientId.id = :clientId "
                        + "GROUP BY i.id, s.reference, i.installmentNumber, i.amount, i.dueDate, i.paid "
                        + "ORDER BY i.dueDate, i.installmentNumber", Object[].class)
                        .setParameter("clientId", clientId)
                        .setParameter("reservedStatuses", Arrays.asList(
                                PaymentSubmissionStatus.PENDING,
                                PaymentSubmissionStatus.APPROVED))
                        .getResultList(),
                entityManager.createQuery(
                        "SELECT p.id, s.reference, i.installmentNumber, p.amount, p.channel, "
                        + "p.reference, p.status, p.submittedAt, p.reason, p.reviewedAt "
                        + ", p.paymentGroupId, p.paymentGroupAmount, p.creditAmount, "
                        + "p.paymentDate, p.proofFileName "
                        + "FROM PaymentSubmissions p JOIN p.installmentId i "
                        + "JOIN i.scheduleId s WHERE s.clientId.id = :clientId "
                        + "ORDER BY p.submittedAt DESC, i.installmentNumber", Object[].class)
                        .setParameter("clientId", clientId)
                        .getResultList());
    }

    @Transactional
    public void createPaymentRequest(UUID userId, long total, int installmentCount,
            List<LocalDate> proposedDueDates, String note) {
        Users user = requireClient(userId);
        if (total <= 0) {
            throw new IllegalArgumentException("Le montant demandé doit être supérieur à zéro.");
        }
        if (installmentCount < 1 || installmentCount > MAX_INSTALLMENT_COUNT) {
            throw new IllegalArgumentException("Le nombre d’échéances est invalide.");
        }
        Date createdAt = new Date();
        LocalDate requestDate = createdAt.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        validateProposedDueDates(proposedDueDates, installmentCount, requestDate);
        if (total < installmentCount) {
            throw new IllegalArgumentException(
                    "Le montant doit être au moins égal au nombre d’échéances.");
        }
        String normalizedNote = note == null ? null : note.trim();
        if (normalizedNote != null && normalizedNote.length() > 300) {
            throw new IllegalArgumentException("La note ne peut pas dépasser 300 caractères.");
        }

        PaymentRequests request = new PaymentRequests();
        request.setTotal(total);
        request.setInstallmentCount((short) installmentCount);
        request.setStatus("PENDING");
        request.setNote(normalizedNote == null || normalizedNote.isEmpty() ? null : normalizedNote);
        request.setChannel("CLIENT_PORTAL");
        request.setCreatedAt(createdAt);
        request.setClientId(user.getClientId());
        request.setRequesterId(user);
        List<PaymentRequestInstallments> proposedInstallments = new ArrayList<>(installmentCount);
        for (int index = 0; index < proposedDueDates.size(); index++) {
            PaymentRequestInstallments proposed = new PaymentRequestInstallments();
            proposed.setInstallmentNumber((short) (index + 1));
            proposed.setDueDate(java.sql.Date.valueOf(proposedDueDates.get(index)));
            proposed.setRequestId(request);
            proposedInstallments.add(proposed);
        }
        request.setPaymentRequestInstallmentsList(proposedInstallments);
        entityManager.persist(request);
    }

    static void validateProposedDueDates(List<LocalDate> dueDates, int count, LocalDate today) {
        if (dueDates == null || dueDates.size() != count) {
            throw new IllegalArgumentException(
                    "Une date doit être choisie pour chaque échéance.");
        }
        if (count < 1 || count > MAX_INSTALLMENT_COUNT) {
            throw new IllegalArgumentException("Le nombre d’échéances est invalide.");
        }
        LocalDate earliestFirstDate = today.plusMonths(1);
        LocalDate latestFirstDate = today.plusYears(1);
        LocalDate previousDate = null;
        for (LocalDate dueDate : dueDates) {
            if (dueDate == null) {
                throw new IllegalArgumentException(
                        "Toutes les dates d’échéance doivent être renseignées.");
            }
            if (previousDate == null) {
                if (dueDate.isBefore(earliestFirstDate) || dueDate.isAfter(latestFirstDate)) {
                    throw new IllegalArgumentException(
                            "La première échéance doit être prévue entre un mois et un an après la demande.");
                }
            } else if (dueDate.isBefore(previousDate.plusMonths(1))
                    || dueDate.isAfter(previousDate.plusYears(1))) {
                throw new IllegalArgumentException(
                        "L’intervalle entre deux échéances doit être compris entre un mois et un an.");
            }
            previousDate = dueDate;
        }
    }

    @Transactional
    public void submitPayment(UUID userId, UUID installmentId, long amount,
            LocalDate paymentDate, String channel, String reference, String note, Part proof) {
        paymentTransactionService.submitClientPayment(
                userId, installmentId, amount, paymentDate, channel, reference, note, proof);
    }

    private Users requireClient(UUID userId) {
        Users user = userId == null ? null : entityManager.find(Users.class, userId);
        if (user == null || !user.getActive()
                || !RoleNames.CLIENT.equals(RoleNames.normalize(user.getRole()))
                || user.getClientId() == null) {
            throw new SecurityException("Un compte client actif est nécessaire.");
        }
        return user;
    }

    public static final class ClientDashboard implements Serializable {

        private static final long serialVersionUID = 1L;
        private final List<RequestSummary> requests = new ArrayList<>();
        private final List<ScheduleSummary> schedules = new ArrayList<>();
        private final List<InstallmentSummary> installments = new ArrayList<>();
        private final List<SubmissionSummary> submissions = new ArrayList<>();
        private final long creditBalance;

        private ClientDashboard(long creditBalance, List<PaymentRequests> requestRows,
                List<Object[]> scheduleRows,
                List<Object[]> installmentRows, List<Object[]> submissionRows) {
            this.creditBalance = creditBalance;
            requestRows.forEach(request -> requests.add(new RequestSummary(request)));
            scheduleRows.forEach(row -> schedules.add(new ScheduleSummary(row)));
            installmentRows.forEach(row -> {
                InstallmentSummary installment = new InstallmentSummary(row);
                installments.add(installment);
            });
            Map<UUID, List<Object[]>> groupedSubmissions = new LinkedHashMap<>();
            submissionRows.forEach(row -> {
                UUID groupId = row[10] == null ? (UUID) row[0] : (UUID) row[10];
                groupedSubmissions.computeIfAbsent(groupId, key -> new ArrayList<>()).add(row);
            });
            groupedSubmissions.values().forEach(
                    rows -> submissions.add(new SubmissionSummary(rows)));
        }

        public long getCreditBalance() { return creditBalance; }
        public List<RequestSummary> getRequests() {
            return requests;
        }

        public List<ScheduleSummary> getSchedules() {
            return schedules;
        }

        public List<InstallmentSummary> getInstallments() {
            return installments;
        }

        public List<SubmissionSummary> getSubmissions() {
            return submissions;
        }

        public List<InstallmentSummary> getOpenInstallments() {
            return installments.stream()
                    .filter(item -> !item.isPaid() && item.getRemaining() > 0)
                    .toList();
        }
    }

    public static final class RequestSummary implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private final long total;
        private final short installmentCount;
        private final String status;
        private final Date createdAt;
        private final Long agreedTotal;
        private final Date reviewedAt;
        private final String scheduleReference;
        private final List<Date> proposedDueDates;

        private RequestSummary(PaymentRequests request) {
            id = request.getId();
            total = request.getTotal();
            installmentCount = request.getInstallmentCount();
            status = request.getStatus();
            createdAt = request.getCreatedAt();
            agreedTotal = request.getAgreedTotal();
            reviewedAt = request.getReviewedAt();
            scheduleReference = request.getScheduleId() == null
                    ? null : request.getScheduleId().getReference();
            List<PaymentRequestInstallments> proposed =
                    request.getPaymentRequestInstallmentsList();
            proposedDueDates = proposed == null ? List.of() : proposed.stream()
                    .sorted(Comparator.comparingInt(
                            PaymentRequestInstallments::getInstallmentNumber))
                    .map(PaymentRequestInstallments::getDueDate)
                    .toList();
        }

        public UUID getId() { return id; }
        public long getTotal() { return total; }
        public short getInstallmentCount() { return installmentCount; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return PaymentDisplayLabels.status(status); }
        public Date getCreatedAt() { return createdAt; }
        public Long getAgreedTotal() { return agreedTotal; }
        public Date getReviewedAt() { return reviewedAt; }
        public String getScheduleReference() { return scheduleReference; }
        public List<Date> getProposedDueDates() { return proposedDueDates; }
    }

    public static final class ScheduleSummary implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private final String reference;
        private final long total;
        private final String status;
        private final Date createdAt;

        private ScheduleSummary(Object[] row) {
            id = (UUID) row[0];
            reference = (String) row[1];
            total = ((Number) row[2]).longValue();
            status = (String) row[3];
            createdAt = (Date) row[4];
        }

        public UUID getId() { return id; }
        public String getReference() { return reference; }
        public long getTotal() { return total; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return PaymentDisplayLabels.confirmationStatus(status); }
        public Date getCreatedAt() { return createdAt; }
    }

    public static final class InstallmentSummary implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private final String scheduleReference;
        private final short number;
        private final long amount;
        private final Date dueDate;
        private final boolean paid;
        private final long reserved;

        private InstallmentSummary(Object[] row) {
            id = (UUID) row[0];
            scheduleReference = (String) row[1];
            number = ((Number) row[2]).shortValue();
            amount = ((Number) row[3]).longValue();
            dueDate = (Date) row[4];
            paid = (Boolean) row[5];
            reserved = ((Number) row[6]).longValue();
        }

        public UUID getId() { return id; }
        public String getScheduleReference() { return scheduleReference; }
        public short getNumber() { return number; }
        public long getAmount() { return amount; }
        public Date getDueDate() { return dueDate; }
        public boolean isPaid() { return paid; }
        public long getRemaining() { return Math.max(amount - reserved, 0); }
    }

    public static final class SubmissionSummary implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private final String scheduleReference;
        private final short installmentNumber;
        private final long amount;
        private final String channel;
        private final String reference;
        private final String status;
        private final Date submittedAt;
        private final String reason;
        private final Date reviewedAt;
        private final Date paymentDate;
        private final long creditAmount;
        private final boolean proofAvailable;

        private SubmissionSummary(List<Object[]> rows) {
            Object[] row = rows.get(0);
            id = (UUID) row[0];
            scheduleReference = (String) row[1];
            installmentNumber = ((Number) row[2]).shortValue();
            amount = row[11] == null
                    ? rows.stream().mapToLong(item -> ((Number) item[3]).longValue()).sum()
                    : ((Number) row[11]).longValue();
            channel = (String) row[4];
            reference = (String) row[5];
            status = (String) row[6];
            submittedAt = (Date) row[7];
            reason = (String) row[8];
            reviewedAt = (Date) row[9];
            creditAmount = rows.stream()
                    .mapToLong(item -> ((Number) item[12]).longValue())
                    .sum();
            paymentDate = (Date) row[13];
            proofAvailable = rows.stream().anyMatch(item -> item[14] != null);
        }

        public UUID getId() { return id; }
        public String getScheduleReference() { return scheduleReference; }
        public short getInstallmentNumber() { return installmentNumber; }
        public long getAmount() { return amount; }
        public String getChannel() { return channel; }
        public String getReference() { return reference; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return PaymentDisplayLabels.status(status); }
        public String getChannelLabel() { return PaymentDisplayLabels.channel(channel); }
        public Date getSubmittedAt() { return submittedAt; }
        public String getReason() { return reason; }
        public Date getReviewedAt() { return reviewedAt; }
        public Date getPaymentDate() { return paymentDate; }
        public long getCreditAmount() { return creditAmount; }
        public boolean isProofAvailable() { return proofAvailable; }
    }
}
