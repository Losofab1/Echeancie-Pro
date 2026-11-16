/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.IllegalOrphanException;
import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import entities.Clients;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.PaymentRequests;
import java.util.ArrayList;
import java.util.List;
import entities.PaymentSchedules;
import entities.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.UserTransaction;

/**
 *
 * @author fabri
 */
public class ClientsJpaController implements Serializable {

    public ClientsJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Clients clients) throws PreexistingEntityException, RollbackFailureException, Exception {
        if (clients.getPaymentRequestsList() == null) {
            clients.setPaymentRequestsList(new ArrayList<PaymentRequests>());
        }
        if (clients.getPaymentSchedulesList() == null) {
            clients.setPaymentSchedulesList(new ArrayList<PaymentSchedules>());
        }
        if (clients.getUsersList() == null) {
            clients.setUsersList(new ArrayList<Users>());
        }
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            List<PaymentRequests> attachedPaymentRequestsList = new ArrayList<PaymentRequests>();
            for (PaymentRequests paymentRequestsListPaymentRequestsToAttach : clients.getPaymentRequestsList()) {
                paymentRequestsListPaymentRequestsToAttach = em.getReference(paymentRequestsListPaymentRequestsToAttach.getClass(), paymentRequestsListPaymentRequestsToAttach.getId());
                attachedPaymentRequestsList.add(paymentRequestsListPaymentRequestsToAttach);
            }
            clients.setPaymentRequestsList(attachedPaymentRequestsList);
            List<PaymentSchedules> attachedPaymentSchedulesList = new ArrayList<PaymentSchedules>();
            for (PaymentSchedules paymentSchedulesListPaymentSchedulesToAttach : clients.getPaymentSchedulesList()) {
                paymentSchedulesListPaymentSchedulesToAttach = em.getReference(paymentSchedulesListPaymentSchedulesToAttach.getClass(), paymentSchedulesListPaymentSchedulesToAttach.getId());
                attachedPaymentSchedulesList.add(paymentSchedulesListPaymentSchedulesToAttach);
            }
            clients.setPaymentSchedulesList(attachedPaymentSchedulesList);
            List<Users> attachedUsersList = new ArrayList<Users>();
            for (Users usersListUsersToAttach : clients.getUsersList()) {
                usersListUsersToAttach = em.getReference(usersListUsersToAttach.getClass(), usersListUsersToAttach.getId());
                attachedUsersList.add(usersListUsersToAttach);
            }
            clients.setUsersList(attachedUsersList);
            em.persist(clients);
            for (PaymentRequests paymentRequestsListPaymentRequests : clients.getPaymentRequestsList()) {
                Clients oldClientIdOfPaymentRequestsListPaymentRequests = paymentRequestsListPaymentRequests.getClientId();
                paymentRequestsListPaymentRequests.setClientId(clients);
                paymentRequestsListPaymentRequests = em.merge(paymentRequestsListPaymentRequests);
                if (oldClientIdOfPaymentRequestsListPaymentRequests != null) {
                    oldClientIdOfPaymentRequestsListPaymentRequests.getPaymentRequestsList().remove(paymentRequestsListPaymentRequests);
                    oldClientIdOfPaymentRequestsListPaymentRequests = em.merge(oldClientIdOfPaymentRequestsListPaymentRequests);
                }
            }
            for (PaymentSchedules paymentSchedulesListPaymentSchedules : clients.getPaymentSchedulesList()) {
                Clients oldClientIdOfPaymentSchedulesListPaymentSchedules = paymentSchedulesListPaymentSchedules.getClientId();
                paymentSchedulesListPaymentSchedules.setClientId(clients);
                paymentSchedulesListPaymentSchedules = em.merge(paymentSchedulesListPaymentSchedules);
                if (oldClientIdOfPaymentSchedulesListPaymentSchedules != null) {
                    oldClientIdOfPaymentSchedulesListPaymentSchedules.getPaymentSchedulesList().remove(paymentSchedulesListPaymentSchedules);
                    oldClientIdOfPaymentSchedulesListPaymentSchedules = em.merge(oldClientIdOfPaymentSchedulesListPaymentSchedules);
                }
            }
            for (Users usersListUsers : clients.getUsersList()) {
                Clients oldClientIdOfUsersListUsers = usersListUsers.getClientId();
                usersListUsers.setClientId(clients);
                usersListUsers = em.merge(usersListUsers);
                if (oldClientIdOfUsersListUsers != null) {
                    oldClientIdOfUsersListUsers.getUsersList().remove(usersListUsers);
                    oldClientIdOfUsersListUsers = em.merge(oldClientIdOfUsersListUsers);
                }
            }
            utx.commit();
        } catch (Exception ex) {
            try {
                utx.rollback();
            } catch (Exception re) {
                throw new RollbackFailureException("An error occurred attempting to roll back the transaction.", re);
            }
            if (findClients(clients.getId()) != null) {
                throw new PreexistingEntityException("Clients " + clients + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Clients clients) throws IllegalOrphanException, NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Clients persistentClients = em.find(Clients.class, clients.getId());
            List<PaymentRequests> paymentRequestsListOld = persistentClients.getPaymentRequestsList();
            List<PaymentRequests> paymentRequestsListNew = clients.getPaymentRequestsList();
            List<PaymentSchedules> paymentSchedulesListOld = persistentClients.getPaymentSchedulesList();
            List<PaymentSchedules> paymentSchedulesListNew = clients.getPaymentSchedulesList();
            List<Users> usersListOld = persistentClients.getUsersList();
            List<Users> usersListNew = clients.getUsersList();
            List<String> illegalOrphanMessages = null;
            for (PaymentRequests paymentRequestsListOldPaymentRequests : paymentRequestsListOld) {
                if (!paymentRequestsListNew.contains(paymentRequestsListOldPaymentRequests)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain PaymentRequests " + paymentRequestsListOldPaymentRequests + " since its clientId field is not nullable.");
                }
            }
            for (PaymentSchedules paymentSchedulesListOldPaymentSchedules : paymentSchedulesListOld) {
                if (!paymentSchedulesListNew.contains(paymentSchedulesListOldPaymentSchedules)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain PaymentSchedules " + paymentSchedulesListOldPaymentSchedules + " since its clientId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            List<PaymentRequests> attachedPaymentRequestsListNew = new ArrayList<PaymentRequests>();
            for (PaymentRequests paymentRequestsListNewPaymentRequestsToAttach : paymentRequestsListNew) {
                paymentRequestsListNewPaymentRequestsToAttach = em.getReference(paymentRequestsListNewPaymentRequestsToAttach.getClass(), paymentRequestsListNewPaymentRequestsToAttach.getId());
                attachedPaymentRequestsListNew.add(paymentRequestsListNewPaymentRequestsToAttach);
            }
            paymentRequestsListNew = attachedPaymentRequestsListNew;
            clients.setPaymentRequestsList(paymentRequestsListNew);
            List<PaymentSchedules> attachedPaymentSchedulesListNew = new ArrayList<PaymentSchedules>();
            for (PaymentSchedules paymentSchedulesListNewPaymentSchedulesToAttach : paymentSchedulesListNew) {
                paymentSchedulesListNewPaymentSchedulesToAttach = em.getReference(paymentSchedulesListNewPaymentSchedulesToAttach.getClass(), paymentSchedulesListNewPaymentSchedulesToAttach.getId());
                attachedPaymentSchedulesListNew.add(paymentSchedulesListNewPaymentSchedulesToAttach);
            }
            paymentSchedulesListNew = attachedPaymentSchedulesListNew;
            clients.setPaymentSchedulesList(paymentSchedulesListNew);
            List<Users> attachedUsersListNew = new ArrayList<Users>();
            for (Users usersListNewUsersToAttach : usersListNew) {
                usersListNewUsersToAttach = em.getReference(usersListNewUsersToAttach.getClass(), usersListNewUsersToAttach.getId());
                attachedUsersListNew.add(usersListNewUsersToAttach);
            }
            usersListNew = attachedUsersListNew;
            clients.setUsersList(usersListNew);
            clients = em.merge(clients);
            for (PaymentRequests paymentRequestsListNewPaymentRequests : paymentRequestsListNew) {
                if (!paymentRequestsListOld.contains(paymentRequestsListNewPaymentRequests)) {
                    Clients oldClientIdOfPaymentRequestsListNewPaymentRequests = paymentRequestsListNewPaymentRequests.getClientId();
                    paymentRequestsListNewPaymentRequests.setClientId(clients);
                    paymentRequestsListNewPaymentRequests = em.merge(paymentRequestsListNewPaymentRequests);
                    if (oldClientIdOfPaymentRequestsListNewPaymentRequests != null && !oldClientIdOfPaymentRequestsListNewPaymentRequests.equals(clients)) {
                        oldClientIdOfPaymentRequestsListNewPaymentRequests.getPaymentRequestsList().remove(paymentRequestsListNewPaymentRequests);
                        oldClientIdOfPaymentRequestsListNewPaymentRequests = em.merge(oldClientIdOfPaymentRequestsListNewPaymentRequests);
                    }
                }
            }
            for (PaymentSchedules paymentSchedulesListNewPaymentSchedules : paymentSchedulesListNew) {
                if (!paymentSchedulesListOld.contains(paymentSchedulesListNewPaymentSchedules)) {
                    Clients oldClientIdOfPaymentSchedulesListNewPaymentSchedules = paymentSchedulesListNewPaymentSchedules.getClientId();
                    paymentSchedulesListNewPaymentSchedules.setClientId(clients);
                    paymentSchedulesListNewPaymentSchedules = em.merge(paymentSchedulesListNewPaymentSchedules);
                    if (oldClientIdOfPaymentSchedulesListNewPaymentSchedules != null && !oldClientIdOfPaymentSchedulesListNewPaymentSchedules.equals(clients)) {
                        oldClientIdOfPaymentSchedulesListNewPaymentSchedules.getPaymentSchedulesList().remove(paymentSchedulesListNewPaymentSchedules);
                        oldClientIdOfPaymentSchedulesListNewPaymentSchedules = em.merge(oldClientIdOfPaymentSchedulesListNewPaymentSchedules);
                    }
                }
            }
            for (Users usersListOldUsers : usersListOld) {
                if (!usersListNew.contains(usersListOldUsers)) {
                    usersListOldUsers.setClientId(null);
                    usersListOldUsers = em.merge(usersListOldUsers);
                }
            }
            for (Users usersListNewUsers : usersListNew) {
                if (!usersListOld.contains(usersListNewUsers)) {
                    Clients oldClientIdOfUsersListNewUsers = usersListNewUsers.getClientId();
                    usersListNewUsers.setClientId(clients);
                    usersListNewUsers = em.merge(usersListNewUsers);
                    if (oldClientIdOfUsersListNewUsers != null && !oldClientIdOfUsersListNewUsers.equals(clients)) {
                        oldClientIdOfUsersListNewUsers.getUsersList().remove(usersListNewUsers);
                        oldClientIdOfUsersListNewUsers = em.merge(oldClientIdOfUsersListNewUsers);
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
                Object id = clients.getId();
                if (findClients(id) == null) {
                    throw new NonexistentEntityException("The clients with id " + id + " no longer exists.");
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
            Clients clients;
            try {
                clients = em.getReference(Clients.class, id);
                clients.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The clients with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            List<PaymentRequests> paymentRequestsListOrphanCheck = clients.getPaymentRequestsList();
            for (PaymentRequests paymentRequestsListOrphanCheckPaymentRequests : paymentRequestsListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Clients (" + clients + ") cannot be destroyed since the PaymentRequests " + paymentRequestsListOrphanCheckPaymentRequests + " in its paymentRequestsList field has a non-nullable clientId field.");
            }
            List<PaymentSchedules> paymentSchedulesListOrphanCheck = clients.getPaymentSchedulesList();
            for (PaymentSchedules paymentSchedulesListOrphanCheckPaymentSchedules : paymentSchedulesListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Clients (" + clients + ") cannot be destroyed since the PaymentSchedules " + paymentSchedulesListOrphanCheckPaymentSchedules + " in its paymentSchedulesList field has a non-nullable clientId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            List<Users> usersList = clients.getUsersList();
            for (Users usersListUsers : usersList) {
                usersListUsers.setClientId(null);
                usersListUsers = em.merge(usersListUsers);
            }
            em.remove(clients);
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

    public List<Clients> findClientsEntities() {
        return findClientsEntities(true, -1, -1);
    }

    public List<Clients> findClientsEntities(int maxResults, int firstResult) {
        return findClientsEntities(false, maxResults, firstResult);
    }

    private List<Clients> findClientsEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Clients.class));
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

    public Clients findClients(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Clients.class, id);
        } finally {
            em.close();
        }
    }

    public int getClientsCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Clients> rt = cq.from(Clients.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
