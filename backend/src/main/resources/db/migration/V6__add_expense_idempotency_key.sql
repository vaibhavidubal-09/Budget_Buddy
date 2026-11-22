ALTER TABLE expenses
    ADD COLUMN idempotency_key VARCHAR(128) NULL;

ALTER TABLE expenses
    ADD COLUMN idempotency_payload_hash VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_expenses_user_idempotency_key
    ON expenses (user_id, idempotency_key);
