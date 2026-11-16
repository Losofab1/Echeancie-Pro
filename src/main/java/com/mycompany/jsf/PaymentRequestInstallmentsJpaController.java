/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import entities.PaymentRequestInstallments;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.PaymentRequests;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.UserTransaction;
import java.util.List;

/**
 *
 * @author fabri
 */
public class PaymentRequestInstallmentsJpaController implements Serializable {

    public PaymentRequestInstallmentsJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(PaymentRequestInstallments paymentRequestInstallments) throws PreexistingEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentRequests requestId = paymentRequestInstallments.getRequestId();
            if (requestId != null) {
                requestId = em.getReference(requestId.getClass(), requestId.getId());
                paymentRequestInstallments.setRequestId(requestId);
            }
            em.persist(paymentRequestInstallments);
            if (requestId != null) {
                requestId.getPaymentRequestInstallmentsList().add(paymentRequestInstallments);
                requestId = em.merge(requestId);
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findPaymentRequestInstallments(paymentRequestInstallments.getId()) != null) {
                throw new PreexistingEntityException("PaymentRequestInstallments " + paymentRequestInstallments + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(PaymentRequestInstallments paymentRequestInstallments) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentRequestInstallments persistentPaymentRequestInstallments = em.find(PaymentRequestInstallments.class, paymentRequestInstallments.getId());
            PaymentRequests requestIdOld = persistentPaymentRequestInstallments.getRequestId();
            PaymentRequests requestIdNew = paymentRequestInstallments.getRequestId();
            if (requestIdNew != null) {
                requestIdNew = em.getReference(requestIdNew.getClass(), requestIdNew.getId());
                paymentRequestInstallments.setRequestId(requestIdNew);
            }
            paymentRequestInstallments = em.merge(paymentRequestInstallments);
            if (requestIdOld != null && !requestIdOld.equals(requestIdNew)) {
                requestIdOld.getPaymentRequestInstallmentsList().remove(paymentRequestInstallments);
                requestIdOld = em.merge(requestIdOld);
            }
            if (requestIdNew != null && !requestIdNew.equals(requestIdOld)) {
                requestIdNew.getPaymentRequestInstallmentsList().add(paymentRequestInstallments);
                requestIdNew = em.merge(requestIdNew);
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
                Object id = paymentRequestInstallments.getId();
                if (findPaymentRequestInstallments(id) == null) {
                    throw new NonexistentEntityException("The paymentRequestInstallments with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Object id) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentRequestInstallments paymentRequestInstallments;
            try {
                paymentRequestInstallments = em.getReference(PaymentRequestInstallments.class, id);
                paymentRequestInstallments.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The paymentRequestInstallments with id " + id + " no longer exists.", enfe);
            }
            PaymentRequests requestId = paymentRequestInstallments.getRequestId();
            if (requestId != null) {
                requestId.getPaymentRequestInstallmentsList().remove(paymentRequestInstallments);
                requestId = em.merge(requestId);
            }
            em.remove(paymentRequestInstallments);
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

    public List<PaymentRequestInstallments> findPaymentRequestInstallmentsEntities() {
        return findPaymentRequestInstallmentsEntities(true, -1, -1);
    }

    public List<PaymentRequestInstallments> findPaymentRequestInstallmentsEntities(int maxResults, int firstResult) {
        return findPaymentRequestInstallmentsEntities(false, maxResults, firstResult);
    }

    private List<PaymentRequestInstallments> findPaymentRequestInstallmentsEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(PaymentRequestInstallments.class));
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

    public PaymentRequestInstallments findPaymentRequestInstallments(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(PaymentRequestInstallments.class, id);
        } finally {
            em.close();
        }
    }

    public int getPaymentRequestInstallmentsCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<PaymentRequestInstallments> rt = cq.from(PaymentRequestInstallments.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
