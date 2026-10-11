package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Role;
import com.library.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public User findByUsername(String username) {
        String sql = "SELECT u.*, r.name AS role_name FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE LOWER(u.username) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT u.*, r.name AS role_name FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE LOWER(u.email) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email: {}", email, e);
        }
        return null;
    }

    public User findById(long id) {
        String sql = "SELECT u.*, r.name AS role_name FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", id, e);
        }
        return null;
    }

    public List<User> findAll(String search, Integer roleId, Boolean active, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT u.*, r.name AS role_name FROM users u ")
                .append("JOIN roles r ON u.role_id = r.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (roleId != null) {
            sql.append("AND u.role_id = ? ");
            params.add(roleId);
        }
        if (active != null) {
            sql.append("AND u.is_active = ? ");
            params.add(active);
        }

        sql.append("ORDER BY u.id ASC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 20);
        params.add(offset >= 0 ? offset : 0);

        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding all users", e);
        }
        return list;
    }

    public long count(String search, Integer roleId, Boolean active) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users u WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (roleId != null) {
            sql.append("AND u.role_id = ? ");
            params.add(roleId);
        }
        if (active != null) {
            sql.append("AND u.is_active = ? ");
            params.add(active);
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting users", e);
        }
        return 0;
    }

    public User create(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, role_id, first_name, last_name, phone, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at, updated_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getRoleId());
            ps.setString(5, user.getFirstName());
            ps.setString(6, user.getLastName());
            ps.setString(7, user.getPhone());
            ps.setBoolean(8, user.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong("id"));
                    user.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    user.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating user: {}", user.getUsername(), e);
            throw new RuntimeException("Could not create user: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET email = ?, role_id = ?, first_name = ?, last_name = ?, phone = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setInt(2, user.getRoleId());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setString(5, user.getPhone());
            ps.setBoolean(6, user.isActive());
            ps.setLong(7, user.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getId(), e);
            throw new RuntimeException("Could not update user: " + e.getMessage(), e);
        }
    }

    public boolean updatePassword(long userId, String newHash) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating password for user: {}", userId, e);
            throw new RuntimeException("Could not update password: " + e.getMessage(), e);
        }
    }

    public boolean setActive(long userId, boolean active) {
        String sql = "UPDATE users SET is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error setting active status for user: {}", userId, e);
            return false;
        }
    }

    public List<Role> findAllRoles() {
        String sql = "SELECT * FROM roles ORDER BY id ASC";
        List<Role> roles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Role r = new Role();
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                r.setDescription(rs.getString("description"));
                roles.add(r);
            }
        } catch (SQLException e) {
            logger.error("Error fetching roles", e);
        }
        return roles;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRoleId(rs.getInt("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("is_active"));
        u.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        u.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return u;
    }
}
