package com.library.filter;

import com.library.dto.ApiResponse;
import com.library.model.User;
import com.library.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(filterName = "AuthorizationFilter", urlPatterns = "/api/*")
public class AuthorizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String contextPath = req.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        // Skip auth check for auth endpoints
        if (path.startsWith("/api/auth/") || path.startsWith("/api/public/")) {
            chain.doFilter(request, response);
            return;
        }

        User user = (User) req.getAttribute("authenticatedUser");
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        String role = user.getRoleName() != null ? user.getRoleName().toUpperCase() : "MEMBER";
        String method = req.getMethod().toUpperCase();

        // Admin-only endpoints
        if (path.startsWith("/api/users") || path.startsWith("/api/audit-logs") || path.startsWith("/api/settings")) {
            if (!"ADMIN".equals(role)) {
                sendForbidden(res, "Access denied: Administrator privileges required.");
                return;
            }
        }

        // Staff-only (Admin or Librarian) state-changing endpoints
        if (path.startsWith("/api/loans/issue") || path.contains("/return") ||
            path.startsWith("/api/members") && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) ||
            path.startsWith("/api/books") && (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) ||
            path.startsWith("/api/categories") && !method.equals("GET") ||
            path.startsWith("/api/authors") && !method.equals("GET") ||
            path.startsWith("/api/publishers") && !method.equals("GET") ||
            path.contains("/waive") || path.contains("/pay") && path.startsWith("/api/fines")) {

            if (!"ADMIN".equals(role) && !"LIBRARIAN".equals(role)) {
                sendForbidden(res, "Access denied: Staff privileges required to perform this operation.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void sendForbidden(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json");
        ApiResponse<Void> err = ApiResponse.error(message);
        res.getWriter().write(JsonUtil.toJson(err));
    }
}
