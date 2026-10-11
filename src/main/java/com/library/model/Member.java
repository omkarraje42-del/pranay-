package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class Member {
    private long id;
    private Long userId;
    private String membershipNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private LocalDate membershipStartDate;
    private LocalDate membershipExpiryDate;
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED, EXPIRED
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Derived / Aggregated stats
    private int activeLoansCount;
    private BigDecimal unpaidFinesTotal = BigDecimal.ZERO;

    public Member() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getMembershipStartDate() { return membershipStartDate; }
    public void setMembershipStartDate(LocalDate membershipStartDate) { this.membershipStartDate = membershipStartDate; }

    public LocalDate getMembershipExpiryDate() { return membershipExpiryDate; }
    public void setMembershipExpiryDate(LocalDate membershipExpiryDate) { this.membershipExpiryDate = membershipExpiryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getActiveLoansCount() { return activeLoansCount; }
    public void setActiveLoansCount(int activeLoansCount) { this.activeLoansCount = activeLoansCount; }

    public BigDecimal getUnpaidFinesTotal() { return unpaidFinesTotal; }
    public void setUnpaidFinesTotal(BigDecimal unpaidFinesTotal) { this.unpaidFinesTotal = unpaidFinesTotal; }
}
