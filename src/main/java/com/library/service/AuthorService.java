package com.library.service;

import com.library.dao.AuthorDAO;
import com.library.dto.PaginatedResponse;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Author;
import com.library.util.ValidationUtil;

import java.util.List;

public class AuthorService {

    private final AuthorDAO authorDAO = new AuthorDAO();

    public PaginatedResponse<Author> getAuthors(String search, int page, int pageSize) {
        int limit = pageSize > 0 ? pageSize : 20;
        int currentPage = page > 0 ? page : 1;
        int offset = (currentPage - 1) * limit;

        long total = authorDAO.count(search);
        List<Author> list = authorDAO.findAll(search, offset, limit);
        return new PaginatedResponse<>(list, total, currentPage, limit);
    }

    public List<Author> getAllAuthors() {
        return authorDAO.findAll(null, 0, 500);
    }

    public Author getAuthorById(int id) {
        Author a = authorDAO.findById(id);
        if (a == null) {
            throw new ResourceNotFoundException("Author not found");
        }
        return a;
    }

    public Author createAuthor(Author author) {
        if (!ValidationUtil.isNotEmpty(author.getLastName())) {
            throw new ValidationException("Author last name is required");
        }
        return authorDAO.create(author);
    }

    public Author updateAuthor(int id, Author author) {
        getAuthorById(id);
        if (!ValidationUtil.isNotEmpty(author.getLastName())) {
            throw new ValidationException("Author last name is required");
        }
        author.setId(id);
        authorDAO.update(author);
        return getAuthorById(id);
    }

    public void deleteAuthor(int id) {
        getAuthorById(id);
        authorDAO.delete(id);
    }
}
