package com.carson.ledger.domain;

public class CurrencyMismatchException extends Exception {
    public CurrencyMismatchException(String message) {
        super(message);
    }
}
