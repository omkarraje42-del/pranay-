package com.library.dao;

import com.library.config.DatabaseConfig;
import com.library.model.LibrarySetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class LibrarySettingDAO {

    private static final Logger logger = LoggerFactory.getLogger(LibrarySettingDAO.class);

    public Map<String, String> getAllSettings() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM library_settings";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            logger.error("Error loading library settings", e);
        }
        return map;
    }

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM library_settings WHERE setting_key = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        } catch (SQLException e) {
            logger.error("Error reading setting: {}", key, e);
        }
        return defaultValue;
    }

    public int getIntSetting(String key, int defaultValue) {
        String val = getSetting(key, null);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public double getDoubleSetting(String key, double defaultValue) {
        String val = getSetting(key, null);
        if (val != null) {
            try {
                return Double.parseDouble(val.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean updateSetting(String key, String value) {
        String sql = "INSERT INTO library_settings (setting_key, setting_value, updated_at) " +
                     "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating setting: {}", key, e);
            return false;
        }
    }
}
