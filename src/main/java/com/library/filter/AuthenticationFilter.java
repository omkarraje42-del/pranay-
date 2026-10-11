package com.library.filter;

import com.library.dto.ApiResponse;
import com.library.model.User;
import com.library.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(filterName = "AuthenticationFilter", urlPatterns = "/api/*")
public class AuthenticationFilter implements Filter {

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

        // Public API endpoints
        if (path.equals("/api/auth/login") || path.equals("/api/auth/status") || path.startsWith("/api/public/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            ApiResponse<Void> err = ApiResponse.error("Authentication required. Please log in.");
            res.getWriter().write(JsonUtil.toJson(err));
            return;
        }

        // Attach user to request attributes for controllers to use
        req.setAttribute("authenticatedUser", currentUser);
        chain.doFilter(request, response);
    }
}
