/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import entities.FlywaySchemaHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.UserTransaction;
import java.util.List;

/**
 *
 * @author fabri
 */
public class FlywaySchemaHistoryJpaController implements Serializable {

    public FlywaySchemaHistoryJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(FlywaySchemaHistory flywaySchemaHistory) throws PreexistingEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            em.persist(flywaySchemaHistory);
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findFlywaySchemaHistory(flywaySchemaHistory.getInstalledRank()) != null) {
                throw new PreexistingEntityException("FlywaySchemaHistory " + flywaySchemaHistory + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(FlywaySchemaHistory flywaySchemaHistory) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            flywaySchemaHistory = em.merge(flywaySchemaHistory);
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = flywaySchemaHistory.getInstalledRank();
                if (findFlywaySchemaHistory(id) == null) {
                    throw new NonexistentEntityException("The flywaySchemaHistory with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            FlywaySchemaHistory flywaySchemaHistory;
            try {
                flywaySchemaHistory = em.getReference(FlywaySchemaHistory.class, id);
                flywaySchemaHistory.getInstalledRank();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The flywaySchemaHistory with id " + id + " no longer exists.", enfe);
            }
            em.remove(flywaySchemaHistory);
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

    public List<FlywaySchemaHistory> findFlywaySchemaHistoryEntities() {
        return findFlywaySchemaHistoryEntities(true, -1, -1);
    }

    public List<FlywaySchemaHistory> findFlywaySchemaHistoryEntities(int maxResults, int firstResult) {
        return findFlywaySchemaHistoryEntities(false, maxResults, firstResult);
    }

    private List<FlywaySchemaHistory> findFlywaySchemaHistoryEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(FlywaySchemaHistory.class));
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

    public FlywaySchemaHistory findFlywaySchemaHistory(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(FlywaySchemaHistory.class, id);
        } finally {
            em.close();
        }
    }

    public int getFlywaySchemaHistoryCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<FlywaySchemaHistory> rt = cq.from(FlywaySchemaHistory.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
