package ru.practicum.ewm.main.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class EwmMainServiceErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseList handleConstraintViolationException(ConstraintViolationException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponseList(
                e.getConstraintViolations().stream()
                        .map(violation -> new ErrorResponse(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseList handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponseList(
                e.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> new ErrorResponse(
                                fieldError.getField(),
                                fieldError.getDefaultMessage()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidParamException(InvalidParamException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse(e.getParamName(), e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotExistsException(NotExistsException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse(e.getClassName(), e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse(e.getHeaderName(), e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse("Request body", e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse("Integrity violation", e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleForbiddenException(ForbiddenException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse(e.getParamName(), e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse("Query parameter", e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleThrowable(Throwable e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse("Unrecognized param", e.getMessage());
    }

    @Getter
    @AllArgsConstructor
    private class ErrorResponseList {
        private final List<ErrorResponse> errorResponses;
    }

    @Getter
    @AllArgsConstructor
    private class ErrorResponse {

        private String paramName;
        private String error;

    }

}
