package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Author;
import com.library.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class BookDAO {

    private static final Logger logger = LoggerFactory.getLogger(BookDAO.class);
    private final AuthorDAO authorDAO = new AuthorDAO();

    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "b.title", "b.isbn", "b.publication_year", "b.created_at", "total_copies", "available_copies"
    ));

    public Book findById(long id) {
        String sql = "SELECT b.*, c.name AS category_name, p.name AS publisher_name, " +
                     "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id) AS total_copies, " +
                     "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE') AS available_copies, " +
                     "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'BORROWED') AS borrowed_copies " +
                     "FROM books b " +
                     "JOIN categories c ON b.category_id = c.id " +
                     "LEFT JOIN publishers p ON b.publisher_id = p.id " +
                     "WHERE b.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book book = mapBook(rs);
                    book.setAuthors(authorDAO.findByBookId(book.getId()));
                    return book;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding book by id: {}", id, e);
        }
        return null;
    }

    public Book findByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) return null;
        String sql = "SELECT b.*, c.name AS category_name, p.name AS publisher_name, " +
                     "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id) AS total_copies, " +
                     "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE') AS available_copies, " +
                     "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'BORROWED') AS borrowed_copies " +
                     "FROM books b " +
                     "JOIN categories c ON b.category_id = c.id " +
                     "LEFT JOIN publishers p ON b.publisher_id = p.id " +
                     "WHERE REPLACE(REPLACE(LOWER(b.isbn), '-', ''), ' ', '') = REPLACE(REPLACE(LOWER(?), '-', ''), ' ', '')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book book = mapBook(rs);
                    book.setAuthors(authorDAO.findByBookId(book.getId()));
                    return book;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding book by isbn: {}", isbn, e);
        }
        return null;
    }

    public List<Book> findAll(String search, Integer categoryId, Integer authorId, String availability,
                              Boolean activeOnly, String sortField, String sortOrder, int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT b.*, c.name AS category_name, p.name AS publisher_name, " +
                "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id) AS total_copies, " +
                "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE') AS available_copies, " +
                "(SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'BORROWED') AS borrowed_copies " +
                "FROM books b " +
                "JOIN categories c ON b.category_id = c.id " +
                "LEFT JOIN publishers p ON b.publisher_id = p.id " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (activeOnly != null && activeOnly) {
            sql.append("AND b.is_active = true ");
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(b.title) LIKE ? OR LOWER(b.isbn) LIKE ? OR LOWER(b.subtitle) LIKE ? " +
                       "OR EXISTS (SELECT 1 FROM book_authors ba JOIN authors a ON ba.author_id = a.id " +
                       "           WHERE ba.book_id = b.id AND (LOWER(a.first_name) LIKE ? OR LOWER(a.last_name) LIKE ?))) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append("AND b.category_id = ? ");
            params.add(categoryId);
        }

        if (authorId != null && authorId > 0) {
            sql.append("AND EXISTS (SELECT 1 FROM book_authors ba WHERE ba.book_id = b.id AND ba.author_id = ?) ");
            params.add(authorId);
        }

        if (availability != null && !availability.trim().isEmpty()) {
            if ("AVAILABLE".equalsIgnoreCase(availability)) {
                sql.append("AND (SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE') > 0 ");
            } else if ("UNAVAILABLE".equalsIgnoreCase(availability)) {
                sql.append("AND (SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE') = 0 ");
            }
        }

        // Safe Sorting
        String orderCol = "b.id";
        if (sortField != null) {
            String candidate = sortField.toLowerCase().trim();
            if ("title".equals(candidate)) orderCol = "b.title";
            else if ("isbn".equals(candidate)) orderCol = "b.isbn";
            else if ("year".equals(candidate) || "publication_year".equals(candidate)) orderCol = "b.publication_year";
            else if ("created_at".equals(candidate)) orderCol = "b.created_at";
            else if ("available".equals(candidate)) orderCol = "available_copies";
            else if ("copies".equals(candidate)) orderCol = "total_copies";
        }
        String orderDir = "DESC".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        sql.append("ORDER BY ").append(orderCol).append(" ").append(orderDir).append(" LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 20);
        params.add(offset >= 0 ? offset : 0);

        List<Book> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Book b = mapBook(rs);
                    list.add(b);
                }
            }
            // Populate authors in batch or per book
            for (Book b : list) {
                b.setAuthors(authorDAO.findByBookId(b.getId()));
            }
        } catch (SQLException e) {
            logger.error("Error finding all books", e);
        }
        return list;
    }

    public long count(String search, Integer categoryId, Integer authorId, String availability, Boolean activeOnly) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM books b WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (activeOnly != null && activeOnly) {
            sql.append("AND b.is_active = true ");
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(b.title) LIKE ? OR LOWER(b.isbn) LIKE ? OR LOWER(b.subtitle) LIKE ? " +
                       "OR EXISTS (SELECT 1 FROM book_authors ba JOIN authors a ON ba.author_id = a.id " +
                       "           WHERE ba.book_id = b.id AND (LOWER(a.first_name) LIKE ? OR LOWER(a.last_name) LIKE ?))) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append("AND b.category_id = ? ");
            params.add(categoryId);
        }

        if (authorId != null && authorId > 0) {
            sql.append("AND EXISTS (SELECT 1 FROM book_authors ba WHERE ba.book_id = b.id AND ba.author_id = ?) ");
            params.add(authorId);
        }

        if (availability != null && !availability.trim().isEmpty()) {
            if ("AVAILABLE".equalsIgnoreCase(availability)) {
                sql.append("AND (SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE') > 0 ");
            } else if ("UNAVAILABLE".equalsIgnoreCase(availability)) {
                sql.append("AND (SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE') = 0 ");
            }
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
            logger.error("Error counting books", e);
        }
        return 0;
    }

    public Book create(Book book, Connection conn) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, subtitle, category_id, publisher_id, publication_year, " +
                     "edition, language, description, shelf_location, cover_image_url, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getSubtitle());
            ps.setInt(4, book.getCategoryId());
            if (book.getPublisherId() != null) {
                ps.setInt(5, book.getPublisherId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            if (book.getPublicationYear() != null) {
                ps.setInt(6, book.getPublicationYear());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, book.getEdition());
            ps.setString(8, book.getLanguage() != null ? book.getLanguage() : "English");
            ps.setString(9, book.getDescription());
            ps.setString(10, book.getShelfLocation());
            ps.setString(11, book.getCoverImageUrl());
            ps.setBoolean(12, book.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    book.setId(rs.getLong("id"));
                    book.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    book.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return book;
                }
            }
        }
        return null;
    }

    public boolean update(Book book, Connection conn) throws SQLException {
        String sql = "UPDATE books SET isbn = ?, title = ?, subtitle = ?, category_id = ?, publisher_id = ?, " +
                     "publication_year = ?, edition = ?, language = ?, description = ?, shelf_location = ?, " +
                     "cover_image_url = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getSubtitle());
            ps.setInt(4, book.getCategoryId());
            if (book.getPublisherId() != null) {
                ps.setInt(5, book.getPublisherId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            if (book.getPublicationYear() != null) {
                ps.setInt(6, book.getPublicationYear());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, book.getEdition());
            ps.setString(8, book.getLanguage());
            ps.setString(9, book.getDescription());
            ps.setString(10, book.getShelfLocation());
            ps.setString(11, book.getCoverImageUrl());
            ps.setBoolean(12, book.isActive());
            ps.setLong(13, book.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean setActive(long id, boolean active) {
        String sql = "UPDATE books SET is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error setting active status on book: {}", id, e);
            return false;
        }
    }

    public void setBookAuthors(long bookId, List<Integer> authorIds, Connection conn) throws SQLException {
        // Delete existing links
        String delSql = "DELETE FROM book_authors WHERE book_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(delSql)) {
            ps.setLong(1, bookId);
            ps.executeUpdate();
        }

        if (authorIds != null && !authorIds.isEmpty()) {
            String insSql = "INSERT INTO book_authors (book_id, author_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
            try (PreparedStatement ps = conn.prepareStatement(insSql)) {
                for (Integer authorId : authorIds) {
                    if (authorId != null && authorId > 0) {
                        ps.setLong(1, bookId);
                        ps.setInt(2, authorId);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        }
    }

    private Book mapBook(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setId(rs.getLong("id"));
        b.setIsbn(rs.getString("isbn"));
        b.setTitle(rs.getString("title"));
        b.setSubtitle(rs.getString("subtitle"));
        b.setCategoryId(rs.getInt("category_id"));
        b.setCategoryName(rs.getString("category_name"));
        int pId = rs.getInt("publisher_id");
        if (!rs.wasNull()) {
            b.setPublisherId(pId);
        }
        b.setPublisherName(rs.getString("publisher_name"));
        int year = rs.getInt("publication_year");
        if (!rs.wasNull()) {
            b.setPublicationYear(year);
        }
        b.setEdition(rs.getString("edition"));
        b.setLanguage(rs.getString("language"));
        b.setDescription(rs.getString("description"));
        b.setShelfLocation(rs.getString("shelf_location"));
        b.setCoverImageUrl(rs.getString("cover_image_url"));
        b.setActive(rs.getBoolean("is_active"));
        b.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        b.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

        b.setTotalCopies(rs.getInt("total_copies"));
        b.setAvailableCopies(rs.getInt("available_copies"));
        b.setBorrowedCopies(rs.getInt("borrowed_copies"));
        return b;
    }
}
