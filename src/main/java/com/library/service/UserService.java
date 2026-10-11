package com.library.service;

import com.library.dao.AuditLogDAO;
import com.library.dao.UserDAO;
import com.library.dto.PaginatedResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.AuditLog;
import com.library.model.Role;
import com.library.model.User;
import com.library.util.PasswordUtil;
import com.library.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public PaginatedResponse<User> getUsers(String search, Integer roleId, Boolean active, int page, int pageSize) {
        int limit = pageSize > 0 ? pageSize : 10;
        int currentPage = page > 0 ? page : 1;
        int offset = (currentPage - 1) * limit;

        long total = userDAO.count(search, roleId, active);
        List<User> list = userDAO.findAll(search, roleId, active, offset, limit);
        // Clear password hashes from response
        list.forEach(u -> u.setPasswordHash(null));
        return new PaginatedResponse<>(list, total, currentPage, limit);
    }

    public User getUserById(long id) {
        User u = userDAO.findById(id);
        if (u == null) {
            throw new ResourceNotFoundException("User not found");
        }
        u.setPasswordHash(null);
        return u;
    }

    public List<Role> getRoles() {
        return userDAO.findAllRoles();
    }

    public User createUser(User user, String password, Long adminUserId, String ipAddress) {
        validateUser(user, true);
        if (!ValidationUtil.isNotEmpty(password) || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }

        user.setPasswordHash(PasswordUtil.hashPassword(password));
        User created = userDAO.create(user);
        auditLogDAO.log(new AuditLog(adminUserId, "CREATE_USER", "USER", created.getId(),
                "Created staff user account " + created.getUsername() + " with role " + created.getRoleId(), ipAddress));

        created.setPasswordHash(null);
        return created;
    }

    public User updateUser(long id, User user, Long adminUserId, String ipAddress) {
        User existing = userDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("User not found");
        }

        user.setId(id);
        validateUser(user, false);

        if (!user.getEmail().equalsIgnoreCase(existing.getEmail())) {
            User duplicate = userDAO.findByEmail(user.getEmail());
            if (duplicate != null && duplicate.getId() != id) {
                throw new ValidationException("Email '" + user.getEmail() + "' already taken");
            }
        }

        userDAO.update(user);
        auditLogDAO.log(new AuditLog(adminUserId, "UPDATE_USER", "USER", id,
                "Updated staff user " + user.getUsername(), ipAddress));
        return getUserById(id);
    }

    public void setActive(long id, boolean active, Long adminUserId, String ipAddress) {
        User u = userDAO.findById(id);
        if (u == null) {
            throw new ResourceNotFoundException("User not found");
        }
        if (adminUserId != null && adminUserId == id && !active) {
            throw new BusinessRuleException("You cannot deactivate your own administrative account");
        }

        userDAO.setActive(id, active);
        auditLogDAO.log(new AuditLog(adminUserId, "SET_USER_ACTIVE", "USER", id,
                "Set user " + u.getUsername() + " active status to " + active, ipAddress));
    }

    public void resetPassword(long id, String newPassword, Long adminUserId, String ipAddress) {
        User u = userDAO.findById(id);
        if (u == null) {
            throw new ResourceNotFoundException("User not found");
        }
        if (!ValidationUtil.isNotEmpty(newPassword) || newPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }

        userDAO.updatePassword(id, PasswordUtil.hashPassword(newPassword));
        auditLogDAO.log(new AuditLog(adminUserId, "RESET_PASSWORD", "USER", id,
                "Reset password for user " + u.getUsername(), ipAddress));
    }

    private void validateUser(User user, boolean isNew) {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isNotEmpty(user.getUsername())) {
            errors.add("Username is required");
        } else if (isNew && userDAO.findByUsername(user.getUsername().trim()) != null) {
            errors.add("Username '" + user.getUsername() + "' is already taken");
        }

        if (!ValidationUtil.isNotEmpty(user.getEmail())) {
            errors.add("Email is required");
        } else if (!ValidationUtil.isValidEmail(user.getEmail())) {
            errors.add("Invalid email format");
        } else if (isNew && userDAO.findByEmail(user.getEmail().trim()) != null) {
            errors.add("Email '" + user.getEmail() + "' is already in use");
        }

        if (user.getRoleId() <= 0) {
            errors.add("A valid role must be selected");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
