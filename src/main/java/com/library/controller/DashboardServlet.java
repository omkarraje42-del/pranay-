package com.library.controller;

import com.library.service.DashboardService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet(name = "DashboardServlet", urlPatterns = "/api/dashboard/*")
public class DashboardServlet extends BaseServlet {

    private final DashboardService dashboardService = new DashboardService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> data = dashboardService.getDashboardData();
            sendSuccess(resp, "Dashboard data loaded", data);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
