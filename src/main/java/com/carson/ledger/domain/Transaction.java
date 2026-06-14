package com.carson.ledger.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Transaction {
    private final List<LedgerEntry> entries;
    private final UUID id;
    private final Instant timestamp;
    private final TransactionEvent eventType;

    private Transaction(UUID id , Instant timestamp, List<LedgerEntry> entries, TransactionEvent eventType) {
        this.id = id;
        this.timestamp = timestamp;
        this.entries = List.copyOf(entries);
        this.eventType = eventType;
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }

    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public TransactionEvent getEventType() {
        return eventType;
    }

    public static class Builder {
        private final List<LedgerEntry> entries = new ArrayList<>();
        private TransactionEvent event;
        private UUID transactionId;
        private Instant timestamp;

        public Builder addEntry(LedgerEntry entry) {
            this.entries.add(entry);
            return this;
        }

        public Builder addEventType(TransactionEvent event) {
            this.event = event;
            return this;
        }

        public Builder addTransactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder addTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Transaction build() {
            if (this.entries.isEmpty()) {
                throw new IllegalArgumentException("Transaction must contain entries");
            }
            if(this.event == null) {
                throw new IllegalArgumentException("Transaction must contain event type");
            }

            if(this.transactionId == null) {
                this.transactionId = UUID.randomUUID();
            }

            if (this.timestamp == null) {
                this.timestamp = Instant.now();
            }

            return new Transaction(this.transactionId, this.timestamp, entries, event);
        }
    }
}
