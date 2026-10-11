package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    private static final Logger logger = LoggerFactory.getLogger(ReservationDAO.class);

    public Reservation findById(long id) {
        String sql = baseReservationQuery() + " WHERE r.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reservation by id: {}", id, e);
        }
        return null;
    }

    public Reservation findActiveByBookAndMember(long bookId, long memberId) {
        String sql = baseReservationQuery() + " WHERE r.book_id = ? AND r.member_id = ? AND r.status IN ('WAITING', 'READY_FOR_PICKUP')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding active reservation: book={}, member={}", bookId, memberId, e);
        }
        return null;
    }

    public List<Reservation> findAll(Long memberId, Long bookId, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseReservationQuery()).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (memberId != null && memberId > 0) {
            sql.append("AND r.member_id = ? ");
            params.add(memberId);
        }

        if (bookId != null && bookId > 0) {
            sql.append("AND r.book_id = ? ");
            params.add(bookId);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND r.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY r.priority ASC, r.reservation_date ASC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 20);
        params.add(offset >= 0 ? offset : 0);

        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations", e);
        }
        return list;
    }

    public long count(Long memberId, Long bookId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM reservations r WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (memberId != null && memberId > 0) {
            sql.append("AND r.member_id = ? ");
            params.add(memberId);
        }

        if (bookId != null && bookId > 0) {
            sql.append("AND r.book_id = ? ");
            params.add(bookId);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND r.status = ? ");
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
            logger.error("Error counting reservations", e);
        }
        return 0;
    }

    /**
     * Finds highest priority WAITING reservation for this title.
     * Uses row lock FOR UPDATE for transactional progression when a copy becomes available.
     */
    public Reservation findNextWaitingReservationForBook(long bookId, Connection conn) throws SQLException {
        String sql = baseReservationQuery() +
                     " WHERE r.book_id = ? AND r.status = 'WAITING' " +
                     "ORDER BY r.priority ASC, r.reservation_date ASC LIMIT 1 FOR UPDATE OF r";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public Reservation create(Reservation res, Connection conn) throws SQLException {
        String sql = "INSERT INTO reservations (book_id, member_id, priority, notes) " +
                     "VALUES (?, ?, ?, ?) RETURNING id, reservation_date, status, created_at, updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, res.getBookId());
            ps.setLong(2, res.getMemberId());
            ps.setInt(3, res.getPriority() > 0 ? res.getPriority() : 1);
            ps.setString(4, res.getNotes());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    res.setId(rs.getLong("id"));
                    res.setReservationDate(rs.getObject("reservation_date", OffsetDateTime.class));
                    res.setStatus(rs.getString("status"));
                    res.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    res.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                    return res;
                }
            }
        }
        return null;
    }

    public boolean updateStatus(long id, String status, OffsetDateTime notificationSent, LocalDate pickupDeadline, Connection conn) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, notification_sent_at = ?, pickup_deadline = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (notificationSent != null) {
                ps.setObject(2, notificationSent);
            } else {
                ps.setNull(2, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            if (pickupDeadline != null) {
                ps.setDate(3, Date.valueOf(pickupDeadline));
            } else {
                ps.setNull(3, Types.DATE);
            }
            ps.setLong(4, id);

            return ps.executeUpdate() > 0;
        }
    }

    private String baseReservationQuery() {
        return "SELECT r.*, b.title AS book_title, b.isbn, " +
               "m.first_name AS member_first_name, m.last_name AS member_last_name, m.membership_number " +
               "FROM reservations r " +
               "JOIN books b ON r.book_id = b.id " +
               "JOIN members m ON r.member_id = m.id";
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setBookId(rs.getLong("book_id"));
        r.setBookTitle(rs.getString("book_title"));
        r.setIsbn(rs.getString("isbn"));
        r.setMemberId(rs.getLong("member_id"));
        String fn = rs.getString("member_first_name");
        String ln = rs.getString("member_last_name");
        r.setMemberName((fn != null ? fn : "") + " " + (ln != null ? ln : ""));
        r.setMembershipNumber(rs.getString("membership_number"));
        r.setReservationDate(rs.getObject("reservation_date", OffsetDateTime.class));
        r.setStatus(rs.getString("status"));
        r.setPriority(rs.getInt("priority"));
        r.setNotificationSentAt(rs.getObject("notification_sent_at", OffsetDateTime.class));
        Date pDeadline = rs.getDate("pickup_deadline");
        if (pDeadline != null) r.setPickupDeadline(pDeadline.toLocalDate());
        r.setNotes(rs.getString("notes"));
        r.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        r.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return r;
    }
}
