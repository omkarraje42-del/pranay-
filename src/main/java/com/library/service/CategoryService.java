package com.library.service;

import com.library.dao.CategoryDAO;
import com.library.exception.BusinessRuleException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Category;
import com.library.util.ValidationUtil;

import java.util.List;

public class CategoryService {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Category> getCategories(String search) {
        return categoryDAO.findAll(search);
    }

    public Category getCategoryById(int id) {
        Category c = categoryDAO.findById(id);
        if (c == null) {
            throw new ResourceNotFoundException("Category not found");
        }
        return c;
    }

    public Category createCategory(Category category) {
        if (!ValidationUtil.isNotEmpty(category.getName())) {
            throw new ValidationException("Category name is required");
        }
        if (categoryDAO.findByName(category.getName().trim()) != null) {
            throw new ValidationException("Category with name '" + category.getName() + "' already exists");
        }
        return categoryDAO.create(category);
    }

    public Category updateCategory(int id, Category category) {
        Category existing = getCategoryById(id);
        if (!ValidationUtil.isNotEmpty(category.getName())) {
            throw new ValidationException("Category name is required");
        }
        Category duplicate = categoryDAO.findByName(category.getName().trim());
        if (duplicate != null && duplicate.getId() != id) {
            throw new ValidationException("Another category with name '" + category.getName() + "' already exists");
        }
        category.setId(id);
        categoryDAO.update(category);
        return getCategoryById(id);
    }

    public void deleteCategory(int id) {
        Category existing = getCategoryById(id);
        int bookCount = categoryDAO.countBooksInCategory(id);
        if (bookCount > 0) {
            throw new BusinessRuleException("Cannot delete category '" + existing.getName() + "' because " +
                    bookCount + " book(s) belong to it. Please reassign or delete the books first.");
        }
        categoryDAO.delete(id);
    }
}
