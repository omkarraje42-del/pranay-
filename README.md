# Enterprise Library Management System (J2EE & PostgreSQL)

[![Java CI with Maven](https://github.com/developer/library-management-system-j2ee/actions/workflows/maven.yml/badge.svg)](https://github.com/developer/library-management-system-j2ee/actions/workflows/maven.yml)
[![Java 17](https://img.shields.io/badge/Java-17%20LTS-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Jakarta EE 10](https://img.shields.io/badge/Jakarta%20EE-10-orange.svg)](https://jakarta.ee/)
[![Apache Tomcat](https://img.shields.io/badge/Tomcat-10.1-yellow.svg)](https://tomcat.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Bootstrap 5](https://img.shields.io/badge/Bootstrap-5.3-purple.svg)](https://getbootstrap.com/)

A complete, production-grade enterprise **Library Management System** built strictly using Java Enterprise Edition (**J2EE / Jakarta EE 10**), **Apache Tomcat 10.1**, and a **PostgreSQL 16** relational database.

---

## Architecture Overview

The system strictly follows a clean 5-layer enterprise architecture:

```
User Interface (HTML5, CSS3, JavaScript ES6+, Bootstrap 5.3, Chart.js)
  │
  ▼  HTTP REST (Fetch API with Cookie Authentication)
Security Pipeline (SecurityHeadersFilter ➔ AuthenticationFilter ➔ AuthorizationFilter)
  │
  ▼
Controller Layer (Jakarta Servlets 6.0: BaseServlet, BookServlet, LoanServlet, etc.)
  │
  ▼
Service Layer (AuthService, BookService, LoanService, FineService, etc.)
  │
  ▼
DAO Layer (JDBC PreparedStatements + HikariCP Connection Pool + Row Locking)
  │
  ▼
PostgreSQL 16 Relational Database (14 Tables, Constraints, Indexes, Foreign Keys)
```

---

## Key Features

1. **Enterprise Role-Based Access Control (RBAC):**
   - **`ADMIN`:** Complete dashboard analytics, staff/librarian user management, system audit logs, policy configuration.
   - **`LIBRARIAN`:** Book catalog management, physical copy barcode/accession tracking, book issue & return, fine collection & waiving, reservation handling.
   - **`MEMBER`:** Book catalog search & availability checks, borrowing history, active loans, reservation placement, profile & password change.
2. **ACID Circulation Engine:**
   - **Pessimistic Row-Level Locking:** Prevents double-borrowing race conditions under high concurrent checkout traffic (`SELECT ... FOR UPDATE`).
   - Physical copy tracking via unique accession numbers (`ACC-0001`, `ACC-0002`).
   - Dynamic due date calculation and configurable borrowing limits.
3. **Automated Overdue & Fine Management:**
   - Accurate daily overdue penalty calculations with `BigDecimal` currency precision.
   - Cash, Card, and UPI receipt tracking with unique receipt numbers.
   - Administrative fine waiving with audit trail logging.
4. **Priority-Based Reservation Queue:**
   - FIFO hold queue for borrowed titles.
   - Automated copy reassignment upon book return to the highest-priority waiting patron.
5. **Modern Single-Page-Feel Frontend:**
   - Dark navy sidebar navigation, metric cards, responsive tables, real-time debounced search, modal dialogs, and Chart.js 7-day circulation trends.
6. **Robust Security:**
   - BCrypt password hashing (cost factor 12) — zero plaintext storage.
   - `HttpOnly`, `SameSite=Lax` session management.
   - Complete protection against SQL injection (100% `PreparedStatement`) and XSS.

---

## Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | HTML5, CSS3, JavaScript ES6+, Bootstrap 5.3.3, Bootstrap Icons 1.11.3, Chart.js |
| **Backend Framework** | Jakarta EE 10 (Jakarta Servlet 6.0, Jakarta RESTful conventions, Jakarta Filters) |
| **Application Server** | Apache Tomcat 10.1 (or embedded standalone runner `com.library.Main`) |
| **Data Access** | JDBC, `PreparedStatement`, HikariCP 5.1.0 Connection Pool |
| **Database** | PostgreSQL 16.2 |
| **JSON Serialization**| Google Gson 2.10.1 (with custom Date & Time adapters) |
| **Security** | jBCrypt 0.4, Jakarta Servlet Security Filters |
| **Testing** | JUnit 5 (Jupiter), AssertJ Core, Mockito |
| **Build & DevOps** | Apache Maven 3.9+, Docker (Multi-stage), Docker Compose, GitHub Actions |

---

## Default Credentials

The system includes pre-seeded accounts for testing and verification:

| Role | Username | Password | Email |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `Admin@123` | `admin@library.edu` |
| **Librarian** | `librarian` | `Librarian@123` | `librarian@library.edu` |
| **Member** | `johndoe` | `Member@123` | `john.doe@example.com` |
| **Member** | `janesmith` | `Member@123` | `jane.smith@example.com` |

---

## Quick Start with Docker Compose

The fastest way to spin up the complete application stack (PostgreSQL + Tomcat 10.1):

```bash
# 1. Clone repository
git clone https://github.com/developer/library-management-system-j2ee.git
cd library-management-system-j2ee

# 2. Start PostgreSQL and J2EE application
docker-compose up -d --build

# 3. Access the web interface
# Navigate to: http://localhost:8080/login.html
```

---

## Local Setup Without Docker

### Prerequisites
- **Java Development Kit (JDK):** OpenJDK 17 or higher
- **Build Tool:** Apache Maven 3.8+
- **Database:** PostgreSQL 16+
- **Application Server (Optional):** Apache Tomcat 10.1 (embedded runner included)

### Step 1: Database Setup
1. Start your local PostgreSQL server.
2. Create the database and user:
```sql
CREATE DATABASE library_db;
CREATE USER library_app WITH ENCRYPTED PASSWORD 'library_secret';
GRANT ALL PRIVILEGES ON DATABASE library_db TO library_app;
```
3. Run the schema and seed scripts:
```bash
psql -U library_app -d library_db -h localhost -f database/schema.sql
psql -U library_app -d library_db -h localhost -f database/seed.sql
```

### Step 2: Configure Database Credentials
Edit `src/main/resources/db.properties` (or set environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`):
```properties
db.url=jdbc:postgresql://localhost:5432/library_db
db.username=library_app
db.password=library_secret
db.pool.maximumPoolSize=15
```

### Step 3: Run Automated Test Suite
```bash
mvn clean test
```
All 41 unit and DAO integration tests will execute against your PostgreSQL instance.

### Step 4: Run Application

#### Option A: Standalone Embedded Tomcat Runner (Recommended for Development)
```bash
mvn compile exec:java "-Dexec.mainClass=com.library.Main"
```
The server starts instantly on `http://localhost:8080`.

#### Option B: Standalone Apache Tomcat 10.1
1. Build the production WAR archive:
```bash
mvn clean package -DskipTests
```
2. Copy `target/library-management-system-j2ee.war` into Tomcat's `webapps/ROOT.war` directory.
3. Start Tomcat:
```bash
# Windows
bin\catalina.bat run

# Linux / macOS
bin/catalina.sh run
```
4. Open your browser at `http://localhost:8080/`.

---

## REST API Summary

| Category | Endpoint | Methods | Description |
| :--- | :--- | :--- | :--- |
| **Auth** | `/api/auth/login` | `POST` | Authenticate & create session cookie |
| **Auth** | `/api/auth/me` | `GET` | Get logged-in user details |
| **Auth** | `/api/auth/logout` | `POST` | Invalidate session |
| **Books** | `/api/books` | `GET`, `POST` | List/search books; register new title |
| **Books** | `/api/books/{id}` | `GET`, `PUT`, `DELETE` | Single book CRUD |
| **Copies** | `/api/books/{id}/copies` | `GET`, `POST` | Physical copy inventory & accessioning |
| **Circulation**| `/api/loans` | `GET` | List borrowing transactions |
| **Circulation**| `/api/loans/issue` | `POST` | ACID book checkout with row locking |
| **Circulation**| `/api/loans/{id}/return` | `POST` | Book return with fine assessment |
| **Circulation**| `/api/loans/overdue` | `GET` | Active overdue loans |
| **Circulation**| `/api/loans/my` | `GET` | Current member loans |
| **Fines** | `/api/fines` | `GET` | Outstanding and paid fines |
| **Fines** | `/api/fines/{id}/pay` | `POST` | Record payment & generate receipt |
| **Fines** | `/api/fines/{id}/waive` | `POST` | Administrative fine waiver |
| **Holds** | `/api/reservations` | `GET`, `POST` | Place priority reservation |
| **Holds** | `/api/reservations/{id}` | `DELETE` | Cancel reservation |
| **Patrons** | `/api/members` | `GET`, `POST`, `PUT` | Patron directory & registration |
| **Staff** | `/api/users` | `GET`, `POST`, `PUT` | Administrator user management |
| **Dashboard** | `/api/dashboard/stats` | `GET` | Circulation KPIs & real-time counts |

For detailed request and response payload schemas, refer to [docs/api-documentation.md](docs/api-documentation.md).

---

## Automated Test Coverage

The test suite consists of 41 unit and DAO integration tests:
- **`ValidationUtilTest` (20 tests):** Email format, ISBN-10/13 formats, input sanitization, non-empty validation.
- **`PasswordUtilTest` (4 tests):** BCrypt prefix formatting, salt generation uniqueness, password match verification, null handling.
- **`LoanCalculationTest` (5 tests):** Due date calculations, overdue day differentials, `BigDecimal` fine computation.
- **`ApiResponseTest` (3 tests):** JSON serialization contract, pagination calculation.
- **`BookDAOTest` (5 tests):** PostgreSQL queries, category/author joins, search filtering, copy counts.
- **`MemberDAOTest` (4 tests):** Membership code resolution, active loan and fine aggregates.

---

## Documentation

- [Architecture Design & Filter Pipeline](docs/architecture.md)
- [Database Schema & ER Diagrams](docs/database-design.md)
- [Complete REST API Specification](docs/api-documentation.md)
- [Database Setup & Seed Guide](database/README.md)

---

## License

This project is licensed under the MIT License.
