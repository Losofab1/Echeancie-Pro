package com.mycompany.jsf.security;

import java.io.IOException;
import java.util.Arrays;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public final class NamePasswordCallbackHandler implements CallbackHandler {

    private final String email;
    private char[] password;

    public NamePasswordCallbackHandler(String email, char[] password) {
        this.email = email;
        this.password = password.clone();
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback nameCallback) {
                nameCallback.setName(email);
            } else if (callback instanceof PasswordCallback passwordCallback) {
                passwordCallback.setPassword(password);
            } else {
                throw new UnsupportedCallbackException(callback, "Unsupported authentication callback.");
            }
        }
    }

    public void clear() {
        Arrays.fill(password, '\0');
        password = new char[0];
    }
}
