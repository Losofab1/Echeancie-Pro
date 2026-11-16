/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import entities.ClientCreationRequests;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.UserTransaction;
import java.util.List;

/**
 *
 * @author fabri
 */
public class ClientCreationRequestsJpaController implements Serializable {

    public ClientCreationRequestsJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(ClientCreationRequests clientCreationRequests) throws PreexistingEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Users createdBy = clientCreationRequests.getCreatedBy();
            if (createdBy != null) {
                createdBy = em.getReference(createdBy.getClass(), createdBy.getId());
                clientCreationRequests.setCreatedBy(createdBy);
            }
            Users reviewedBy = clientCreationRequests.getReviewedBy();
            if (reviewedBy != null) {
                reviewedBy = em.getReference(reviewedBy.getClass(), reviewedBy.getId());
                clientCreationRequests.setReviewedBy(reviewedBy);
            }
            em.persist(clientCreationRequests);
            if (createdBy != null) {
                createdBy.getClientCreationRequestsList().add(clientCreationRequests);
                createdBy = em.merge(createdBy);
            }
            if (reviewedBy != null) {
                reviewedBy.getClientCreationRequestsList().add(clientCreationRequests);
                reviewedBy = em.merge(reviewedBy);
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findClientCreationRequests(clientCreationRequests.getId()) != null) {
                throw new PreexistingEntityException("ClientCreationRequests " + clientCreationRequests + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(ClientCreationRequests clientCreationRequests) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            ClientCreationRequests persistentClientCreationRequests = em.find(ClientCreationRequests.class, clientCreationRequests.getId());
            Users createdByOld = persistentClientCreationRequests.getCreatedBy();
            Users createdByNew = clientCreationRequests.getCreatedBy();
            Users reviewedByOld = persistentClientCreationRequests.getReviewedBy();
            Users reviewedByNew = clientCreationRequests.getReviewedBy();
            if (createdByNew != null) {
                createdByNew = em.getReference(createdByNew.getClass(), createdByNew.getId());
                clientCreationRequests.setCreatedBy(createdByNew);
            }
            if (reviewedByNew != null) {
                reviewedByNew = em.getReference(reviewedByNew.getClass(), reviewedByNew.getId());
                clientCreationRequests.setReviewedBy(reviewedByNew);
            }
            clientCreationRequests = em.merge(clientCreationRequests);
            if (createdByOld != null && !createdByOld.equals(createdByNew)) {
                createdByOld.getClientCreationRequestsList().remove(clientCreationRequests);
                createdByOld = em.merge(createdByOld);
            }
            if (createdByNew != null && !createdByNew.equals(createdByOld)) {
                createdByNew.getClientCreationRequestsList().add(clientCreationRequests);
                createdByNew = em.merge(createdByNew);
            }
            if (reviewedByOld != null && !reviewedByOld.equals(reviewedByNew)) {
                reviewedByOld.getClientCreationRequestsList().remove(clientCreationRequests);
                reviewedByOld = em.merge(reviewedByOld);
            }
            if (reviewedByNew != null && !reviewedByNew.equals(reviewedByOld)) {
                reviewedByNew.getClientCreationRequestsList().add(clientCreationRequests);
                reviewedByNew = em.merge(reviewedByNew);
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
                Object id = clientCreationRequests.getId();
                if (findClientCreationRequests(id) == null) {
                    throw new NonexistentEntityException("The clientCreationRequests with id " + id + " no longer exists.");
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
            ClientCreationRequests clientCreationRequests;
            try {
                clientCreationRequests = em.getReference(ClientCreationRequests.class, id);
                clientCreationRequests.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The clientCreationRequests with id " + id + " no longer exists.", enfe);
            }
            Users createdBy = clientCreationRequests.getCreatedBy();
            if (createdBy != null) {
                createdBy.getClientCreationRequestsList().remove(clientCreationRequests);
                createdBy = em.merge(createdBy);
            }
            Users reviewedBy = clientCreationRequests.getReviewedBy();
            if (reviewedBy != null) {
                reviewedBy.getClientCreationRequestsList().remove(clientCreationRequests);
                reviewedBy = em.merge(reviewedBy);
            }
            em.remove(clientCreationRequests);
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

    public List<ClientCreationRequests> findClientCreationRequestsEntities() {
        return findClientCreationRequestsEntities(true, -1, -1);
    }

    public List<ClientCreationRequests> findClientCreationRequestsEntities(int maxResults, int firstResult) {
        return findClientCreationRequestsEntities(false, maxResults, firstResult);
    }

    private List<ClientCreationRequests> findClientCreationRequestsEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(ClientCreationRequests.class));
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

    public ClientCreationRequests findClientCreationRequests(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(ClientCreationRequests.class, id);
        } finally {
            em.close();
        }
    }

    public int getClientCreationRequestsCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<ClientCreationRequests> rt = cq.from(ClientCreationRequests.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
