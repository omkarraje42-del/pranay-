# Architecture Design Document

## 1. System Overview

The **Enterprise Library Management System (J2EE)** is a production-grade, multi-tier web application built on the Jakarta EE 10 specification, using Java 17, Apache Tomcat 10.1, and PostgreSQL 16. It is designed to handle high-concurrency circulation transactions with strict ACID guarantees, granular role-based access control (RBAC), and modern responsive presentation.

```
+-------------------------------------------------------------------------------+
|                             Presentation Tier                                 |
|   HTML5, CSS3, JavaScript ES6+, Bootstrap 5.3, Bootstrap Icons, Chart.js      |
+-------------------------------------------------------------------------------+
                                      |
                            HTTP/REST (Fetch API)
                                      v
+-------------------------------------------------------------------------------+
|                       Security & Filter Pipeline                              |
|   SecurityHeadersFilter -> AuthenticationFilter -> AuthorizationFilter        |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                           Controller / API Tier                               |
|   BaseServlet, AuthServlet, BookServlet, MemberServlet, LoanServlet, etc.     |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                              Business Service Tier                            |
|   AuthService, BookService, MemberService, LoanService, ReservationService... |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                        Data Access Object (DAO) Tier                          |
|   HikariCP Connection Pool + JDBC PreparedStatements + Row-Level Locking      |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                          Relational Database Tier                             |
|       PostgreSQL 16 (14 Tables, Foreign Keys, Indexes, Constraints)           |
+-------------------------------------------------------------------------------+
```

---

## 2. Layered Architecture Principles

### 2.1 Presentation Tier
- **Separation of Concerns:** Static presentation (`HTML`, `CSS`, `JS`) is separated from the Java enterprise backend.
- **Dynamic Interaction:** Asynchronous Fetch API client with automatic session management (`credentials: 'same-origin'`).
- **Responsive Layout:** Dark navy sidebar navigation (`#1a2238`), top breadcrumb navigation, interactive modals, floating toasts, responsive data tables, and Chart.js analytics.
- **Client-Side Authorization:** Menus, buttons, and views conditionally render based on server-verified session state (`data-role="ADMIN"`, `data-role="STAFF"`).

### 2.2 Security & Filter Pipeline
1. **`SecurityHeadersFilter`:**
   - Enforces `X-Content-Type-Options: nosniff`
   - Enforces `X-Frame-Options: DENY`
   - Enforces `X-XSS-Protection: 1; mode=block`
   - Enforces `Referrer-Policy: strict-origin-when-cross-origin`
   - Enforces strict `Content-Security-Policy`
2. **`AuthenticationFilter`:**
   - Intercepts requests to `/api/*` (excluding public paths `/api/auth/login`, `/api/auth/logout`, and public book browse `GET /api/books`).
   - Validates active `HttpSession` and retrieves the authenticated `User` principal.
   - Rejects unauthenticated requests with HTTP 401 Unauthorized (`ApiResponse.error(...)`).
3. **`AuthorizationFilter`:**
   - Enforces Role-Based Access Control (RBAC) across three distinct enterprise roles:
     - `ADMIN`: Full administrative access (users, staff, audit logs, policies, circulation, catalog).
     - `LIBRARIAN`: Circulation operations (issue, return, reserve, waive/collect fines, catalog management).
     - `MEMBER`: Read-only catalogue access, self-service borrowing history, and personal reservation placement.
   - Rejects unauthorized privilege escalation attempts with HTTP 403 Forbidden.

### 2.3 Controller / Servlet Tier
- Built on `jakarta.servlet.http.HttpServlet`.
- `BaseServlet` provides centralized utilities:
  - JSON deserialization via Google Gson (with `LocalDate` and `OffsetDateTime` type adapters).
  - Standardized JSON responses via `ApiResponse<T>` and `PaginatedResponse<T>`.
  - Global exception handling mapping business exceptions to standard HTTP error codes:
    - `IllegalArgumentException` / `ValidationException` -> HTTP 400 Bad Request
    - `AuthenticationException` -> HTTP 401 Unauthorized
    - `AuthorizationException` -> HTTP 403 Forbidden
    - `ResourceNotFoundException` -> HTTP 404 Not Found
    - `ConflictException` -> HTTP 409 Conflict
    - Unhandled exceptions -> HTTP 500 Internal Server Error (without leaking stack traces).

### 2.4 Service Tier
- Encapsulates pure business logic and transactional orchestration:
  - **`LoanService`:** Coordinates atomic book checkout, accession number resolution, fine calculation, and automatic assignment to waiting reservations.
  - **`AuthService`:** Password verification using BCrypt (cost factor 12) and system administrator bootstrapping.
  - **`BookService`:** Enforces ISBN validation, duplicate detection, and category/author relational mapping.
  - **`ReservationService`:** Manages priority FIFO queueing and auto-expiration deadlines.
  - **`FineService`:** Financial tracking with `BigDecimal` arithmetic and receipt generation.

### 2.5 DAO Tier & Database Concurrency
- Uses high-performance **HikariCP** connection pooling.
- **Zero SQL Injection:** 100% of SQL statements execute through parameterized `PreparedStatement`.
- **Pessimistic Row-Level Locking:**
  - When issuing a book copy, the DAO executes:
    ```sql
    SELECT * FROM book_copies WHERE id = ? FOR UPDATE;
    ```
  - When processing returns with potential outer joins, the DAO executes:
    ```sql
    SELECT ... FROM loans l ... WHERE l.id = ? FOR UPDATE OF l;
    ```
  - When picking the highest-priority reservation:
    ```sql
    SELECT ... FROM reservations r WHERE r.book_id = ? AND r.status = 'WAITING'
    ORDER BY r.priority ASC, r.reservation_date ASC LIMIT 1 FOR UPDATE OF r;
    ```
  - This completely prevents double-borrowing race conditions under concurrent requests.

---

## 3. Session Management

- Standard Jakarta EE servlet sessions backed by `HttpSession`.
- Cookies configured in `web.xml`:
  - `HttpOnly: true` (prevents XSS cookie theft)
  - `SameSite: Lax`
  - Configurable `Secure: true` for TLS deployments
  - 30-minute idle session timeout.
