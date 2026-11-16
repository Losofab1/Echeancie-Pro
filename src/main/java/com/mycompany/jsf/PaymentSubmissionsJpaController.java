/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import com.mycompany.jsf.security.PaymentSubmissionStatus;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.Installments;
import entities.PaymentSubmissions;
import entities.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.transaction.UserTransaction;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author fabri
 */
public class PaymentSubmissionsJpaController implements Serializable {

    public PaymentSubmissionsJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(PaymentSubmissions paymentSubmissions) throws PreexistingEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Installments installmentId = paymentSubmissions.getInstallmentId();
            if (installmentId != null) {
                installmentId = em.getReference(installmentId.getClass(), installmentId.getId());
                paymentSubmissions.setInstallmentId(installmentId);
            }
            Users reviewedBy = paymentSubmissions.getReviewedBy();
            if (reviewedBy != null) {
                reviewedBy = em.getReference(reviewedBy.getClass(), reviewedBy.getId());
                paymentSubmissions.setReviewedBy(reviewedBy);
            }
            Users submittedBy = paymentSubmissions.getSubmittedBy();
            if (submittedBy != null) {
                submittedBy = em.getReference(submittedBy.getClass(), submittedBy.getId());
                paymentSubmissions.setSubmittedBy(submittedBy);
            }
            em.persist(paymentSubmissions);
            if (installmentId != null) {
                installmentId.getPaymentSubmissionsList().add(paymentSubmissions);
                installmentId = em.merge(installmentId);
            }
            if (reviewedBy != null) {
                reviewedBy.getPaymentSubmissionsList().add(paymentSubmissions);
                reviewedBy = em.merge(reviewedBy);
            }
            if (submittedBy != null) {
                submittedBy.getPaymentSubmissionsList().add(paymentSubmissions);
                submittedBy = em.merge(submittedBy);
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findPaymentSubmissions(paymentSubmissions.getId()) != null) {
                throw new PreexistingEntityException("PaymentSubmissions " + paymentSubmissions + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void ensureNotLocked(PaymentSubmissions submission) {
        if (PaymentSubmissionStatus.APPROVED.equalsIgnoreCase(submission.getStatus())) {
            throw new IllegalStateException(
                    "Un paiement validé par le gestionnaire figure dans l’historique et ne peut plus être modifié ni supprimé.");
        }
    }

    public void edit(PaymentSubmissions paymentSubmissions) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentSubmissions persistentPaymentSubmissions = em.find(PaymentSubmissions.class, paymentSubmissions.getId());
            if (persistentPaymentSubmissions == null) {
                throw new NonexistentEntityException(
                        "The paymentSubmissions with id " + paymentSubmissions.getId() + " no longer exists.");
            }
            ensureNotLocked(persistentPaymentSubmissions);
            if (persistentPaymentSubmissions.getPaymentGroupId() != null) {
                throw new IllegalStateException(
                        "Un paiement réparti doit être traité comme une seule opération depuis la file gestionnaire.");
            }
            Installments installmentIdOld = persistentPaymentSubmissions.getInstallmentId();
            Installments installmentIdNew = paymentSubmissions.getInstallmentId();
            if (installmentIdOld == null || installmentIdNew == null
                    || !installmentIdOld.getId().equals(installmentIdNew.getId())) {
                throw new IllegalStateException("L’échéance d’un paiement soumis ne peut pas être modifiée.");
            }
            Installments lockedInstallment = em.find(
                    Installments.class, installmentIdOld.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (lockedInstallment == null) {
                throw new NonexistentEntityException("L’échéance associée n’existe plus.");
            }
            String newStatus = normalizeSubmissionStatus(paymentSubmissions.getStatus());
            if (paymentSubmissions.getAmount() <= 0) {
                throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro.");
            }
            if (PaymentSubmissionStatus.REJECTED.equals(newStatus)
                    && (paymentSubmissions.getReason() == null
                    || paymentSubmissions.getReason().isBlank())) {
                throw new IllegalArgumentException("Un motif est obligatoire pour rejeter un paiement.");
            }
            paymentSubmissions.setStatus(newStatus);
            Long reservedByOtherSubmissions = em.createQuery(
                    "SELECT COALESCE(SUM(p.amount), 0) FROM PaymentSubmissions p "
                    + "WHERE p.installmentId.id = :installmentId AND p.id <> :submissionId "
                    + "AND UPPER(p.status) IN :reservedStatuses", Long.class)
                    .setParameter("installmentId", installmentIdOld.getId())
                    .setParameter("submissionId", paymentSubmissions.getId())
                    .setParameter("reservedStatuses", Arrays.asList(
                            PaymentSubmissionStatus.PENDING,
                            PaymentSubmissionStatus.APPROVED))
                    .getSingleResult();
            if (!PaymentSubmissionStatus.REJECTED.equals(newStatus)
                    && paymentSubmissions.getAmount()
                    > lockedInstallment.getAmount() - reservedByOtherSubmissions) {
                throw new IllegalStateException(
                        "Le total des paiements en attente ou approuvés dépasserait le montant de l’échéance.");
            }
            boolean paymentChanged = !persistentPaymentSubmissions.getStatus().equalsIgnoreCase(newStatus)
                    || persistentPaymentSubmissions.getAmount() != paymentSubmissions.getAmount();
            Users reviewedByOld = persistentPaymentSubmissions.getReviewedBy();
            Users reviewedByNew = paymentSubmissions.getReviewedBy();
            Users submittedByOld = persistentPaymentSubmissions.getSubmittedBy();
            Users submittedByNew = paymentSubmissions.getSubmittedBy();
            if (reviewedByNew != null) {
                reviewedByNew = em.getReference(reviewedByNew.getClass(), reviewedByNew.getId());
                paymentSubmissions.setReviewedBy(reviewedByNew);
            }
            if (submittedByNew != null) {
                submittedByNew = em.getReference(submittedByNew.getClass(), submittedByNew.getId());
                paymentSubmissions.setSubmittedBy(submittedByNew);
            }
            paymentSubmissions.setInstallmentId(lockedInstallment);
            paymentSubmissions = em.merge(paymentSubmissions);
            if (reviewedByOld != null && !reviewedByOld.equals(reviewedByNew)) {
                reviewedByOld.getPaymentSubmissionsList().remove(paymentSubmissions);
                reviewedByOld = em.merge(reviewedByOld);
            }
            if (reviewedByNew != null && !reviewedByNew.equals(reviewedByOld)) {
                reviewedByNew.getPaymentSubmissionsList().add(paymentSubmissions);
                reviewedByNew = em.merge(reviewedByNew);
            }
            if (submittedByOld != null && !submittedByOld.equals(submittedByNew)) {
                submittedByOld.getPaymentSubmissionsList().remove(paymentSubmissions);
                submittedByOld = em.merge(submittedByOld);
            }
            if (submittedByNew != null && !submittedByNew.equals(submittedByOld)) {
                submittedByNew.getPaymentSubmissionsList().add(paymentSubmissions);
                submittedByNew = em.merge(submittedByNew);
            }
            if (paymentChanged) {
                Long approvedAmount = em.createQuery(
                        "SELECT COALESCE(SUM(p.amount), 0) FROM PaymentSubmissions p "
                        + "WHERE p.installmentId.id = :installmentId "
                        + "AND UPPER(p.status) = :approvedStatus", Long.class)
                        .setParameter("installmentId", lockedInstallment.getId())
                        .setParameter("approvedStatus", PaymentSubmissionStatus.APPROVED)
                        .getSingleResult();
                boolean paid = approvedAmount >= lockedInstallment.getAmount();
                lockedInstallment.setPaid(paid);
                lockedInstallment.setPaidAt(paid ? new Date() : null);
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Object id = paymentSubmissions.getId();
                if (findPaymentSubmissions(id) == null) {
                    throw new NonexistentEntityException("The paymentSubmissions with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private String normalizeSubmissionStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!PaymentSubmissionStatus.PENDING.equals(normalized)
                && !PaymentSubmissionStatus.APPROVED.equals(normalized)
                && !PaymentSubmissionStatus.REJECTED.equals(normalized)) {
            throw new IllegalArgumentException("Le statut du paiement est invalide.");
        }
        return normalized;
    }

    public void destroy(Object id) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentSubmissions paymentSubmissions;
            try {
                paymentSubmissions = em.getReference(PaymentSubmissions.class, id);
                paymentSubmissions.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The paymentSubmissions with id " + id + " no longer exists.", enfe);
            }
            ensureNotLocked(paymentSubmissions);
            if (paymentSubmissions.getPaymentGroupId() != null) {
                throw new IllegalStateException(
                        "Un paiement réparti ne peut pas être supprimé ligne par ligne.");
            }
            Installments installmentId = paymentSubmissions.getInstallmentId();
            if (installmentId != null) {
                installmentId.getPaymentSubmissionsList().remove(paymentSubmissions);
                installmentId = em.merge(installmentId);
            }
            Users reviewedBy = paymentSubmissions.getReviewedBy();
            if (reviewedBy != null) {
                reviewedBy.getPaymentSubmissionsList().remove(paymentSubmissions);
                reviewedBy = em.merge(reviewedBy);
            }
            Users submittedBy = paymentSubmissions.getSubmittedBy();
            if (submittedBy != null) {
                submittedBy.getPaymentSubmissionsList().remove(paymentSubmissions);
                submittedBy = em.merge(submittedBy);
            }
            em.remove(paymentSubmissions);
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<PaymentSubmissions> findPaymentSubmissionsEntities() {
        return findPaymentSubmissionsEntities(true, -1, -1);
    }

    public List<PaymentSubmissions> findPaymentSubmissionsEntities(int maxResults, int firstResult) {
        return findPaymentSubmissionsEntities(false, maxResults, firstResult);
    }

    private List<PaymentSubmissions> findPaymentSubmissionsEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(PaymentSubmissions.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public PaymentSubmissions findPaymentSubmissions(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(PaymentSubmissions.class, id);
        } finally {
            em.close();
        }
    }

    public int getPaymentSubmissionsCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<PaymentSubmissions> rt = cq.from(PaymentSubmissions.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
