-- ============================================================================
-- LIBRARY MANAGEMENT SYSTEM - DATABASE SCHEMA (PostgreSQL)
-- ============================================================================

-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS fines CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS loans CASCADE;
DROP TABLE IF EXISTS book_copies CASCADE;
DROP TABLE IF EXISTS book_authors CASCADE;
DROP TABLE IF EXISTS books CASCADE;
DROP TABLE IF EXISTS publishers CASCADE;
DROP TABLE IF EXISTS authors CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS members CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS library_settings CASCADE;

-- 1. Roles table
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Users table (for authentication and staff accounts)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Members table
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    membership_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(30),
    address TEXT,
    membership_start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    membership_expiry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Categories table
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Authors table
CREATE TABLE authors (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    biography TEXT,
    nationality VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Publishers table
CREATE TABLE publishers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) UNIQUE NOT NULL,
    contact_email VARCHAR(100),
    phone VARCHAR(50),
    website VARCHAR(255),
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Books table
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) UNIQUE,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    category_id INT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    publisher_id INT REFERENCES publishers(id) ON DELETE SET NULL,
    publication_year INT CHECK (publication_year >= 1000 AND publication_year <= 2100),
    edition VARCHAR(50),
    language VARCHAR(50) DEFAULT 'English',
    description TEXT,
    shelf_location VARCHAR(100),
    cover_image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Book-Author bridge table (Many-to-Many)
CREATE TABLE book_authors (
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    author_id INT NOT NULL REFERENCES authors(id) ON DELETE RESTRICT,
    PRIMARY KEY (book_id, author_id)
);

-- 9. Physical Book Copies table
CREATE TABLE book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    accession_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'BORROWED', 'RESERVED', 'DAMAGED', 'LOST', 'MAINTENANCE')),
    condition VARCHAR(50) DEFAULT 'Good',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Loans (Borrowing transactions) table
CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    book_copy_id BIGINT NOT NULL REFERENCES book_copies(id) ON DELETE RESTRICT,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    issued_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    returned_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RETURNED', 'OVERDUE', 'LOST')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_loan_dates CHECK (return_date IS NULL OR return_date >= issue_date)
);

-- 11. Reservations table
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    reservation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(25) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'READY_FOR_PICKUP', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    priority INT NOT NULL DEFAULT 1,
    notification_sent_at TIMESTAMPTZ,
    pickup_deadline DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Fines table
CREATE TABLE fines (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans(id) ON DELETE RESTRICT,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    reason VARCHAR(255) NOT NULL DEFAULT 'Overdue return',
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID', 'WAIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMPTZ,
    waived_at TIMESTAMPTZ,
    waived_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    waiver_reason TEXT,
    payment_method VARCHAR(50),
    receipt_number VARCHAR(100)
);

-- 13. Library Settings table
CREATE TABLE library_settings (
    id SERIAL PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. Audit Logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_category ON books(category_id);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_publisher ON books(publisher_id);
CREATE INDEX idx_books_is_active ON books(is_active);

CREATE INDEX idx_book_copies_book ON book_copies(book_id);
CREATE INDEX idx_book_copies_status ON book_copies(status);
CREATE INDEX idx_book_copies_accession ON book_copies(accession_number);

CREATE INDEX idx_members_membership_number ON members(membership_number);
CREATE INDEX idx_members_status ON members(status);
CREATE INDEX idx_members_email ON members(email);

CREATE INDEX idx_loans_member ON loans(member_id);
CREATE INDEX idx_loans_copy ON loans(book_copy_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_due_date ON loans(due_date);

CREATE INDEX idx_reservations_book ON reservations(book_id);
CREATE INDEX idx_reservations_member ON reservations(member_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_date ON reservations(reservation_date);

CREATE INDEX idx_fines_member ON fines(member_id);
CREATE INDEX idx_fines_loan ON fines(loan_id);
CREATE INDEX idx_fines_status ON fines(status);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
