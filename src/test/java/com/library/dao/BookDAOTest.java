package com.library.dao;

import com.library.model.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookDAOTest {

    private static BookDAO bookDAO;

    @BeforeAll
    static void setUp() {
        bookDAO = new BookDAO();
    }

    @Test
    @DisplayName("Should find existing book by ID with category and author associations")
    void shouldFindBookById() {
        Book book = bookDAO.findById(1);

        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getSubtitle()).isEqualTo("A Handbook of Agile Software Craftsmanship");
        assertThat(book.getCategoryName()).isEqualTo("Computer Science & Software Engineering");
        assertThat(book.getAuthors()).isNotEmpty();
        assertThat(book.getTotalCopies()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should find book by ISBN (both hyphenated and non-hyphenated)")
    void shouldFindBookByIsbn() {
        Book bookWithHyphen = bookDAO.findByIsbn("978-0132350884");
        assertThat(bookWithHyphen).isNotNull();
        assertThat(bookWithHyphen.getId()).isEqualTo(1L);

        Book bookWithoutHyphen = bookDAO.findByIsbn("9780132350884");
        assertThat(bookWithoutHyphen).isNotNull();
        assertThat(bookWithoutHyphen.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return null for non-existent book ID or ISBN")
    void shouldReturnNullForMissingBook() {
        assertThat(bookDAO.findById(99999)).isNull();
        assertThat(bookDAO.findByIsbn("0000000000")).isNull();
    }

    @Test
    @DisplayName("Should retrieve paginated list and count of books")
    void shouldRetrievePaginatedBooks() {
        long total = bookDAO.count(null, null, null, null, true);
        assertThat(total).isGreaterThanOrEqualTo(7);

        List<Book> books = bookDAO.findAll(null, null, null, null, true, "b.title", "ASC", 0, 5);
        assertThat(books).hasSizeLessThanOrEqualTo(5);
        assertThat(books).isNotEmpty();
    }

    @Test
    @DisplayName("Should filter books by search query across titles and authors")
    void shouldFilterBooksBySearch() {
        List<Book> results = bookDAO.findAll("Effective Java", null, null, null, true, "b.title", "ASC", 0, 10);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getTitle()).isEqualTo("Effective Java");
    }
}
