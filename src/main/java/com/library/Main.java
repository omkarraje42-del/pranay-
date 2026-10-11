package com.library;

import com.library.config.DatabaseConfig;
import com.library.service.AuthService;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Embedded Application Server Launcher for Library Management System.
 * Enables zero-setup local running via `mvn compile exec:java`.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort == null || envPort.trim().isEmpty()) {
            envPort = System.getenv("SERVER_PORT");
        }
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        logger.info("Initializing Library Management System on port {}...", port);

        try {
            // 1. Verify database connectivity and bootstrap admin
            DatabaseConfig.getDataSource();
            AuthService authService = new AuthService();
            authService.bootstrapInitialAdmin("admin", "admin@library.edu", "Admin@123");

            // 2. Configure Embedded Tomcat 10.1
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(port);
            tomcat.getConnector(); // Initialize default HTTP connector

            // Base directory for Tomcat work files
            String baseDir = new File("target/tomcat-work").getAbsolutePath();
            new File(baseDir).mkdirs();
            tomcat.setBaseDir(baseDir);

            // Webapp directory
            File webappDir = new File("src/main/webapp");
            if (!webappDir.exists()) {
                webappDir = new File("webapp");
            }

            Context context = tomcat.addWebapp("", webappDir.getAbsolutePath());
            context.setAllowCasualMultipartParsing(true);

            // Ensure compiled classes in target/classes are visible to Tomcat webapp classloader
            File additionWebInfClasses = new File("target/classes");
            if (additionWebInfClasses.exists()) {
                org.apache.catalina.WebResourceRoot resources = new org.apache.catalina.webresources.StandardRoot(context);
                resources.addPreResources(new org.apache.catalina.webresources.DirResourceSet(
                        resources, "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
                context.setResources(resources);
            }

            logger.info("Configured webapp context at '{}' pointing to '{}'", "", webappDir.getAbsolutePath());

            // 3. Start server
            tomcat.start();
            logger.info("=================================================================");
            logger.info("  LIBRARY MANAGEMENT SYSTEM IS READY!");
            logger.info("  Web UI & API: http://localhost:{}", port);
            logger.info("  Default Admin Login: admin / Admin@123");
            logger.info("=================================================================");

            // Keep alive
            tomcat.getServer().await();

        } catch (LifecycleException e) {
            logger.error("Tomcat lifecycle failure during startup", e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Fatal startup error", e);
            System.exit(1);
        }
    }
}
