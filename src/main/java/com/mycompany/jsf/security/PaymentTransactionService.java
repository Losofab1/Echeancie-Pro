package com.mycompany.jsf.security;

import entities.Clients;
import entities.Installments;
import entities.PaymentSchedules;
import entities.PaymentSubmissions;
import entities.Users;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Part;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class PaymentTransactionService {

    private static final long MAX_PROOF_SIZE = 5 * 1024 * 1024;
    private static final List<String> PAYMENT_CHANNELS = List.of(
            "BANK_TRANSFER", "MOBILE_MONEY", "CASH");

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager entityManager;

    @Transactional
    public void submitClientPayment(UUID clientUserId, UUID installmentId, long amount,
            LocalDate paymentDate, String channel, String reference, String note, Part proof) {
        Users user = requireClient(clientUserId);
        PaymentProof paymentProof = readProof(proof);
        createPaymentGroup(user, null, installmentId, amount, paymentDate,
                channel, reference, note, paymentProof, false);
    }

    @Transactional
    public void recordAgencyPayment(UUID staffId, UUID installmentId, long amount,
            LocalDate paymentDate, String channel, String reference, String note) {
        Users staff = requireStaff(staffId);
        createPaymentGroup(staff, staff, installmentId, amount, paymentDate,
                channel, reference, note, null, true);
    }

    @Transactional
    public void reviewPaymentGroup(UUID staffId, UUID groupId, boolean approved, String reason) {
        Users reviewer = requireStaff(staffId);
        if (groupId == null) {
            throw new IllegalArgumentException("Le paiement sélectionné est invalide.");
        }
        List<PaymentSubmissions> group = entityManager.createQuery(
                "SELECT p FROM PaymentSubmissions p JOIN FETCH p.installmentId "
                + "WHERE p.paymentGroupId = :groupId "
                + "OR (p.paymentGroupId IS NULL AND p.id = :groupId)",
                PaymentSubmissions.class)
                .setParameter("groupId", groupId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (group.isEmpty()) {
            throw new IllegalArgumentException("Ce paiement n’existe plus.");
        }
        if (group.stream().anyMatch(payment
                -> !PaymentSubmissionStatus.PENDING.equalsIgnoreCase(payment.getStatus()))) {
            throw new IllegalStateException("Ce paiement a déjà été traité.");
        }
        String normalizedReason = normalizeOptional(reason, 240, "Le motif");
        if (!approved && normalizedReason == null) {
            throw new IllegalArgumentException("Un motif est obligatoire pour rejeter un paiement.");
        }

        Date reviewedAt = new Date();
        Map<UUID, Installments> touchedInstallments = new LinkedHashMap<>();
        long creditAmount = 0;
        for (PaymentSubmissions payment : group) {
            payment.setStatus(approved
                    ? PaymentSubmissionStatus.APPROVED : PaymentSubmissionStatus.REJECTED);
            payment.setReason(approved ? null : normalizedReason);
            payment.setReviewedAt(reviewedAt);
            payment.setReviewedBy(reviewer);
            touchedInstallments.put(payment.getInstallmentId().getId(), payment.getInstallmentId());
            creditAmount = Math.max(creditAmount, payment.getCreditAmount());
        }
        if (approved) {
            applyPaymentDetails(group, touchedInstallments);
            if (creditAmount > 0) {
                Clients client = group.get(0).getInstallmentId().getScheduleId().getClientId();
                client.setCreditBalance(Math.addExact(client.getCreditBalance(), creditAmount));
            }
        }
        entityManager.flush();
        updatePaidFlags(touchedInstallments.values(), reviewedAt);
    }

    private void createPaymentGroup(Users submitter, Users reviewer, UUID installmentId,
            long amount, LocalDate paymentDate, String channel, String reference,
            String note, PaymentProof proof, boolean approved) {
        if (installmentId == null) {
            throw new IllegalArgumentException("Sélectionnez une échéance.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro.");
        }
        if (paymentDate == null || paymentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La date du paiement est obligatoire et ne peut pas être future.");
        }
        String normalizedChannel = normalizeRequired(channel, 32, "Le moyen de paiement");
        if (!PAYMENT_CHANNELS.contains(normalizedChannel)) {
            throw new IllegalArgumentException("Le moyen de paiement sélectionné est invalide.");
        }
        String normalizedReference = normalizeOptional(reference, 80, "La référence");
        String normalizedNote = normalizeOptional(note, 240, "La note");

        Installments selected = entityManager.find(Installments.class, installmentId);
        if (selected == null || selected.getScheduleId() == null) {
            throw new IllegalArgumentException("L’échéance sélectionnée n’est pas accessible.");
        }
        PaymentSchedules schedule = entityManager.find(
                PaymentSchedules.class, selected.getScheduleId().getId(),
                LockModeType.PESSIMISTIC_WRITE);
        if (schedule == null
                || (submitter.getClientId() != null
                && !submitter.getClientId().getId().equals(schedule.getClientId().getId()))) {
            throw new IllegalArgumentException("L’échéance sélectionnée n’est pas accessible.");
        }

        List<Installments> candidates = entityManager.createQuery(
                "SELECT i FROM Installments i WHERE i.scheduleId.id = :scheduleId "
                + "AND i.installmentNumber >= :startNumber AND i.paid = false "
                + "ORDER BY i.installmentNumber", Installments.class)
                .setParameter("scheduleId", schedule.getId())
                .setParameter("startNumber", selected.getInstallmentNumber())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (candidates.stream().noneMatch(candidate
                -> candidate.getId().equals(selected.getId()))) {
            throw new IllegalArgumentException(
                    "Aucun solde disponible à partir de cette échéance.");
        }
        Map<UUID, Long> reservedByInstallment = getReservedAmounts(schedule.getId());
        AllocationResult allocationResult = allocateAmount(
                amount, candidates, reservedByInstallment);
        List<InstallmentAllocation> allocations = allocationResult.allocations();
        long unallocated = allocationResult.creditAmount();

        UUID groupId = UUID.randomUUID();
        Date recordedAt = new Date();
        List<PaymentSubmissions> createdPayments = new ArrayList<>(allocations.size());
        for (int index = 0; index < allocations.size(); index++) {
            InstallmentAllocation allocation = allocations.get(index);
            PaymentSubmissions payment = new PaymentSubmissions();
            payment.setAmount(allocation.amount());
            payment.setChannel(normalizedChannel);
            payment.setReference(normalizedReference);
            payment.setNote(normalizedNote);
            payment.setStatus(approved
                    ? PaymentSubmissionStatus.APPROVED : PaymentSubmissionStatus.PENDING);
            payment.setSubmittedAt(recordedAt);
            payment.setPaymentDate(java.sql.Date.valueOf(paymentDate));
            payment.setPaymentGroupId(groupId);
            payment.setPaymentGroupAmount(amount);
            payment.setCreditAmount(index == 0 ? unallocated : 0);
            payment.setInstallmentId(allocation.installment());
            payment.setSubmittedBy(submitter);
            if (approved) {
                payment.setReviewedAt(recordedAt);
                payment.setReviewedBy(reviewer);
            }
            if (index == 0 && proof != null) {
                payment.setProofFileName(proof.fileName());
                payment.setProofContentType(proof.contentType());
                payment.setProofData(proof.data());
            }
            entityManager.persist(payment);
            createdPayments.add(payment);
        }
        if (approved && unallocated > 0) {
            Clients client = schedule.getClientId();
            client.setCreditBalance(Math.addExact(client.getCreditBalance(), unallocated));
        }
        if (approved) {
            Map<UUID, Installments> approvedInstallments = new LinkedHashMap<>();
            for (InstallmentAllocation allocation : allocations) {
                approvedInstallments.put(allocation.installment().getId(), allocation.installment());
            }
            applyPaymentDetails(createdPayments, approvedInstallments);
            entityManager.flush();
            updatePaidFlags(
                    allocations.stream().map(InstallmentAllocation::installment).toList(),
                    recordedAt);
        }
    }

    private Map<UUID, Long> getReservedAmounts(UUID scheduleId) {
        List<Object[]> rows = entityManager.createQuery(
                "SELECT p.installmentId.id, SUM(p.amount) FROM PaymentSubmissions p "
                + "WHERE p.installmentId.scheduleId.id = :scheduleId "
                + "AND UPPER(p.status) IN :statuses GROUP BY p.installmentId.id",
                Object[].class)
                .setParameter("scheduleId", scheduleId)
                .setParameter("statuses", Arrays.asList(
                        PaymentSubmissionStatus.PENDING, PaymentSubmissionStatus.APPROVED))
                .getResultList();
        Map<UUID, Long> reserved = new LinkedHashMap<>();
        for (Object[] row : rows) {
            reserved.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return reserved;
    }

    static AllocationResult allocateAmount(long amount, List<Installments> candidates,
            Map<UUID, Long> reservedByInstallment) {
        List<InstallmentAllocation> allocations = new ArrayList<>();
        long unallocated = amount;
        for (Installments installment : candidates) {
            long reserved = reservedByInstallment.getOrDefault(installment.getId(), 0L);
            long available = Math.max(installment.getAmount() - reserved, 0);
            long allocated = Math.min(unallocated, available);
            if (allocated > 0) {
                allocations.add(new InstallmentAllocation(installment, allocated));
                unallocated -= allocated;
            }
            if (unallocated == 0) {
                break;
            }
        }
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun solde disponible à partir de cette échéance.");
        }
        return new AllocationResult(List.copyOf(allocations), unallocated);
    }

    static void applyPaymentDetails(List<PaymentSubmissions> payments,
            Map<UUID, Installments> installmentsById) {
        for (PaymentSubmissions payment : payments) {
            Installments installment = installmentsById.get(payment.getInstallmentId().getId());
            if (installment == null) {
                continue;
            }
            installment.setChannel(payment.getChannel());
            installment.setPaymentReference(payment.getReference());
        }
    }

    private void updatePaidFlags(Iterable<Installments> installments, Date updatedAt) {
        for (Installments installment : installments) {
            Long approvedAmount = entityManager.createQuery(
                    "SELECT COALESCE(SUM(p.amount), 0) FROM PaymentSubmissions p "
                    + "WHERE p.installmentId.id = :installmentId "
                    + "AND UPPER(p.status) = :status", Long.class)
                    .setParameter("installmentId", installment.getId())
                    .setParameter("status", PaymentSubmissionStatus.APPROVED)
                    .getSingleResult();
            boolean paid = approvedAmount >= installment.getAmount();
            installment.setPaid(paid);
            installment.setPaidAt(paid ? updatedAt : null);
        }
    }

    private PaymentProof readProof(Part part) {
        if (part == null || part.getSize() <= 0 || part.getSize() > MAX_PROOF_SIZE) {
            throw new IllegalArgumentException("Ajoutez un justificatif de 5 Mo maximum.");
        }
        String contentType = part.getContentType() == null
                ? "" : part.getContentType().toLowerCase(Locale.ROOT);
        if (!List.of("image/jpeg", "image/png", "application/pdf").contains(contentType)) {
            throw new IllegalArgumentException("Le justificatif doit être une image JPG/PNG ou un PDF.");
        }
        try (InputStream input = part.getInputStream()) {
            byte[] data = input.readNBytes((int) MAX_PROOF_SIZE + 1);
            if (data.length == 0 || data.length > MAX_PROOF_SIZE || !matchesContent(data, contentType)) {
                throw new IllegalArgumentException("Le contenu du justificatif est invalide.");
            }
            String submittedName = part.getSubmittedFileName() == null
                    ? "justificatif" : part.getSubmittedFileName().replace('\\', '/');
            String fileName = submittedName.substring(submittedName.lastIndexOf('/') + 1)
                    .replaceAll("[^A-Za-z0-9._ -]", "_");
            if (fileName.isBlank() || fileName.length() > 255) {
                fileName = "justificatif";
            }
            return new PaymentProof(fileName, contentType, data);
        } catch (IOException e) {
            throw new IllegalArgumentException("Le justificatif n’a pas pu être lu.", e);
        }
    }

    private boolean matchesContent(byte[] data, String contentType) {
        if ("image/jpeg".equals(contentType)) {
            return data.length >= 3 && (data[0] & 0xFF) == 0xFF
                    && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
        }
        if ("image/png".equals(contentType)) {
            return data.length >= 8 && (data[0] & 0xFF) == 0x89
                    && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
                    && data[4] == 0x0D && data[5] == 0x0A
                    && data[6] == 0x1A && data[7] == 0x0A;
        }
        return data.length >= 5
                && "%PDF-".equals(new String(data, 0, 5, StandardCharsets.US_ASCII));
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

    private Users requireStaff(UUID staffId) {
        Users staff = staffId == null ? null : entityManager.find(Users.class, staffId);
        String role = staff == null ? "" : RoleNames.normalize(staff.getRole());
        if (staff == null || !staff.getActive()
                || (!RoleNames.ADMIN.equals(role) && !RoleNames.MANAGER.equals(role))) {
            throw new SecurityException("Un compte gestionnaire actif est nécessaire.");
        }
        return staff;
    }

    private String normalizeRequired(String value, int maxLength, String label) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " est obligatoire (maximum "
                    + maxLength + " caractères).");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength, String label) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " ne peut pas dépasser "
                    + maxLength + " caractères.");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    record InstallmentAllocation(Installments installment, long amount) {
    }

    record AllocationResult(List<InstallmentAllocation> allocations, long creditAmount) {
    }

    private record PaymentProof(String fileName, String contentType, byte[] data) {
    }
}
