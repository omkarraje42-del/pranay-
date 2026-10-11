package com.library.controller;

import com.library.dto.LoanIssueRequest;
import com.library.dto.LoanReturnRequest;
import com.library.dto.PaginatedResponse;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.User;
import com.library.service.LoanService;
import com.library.service.MemberService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "LoanServlet", urlPatterns = "/api/loans/*")
public class LoanServlet extends BaseServlet {

    private final LoanService loanService = new LoanService();
    private final MemberService memberService = new MemberService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String search = req.getParameter("search");
                String memParam = req.getParameter("memberId");
                Long memberId = (memParam != null && !memParam.isEmpty()) ? Long.parseLong(memParam) : null;
                String bookParam = req.getParameter("bookId");
                Long bookId = (bookParam != null && !bookParam.isEmpty()) ? Long.parseLong(bookParam) : null;
                String status = req.getParameter("status");

                LocalDate fromDate = parseDateParam(req.getParameter("fromDate"));
                LocalDate toDate = parseDateParam(req.getParameter("toDate"));

                int page = parseIntParam(req.getParameter("page"), 1);
                int pageSize = parseIntParam(req.getParameter("pageSize"), 10);

                PaginatedResponse<Loan> result = loanService.getLoans(search, memberId, bookId, status, fromDate, toDate, page, pageSize);
                sendSuccess(resp, "Loans retrieved", result);

            } else if ("/overdue".equals(pathInfo)) {
                List<Loan> overdues = loanService.getOverdueLoans();
                sendSuccess(resp, "Overdue loans retrieved", overdues);

            } else if ("/my".equals(pathInfo)) {
                User currentUser = getAuthenticatedUser(req);
                if (currentUser == null) {
                    sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return;
                }
                Member member = memberService.getMemberByUserId(currentUser.getId());
                if (member == null) {
                    sendSuccess(resp, "Member loans", new ArrayList<>());
                    return;
                }
                List<Loan> memberLoans = loanService.getLoansByMember(member.getId());
                sendSuccess(resp, "Member loans", memberLoans);

            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    Loan loan = loanService.getLoanById(id);
                    sendSuccess(resp, "Loan retrieved", loan);
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
            if (pathInfo == null || pathInfo.equals("/") || "/issue".equals(pathInfo)) {
                LoanIssueRequest issueReq = parseJson(req, LoanIssueRequest.class);
                if (issueReq == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing issue request body");
                    return;
                }
                Loan loan = loanService.issueBook(
                        issueReq.getMemberId(),
                        issueReq.getCopyId(),
                        issueReq.getAccessionNumber(),
                        issueReq.getLoanDays(),
                        staffUserId,
                        issueReq.getNotes(),
                        getClientIp(req)
                );
                sendCreated(resp, "Book issued successfully", loan);

            } else if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                // POST /api/loans/{id}/return
                if (parts.length == 3 && "return".equals(parts[2])) {
                    long loanId = Long.parseLong(parts[1]);
                    LoanReturnRequest returnReq = parseJson(req, LoanReturnRequest.class);
                    String notes = (returnReq != null) ? returnReq.getNotes() : null;

                    Loan returned = loanService.returnBook(loanId, staffUserId, notes, getClientIp(req));
                    sendSuccess(resp, "Book return processed successfully", returned);
                    return;
                }
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
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
