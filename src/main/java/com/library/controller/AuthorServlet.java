package com.library.controller;

import com.library.dto.PaginatedResponse;
import com.library.model.Author;
import com.library.service.AuthorService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "AuthorServlet", urlPatterns = "/api/authors/*")
public class AuthorServlet extends BaseServlet {

    private final AuthorService authorService = new AuthorService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String all = req.getParameter("all");
                if ("true".equalsIgnoreCase(all)) {
                    List<Author> list = authorService.getAllAuthors();
                    sendSuccess(resp, "All authors retrieved", list);
                } else {
                    String search = req.getParameter("search");
                    int page = parseIntParam(req.getParameter("page"), 1);
                    int pageSize = parseIntParam(req.getParameter("pageSize"), 20);
                    PaginatedResponse<Author> result = authorService.getAuthors(search, page, pageSize);
                    sendSuccess(resp, "Authors retrieved", result);
                }
            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[1]);
                    Author a = authorService.getAuthorById(id);
                    sendSuccess(resp, "Author retrieved", a);
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
            Author author = parseJson(req, Author.class);
            if (author == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return;
            }
            Author created = authorService.createAuthor(author);
            sendCreated(resp, "Author created successfully", created);
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
                    Author author = parseJson(req, Author.class);
                    if (author == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing update body");
                        return;
                    }
                    Author updated = authorService.updateAuthor(id, author);
                    sendSuccess(resp, "Author updated successfully", updated);
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
                    authorService.deleteAuthor(id);
                    sendSuccess(resp, "Author deleted successfully", null);
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
