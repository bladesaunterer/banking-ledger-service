package com.carson.ledger.domain;

public class SameAccountException extends Exception {
    public SameAccountException(String message) {
        super(message);
    }
}
