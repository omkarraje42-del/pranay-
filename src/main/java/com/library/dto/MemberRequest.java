package com.library.dto;

import java.time.LocalDate;

public class MemberRequest {
    private String membershipNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private LocalDate membershipStartDate;
    private LocalDate membershipExpiryDate;
    private String status;

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

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
}
