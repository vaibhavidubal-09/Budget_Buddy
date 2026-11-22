package com.budgetbuddy.api;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException exception,
                                                          HttpServletRequest request) {
        return createError(HttpStatus.CONFLICT, "Conflict",
                "This record changed while you were editing it. Refresh and try again.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatusException(ResponseStatusException exception,
                                                          HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        String error = HttpStatus.resolve(status.value()) == null
                ? "Request failed"
                : HttpStatus.valueOf(status.value()).getReasonPhrase();
        String message = exception.getReason() == null ? error : exception.getReason();
        return createError(status, error, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException exception,
                                                               HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst()
                .orElse("Please check the submitted values");
        return createError(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                          HttpServletRequest request) {
        return createError(HttpStatus.BAD_REQUEST, "Bad Request",
                "Request body is missing or has invalid JSON or date values", request);
    }

    @ExceptionHandler({TypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> handleInvalidRequestParameter(Exception exception,
                                                                   HttpServletRequest request) {
        return createError(HttpStatus.BAD_REQUEST, "Bad Request",
                "One or more request parameters are missing or invalid", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return createError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "The request could not be completed", request);
    }

    private ResponseEntity<ApiError> createError(HttpStatusCode status, String error,
                                                  String message, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
