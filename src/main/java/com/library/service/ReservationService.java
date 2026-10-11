package com.library.service;

import com.library.config.DatabaseConfig;
import com.library.dao.*;
import com.library.dto.PaginatedResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final BookDAO bookDAO = new BookDAO();
    private final BookCopyDAO bookCopyDAO = new BookCopyDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final LibrarySettingDAO settingDAO = new LibrarySettingDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public PaginatedResponse<Reservation> getReservations(Long memberId, Long bookId, String status, int page, int pageSize) {
        int limit = pageSize > 0 ? pageSize : 10;
        int currentPage = page > 0 ? page : 1;
        int offset = (currentPage - 1) * limit;

        long total = reservationDAO.count(memberId, bookId, status);
        List<Reservation> list = reservationDAO.findAll(memberId, bookId, status, offset, limit);
        return new PaginatedResponse<>(list, total, currentPage, limit);
    }

    public Reservation placeReservation(long bookId, long memberId, String notes, Long staffUserId, String ipAddress) {
        Book book = bookDAO.findById(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found");
        }

        Member member = memberDAO.findById(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("Member not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
            throw new BusinessRuleException("Cannot place reservation. Member is currently " + member.getStatus());
        }

        // Prevent duplicate active reservation for same book
        Reservation existing = reservationDAO.findActiveByBookAndMember(bookId, memberId);
        if (existing != null) {
            throw new BusinessRuleException("Member already has an active reservation for '" + book.getTitle() + "' (Status: " + existing.getStatus() + ")");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Reservation res = new Reservation();
                res.setBookId(bookId);
                res.setMemberId(memberId);
                res.setNotes(notes);

                // Check if any copy is already available right now
                BookCopy availCopy = bookCopyDAO.findFirstAvailableCopyByBookId(bookId, conn);
                if (availCopy != null) {
                    // Instantly ready for pickup
                    bookCopyDAO.updateStatus(availCopy.getId(), "RESERVED", conn);
                    res.setStatus("READY_FOR_PICKUP");
                    int pickupDays = settingDAO.getIntSetting("reservation_pickup_days", 3);
                    res.setPickupDeadline(LocalDate.now().plusDays(pickupDays));
                    res.setNotificationSentAt(OffsetDateTime.now());
                } else {
                    res.setStatus("WAITING");
                }

                Reservation created = reservationDAO.create(res, conn);
                conn.commit();

                auditLogDAO.log(new AuditLog(staffUserId, "CREATE_RESERVATION", "RESERVATION", created.getId(),
                        "Placed reservation for '" + book.getTitle() + "' by " + member.getFullName() +
                                " (Queue status: " + res.getStatus() + ")", ipAddress));

                return reservationDAO.findById(created.getId());

            } catch (Exception e) {
                conn.rollback();
                logger.error("Error creating reservation", e);
                throw (e instanceof RuntimeException) ? (RuntimeException) e : new BusinessRuleException(e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure", e);
            throw new BusinessRuleException("Database failure: " + e.getMessage());
        }
    }

    public void cancelReservation(long id, Long staffUserId, String ipAddress) {
        Reservation res = reservationDAO.findById(id);
        if (res == null) {
            throw new ResourceNotFoundException("Reservation not found");
        }

        if ("CANCELLED".equalsIgnoreCase(res.getStatus()) || "FULFILLED".equalsIgnoreCase(res.getStatus())) {
            throw new BusinessRuleException("Reservation is already " + res.getStatus());
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                reservationDAO.updateStatus(id, "CANCELLED", null, null, conn);

                // If reservation was READY_FOR_PICKUP, hand off to next in queue or release copy
                if ("READY_FOR_PICKUP".equalsIgnoreCase(res.getStatus())) {
                    Reservation next = reservationDAO.findNextWaitingReservationForBook(res.getBookId(), conn);
                    if (next != null) {
                        int pickupDays = settingDAO.getIntSetting("reservation_pickup_days", 3);
                        reservationDAO.updateStatus(next.getId(), "READY_FOR_PICKUP", OffsetDateTime.now(), LocalDate.now().plusDays(pickupDays), conn);
                    } else {
                        // Release copy back to AVAILABLE
                        List<BookCopy> copies = bookCopyDAO.findByBookId(res.getBookId());
                        for (BookCopy c : copies) {
                            if ("RESERVED".equalsIgnoreCase(c.getStatus())) {
                                bookCopyDAO.updateStatus(c.getId(), "AVAILABLE", conn);
                                break;
                            }
                        }
                    }
                }

                conn.commit();
                auditLogDAO.log(new AuditLog(staffUserId, "CANCEL_RESERVATION", "RESERVATION", id,
                        "Cancelled reservation #" + id + " for book " + res.getBookTitle(), ipAddress));

            } catch (Exception e) {
                conn.rollback();
                logger.error("Error cancelling reservation", e);
                throw new BusinessRuleException("Could not cancel reservation: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database failure", e);
            throw new BusinessRuleException("Database failure: " + e.getMessage());
        }
    }
}
