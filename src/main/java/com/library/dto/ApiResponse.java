package com.library.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;

    public ApiResponse() {
        this.errors = new ArrayList<>();
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = new ArrayList<>();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> resp = new ApiResponse<>(false, message, null);
        resp.addError(message);
        return resp;
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        ApiResponse<T> resp = new ApiResponse<>(false, message, null);
        if (errors != null) {
            resp.setErrors(errors);
        }
        return resp;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
}
