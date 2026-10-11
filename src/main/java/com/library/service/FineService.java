package com.library.service;

import com.library.config.DatabaseConfig;
import com.library.dao.AuditLogDAO;
import com.library.dao.FineDAO;
import com.library.dto.PaginatedResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.AuditLog;
import com.library.model.Fine;
import com.library.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FineService {

    private static final Logger logger = LoggerFactory.getLogger(FineService.class);

    private final FineDAO fineDAO = new FineDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public PaginatedResponse<Fine> getFines(Long memberId, String status, int page, int pageSize) {
        int limit = pageSize > 0 ? pageSize : 10;
        int currentPage = page > 0 ? page : 1;
        int offset = (currentPage - 1) * limit;

        long total = fineDAO.count(memberId, status);
        List<Fine> list = fineDAO.findAll(memberId, status, offset, limit);
        return new PaginatedResponse<>(list, total, currentPage, limit);
    }

    public Fine getFineById(long id) {
        Fine fine = fineDAO.findById(id);
        if (fine == null) {
            throw new ResourceNotFoundException("Fine record not found");
        }
        return fine;
    }

    public Fine payFine(long fineId, String paymentMethod, String receiptNumber, Long staffUserId, String ipAddress) {
        Fine fine = getFineById(fineId);
        if (!"UNPAID".equalsIgnoreCase(fine.getStatus())) {
            throw new BusinessRuleException("Fine has already been " + fine.getStatus().toLowerCase());
        }

        String receipt = ValidationUtil.isNotEmpty(receiptNumber) ? receiptNumber.trim() : "RCPT-" + System.currentTimeMillis();
        String method = ValidationUtil.isNotEmpty(paymentMethod) ? paymentMethod.trim().toUpperCase() : "CASH";

        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean ok = fineDAO.recordPayment(fineId, method, receipt, conn);
            if (!ok) {
                throw new BusinessRuleException("Failed to record fine payment");
            }

            auditLogDAO.log(new AuditLog(staffUserId, "PAY_FINE", "FINE", fineId,
                    "Recorded payment of ₹" + fine.getAmount() + " via " + method + " (Receipt: " + receipt + ") for " + fine.getMemberName(), ipAddress));
            return getFineById(fineId);
        } catch (SQLException e) {
            logger.error("Error processing fine payment", e);
            throw new BusinessRuleException("Database failure: " + e.getMessage());
        }
    }

    public Fine waiveFine(long fineId, String waiverReason, Long staffUserId, String ipAddress) {
        Fine fine = getFineById(fineId);
        if (!"UNPAID".equalsIgnoreCase(fine.getStatus())) {
            throw new BusinessRuleException("Fine has already been " + fine.getStatus().toLowerCase());
        }

        if (!ValidationUtil.isNotEmpty(waiverReason)) {
            throw new ValidationException("A reason must be recorded when waiving a fine");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean ok = fineDAO.waiveFine(fineId, staffUserId, waiverReason.trim(), conn);
            if (!ok) {
                throw new BusinessRuleException("Failed to waive fine");
            }

            auditLogDAO.log(new AuditLog(staffUserId, "WAIVE_FINE", "FINE", fineId,
                    "Waived fine of ₹" + fine.getAmount() + " for member " + fine.getMemberName() + ". Reason: " + waiverReason, ipAddress));
            return getFineById(fineId);
        } catch (SQLException e) {
            logger.error("Error waiving fine", e);
            throw new BusinessRuleException("Database failure: " + e.getMessage());
        }
    }
}
