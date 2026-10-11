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

            String rawUrl = getEnvOrProp("DB_URL", props, null);
            String user = getEnvOrProp("DB_USERNAME", props, null);
            String pass = getEnvOrProp("DB_PASSWORD", props, null);
            String sslMode = getEnvOrProp("DB_SSLMODE", props, null);

            String jdbcUrl;
            if (rawUrl != null && !rawUrl.trim().isEmpty()) {
                rawUrl = rawUrl.trim();

                // Fix accidental copy-paste duplication (e.g. "jdbc:postgresql://aws-0-ap-south-jdbc:postgresql://...")
                int lastJdbcIndex = rawUrl.lastIndexOf("jdbc:postgresql://");
                if (lastJdbcIndex > 0) {
                    rawUrl = rawUrl.substring(lastJdbcIndex);
                } else {
                    int lastPgIndex = rawUrl.lastIndexOf("postgresql://");
                    if (lastPgIndex > 0) {
                        rawUrl = rawUrl.substring(lastPgIndex);
                    }
                }

                // If user embedded query params like ?user=...&password=... extract them so special chars don't break JDBC URL
                if (rawUrl.contains("user=") || rawUrl.contains("password=")) {
                    int qIdx = rawUrl.indexOf('?');
                    if (qIdx != -1) {
                        String query = rawUrl.substring(qIdx + 1);
                        String base = rawUrl.substring(0, qIdx);
                        StringBuilder newQuery = new StringBuilder();
                        for (String param : query.split("&")) {
                            if (param.startsWith("user=")) {
                                if (user == null || user.isEmpty()) {
                                    user = param.substring(5);
                                }
                            } else if (param.startsWith("password=")) {
                                if (pass == null || pass.isEmpty()) {
                                    pass = param.substring(9);
                                }
                            } else {
                                if (newQuery.length() > 0) newQuery.append("&");
                                newQuery.append(param);
                            }
                        }
                        rawUrl = base + (newQuery.length() > 0 ? "?" + newQuery.toString() : "");
                    }
                }

                // If URI format: postgresql://user:pass@host:port/dbname or postgres://user:pass@...
                if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                    try {
                        String cleanUri = rawUrl.startsWith("postgres://") 
                                ? "http://" + rawUrl.substring("postgres://".length()) 
                                : "http://" + rawUrl.substring("postgresql://".length());
                        java.net.URI uri = java.net.URI.create(cleanUri);
                        if (uri.getUserInfo() != null) {
                            String[] userParts = uri.getUserInfo().split(":", 2);
                            if (user == null || user.isEmpty()) {
                                user = userParts[0];
                            }
                            if (userParts.length > 1 && (pass == null || pass.isEmpty())) {
                                pass = java.net.URLDecoder.decode(userParts[1], java.nio.charset.StandardCharsets.UTF_8);
                            }
                        }
                        int portNum = uri.getPort() > 0 ? uri.getPort() : 6543;
                        String path = uri.getPath();
                        if (path != null && path.startsWith("/")) path = path.substring(1);
                        rawUrl = "jdbc:postgresql://" + uri.getHost() + ":" + portNum + "/" + (path != null && !path.isEmpty() ? path : "postgres");
                        if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                            rawUrl += "?" + uri.getQuery();
                        }
                    } catch (Exception e) {
                        logger.warn("Could not parse DB_URL as URI, using as-is with jdbc: prefix: {}", e.getMessage());
                        if (rawUrl.startsWith("postgres://")) {
                            rawUrl = "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
                        } else if (!rawUrl.startsWith("jdbc:")) {
                            rawUrl = "jdbc:" + rawUrl;
                        }
                    }
                } else if (!rawUrl.startsWith("jdbc:")) {
                    rawUrl = "jdbc:" + rawUrl;
                }

                // If connecting to Supabase / cloud database, ensure sslmode=require
                if (!rawUrl.contains("sslmode=")) {
                    String ssl = (sslMode != null && !sslMode.isEmpty()) ? sslMode : (rawUrl.contains("supabase.") ? "require" : null);
                    if (ssl != null) {
                        rawUrl += (rawUrl.contains("?") ? "&" : "?") + "sslmode=" + ssl;
                    }
                }
                jdbcUrl = rawUrl;
            } else {
                String host = getEnvOrProp("DB_HOST", props, "localhost");
                String port = getEnvOrProp("DB_PORT", props, "5432");
                String dbName = getEnvOrProp("DB_NAME", props, "library_db");
                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + 
                        (sslMode != null && !sslMode.isEmpty() ? "?sslmode=" + sslMode : "");
            }

            // Defaults if not provided
            if (user == null || user.isEmpty()) {
                user = jdbcUrl.contains("supabase.") ? "postgres" : "library_app";
            }
            if (pass == null || pass.isEmpty()) {
                pass = "library_secret";
            }

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
