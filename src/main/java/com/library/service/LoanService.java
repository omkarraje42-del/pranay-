package com.library.service;

import com.library.config.DatabaseConfig;
import com.library.dao.*;
import com.library.dto.PaginatedResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    private final LoanDAO loanDAO = new LoanDAO();
    private final BookCopyDAO bookCopyDAO = new BookCopyDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final FineDAO fineDAO = new FineDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final LibrarySettingDAO settingDAO = new LibrarySettingDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public PaginatedResponse<Loan> getLoans(String search, Long memberId, Long bookId, String status,
                                            LocalDate fromDate, LocalDate toDate, int page, int pageSize) {
        int limit = pageSize > 0 ? pageSize : 10;
        int currentPage = page > 0 ? page : 1;
        int offset = (currentPage - 1) * limit;

        long total = loanDAO.count(search, memberId, bookId, status, fromDate, toDate);
        List<Loan> list = loanDAO.findAll(search, memberId, bookId, status, fromDate, toDate, offset, limit);
        return new PaginatedResponse<>(list, total, currentPage, limit);
    }

    public Loan getLoanById(long id) {
        Loan loan = loanDAO.findById(id);
        if (loan == null) {
            throw new ResourceNotFoundException("Loan transaction with ID " + id + " not found");
        }
        return loan;
    }

    public List<Loan> getOverdueLoans() {
        return loanDAO.findOverdueLoans();
    }

    public List<Loan> getLoansByMember(long memberId) {
        return loanDAO.findAll(null, memberId, null, null, null, null, 0, 100);
    }

    /**
     * ACID Transactional Book Issue Workflow with Pessimistic Row Locking.
     */
    public Loan issueBook(long memberId, Long copyId, String accessionNumber, Integer loanDays,
                          Long staffUserId, String notes, String ipAddress) {
        Member member = memberDAO.findById(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("Member with ID " + memberId + " not found");
        }

        // Rule 1: Member Status Check
        if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
            throw new BusinessRuleException("Cannot issue book. Member status is currently " + member.getStatus());
        }

        // Rule 2: Membership Expiry Check
        if (member.getMembershipExpiryDate() != null && member.getMembershipExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Cannot issue book. Membership expired on " + member.getMembershipExpiryDate());
        }

        // Rule 3: Max Active Loans Policy
        int maxLoans = settingDAO.getIntSetting("max_loans_per_member", 5);
        if (member.getActiveLoansCount() >= maxLoans) {
            throw new BusinessRuleException("Borrowing limit reached. Member already has " +
                    member.getActiveLoansCount() + " active loans (policy limit is " + maxLoans + ")");
        }

        // Rule 4: Unpaid Fines Block Policy
        boolean allowFineBlock = "true".equalsIgnoreCase(settingDAO.getSetting("allow_fine_block", "true"));
        double fineThreshold = settingDAO.getDoubleSetting("fine_block_threshold", 50.0);
        if (allowFineBlock && member.getUnpaidFinesTotal().doubleValue() > fineThreshold) {
            throw new BusinessRuleException("Member has unpaid fines of ₹" + member.getUnpaidFinesTotal() +
                    " exceeding allowed threshold of ₹" + fineThreshold + ". Clear outstanding fines first.");
        }

        // Determine target copy id
        long targetCopyId;
        if (copyId != null && copyId > 0) {
            targetCopyId = copyId;
        } else if (accessionNumber != null && !accessionNumber.trim().isEmpty()) {
            BookCopy bc = bookCopyDAO.findByAccessionNumber(accessionNumber.trim());
            if (bc == null) {
                throw new ResourceNotFoundException("No physical copy found with accession number '" + accessionNumber + "'");
            }
            targetCopyId = bc.getId();
        } else {
            throw new ValidationException("Either copy ID or accession number must be provided");
        }

        int duration = (loanDays != null && loanDays > 0) ? loanDays : settingDAO.getIntSetting("default_loan_days", 14);
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(duration);

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Rule 5: Concurrency-Safe Pessimistic Row Lock (SELECT ... FOR UPDATE)
                BookCopy copy = bookCopyDAO.findByIdForUpdate(targetCopyId, conn);
                if (copy == null) {
                    throw new ResourceNotFoundException("Book copy not found");
                }

                // Check reservation queue: If the copy is marked RESERVED, ensure it is reserved for this member
                if ("RESERVED".equalsIgnoreCase(copy.getStatus())) {
                    Reservation res = reservationDAO.findActiveByBookAndMember(copy.getBookId(), memberId);
                    if (res == null || !"READY_FOR_PICKUP".equalsIgnoreCase(res.getStatus())) {
                        throw new BusinessRuleException("This copy is currently reserved for another member with higher queue priority.");
                    } else {
                        // Fulfill the reservation as part of this issue
                        reservationDAO.updateStatus(res.getId(), "FULFILLED", null, null, conn);
                    }
                } else if (!"AVAILABLE".equalsIgnoreCase(copy.getStatus())) {
                    throw new BusinessRuleException("Book copy " + copy.getAccessionNumber() +
                            " is not available for issue. Current status: " + copy.getStatus());
                }

                // Create loan record
                Loan loan = new Loan();
                loan.setBookCopyId(targetCopyId);
                loan.setMemberId(memberId);
                loan.setIssuedBy(staffUserId);
                loan.setIssueDate(issueDate);
                loan.setDueDate(dueDate);
                loan.setStatus("ACTIVE");
                loan.setNotes(notes);

                Loan created = loanDAO.create(loan, conn);

                // Update copy status to BORROWED
                bookCopyDAO.updateStatus(targetCopyId, "BORROWED", conn);

                conn.commit();

                auditLogDAO.log(new AuditLog(staffUserId, "ISSUE_BOOK", "LOAN", created.getId(),
                        "Issued copy " + copy.getAccessionNumber() + " (" + copy.getBookTitle() + ") to " +
                                member.getFullName() + ". Due date: " + dueDate, ipAddress));

                logger.info("Loan #{} issued successfully: Copy {} to Member {}", created.getId(), copy.getAccessionNumber(), member.getMembershipNumber());
                return loanDAO.findById(created.getId());

            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction rolled back during book issue", e);
                throw (e instanceof RuntimeException) ? (RuntimeException) e : new BusinessRuleException(e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection error during book issue", e);
            throw new BusinessRuleException("Database error: " + e.getMessage());
        }
    }

    /**
     * ACID Transactional Book Return Workflow with Automated Overdue Fine Calculation and Reservation Handoff.
     */
    public Loan returnBook(long loanId, Long staffUserId, String notes, String ipAddress) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock loan record
                Loan loan = loanDAO.findByIdForUpdate(loanId, conn);
                if (loan == null) {
                    throw new ResourceNotFoundException("Loan transaction with ID " + loanId + " not found");
                }

                if ("RETURNED".equalsIgnoreCase(loan.getStatus())) {
                    throw new BusinessRuleException("Book copy has already been returned on " + loan.getReturnDate());
                }

                LocalDate returnDate = LocalDate.now();
                long overdueDays = 0;
                BigDecimal fineAmount = BigDecimal.ZERO;

                if (loan.getDueDate() != null && returnDate.isAfter(loan.getDueDate())) {
                    overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
                    double fineRate = settingDAO.getDoubleSetting("fine_per_day", 5.0);
                    fineAmount = BigDecimal.valueOf(overdueDays * fineRate);
                }

                // Update loan transaction
                loanDAO.updateReturn(loanId, returnDate, staffUserId, "RETURNED", notes, conn);

                // Generate fine if overdue
                if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                    Fine fine = new Fine();
                    fine.setLoanId(loanId);
                    fine.setMemberId(loan.getMemberId());
                    fine.setAmount(fineAmount);
                    fine.setReason("Overdue by " + overdueDays + " day(s) at ₹" + settingDAO.getSetting("fine_per_day", "5.00") + "/day");
                    fine.setStatus("UNPAID");
                    fineDAO.create(fine, conn);
                }

                // Check reservation queue for this title
                Reservation nextRes = reservationDAO.findNextWaitingReservationForBook(loan.getBookId(), conn);
                if (nextRes != null) {
                    // Reserve this copy for the waiting member
                    bookCopyDAO.updateStatus(loan.getBookCopyId(), "RESERVED", conn);
                    int pickupDays = settingDAO.getIntSetting("reservation_pickup_days", 3);
                    LocalDate pickupDeadline = returnDate.plusDays(pickupDays);
                    reservationDAO.updateStatus(nextRes.getId(), "READY_FOR_PICKUP", OffsetDateTime.now(), pickupDeadline, conn);

                    logger.info("Copy {} auto-assigned to waiting reservation #{} for member #{}",
                            loan.getAccessionNumber(), nextRes.getId(), nextRes.getMemberId());
                } else {
                    // Mark copy as AVAILABLE
                    bookCopyDAO.updateStatus(loan.getBookCopyId(), "AVAILABLE", conn);
                }

                conn.commit();

                String logDetail = "Returned copy " + loan.getAccessionNumber() + " from " + loan.getMemberName();
                if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                    logDetail += ". Overdue " + overdueDays + " days, fine assessed: ₹" + fineAmount;
                }
                auditLogDAO.log(new AuditLog(staffUserId, "RETURN_BOOK", "LOAN", loanId, logDetail, ipAddress));

                logger.info("Loan #{} processed return successfully.", loanId);
                return loanDAO.findById(loanId);

            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction rolled back during book return", e);
                throw (e instanceof RuntimeException) ? (RuntimeException) e : new BusinessRuleException(e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection error during book return", e);
            throw new BusinessRuleException("Database error: " + e.getMessage());
        }
    }
}
