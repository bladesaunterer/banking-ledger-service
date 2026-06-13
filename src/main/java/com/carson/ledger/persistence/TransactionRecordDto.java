package com.carson.ledger.persistence;

import com.carson.ledger.domain.TransactionEvent;

import java.time.Instant;
import java.util.UUID;

public class TransactionRecordDto {
    private final UUID id;
    private final Instant timestamp;
    private final TransactionEvent eventType;

    public TransactionRecordDto(UUID id, Instant timestamp, TransactionEvent eventType) {
        this.id = id;
        this.timestamp = timestamp;
        this.eventType = eventType;
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
}
