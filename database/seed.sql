-- ============================================================================
-- LIBRARY MANAGEMENT SYSTEM - SEED DATA (PostgreSQL)
-- ============================================================================

-- 1. Insert Roles
INSERT INTO roles (id, name, description) VALUES
(1, 'ADMIN', 'System Administrator with full access to all modules and configurations'),
(2, 'LIBRARIAN', 'Library Staff with permissions to manage books, members, issues and returns'),
(3, 'MEMBER', 'Library Member with access to catalog search, loan history and reservations')
ON CONFLICT (id) DO NOTHING;

SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));

-- 2. Insert Default Users
-- Passwords:
-- admin: Admin@123 -> $2a$10$tM3NqB7t7z10Qn1JbZlZLe8m9A.gUvG.n13n0q2kR5b6u7y8i9o0p (standard test BCrypt hash)
-- librarian: Librarian@123 -> $2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQv20b52rF5K43F29.U7LwVz6UaKsy
-- member1: Member@123 -> $2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQv20b52rF5K43F29.U7LwVz6UaKsy
INSERT INTO users (id, username, email, password_hash, role_id, first_name, last_name, phone, is_active) VALUES
(1, 'admin', 'admin@library.edu', '$2a$12$eBDj1RjICB2yQ/VqTTZJP.c/MWMqquvTe9m3TzuZesczqB2tOk6ZO', 1, 'System', 'Administrator', '+1-555-0100', true),
(2, 'librarian', 'librarian@library.edu', '$2a$12$DfSrnLiKwdG/WtUky8Qwf.0Ubgk1SyKyKxds087bA7rVceqJ6VHPu', 2, 'Sarah', 'Connor', '+1-555-0101', true),
(3, 'johndoe', 'john.doe@example.com', '$2a$12$rxMwP5cAoaKNahTV5CTF9..38F9etnqHQL5J44POYkyvY1hIlW.XS', 3, 'John', 'Doe', '+1-555-0102', true),
(4, 'janesmith', 'jane.smith@example.com', '$2a$12$rxMwP5cAoaKNahTV5CTF9..38F9etnqHQL5J44POYkyvY1hIlW.XS', 3, 'Jane', 'Smith', '+1-555-0103', true)
ON CONFLICT (id) DO NOTHING;

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- 3. Insert Categories
INSERT INTO categories (id, name, description) VALUES
(1, 'Computer Science & Software Engineering', 'Programming languages, algorithms, software design, systems, and AI'),
(2, 'Mathematics & Statistics', 'Calculus, discrete mathematics, linear algebra, probability, and statistics'),
(3, 'Physics & Engineering', 'Classical mechanics, quantum mechanics, electromagnetism, and robotics'),
(4, 'Literature & Classics', 'World literature, literary fiction, essays, and poetry'),
(5, 'Business & Economics', 'Finance, management, economics, leadership, and entrepreneurship')
ON CONFLICT (id) DO NOTHING;

SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- 4. Insert Authors
INSERT INTO authors (id, first_name, last_name, biography, nationality) VALUES
(1, 'Robert C.', 'Martin', 'Software engineer, craftsman and author known as Uncle Bob.', 'American'),
(2, 'Martin', 'Fowler', 'Software developer, author and speaker specializing in enterprise architecture.', 'British'),
(3, 'Joshua', 'Bloch', 'Software engineer and author who led the design of many Java platform features.', 'American'),
(4, 'Brian', 'Goetz', 'Java Language Architect at Oracle and author of Java Concurrency in Practice.', 'American'),
(5, 'Thomas H.', 'Cormen', 'Professor of Computer Science and co-author of Introduction to Algorithms.', 'American'),
(6, 'Eric', 'Evans', 'Domain-Driven Design thought leader and software design consultant.', 'American'),
(7, 'Donald E.', 'Knuth', 'Computer scientist and author of The Art of Computer Programming.', 'American')
ON CONFLICT (id) DO NOTHING;

SELECT setval('authors_id_seq', (SELECT MAX(id) FROM authors));

-- 5. Insert Publishers
INSERT INTO publishers (id, name, contact_email, phone, website, address) VALUES
(1, 'Addison-Wesley Professional', 'contact@addison-wesley.com', '+1-800-824-7799', 'https://www.pearson.com', 'Boston, MA, USA'),
(2, 'O''Reilly Media', 'support@oreilly.com', '+1-707-827-7000', 'https://www.oreilly.com', 'Sebastopol, CA, USA'),
(3, 'Prentice Hall', 'info@prenticehall.com', '+1-800-922-0579', 'https://www.pearson.com', 'Upper Saddle River, NJ, USA'),
(4, 'The MIT Press', 'mitpress-orders@mit.edu', '+1-617-253-5646', 'https://mitpress.mit.edu', 'Cambridge, MA, USA'),
(5, 'Packt Publishing', 'customercare@packt.com', '+44-121-265-6484', 'https://www.packtpub.com', 'Birmingham, UK')
ON CONFLICT (id) DO NOTHING;

SELECT setval('publishers_id_seq', (SELECT MAX(id) FROM publishers));

-- 6. Insert Books
INSERT INTO books (id, isbn, title, subtitle, category_id, publisher_id, publication_year, edition, language, description, shelf_location, cover_image_url, is_active) VALUES
(1, '978-0132350884', 'Clean Code', 'A Handbook of Agile Software Craftsmanship', 1, 3, 2008, '1st Edition', 'English', 'Even bad code can function. But if code isn''t clean, it can bring a development organization to its knees.', 'Shelf CS-A1', 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd8?w=400', true),
(2, '978-0134494166', 'Clean Architecture', 'A Craftsman''s Guide to Software Structure and Design', 1, 3, 2017, '1st Edition', 'English', 'Building upon the success of Clean Code, legendary software craftsman Robert C. Martin reveals universal rules of software architecture.', 'Shelf CS-A2', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400', true),
(3, '978-0134685991', 'Effective Java', 'Best Practices for the Java Platform', 1, 1, 2018, '3rd Edition', 'English', 'The definitive guide to Java best practices, covering modularity, lambdas, streams, generics, and concurrent programming.', 'Shelf CS-B1', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400', true),
(4, '978-0321349606', 'Java Concurrency in Practice', 'Modern Multithreading and Memory Models', 1, 1, 2006, '1st Edition', 'English', 'Threads are a fundamental part of the Java platform. This book provides both the theoretical underpinning and concrete techniques.', 'Shelf CS-B2', 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=400', true),
(5, '978-0262033848', 'Introduction to Algorithms', 'CLRS Comprehensive Reference', 1, 4, 2009, '3rd Edition', 'English', 'A comprehensive textbook on computer algorithms, covering dynamic programming, graph algorithms, and data structures.', 'Shelf CS-C1', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=400', true),
(6, '978-0321125217', 'Domain-Driven Design', 'Tackling Complexity in the Heart of Software', 1, 1, 2003, '1st Edition', 'English', 'Presents a systematic approach to domain-driven design, incorporating a collection of design patterns and practical insights.', 'Shelf CS-C2', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400', true),
(7, '978-0201485677', 'Refactoring', 'Improving the Design of Existing Code', 1, 1, 1999, '1st Edition', 'English', 'Explains the principles of refactoring along with step-by-step code transformations that improve program structure.', 'Shelf CS-D1', 'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=400', true)
ON CONFLICT (id) DO NOTHING;

SELECT setval('books_id_seq', (SELECT MAX(id) FROM books));

-- 7. Insert Book Authors
INSERT INTO book_authors (book_id, author_id) VALUES
(1, 1), -- Clean Code -> Robert C. Martin
(2, 1), -- Clean Architecture -> Robert C. Martin
(3, 3), -- Effective Java -> Joshua Bloch
(4, 4), -- Java Concurrency in Practice -> Brian Goetz
(4, 3), -- Java Concurrency in Practice -> Joshua Bloch (co-author)
(5, 5), -- Introduction to Algorithms -> Thomas H. Cormen
(6, 6), -- Domain-Driven Design -> Eric Evans
(7, 2)  -- Refactoring -> Martin Fowler
ON CONFLICT DO NOTHING;

-- 8. Insert Physical Book Copies
INSERT INTO book_copies (id, book_id, accession_number, status, condition) VALUES
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

SELECT setval('book_copies_id_seq', (SELECT MAX(id) FROM book_copies));

-- 9. Insert Members
INSERT INTO members (id, user_id, membership_number, first_name, last_name, email, phone, address, membership_start_date, membership_expiry_date, status) VALUES
(1, 3, 'LIB-2024-001', 'John', 'Doe', 'john.doe@example.com', '+1-555-0102', '123 University Ave, Silicon Valley, CA', '2024-01-01', '2027-12-31', 'ACTIVE'),
(2, 4, 'LIB-2024-002', 'Jane', 'Smith', 'jane.smith@example.com', '+1-555-0103', '456 Campus Blvd, Cambridge, MA', '2024-02-15', '2027-12-31', 'ACTIVE'),
(3, NULL, 'LIB-2024-003', 'Robert', 'Taylor', 'robert.taylor@example.com', '+1-555-0104', '789 Library Way, Austin, TX', '2024-03-10', '2027-12-31', 'ACTIVE'),
(4, NULL, 'LIB-2024-004', 'Emily', 'Clark', 'emily.clark@example.com', '+1-555-0105', '101 Pine St, Seattle, WA', '2023-01-01', '2024-01-01', 'EXPIRED'),
(5, NULL, 'LIB-2024-005', 'Michael', 'Johnson', 'michael.j@example.com', '+1-555-0106', '202 Oak Ave, Denver, CO', '2024-04-01', '2027-12-31', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

SELECT setval('members_id_seq', (SELECT MAX(id) FROM members));

-- 10. Insert Loans (Past and Active)
INSERT INTO loans (id, book_copy_id, member_id, issued_by, returned_by, issue_date, due_date, return_date, status, notes) VALUES
-- Active loan within due date
(1, 3, 1, 2, NULL, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '9 days', NULL, 'ACTIVE', 'Regular loan'),
-- Active overdue loan
(2, 7, 2, 2, NULL, CURRENT_DATE - INTERVAL '20 days', CURRENT_DATE - INTERVAL '6 days', NULL, 'OVERDUE', 'Past due date'),
-- Active loan
(3, 11, 3, 2, NULL, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '4 days', NULL, 'ACTIVE', 'Algorithmic study reference'),
-- Returned on time
(4, 1, 1, 2, 2, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE - INTERVAL '16 days', CURRENT_DATE - INTERVAL '18 days', 'RETURNED', 'Returned in good condition'),
-- Returned late with fine
(5, 5, 2, 2, 2, CURRENT_DATE - INTERVAL '40 days', CURRENT_DATE - INTERVAL '26 days', CURRENT_DATE - INTERVAL '20 days', 'RETURNED', 'Returned 6 days late')
ON CONFLICT (id) DO NOTHING;

SELECT setval('loans_id_seq', (SELECT MAX(id) FROM loans));

-- 11. Insert Fines
INSERT INTO fines (id, loan_id, member_id, amount, reason, status, created_at, paid_at, payment_method, receipt_number) VALUES
-- Paid fine from previous late return
(1, 5, 2, 30.00, 'Returned 6 days late (₹5.00/day)', 'PAID', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '19 days', 'CASH', 'RCPT-2024-001'),
-- Unpaid fine for current overdue loan
(2, 2, 2, 30.00, 'Overdue book loan (6 days)', 'UNPAID', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

SELECT setval('fines_id_seq', (SELECT MAX(id) FROM fines));

-- 12. Insert Reservations
INSERT INTO reservations (id, book_id, member_id, reservation_date, status, priority, notes) VALUES
(1, 6, 1, CURRENT_TIMESTAMP - INTERVAL '2 days', 'READY_FOR_PICKUP', 1, 'Notification sent to member')
ON CONFLICT (id) DO NOTHING;

SELECT setval('reservations_id_seq', (SELECT MAX(id) FROM reservations));

-- 13. Insert Library Settings
INSERT INTO library_settings (setting_key, setting_value, description) VALUES
('default_loan_days', '14', 'Default loan period in days for borrowed books'),
('max_loans_per_member', '5', 'Maximum number of concurrent active loans per member'),
('fine_per_day', '5.00', 'Overdue fine rate per day in local currency'),
('allow_fine_block', 'true', 'Whether unpaid fines above threshold block new borrowings'),
('fine_block_threshold', '50.00', 'Unpaid fine threshold that blocks borrowing'),
('reservation_pickup_days', '3', 'Number of days a reserved book is held for pickup'),
('library_name', 'Metropolitan Central Library', 'Display name of the library institution'),
('currency_symbol', '₹', 'Currency symbol used across system')
ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value;

-- 14. Insert Initial Audit Log
INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address) VALUES
(1, 'SYSTEM_INIT', 'SYSTEM', 1, 'Initial database schema and seed data loaded successfully', '127.0.0.1');
