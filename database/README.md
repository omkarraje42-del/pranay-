# PostgreSQL Database Setup & Documentation

This directory contains the database definition and seed data scripts for the **Library Management System (J2EE)**.

## Database Overview

- **RDBMS**: PostgreSQL 16+
- **Database Name**: `library_db`
- **Application User**: `library_app`
- **Default Port**: `5432`

## Schema Files

| File | Description |
|---|---|
| [`schema.sql`](file:///c:/Users/User/Documents/antigravity/zealous-oppenheimer/database/schema.sql) | DDL script defining all 14 relational tables, foreign keys, check constraints, unique constraints, and performance indexes. |
| [`seed.sql`](file:///c:/Users/User/Documents/antigravity/zealous-oppenheimer/database/seed.sql) | Initial dataset containing standard roles, demo accounts, categories, authors, publishers, books, physical copies, sample members, historical loans, fines, reservations, and configuration policies. |

## Entity Relationship Overview

1. **`roles`**: System roles (`ADMIN`, `LIBRARIAN`, `MEMBER`).
2. **`users`**: Authentication credentials and staff metadata. Passwords hashed using BCrypt.
3. **`members`**: Library patrons with unique membership numbers (`LIB-2024-xxx`), contact information, and membership statuses (`ACTIVE`, `INACTIVE`, `SUSPENDED`, `EXPIRED`).
4. **`categories`**: Book classifications (e.g., Computer Science, Mathematics).
5. **`authors`**: Book authors with biography and nationality.
6. **`publishers`**: Publishing houses with contact information.
7. **`books`**: Bibliographic book titles, ISBNs, editions, languages, shelf locations, and cover image URLs.
8. **`book_authors`**: Many-to-many relationship table between `books` and `authors`.
9. **`book_copies`**: Physical inventory items, each with a unique barcode/accession number (`ACC-0001`) and copy status (`AVAILABLE`, `BORROWED`, `RESERVED`, `DAMAGED`, `LOST`).
10. **`loans`**: Borrowing transaction records linking a member with an exact physical book copy, issue date, due date, return date, and processing staff.
11. **`reservations`**: Queue records for members requesting books when no copies are currently available (`WAITING`, `READY_FOR_PICKUP`, `FULFILLED`, `CANCELLED`).
12. **`fines`**: Overdue fines and replacement charges with financial precision (`NUMERIC(10,2)`), payment tracking (`UNPAID`, `PAID`, `WAIVED`), and waiver audit notes.
13. **`library_settings`**: Configurable system policies such as loan period, max loans per patron, fine per day, and thresholds.
14. **`audit_logs`**: System audit trail capturing staff actions on critical entities.

## Manual Database Setup Commands

```bash
# 1. Create role and database in PostgreSQL
psql -U postgres -c "CREATE USER library_app WITH PASSWORD 'library_secret';"
psql -U postgres -c "CREATE DATABASE library_db OWNER library_app;"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE library_db TO library_app;"

# 2. Apply schema
psql -U library_app -d library_db -f database/schema.sql

# 3. Apply seed data
psql -U library_app -d library_db -f database/seed.sql
```

## Concurrency and Safety Features

- **Pessimistic Row-Level Locking**: The checkout/issue service uses `SELECT ... FOR UPDATE` on `book_copies` within an ACID database transaction to strictly prevent race conditions where two librarians attempt to issue the same physical copy at the same millisecond.
- **Referential Integrity**: Books cannot be deleted if active copies or past borrowing history exists; categories cannot be deleted if books are assigned.
- **Financial Precision**: All fines use `NUMERIC(10, 2)` at the database level and `java.math.BigDecimal` in the Java enterprise backend.
