package com.carson.ledger.persistence;

import com.carson.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository {
    LedgerEntry save(LedgerEntry entry, UUID transactionId);
    List<LedgerEntry> findByAccountId(UUID accountId);
    BigDecimal calculateAccountBalance(UUID accountId);

}
