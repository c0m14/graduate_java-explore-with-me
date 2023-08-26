package ru.practicum.ewm.main.exception;

import lombok.Getter;

public class NotExistsException extends RuntimeException {
    @Getter
    private final String className;

    public NotExistsException(String className, String message) {
        super(message);
        this.className = className;
    }
}
