package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class PublisherDAO {

    private static final Logger logger = LoggerFactory.getLogger(PublisherDAO.class);

    public List<Publisher> findAll(String search) {
        StringBuilder sql = new StringBuilder("SELECT * FROM publishers WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(name) LIKE ? OR LOWER(contact_email) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }
        sql.append("ORDER BY name ASC");

        List<Publisher> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPublisher(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding publishers", e);
        }
        return list;
    }

    public Publisher findById(int id) {
        String sql = "SELECT * FROM publishers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPublisher(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding publisher by id: {}", id, e);
        }
        return null;
    }

    public Publisher findByName(String name) {
        String sql = "SELECT * FROM publishers WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPublisher(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding publisher by name: {}", name, e);
        }
        return null;
    }

    public Publisher create(Publisher publisher) {
        String sql = "INSERT INTO publishers (name, contact_email, phone, website, address) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, publisher.getName());
            ps.setString(2, publisher.getContactEmail());
            ps.setString(3, publisher.getPhone());
            ps.setString(4, publisher.getWebsite());
            ps.setString(5, publisher.getAddress());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    publisher.setId(rs.getInt("id"));
                    publisher.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    return publisher;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating publisher: {}", publisher.getName(), e);
            throw new RuntimeException("Could not create publisher: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean update(Publisher publisher) {
        String sql = "UPDATE publishers SET name = ?, contact_email = ?, phone = ?, website = ?, address = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, publisher.getName());
            ps.setString(2, publisher.getContactEmail());
            ps.setString(3, publisher.getPhone());
            ps.setString(4, publisher.getWebsite());
            ps.setString(5, publisher.getAddress());
            ps.setInt(6, publisher.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating publisher: {}", publisher.getId(), e);
            throw new RuntimeException("Could not update publisher: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM publishers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting publisher: {}", id, e);
            throw new RuntimeException("Cannot delete publisher: " + e.getMessage(), e);
        }
    }

    private Publisher mapPublisher(ResultSet rs) throws SQLException {
        Publisher p = new Publisher();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setContactEmail(rs.getString("contact_email"));
        p.setPhone(rs.getString("phone"));
        p.setWebsite(rs.getString("website"));
        p.setAddress(rs.getString("address"));
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return p;
    }
}
