package com.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Enterprise Database Configuration & HikariCP Connection Pool Manager.
 * Loads settings from environment variables or classpath db.properties.
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {
        // Utility class
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    initDataSource();
                }
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static void initDataSource() {
        try {
            Properties props = new Properties();
            try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                }
            } catch (Exception e) {
                logger.debug("No db.properties file found on classpath, relying on environment defaults");
            }

            String host = getEnvOrProp("DB_HOST", props, "localhost");
            String port = getEnvOrProp("DB_PORT", props, "5432");
            String dbName = getEnvOrProp("DB_NAME", props, "library_db");
            String user = getEnvOrProp("DB_USERNAME", props, "library_app");
            String pass = getEnvOrProp("DB_PASSWORD", props, "library_secret");

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(pass);
            config.setDriverClassName("org.postgresql.Driver");

            // Pool performance tuning
            config.setMaximumPoolSize(Integer.parseInt(getEnvOrProp("DB_POOL_MAX_SIZE", props, "10")));
            config.setMinimumIdle(Integer.parseInt(getEnvOrProp("DB_POOL_MIN_IDLE", props, "2")));
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);
            config.setMaxLifetime(1800000);
            config.setPoolName("LibraryHikariPool");

            // Health check
            config.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized successfully for URL: {}", jdbcUrl);

        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database pool initialization failed: " + e.getMessage(), e);
        }
    }

    private static String getEnvOrProp(String key, Properties props, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }
        return defaultValue;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed.");
        }
    }
}
