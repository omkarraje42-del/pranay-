package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    private static final Logger logger = LoggerFactory.getLogger(LoanDAO.class);

    public Loan findById(long id) {
        String sql = baseLoanQuery() + " WHERE l.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLoan(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding loan by id: {}", id, e);
        }
        return null;
    }

    public Loan findByIdForUpdate(long id, Connection conn) throws SQLException {
        String sql = baseLoanQuery() + " WHERE l.id = ? FOR UPDATE OF l";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLoan(rs);
                }
            }
        }
        return null;
    }

    public List<Loan> findAll(String search, Long memberId, Long bookId, String status,
                              LocalDate fromDate, LocalDate toDate, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseLoanQuery()).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(b.title) LIKE ? OR LOWER(m.first_name) LIKE ? OR LOWER(m.last_name) LIKE ? " +
                       "OR LOWER(m.membership_number) LIKE ? OR LOWER(bc.accession_number) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (memberId != null && memberId > 0) {
            sql.append("AND l.member_id = ? ");
            params.add(memberId);
        }

        if (bookId != null && bookId > 0) {
            sql.append("AND b.id = ? ");
            params.add(bookId);
        }

        if (status != null && !status.trim().isEmpty()) {
            if ("OVERDUE".equalsIgnoreCase(status)) {
                sql.append("AND (l.status = 'OVERDUE' OR (l.status = 'ACTIVE' AND l.due_date < CURRENT_DATE)) ");
            } else {
                sql.append("AND l.status = ? ");
                params.add(status.trim().toUpperCase());
            }
        }

        if (fromDate != null) {
            sql.append("AND l.issue_date >= ? ");
            params.add(Date.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append("AND l.issue_date <= ? ");
            params.add(Date.valueOf(toDate));
        }

        sql.append("ORDER BY l.id DESC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 20);
        params.add(offset >= 0 ? offset : 0);

        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLoan(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding all loans", e);
        }
        return list;
    }

    public long count(String search, Long memberId, Long bookId, String status, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM loans l ")
                .append("JOIN book_copies bc ON l.book_copy_id = bc.id ")
                .append("JOIN books b ON bc.book_id = b.id ")
                .append("JOIN members m ON l.member_id = m.id ")
                .append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(b.title) LIKE ? OR LOWER(m.first_name) LIKE ? OR LOWER(m.last_name) LIKE ? " +
                       "OR LOWER(m.membership_number) LIKE ? OR LOWER(bc.accession_number) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (memberId != null && memberId > 0) {
            sql.append("AND l.member_id = ? ");
            params.add(memberId);
        }

        if (bookId != null && bookId > 0) {
            sql.append("AND b.id = ? ");
            params.add(bookId);
        }

        if (status != null && !status.trim().isEmpty()) {
            if ("OVERDUE".equalsIgnoreCase(status)) {
                sql.append("AND (l.status = 'OVERDUE' OR (l.status = 'ACTIVE' AND l.due_date < CURRENT_DATE)) ");
            } else {
                sql.append("AND l.status = ? ");
                params.add(status.trim().toUpperCase());
            }
        }

        if (fromDate != null) {
            sql.append("AND l.issue_date >= ? ");
            params.add(Date.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append("AND l.issue_date <= ? ");
            params.add(Date.valueOf(toDate));
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
            logger.error("Error counting loans", e);
        }
        return 0;
    }

    public List<Loan> findActiveByMemberId(long memberId) {
        String sql = baseLoanQuery() + " WHERE l.member_id = ? AND l.status IN ('ACTIVE', 'OVERDUE') ORDER BY l.due_date ASC";
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLoan(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding active loans for member: {}", memberId, e);
        }
        return list;
    }

    public int countActiveByMemberId(long memberId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE member_id = ? AND status IN ('ACTIVE', 'OVERDUE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Loan create(Loan loan, Connection conn) throws SQLException {
        String sql = "INSERT INTO loans (book_copy_id, member_id, issued_by, issue_date, due_date, status, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loan.getBookCopyId());
            ps.setLong(2, loan.getMemberId());
            if (loan.getIssuedBy() != null) {
                ps.setLong(3, loan.getIssuedBy());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setDate(4, Date.valueOf(loan.getIssueDate() != null ? loan.getIssueDate() : LocalDate.now()));
            ps.setDate(5, Date.valueOf(loan.getDueDate()));
            ps.setString(6, loan.getStatus() != null ? loan.getStatus() : "ACTIVE");
            ps.setString(7, loan.getNotes());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loan.setId(rs.getLong("id"));
                    loan.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    loan.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return loan;
                }
            }
        }
        return null;
    }

    public boolean updateReturn(long loanId, LocalDate returnDate, Long returnedBy, String status, String notes, Connection conn) throws SQLException {
        String sql = "UPDATE loans SET return_date = ?, returned_by = ?, status = ?, notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            if (returnedBy != null) {
                ps.setLong(2, returnedBy);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, status);
            ps.setString(4, notes);
            ps.setLong(5, loanId);

            return ps.executeUpdate() > 0;
        }
    }

    public List<Loan> findOverdueLoans() {
        String sql = baseLoanQuery() + " WHERE l.status IN ('ACTIVE', 'OVERDUE') AND l.due_date < CURRENT_DATE ORDER BY l.due_date ASC";
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapLoan(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding overdue loans", e);
        }
        return list;
    }

    private String baseLoanQuery() {
        return "SELECT l.*, " +
               "bc.accession_number, bc.book_id, b.title AS book_title, b.isbn, " +
               "m.first_name AS member_first_name, m.last_name AS member_last_name, m.membership_number, m.email AS member_email, " +
               "u1.first_name AS issued_first_name, u1.last_name AS issued_last_name, " +
               "u2.first_name AS returned_first_name, u2.last_name AS returned_last_name, " +
               "f.amount AS fine_amount, f.status AS fine_status " +
               "FROM loans l " +
               "JOIN book_copies bc ON l.book_copy_id = bc.id " +
               "JOIN books b ON bc.book_id = b.id " +
               "JOIN members m ON l.member_id = m.id " +
               "LEFT JOIN users u1 ON l.issued_by = u1.id " +
               "LEFT JOIN users u2 ON l.returned_by = u2.id " +
               "LEFT JOIN fines f ON f.loan_id = l.id";
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        Loan l = new Loan();
        l.setId(rs.getLong("id"));
        l.setBookCopyId(rs.getLong("book_copy_id"));
        l.setAccessionNumber(rs.getString("accession_number"));
        l.setBookId(rs.getLong("book_id"));
        l.setBookTitle(rs.getString("book_title"));
        l.setIsbn(rs.getString("isbn"));
        l.setMemberId(rs.getLong("member_id"));
        String mf = rs.getString("member_first_name");
        String ml = rs.getString("member_last_name");
        l.setMemberName((mf != null ? mf : "") + " " + (ml != null ? ml : ""));
        l.setMembershipNumber(rs.getString("membership_number"));
        l.setMemberEmail(rs.getString("member_email"));

        long iBy = rs.getLong("issued_by");
        if (!rs.wasNull()) {
            l.setIssuedBy(iBy);
            String ifn = rs.getString("issued_first_name");
            String iln = rs.getString("issued_last_name");
            l.setIssuedByName((ifn != null ? ifn : "") + " " + (iln != null ? iln : ""));
        }

        long rBy = rs.getLong("returned_by");
        if (!rs.wasNull()) {
            l.setReturnedBy(rBy);
            String rfn = rs.getString("returned_first_name");
            String rln = rs.getString("returned_last_name");
            l.setReturnedByName((rfn != null ? rfn : "") + " " + (rln != null ? rln : ""));
        }

        Date iDate = rs.getDate("issue_date");
        if (iDate != null) l.setIssueDate(iDate.toLocalDate());
        Date dDate = rs.getDate("due_date");
        if (dDate != null) l.setDueDate(dDate.toLocalDate());
        Date rDate = rs.getDate("return_date");
        if (rDate != null) l.setReturnDate(rDate.toLocalDate());

        l.setStatus(rs.getString("status"));
        l.setNotes(rs.getString("notes"));
        l.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        l.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

        // Overdue calculation
        LocalDate refDate = l.getReturnDate() != null ? l.getReturnDate() : LocalDate.now();
        if (l.getDueDate() != null && refDate.isAfter(l.getDueDate())) {
            l.setOverdueDays(ChronoUnit.DAYS.between(l.getDueDate(), refDate));
        } else {
            l.setOverdueDays(0);
        }

        BigDecimal fineAmt = rs.getBigDecimal("fine_amount");
        l.setFineAmount(fineAmt);
        l.setFineStatus(rs.getString("fine_status"));

        return l;
    }
}
