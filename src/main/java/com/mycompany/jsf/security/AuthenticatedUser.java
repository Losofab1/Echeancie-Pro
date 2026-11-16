package com.mycompany.jsf.security;

import java.io.Serializable;
import java.util.UUID;

public final class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String SESSION_ATTRIBUTE =
            AuthenticatedUser.class.getName() + ".current";

    private final UUID id;
    private final UUID clientId;
    private final String email;
    private final String displayName;
    private final String role;

    public AuthenticatedUser(UUID id, UUID clientId, String email, String displayName, String role) {
        this.id = id;
        this.clientId = clientId;
        this.email = email;
        this.displayName = displayName;
        this.role = RoleNames.normalize(role);
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return RoleNames.ADMIN.equals(role);
    }

    public boolean isManager() {
        return RoleNames.MANAGER.equals(role);
    }

    public boolean isClient() {
        return RoleNames.CLIENT.equals(role);
    }
}
