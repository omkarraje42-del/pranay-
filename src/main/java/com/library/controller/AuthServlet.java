package com.library.controller;

import com.library.dto.ChangePasswordRequest;
import com.library.dto.LoginRequest;
import com.library.model.User;
import com.library.service.AuthService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AuthServlet", urlPatterns = "/api/auth/*")
public class AuthServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/me")) {
            handleMe(req, resp);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        try {
            switch (pathInfo) {
                case "/login" -> handleLogin(req, resp);
                case "/logout" -> handleLogout(req, resp);
                case "/change-password" -> handleChangePassword(req, resp);
                default -> sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginRequest loginReq = parseJson(req, LoginRequest.class);
        if (loginReq == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing login credentials in request body");
            return;
        }

        User user = authService.authenticate(loginReq.getUsername(), loginReq.getPassword(), getClientIp(req));

        // Create secure session
        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);
        session.setMaxInactiveInterval(1800); // 30 minutes expiration

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("role", user.getRoleName());
        userData.put("roleId", user.getRoleId());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());
        userData.put("fullName", user.getFullName());

        sendSuccess(resp, "Login successful", userData);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        sendSuccess(resp, "Logged out successfully", null);
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("role", user.getRoleName());
        userData.put("roleId", user.getRoleId());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());
        userData.put("fullName", user.getFullName());

        sendSuccess(resp, "Current session", userData);
    }

    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        ChangePasswordRequest cpReq = parseJson(req, ChangePasswordRequest.class);
        if (cpReq == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
            return;
        }

        authService.changePassword(user.getId(), cpReq.getCurrentPassword(), cpReq.getNewPassword(), getClientIp(req));
        sendSuccess(resp, "Password changed successfully", null);
    }
}
