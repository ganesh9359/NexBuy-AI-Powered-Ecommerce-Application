package com.nexbuy.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Exception Handling Tests")
class ExceptionHandlingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("CustomException stores message and status correctly")
    void customException_storesFields() {
        CustomException ex = new CustomException("Not found", HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Not found");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("CustomException is a RuntimeException")
    void customException_isRuntime() {
        assertThat(new CustomException("err", HttpStatus.BAD_REQUEST))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("GlobalExceptionHandler returns correct HTTP status for CustomException")
    void globalHandler_customException() {
        CustomException ex = new CustomException("Product not found", HttpStatus.NOT_FOUND);
        ResponseEntity<ErrorResponse> response = handler.handleCustom(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found");
    }

    @Test
    @DisplayName("GlobalExceptionHandler returns 500 for generic exceptions")
    void globalHandler_genericException() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
    }

    @Test
    @DisplayName("CustomException with null status defaults to BAD_REQUEST in handler")
    void globalHandler_nullStatus_defaultsBadRequest() {
        CustomException ex = new CustomException("bad", null);
        ResponseEntity<ErrorResponse> response = handler.handleCustom(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("All common HTTP statuses are stored correctly")
    void customException_allStatuses() {
        for (HttpStatus status : new HttpStatus[]{
                HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND, HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR}) {
            assertThat(new CustomException("msg", status).getStatus()).isEqualTo(status);
        }
    }
}
