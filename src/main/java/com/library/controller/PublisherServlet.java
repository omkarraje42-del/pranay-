package com.library.controller;

import com.library.model.Publisher;
import com.library.service.PublisherService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "PublisherServlet", urlPatterns = "/api/publishers/*")
public class PublisherServlet extends BaseServlet {

    private final PublisherService publisherService = new PublisherService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                String search = req.getParameter("search");
                List<Publisher> list = publisherService.getPublishers(search);
                sendSuccess(resp, "Publishers retrieved", list);
            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[1]);
                    Publisher p = publisherService.getPublisherById(id);
                    sendSuccess(resp, "Publisher retrieved", p);
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
            Publisher p = parseJson(req, Publisher.class);
            if (p == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body");
                return;
            }
            Publisher created = publisherService.createPublisher(p);
            sendCreated(resp, "Publisher created successfully", created);
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
                    Publisher p = parseJson(req, Publisher.class);
                    if (p == null) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing update body");
                        return;
                    }
                    Publisher updated = publisherService.updatePublisher(id, p);
                    sendSuccess(resp, "Publisher updated successfully", updated);
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
                    publisherService.deletePublisher(id);
                    sendSuccess(resp, "Publisher deleted successfully", null);
                    return;
                }
            }
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
