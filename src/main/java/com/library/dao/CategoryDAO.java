package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private static final Logger logger = LoggerFactory.getLogger(CategoryDAO.class);

    public List<Category> findAll(String search) {
        StringBuilder sql = new StringBuilder(
            "SELECT c.*, (SELECT COUNT(*) FROM books b WHERE b.category_id = c.id AND b.is_active = true) AS book_count " +
            "FROM categories c WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND LOWER(c.name) LIKE ? ");
            params.add("%" + search.trim().toLowerCase() + "%");
        }
        sql.append("ORDER BY c.name ASC");

        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category c = new Category();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setDescription(rs.getString("description"));
                    c.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    c.setBookCount(rs.getInt("book_count"));
                    list.add(c);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding categories", e);
        }
        return list;
    }

    public Category findById(int id) {
        String sql = "SELECT c.*, (SELECT COUNT(*) FROM books b WHERE b.category_id = c.id AND b.is_active = true) AS book_count " +
                     "FROM categories c WHERE c.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category c = new Category();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setDescription(rs.getString("description"));
                    c.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    c.setBookCount(rs.getInt("book_count"));
                    return c;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding category by id: {}", id, e);
        }
        return null;
    }

    public Category findByName(String name) {
        String sql = "SELECT * FROM categories WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category c = new Category();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setDescription(rs.getString("description"));
                    c.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    return c;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding category by name: {}", name, e);
        }
        return null;
    }

    public Category create(Category category) {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    category.setId(rs.getInt("id"));
                    category.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    return category;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating category: {}", category.getName(), e);
            throw new RuntimeException("Could not create category: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean update(Category category) {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating category: {}", category.getId(), e);
            throw new RuntimeException("Could not update category: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting category: {}", id, e);
            throw new RuntimeException("Cannot delete category (it may contain books): " + e.getMessage(), e);
        }
    }

    public int countBooksInCategory(int id) {
        String sql = "SELECT COUNT(*) FROM books WHERE category_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting books in category: {}", id, e);
        }
        return 0;
    }
}
