package com.library.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class Reservation {
    private long id;
    private long bookId;
    private String bookTitle;
    private String isbn;
    private long memberId;
    private String memberName;
    private String membershipNumber;
    private OffsetDateTime reservationDate;
    private String status = "WAITING"; // WAITING, READY_FOR_PICKUP, FULFILLED, CANCELLED, EXPIRED
    private int priority = 1;
    private OffsetDateTime notificationSentAt;
    private LocalDate pickupDeadline;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Reservation() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }

    public OffsetDateTime getReservationDate() { return reservationDate; }
    public void setReservationDate(OffsetDateTime reservationDate) { this.reservationDate = reservationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public OffsetDateTime getNotificationSentAt() { return notificationSentAt; }
    public void setNotificationSentAt(OffsetDateTime notificationSentAt) { this.notificationSentAt = notificationSentAt; }

    public LocalDate getPickupDeadline() { return pickupDeadline; }
    public void setPickupDeadline(LocalDate pickupDeadline) { this.pickupDeadline = pickupDeadline; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
