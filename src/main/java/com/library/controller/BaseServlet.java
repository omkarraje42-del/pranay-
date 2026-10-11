package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.exception.ValidationException;
import com.library.model.User;
import com.library.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(BaseServlet.class);

    protected void sendJson(HttpServletResponse resp, int statusCode, ApiResponse<?> body) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(body));
    }

    protected void sendSuccess(HttpServletResponse resp, String message, Object data) throws IOException {
        sendJson(resp, HttpServletResponse.SC_OK, ApiResponse.success(message, data));
    }

    protected void sendCreated(HttpServletResponse resp, String message, Object data) throws IOException {
        sendJson(resp, HttpServletResponse.SC_CREATED, ApiResponse.success(message, data));
    }

    protected void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        sendJson(resp, statusCode, ApiResponse.error(message));
    }

    protected <T> T parseJson(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return JsonUtil.fromJson(sb.toString(), clazz);
    }

    protected User getAuthenticatedUser(HttpServletRequest req) {
        User u = (User) req.getAttribute("authenticatedUser");
        if (u != null) return u;
        HttpSession session = req.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    protected String getClientIp(HttpServletRequest req) {
        String xForwarded = req.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.trim().isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    protected void handleException(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof ResourceNotFoundException) {
            sendJson(resp, HttpServletResponse.SC_NOT_FOUND, ApiResponse.error(e.getMessage()));
        } else if (e instanceof ValidationException ve) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, ApiResponse.error(ve.getMessage(), ve.getErrors()));
        } else if (e instanceof UnauthorizedException) {
            sendJson(resp, HttpServletResponse.SC_UNAUTHORIZED, ApiResponse.error(e.getMessage()));
        } else if (e instanceof BusinessRuleException) {
            sendJson(resp, HttpServletResponse.SC_CONFLICT, ApiResponse.error(e.getMessage()));
        } else {
            logger.error("Unexpected server exception in API controller", e);
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.error("An unexpected internal error occurred. Please contact the administrator."));
        }
    }
}
