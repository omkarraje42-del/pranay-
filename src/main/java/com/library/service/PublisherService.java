package com.library.service;

import com.library.dao.PublisherDAO;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Publisher;
import com.library.util.ValidationUtil;

import java.util.List;

public class PublisherService {

    private final PublisherDAO publisherDAO = new PublisherDAO();

    public List<Publisher> getPublishers(String search) {
        return publisherDAO.findAll(search);
    }

    public Publisher getPublisherById(int id) {
        Publisher p = publisherDAO.findById(id);
        if (p == null) {
            throw new ResourceNotFoundException("Publisher not found");
        }
        return p;
    }

    public Publisher createPublisher(Publisher publisher) {
        if (!ValidationUtil.isNotEmpty(publisher.getName())) {
            throw new ValidationException("Publisher name is required");
        }
        if (publisherDAO.findByName(publisher.getName().trim()) != null) {
            throw new ValidationException("Publisher with name '" + publisher.getName() + "' already exists");
        }
        return publisherDAO.create(publisher);
    }

    public Publisher updatePublisher(int id, Publisher publisher) {
        getPublisherById(id);
        if (!ValidationUtil.isNotEmpty(publisher.getName())) {
            throw new ValidationException("Publisher name is required");
        }
        Publisher duplicate = publisherDAO.findByName(publisher.getName().trim());
        if (duplicate != null && duplicate.getId() != id) {
            throw new ValidationException("Another publisher with name '" + publisher.getName() + "' already exists");
        }
        publisher.setId(id);
        publisherDAO.update(publisher);
        return getPublisherById(id);
    }

    public void deletePublisher(int id) {
        getPublisherById(id);
        publisherDAO.delete(id);
    }
}
