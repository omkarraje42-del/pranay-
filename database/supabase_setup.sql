-- ============================================================================
-- LIBRARY MANAGEMENT SYSTEM - COMPLETE SUPABASE SETUP SCRIPT
-- (Combines Schema Creation + Performance Indexes + Seed Data in One Single Run)
-- ============================================================================

-- Explicitly ensure public schema is targeted in Supabase
SET search_path = public;

-- Drop existing tables in reverse dependency order if any exist
DROP TABLE IF EXISTS public.audit_logs CASCADE;
DROP TABLE IF EXISTS public.fines CASCADE;
DROP TABLE IF EXISTS public.reservations CASCADE;
DROP TABLE IF EXISTS public.loans CASCADE;
DROP TABLE IF EXISTS public.book_copies CASCADE;
DROP TABLE IF EXISTS public.book_authors CASCADE;
DROP TABLE IF EXISTS public.books CASCADE;
DROP TABLE IF EXISTS public.publishers CASCADE;
DROP TABLE IF EXISTS public.authors CASCADE;
DROP TABLE IF EXISTS public.categories CASCADE;
DROP TABLE IF EXISTS public.members CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;
DROP TABLE IF EXISTS public.roles CASCADE;
DROP TABLE IF EXISTS public.library_settings CASCADE;

-- ----------------------------------------------------------------------------
-- 1. ROLES TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE public.roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 2. USERS TABLE (System Staff & Member Authentication)
-- ----------------------------------------------------------------------------
CREATE TABLE public.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL REFERENCES public.roles(id) ON DELETE RESTRICT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 3. MEMBERS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE public.members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES public.users(id) ON DELETE SET NULL,
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

-- ----------------------------------------------------------------------------
-- 4. CATEGORIES TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE public.categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 5. AUTHORS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE public.authors (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    biography TEXT,
    nationality VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 6. PUBLISHERS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE public.publishers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) UNIQUE NOT NULL,
    contact_email VARCHAR(100),
    phone VARCHAR(30),
    website VARCHAR(255),
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 7. BOOKS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE public.books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) UNIQUE,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    category_id INT NOT NULL REFERENCES public.categories(id) ON DELETE RESTRICT,
    publisher_id INT REFERENCES public.publishers(id) ON DELETE SET NULL,
    publication_year INT CHECK (publication_year BETWEEN 1000 AND 2100),
    edition VARCHAR(50),
    language VARCHAR(50) DEFAULT 'English',
    description TEXT,
    shelf_location VARCHAR(50),
    cover_image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 8. BOOK_AUTHORS TABLE (Many-to-Many Relationship)
-- ----------------------------------------------------------------------------
CREATE TABLE public.book_authors (
    book_id BIGINT NOT NULL REFERENCES public.books(id) ON DELETE CASCADE,
    author_id INT NOT NULL REFERENCES public.authors(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

-- ----------------------------------------------------------------------------
-- 9. BOOK_COPIES TABLE (Physical Copies with Barcodes / Accession Numbers)
-- ----------------------------------------------------------------------------
CREATE TABLE public.book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES public.books(id) ON DELETE CASCADE,
    accession_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'BORROWED', 'RESERVED', 'LOST', 'DAMAGED', 'MAINTENANCE')),
    condition VARCHAR(50) DEFAULT 'Good',
    acquisition_date DATE DEFAULT CURRENT_DATE,
    price NUMERIC(10,2) CHECK (price >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 10. LOANS TABLE (Circulation Borrowing Transactions)
-- ----------------------------------------------------------------------------
CREATE TABLE public.loans (
    id BIGSERIAL PRIMARY KEY,
    book_copy_id BIGINT NOT NULL REFERENCES public.book_copies(id) ON DELETE RESTRICT,
    member_id BIGINT NOT NULL REFERENCES public.members(id) ON DELETE RESTRICT,
    issued_by BIGINT REFERENCES public.users(id) ON DELETE SET NULL,
    returned_by BIGINT REFERENCES public.users(id) ON DELETE SET NULL,
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RETURNED', 'OVERDUE', 'LOST')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_due_date_after_issue CHECK (due_date >= issue_date)
);

-- ----------------------------------------------------------------------------
-- 11. FINES TABLE (Accrued Penalties with High Precision)
-- ----------------------------------------------------------------------------
CREATE TABLE public.fines (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT UNIQUE NOT NULL REFERENCES public.loans(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES public.members(id) ON DELETE CASCADE,
    amount NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    paid_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (paid_amount >= 0),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID', 'PARTIALLY_PAID', 'WAIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMPTZ,
    payment_method VARCHAR(50),
    receipt_number VARCHAR(100)
);

-- ----------------------------------------------------------------------------
-- 12. RESERVATIONS TABLE (Priority Hold Queue)
-- ----------------------------------------------------------------------------
CREATE TABLE public.reservations (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES public.books(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES public.members(id) ON DELETE CASCADE,
    reservation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'READY_FOR_PICKUP', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    priority INT NOT NULL DEFAULT 1,
    notification_sent_at TIMESTAMPTZ,
    pickup_deadline DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_active_member_book_reservation UNIQUE (book_id, member_id, status)
);

-- ----------------------------------------------------------------------------
-- 13. LIBRARY SETTINGS TABLE (Configurable System Parameters)
-- ----------------------------------------------------------------------------
CREATE TABLE public.library_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 14. AUDIT LOGS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE public.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES public.users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- PERFORMANCE INDEXES
-- ----------------------------------------------------------------------------
CREATE INDEX idx_books_category ON public.books(category_id);
CREATE INDEX idx_books_title ON public.books(LOWER(title));
CREATE INDEX idx_books_isbn ON public.books(isbn);
CREATE INDEX idx_book_copies_book ON public.book_copies(book_id);
CREATE INDEX idx_book_copies_status ON public.book_copies(status);
CREATE INDEX idx_book_copies_accession ON public.book_copies(accession_number);
CREATE INDEX idx_loans_member_status ON public.loans(member_id, status);
CREATE INDEX idx_loans_due_date ON public.loans(due_date) WHERE status = 'ACTIVE';
CREATE INDEX idx_fines_member_status ON public.fines(member_id, status);
CREATE INDEX idx_reservations_book_status ON public.reservations(book_id, status);

-- ============================================================================
-- SEED DATA INSERTION
-- ============================================================================

-- 1. Insert Roles
INSERT INTO public.roles (id, name, description) VALUES
(1, 'ADMIN', 'System Administrator with full access to all modules and configurations'),
(2, 'LIBRARIAN', 'Library Staff with permissions to manage books, members, issues and returns'),
(3, 'MEMBER', 'Library Member with access to catalog search, loan history and reservations')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.roles_id_seq', (SELECT MAX(id) FROM public.roles));

-- 2. Insert Default Users (Salted BCrypt Hashes)
-- Passwords:
-- admin: Admin@123
-- librarian: Librarian@123
-- johndoe & janesmith: Member@123
INSERT INTO public.users (id, username, email, password_hash, role_id, first_name, last_name, phone, is_active) VALUES
(1, 'admin', 'admin@library.edu', '$2a$12$eBDj1RjICB2yQ/VqTTZJP.c/MWMqquvTe9m3TzuZesczqB2tOk6ZO', 1, 'System', 'Administrator', '+1-555-0100', true),
(2, 'librarian', 'librarian@library.edu', '$2a$12$DfSrnLiKwdG/WtUky8Qwf.0Ubgk1SyKyKxds087bA7rVceqJ6VHPu', 2, 'Sarah', 'Connor', '+1-555-0101', true),
(3, 'johndoe', 'john.doe@example.com', '$2a$12$rxMwP5cAoaKNahTV5CTF9..38F9etnqHQL5J44POYkyvY1hIlW.XS', 3, 'John', 'Doe', '+1-555-0102', true),
(4, 'janesmith', 'jane.smith@example.com', '$2a$12$rxMwP5cAoaKNahTV5CTF9..38F9etnqHQL5J44POYkyvY1hIlW.XS', 3, 'Jane', 'Smith', '+1-555-0103', true)
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.users_id_seq', (SELECT MAX(id) FROM public.users));

-- 3. Insert Categories
INSERT INTO public.categories (id, name, description) VALUES
(1, 'Computer Science & Software Engineering', 'Programming languages, algorithms, software design, systems, and AI'),
(2, 'Mathematics & Statistics', 'Calculus, discrete mathematics, linear algebra, probability, and statistics'),
(3, 'Physics & Engineering', 'Classical mechanics, quantum mechanics, electromagnetism, and robotics'),
(4, 'Literature & Classics', 'World literature, literary fiction, essays, and poetry'),
(5, 'Business & Economics', 'Finance, management, economics, leadership, and entrepreneurship')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.categories_id_seq', (SELECT MAX(id) FROM public.categories));

-- 4. Insert Authors
INSERT INTO public.authors (id, first_name, last_name, biography, nationality) VALUES
(1, 'Robert C.', 'Martin', 'Software engineer, craftsman and author known as Uncle Bob.', 'American'),
(2, 'Martin', 'Fowler', 'Software developer, author and speaker specializing in enterprise architecture.', 'British'),
(3, 'Joshua', 'Bloch', 'Software engineer and author who led the design of many Java platform features.', 'American'),
(4, 'Brian', 'Goetz', 'Java Language Architect at Oracle and author of Java Concurrency in Practice.', 'American'),
(5, 'Thomas H.', 'Cormen', 'Professor of Computer Science and co-author of Introduction to Algorithms.', 'American'),
(6, 'Eric', 'Evans', 'Domain-Driven Design thought leader and software design consultant.', 'American'),
(7, 'Donald E.', 'Knuth', 'Computer scientist and author of The Art of Computer Programming.', 'American')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.authors_id_seq', (SELECT MAX(id) FROM public.authors));

-- 5. Insert Publishers
INSERT INTO public.publishers (id, name, contact_email, phone, website, address) VALUES
(1, 'Addison-Wesley Professional', 'contact@addison-wesley.com', '+1-800-824-7799', 'https://www.pearson.com', 'Boston, MA, USA'),
(2, 'O''Reilly Media', 'support@oreilly.com', '+1-707-827-7000', 'https://www.oreilly.com', 'Sebastopol, CA, USA'),
(3, 'Prentice Hall', 'info@prenticehall.com', '+1-800-922-0579', 'https://www.pearson.com', 'Upper Saddle River, NJ, USA'),
(4, 'The MIT Press', 'mitpress-orders@mit.edu', '+1-617-253-5646', 'https://mitpress.mit.edu', 'Cambridge, MA, USA'),
(5, 'Packt Publishing', 'customercare@packt.com', '+44-121-265-6484', 'https://www.packtpub.com', 'Birmingham, UK')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.publishers_id_seq', (SELECT MAX(id) FROM public.publishers));

-- 6. Insert Books
INSERT INTO public.books (id, isbn, title, subtitle, category_id, publisher_id, publication_year, edition, language, description, shelf_location, cover_image_url, is_active) VALUES
(1, '978-0132350884', 'Clean Code', 'A Handbook of Agile Software Craftsmanship', 1, 3, 2008, '1st Edition', 'English', 'Even bad code can function. But if code isn''t clean, it can bring a development organization to its knees.', 'Shelf CS-A1', 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd8?w=400', true),
(2, '978-0134494166', 'Clean Architecture', 'A Craftsman''s Guide to Software Structure and Design', 1, 3, 2017, '1st Edition', 'English', 'Building upon the success of Clean Code, legendary software craftsman Robert C. Martin reveals universal rules of software architecture.', 'Shelf CS-A2', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400', true),
(3, '978-0134685991', 'Effective Java', 'Best Practices for the Java Platform', 1, 1, 2018, '3rd Edition', 'English', 'The definitive guide to Java best practices, covering modularity, lambdas, streams, generics, and concurrent programming.', 'Shelf CS-B1', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400', true),
(4, '978-0321349606', 'Java Concurrency in Practice', 'Modern Multithreading and Memory Models', 1, 1, 2006, '1st Edition', 'English', 'Threads are a fundamental part of the Java platform. This book provides both the theoretical underpinning and concrete techniques.', 'Shelf CS-B2', 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=400', true),
(5, '978-0262033848', 'Introduction to Algorithms', 'CLRS Comprehensive Reference', 1, 4, 2009, '3rd Edition', 'English', 'A comprehensive textbook on computer algorithms, covering dynamic programming, graph algorithms, and data structures.', 'Shelf CS-C1', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=400', true),
(6, '978-0321125217', 'Domain-Driven Design', 'Tackling Complexity in the Heart of Software', 1, 1, 2003, '1st Edition', 'English', 'Presents a systematic approach to domain-driven design, incorporating a collection of design patterns and practical insights.', 'Shelf CS-C2', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400', true),
(7, '978-0201485677', 'Refactoring', 'Improving the Design of Existing Code', 1, 1, 1999, '1st Edition', 'English', 'Explains the principles of refactoring along with step-by-step code transformations that improve program structure.', 'Shelf CS-D1', 'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=400', true)
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.books_id_seq', (SELECT MAX(id) FROM public.books));

-- 7. Insert Book Authors (Bridge Table)
INSERT INTO public.book_authors (book_id, author_id) VALUES
(1, 1),
(2, 1),
(3, 3),
(4, 4),
(4, 3),
(5, 5),
(6, 6),
(7, 2)
ON CONFLICT DO NOTHING;

-- 8. Insert Physical Book Copies
INSERT INTO public.book_copies (id, book_id, accession_number, status, condition) VALUES
(1, 1, 'ACC-0001', 'AVAILABLE', 'Good'),
(2, 1, 'ACC-0002', 'AVAILABLE', 'Good'),
(3, 1, 'ACC-0003', 'BORROWED', 'Fair'),
(4, 2, 'ACC-0004', 'AVAILABLE', 'New'),
(5, 2, 'ACC-0005', 'AVAILABLE', 'Good'),
(6, 3, 'ACC-0006', 'AVAILABLE', 'Good'),
(7, 3, 'ACC-0007', 'BORROWED', 'Good'),
(8, 3, 'ACC-0008', 'AVAILABLE', 'New'),
(9, 4, 'ACC-0009', 'AVAILABLE', 'Good'),
(10, 4, 'ACC-0010', 'AVAILABLE', 'Good'),
(11, 5, 'ACC-0011', 'BORROWED', 'Fair'),
(12, 5, 'ACC-0012', 'AVAILABLE', 'Good'),
(13, 6, 'ACC-0013', 'AVAILABLE', 'Good'),
(14, 6, 'ACC-0014', 'RESERVED', 'Good'),
(15, 7, 'ACC-0015', 'AVAILABLE', 'Good')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.book_copies_id_seq', (SELECT MAX(id) FROM public.book_copies));

-- 9. Insert Members
INSERT INTO public.members (id, user_id, membership_number, first_name, last_name, email, phone, address, membership_start_date, membership_expiry_date, status) VALUES
(1, 3, 'LIB-2024-001', 'John', 'Doe', 'john.doe@example.com', '+1-555-0102', '123 University Ave, Silicon Valley, CA', '2024-01-01', '2027-12-31', 'ACTIVE'),
(2, 4, 'LIB-2024-002', 'Jane', 'Smith', 'jane.smith@example.com', '+1-555-0103', '456 Campus Blvd, Cambridge, MA', '2024-02-15', '2027-12-31', 'ACTIVE'),
(3, NULL, 'LIB-2024-003', 'Robert', 'Taylor', 'robert.taylor@example.com', '+1-555-0104', '789 Library Way, Austin, TX', '2024-03-10', '2027-12-31', 'ACTIVE'),
(4, NULL, 'LIB-2024-004', 'Emily', 'Clark', 'emily.clark@example.com', '+1-555-0105', '101 Pine St, Seattle, WA', '2023-01-01', '2024-01-01', 'EXPIRED'),
(5, NULL, 'LIB-2024-005', 'Michael', 'Johnson', 'michael.j@example.com', '+1-555-0106', '202 Oak Ave, Denver, CO', '2024-04-01', '2027-12-31', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.members_id_seq', (SELECT MAX(id) FROM public.members));

-- 10. Insert Loans (Past and Active)
INSERT INTO public.loans (id, book_copy_id, member_id, issued_by, returned_by, issue_date, due_date, return_date, status, notes) VALUES
(1, 3, 1, 2, NULL, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '9 days', NULL, 'ACTIVE', 'Regular loan'),
(2, 7, 2, 2, NULL, CURRENT_DATE - INTERVAL '20 days', CURRENT_DATE - INTERVAL '6 days', NULL, 'OVERDUE', 'Past due date'),
(3, 11, 3, 2, NULL, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '4 days', NULL, 'ACTIVE', 'Algorithmic study reference'),
(4, 1, 1, 2, 2, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE - INTERVAL '16 days', CURRENT_DATE - INTERVAL '18 days', 'RETURNED', 'Returned in good condition'),
(5, 5, 2, 2, 2, CURRENT_DATE - INTERVAL '40 days', CURRENT_DATE - INTERVAL '26 days', CURRENT_DATE - INTERVAL '20 days', 'RETURNED', 'Returned 6 days late')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.loans_id_seq', (SELECT MAX(id) FROM public.loans));

-- 11. Insert Fines
INSERT INTO public.fines (id, loan_id, member_id, amount, paid_amount, reason, status, created_at, paid_at, payment_method, receipt_number) VALUES
(1, 5, 2, 30.00, 30.00, 'Returned 6 days late (₹5.00/day)', 'PAID', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '19 days', 'CASH', 'RCPT-2024-001'),
(2, 2, 2, 30.00, 0.00, 'Currently overdue by 6 days (₹5.00/day)', 'UNPAID', CURRENT_TIMESTAMP - INTERVAL '6 days', NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.fines_id_seq', (SELECT MAX(id) FROM public.fines));

-- 12. Insert Reservations
INSERT INTO public.reservations (id, book_id, member_id, reservation_date, status, priority, notes) VALUES
(1, 1, 2, CURRENT_TIMESTAMP - INTERVAL '2 days', 'WAITING', 1, 'Waiting for physical copy return')
ON CONFLICT (id) DO NOTHING;
SELECT setval('public.reservations_id_seq', (SELECT MAX(id) FROM public.reservations));

-- 13. Insert Library Operational Settings
INSERT INTO public.library_settings (setting_key, setting_value, description) VALUES
('fine_per_day', '5.00', 'Overdue fine assessment per calendar day in rupees'),
('max_books_per_member', '3', 'Maximum simultaneous active loans permitted per member'),
('loan_duration_days', '14', 'Default circulation borrowing duration in days'),
('reservation_pickup_days', '3', 'Days before an unclaimed ready reservation expires')
ON CONFLICT (setting_key) DO NOTHING;

-- 14. Insert Initial System Audit Log
INSERT INTO public.audit_logs (user_id, action, entity, entity_id, details) VALUES
(1, 'DATABASE_SEED', 'SYSTEM', NULL, 'Database schema and seed data initialized successfully');
