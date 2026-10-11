package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Book;
import com.library.model.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DashboardDAO {

    private static final Logger logger = LoggerFactory.getLogger(DashboardDAO.class);

    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new HashMap<>();

        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM books WHERE is_active = true) AS total_titles, " +
                     "(SELECT COUNT(*) FROM book_copies) AS total_copies, " +
                     "(SELECT COUNT(*) FROM book_copies WHERE status = 'AVAILABLE') AS available_copies, " +
                     "(SELECT COUNT(*) FROM book_copies WHERE status = 'BORROWED') AS borrowed_copies, " +
                     "(SELECT COUNT(*) FROM members) AS total_members, " +
                     "(SELECT COUNT(*) FROM members WHERE status = 'ACTIVE') AS active_members, " +
                     "(SELECT COUNT(*) FROM loans WHERE status IN ('ACTIVE', 'OVERDUE') AND due_date < CURRENT_DATE) AS overdue_loans, " +
                     "COALESCE((SELECT SUM(amount) FROM fines WHERE status = 'UNPAID'), 0) AS outstanding_fines, " +
                     "(SELECT COUNT(*) FROM loans WHERE issue_date = CURRENT_DATE) AS issued_today, " +
                     "(SELECT COUNT(*) FROM loans WHERE return_date = CURRENT_DATE) AS returned_today, " +
                     "(SELECT COUNT(*) FROM reservations WHERE status = 'WAITING') AS pending_reservations";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.put("totalTitles", rs.getLong("total_titles"));
                stats.put("totalCopies", rs.getLong("total_copies"));
                stats.put("availableCopies", rs.getLong("available_copies"));
                stats.put("borrowedCopies", rs.getLong("borrowed_copies"));
                stats.put("totalMembers", rs.getLong("total_members"));
                stats.put("activeMembers", rs.getLong("active_members"));
                stats.put("overdueLoans", rs.getLong("overdue_loans"));
                stats.put("outstandingFines", rs.getBigDecimal("outstanding_fines"));
                stats.put("issuedToday", rs.getLong("issued_today"));
                stats.put("returnedToday", rs.getLong("returned_today"));
                stats.put("pendingReservations", rs.getLong("pending_reservations"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving dashboard summary stats", e);
            // Fallback default zeroes
            stats.put("totalTitles", 0L);
            stats.put("totalCopies", 0L);
            stats.put("availableCopies", 0L);
            stats.put("borrowedCopies", 0L);
            stats.put("totalMembers", 0L);
            stats.put("activeMembers", 0L);
            stats.put("overdueLoans", 0L);
            stats.put("outstandingFines", BigDecimal.ZERO);
            stats.put("issuedToday", 0L);
            stats.put("returnedToday", 0L);
            stats.put("pendingReservations", 0L);
        }
        return stats;
    }

    public Map<String, Object> getBorrowingActivityChartData() {
        Map<String, Object> chartData = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> issues = new ArrayList<>();
        List<Long> returns = new ArrayList<>();

        // Last 7 days activity
        String sql = "WITH days AS ( " +
                     "    SELECT generate_series(CURRENT_DATE - INTERVAL '6 days', CURRENT_DATE, INTERVAL '1 day')::date AS day " +
                     ") " +
                     "SELECT d.day, " +
                     "       COUNT(l_issue.id) AS issued_count, " +
                     "       COUNT(l_return.id) AS returned_count " +
                     "FROM days d " +
                     "LEFT JOIN loans l_issue ON l_issue.issue_date = d.day " +
                     "LEFT JOIN loans l_return ON l_return.return_date = d.day " +
                     "GROUP BY d.day ORDER BY d.day ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                labels.add(rs.getDate("day").toString());
                issues.add(rs.getLong("issued_count"));
                returns.add(rs.getLong("returned_count"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving borrowing activity chart data", e);
        }

        chartData.put("labels", labels);
        chartData.put("issues", issues);
        chartData.put("returns", returns);
        return chartData;
    }

    public List<Map<String, Object>> getCategoryDistribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.name, COUNT(b.id) AS book_count, " +
                     "(SELECT COUNT(*) FROM book_copies bc JOIN books b2 ON bc.book_id = b2.id WHERE b2.category_id = c.id) AS copy_count " +
                     "FROM categories c " +
                     "LEFT JOIN books b ON b.category_id = c.id AND b.is_active = true " +
                     "GROUP BY c.id, c.name " +
                     "HAVING COUNT(b.id) > 0 " +
                     "ORDER BY book_count DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("category", rs.getString("name"));
                map.put("bookCount", rs.getLong("book_count"));
                map.put("copyCount", rs.getLong("copy_count"));
                list.add(map);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving category distribution", e);
        }
        return list;
    }

    public List<Map<String, Object>> getPopularBooks(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT b.id, b.title, b.isbn, c.name AS category_name, " +
                     "COUNT(l.id) AS borrow_count " +
                     "FROM books b " +
                     "JOIN categories c ON b.category_id = c.id " +
                     "JOIN book_copies bc ON bc.book_id = b.id " +
                     "JOIN loans l ON l.book_copy_id = bc.id " +
                     "WHERE b.is_active = true " +
                     "GROUP BY b.id, b.title, b.isbn, c.name " +
                     "ORDER BY borrow_count DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 5);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getLong("id"));
                    map.put("title", rs.getString("title"));
                    map.put("isbn", rs.getString("isbn"));
                    map.put("category", rs.getString("category_name"));
                    map.put("borrowCount", rs.getLong("borrow_count"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving popular books", e);
        }
        return list;
    }
}
