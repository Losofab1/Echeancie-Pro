package com.mycompany.jsf.security;

import entities.Clients;
import entities.Users;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

@ApplicationScoped
public class ClientProvisioningService {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager entityManager;

    @Transactional
    public void createClientWithAccount(Clients client, char[] password) {
        if (client == null) {
            throw new IllegalArgumentException("La fiche client est obligatoire.");
        }
        String name = client.getName() == null ? "" : client.getName().trim();
        String email = client.getEmail() == null
                ? "" : client.getEmail().trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty() || email.isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Le nom et une adresse courriel valide sont obligatoires.");
        }
        if (password == null || password.length < PasswordHasher.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins "
                    + PasswordHasher.MIN_PASSWORD_LENGTH + " caractères.");
        }

        Long existingAccountCount = entityManager.createQuery(
                "SELECT COUNT(u) FROM Users u WHERE LOWER(u.email) = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        if (existingAccountCount != 0) {
            throw new IllegalArgumentException("Un compte utilise déjà cette adresse courriel.");
        }

        client.setName(name);
        client.setEmail(email);
        client.setCreatedAt(new Date());
        entityManager.persist(client);

        try {
            Users user = new Users();
            user.setName(name);
            user.setEmail(email);
            user.setPasswordHash(PasswordHasher.hash(password));
            user.setRole(RoleNames.CLIENT);
            user.setActive(true);
            user.setCreatedAt(new Date());
            user.setClientId(client);
            entityManager.persist(user);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
