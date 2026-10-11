package com.library.controller;

import com.library.dto.FinePaymentRequest;
import com.library.dto.FineWaiveRequest;
import com.library.dto.PaginatedResponse;
import com.library.model.Fine;
import com.library.model.User;
import com.library.service.FineService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "FineServlet", urlPatterns = "/api/fines/*")
public class FineServlet extends BaseServlet {

    private final FineService fineService = new FineService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String memParam = req.getParameter("memberId");
                Long memberId = (memParam != null && !memParam.isEmpty()) ? Long.parseLong(memParam) : null;
                String status = req.getParameter("status");

                int page = parseIntParam(req.getParameter("page"), 1);
                int pageSize = parseIntParam(req.getParameter("pageSize"), 10);

                PaginatedResponse<Fine> result = fineService.getFines(memberId, status, page, pageSize);
                sendSuccess(resp, "Fines retrieved", result);

            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    Fine fine = fineService.getFineById(id);
                    sendSuccess(resp, "Fine retrieved", fine);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
                }
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long staffUserId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                // POST /api/fines/{id}/pay
                if (parts.length == 3 && "pay".equals(parts[2])) {
                    long fineId = Long.parseLong(parts[1]);
                    FinePaymentRequest payReq = parseJson(req, FinePaymentRequest.class);
                    String method = (payReq != null) ? payReq.getPaymentMethod() : "CASH";
                    String receipt = (payReq != null) ? payReq.getReceiptNumber() : null;

                    Fine paid = fineService.payFine(fineId, method, receipt, staffUserId, getClientIp(req));
                    sendSuccess(resp, "Fine payment recorded successfully", paid);
                    return;

                // POST /api/fines/{id}/waive
                } else if (parts.length == 3 && "waive".equals(parts[2])) {
                    long fineId = Long.parseLong(parts[1]);
                    FineWaiveRequest waiveReq = parseJson(req, FineWaiveRequest.class);
                    String reason = (waiveReq != null) ? waiveReq.getReason() : null;

                    Fine waived = fineService.waiveFine(fineId, reason, staffUserId, getClientIp(req));
                    sendSuccess(resp, "Fine waived successfully", waived);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
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
