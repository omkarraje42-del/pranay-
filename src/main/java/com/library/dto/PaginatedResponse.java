package com.library.dto;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public PaginatedResponse() {}

    public PaginatedResponse(List<T> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 1;
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
