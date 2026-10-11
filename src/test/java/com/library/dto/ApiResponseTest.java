package com.library.dto;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final Gson gson = new Gson();

    @Test
    @DisplayName("Should construct and serialize success ApiResponse properly")
    void shouldSerializeSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("Book retrieved", "Clean Code");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Book retrieved");
        assertThat(response.getData()).isEqualTo("Clean Code");

        String json = gson.toJson(response);
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"message\":\"Book retrieved\"");
        assertThat(json).contains("\"data\":\"Clean Code\"");
    }

    @Test
    @DisplayName("Should construct and serialize error ApiResponse properly")
    void shouldSerializeErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("Resource not found");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Resource not found");
        assertThat(response.getData()).isNull();

        String json = gson.toJson(response);
        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"message\":\"Resource not found\"");
    }

    @Test
    @DisplayName("Should correctly calculate pagination metadata in PaginatedResponse")
    void shouldCalculatePaginationCorrectly() {
        List<String> items = List.of("Item1", "Item2", "Item3");
        PaginatedResponse<String> paginated = new PaginatedResponse<>(items, 25, 2, 10);

        assertThat(paginated.getItems()).hasSize(3);
        assertThat(paginated.getTotal()).isEqualTo(25);
        assertThat(paginated.getPage()).isEqualTo(2);
        assertThat(paginated.getPageSize()).isEqualTo(10);
        assertThat(paginated.getTotalPages()).isEqualTo(3); // ceil(25 / 10) = 3
    }
}
