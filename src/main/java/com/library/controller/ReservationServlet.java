package com.library.controller;

import com.library.dto.PaginatedResponse;
import com.library.dto.ReservationRequest;
import com.library.model.Reservation;
import com.library.model.User;
import com.library.service.ReservationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "ReservationServlet", urlPatterns = "/api/reservations/*")
public class ReservationServlet extends BaseServlet {

    private final ReservationService reservationService = new ReservationService();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String memParam = req.getParameter("memberId");
            Long memberId = (memParam != null && !memParam.isEmpty()) ? Long.parseLong(memParam) : null;
            String bookParam = req.getParameter("bookId");
            Long bookId = (bookParam != null && !bookParam.isEmpty()) ? Long.parseLong(bookParam) : null;
            String status = req.getParameter("status");

            int page = parseIntParam(req.getParameter("page"), 1);
            int pageSize = parseIntParam(req.getParameter("pageSize"), 10);

            PaginatedResponse<Reservation> result = reservationService.getReservations(memberId, bookId, status, page, pageSize);
            sendSuccess(resp, "Reservations retrieved", result);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User currentUser = getAuthenticatedUser(req);
        Long staffUserId = (currentUser != null) ? currentUser.getId() : null;
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 3 && "cancel".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    reservationService.cancelReservation(id, staffUserId, getClientIp(req));
                    sendSuccess(resp, "Reservation cancelled successfully", null);
                    return;
                }
            }

            ReservationRequest resReq = parseJson(req, ReservationRequest.class);
            if (resReq == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing reservation request body");
                return;
            }

            long targetMemberId;
            if (resReq.getMemberId() != null) {
                targetMemberId = resReq.getMemberId();
            } else if (currentUser != null) {
                targetMemberId = currentUser.getId();
            } else {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Member ID is required");
                return;
            }

            Reservation created = reservationService.placeReservation(resReq.getBookId(), targetMemberId, resReq.getNotes(), staffUserId, getClientIp(req));
            sendCreated(resp, "Reservation placed successfully", created);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long staffUserId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    reservationService.cancelReservation(id, staffUserId, getClientIp(req));
                    sendSuccess(resp, "Reservation cancelled successfully", null);
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
        Long staffUserId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 3 && "cancel".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    reservationService.cancelReservation(id, staffUserId, getClientIp(req));
                    sendSuccess(resp, "Reservation cancelled successfully", null);
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
