package com.library.dto;

public class ReservationRequest {
    private long bookId;
    private Long memberId;
    private String notes;

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
