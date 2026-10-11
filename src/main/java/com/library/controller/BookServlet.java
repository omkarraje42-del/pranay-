package com.library.controller;

import com.library.dto.BookRequest;
import com.library.dto.CopyRequest;
import com.library.dto.PaginatedResponse;
import com.library.model.Book;
import com.library.model.BookCopy;
import com.library.model.User;
import com.library.service.BookService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "BookServlet", urlPatterns = "/api/books/*")
public class BookServlet extends BaseServlet {

    private final BookService bookService = new BookService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // List books
                String search = req.getParameter("search");
                String catParam = req.getParameter("categoryId");
                Integer categoryId = (catParam != null && !catParam.isEmpty()) ? Integer.parseInt(catParam) : null;
                String authParam = req.getParameter("authorId");
                Integer authorId = (authParam != null && !authParam.isEmpty()) ? Integer.parseInt(authParam) : null;
                String availability = req.getParameter("availability");
                String activeParam = req.getParameter("activeOnly");
                Boolean activeOnly = (activeParam != null) ? Boolean.parseBoolean(activeParam) : true;
                String sortField = req.getParameter("sortBy");
                String sortOrder = req.getParameter("sortOrder");

                int page = parseIntParam(req.getParameter("page"), 1);
                int pageSize = parseIntParam(req.getParameter("pageSize"), 10);

                PaginatedResponse<Book> result = bookService.getBooks(
                        search, categoryId, authorId, availability, activeOnly, sortField, sortOrder, page, pageSize
                );
                sendSuccess(resp, "Books retrieved", result);

            } else {
                String[] parts = pathInfo.split("/");
                // /api/books/{id} or /api/books/{id}/copies
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    Book book = bookService.getBookById(id);
                    sendSuccess(resp, "Book retrieved", book);
                } else if (parts.length == 3 && "copies".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    List<BookCopy> copies = bookService.getBookCopies(id);
                    sendSuccess(resp, "Book copies retrieved", copies);
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
        Long userId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Create book
                BookRequest bookReq = parseJson(req, BookRequest.class);
                if (bookReq == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                    return;
                }
                Book book = mapToBook(bookReq);
                Book created = bookService.createBook(book, bookReq.getAuthorIds(), bookReq.getInitialCopies(), userId, getClientIp(req));
                sendCreated(resp, "Book created successfully", created);

            } else {
                String[] parts = pathInfo.split("/");
                // /api/books/{id}/copies
                if (parts.length == 3 && "copies".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    CopyRequest copyReq = parseJson(req, CopyRequest.class);
                    if (copyReq == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing copy details");
                        return;
                    }
                    BookCopy copy = bookService.addCopy(id, copyReq.getAccessionNumber(), copyReq.getCondition(), userId, getClientIp(req));
                    sendCreated(resp, "Copy added successfully", copy);

                } else if (parts.length == 3 && "activate".equals(parts[2])) {
                    long id = Long.parseLong(parts[1]);
                    bookService.activateBook(id, userId, getClientIp(req));
                    sendSuccess(resp, "Book reactivated successfully", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
                }
            }
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
                // PUT /api/books/{id}
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    BookRequest bookReq = parseJson(req, BookRequest.class);
                    if (bookReq == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing update body");
                        return;
                    }
                    Book book = mapToBook(bookReq);
                    Book updated = bookService.updateBook(id, book, bookReq.getAuthorIds(), userId, getClientIp(req));
                    sendSuccess(resp, "Book updated successfully", updated);

                } else if (parts.length == 3 && "copies".equals(parts[1])) {
                    // PUT /api/books/copies/{copyId}
                    long copyId = Long.parseLong(parts[2]);
                    CopyRequest copyReq = parseJson(req, CopyRequest.class);
                    if (copyReq == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing update body");
                        return;
                    }
                    bookService.updateCopyStatus(copyId, copyReq.getStatus(), copyReq.getCondition(), userId, getClientIp(req));
                    sendSuccess(resp, "Copy status updated successfully", null);
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
                }
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User currentUser = getAuthenticatedUser(req);
        Long userId = (currentUser != null) ? currentUser.getId() : null;

        try {
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    bookService.deactivateBook(id, userId, getClientIp(req));
                    sendSuccess(resp, "Book deactivated successfully", null);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private Book mapToBook(BookRequest req) {
        Book b = new Book();
        b.setIsbn(req.getIsbn());
        b.setTitle(req.getTitle());
        b.setSubtitle(req.getSubtitle());
        b.setCategoryId(req.getCategoryId());
        b.setPublisherId(req.getPublisherId());
        b.setPublicationYear(req.getPublicationYear());
        b.setEdition(req.getEdition());
        b.setLanguage(req.getLanguage());
        b.setDescription(req.getDescription());
        b.setShelfLocation(req.getShelfLocation());
        b.setCoverImageUrl(req.getCoverImageUrl());
        b.setActive(req.isActive());
        return b;
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
