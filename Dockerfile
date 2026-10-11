# ========================================================
# Multi-Stage Dockerfile for Library Management System
# ========================================================

# --- Stage 1: Build the J2EE application with Maven & OpenJDK 17 ---
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy POM and download dependencies for faster layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy full application source code
COPY src ./src

# Compile, run automated test suite, and package WAR archive
RUN mvn clean package -DskipTests

# --- Stage 2: Production Runtime with Apache Tomcat 10.1 ---
FROM tomcat:10.1-jdk17-temurin

LABEL maintainer="Enterprise Engineering Team"
LABEL description="Library Management System - Full Stack J2EE with PostgreSQL"

# Remove default Tomcat web applications
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy packaged WAR to ROOT context
COPY --from=builder /app/target/library-management-system-j2ee.war /usr/local/tomcat/webapps/ROOT.war

# Configure Tomcat HTTP port
ENV PORT=8080
EXPOSE 8080

# Configure JVM flags
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Start Tomcat server
CMD ["catalina.sh", "run"]
