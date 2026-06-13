package com.carson.ledger.persistence;

import com.carson.ledger.domain.Transaction;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<TransactionRecordDto> findByAccountId(UUID accountId);
}
