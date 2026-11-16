/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.IllegalOrphanException;
import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.Clients;
import entities.PaymentSchedules;
import entities.Users;
import entities.PaymentRequestInstallments;
import entities.PaymentRequests;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fabri
 */
public class PaymentRequestsJpaController implements Serializable {

    public PaymentRequestsJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(PaymentRequests paymentRequests) throws PreexistingEntityException, RollbackFailureException, Exception {
        if (paymentRequests.getPaymentRequestInstallmentsList() == null) {
            paymentRequests.setPaymentRequestInstallmentsList(new ArrayList<PaymentRequestInstallments>());
        }
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Clients clientId = paymentRequests.getClientId();
            if (clientId != null) {
                clientId = em.getReference(clientId.getClass(), clientId.getId());
                paymentRequests.setClientId(clientId);
            }
            PaymentSchedules scheduleId = paymentRequests.getScheduleId();
            if (scheduleId != null) {
                scheduleId = em.getReference(scheduleId.getClass(), scheduleId.getId());
                paymentRequests.setScheduleId(scheduleId);
            }
            Users requesterId = paymentRequests.getRequesterId();
            if (requesterId != null) {
                requesterId = em.getReference(requesterId.getClass(), requesterId.getId());
                paymentRequests.setRequesterId(requesterId);
            }
            Users reviewedBy = paymentRequests.getReviewedBy();
            if (reviewedBy != null) {
                reviewedBy = em.getReference(reviewedBy.getClass(), reviewedBy.getId());
                paymentRequests.setReviewedBy(reviewedBy);
            }
            List<PaymentRequestInstallments> attachedPaymentRequestInstallmentsList = new ArrayList<PaymentRequestInstallments>();
            for (PaymentRequestInstallments paymentRequestInstallmentsListPaymentRequestInstallmentsToAttach : paymentRequests.getPaymentRequestInstallmentsList()) {
                paymentRequestInstallmentsListPaymentRequestInstallmentsToAttach = em.getReference(paymentRequestInstallmentsListPaymentRequestInstallmentsToAttach.getClass(), paymentRequestInstallmentsListPaymentRequestInstallmentsToAttach.getId());
                attachedPaymentRequestInstallmentsList.add(paymentRequestInstallmentsListPaymentRequestInstallmentsToAttach);
            }
            paymentRequests.setPaymentRequestInstallmentsList(attachedPaymentRequestInstallmentsList);
            em.persist(paymentRequests);
            if (clientId != null) {
                clientId.getPaymentRequestsList().add(paymentRequests);
                clientId = em.merge(clientId);
            }
            if (scheduleId != null) {
                PaymentRequests oldPaymentRequestsOfScheduleId = scheduleId.getPaymentRequests();
                if (oldPaymentRequestsOfScheduleId != null) {
                    oldPaymentRequestsOfScheduleId.setScheduleId(null);
                    oldPaymentRequestsOfScheduleId = em.merge(oldPaymentRequestsOfScheduleId);
                }
                scheduleId.setPaymentRequests(paymentRequests);
                scheduleId = em.merge(scheduleId);
            }
            if (requesterId != null) {
                requesterId.getPaymentRequestsList().add(paymentRequests);
                requesterId = em.merge(requesterId);
            }
            if (reviewedBy != null) {
                reviewedBy.getPaymentRequestsList().add(paymentRequests);
                reviewedBy = em.merge(reviewedBy);
            }
            for (PaymentRequestInstallments paymentRequestInstallmentsListPaymentRequestInstallments : paymentRequests.getPaymentRequestInstallmentsList()) {
                PaymentRequests oldRequestIdOfPaymentRequestInstallmentsListPaymentRequestInstallments = paymentRequestInstallmentsListPaymentRequestInstallments.getRequestId();
                paymentRequestInstallmentsListPaymentRequestInstallments.setRequestId(paymentRequests);
                paymentRequestInstallmentsListPaymentRequestInstallments = em.merge(paymentRequestInstallmentsListPaymentRequestInstallments);
                if (oldRequestIdOfPaymentRequestInstallmentsListPaymentRequestInstallments != null) {
                    oldRequestIdOfPaymentRequestInstallmentsListPaymentRequestInstallments.getPaymentRequestInstallmentsList().remove(paymentRequestInstallmentsListPaymentRequestInstallments);
                    oldRequestIdOfPaymentRequestInstallmentsListPaymentRequestInstallments = em.merge(oldRequestIdOfPaymentRequestInstallmentsListPaymentRequestInstallments);
                }
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findPaymentRequests(paymentRequests.getId()) != null) {
                throw new PreexistingEntityException("PaymentRequests " + paymentRequests + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void ensureNotLocked(PaymentRequests request) {
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase(java.util.Locale.ROOT);
        if ("APPROVED".equals(status) || "CONFIRMED".equals(status)) {
            throw new IllegalStateException(
                    "Une demande approuvée par le gestionnaire ne peut plus être modifiée ni supprimée.");
        }
    }

    public void edit(PaymentRequests paymentRequests) throws IllegalOrphanException, NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentRequests persistentPaymentRequests = em.find(PaymentRequests.class, paymentRequests.getId());
            ensureNotLocked(persistentPaymentRequests);
            Clients clientIdOld = persistentPaymentRequests.getClientId();
            Clients clientIdNew = paymentRequests.getClientId();
            PaymentSchedules scheduleIdOld = persistentPaymentRequests.getScheduleId();
            PaymentSchedules scheduleIdNew = paymentRequests.getScheduleId();
            Users requesterIdOld = persistentPaymentRequests.getRequesterId();
            Users requesterIdNew = paymentRequests.getRequesterId();
            Users reviewedByOld = persistentPaymentRequests.getReviewedBy();
            Users reviewedByNew = paymentRequests.getReviewedBy();
            List<PaymentRequestInstallments> paymentRequestInstallmentsListOld = persistentPaymentRequests.getPaymentRequestInstallmentsList();
            List<PaymentRequestInstallments> paymentRequestInstallmentsListNew = paymentRequests.getPaymentRequestInstallmentsList();
            List<String> illegalOrphanMessages = null;
            for (PaymentRequestInstallments paymentRequestInstallmentsListOldPaymentRequestInstallments : paymentRequestInstallmentsListOld) {
                if (!paymentRequestInstallmentsListNew.contains(paymentRequestInstallmentsListOldPaymentRequestInstallments)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain PaymentRequestInstallments " + paymentRequestInstallmentsListOldPaymentRequestInstallments + " since its requestId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (clientIdNew != null) {
                clientIdNew = em.getReference(clientIdNew.getClass(), clientIdNew.getId());
                paymentRequests.setClientId(clientIdNew);
            }
            if (scheduleIdNew != null) {
                scheduleIdNew = em.getReference(scheduleIdNew.getClass(), scheduleIdNew.getId());
                paymentRequests.setScheduleId(scheduleIdNew);
            }
            if (requesterIdNew != null) {
                requesterIdNew = em.getReference(requesterIdNew.getClass(), requesterIdNew.getId());
                paymentRequests.setRequesterId(requesterIdNew);
            }
            if (reviewedByNew != null) {
                reviewedByNew = em.getReference(reviewedByNew.getClass(), reviewedByNew.getId());
                paymentRequests.setReviewedBy(reviewedByNew);
            }
            List<PaymentRequestInstallments> attachedPaymentRequestInstallmentsListNew = new ArrayList<PaymentRequestInstallments>();
            for (PaymentRequestInstallments paymentRequestInstallmentsListNewPaymentRequestInstallmentsToAttach : paymentRequestInstallmentsListNew) {
                paymentRequestInstallmentsListNewPaymentRequestInstallmentsToAttach = em.getReference(paymentRequestInstallmentsListNewPaymentRequestInstallmentsToAttach.getClass(), paymentRequestInstallmentsListNewPaymentRequestInstallmentsToAttach.getId());
                attachedPaymentRequestInstallmentsListNew.add(paymentRequestInstallmentsListNewPaymentRequestInstallmentsToAttach);
            }
            paymentRequestInstallmentsListNew = attachedPaymentRequestInstallmentsListNew;
            paymentRequests.setPaymentRequestInstallmentsList(paymentRequestInstallmentsListNew);
            paymentRequests = em.merge(paymentRequests);
            if (clientIdOld != null && !clientIdOld.equals(clientIdNew)) {
                clientIdOld.getPaymentRequestsList().remove(paymentRequests);
                clientIdOld = em.merge(clientIdOld);
            }
            if (clientIdNew != null && !clientIdNew.equals(clientIdOld)) {
                clientIdNew.getPaymentRequestsList().add(paymentRequests);
                clientIdNew = em.merge(clientIdNew);
            }
            if (scheduleIdOld != null && !scheduleIdOld.equals(scheduleIdNew)) {
                scheduleIdOld.setPaymentRequests(null);
                scheduleIdOld = em.merge(scheduleIdOld);
            }
            if (scheduleIdNew != null && !scheduleIdNew.equals(scheduleIdOld)) {
                PaymentRequests oldPaymentRequestsOfScheduleId = scheduleIdNew.getPaymentRequests();
                if (oldPaymentRequestsOfScheduleId != null) {
                    oldPaymentRequestsOfScheduleId.setScheduleId(null);
                    oldPaymentRequestsOfScheduleId = em.merge(oldPaymentRequestsOfScheduleId);
                }
                scheduleIdNew.setPaymentRequests(paymentRequests);
                scheduleIdNew = em.merge(scheduleIdNew);
            }
            if (requesterIdOld != null && !requesterIdOld.equals(requesterIdNew)) {
                requesterIdOld.getPaymentRequestsList().remove(paymentRequests);
                requesterIdOld = em.merge(requesterIdOld);
            }
            if (requesterIdNew != null && !requesterIdNew.equals(requesterIdOld)) {
                requesterIdNew.getPaymentRequestsList().add(paymentRequests);
                requesterIdNew = em.merge(requesterIdNew);
            }
            if (reviewedByOld != null && !reviewedByOld.equals(reviewedByNew)) {
                reviewedByOld.getPaymentRequestsList().remove(paymentRequests);
                reviewedByOld = em.merge(reviewedByOld);
            }
            if (reviewedByNew != null && !reviewedByNew.equals(reviewedByOld)) {
                reviewedByNew.getPaymentRequestsList().add(paymentRequests);
                reviewedByNew = em.merge(reviewedByNew);
            }
            for (PaymentRequestInstallments paymentRequestInstallmentsListNewPaymentRequestInstallments : paymentRequestInstallmentsListNew) {
                if (!paymentRequestInstallmentsListOld.contains(paymentRequestInstallmentsListNewPaymentRequestInstallments)) {
                    PaymentRequests oldRequestIdOfPaymentRequestInstallmentsListNewPaymentRequestInstallments = paymentRequestInstallmentsListNewPaymentRequestInstallments.getRequestId();
                    paymentRequestInstallmentsListNewPaymentRequestInstallments.setRequestId(paymentRequests);
                    paymentRequestInstallmentsListNewPaymentRequestInstallments = em.merge(paymentRequestInstallmentsListNewPaymentRequestInstallments);
                    if (oldRequestIdOfPaymentRequestInstallmentsListNewPaymentRequestInstallments != null && !oldRequestIdOfPaymentRequestInstallmentsListNewPaymentRequestInstallments.equals(paymentRequests)) {
                        oldRequestIdOfPaymentRequestInstallmentsListNewPaymentRequestInstallments.getPaymentRequestInstallmentsList().remove(paymentRequestInstallmentsListNewPaymentRequestInstallments);
                        oldRequestIdOfPaymentRequestInstallmentsListNewPaymentRequestInstallments = em.merge(oldRequestIdOfPaymentRequestInstallmentsListNewPaymentRequestInstallments);
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
                Object id = paymentRequests.getId();
                if (findPaymentRequests(id) == null) {
                    throw new NonexistentEntityException("The paymentRequests with id " + id + " no longer exists.");
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
            PaymentRequests paymentRequests;
            try {
                paymentRequests = em.getReference(PaymentRequests.class, id);
                paymentRequests.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The paymentRequests with id " + id + " no longer exists.", enfe);
            }
            ensureNotLocked(paymentRequests);
            List<String> illegalOrphanMessages = null;
            List<PaymentRequestInstallments> paymentRequestInstallmentsListOrphanCheck = paymentRequests.getPaymentRequestInstallmentsList();
            for (PaymentRequestInstallments paymentRequestInstallmentsListOrphanCheckPaymentRequestInstallments : paymentRequestInstallmentsListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This PaymentRequests (" + paymentRequests + ") cannot be destroyed since the PaymentRequestInstallments " + paymentRequestInstallmentsListOrphanCheckPaymentRequestInstallments + " in its paymentRequestInstallmentsList field has a non-nullable requestId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Clients clientId = paymentRequests.getClientId();
            if (clientId != null) {
                clientId.getPaymentRequestsList().remove(paymentRequests);
                clientId = em.merge(clientId);
            }
            PaymentSchedules scheduleId = paymentRequests.getScheduleId();
            if (scheduleId != null) {
                scheduleId.setPaymentRequests(null);
                scheduleId = em.merge(scheduleId);
            }
            Users requesterId = paymentRequests.getRequesterId();
            if (requesterId != null) {
                requesterId.getPaymentRequestsList().remove(paymentRequests);
                requesterId = em.merge(requesterId);
            }
            Users reviewedBy = paymentRequests.getReviewedBy();
            if (reviewedBy != null) {
                reviewedBy.getPaymentRequestsList().remove(paymentRequests);
                reviewedBy = em.merge(reviewedBy);
            }
            em.remove(paymentRequests);
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

    public List<PaymentRequests> findPaymentRequestsEntities() {
        return findPaymentRequestsEntities(true, -1, -1);
    }

    public List<PaymentRequests> findPaymentRequestsEntities(int maxResults, int firstResult) {
        return findPaymentRequestsEntities(false, maxResults, firstResult);
    }

    private List<PaymentRequests> findPaymentRequestsEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(PaymentRequests.class));
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

    public PaymentRequests findPaymentRequests(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(PaymentRequests.class, id);
        } finally {
            em.close();
        }
    }

    /** Charge la demande avec ses dates proposées (collection paresseuse initialisée). */
    public PaymentRequests findPaymentRequestsWithInstallments(Object id) {
        EntityManager em = getEntityManager();
        try {
            PaymentRequests request = em.find(PaymentRequests.class, id);
            if (request != null) {
                request.getPaymentRequestInstallmentsList().size();
            }
            return request;
        } finally {
            em.close();
        }
    }

    public int getPaymentRequestsCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<PaymentRequests> rt = cq.from(PaymentRequests.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
