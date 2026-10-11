package com.library.controller;

import com.library.dto.PaginatedResponse;
import com.library.dto.UserRequest;
import com.library.model.Role;
import com.library.model.User;
import com.library.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "UserServlet", urlPatterns = "/api/users/*")
public class UserServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if ("/roles".equals(pathInfo)) {
                List<Role> roles = userService.getRoles();
                sendSuccess(resp, "Roles retrieved", roles);
                return;
            }

            if (pathInfo == null || pathInfo.equals("/")) {
                String search = req.getParameter("search");
                String roleParam = req.getParameter("roleId");
                Integer roleId = (roleParam != null && !roleParam.isEmpty()) ? Integer.parseInt(roleParam) : null;
                String actParam = req.getParameter("active");
                Boolean active = (actParam != null && !actParam.isEmpty()) ? Boolean.parseBoolean(actParam) : null;

                int page = parseIntParam(req.getParameter("page"), 1);
                int pageSize = parseIntParam(req.getParameter("pageSize"), 10);

                PaginatedResponse<User> result = userService.getUsers(search, roleId, active, page, pageSize);
                sendSuccess(resp, "Users retrieved", result);

            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    User user = userService.getUserById(id);
                    sendSuccess(resp, "User retrieved", user);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
                }
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long adminId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                UserRequest userReq = parseJson(req, UserRequest.class);
                if (userReq == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing user details");
                    return;
                }
                User u = mapToUser(userReq);
                User created = userService.createUser(u, userReq.getPassword(), adminId, getClientIp(req));
                sendCreated(resp, "User created successfully", created);

            } else {
                String[] parts = pathInfo.split("/");
                // POST /api/users/{id}/reset-password
                if (parts.length == 3 && "reset-password".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    @SuppressWarnings("unchecked")
                    Map<String, String> body = parseJson(req, Map.class);
                    String newPass = (body != null) ? body.get("newPassword") : null;
                    userService.resetPassword(id, newPass, adminId, getClientIp(req));
                    sendSuccess(resp, "Password reset successfully", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
                }
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long adminId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    UserRequest userReq = parseJson(req, UserRequest.class);
                    if (userReq == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing user details");
                        return;
                    }
                    User u = mapToUser(userReq);
                    User updated = userService.updateUser(id, u, adminId, getClientIp(req));
                    sendSuccess(resp, "User updated successfully", updated);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long adminId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                // PATCH /api/users/{id}/status
                if (parts.length == 3 && "status".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = parseJson(req, Map.class);
                    Boolean active = (body != null && body.containsKey("active")) ? (Boolean) body.get("active") : false;
                    userService.setActive(id, active, adminId, getClientIp(req));
                    sendSuccess(resp, "User active status updated", null);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private User mapToUser(UserRequest req) {
        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setRoleId(req.getRoleId());
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setPhone(req.getPhone());
        u.setActive(req.isActive());
        return u;
    }

    private int parseIntParam(String val, int def) {
        if (val == null || val.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
