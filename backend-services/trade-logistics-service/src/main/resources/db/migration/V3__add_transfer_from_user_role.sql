ALTER TABLE transfer_records
    ADD COLUMN IF NOT EXISTS from_user_role VARCHAR(30);
