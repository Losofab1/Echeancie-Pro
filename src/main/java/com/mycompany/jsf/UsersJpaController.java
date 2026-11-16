/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import com.mycompany.jsf.exceptions.IllegalOrphanException;
import com.mycompany.jsf.exceptions.NonexistentEntityException;
import com.mycompany.jsf.exceptions.PreexistingEntityException;
import com.mycompany.jsf.exceptions.RollbackFailureException;
import com.mycompany.jsf.security.RoleNames;
import java.io.Serializable;
import jakarta.persistence.Query;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import entities.Clients;
import entities.ClientCreationRequests;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import entities.PaymentRequests;
import entities.PaymentSchedules;
import entities.PaymentSubmissions;
import entities.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.transaction.UserTransaction;

/**
 *
 * @author fabri
 */
public class UsersJpaController implements Serializable {

    public record AccountDeletionSummary(int deleted, int retained) {
    }

    public UsersJpaController(UserTransaction utx, EntityManagerFactory emf) {
        this.utx = utx;
        this.emf = emf;
    }
    private UserTransaction utx = null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Annule la transaction sans masquer l'exception d'origine : quand {@code commit()}
     * échoue, le conteneur a souvent déjà annulé la transaction et un second
     * {@code rollback()} lève IllegalStateException.
     */
    private void rollbackQuietly() {
        try {
            utx.rollback();
        } catch (Exception ignored) {
            // La transaction est déjà annulée ; on conserve la cause réelle de l'échec.
        }
    }

    public void create(Users users) throws PreexistingEntityException, RollbackFailureException, Exception {
        if (users.getClientCreationRequestsList() == null) {
            users.setClientCreationRequestsList(new ArrayList<ClientCreationRequests>());
        }
        if (users.getClientCreationRequestsList1() == null) {
            users.setClientCreationRequestsList1(new ArrayList<ClientCreationRequests>());
        }
        if (users.getPaymentRequestsList() == null) {
            users.setPaymentRequestsList(new ArrayList<PaymentRequests>());
        }
        if (users.getPaymentRequestsList1() == null) {
            users.setPaymentRequestsList1(new ArrayList<PaymentRequests>());
        }
        if (users.getPaymentSchedulesList() == null) {
            users.setPaymentSchedulesList(new ArrayList<PaymentSchedules>());
        }
        if (users.getPaymentSubmissionsList() == null) {
            users.setPaymentSubmissionsList(new ArrayList<PaymentSubmissions>());
        }
        if (users.getPaymentSubmissionsList1() == null) {
            users.setPaymentSubmissionsList1(new ArrayList<PaymentSubmissions>());
        }
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            Clients clientId = users.getClientId();
            if (clientId != null) {
                clientId = em.getReference(clientId.getClass(), clientId.getId());
                users.setClientId(clientId);
            }
            List<ClientCreationRequests> attachedClientCreationRequestsList = new ArrayList<ClientCreationRequests>();
            for (ClientCreationRequests clientCreationRequestsListClientCreationRequestsToAttach : users.getClientCreationRequestsList()) {
                clientCreationRequestsListClientCreationRequestsToAttach = em.getReference(clientCreationRequestsListClientCreationRequestsToAttach.getClass(), clientCreationRequestsListClientCreationRequestsToAttach.getId());
                attachedClientCreationRequestsList.add(clientCreationRequestsListClientCreationRequestsToAttach);
            }
            users.setClientCreationRequestsList(attachedClientCreationRequestsList);
            List<ClientCreationRequests> attachedClientCreationRequestsList1 = new ArrayList<ClientCreationRequests>();
            for (ClientCreationRequests clientCreationRequestsList1ClientCreationRequestsToAttach : users.getClientCreationRequestsList1()) {
                clientCreationRequestsList1ClientCreationRequestsToAttach = em.getReference(clientCreationRequestsList1ClientCreationRequestsToAttach.getClass(), clientCreationRequestsList1ClientCreationRequestsToAttach.getId());
                attachedClientCreationRequestsList1.add(clientCreationRequestsList1ClientCreationRequestsToAttach);
            }
            users.setClientCreationRequestsList1(attachedClientCreationRequestsList1);
            List<PaymentRequests> attachedPaymentRequestsList = new ArrayList<PaymentRequests>();
            for (PaymentRequests paymentRequestsListPaymentRequestsToAttach : users.getPaymentRequestsList()) {
                paymentRequestsListPaymentRequestsToAttach = em.getReference(paymentRequestsListPaymentRequestsToAttach.getClass(), paymentRequestsListPaymentRequestsToAttach.getId());
                attachedPaymentRequestsList.add(paymentRequestsListPaymentRequestsToAttach);
            }
            users.setPaymentRequestsList(attachedPaymentRequestsList);
            List<PaymentRequests> attachedPaymentRequestsList1 = new ArrayList<PaymentRequests>();
            for (PaymentRequests paymentRequestsList1PaymentRequestsToAttach : users.getPaymentRequestsList1()) {
                paymentRequestsList1PaymentRequestsToAttach = em.getReference(paymentRequestsList1PaymentRequestsToAttach.getClass(), paymentRequestsList1PaymentRequestsToAttach.getId());
                attachedPaymentRequestsList1.add(paymentRequestsList1PaymentRequestsToAttach);
            }
            users.setPaymentRequestsList1(attachedPaymentRequestsList1);
            List<PaymentSchedules> attachedPaymentSchedulesList = new ArrayList<PaymentSchedules>();
            for (PaymentSchedules paymentSchedulesListPaymentSchedulesToAttach : users.getPaymentSchedulesList()) {
                paymentSchedulesListPaymentSchedulesToAttach = em.getReference(paymentSchedulesListPaymentSchedulesToAttach.getClass(), paymentSchedulesListPaymentSchedulesToAttach.getId());
                attachedPaymentSchedulesList.add(paymentSchedulesListPaymentSchedulesToAttach);
            }
            users.setPaymentSchedulesList(attachedPaymentSchedulesList);
            List<PaymentSubmissions> attachedPaymentSubmissionsList = new ArrayList<PaymentSubmissions>();
            for (PaymentSubmissions paymentSubmissionsListPaymentSubmissionsToAttach : users.getPaymentSubmissionsList()) {
                paymentSubmissionsListPaymentSubmissionsToAttach = em.getReference(paymentSubmissionsListPaymentSubmissionsToAttach.getClass(), paymentSubmissionsListPaymentSubmissionsToAttach.getId());
                attachedPaymentSubmissionsList.add(paymentSubmissionsListPaymentSubmissionsToAttach);
            }
            users.setPaymentSubmissionsList(attachedPaymentSubmissionsList);
            List<PaymentSubmissions> attachedPaymentSubmissionsList1 = new ArrayList<PaymentSubmissions>();
            for (PaymentSubmissions paymentSubmissionsList1PaymentSubmissionsToAttach : users.getPaymentSubmissionsList1()) {
                paymentSubmissionsList1PaymentSubmissionsToAttach = em.getReference(paymentSubmissionsList1PaymentSubmissionsToAttach.getClass(), paymentSubmissionsList1PaymentSubmissionsToAttach.getId());
                attachedPaymentSubmissionsList1.add(paymentSubmissionsList1PaymentSubmissionsToAttach);
            }
            users.setPaymentSubmissionsList1(attachedPaymentSubmissionsList1);
            em.persist(users);
            if (clientId != null) {
                clientId.getUsersList().add(users);
                clientId = em.merge(clientId);
            }
            for (ClientCreationRequests clientCreationRequestsListClientCreationRequests : users.getClientCreationRequestsList()) {
                Users oldCreatedByOfClientCreationRequestsListClientCreationRequests = clientCreationRequestsListClientCreationRequests.getCreatedBy();
                clientCreationRequestsListClientCreationRequests.setCreatedBy(users);
                clientCreationRequestsListClientCreationRequests = em.merge(clientCreationRequestsListClientCreationRequests);
                if (oldCreatedByOfClientCreationRequestsListClientCreationRequests != null) {
                    oldCreatedByOfClientCreationRequestsListClientCreationRequests.getClientCreationRequestsList().remove(clientCreationRequestsListClientCreationRequests);
                    oldCreatedByOfClientCreationRequestsListClientCreationRequests = em.merge(oldCreatedByOfClientCreationRequestsListClientCreationRequests);
                }
            }
            for (ClientCreationRequests clientCreationRequestsList1ClientCreationRequests : users.getClientCreationRequestsList1()) {
                Users oldReviewedByOfClientCreationRequestsList1ClientCreationRequests = clientCreationRequestsList1ClientCreationRequests.getReviewedBy();
                clientCreationRequestsList1ClientCreationRequests.setReviewedBy(users);
                clientCreationRequestsList1ClientCreationRequests = em.merge(clientCreationRequestsList1ClientCreationRequests);
                if (oldReviewedByOfClientCreationRequestsList1ClientCreationRequests != null) {
                    oldReviewedByOfClientCreationRequestsList1ClientCreationRequests.getClientCreationRequestsList1().remove(clientCreationRequestsList1ClientCreationRequests);
                    oldReviewedByOfClientCreationRequestsList1ClientCreationRequests = em.merge(oldReviewedByOfClientCreationRequestsList1ClientCreationRequests);
                }
            }
            for (PaymentRequests paymentRequestsListPaymentRequests : users.getPaymentRequestsList()) {
                Users oldRequesterIdOfPaymentRequestsListPaymentRequests = paymentRequestsListPaymentRequests.getRequesterId();
                paymentRequestsListPaymentRequests.setRequesterId(users);
                paymentRequestsListPaymentRequests = em.merge(paymentRequestsListPaymentRequests);
                if (oldRequesterIdOfPaymentRequestsListPaymentRequests != null) {
                    oldRequesterIdOfPaymentRequestsListPaymentRequests.getPaymentRequestsList().remove(paymentRequestsListPaymentRequests);
                    oldRequesterIdOfPaymentRequestsListPaymentRequests = em.merge(oldRequesterIdOfPaymentRequestsListPaymentRequests);
                }
            }
            for (PaymentRequests paymentRequestsList1PaymentRequests : users.getPaymentRequestsList1()) {
                Users oldReviewedByOfPaymentRequestsList1PaymentRequests = paymentRequestsList1PaymentRequests.getReviewedBy();
                paymentRequestsList1PaymentRequests.setReviewedBy(users);
                paymentRequestsList1PaymentRequests = em.merge(paymentRequestsList1PaymentRequests);
                if (oldReviewedByOfPaymentRequestsList1PaymentRequests != null) {
                    oldReviewedByOfPaymentRequestsList1PaymentRequests.getPaymentRequestsList1().remove(paymentRequestsList1PaymentRequests);
                    oldReviewedByOfPaymentRequestsList1PaymentRequests = em.merge(oldReviewedByOfPaymentRequestsList1PaymentRequests);
                }
            }
            for (PaymentSchedules paymentSchedulesListPaymentSchedules : users.getPaymentSchedulesList()) {
                Users oldManagerIdOfPaymentSchedulesListPaymentSchedules = paymentSchedulesListPaymentSchedules.getManagerId();
                paymentSchedulesListPaymentSchedules.setManagerId(users);
                paymentSchedulesListPaymentSchedules = em.merge(paymentSchedulesListPaymentSchedules);
                if (oldManagerIdOfPaymentSchedulesListPaymentSchedules != null) {
                    oldManagerIdOfPaymentSchedulesListPaymentSchedules.getPaymentSchedulesList().remove(paymentSchedulesListPaymentSchedules);
                    oldManagerIdOfPaymentSchedulesListPaymentSchedules = em.merge(oldManagerIdOfPaymentSchedulesListPaymentSchedules);
                }
            }
            for (PaymentSubmissions paymentSubmissionsListPaymentSubmissions : users.getPaymentSubmissionsList()) {
                Users oldReviewedByOfPaymentSubmissionsListPaymentSubmissions = paymentSubmissionsListPaymentSubmissions.getReviewedBy();
                paymentSubmissionsListPaymentSubmissions.setReviewedBy(users);
                paymentSubmissionsListPaymentSubmissions = em.merge(paymentSubmissionsListPaymentSubmissions);
                if (oldReviewedByOfPaymentSubmissionsListPaymentSubmissions != null) {
                    oldReviewedByOfPaymentSubmissionsListPaymentSubmissions.getPaymentSubmissionsList().remove(paymentSubmissionsListPaymentSubmissions);
                    oldReviewedByOfPaymentSubmissionsListPaymentSubmissions = em.merge(oldReviewedByOfPaymentSubmissionsListPaymentSubmissions);
                }
            }
            for (PaymentSubmissions paymentSubmissionsList1PaymentSubmissions : users.getPaymentSubmissionsList1()) {
                Users oldSubmittedByOfPaymentSubmissionsList1PaymentSubmissions = paymentSubmissionsList1PaymentSubmissions.getSubmittedBy();
                paymentSubmissionsList1PaymentSubmissions.setSubmittedBy(users);
                paymentSubmissionsList1PaymentSubmissions = em.merge(paymentSubmissionsList1PaymentSubmissions);
                if (oldSubmittedByOfPaymentSubmissionsList1PaymentSubmissions != null) {
                    oldSubmittedByOfPaymentSubmissionsList1PaymentSubmissions.getPaymentSubmissionsList1().remove(paymentSubmissionsList1PaymentSubmissions);
                    oldSubmittedByOfPaymentSubmissionsList1PaymentSubmissions = em.merge(oldSubmittedByOfPaymentSubmissionsList1PaymentSubmissions);
                }
            }
            utx.commit();
        } catch (Exception ex) {
            rollbackQuietly();
            if (findUsers(users.getId()) != null) {
                throw new PreexistingEntityException("Users " + users + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Users users) throws IllegalOrphanException, NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            List<Users> administrators = lockAdministrators(em);
            List<Users> activeAdmins = administrators.stream()
                    .filter(Users::getActive)
                    .toList();
            Users persistentUsers = em.find(
                    Users.class, users.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (persistentUsers == null) {
                throw new NonexistentEntityException(
                        "The users with id " + users.getId() + " no longer exists.");
            }
            ensurePrimaryAdministratorProtected(
                    persistentUsers, administrators, users.getRole(), users.getActive());
            ensureActiveAdministratorRemains(
                    persistentUsers, activeAdmins, users.getRole(), users.getActive());
            Clients clientIdOld = persistentUsers.getClientId();
            Clients clientIdNew = users.getClientId();
            List<ClientCreationRequests> clientCreationRequestsListOld = persistentUsers.getClientCreationRequestsList();
            List<ClientCreationRequests> clientCreationRequestsListNew = users.getClientCreationRequestsList();
            List<ClientCreationRequests> clientCreationRequestsList1Old = persistentUsers.getClientCreationRequestsList1();
            List<ClientCreationRequests> clientCreationRequestsList1New = users.getClientCreationRequestsList1();
            List<PaymentRequests> paymentRequestsListOld = persistentUsers.getPaymentRequestsList();
            List<PaymentRequests> paymentRequestsListNew = users.getPaymentRequestsList();
            List<PaymentRequests> paymentRequestsList1Old = persistentUsers.getPaymentRequestsList1();
            List<PaymentRequests> paymentRequestsList1New = users.getPaymentRequestsList1();
            List<PaymentSchedules> paymentSchedulesListOld = persistentUsers.getPaymentSchedulesList();
            List<PaymentSchedules> paymentSchedulesListNew = users.getPaymentSchedulesList();
            List<PaymentSubmissions> paymentSubmissionsListOld = persistentUsers.getPaymentSubmissionsList();
            List<PaymentSubmissions> paymentSubmissionsListNew = users.getPaymentSubmissionsList();
            List<PaymentSubmissions> paymentSubmissionsList1Old = persistentUsers.getPaymentSubmissionsList1();
            List<PaymentSubmissions> paymentSubmissionsList1New = users.getPaymentSubmissionsList1();
            List<String> illegalOrphanMessages = null;
            for (PaymentRequests paymentRequestsListOldPaymentRequests : paymentRequestsListOld) {
                if (!paymentRequestsListNew.contains(paymentRequestsListOldPaymentRequests)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain PaymentRequests " + paymentRequestsListOldPaymentRequests + " since its requesterId field is not nullable.");
                }
            }
            for (PaymentSchedules paymentSchedulesListOldPaymentSchedules : paymentSchedulesListOld) {
                if (!paymentSchedulesListNew.contains(paymentSchedulesListOldPaymentSchedules)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain PaymentSchedules " + paymentSchedulesListOldPaymentSchedules + " since its managerId field is not nullable.");
                }
            }
            for (PaymentSubmissions paymentSubmissionsList1OldPaymentSubmissions : paymentSubmissionsList1Old) {
                if (!paymentSubmissionsList1New.contains(paymentSubmissionsList1OldPaymentSubmissions)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain PaymentSubmissions " + paymentSubmissionsList1OldPaymentSubmissions + " since its submittedBy field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (clientIdNew != null) {
                clientIdNew = em.getReference(clientIdNew.getClass(), clientIdNew.getId());
                users.setClientId(clientIdNew);
            }
            List<ClientCreationRequests> attachedClientCreationRequestsListNew = new ArrayList<ClientCreationRequests>();
            for (ClientCreationRequests clientCreationRequestsListNewClientCreationRequestsToAttach : clientCreationRequestsListNew) {
                clientCreationRequestsListNewClientCreationRequestsToAttach = em.getReference(clientCreationRequestsListNewClientCreationRequestsToAttach.getClass(), clientCreationRequestsListNewClientCreationRequestsToAttach.getId());
                attachedClientCreationRequestsListNew.add(clientCreationRequestsListNewClientCreationRequestsToAttach);
            }
            clientCreationRequestsListNew = attachedClientCreationRequestsListNew;
            users.setClientCreationRequestsList(clientCreationRequestsListNew);
            List<ClientCreationRequests> attachedClientCreationRequestsList1New = new ArrayList<ClientCreationRequests>();
            for (ClientCreationRequests clientCreationRequestsList1NewClientCreationRequestsToAttach : clientCreationRequestsList1New) {
                clientCreationRequestsList1NewClientCreationRequestsToAttach = em.getReference(clientCreationRequestsList1NewClientCreationRequestsToAttach.getClass(), clientCreationRequestsList1NewClientCreationRequestsToAttach.getId());
                attachedClientCreationRequestsList1New.add(clientCreationRequestsList1NewClientCreationRequestsToAttach);
            }
            clientCreationRequestsList1New = attachedClientCreationRequestsList1New;
            users.setClientCreationRequestsList1(clientCreationRequestsList1New);
            List<PaymentRequests> attachedPaymentRequestsListNew = new ArrayList<PaymentRequests>();
            for (PaymentRequests paymentRequestsListNewPaymentRequestsToAttach : paymentRequestsListNew) {
                paymentRequestsListNewPaymentRequestsToAttach = em.getReference(paymentRequestsListNewPaymentRequestsToAttach.getClass(), paymentRequestsListNewPaymentRequestsToAttach.getId());
                attachedPaymentRequestsListNew.add(paymentRequestsListNewPaymentRequestsToAttach);
            }
            paymentRequestsListNew = attachedPaymentRequestsListNew;
            users.setPaymentRequestsList(paymentRequestsListNew);
            List<PaymentRequests> attachedPaymentRequestsList1New = new ArrayList<PaymentRequests>();
            for (PaymentRequests paymentRequestsList1NewPaymentRequestsToAttach : paymentRequestsList1New) {
                paymentRequestsList1NewPaymentRequestsToAttach = em.getReference(paymentRequestsList1NewPaymentRequestsToAttach.getClass(), paymentRequestsList1NewPaymentRequestsToAttach.getId());
                attachedPaymentRequestsList1New.add(paymentRequestsList1NewPaymentRequestsToAttach);
            }
            paymentRequestsList1New = attachedPaymentRequestsList1New;
            users.setPaymentRequestsList1(paymentRequestsList1New);
            List<PaymentSchedules> attachedPaymentSchedulesListNew = new ArrayList<PaymentSchedules>();
            for (PaymentSchedules paymentSchedulesListNewPaymentSchedulesToAttach : paymentSchedulesListNew) {
                paymentSchedulesListNewPaymentSchedulesToAttach = em.getReference(paymentSchedulesListNewPaymentSchedulesToAttach.getClass(), paymentSchedulesListNewPaymentSchedulesToAttach.getId());
                attachedPaymentSchedulesListNew.add(paymentSchedulesListNewPaymentSchedulesToAttach);
            }
            paymentSchedulesListNew = attachedPaymentSchedulesListNew;
            users.setPaymentSchedulesList(paymentSchedulesListNew);
            List<PaymentSubmissions> attachedPaymentSubmissionsListNew = new ArrayList<PaymentSubmissions>();
            for (PaymentSubmissions paymentSubmissionsListNewPaymentSubmissionsToAttach : paymentSubmissionsListNew) {
                paymentSubmissionsListNewPaymentSubmissionsToAttach = em.getReference(paymentSubmissionsListNewPaymentSubmissionsToAttach.getClass(), paymentSubmissionsListNewPaymentSubmissionsToAttach.getId());
                attachedPaymentSubmissionsListNew.add(paymentSubmissionsListNewPaymentSubmissionsToAttach);
            }
            paymentSubmissionsListNew = attachedPaymentSubmissionsListNew;
            users.setPaymentSubmissionsList(paymentSubmissionsListNew);
            List<PaymentSubmissions> attachedPaymentSubmissionsList1New = new ArrayList<PaymentSubmissions>();
            for (PaymentSubmissions paymentSubmissionsList1NewPaymentSubmissionsToAttach : paymentSubmissionsList1New) {
                paymentSubmissionsList1NewPaymentSubmissionsToAttach = em.getReference(paymentSubmissionsList1NewPaymentSubmissionsToAttach.getClass(), paymentSubmissionsList1NewPaymentSubmissionsToAttach.getId());
                attachedPaymentSubmissionsList1New.add(paymentSubmissionsList1NewPaymentSubmissionsToAttach);
            }
            paymentSubmissionsList1New = attachedPaymentSubmissionsList1New;
            users.setPaymentSubmissionsList1(paymentSubmissionsList1New);
            users = em.merge(users);
            if (clientIdOld != null && !clientIdOld.equals(clientIdNew)) {
                clientIdOld.getUsersList().remove(users);
                clientIdOld = em.merge(clientIdOld);
            }
            if (clientIdNew != null && !clientIdNew.equals(clientIdOld)) {
                clientIdNew.getUsersList().add(users);
                clientIdNew = em.merge(clientIdNew);
            }
            for (ClientCreationRequests clientCreationRequestsListOldClientCreationRequests : clientCreationRequestsListOld) {
                if (!clientCreationRequestsListNew.contains(clientCreationRequestsListOldClientCreationRequests)) {
                    clientCreationRequestsListOldClientCreationRequests.setCreatedBy(null);
                    clientCreationRequestsListOldClientCreationRequests = em.merge(clientCreationRequestsListOldClientCreationRequests);
                }
            }
            for (ClientCreationRequests clientCreationRequestsListNewClientCreationRequests : clientCreationRequestsListNew) {
                if (!clientCreationRequestsListOld.contains(clientCreationRequestsListNewClientCreationRequests)) {
                    Users oldCreatedByOfClientCreationRequestsListNewClientCreationRequests = clientCreationRequestsListNewClientCreationRequests.getCreatedBy();
                    clientCreationRequestsListNewClientCreationRequests.setCreatedBy(users);
                    clientCreationRequestsListNewClientCreationRequests = em.merge(clientCreationRequestsListNewClientCreationRequests);
                    if (oldCreatedByOfClientCreationRequestsListNewClientCreationRequests != null && !oldCreatedByOfClientCreationRequestsListNewClientCreationRequests.equals(users)) {
                        oldCreatedByOfClientCreationRequestsListNewClientCreationRequests.getClientCreationRequestsList().remove(clientCreationRequestsListNewClientCreationRequests);
                        oldCreatedByOfClientCreationRequestsListNewClientCreationRequests = em.merge(oldCreatedByOfClientCreationRequestsListNewClientCreationRequests);
                    }
                }
            }
            for (ClientCreationRequests clientCreationRequestsList1OldClientCreationRequests : clientCreationRequestsList1Old) {
                if (!clientCreationRequestsList1New.contains(clientCreationRequestsList1OldClientCreationRequests)) {
                    clientCreationRequestsList1OldClientCreationRequests.setReviewedBy(null);
                    clientCreationRequestsList1OldClientCreationRequests = em.merge(clientCreationRequestsList1OldClientCreationRequests);
                }
            }
            for (ClientCreationRequests clientCreationRequestsList1NewClientCreationRequests : clientCreationRequestsList1New) {
                if (!clientCreationRequestsList1Old.contains(clientCreationRequestsList1NewClientCreationRequests)) {
                    Users oldReviewedByOfClientCreationRequestsList1NewClientCreationRequests = clientCreationRequestsList1NewClientCreationRequests.getReviewedBy();
                    clientCreationRequestsList1NewClientCreationRequests.setReviewedBy(users);
                    clientCreationRequestsList1NewClientCreationRequests = em.merge(clientCreationRequestsList1NewClientCreationRequests);
                    if (oldReviewedByOfClientCreationRequestsList1NewClientCreationRequests != null && !oldReviewedByOfClientCreationRequestsList1NewClientCreationRequests.equals(users)) {
                        oldReviewedByOfClientCreationRequestsList1NewClientCreationRequests.getClientCreationRequestsList1().remove(clientCreationRequestsList1NewClientCreationRequests);
                        oldReviewedByOfClientCreationRequestsList1NewClientCreationRequests = em.merge(oldReviewedByOfClientCreationRequestsList1NewClientCreationRequests);
                    }
                }
            }
            for (PaymentRequests paymentRequestsListNewPaymentRequests : paymentRequestsListNew) {
                if (!paymentRequestsListOld.contains(paymentRequestsListNewPaymentRequests)) {
                    Users oldRequesterIdOfPaymentRequestsListNewPaymentRequests = paymentRequestsListNewPaymentRequests.getRequesterId();
                    paymentRequestsListNewPaymentRequests.setRequesterId(users);
                    paymentRequestsListNewPaymentRequests = em.merge(paymentRequestsListNewPaymentRequests);
                    if (oldRequesterIdOfPaymentRequestsListNewPaymentRequests != null && !oldRequesterIdOfPaymentRequestsListNewPaymentRequests.equals(users)) {
                        oldRequesterIdOfPaymentRequestsListNewPaymentRequests.getPaymentRequestsList().remove(paymentRequestsListNewPaymentRequests);
                        oldRequesterIdOfPaymentRequestsListNewPaymentRequests = em.merge(oldRequesterIdOfPaymentRequestsListNewPaymentRequests);
                    }
                }
            }
            for (PaymentRequests paymentRequestsList1OldPaymentRequests : paymentRequestsList1Old) {
                if (!paymentRequestsList1New.contains(paymentRequestsList1OldPaymentRequests)) {
                    paymentRequestsList1OldPaymentRequests.setReviewedBy(null);
                    paymentRequestsList1OldPaymentRequests = em.merge(paymentRequestsList1OldPaymentRequests);
                }
            }
            for (PaymentRequests paymentRequestsList1NewPaymentRequests : paymentRequestsList1New) {
                if (!paymentRequestsList1Old.contains(paymentRequestsList1NewPaymentRequests)) {
                    Users oldReviewedByOfPaymentRequestsList1NewPaymentRequests = paymentRequestsList1NewPaymentRequests.getReviewedBy();
                    paymentRequestsList1NewPaymentRequests.setReviewedBy(users);
                    paymentRequestsList1NewPaymentRequests = em.merge(paymentRequestsList1NewPaymentRequests);
                    if (oldReviewedByOfPaymentRequestsList1NewPaymentRequests != null && !oldReviewedByOfPaymentRequestsList1NewPaymentRequests.equals(users)) {
                        oldReviewedByOfPaymentRequestsList1NewPaymentRequests.getPaymentRequestsList1().remove(paymentRequestsList1NewPaymentRequests);
                        oldReviewedByOfPaymentRequestsList1NewPaymentRequests = em.merge(oldReviewedByOfPaymentRequestsList1NewPaymentRequests);
                    }
                }
            }
            for (PaymentSchedules paymentSchedulesListNewPaymentSchedules : paymentSchedulesListNew) {
                if (!paymentSchedulesListOld.contains(paymentSchedulesListNewPaymentSchedules)) {
                    Users oldManagerIdOfPaymentSchedulesListNewPaymentSchedules = paymentSchedulesListNewPaymentSchedules.getManagerId();
                    paymentSchedulesListNewPaymentSchedules.setManagerId(users);
                    paymentSchedulesListNewPaymentSchedules = em.merge(paymentSchedulesListNewPaymentSchedules);
                    if (oldManagerIdOfPaymentSchedulesListNewPaymentSchedules != null && !oldManagerIdOfPaymentSchedulesListNewPaymentSchedules.equals(users)) {
                        oldManagerIdOfPaymentSchedulesListNewPaymentSchedules.getPaymentSchedulesList().remove(paymentSchedulesListNewPaymentSchedules);
                        oldManagerIdOfPaymentSchedulesListNewPaymentSchedules = em.merge(oldManagerIdOfPaymentSchedulesListNewPaymentSchedules);
                    }
                }
            }
            for (PaymentSubmissions paymentSubmissionsListOldPaymentSubmissions : paymentSubmissionsListOld) {
                if (!paymentSubmissionsListNew.contains(paymentSubmissionsListOldPaymentSubmissions)) {
                    paymentSubmissionsListOldPaymentSubmissions.setReviewedBy(null);
                    paymentSubmissionsListOldPaymentSubmissions = em.merge(paymentSubmissionsListOldPaymentSubmissions);
                }
            }
            for (PaymentSubmissions paymentSubmissionsListNewPaymentSubmissions : paymentSubmissionsListNew) {
                if (!paymentSubmissionsListOld.contains(paymentSubmissionsListNewPaymentSubmissions)) {
                    Users oldReviewedByOfPaymentSubmissionsListNewPaymentSubmissions = paymentSubmissionsListNewPaymentSubmissions.getReviewedBy();
                    paymentSubmissionsListNewPaymentSubmissions.setReviewedBy(users);
                    paymentSubmissionsListNewPaymentSubmissions = em.merge(paymentSubmissionsListNewPaymentSubmissions);
                    if (oldReviewedByOfPaymentSubmissionsListNewPaymentSubmissions != null && !oldReviewedByOfPaymentSubmissionsListNewPaymentSubmissions.equals(users)) {
                        oldReviewedByOfPaymentSubmissionsListNewPaymentSubmissions.getPaymentSubmissionsList().remove(paymentSubmissionsListNewPaymentSubmissions);
                        oldReviewedByOfPaymentSubmissionsListNewPaymentSubmissions = em.merge(oldReviewedByOfPaymentSubmissionsListNewPaymentSubmissions);
                    }
                }
            }
            for (PaymentSubmissions paymentSubmissionsList1NewPaymentSubmissions : paymentSubmissionsList1New) {
                if (!paymentSubmissionsList1Old.contains(paymentSubmissionsList1NewPaymentSubmissions)) {
                    Users oldSubmittedByOfPaymentSubmissionsList1NewPaymentSubmissions = paymentSubmissionsList1NewPaymentSubmissions.getSubmittedBy();
                    paymentSubmissionsList1NewPaymentSubmissions.setSubmittedBy(users);
                    paymentSubmissionsList1NewPaymentSubmissions = em.merge(paymentSubmissionsList1NewPaymentSubmissions);
                    if (oldSubmittedByOfPaymentSubmissionsList1NewPaymentSubmissions != null && !oldSubmittedByOfPaymentSubmissionsList1NewPaymentSubmissions.equals(users)) {
                        oldSubmittedByOfPaymentSubmissionsList1NewPaymentSubmissions.getPaymentSubmissionsList1().remove(paymentSubmissionsList1NewPaymentSubmissions);
                        oldSubmittedByOfPaymentSubmissionsList1NewPaymentSubmissions = em.merge(oldSubmittedByOfPaymentSubmissionsList1NewPaymentSubmissions);
                    }
                }
            }
            utx.commit();
        } catch (Exception ex) {
            rollbackQuietly();
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Object id = users.getId();
                if (findUsers(id) == null) {
                    throw new NonexistentEntityException("The users with id " + id + " no longer exists.");
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
        deactivate(id);
    }

    public void deactivate(Object id)
            throws NonexistentEntityException, RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            List<Users> administrators = lockAdministrators(em);
            List<Users> activeAdmins = administrators.stream()
                    .filter(Users::getActive)
                    .toList();
            Users users = em.find(Users.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (users == null) {
                throw new NonexistentEntityException(
                        "The users with id " + id + " no longer exists.");
            }
            ensurePrimaryAdministratorProtected(
                    users, administrators, users.getRole(), false);
            ensureActiveAdministratorRemains(
                    users, activeAdmins, users.getRole(), false);
            users.setActive(false);
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

    public AccountDeletionSummary deleteUnreferencedAccountsExceptPrimary()
            throws RollbackFailureException, Exception {
        EntityManager em = null;
        try {
            utx.begin();
            em = getEntityManager();
            List<Users> administrators = lockAdministrators(em);
            if (administrators.isEmpty()) {
                throw new IllegalStateException(
                        "Aucun administrateur principal n’a été trouvé; aucune suppression effectuée.");
            }

            UUID primaryAdministratorId = administrators.get(0).getId();
            List<Users> accounts = em.createQuery(
                    "SELECT u FROM Users u WHERE u.id <> :primaryAdministratorId",
                    Users.class)
                    .setParameter("primaryAdministratorId", primaryAdministratorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();

            int deleted = 0;
            int retained = 0;
            for (Users account : accounts) {
                if (hasProtectedReferences(em, account.getId())) {
                    retained++;
                } else {
                    em.remove(account);
                    deleted++;
                }
            }
            utx.commit();
            return new AccountDeletionSummary(deleted, retained);
        } catch (Exception ex) {
            rollbackQuietly();
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private boolean hasProtectedReferences(EntityManager em, UUID userId) {
        return hasReferences(em,
                "SELECT COUNT(r) FROM ClientCreationRequests r "
                + "WHERE r.createdBy.id = :userId OR r.reviewedBy.id = :userId",
                userId)
                || hasReferences(em,
                        "SELECT COUNT(r) FROM PaymentRequests r "
                        + "WHERE r.requesterId.id = :userId OR r.reviewedBy.id = :userId",
                        userId)
                || hasReferences(em,
                        "SELECT COUNT(s) FROM PaymentSchedules s WHERE s.managerId.id = :userId",
                        userId)
                || hasReferences(em,
                        "SELECT COUNT(s) FROM PaymentSubmissions s "
                        + "WHERE s.submittedBy.id = :userId OR s.reviewedBy.id = :userId",
                        userId);
    }

    private boolean hasReferences(EntityManager em, String jpql, UUID userId) {
        return em.createQuery(jpql, Long.class)
                .setParameter("userId", userId)
                .getSingleResult() > 0;
    }

    private List<Users> lockAdministrators(EntityManager em) {
        return em.createQuery(
                "SELECT u FROM Users u WHERE UPPER(u.role) = :role "
                + "ORDER BY u.createdAt ASC, u.email ASC",
                Users.class)
                .setParameter("role", RoleNames.ADMIN)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    private void ensurePrimaryAdministratorProtected(Users existingUser,
            List<Users> administrators, String newRole, boolean newActive) {
        boolean isPrimaryAdministrator = RoleNames.ADMIN.equals(
                RoleNames.normalize(existingUser.getRole()))
                && !administrators.isEmpty()
                && administrators.get(0).getId().equals(existingUser.getId());
        if (isPrimaryAdministrator
                && (!newActive || !RoleNames.ADMIN.equals(RoleNames.normalize(newRole)))) {
            throw new IllegalStateException(
                    "Le compte administrateur principal ne peut pas être désactivé ni rétrogradé.");
        }
    }

    private void ensureActiveAdministratorRemains(Users existingUser,
            List<Users> activeAdmins, String newRole, boolean newActive) {
        if (!existingUser.getActive()
                || !RoleNames.ADMIN.equals(RoleNames.normalize(existingUser.getRole()))
                || (newActive && RoleNames.ADMIN.equals(RoleNames.normalize(newRole)))) {
            return;
        }
        boolean anotherActiveAdminExists = activeAdmins.stream()
                .anyMatch(admin -> !admin.getId().equals(existingUser.getId()));
        if (!anotherActiveAdminExists) {
            throw new IllegalStateException(
                    "Impossible de désactiver ou rétrograder le dernier administrateur actif.");
        }
    }

    public UUID findPrimaryAdministratorId() {
        EntityManager em = getEntityManager();
        try {
            List<Users> administrators = em.createQuery(
                    "SELECT u FROM Users u WHERE UPPER(u.role) = :role "
                    + "ORDER BY u.createdAt ASC, u.email ASC", Users.class)
                    .setParameter("role", RoleNames.ADMIN)
                    .setMaxResults(1)
                    .getResultList();
            return administrators.isEmpty() ? null : administrators.get(0).getId();
        } finally {
            em.close();
        }
    }

    public boolean emailExists(String email, UUID excludedUserId) {
        EntityManager em = getEntityManager();
        try {
            String jpql = "SELECT COUNT(u) FROM Users u WHERE LOWER(u.email) = :email";
            if (excludedUserId != null) {
                jpql += " AND u.id <> :excludedUserId";
            }
            var query = em.createQuery(jpql, Long.class)
                    .setParameter("email", email);
            if (excludedUserId != null) {
                query.setParameter("excludedUserId", excludedUserId);
            }
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    public List<Users> findUsersEntities() {
        return findUsersEntities(true, -1, -1);
    }

    public List<Users> findUsersEntities(int maxResults, int firstResult) {
        return findUsersEntities(false, maxResults, firstResult);
    }

    private List<Users> findUsersEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Users.class));
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

    public Users findUsers(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Users.class, id);
        } finally {
            em.close();
        }
    }

    /** Charge le compte avec toutes ses relations (collections paresseuses initialisées). */
    public Users findUsersWithRelations(Object id) {
        EntityManager em = getEntityManager();
        try {
            Users user = em.find(Users.class, id);
            if (user != null) {
                user.getClientCreationRequestsList().size();
                user.getClientCreationRequestsList1().size();
                user.getPaymentRequestsList().size();
                user.getPaymentRequestsList1().size();
                user.getPaymentSchedulesList().size();
                user.getPaymentSubmissionsList().size();
                user.getPaymentSubmissionsList1().size();
            }
            return user;
        } finally {
            em.close();
        }
    }

    public int getUsersCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Users> rt = cq.from(Users.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
