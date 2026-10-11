package com.library.service;

import com.library.dao.DashboardDAO;
import com.library.dao.LoanDAO;
import com.library.model.Loan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final LoanDAO loanDAO = new LoanDAO();

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("stats", dashboardDAO.getSummaryStats());
        data.put("chart", dashboardDAO.getBorrowingActivityChartData());
        data.put("categories", dashboardDAO.getCategoryDistribution());
        data.put("popularBooks", dashboardDAO.getPopularBooks(5));
        data.put("recentLoans", loanDAO.findAll(null, null, null, null, null, null, 0, 5));
        return data;
    }
}
