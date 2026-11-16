/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.IllegalOrphanException;
import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import com.mycompany.jsf.security.ScheduleConfirmationStatus;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.PaymentRequests;
import entities.Clients;
import entities.Users;
import entities.Installments;
import entities.PaymentSchedules;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fabri
 */
public class PaymentSchedulesJpaController implements Serializable {

    public PaymentSchedulesJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    private void rollbackQuietly() {
        try {
            utx.rollback();
        } catch (Exception ignored) {
            // Transaction déjà annulée par le conteneur : on conserve la cause réelle.
        }
    }

    private void ensureNotLocked(PaymentSchedules schedule) {
        if (ScheduleConfirmationStatus.CONFIRMED.equals(
                ScheduleConfirmationStatus.normalize(schedule.getConfirmationStatus()))) {
            throw new IllegalStateException(
                    "Un échéancier validé par le gestionnaire ne peut plus être modifié ni supprimé.");
        }
    }

    private void validateConfirmationStatus(PaymentSchedules paymentSchedules) {
        String status = ScheduleConfirmationStatus.normalize(
                paymentSchedules.getConfirmationStatus());
        if (!ScheduleConfirmationStatus.isAllowed(status)) {
            throw new IllegalArgumentException(
                    "Le statut de confirmation doit être AWAITING_CLIENT ou CONFIRMED.");
        }
        paymentSchedules.setConfirmationStatus(status);
    }

    public void create(PaymentSchedules paymentSchedules) throws PreexistingEntityException, RollbackFailureException, Exception {
        validateConfirmationStatus(paymentSchedules);
        if (paymentSchedules.getInstallmentsList() == null) {
            paymentSchedules.setInstallmentsList(new ArrayList<Installments>());
        }
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentRequests paymentRequests = paymentSchedules.getPaymentRequests();
            if (paymentRequests != null) {
                paymentRequests = em.find(PaymentRequests.class, paymentRequests.getId());
                if (paymentRequests == null) {
                    throw new IllegalArgumentException("La demande sélectionnée n’existe plus.");
                }
                if (!"APPROVED".equalsIgnoreCase(paymentRequests.getStatus())) {
                    throw new IllegalStateException(
                            "La demande doit être approuvée depuis la file gestionnaire.");
                }
                if (paymentRequests.getScheduleId() != null) {
                    throw new IllegalStateException(
                            "Cette demande est déjà associée à un échéancier.");
                }
                paymentSchedules.setPaymentRequests(paymentRequests);
            }
            Clients clientId = paymentSchedules.getClientId();
            if (clientId != null) {
                clientId = em.getReference(clientId.getClass(), clientId.getId());
                paymentSchedules.setClientId(clientId);
            }
            Users managerId = paymentSchedules.getManagerId();
            if (managerId != null) {
                managerId = em.getReference(managerId.getClass(), managerId.getId());
                paymentSchedules.setManagerId(managerId);
            }
            List<Installments> attachedInstallmentsList = new ArrayList<Installments>();
            for (Installments installmentsListInstallmentsToAttach : paymentSchedules.getInstallmentsList()) {
                installmentsListInstallmentsToAttach = em.getReference(installmentsListInstallmentsToAttach.getClass(), installmentsListInstallmentsToAttach.getId());
                attachedInstallmentsList.add(installmentsListInstallmentsToAttach);
            }
            paymentSchedules.setInstallmentsList(attachedInstallmentsList);
            em.persist(paymentSchedules);
            if (paymentRequests != null) {
                paymentRequests.setScheduleId(paymentSchedules);
                paymentRequests = em.merge(paymentRequests);
            }
            if (clientId != null) {
                clientId.getPaymentSchedulesList().add(paymentSchedules);
                clientId = em.merge(clientId);
            }
            if (managerId != null) {
                managerId.getPaymentSchedulesList().add(paymentSchedules);
                managerId = em.merge(managerId);
            }
            for (Installments installmentsListInstallments : paymentSchedules.getInstallmentsList()) {
                PaymentSchedules oldScheduleIdOfInstallmentsListInstallments = installmentsListInstallments.getScheduleId();
                installmentsListInstallments.setScheduleId(paymentSchedules);
                installmentsListInstallments = em.merge(installmentsListInstallments);
                if (oldScheduleIdOfInstallmentsListInstallments != null) {
                    oldScheduleIdOfInstallmentsListInstallments.getInstallmentsList().remove(installmentsListInstallments);
                    oldScheduleIdOfInstallmentsListInstallments = em.merge(oldScheduleIdOfInstallmentsListInstallments);
                }
            }
            utx.commit();
        } catch (Exception ex) {
            rollbackQuietly();
            if (paymentSchedules.getId() != null
                    && findPaymentSchedules(paymentSchedules.getId()) != null) {
                throw new PreexistingEntityException("PaymentSchedules " + paymentSchedules + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(PaymentSchedules paymentSchedules) throws IllegalOrphanException, NonexistentEntityException, RollbackFailureException, Exception {
        validateConfirmationStatus(paymentSchedules);
        ensureNotLocked(paymentSchedules);
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            PaymentSchedules persistentPaymentSchedules = em.find(PaymentSchedules.class, paymentSchedules.getId());
            PaymentRequests paymentRequestsOld = persistentPaymentSchedules.getPaymentRequests();
            PaymentRequests paymentRequestsNew = paymentSchedules.getPaymentRequests();
            Clients clientIdOld = persistentPaymentSchedules.getClientId();
            Clients clientIdNew = paymentSchedules.getClientId();
            Users managerIdOld = persistentPaymentSchedules.getManagerId();
            Users managerIdNew = paymentSchedules.getManagerId();
            List<Installments> installmentsListOld = persistentPaymentSchedules.getInstallmentsList();
            List<Installments> installmentsListNew = paymentSchedules.getInstallmentsList();
            List<String> illegalOrphanMessages = null;
            for (Installments installmentsListOldInstallments : installmentsListOld) {
                if (!installmentsListNew.contains(installmentsListOldInstallments)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Installments " + installmentsListOldInstallments + " since its scheduleId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (paymentRequestsNew != null) {
                paymentRequestsNew = em.find(PaymentRequests.class, paymentRequestsNew.getId());
                if (paymentRequestsNew == null) {
                    throw new IllegalArgumentException("La demande sélectionnée n’existe plus.");
                }
                if (!"APPROVED".equalsIgnoreCase(paymentRequestsNew.getStatus())) {
                    throw new IllegalStateException(
                            "La demande doit être approuvée depuis la file gestionnaire.");
                }
                PaymentSchedules linkedSchedule = paymentRequestsNew.getScheduleId();
                if (linkedSchedule != null
                        && !linkedSchedule.equals(persistentPaymentSchedules)) {
                    throw new IllegalStateException(
                            "Cette demande est déjà associée à un autre échéancier.");
                }
                paymentSchedules.setPaymentRequests(paymentRequestsNew);
            }
            if (clientIdNew != null) {
                clientIdNew = em.getReference(clientIdNew.getClass(), clientIdNew.getId());
                paymentSchedules.setClientId(clientIdNew);
            }
            if (managerIdNew != null) {
                managerIdNew = em.getReference(managerIdNew.getClass(), managerIdNew.getId());
                paymentSchedules.setManagerId(managerIdNew);
            }
            List<Installments> attachedInstallmentsListNew = new ArrayList<Installments>();
            for (Installments installmentsListNewInstallmentsToAttach : installmentsListNew) {
                installmentsListNewInstallmentsToAttach = em.getReference(installmentsListNewInstallmentsToAttach.getClass(), installmentsListNewInstallmentsToAttach.getId());
                attachedInstallmentsListNew.add(installmentsListNewInstallmentsToAttach);
            }
            installmentsListNew = attachedInstallmentsListNew;
            paymentSchedules.setInstallmentsList(installmentsListNew);
            paymentSchedules = em.merge(paymentSchedules);
            if (paymentRequestsOld != null && !paymentRequestsOld.equals(paymentRequestsNew)) {
                paymentRequestsOld.setScheduleId(null);
                paymentRequestsOld = em.merge(paymentRequestsOld);
            }
            if (paymentRequestsNew != null && !paymentRequestsNew.equals(paymentRequestsOld)) {
                PaymentSchedules oldScheduleIdOfPaymentRequests = paymentRequestsNew.getScheduleId();
                if (oldScheduleIdOfPaymentRequests != null) {
                    oldScheduleIdOfPaymentRequests.setPaymentRequests(null);
                    oldScheduleIdOfPaymentRequests = em.merge(oldScheduleIdOfPaymentRequests);
                }
                paymentRequestsNew.setScheduleId(paymentSchedules);
                paymentRequestsNew = em.merge(paymentRequestsNew);
            }
            if (clientIdOld != null && !clientIdOld.equals(clientIdNew)) {
                clientIdOld.getPaymentSchedulesList().remove(paymentSchedules);
                clientIdOld = em.merge(clientIdOld);
            }
            if (clientIdNew != null && !clientIdNew.equals(clientIdOld)) {
                clientIdNew.getPaymentSchedulesList().add(paymentSchedules);
                clientIdNew = em.merge(clientIdNew);
            }
            if (managerIdOld != null && !managerIdOld.equals(managerIdNew)) {
                managerIdOld.getPaymentSchedulesList().remove(paymentSchedules);
                managerIdOld = em.merge(managerIdOld);
            }
            if (managerIdNew != null && !managerIdNew.equals(managerIdOld)) {
                managerIdNew.getPaymentSchedulesList().add(paymentSchedules);
                managerIdNew = em.merge(managerIdNew);
            }
            for (Installments installmentsListNewInstallments : installmentsListNew) {
                if (!installmentsListOld.contains(installmentsListNewInstallments)) {
                    PaymentSchedules oldScheduleIdOfInstallmentsListNewInstallments = installmentsListNewInstallments.getScheduleId();
                    installmentsListNewInstallments.setScheduleId(paymentSchedules);
                    installmentsListNewInstallments = em.merge(installmentsListNewInstallments);
                    if (oldScheduleIdOfInstallmentsListNewInstallments != null && !oldScheduleIdOfInstallmentsListNewInstallments.equals(paymentSchedules)) {
                        oldScheduleIdOfInstallmentsListNewInstallments.getInstallmentsList().remove(installmentsListNewInstallments);
                        oldScheduleIdOfInstallmentsListNewInstallments = em.merge(oldScheduleIdOfInstallmentsListNewInstallments);
                    }
                }
            }
            utx.commit();
        } catch (Exception ex) {
            rollbackQuietly();
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Object id = paymentSchedules.getId();
                if (findPaymentSchedules(id) == null) {
                    throw new NonexistentEntityException("The paymentSchedules with id " + id + " no longer exists.");
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
            PaymentSchedules paymentSchedules;
            try {
                paymentSchedules = em.getReference(PaymentSchedules.class, id);
                paymentSchedules.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The paymentSchedules with id " + id + " no longer exists.", enfe);
            }
            ensureNotLocked(paymentSchedules);
            List<String> illegalOrphanMessages = null;
            List<Installments> installmentsListOrphanCheck = paymentSchedules.getInstallmentsList();
            for (Installments installmentsListOrphanCheckInstallments : installmentsListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This PaymentSchedules (" + paymentSchedules + ") cannot be destroyed since the Installments " + installmentsListOrphanCheckInstallments + " in its installmentsList field has a non-nullable scheduleId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            PaymentRequests paymentRequests = paymentSchedules.getPaymentRequests();
            if (paymentRequests != null) {
                paymentRequests.setScheduleId(null);
                paymentRequests = em.merge(paymentRequests);
            }
            Clients clientId = paymentSchedules.getClientId();
            if (clientId != null) {
                clientId.getPaymentSchedulesList().remove(paymentSchedules);
                clientId = em.merge(clientId);
            }
            Users managerId = paymentSchedules.getManagerId();
            if (managerId != null) {
                managerId.getPaymentSchedulesList().remove(paymentSchedules);
                managerId = em.merge(managerId);
            }
            em.remove(paymentSchedules);
            utx.commit();
        } catch (Exception ex) {
            rollbackQuietly();
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<PaymentSchedules> findPaymentSchedulesEntities() {
        return findPaymentSchedulesEntities(true, -1, -1);
    }

    public List<PaymentSchedules> findPaymentSchedulesEntities(int maxResults, int firstResult) {
        return findPaymentSchedulesEntities(false, maxResults, firstResult);
    }

    private List<PaymentSchedules> findPaymentSchedulesEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(PaymentSchedules.class));
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

    public PaymentSchedules findPaymentSchedules(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(PaymentSchedules.class, id);
        } finally {
            em.close();
        }
    }

    /** Charge l'échéancier avec ses échéances (collection paresseuse initialisée). */
    public PaymentSchedules findPaymentSchedulesWithInstallments(Object id) {
        EntityManager em = getEntityManager();
        try {
            PaymentSchedules schedule = em.find(PaymentSchedules.class, id);
            if (schedule != null) {
                schedule.getInstallmentsList().size();
            }
            return schedule;
        } finally {
            em.close();
        }
    }

    public int getPaymentSchedulesCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<PaymentSchedules> rt = cq.from(PaymentSchedules.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
