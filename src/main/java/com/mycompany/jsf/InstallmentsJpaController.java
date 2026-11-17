/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.IllegalOrphanException;
import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import com.mycompany.jsf.security.PaymentSubmissionStatus;
import com.mycompany.jsf.security.ScheduleConfirmationStatus;
import entities.Installments;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.PaymentSchedules;
import entities.PaymentSubmissions;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fabri
 */
public class InstallmentsJpaController implements Serializable {

    public InstallmentsJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Installments installments) throws PreexistingEntityException, RollbackFailureException, Exception {
        if (installments.getPaymentSubmissionsList() == null) {
            installments.setPaymentSubmissionsList(new ArrayList<PaymentSubmissions>());
        }
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentSchedules scheduleId = installments.getScheduleId();
            if (scheduleId != null) {
                scheduleId = em.getReference(scheduleId.getClass(), scheduleId.getId());
                installments.setScheduleId(scheduleId);
            }
            List<PaymentSubmissions> attachedPaymentSubmissionsList = new ArrayList<PaymentSubmissions>();
            for (PaymentSubmissions paymentSubmissionsListPaymentSubmissionsToAttach : installments.getPaymentSubmissionsList()) {
                paymentSubmissionsListPaymentSubmissionsToAttach = em.getReference(paymentSubmissionsListPaymentSubmissionsToAttach.getClass(), paymentSubmissionsListPaymentSubmissionsToAttach.getId());
                attachedPaymentSubmissionsList.add(paymentSubmissionsListPaymentSubmissionsToAttach);
            }
            installments.setPaymentSubmissionsList(attachedPaymentSubmissionsList);
            em.persist(installments);
            if (scheduleId != null) {
                scheduleId.getInstallmentsList().add(installments);
                scheduleId = em.merge(scheduleId);
            }
            for (PaymentSubmissions paymentSubmissionsListPaymentSubmissions : installments.getPaymentSubmissionsList()) {
                Installments oldInstallmentIdOfPaymentSubmissionsListPaymentSubmissions = paymentSubmissionsListPaymentSubmissions.getInstallmentId();
                paymentSubmissionsListPaymentSubmissions.setInstallmentId(installments);
                paymentSubmissionsListPaymentSubmissions = em.merge(paymentSubmissionsListPaymentSubmissions);
                if (oldInstallmentIdOfPaymentSubmissionsListPaymentSubmissions != null) {
                    oldInstallmentIdOfPaymentSubmissionsListPaymentSubmissions.getPaymentSubmissionsList().remove(paymentSubmissionsListPaymentSubmissions);
                    oldInstallmentIdOfPaymentSubmissionsListPaymentSubmissions = em.merge(oldInstallmentIdOfPaymentSubmissionsListPaymentSubmissions);
                }
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findInstallments(installments.getId()) != null) {
                throw new PreexistingEntityException("Installments " + installments + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void ensureNotLocked(Installments installment) {
        if (installment.getPaid()) {
            throw new IllegalStateException(
                    "Une échéance réglée ne peut plus être modifiée ni supprimée.");
        }
        PaymentSchedules schedule = installment.getScheduleId();
        if (schedule != null && ScheduleConfirmationStatus.CONFIRMED.equals(
                ScheduleConfirmationStatus.normalize(schedule.getConfirmationStatus()))) {
            throw new IllegalStateException(
                    "Une échéance d’un échéancier validé par le gestionnaire ne peut plus être modifiée ni supprimée.");
        }
        List<PaymentSubmissions> submissions = installment.getPaymentSubmissionsList();
        if (submissions != null && submissions.stream().anyMatch(submission
                -> PaymentSubmissionStatus.APPROVED.equalsIgnoreCase(submission.getStatus()))) {
            throw new IllegalStateException(
                    "Une échéance avec un paiement validé ne peut plus être modifiée ni supprimée.");
        }
    }

    public void edit(Installments installments) throws IllegalOrphanException, NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Installments persistentInstallments = em.find(Installments.class, installments.getId());
            ensureNotLocked(persistentInstallments);
            PaymentSchedules scheduleIdOld = persistentInstallments.getScheduleId();
            PaymentSchedules scheduleIdNew = installments.getScheduleId();
            List<PaymentSubmissions> paymentSubmissionsListOld = persistentInstallments.getPaymentSubmissionsList();
            List<PaymentSubmissions> paymentSubmissionsListNew = installments.getPaymentSubmissionsList();
            List<String> illegalOrphanMessages = null;
            for (PaymentSubmissions paymentSubmissionsListOldPaymentSubmissions : paymentSubmissionsListOld) {
                if (!paymentSubmissionsListNew.contains(paymentSubmissionsListOldPaymentSubmissions)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain PaymentSubmissions " + paymentSubmissionsListOldPaymentSubmissions + " since its installmentId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (scheduleIdNew != null) {
                scheduleIdNew = em.getReference(scheduleIdNew.getClass(), scheduleIdNew.getId());
                installments.setScheduleId(scheduleIdNew);
            }
            List<PaymentSubmissions> attachedPaymentSubmissionsListNew = new ArrayList<PaymentSubmissions>();
            for (PaymentSubmissions paymentSubmissionsListNewPaymentSubmissionsToAttach : paymentSubmissionsListNew) {
                paymentSubmissionsListNewPaymentSubmissionsToAttach = em.getReference(paymentSubmissionsListNewPaymentSubmissionsToAttach.getClass(), paymentSubmissionsListNewPaymentSubmissionsToAttach.getId());
                attachedPaymentSubmissionsListNew.add(paymentSubmissionsListNewPaymentSubmissionsToAttach);
            }
            paymentSubmissionsListNew = attachedPaymentSubmissionsListNew;
            installments.setPaymentSubmissionsList(paymentSubmissionsListNew);
            installments = em.merge(installments);
            if (scheduleIdOld != null && !scheduleIdOld.equals(scheduleIdNew)) {
                scheduleIdOld.getInstallmentsList().remove(installments);
                scheduleIdOld = em.merge(scheduleIdOld);
            }
            if (scheduleIdNew != null && !scheduleIdNew.equals(scheduleIdOld)) {
                scheduleIdNew.getInstallmentsList().add(installments);
                scheduleIdNew = em.merge(scheduleIdNew);
            }
            for (PaymentSubmissions paymentSubmissionsListNewPaymentSubmissions : paymentSubmissionsListNew) {
                if (!paymentSubmissionsListOld.contains(paymentSubmissionsListNewPaymentSubmissions)) {
                    Installments oldInstallmentIdOfPaymentSubmissionsListNewPaymentSubmissions = paymentSubmissionsListNewPaymentSubmissions.getInstallmentId();
                    paymentSubmissionsListNewPaymentSubmissions.setInstallmentId(installments);
                    paymentSubmissionsListNewPaymentSubmissions = em.merge(paymentSubmissionsListNewPaymentSubmissions);
                    if (oldInstallmentIdOfPaymentSubmissionsListNewPaymentSubmissions != null && !oldInstallmentIdOfPaymentSubmissionsListNewPaymentSubmissions.equals(installments)) {
                        oldInstallmentIdOfPaymentSubmissionsListNewPaymentSubmissions.getPaymentSubmissionsList().remove(paymentSubmissionsListNewPaymentSubmissions);
                        oldInstallmentIdOfPaymentSubmissionsListNewPaymentSubmissions = em.merge(oldInstallmentIdOfPaymentSubmissionsListNewPaymentSubmissions);
                    }
                }
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
                Object id = installments.getId();
                if (findInstallments(id) == null) {
                    throw new NonexistentEntityException("The installments with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Object id) throws IllegalOrphanException, NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Installments installments;
            try {
                installments = em.getReference(Installments.class, id);
                installments.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The installments with id " + id + " no longer exists.", enfe);
            }
            ensureNotLocked(installments);
            List<String> illegalOrphanMessages = null;
            List<PaymentSubmissions> paymentSubmissionsListOrphanCheck = installments.getPaymentSubmissionsList();
            for (PaymentSubmissions paymentSubmissionsListOrphanCheckPaymentSubmissions : paymentSubmissionsListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Installments (" + installments + ") cannot be destroyed since the PaymentSubmissions " + paymentSubmissionsListOrphanCheckPaymentSubmissions + " in its paymentSubmissionsList field has a non-nullable installmentId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            PaymentSchedules scheduleId = installments.getScheduleId();
            if (scheduleId != null) {
                scheduleId.getInstallmentsList().remove(installments);
                scheduleId = em.merge(scheduleId);
            }
            em.remove(installments);
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

    public List<Installments> findInstallmentsEntities() {
        return findInstallmentsEntities(true, -1, -1);
    }

    public List<Installments> findInstallmentsEntities(int maxResults, int firstResult) {
        return findInstallmentsEntities(false, maxResults, firstResult);
    }

    private List<Installments> findInstallmentsEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Installments.class));
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

    public Installments findInstallments(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Installments.class, id);
        } finally {
            em.close();
        }
    }

    /** Charge l'échéance avec ses paiements soumis (collections paresseuses initialisées). */
    public Installments findInstallmentsWithSubmissions(Object id) {
        EntityManager em = getEntityManager();
        try {
            Installments installment = em.find(Installments.class, id);
            if (installment != null) {
                installment.getPaymentSubmissionsList().size();
            }
            return installment;
        } finally {
            em.close();
        }
    }

    public int getInstallmentsCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Installments> rt = cq.from(Installments.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
