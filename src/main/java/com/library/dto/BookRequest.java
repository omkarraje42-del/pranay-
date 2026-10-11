package com.library.dto;

import java.util.List;

public class BookRequest {
    private String isbn;
    private String title;
    private String subtitle;
    private int categoryId;
    private Integer publisherId;
    private Integer publicationYear;
    private String edition;
    private String language;
    private String description;
    private String shelfLocation;
    private String coverImageUrl;
    private boolean active = true;
    private List<Integer> authorIds;
    private int initialCopies = 1;

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public Integer getPublisherId() { return publisherId; }
    public void setPublisherId(Integer publisherId) { this.publisherId = publisherId; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<Integer> getAuthorIds() { return authorIds; }
    public void setAuthorIds(List<Integer> authorIds) { this.authorIds = authorIds; }

    public int getInitialCopies() { return initialCopies; }
    public void setInitialCopies(int initialCopies) { this.initialCopies = initialCopies; }
}
