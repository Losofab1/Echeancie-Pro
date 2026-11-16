package com.mycompany.jsf.security;

import entities.Clients;
import entities.Users;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ProfileService {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager entityManager;

    @Transactional
    public ProfileData loadProfile(UUID userId) {
        Users user = requireActiveUser(userId);
        boolean clientRole = RoleNames.CLIENT.equals(RoleNames.normalize(user.getRole()));
        Clients client = clientRole ? user.getClientId() : null;
        if (clientRole && client == null) {
            throw new SecurityException("Le compte client n’est pas associé à une fiche client.");
        }
        return new ProfileData(
                clientRole ? client.getName() : user.getName(),
                clientRole ? client.getEmail() : user.getEmail(),
                clientRole ? client.getPhone() : null,
                clientRole,
                RoleNames.normalize(user.getRole()));
    }

    @Transactional
    public ProfileData updateProfile(UUID userId, String name, String email, String phone,
            char[] currentPassword, char[] newPassword, char[] passwordConfirmation) {
        try {
            Users user = requireActiveUser(userId);
            String normalizedName = name == null ? "" : name.trim();
            String normalizedEmail = email == null
                    ? "" : email.trim().toLowerCase(Locale.ROOT);
            String normalizedPhone = phone == null || phone.isBlank() ? null : phone.trim();
            boolean clientRole = RoleNames.CLIENT.equals(RoleNames.normalize(user.getRole()));

            if (normalizedName.isEmpty() || normalizedName.length() > 120) {
                throw new IllegalArgumentException("Le nom est obligatoire et ne peut pas dépasser 120 caractères.");
            }
            if (!isValidEmail(normalizedEmail)) {
                throw new IllegalArgumentException("Saisissez une adresse courriel valide (254 caractères maximum).");
            }
            if (normalizedPhone != null && normalizedPhone.length() > 32) {
                throw new IllegalArgumentException(
                        "Le numéro de téléphone ne peut pas dépasser 32 caractères.");
            }

            Clients client = null;
            if (clientRole) {
                if (user.getClientId() == null) {
                    throw new SecurityException("Le compte client n’est pas associé à une fiche client.");
                }
                client = entityManager.find(
                        Clients.class, user.getClientId().getId(), LockModeType.PESSIMISTIC_WRITE);
                if (client == null) {
                    throw new SecurityException("La fiche client associée n’existe plus.");
                }
            }

            ensureEmailAvailable(normalizedEmail, user, client);
            updatePassword(user, currentPassword, newPassword, passwordConfirmation);

            user.setName(normalizedName);
            user.setEmail(normalizedEmail);
            if (client != null) {
                client.setName(normalizedName);
                client.setEmail(normalizedEmail);
                client.setPhone(normalizedPhone);
            }
            return new ProfileData(normalizedName, normalizedEmail, normalizedPhone,
                    clientRole, RoleNames.normalize(user.getRole()));
        } finally {
            clear(currentPassword);
            clear(newPassword);
            clear(passwordConfirmation);
        }
    }

    private void ensureEmailAvailable(String email, Users user, Clients client) {
        Long duplicateAccounts = entityManager.createQuery(
                "SELECT COUNT(u) FROM Users u WHERE LOWER(u.email) = :email AND u.id <> :userId",
                Long.class)
                .setParameter("email", email)
                .setParameter("userId", user.getId())
                .getSingleResult();
        if (duplicateAccounts > 0) {
            throw new IllegalArgumentException(
                    "Cette adresse courriel est déjà utilisée par un autre compte.");
        }

        Long duplicateClients;
        if (client == null) {
            duplicateClients = entityManager.createQuery(
                    "SELECT COUNT(c) FROM Clients c WHERE LOWER(c.email) = :email",
                    Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } else {
            duplicateClients = entityManager.createQuery(
                    "SELECT COUNT(c) FROM Clients c WHERE LOWER(c.email) = :email AND c.id <> :clientId",
                    Long.class)
                    .setParameter("email", email)
                    .setParameter("clientId", client.getId())
                    .getSingleResult();
        }
        if (duplicateClients > 0) {
            throw new IllegalArgumentException(
                    "Cette adresse courriel est déjà utilisée par un autre client.");
        }
    }

    private void updatePassword(Users user, char[] currentPassword,
            char[] newPassword, char[] passwordConfirmation) {
        boolean hasCurrentPassword = hasCharacters(currentPassword);
        boolean hasNewPassword = hasCharacters(newPassword);
        boolean hasConfirmation = hasCharacters(passwordConfirmation);
        if (!hasNewPassword && !hasConfirmation) {
            if (hasCurrentPassword) {
                throw new IllegalArgumentException(
                        "Saisissez le nouveau mot de passe et sa confirmation.");
            }
            return;
        }
        if (!hasCurrentPassword || !hasNewPassword || !hasConfirmation) {
            throw new IllegalArgumentException(
                    "Saisissez le mot de passe actuel, le nouveau mot de passe et sa confirmation.");
        }
        if (newPassword.length < PasswordHasher.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit contenir au moins "
                    + PasswordHasher.MIN_PASSWORD_LENGTH + " caractères.");
        }
        if (!Arrays.equals(newPassword, passwordConfirmation)) {
            throw new IllegalArgumentException(
                    "La confirmation du nouveau mot de passe ne correspond pas.");
        }
        if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
        }
        user.setPasswordHash(PasswordHasher.hash(newPassword));
    }

    private boolean hasCharacters(char[] value) {
        return value != null && value.length > 0;
    }

    private void clear(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }

    private Users requireActiveUser(UUID userId) {
        Users user = userId == null ? null
                : entityManager.find(Users.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !user.getActive() || !RoleNames.isAllowed(user.getRole())) {
            throw new SecurityException("Un compte actif est nécessaire pour modifier le profil.");
        }
        return user;
    }

    private boolean isValidEmail(String email) {
        return email.length() <= 254
                && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    public static final class ProfileData implements Serializable {

        private static final long serialVersionUID = 1L;
        private final String name;
        private final String email;
        private final String phone;
        private final boolean client;
        private final String role;

        private ProfileData(String name, String email, String phone,
                boolean client, String role) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.client = client;
            this.role = role;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public boolean isClient() { return client; }
        public String getRole() { return role; }
    }
}
