package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuthorDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuthorDAO.class);

    public List<Author> findAll(String search, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM authors WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR LOWER(nationality) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        sql.append("ORDER BY last_name ASC, first_name ASC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 50);
        params.add(offset >= 0 ? offset : 0);

        List<Author> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAuthor(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding authors", e);
        }
        return list;
    }

    public long count(String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM authors WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR LOWER(nationality) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
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
            logger.error("Error counting authors", e);
        }
        return 0;
    }

    public Author findById(int id) {
        String sql = "SELECT * FROM authors WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAuthor(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding author by id: {}", id, e);
        }
        return null;
    }

    public List<Author> findByBookId(long bookId) {
        String sql = "SELECT a.* FROM authors a " +
                     "JOIN book_authors ba ON a.id = ba.author_id " +
                     "WHERE ba.book_id = ? ORDER BY a.last_name ASC";
        List<Author> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAuthor(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding authors for book: {}", bookId, e);
        }
        return list;
    }

    public Author create(Author author) {
        String sql = "INSERT INTO authors (first_name, last_name, biography, nationality) VALUES (?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, author.getFirstName());
            ps.setString(2, author.getLastName());
            ps.setString(3, author.getBiography());
            ps.setString(4, author.getNationality());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    author.setId(rs.getInt("id"));
                    author.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    return author;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating author: {}", author.getFullName(), e);
            throw new RuntimeException("Could not create author: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean update(Author author) {
        String sql = "UPDATE authors SET first_name = ?, last_name = ?, biography = ?, nationality = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, author.getFirstName());
            ps.setString(2, author.getLastName());
            ps.setString(3, author.getBiography());
            ps.setString(4, author.getNationality());
            ps.setInt(5, author.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating author: {}", author.getId(), e);
            throw new RuntimeException("Could not update author: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM authors WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting author: {}", id, e);
            throw new RuntimeException("Cannot delete author (author may be referenced by books): " + e.getMessage(), e);
        }
    }

    private Author mapAuthor(ResultSet rs) throws SQLException {
        Author a = new Author();
        a.setId(rs.getInt("id"));
        a.setFirstName(rs.getString("first_name"));
        a.setLastName(rs.getString("last_name"));
        a.setBiography(rs.getString("biography"));
        a.setNationality(rs.getString("nationality"));
        a.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return a;
    }
}
