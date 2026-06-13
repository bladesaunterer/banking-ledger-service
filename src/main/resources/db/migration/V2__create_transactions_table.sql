CREATE TYPE transaction_event AS ENUM (
    'DEPOSIT', 'WITHDRAWAL', 'TRANSFER'
);

CREATE TABLE transactions (
    id          UUID PRIMARY KEY,
    timestamp   timestamptz NOT NULL,
    event_type  transaction_event NOT NULL
);