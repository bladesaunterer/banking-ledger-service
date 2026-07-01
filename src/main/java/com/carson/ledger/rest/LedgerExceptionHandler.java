package com.carson.ledger.rest;

import com.carson.ledger.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class LedgerExceptionHandler {

    @ExceptionHandler({
            CurrencyMismatchException.class,
            InsufficientFundsException.class,
            SameAccountException.class,
            NegativeAmountException.class,
            IllegalArgumentException.class}
    )
    ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleArgumentInvalidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> "'%s' %s".formatted(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

}
