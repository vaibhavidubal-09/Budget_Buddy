ALTER TABLE expenses ADD COLUMN import_fingerprint VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_expenses_user_import_fingerprint
    ON expenses (user_id, import_fingerprint);
