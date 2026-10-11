package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class Loan {
    private long id;
    private long bookCopyId;
    private String accessionNumber;
    private long bookId;
    private String bookTitle;
    private String isbn;
    private long memberId;
    private String memberName;
    private String membershipNumber;
    private String memberEmail;
    private Long issuedBy;
    private String issuedByName;
    private Long returnedBy;
    private String returnedByName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status = "ACTIVE"; // ACTIVE, RETURNED, OVERDUE, LOST
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Derived fields
    private long overdueDays;
    private BigDecimal fineAmount;
    private String fineStatus; // UNPAID, PAID, WAIVED

    public Loan() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBookCopyId() { return bookCopyId; }
    public void setBookCopyId(long bookCopyId) { this.bookCopyId = bookCopyId; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

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

    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }

    public Long getIssuedBy() { return issuedBy; }
    public void setIssuedBy(Long issuedBy) { this.issuedBy = issuedBy; }

    public String getIssuedByName() { return issuedByName; }
    public void setIssuedByName(String issuedByName) { this.issuedByName = issuedByName; }

    public Long getReturnedBy() { return returnedBy; }
    public void setReturnedBy(Long returnedBy) { this.returnedBy = returnedBy; }

    public String getReturnedByName() { return returnedByName; }
    public void setReturnedByName(String returnedByName) { this.returnedByName = returnedByName; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public long getOverdueDays() { return overdueDays; }
    public void setOverdueDays(long overdueDays) { this.overdueDays = overdueDays; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public String getFineStatus() { return fineStatus; }
    public void setFineStatus(String fineStatus) { this.fineStatus = fineStatus; }
}
