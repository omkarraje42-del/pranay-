package com.library.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Fine {
    private long id;
    private long loanId;
    private long memberId;
    private String memberName;
    private String membershipNumber;
    private String bookTitle;
    private String accessionNumber;
    private BigDecimal amount = BigDecimal.ZERO;
    private String reason = "Overdue return";
    private String status = "UNPAID"; // UNPAID, PAID, WAIVED
    private OffsetDateTime createdAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime waivedAt;
    private Long waivedBy;
    private String waivedByName;
    private String waiverReason;
    private String paymentMethod;
    private String receiptNumber;

    public Fine() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getLoanId() { return loanId; }
    public void setLoanId(long loanId) { this.loanId = loanId; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }

    public OffsetDateTime getWaivedAt() { return waivedAt; }
    public void setWaivedAt(OffsetDateTime waivedAt) { this.waivedAt = waivedAt; }

    public Long getWaivedBy() { return waivedBy; }
    public void setWaivedBy(Long waivedBy) { this.waivedBy = waivedBy; }

    public String getWaivedByName() { return waivedByName; }
    public void setWaivedByName(String waivedByName) { this.waivedByName = waivedByName; }

    public String getWaiverReason() { return waiverReason; }
    public void setWaiverReason(String waiverReason) { this.waiverReason = waiverReason; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
}
