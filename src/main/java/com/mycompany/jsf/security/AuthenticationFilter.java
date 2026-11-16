package com.mycompany.jsf.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;

public final class AuthenticationFilter implements Filter {

    private static final Set<String> ADMIN_ONLY_PREFIXES = Set.of(
            "/views/users/",
            "/views/flywaySchemaHistory/",
            "/views/emailOutbox/");

    private static final Set<String> PUBLIC_RESOURCES = Set.of(
            "/jsfcrud.css",
            "/jsfcrud.js",
            "/password-toggle.js",
            "/index.html");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getServletPath();

        if ("/login.xhtml".equals(path)
                || "/access-denied.xhtml".equals(path)
                || PUBLIC_RESOURCES.contains(path)
                || path.startsWith("/jakarta.faces.resource/")
                || path.startsWith("/javax.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        Object sessionUser = session == null
                ? null : session.getAttribute(AuthenticatedUser.SESSION_ATTRIBUTE);
        if (!(sessionUser instanceof AuthenticatedUser user)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.xhtml");
            return;
        }

        if (user.isClient() && !path.startsWith("/client/")
                && !"/profile.xhtml".equals(path)) {
            httpResponse.sendRedirect(
                    httpRequest.getContextPath() + "/client/dashboard.xhtml");
            return;
        }
        if (user.isManager() && "/welcomeJSF.xhtml".equals(path)) {
            httpResponse.sendRedirect(
                    httpRequest.getContextPath() + "/manager/dashboard.xhtml");
            return;
        }
        if (!user.isClient() && path.startsWith("/client/")) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/welcomeJSF.xhtml");
            return;
        }

        if (isAdminOnly(path) && !user.isAdmin()) {
            httpResponse.sendRedirect(
                    httpRequest.getContextPath() + "/access-denied.xhtml");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAdminOnly(String path) {
        return ADMIN_ONLY_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
