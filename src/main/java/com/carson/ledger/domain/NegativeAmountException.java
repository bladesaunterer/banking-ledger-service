package com.carson.ledger.domain;

public class NegativeAmountException extends Exception {
    public NegativeAmountException(String message) {
        super(message);
    }
}
