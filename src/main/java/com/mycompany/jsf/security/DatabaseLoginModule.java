package com.mycompany.jsf.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public final class DatabaseLoginModule implements LoginModule {

    private static final Logger LOGGER =
            Logger.getLogger(DatabaseLoginModule.class.getName());
    private static final String ACCOUNT_QUERY =
            "SELECT id, name, email, password_hash, role, active, client_id "
            + "FROM users WHERE lower(email) = lower(?)";
    private static final String PASSWORD_UPDATE =
            "UPDATE users SET password_hash = ? WHERE id = ? AND password_hash = ?";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private DataSource dataSource;
    private UserPrincipal userPrincipal;
    private RolePrincipal rolePrincipal;
    private boolean succeeded;
    private boolean committed;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        Object jndiName = options.get("dataSourceJndiName");
        InitialContext context = null;
        try {
            context = new InitialContext();
            dataSource = (DataSource) context.lookup(
                    jndiName == null ? "java:jboss/datasources/echeanDS" : jndiName.toString());
        } catch (NamingException ex) {
            LOGGER.log(Level.SEVERE, "Unable to resolve the authentication datasource.", ex);
            dataSource = null;
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ex) {
                    LOGGER.log(Level.WARNING, "Unable to close the JNDI context.", ex);
                }
            }
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null || dataSource == null) {
            throw new LoginException("Authentication service is unavailable.");
        }

        NameCallback nameCallback = new NameCallback("Email:");
        PasswordCallback passwordCallback = new PasswordCallback("Password:", false);
        char[] suppliedPassword = null;
        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
            String email = nameCallback.getName();
            suppliedPassword = passwordCallback.getPassword();
            if (email == null || email.isBlank() || suppliedPassword == null
                    || suppliedPassword.length == 0) {
                throw invalidCredentials();
            }

            Account account = findAccount(email.trim());
            if (account == null || !account.active()
                    || !RoleNames.isAllowed(account.role())
                    || (RoleNames.CLIENT.equals(RoleNames.normalize(account.role()))
                    && account.clientId() == null)
                    || !PasswordHasher.verify(suppliedPassword, account.passwordHash())) {
                throw invalidCredentials();
            }

            String normalizedRole = account.role().trim().toUpperCase(Locale.ROOT);
            if (!PasswordHasher.isBcryptHash(account.passwordHash())) {
                migratePassword(account, suppliedPassword);
            }

            userPrincipal = new UserPrincipal(
                    account.id(), account.clientId(), account.email(), account.name());
            rolePrincipal = new RolePrincipal(normalizedRole);
            succeeded = true;
            return true;
        } catch (LoginException ex) {
            throw ex;
        } catch (Exception ex) {
            LoginException loginException = new LoginException("Authentication service is unavailable.");
            loginException.initCause(ex);
            throw loginException;
        } finally {
            passwordCallback.clearPassword();
            if (suppliedPassword != null) {
                Arrays.fill(suppliedPassword, '\0');
            }
        }
    }

    private Account findAccount(String email) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(ACCOUNT_QUERY)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                Account account = new Account(
                        UUID.fromString(result.getString("id")),
                        result.getString("name"),
                        result.getString("email"),
                        result.getString("password_hash"),
                        result.getString("role"),
                        result.getBoolean("active"),
                        result.getString("client_id") == null
                                ? null : UUID.fromString(result.getString("client_id")));
                if (result.next()) {
                    throw new SQLException("More than one account matches the email.");
                }
                return account;
            }
        }
    }

    private void migratePassword(Account account, char[] password)
            throws SQLException, LoginException {
        String bcryptHash = PasswordHasher.hash(password);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(PASSWORD_UPDATE)) {
            statement.setString(1, bcryptHash);
            statement.setObject(2, account.id());
            statement.setString(3, account.passwordHash());
            if (statement.executeUpdate() != 1) {
                throw invalidCredentials();
            }
        }
    }

    private LoginException invalidCredentials() {
        return new LoginException("Invalid credentials or inactive account.");
    }

    @Override
    public boolean commit() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (subject.isReadOnly()) {
            clearState();
            throw new LoginException("Authentication subject is read-only.");
        }
        subject.getPrincipals().add(userPrincipal);
        subject.getPrincipals().add(rolePrincipal);
        committed = true;
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (committed) {
            return logout();
        }
        clearState();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        if (committed && !subject.isReadOnly()) {
            subject.getPrincipals().remove(userPrincipal);
            subject.getPrincipals().remove(rolePrincipal);
        }
        clearState();
        return true;
    }

    private void clearState() {
        succeeded = false;
        committed = false;
        userPrincipal = null;
        rolePrincipal = null;
    }

    private record Account(UUID id, String name, String email,
            String passwordHash, String role, boolean active, UUID clientId) {
    }
}
