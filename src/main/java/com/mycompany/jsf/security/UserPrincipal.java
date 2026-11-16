package com.mycompany.jsf.security;

import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

public final class UserPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 1L;
    private final UUID id;
    private final UUID clientId;
    private final String email;
    private final String displayName;

    public UserPrincipal(UUID id, UUID clientId, String email, String displayName) {
        this.id = id;
        this.clientId = clientId;
        this.email = email;
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return email;
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
}
