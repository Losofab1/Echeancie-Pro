package com.mycompany.jsf.security;

import java.io.Serializable;
import java.security.Principal;

public final class RolePrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 1L;
    private final String name;

    public RolePrincipal(String name) {
        this.name = RoleNames.normalize(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
