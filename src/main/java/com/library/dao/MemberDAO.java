package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    private static final Logger logger = LoggerFactory.getLogger(MemberDAO.class);

    public Member findById(long id) {
        String sql = "SELECT m.*, " +
                     "(SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.status IN ('ACTIVE', 'OVERDUE')) AS active_loans, " +
                     "COALESCE((SELECT SUM(f.amount) FROM fines f WHERE f.member_id = m.id AND f.status = 'UNPAID'), 0) AS unpaid_fines " +
                     "FROM members m WHERE m.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMember(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding member by id: {}", id, e);
        }
        return null;
    }

    public Member findByMembershipNumber(String number) {
        String sql = "SELECT m.*, " +
                     "(SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.status IN ('ACTIVE', 'OVERDUE')) AS active_loans, " +
                     "COALESCE((SELECT SUM(f.amount) FROM fines f WHERE f.member_id = m.id AND f.status = 'UNPAID'), 0) AS unpaid_fines " +
                     "FROM members m WHERE LOWER(m.membership_number) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMember(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding member by membership number: {}", number, e);
        }
        return null;
    }

    public Member findByEmail(String email) {
        String sql = "SELECT m.*, " +
                     "(SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.status IN ('ACTIVE', 'OVERDUE')) AS active_loans, " +
                     "COALESCE((SELECT SUM(f.amount) FROM fines f WHERE f.member_id = m.id AND f.status = 'UNPAID'), 0) AS unpaid_fines " +
                     "FROM members m WHERE LOWER(m.email) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMember(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding member by email: {}", email, e);
        }
        return null;
    }

    public Member findByUserId(long userId) {
        String sql = "SELECT m.*, " +
                     "(SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.status IN ('ACTIVE', 'OVERDUE')) AS active_loans, " +
                     "COALESCE((SELECT SUM(f.amount) FROM fines f WHERE f.member_id = m.id AND f.status = 'UNPAID'), 0) AS unpaid_fines " +
                     "FROM members m WHERE m.user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMember(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding member by user id: {}", userId, e);
        }
        return null;
    }

    public List<Member> findAll(String search, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT m.*, ")
                .append("(SELECT COUNT(*) FROM loans l WHERE l.member_id = m.id AND l.status IN ('ACTIVE', 'OVERDUE')) AS active_loans, ")
                .append("COALESCE((SELECT SUM(f.amount) FROM fines f WHERE f.member_id = m.id AND f.status = 'UNPAID'), 0) AS unpaid_fines ")
                .append("FROM members m WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(m.first_name) LIKE ? OR LOWER(m.last_name) LIKE ? OR LOWER(m.membership_number) LIKE ? OR LOWER(m.email) LIKE ? OR m.phone LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND m.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY m.id DESC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 20);
        params.add(offset >= 0 ? offset : 0);

        List<Member> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMember(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding all members", e);
        }
        return list;
    }

    public long count(String search, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM members m WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(m.first_name) LIKE ? OR LOWER(m.last_name) LIKE ? OR LOWER(m.membership_number) LIKE ? OR LOWER(m.email) LIKE ? OR m.phone LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND m.status = ? ");
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
            logger.error("Error counting members", e);
        }
        return 0;
    }

    public Member create(Member member) {
        String sql = "INSERT INTO members (user_id, membership_number, first_name, last_name, email, phone, address, " +
                     "membership_start_date, membership_expiry_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at, updated_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (member.getUserId() != null) {
                ps.setLong(1, member.getUserId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, member.getMembershipNumber());
            ps.setString(3, member.getFirstName());
            ps.setString(4, member.getLastName());
            ps.setString(5, member.getEmail());
            ps.setString(6, member.getPhone());
            ps.setString(7, member.getAddress());
            ps.setDate(8, Date.valueOf(member.getMembershipStartDate() != null ? member.getMembershipStartDate() : LocalDate.now()));
            ps.setDate(9, Date.valueOf(member.getMembershipExpiryDate()));
            ps.setString(10, member.getStatus() != null ? member.getStatus() : "ACTIVE");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    member.setId(rs.getLong("id"));
                    member.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    member.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return member;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating member: {}", member.getMembershipNumber(), e);
            throw new RuntimeException("Could not create member: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean update(Member member) {
        String sql = "UPDATE members SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ?, " +
                     "membership_start_date = ?, membership_expiry_date = ?, status = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getFirstName());
            ps.setString(2, member.getLastName());
            ps.setString(3, member.getEmail());
            ps.setString(4, member.getPhone());
            ps.setString(5, member.getAddress());
            ps.setDate(6, Date.valueOf(member.getMembershipStartDate()));
            ps.setDate(7, Date.valueOf(member.getMembershipExpiryDate()));
            ps.setString(8, member.getStatus());
            ps.setLong(9, member.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating member: {}", member.getId(), e);
            throw new RuntimeException("Could not update member: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(long memberId, String status) {
        String sql = "UPDATE members SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating member status: {}", memberId, e);
            return false;
        }
    }

    private Member mapMember(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setId(rs.getLong("id"));
        long uId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            m.setUserId(uId);
        }
        m.setMembershipNumber(rs.getString("membership_number"));
        m.setFirstName(rs.getString("first_name"));
        m.setLastName(rs.getString("last_name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        m.setAddress(rs.getString("address"));
        Date sDate = rs.getDate("membership_start_date");
        if (sDate != null) m.setMembershipStartDate(sDate.toLocalDate());
        Date eDate = rs.getDate("membership_expiry_date");
        if (eDate != null) m.setMembershipExpiryDate(eDate.toLocalDate());
        m.setStatus(rs.getString("status"));
        m.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        m.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

        m.setActiveLoansCount(rs.getInt("active_loans"));
        BigDecimal fines = rs.getBigDecimal("unpaid_fines");
        m.setUnpaidFinesTotal(fines != null ? fines : BigDecimal.ZERO);
        return m;
    }
}
