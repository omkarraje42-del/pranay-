package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogDAO.class);

    public void log(AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (log.getUserId() != null) {
                ps.setLong(1, log.getUserId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, log.getAction());
            ps.setString(3, log.getEntityType());
            if (log.getEntityId() != null) {
                ps.setLong(4, log.getEntityId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, log.getDetails());
            ps.setString(6, log.getIpAddress());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error writing audit log", e);
        }
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT al.*, u.username FROM audit_logs al " +
                     "LEFT JOIN users u ON al.user_id = u.id " +
                     "ORDER BY al.id DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog al = new AuditLog();
                    al.setId(rs.getLong("id"));
                    long uid = rs.getLong("user_id");
                    if (!rs.wasNull()) al.setUserId(uid);
                    al.setUsername(rs.getString("username"));
                    al.setAction(rs.getString("action"));
                    al.setEntityType(rs.getString("entity_type"));
                    long eid = rs.getLong("entity_id");
                    if (!rs.wasNull()) al.setEntityId(eid);
                    al.setDetails(rs.getString("details"));
                    al.setIpAddress(rs.getString("ip_address"));
                    al.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    list.add(al);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding recent audit logs", e);
        }
        return list;
    }
}
