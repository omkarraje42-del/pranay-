package com.library.service;

import com.library.dao.AuditLogDAO;
import com.library.dao.LoanDAO;
import com.library.dao.MemberDAO;
import com.library.dto.PaginatedResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.AuditLog;
import com.library.model.Member;
import com.library.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MemberService {

    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);
    private final MemberDAO memberDAO = new MemberDAO();
    private final LoanDAO loanDAO = new LoanDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public PaginatedResponse<Member> getMembers(String search, String status, int page, int pageSize) {
        int limit = pageSize > 0 ? pageSize : 10;
        int currentPage = page > 0 ? page : 1;
        int offset = (currentPage - 1) * limit;

        long total = memberDAO.count(search, status);
        List<Member> list = memberDAO.findAll(search, status, offset, limit);
        return new PaginatedResponse<>(list, total, currentPage, limit);
    }

    public Member getMemberById(long id) {
        Member m = memberDAO.findById(id);
        if (m == null) {
            throw new ResourceNotFoundException("Member with ID " + id + " not found");
        }
        return m;
    }

    public Member getMemberByUserId(long userId) {
        return memberDAO.findByUserId(userId);
    }

    public Member registerMember(Member member, Long staffUserId, String ipAddress) {
        validateMember(member, true);

        // Auto-generate membership number if missing
        if (!ValidationUtil.isNotEmpty(member.getMembershipNumber())) {
            int year = LocalDate.now().getYear();
            long count = memberDAO.count(null, null) + 1;
            String generated = String.format("LIB-%d-%04d", year, count);
            while (memberDAO.findByMembershipNumber(generated) != null) {
                count++;
                generated = String.format("LIB-%d-%04d", year, count);
            }
            member.setMembershipNumber(generated);
        } else if (memberDAO.findByMembershipNumber(member.getMembershipNumber().trim()) != null) {
            throw new ValidationException("Membership number '" + member.getMembershipNumber() + "' is already in use");
        }

        if (member.getMembershipStartDate() == null) {
            member.setMembershipStartDate(LocalDate.now());
        }
        if (member.getMembershipExpiryDate() == null) {
            member.setMembershipExpiryDate(member.getMembershipStartDate().plusYears(1));
        }

        Member created = memberDAO.create(member);
        auditLogDAO.log(new AuditLog(staffUserId, "REGISTER_MEMBER", "MEMBER", created.getId(),
                "Registered member " + created.getFullName() + " (" + created.getMembershipNumber() + ")", ipAddress));
        return created;
    }

    public Member updateMember(long id, Member member, Long staffUserId, String ipAddress) {
        Member existing = memberDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Member with ID " + id + " not found");
        }

        member.setId(id);
        validateMember(member, false);

        // Check email uniqueness if changed
        if (!member.getEmail().equalsIgnoreCase(existing.getEmail())) {
            Member duplicate = memberDAO.findByEmail(member.getEmail());
            if (duplicate != null && duplicate.getId() != id) {
                throw new ValidationException("Email '" + member.getEmail() + "' is already used by another member");
            }
        }

        memberDAO.update(member);
        auditLogDAO.log(new AuditLog(staffUserId, "UPDATE_MEMBER", "MEMBER", id,
                "Updated details for member " + member.getFullName(), ipAddress));
        return getMemberById(id);
    }

    public void updateStatus(long id, String newStatus, Long staffUserId, String ipAddress) {
        Member m = memberDAO.findById(id);
        if (m == null) {
            throw new ResourceNotFoundException("Member with ID " + id + " not found");
        }

        String target = newStatus.trim().toUpperCase();
        if ("INACTIVE".equals(target) || "SUSPENDED".equals(target)) {
            if (m.getActiveLoansCount() > 0) {
                throw new BusinessRuleException("Cannot deactivate or suspend member while they have " +
                        m.getActiveLoansCount() + " active borrowed book(s)");
            }
        }

        memberDAO.updateStatus(id, target);
        auditLogDAO.log(new AuditLog(staffUserId, "UPDATE_MEMBER_STATUS", "MEMBER", id,
                "Changed member " + m.getMembershipNumber() + " status to " + target, ipAddress));
    }

    private void validateMember(Member member, boolean isNew) {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isNotEmpty(member.getFirstName())) {
            errors.add("First name is required");
        }
        if (!ValidationUtil.isNotEmpty(member.getLastName())) {
            errors.add("Last name is required");
        }
        if (!ValidationUtil.isNotEmpty(member.getEmail())) {
            errors.add("Email address is required");
        } else if (!ValidationUtil.isValidEmail(member.getEmail())) {
            errors.add("Invalid email format");
        } else if (isNew && memberDAO.findByEmail(member.getEmail().trim()) != null) {
            errors.add("A member with email '" + member.getEmail() + "' already exists");
        }

        if (member.getMembershipExpiryDate() != null && member.getMembershipStartDate() != null) {
            if (member.getMembershipExpiryDate().isBefore(member.getMembershipStartDate())) {
                errors.add("Membership expiry date cannot precede start date");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
