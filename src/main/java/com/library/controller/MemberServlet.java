package com.library.controller;

import com.library.dto.MemberRequest;
import com.library.dto.PaginatedResponse;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.User;
import com.library.service.LoanService;
import com.library.service.MemberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet(name = "MemberServlet", urlPatterns = "/api/members/*")
public class MemberServlet extends BaseServlet {

    private final MemberService memberService = new MemberService();
    private final LoanService loanService = new LoanService();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Support PATCH
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String search = req.getParameter("search");
                String status = req.getParameter("status");
                int page = parseIntParam(req.getParameter("page"), 1);
                int pageSize = parseIntParam(req.getParameter("pageSize"), 10);

                PaginatedResponse<Member> result = memberService.getMembers(search, status, page, pageSize);
                sendSuccess(resp, "Members retrieved", result);

            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    Member member = memberService.getMemberById(id);
                    sendSuccess(resp, "Member retrieved", member);
                } else if (parts.length == 3 && "loans".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    PaginatedResponse<Loan> loans = loanService.getLoans(null, id, null, null, null, null, 1, 100);
                    sendSuccess(resp, "Member loans retrieved", loans);
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
        User currentUser = getAuthenticatedUser(req);
        Long userId = (currentUser != null) ? currentUser.getId() : null;

        try {
            MemberRequest memberReq = parseJson(req, MemberRequest.class);
            if (memberReq == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing member details");
                return;
            }
            Member m = mapToMember(memberReq);
            Member created = memberService.registerMember(m, userId, getClientIp(req));
            sendCreated(resp, "Member registered successfully", created);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long userId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    MemberRequest memberReq = parseJson(req, MemberRequest.class);
                    if (memberReq == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing member details");
                        return;
                    }
                    Member m = mapToMember(memberReq);
                    Member updated = memberService.updateMember(id, m, userId, getClientIp(req));
                    sendSuccess(resp, "Member updated successfully", updated);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long userId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                // PATCH /api/members/{id}/status
                if (parts.length == 3 && "status".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    @SuppressWarnings("unchecked")
                    Map<String, String> body = parseJson(req, Map.class);
                    String newStatus = (body != null) ? body.get("status") : null;
                    if (newStatus == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "New status must be provided");
                        return;
                    }
                    memberService.updateStatus(id, newStatus, userId, getClientIp(req));
                    sendSuccess(resp, "Member status updated to " + newStatus, null);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private Member mapToMember(MemberRequest req) {
        Member m = new Member();
        m.setMembershipNumber(req.getMembershipNumber());
        m.setFirstName(req.getFirstName());
        m.setLastName(req.getLastName());
        m.setEmail(req.getEmail());
        m.setPhone(req.getPhone());
        m.setAddress(req.getAddress());
        m.setMembershipStartDate(req.getMembershipStartDate());
        m.setMembershipExpiryDate(req.getMembershipExpiryDate());
        m.setStatus(req.getStatus());
        return m;
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
