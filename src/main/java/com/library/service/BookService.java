package com.library.service;

import com.library.config.DatabaseConfig;
import com.library.dao.*;
import com.library.dto.PaginatedResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.AuditLog;
import com.library.model.Book;
import com.library.model.BookCopy;
import com.library.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);
    private final BookDAO bookDAO = new BookDAO();
    private final BookCopyDAO bookCopyDAO = new BookCopyDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public PaginatedResponse<Book> getBooks(String search, Integer categoryId, Integer authorId, String availability,
                                            Boolean activeOnly, String sortField, String sortOrder, int page, int pageSize) {
        int limit = pageSize > 0 ? pageSize : 10;
        int currentPage = page > 0 ? page : 1;
        int offset = (currentPage - 1) * limit;

        long total = bookDAO.count(search, categoryId, authorId, availability, activeOnly);
        List<Book> items = bookDAO.findAll(search, categoryId, authorId, availability, activeOnly, sortField, sortOrder, offset, limit);

        return new PaginatedResponse<>(items, total, currentPage, limit);
    }

    public Book getBookById(long id) {
        Book book = bookDAO.findById(id);
        if (book == null) {
            throw new ResourceNotFoundException("Book with ID " + id + " not found");
        }
        return book;
    }

    public Book createBook(Book book, List<Integer> authorIds, int initialCopies, Long staffUserId, String ipAddress) {
        validateBook(book, true);

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Book created = bookDAO.create(book, conn);
                if (created == null) {
                    throw new BusinessRuleException("Failed to persist book record");
                }

                if (authorIds != null && !authorIds.isEmpty()) {
                    bookDAO.setBookAuthors(created.getId(), authorIds, conn);
                }

                // Auto-create initial physical copies
                if (initialCopies > 0) {
                    for (int i = 1; i <= initialCopies; i++) {
                        String accNumber = String.format("ACC-%04d-%02d", created.getId(), i);
                        // Ensure unique accession number
                        if (bookCopyDAO.findByAccessionNumber(accNumber) != null) {
                            accNumber = "ACC-" + created.getId() + "-" + System.currentTimeMillis() % 10000;
                        }
                        BookCopy copy = new BookCopy();
                        copy.setBookId(created.getId());
                        copy.setAccessionNumber(accNumber);
                        copy.setStatus("AVAILABLE");
                        copy.setCondition("Good");
                        bookCopyDAO.create(copy, conn);
                    }
                }

                conn.commit();
                auditLogDAO.log(new AuditLog(staffUserId, "CREATE_BOOK", "BOOK", created.getId(),
                        "Created book '" + created.getTitle() + "' with " + initialCopies + " copies", ipAddress));
                return getBookById(created.getId());

            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction rolled back while creating book", e);
                throw new BusinessRuleException("Could not create book: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure during book creation", e);
            throw new BusinessRuleException("Database failure: " + e.getMessage());
        }
    }

    public Book updateBook(long id, Book book, List<Integer> authorIds, Long staffUserId, String ipAddress) {
        Book existing = bookDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Book with ID " + id + " not found");
        }

        book.setId(id);
        validateBook(book, false);

        // Check duplicate ISBN if changed
        if (ValidationUtil.isNotEmpty(book.getIsbn()) && !book.getIsbn().equalsIgnoreCase(existing.getIsbn())) {
            Book duplicate = bookDAO.findByIsbn(book.getIsbn());
            if (duplicate != null && duplicate.getId() != id) {
                throw new ValidationException("Another book already exists with ISBN " + book.getIsbn());
            }
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                bookDAO.update(book, conn);
                if (authorIds != null) {
                    bookDAO.setBookAuthors(id, authorIds, conn);
                }
                conn.commit();
                auditLogDAO.log(new AuditLog(staffUserId, "UPDATE_BOOK", "BOOK", id,
                        "Updated book details for '" + book.getTitle() + "'", ipAddress));
                return getBookById(id);
            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction rolled back while updating book {}", id, e);
                throw new BusinessRuleException("Could not update book: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure during book update", e);
            throw new BusinessRuleException("Database failure: " + e.getMessage());
        }
    }

    public void deactivateBook(long id, Long staffUserId, String ipAddress) {
        Book book = bookDAO.findById(id);
        if (book == null) {
            throw new ResourceNotFoundException("Book with ID " + id + " not found");
        }

        if (book.getBorrowedCopies() > 0) {
            throw new BusinessRuleException("Cannot deactivate book while " + book.getBorrowedCopies() + " physical copies are currently borrowed");
        }

        bookDAO.setActive(id, false);
        auditLogDAO.log(new AuditLog(staffUserId, "DEACTIVATE_BOOK", "BOOK", id,
                "Deactivated book '" + book.getTitle() + "'", ipAddress));
    }

    public void activateBook(long id, Long staffUserId, String ipAddress) {
        Book book = bookDAO.findById(id);
        if (book == null) {
            throw new ResourceNotFoundException("Book with ID " + id + " not found");
        }
        bookDAO.setActive(id, true);
        auditLogDAO.log(new AuditLog(staffUserId, "ACTIVATE_BOOK", "BOOK", id,
                "Activated book '" + book.getTitle() + "'", ipAddress));
    }

    public List<BookCopy> getBookCopies(long bookId) {
        getBookById(bookId); // verify existence
        return bookCopyDAO.findByBookId(bookId);
    }

    public BookCopy addCopy(long bookId, String accessionNumber, String condition, Long staffUserId, String ipAddress) {
        Book book = getBookById(bookId);

        if (!ValidationUtil.isNotEmpty(accessionNumber)) {
            throw new ValidationException("Accession number is required");
        }

        if (bookCopyDAO.findByAccessionNumber(accessionNumber.trim()) != null) {
            throw new ValidationException("A copy with accession number '" + accessionNumber + "' already exists");
        }

        BookCopy copy = new BookCopy();
        copy.setBookId(bookId);
        copy.setAccessionNumber(accessionNumber.trim());
        copy.setStatus("AVAILABLE");
        copy.setCondition(ValidationUtil.isNotEmpty(condition) ? condition.trim() : "Good");

        try (Connection conn = DatabaseConfig.getConnection()) {
            BookCopy created = bookCopyDAO.create(copy, conn);
            auditLogDAO.log(new AuditLog(staffUserId, "ADD_COPY", "BOOK_COPY", created.getId(),
                    "Added copy " + accessionNumber + " to '" + book.getTitle() + "'", ipAddress));
            return created;
        } catch (SQLException e) {
            logger.error("Error creating book copy", e);
            throw new BusinessRuleException("Could not add book copy: " + e.getMessage());
        }
    }

    public boolean updateCopyStatus(long copyId, String status, String condition, Long staffUserId, String ipAddress) {
        BookCopy copy = bookCopyDAO.findById(copyId);
        if (copy == null) {
            throw new ResourceNotFoundException("Book copy not found");
        }

        if ("BORROWED".equalsIgnoreCase(copy.getStatus()) && !"BORROWED".equalsIgnoreCase(status)) {
            throw new BusinessRuleException("Cannot alter status directly while copy is actively borrowed. Must process return first.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean ok = bookCopyDAO.updateStatus(copyId, status, conn);
            if (ValidationUtil.isNotEmpty(condition)) {
                bookCopyDAO.updateCondition(copyId, condition);
            }
            auditLogDAO.log(new AuditLog(staffUserId, "UPDATE_COPY_STATUS", "BOOK_COPY", copyId,
                    "Changed copy " + copy.getAccessionNumber() + " status to " + status, ipAddress));
            return ok;
        } catch (SQLException e) {
            logger.error("Error updating copy status", e);
            throw new BusinessRuleException("Could not update copy status: " + e.getMessage());
        }
    }

    private void validateBook(Book book, boolean isNew) {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isNotEmpty(book.getTitle())) {
            errors.add("Book title is required");
        }
        if (book.getCategoryId() <= 0) {
            errors.add("A valid category must be selected");
        } else if (categoryDAO.findById(book.getCategoryId()) == null) {
            errors.add("Selected category does not exist");
        }
        if (ValidationUtil.isNotEmpty(book.getIsbn())) {
            if (!ValidationUtil.isValidIsbn(book.getIsbn())) {
                errors.add("Invalid ISBN format. Must be a valid 10 or 13 digit ISBN");
            } else if (isNew) {
                Book existing = bookDAO.findByIsbn(book.getIsbn());
                if (existing != null) {
                    errors.add("A book with ISBN '" + book.getIsbn() + "' already exists");
                }
            }
        }
        if (book.getPublicationYear() != null && (book.getPublicationYear() < 1000 || book.getPublicationYear() > 2100)) {
            errors.add("Publication year must be between 1000 and 2100");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
