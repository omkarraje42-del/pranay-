package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Fine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class FineDAO {

    private static final Logger logger = LoggerFactory.getLogger(FineDAO.class);

    public Fine findById(long id) {
        String sql = baseFineQuery() + " WHERE f.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFine(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding fine by id: {}", id, e);
        }
        return null;
    }

    public List<Fine> findAll(Long memberId, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseFineQuery()).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (memberId != null && memberId > 0) {
            sql.append("AND f.member_id = ? ");
            params.add(memberId);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND f.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY f.id DESC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 20);
        params.add(offset >= 0 ? offset : 0);

        List<Fine> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapFine(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding fines", e);
        }
        return list;
    }

    public long count(Long memberId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM fines f WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (memberId != null && memberId > 0) {
            sql.append("AND f.member_id = ? ");
            params.add(memberId);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND f.status = ? ");
            params.add(status.trim().toUpperCase());
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
            logger.error("Error counting fines", e);
        }
        return 0;
    }

    public BigDecimal findUnpaidTotalByMemberId(long memberId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM fines WHERE member_id = ? AND status = 'UNPAID'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error calculating unpaid fines for member: {}", memberId, e);
        }
        return BigDecimal.ZERO;
    }

    public Fine create(Fine fine, Connection conn) throws SQLException {
        String sql = "INSERT INTO fines (loan_id, member_id, amount, reason, status) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id, created_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fine.getLoanId());
            ps.setLong(2, fine.getMemberId());
            ps.setBigDecimal(3, fine.getAmount());
            ps.setString(4, fine.getReason() != null ? fine.getReason() : "Overdue return");
            ps.setString(5, fine.getStatus() != null ? fine.getStatus() : "UNPAID");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    fine.setId(rs.getLong("id"));
                    fine.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    return fine;
                }
            }
        }
        return null;
    }

    public boolean recordPayment(long fineId, String paymentMethod, String receiptNumber, Connection conn) throws SQLException {
        String sql = "UPDATE fines SET status = 'PAID', paid_at = CURRENT_TIMESTAMP, " +
                     "payment_method = ?, receipt_number = ? WHERE id = ? AND status = 'UNPAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentMethod != null ? paymentMethod : "CASH");
            ps.setString(2, receiptNumber);
            ps.setLong(3, fineId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean waiveFine(long fineId, Long waivedBy, String waiverReason, Connection conn) throws SQLException {
        String sql = "UPDATE fines SET status = 'WAIVED', waived_at = CURRENT_TIMESTAMP, " +
                     "waived_by = ?, waiver_reason = ? WHERE id = ? AND status = 'UNPAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (waivedBy != null) {
                ps.setLong(1, waivedBy);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, waiverReason);
            ps.setLong(3, fineId);
            return ps.executeUpdate() > 0;
        }
    }

    private String baseFineQuery() {
        return "SELECT f.*, " +
               "m.first_name AS member_first_name, m.last_name AS member_last_name, m.membership_number, " +
               "b.title AS book_title, bc.accession_number, " +
               "u.first_name AS waived_first_name, u.last_name AS waived_last_name " +
               "FROM fines f " +
               "JOIN members m ON f.member_id = m.id " +
               "JOIN loans l ON f.loan_id = l.id " +
               "JOIN book_copies bc ON l.book_copy_id = bc.id " +
               "JOIN books b ON bc.book_id = b.id " +
               "LEFT JOIN users u ON f.waived_by = u.id";
    }

    private Fine mapFine(ResultSet rs) throws SQLException {
        Fine f = new Fine();
        f.setId(rs.getLong("id"));
        f.setLoanId(rs.getLong("loan_id"));
        f.setMemberId(rs.getLong("member_id"));
        String fn = rs.getString("member_first_name");
        String ln = rs.getString("member_last_name");
        f.setMemberName((fn != null ? fn : "") + " " + (ln != null ? ln : ""));
        f.setMembershipNumber(rs.getString("membership_number"));
        f.setBookTitle(rs.getString("book_title"));
        f.setAccessionNumber(rs.getString("accession_number"));
        f.setAmount(rs.getBigDecimal("amount"));
        f.setReason(rs.getString("reason"));
        f.setStatus(rs.getString("status"));
        f.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        f.setPaidAt(rs.getObject("paid_at", OffsetDateTime.class));
        f.setWaivedAt(rs.getObject("waived_at", OffsetDateTime.class));

        long wBy = rs.getLong("waived_by");
        if (!rs.wasNull()) {
            f.setWaivedBy(wBy);
            String wfn = rs.getString("waived_first_name");
            String wln = rs.getString("waived_last_name");
            f.setWaivedByName((wfn != null ? wfn : "") + " " + (wln != null ? wln : ""));
        }
        f.setWaiverReason(rs.getString("waiver_reason"));
        f.setPaymentMethod(rs.getString("payment_method"));
        f.setReceiptNumber(rs.getString("receipt_number"));
        return f;
    }
}
