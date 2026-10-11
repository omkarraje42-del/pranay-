package com.library.controller;

import com.library.model.Category;
import com.library.service.CategoryService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "CategoryServlet", urlPatterns = "/api/categories/*")
public class CategoryServlet extends BaseServlet {

    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String search = req.getParameter("search");
                List<Category> list = categoryService.getCategories(search);
                sendSuccess(resp, "Categories retrieved", list);
            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[1]);
                    Category c = categoryService.getCategoryById(id);
                    sendSuccess(resp, "Category retrieved", c);
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
        try {
            Category cat = parseJson(req, Category.class);
            if (cat == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return;
            }
            Category created = categoryService.createCategory(cat);
            sendCreated(resp, "Category created successfully", created);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[1]);
                    Category cat = parseJson(req, Category.class);
                    if (cat == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing update body");
                        return;
                    }
                    Category updated = categoryService.updateCategory(id, cat);
                    sendSuccess(resp, "Category updated successfully", updated);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[1]);
                    categoryService.deleteCategory(id);
                    sendSuccess(resp, "Category deleted successfully", null);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
