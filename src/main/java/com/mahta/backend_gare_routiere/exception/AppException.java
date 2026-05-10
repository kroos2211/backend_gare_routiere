package com.mahta.backend_gare_routiere.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(
            String message,
            HttpStatus status
    ) {

        super(message);
        this.status = status;
    }

    public static AppException notFound(
            String entity,
            Object id
    ) {

        return new AppException(
                entity + " not found with id: " + id,
                HttpStatus.NOT_FOUND
        );
    }

    public static AppException conflict(String message) {

        return new AppException(
                message,
                HttpStatus.CONFLICT
        );
    }

    public static AppException forbidden(String message) {

        return new AppException(
                message,
                HttpStatus.FORBIDDEN
        );
    }

    public static AppException badRequest(String message) {

        return new AppException(
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    public static AppException unauthorized(String message) {

        return new AppException(
                message,
                HttpStatus.UNAUTHORIZED
        );
    }
}