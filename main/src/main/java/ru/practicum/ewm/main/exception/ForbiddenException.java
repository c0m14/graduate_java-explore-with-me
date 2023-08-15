package ru.practicum.ewm.main.exception;

import lombok.Getter;

public class ForbiddenException extends RuntimeException {

    @Getter
    private final String paramName;

    public ForbiddenException(String paramName, String message) {
        super(message);
        this.paramName = paramName;
    }
}
