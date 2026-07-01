package com.carson.ledger.rest;

public class WithdrawRequest extends TransactionRequest{
    public WithdrawRequest(String amount) {
        super(amount);
    }
}

