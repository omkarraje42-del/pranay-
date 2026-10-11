package com.library.controller;

import com.library.model.AuditLog;
import com.library.service.ReportService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ReportServlet", urlPatterns = "/api/reports/*")
public class ReportServlet extends BaseServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if ("/loans".equals(pathInfo)) {
                String status = req.getParameter("status");
                LocalDate fromDate = parseDateParam(req.getParameter("fromDate"));
                LocalDate toDate = parseDateParam(req.getParameter("toDate"));
                Map<String, Object> data = reportService.getLoansReport(status, fromDate, toDate);
                sendSuccess(resp, "Loans report generated", data);

            } else if ("/overdue".equals(pathInfo)) {
                Map<String, Object> data = reportService.getOverdueReport();
                sendSuccess(resp, "Overdue report generated", data);

            } else if ("/fines".equals(pathInfo)) {
                String status = req.getParameter("status");
                Map<String, Object> data = reportService.getFinesReport(status);
                sendSuccess(resp, "Fines report generated", data);

            } else if ("/audit-logs".equals(pathInfo)) {
                int limit = parseIntParam(req.getParameter("limit"), 50);
                List<AuditLog> logs = reportService.getAuditLogs(limit);
                sendSuccess(resp, "Audit logs retrieved", logs);

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Report not found");
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private LocalDate parseDateParam(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseIntParam(String val, int def) {
        if (val == null || val.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
