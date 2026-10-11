package com.library.service;

import com.library.dao.AuditLogDAO;
import com.library.dao.UserDAO;
import com.library.exception.BusinessRuleException;
import com.library.exception.UnauthorizedException;
import com.library.exception.ValidationException;
import com.library.model.AuditLog;
import com.library.model.User;
import com.library.util.PasswordUtil;
import com.library.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public User authenticate(String usernameOrEmail, String password, String ipAddress) {
        if (!ValidationUtil.isNotEmpty(usernameOrEmail) || !ValidationUtil.isNotEmpty(password)) {
            throw new ValidationException("Username and password are required");
        }

        User user = null;
        if (usernameOrEmail.contains("@")) {
            user = userDAO.findByEmail(usernameOrEmail.trim());
        } else {
            user = userDAO.findByUsername(usernameOrEmail.trim());
        }

        if (user == null) {
            logger.warn("Failed login attempt for unknown user: {}", usernameOrEmail);
            throw new UnauthorizedException("Invalid username or password");
        }

        if (!user.isActive()) {
            logger.warn("Login attempt for deactivated user: {}", user.getUsername());
            throw new UnauthorizedException("Your account has been deactivated. Please contact an administrator.");
        }

        if (!PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            logger.warn("Password mismatch for user: {}", user.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }

        auditLogDAO.log(new AuditLog(user.getId(), "LOGIN", "USER", user.getId(), "User logged in successfully", ipAddress));
        logger.info("User {} logged in successfully from IP {}", user.getUsername(), ipAddress);
        return user;
    }

    public void changePassword(long userId, String currentPassword, String newPassword, String ipAddress) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new BusinessRuleException("User not found");
        }

        if (!PasswordUtil.checkPassword(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password does not match");
        }

        if (!ValidationUtil.isNotEmpty(newPassword) || newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters in length");
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        userDAO.updatePassword(userId, newHash);
        auditLogDAO.log(new AuditLog(userId, "CHANGE_PASSWORD", "USER", userId, "Password changed successfully", ipAddress));
        logger.info("Password changed for user {}", user.getUsername());
    }

    /**
     * Secure bootstrap procedure: checks if any admin exists; if not, creates the initial admin.
     */
    public synchronized void bootstrapInitialAdmin(String username, String email, String password) {
        User existing = userDAO.findByUsername(username);
        if (existing == null) {
            User admin = new User();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPasswordHash(PasswordUtil.hashPassword(password));
            admin.setRoleId(1); // ADMIN
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setActive(true);
            userDAO.create(admin);
            logger.info("Initial administrator account bootstrapped: {}", username);
        }
    }
}
