package com.library.service;

import com.library.dao.AuditLogDAO;
import com.library.dao.FineDAO;
import com.library.dao.LoanDAO;
import com.library.model.AuditLog;
import com.library.model.Fine;
import com.library.model.Loan;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {

    private final LoanDAO loanDAO = new LoanDAO();
    private final FineDAO fineDAO = new FineDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public Map<String, Object> getLoansReport(String status, LocalDate fromDate, LocalDate toDate) {
        List<Loan> loans = loanDAO.findAll(null, null, null, status, fromDate, toDate, 0, 1000);
        Map<String, Object> report = new HashMap<>();
        report.put("records", loans);
        report.put("totalRecords", loans.size());
        report.put("generatedAt", LocalDate.now().toString());
        return report;
    }

    public Map<String, Object> getOverdueReport() {
        List<Loan> overdues = loanDAO.findOverdueLoans();
        Map<String, Object> report = new HashMap<>();
        report.put("records", overdues);
        report.put("totalOverdue", overdues.size());
        report.put("generatedAt", LocalDate.now().toString());
        return report;
    }

    public Map<String, Object> getFinesReport(String status) {
        List<Fine> fines = fineDAO.findAll(null, status, 0, 1000);
        Map<String, Object> report = new HashMap<>();
        report.put("records", fines);
        report.put("totalRecords", fines.size());
        report.put("generatedAt", LocalDate.now().toString());
        return report;
    }

    public List<AuditLog> getAuditLogs(int limit) {
        return auditLogDAO.findRecent(limit > 0 ? limit : 50);
    }
}
