package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.BookCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookCopyDAO {

    private static final Logger logger = LoggerFactory.getLogger(BookCopyDAO.class);

    public BookCopy findById(long id) {
        String sql = "SELECT bc.*, b.title AS book_title, b.isbn " +
                     "FROM book_copies bc JOIN books b ON bc.book_id = b.id " +
                     "WHERE bc.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCopy(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding book copy by id: {}", id, e);
        }
        return null;
    }

    /**
     * Pessimistic row-level lock for concurrency-safe issue transactions.
     * Prevents race conditions where two simultaneous checkouts attempt to issue the same copy.
     */
    public BookCopy findByIdForUpdate(long id, Connection conn) throws SQLException {
        String sql = "SELECT bc.*, b.title AS book_title, b.isbn " +
                     "FROM book_copies bc JOIN books b ON bc.book_id = b.id " +
                     "WHERE bc.id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCopy(rs);
                }
            }
        }
        return null;
    }

    public BookCopy findByAccessionNumber(String accession) {
        String sql = "SELECT bc.*, b.title AS book_title, b.isbn " +
                     "FROM book_copies bc JOIN books b ON bc.book_id = b.id " +
                     "WHERE LOWER(bc.accession_number) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accession.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCopy(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding book copy by accession number: {}", accession, e);
        }
        return null;
    }

    public List<BookCopy> findByBookId(long bookId) {
        String sql = "SELECT bc.*, b.title AS book_title, b.isbn " +
                     "FROM book_copies bc JOIN books b ON bc.book_id = b.id " +
                     "WHERE bc.book_id = ? ORDER BY bc.accession_number ASC";
        List<BookCopy> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCopy(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding book copies for book: {}", bookId, e);
        }
        return list;
    }

    public BookCopy findFirstAvailableCopyByBookId(long bookId, Connection conn) throws SQLException {
        String sql = "SELECT bc.*, b.title AS book_title, b.isbn " +
                     "FROM book_copies bc JOIN books b ON bc.book_id = b.id " +
                     "WHERE bc.book_id = ? AND bc.status = 'AVAILABLE' " +
                     "ORDER BY bc.id ASC LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCopy(rs);
                }
            }
        }
        return null;
    }

    public BookCopy create(BookCopy copy, Connection conn) throws SQLException {
        String sql = "INSERT INTO book_copies (book_id, accession_number, status, condition) " +
                     "VALUES (?, ?, ?, ?) RETURNING id, created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, copy.getBookId());
            ps.setString(2, copy.getAccessionNumber());
            ps.setString(3, copy.getStatus() != null ? copy.getStatus() : "AVAILABLE");
            ps.setString(4, copy.getCondition() != null ? copy.getCondition() : "Good");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    copy.setId(rs.getLong("id"));
                    copy.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    copy.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return copy;
                }
            }
        }
        return null;
    }

    public boolean updateStatus(long copyId, String status, Connection conn) throws SQLException {
        String sql = "UPDATE book_copies SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, copyId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateCondition(long copyId, String condition) {
        String sql = "UPDATE book_copies SET condition = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, condition);
            ps.setLong(2, copyId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating copy condition: {}", copyId, e);
            return false;
        }
    }

    public boolean delete(long copyId) {
        String sql = "DELETE FROM book_copies WHERE id = ? AND NOT EXISTS (SELECT 1 FROM loans WHERE book_copy_id = ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, copyId);
            ps.setLong(2, copyId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting copy: {}", copyId, e);
            return false;
        }
    }

    private BookCopy mapCopy(ResultSet rs) throws SQLException {
        BookCopy bc = new BookCopy();
        bc.setId(rs.getLong("id"));
        bc.setBookId(rs.getLong("book_id"));
        bc.setBookTitle(rs.getString("book_title"));
        bc.setIsbn(rs.getString("isbn"));
        bc.setAccessionNumber(rs.getString("accession_number"));
        bc.setStatus(rs.getString("status"));
        bc.setCondition(rs.getString("condition"));
        bc.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        bc.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return bc;
    }
}
