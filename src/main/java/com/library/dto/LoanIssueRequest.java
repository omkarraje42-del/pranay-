package com.library.dto;

public class LoanIssueRequest {
    private long memberId;
    private Long copyId;
    private Long bookCopyId;
    private String accessionNumber;
    private Integer loanDays;
    private Integer loanDurationDays;
    private String notes;

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public Long getCopyId() {
        return copyId != null ? copyId : bookCopyId;
    }
    public void setCopyId(Long copyId) { this.copyId = copyId; }

    public Long getBookCopyId() { return copyId != null ? copyId : bookCopyId; }
    public void setBookCopyId(Long bookCopyId) { this.bookCopyId = bookCopyId; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public Integer getLoanDays() {
        return loanDays != null ? loanDays : loanDurationDays;
    }
    public void setLoanDays(Integer loanDays) { this.loanDays = loanDays; }

    public Integer getLoanDurationDays() { return loanDays != null ? loanDays : loanDurationDays; }
    public void setLoanDurationDays(Integer loanDurationDays) { this.loanDurationDays = loanDurationDays; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
