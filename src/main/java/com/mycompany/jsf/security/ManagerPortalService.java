package com.mycompany.jsf.security;

import entities.PaymentRequests;
import entities.PaymentRequestInstallments;
import entities.Installments;
import entities.Clients;
import entities.PaymentSchedules;
import entities.Users;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@ApplicationScoped
public class ManagerPortalService {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager entityManager;
    @Inject
    private PaymentTransactionService paymentTransactionService;

    @Transactional
    public ManagerDashboard loadDashboard(UUID staffId) {
        requireStaff(staffId);
        List<PaymentRequests> requestRows = entityManager.createQuery(
                "SELECT DISTINCT r FROM PaymentRequests r JOIN FETCH r.clientId "
                + "LEFT JOIN FETCH r.paymentRequestInstallmentsList "
                + "WHERE UPPER(r.status) = :status ORDER BY r.createdAt",
                PaymentRequests.class)
                .setParameter("status", "PENDING")
                .getResultList();
        List<Object[]> paymentRows = entityManager.createQuery(
                "SELECT p.id, c.name, c.email, s.reference, i.installmentNumber, "
                + "p.amount, p.channel, p.reference, p.submittedAt, p.paymentDate, "
                + "p.paymentGroupId, p.paymentGroupAmount, p.creditAmount, p.proofFileName "
                + "FROM PaymentSubmissions p JOIN p.installmentId i "
                + "JOIN i.scheduleId s JOIN s.clientId c "
                + "WHERE UPPER(p.status) = :status "
                + "ORDER BY p.submittedAt, i.installmentNumber",
                Object[].class)
                .setParameter("status", PaymentSubmissionStatus.PENDING)
                .getResultList();
        List<Object[]> agencyInstallmentRows = entityManager.createQuery(
                "SELECT i.id, c.name, s.reference, i.installmentNumber, i.amount, i.dueDate, "
                + "COALESCE(SUM(CASE WHEN UPPER(p.status) IN :reservedStatuses "
                + "THEN p.amount ELSE 0 END), 0) "
                + "FROM Installments i JOIN i.scheduleId s JOIN s.clientId c "
                + "LEFT JOIN i.paymentSubmissionsList p WHERE i.paid = false "
                + "GROUP BY i.id, c.name, s.reference, i.installmentNumber, i.amount, i.dueDate "
                + "ORDER BY i.dueDate, c.name, i.installmentNumber", Object[].class)
                .setParameter("reservedStatuses", List.of(
                        PaymentSubmissionStatus.PENDING, PaymentSubmissionStatus.APPROVED))
                .getResultList();
        List<Object[]> creditRows = entityManager.createQuery(
                "SELECT c.id, c.name, c.email, c.creditBalance FROM Clients c "
                + "WHERE c.creditBalance > 0 ORDER BY c.name", Object[].class)
                .getResultList();
        return new ManagerDashboard(requestRows, paymentRows, agencyInstallmentRows, creditRows);
    }

    @Transactional
    public void reviewPaymentGroup(UUID staffId, UUID groupId, boolean approved, String reason) {
        paymentTransactionService.reviewPaymentGroup(staffId, groupId, approved, reason);
    }

    @Transactional
    public void recordAgencyPayment(UUID staffId, UUID installmentId, long amount,
            LocalDate paymentDate, String channel, String reference, String note) {
        paymentTransactionService.recordAgencyPayment(
                staffId, installmentId, amount, paymentDate, channel, reference, note);
    }

    @Transactional
    public void reviewPaymentRequest(UUID staffId, UUID requestId,
            boolean approved, long agreedTotal) {
        Users reviewer = requireStaff(staffId);
        if (requestId == null) {
            throw new IllegalArgumentException("La demande sélectionnée est invalide.");
        }
        PaymentRequests request = entityManager.find(
                PaymentRequests.class, requestId, LockModeType.PESSIMISTIC_WRITE);
        if (request == null) {
            throw new IllegalArgumentException("Cette demande n’existe plus.");
        }
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }
        if (approved && agreedTotal <= 0) {
            throw new IllegalArgumentException("Le montant accepté doit être supérieur à zéro.");
        }
        if (approved && agreedTotal > request.getTotal()) {
            throw new IllegalArgumentException(
                    "Le montant accepté ne peut pas dépasser le montant demandé.");
        }

        Date reviewedAt = new Date();
        if (approved) {
            if (agreedTotal < request.getInstallmentCount()) {
                throw new IllegalArgumentException(
                        "Le montant accepté doit couvrir au moins une unité par échéance.");
            }
            if (request.getScheduleId() != null) {
                throw new IllegalStateException(
                        "Un échéancier existe déjà pour cette demande.");
            }
            createSchedule(request, reviewer, agreedTotal, reviewedAt);
        }
        request.setStatus(approved ? "APPROVED" : "REJECTED");
        request.setAgreedTotal(approved ? agreedTotal : null);
        request.setReviewedAt(reviewedAt);
        request.setReviewedBy(reviewer);
    }

    private void createSchedule(PaymentRequests request, Users manager,
            long total, Date createdAt) {
        String compactRequestId = request.getId().toString().replace("-", "");
        PaymentSchedules schedule = new PaymentSchedules();
        schedule.setReference("ECH-" + compactRequestId.substring(0, 28));
        schedule.setTotal(total);
        schedule.setConfirmationStatus(ScheduleConfirmationStatus.CONFIRMED);
        schedule.setCreatedAt(createdAt);
        schedule.setClientId(request.getClientId());
        schedule.setManagerId(manager);
        schedule.setPaymentRequests(request);
        request.setScheduleId(schedule);

        List<Installments> installments = buildInstallments(
                schedule, total, getProposedDueDates(request));
        schedule.setInstallmentsList(installments);
        entityManager.persist(schedule);
    }

    private List<LocalDate> getProposedDueDates(PaymentRequests request) {
        List<PaymentRequestInstallments> proposed = request.getPaymentRequestInstallmentsList();
        if (proposed == null) {
            throw new IllegalStateException("Les dates proposées sont manquantes.");
        }
        List<PaymentRequestInstallments> ordered = proposed.stream()
                .sorted(Comparator.comparingInt(
                        PaymentRequestInstallments::getInstallmentNumber))
                .toList();
        if (ordered.size() != request.getInstallmentCount()) {
            throw new IllegalStateException(
                    "Le nombre de dates proposées ne correspond pas à la demande.");
        }
        LocalDate requestDate = request.getCreatedAt().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        List<LocalDate> dueDates = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            PaymentRequestInstallments installment = ordered.get(index);
            if (installment.getInstallmentNumber() != index + 1
                    || installment.getDueDate() == null) {
                throw new IllegalStateException(
                        "Les dates proposées de cette demande sont invalides.");
            }
            Date dueDate = installment.getDueDate();
            LocalDate proposedDate = dueDate instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate()
                    : dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            dueDates.add(proposedDate);
        }
        try {
            ClientPortalService.validateProposedDueDates(
                    dueDates, request.getInstallmentCount(), requestDate);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Les dates proposées ne respectent pas les intervalles autorisés : "
                    + e.getMessage(), e);
        }
        return dueDates;
    }

    static List<Installments> buildInstallments(PaymentSchedules schedule,
            long total, List<LocalDate> proposedDueDates) {
        int count = proposedDueDates == null ? 0 : proposedDueDates.size();
        if (total <= 0 || count < 1
                || count > ClientPortalService.MAX_INSTALLMENT_COUNT || total < count) {
            throw new IllegalArgumentException(
                    "Le montant accepté doit couvrir au moins une unité par échéance.");
        }
        LocalDate previousDate = null;
        for (LocalDate dueDate : proposedDueDates) {
            if (dueDate == null || (previousDate != null
                    && (dueDate.isBefore(previousDate.plusMonths(1))
                    || dueDate.isAfter(previousDate.plusYears(1))))) {
                throw new IllegalArgumentException(
                        "L’intervalle entre deux échéances doit être compris entre un mois et un an.");
            }
            previousDate = dueDate;
        }
        long baseAmount = total / count;
        long remainder = total % count;
        List<Installments> installments = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Installments installment = new Installments();
            installment.setInstallmentNumber((short) (index + 1));
            installment.setAmount(baseAmount + (index < remainder ? 1 : 0));
            installment.setDueDate(java.sql.Date.valueOf(proposedDueDates.get(index)));
            installment.setPaid(false);
            installment.setScheduleId(schedule);
            installments.add(installment);
        }
        return installments;
    }

    private Users requireStaff(UUID staffId) {
        Users staff = staffId == null ? null : entityManager.find(Users.class, staffId);
        String role = staff == null ? "" : RoleNames.normalize(staff.getRole());
        if (staff == null || !staff.getActive()
                || (!RoleNames.ADMIN.equals(role) && !RoleNames.MANAGER.equals(role))) {
            throw new SecurityException("Un compte gestionnaire actif est nécessaire.");
        }
        return staff;
    }

    public static final class ManagerDashboard implements Serializable {

        private static final long serialVersionUID = 1L;
        private final List<PendingRequest> pendingRequests = new ArrayList<>();
        private final List<PendingPayment> pendingPayments = new ArrayList<>();
        private final List<AgencyInstallmentOption> agencyInstallments = new ArrayList<>();
        private final List<ClientCreditSummary> clientCredits = new ArrayList<>();

        private ManagerDashboard(List<PaymentRequests> requestRows,
                List<Object[]> paymentRows, List<Object[]> agencyInstallmentRows,
                List<Object[]> creditRows) {
            requestRows.forEach(request -> pendingRequests.add(new PendingRequest(request)));
            Map<UUID, List<Object[]>> groups = new LinkedHashMap<>();
            paymentRows.forEach(payment -> groups.computeIfAbsent(
                    payment[10] == null ? (UUID) payment[0] : (UUID) payment[10],
                    key -> new ArrayList<>()).add(payment));
            groups.values().forEach(rows -> pendingPayments.add(new PendingPayment(rows)));
            agencyInstallmentRows.forEach(row -> {
                AgencyInstallmentOption option = new AgencyInstallmentOption(row);
                if (option.getRemaining() > 0) {
                    agencyInstallments.add(option);
                }
            });
            creditRows.forEach(row -> clientCredits.add(new ClientCreditSummary(row)));
        }

        public List<PendingRequest> getPendingRequests() {
            return pendingRequests;
        }

        public int getPendingRequestCount() {
            return pendingRequests.size();
        }

        public List<PendingPayment> getPendingPayments() {
            return pendingPayments;
        }

        public int getPendingPaymentCount() {
            return pendingPayments.size();
        }

        public List<AgencyInstallmentOption> getAgencyInstallments() {
            return agencyInstallments;
        }

        public List<ClientCreditSummary> getClientCredits() {
            return clientCredits;
        }
    }

    public static final class PendingRequest implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private final String clientName;
        private final String clientEmail;
        private final long total;
        private final short installmentCount;
        private final String note;
        private final Date createdAt;
        private long agreedTotal;

        private final List<Date> proposedDueDates;

        private PendingRequest(PaymentRequests request) {
            id = request.getId();
            clientName = request.getClientId().getName();
            clientEmail = request.getClientId().getEmail();
            total = request.getTotal();
            installmentCount = request.getInstallmentCount();
            note = request.getNote();
            createdAt = request.getCreatedAt();
            List<PaymentRequestInstallments> proposed =
                    request.getPaymentRequestInstallmentsList();
            proposedDueDates = proposed == null ? List.of() : proposed.stream()
                    .sorted(Comparator.comparingInt(
                            PaymentRequestInstallments::getInstallmentNumber))
                    .map(PaymentRequestInstallments::getDueDate)
                    .toList();
            agreedTotal = total;
        }

        public UUID getId() { return id; }
        public String getClientName() { return clientName; }
        public String getClientEmail() { return clientEmail; }
        public long getTotal() { return total; }
        public short getInstallmentCount() { return installmentCount; }
        public String getNote() { return note; }
        public Date getCreatedAt() { return createdAt; }
        public List<Date> getProposedDueDates() { return proposedDueDates; }
        public long getAgreedTotal() { return agreedTotal; }
        public void setAgreedTotal(long agreedTotal) { this.agreedTotal = agreedTotal; }
    }

    public static final class PendingPayment implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private final UUID groupId;
        private final String clientName;
        private final String clientEmail;
        private final String scheduleReference;
        private final short installmentNumber;
        private final long amount;
        private final String channel;
        private final String reference;
        private final Date submittedAt;
        private final Date paymentDate;
        private final boolean proofAvailable;
        private final long creditAmount;
        private String reviewReason;

        private PendingPayment(List<Object[]> group) {
            Object[] first = group.get(0);
            id = (UUID) first[0];
            groupId = first[10] == null ? id : (UUID) first[10];
            clientName = (String) first[1];
            clientEmail = (String) first[2];
            scheduleReference = (String) first[3];
            installmentNumber = ((Number) first[4]).shortValue();
            amount = first[11] == null
                    ? group.stream().mapToLong(row -> ((Number) row[5]).longValue()).sum()
                    : ((Number) first[11]).longValue();
            channel = (String) first[6];
            reference = (String) first[7];
            submittedAt = (Date) first[8];
            paymentDate = (Date) first[9];
            proofAvailable = group.stream().anyMatch(row -> row[13] != null);
            creditAmount = group.stream().mapToLong(row -> ((Number) row[12]).longValue()).sum();
        }

        public UUID getId() { return id; }
        public UUID getGroupId() { return groupId; }
        public String getClientName() { return clientName; }
        public String getClientEmail() { return clientEmail; }
        public String getScheduleReference() { return scheduleReference; }
        public String getShortScheduleReference() {
            if (scheduleReference == null || scheduleReference.length() <= 12) {
                return scheduleReference;
            }
            return scheduleReference.substring(0, 4) + "…"
                    + scheduleReference.substring(scheduleReference.length() - 6);
        }
        public short getInstallmentNumber() { return installmentNumber; }
        public long getAmount() { return amount; }
        public String getChannel() { return channel; }
        public String getChannelLabel() { return PaymentDisplayLabels.channel(channel); }
        public String getReference() { return reference; }
        public Date getSubmittedAt() { return submittedAt; }
        public Date getPaymentDate() { return paymentDate; }
        public boolean isProofAvailable() { return proofAvailable; }
        public long getCreditAmount() { return creditAmount; }
        public String getReviewReason() { return reviewReason; }
        public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    }

    public static final class AgencyInstallmentOption implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID id;
        private final String label;
        private final long remaining;

        private AgencyInstallmentOption(Object[] row) {
            id = (UUID) row[0];
            label = row[1] + " — " + row[2] + " — échéance " + row[3]
                    + " — solde " + (((Number) row[4]).longValue()
                    - ((Number) row[6]).longValue());
            remaining = Math.max(((Number) row[4]).longValue()
                    - ((Number) row[6]).longValue(), 0);
        }

        public UUID getId() { return id; }
        public String getLabel() { return label; }
        public long getRemaining() { return remaining; }
    }

    public static final class ClientCreditSummary implements Serializable {

        private static final long serialVersionUID = 1L;
        private final UUID clientId;
        private final String clientName;
        private final String clientEmail;
        private final long creditBalance;

        private ClientCreditSummary(Object[] row) {
            clientId = (UUID) row[0];
            clientName = (String) row[1];
            clientEmail = (String) row[2];
            creditBalance = ((Number) row[3]).longValue();
        }

        public UUID getClientId() { return clientId; }
        public String getClientName() { return clientName; }
        public String getClientEmail() { return clientEmail; }
        public long getCreditBalance() { return creditBalance; }
    }
}
