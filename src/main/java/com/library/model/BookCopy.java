package com.library.model;

import java.time.OffsetDateTime;

public class BookCopy {
    private long id;
    private long bookId;
    private String bookTitle;
    private String isbn;
    private String accessionNumber;
    private String status = "AVAILABLE"; // AVAILABLE, BORROWED, RESERVED, DAMAGED, LOST, MAINTENANCE
    private String condition = "Good";
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public BookCopy() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
