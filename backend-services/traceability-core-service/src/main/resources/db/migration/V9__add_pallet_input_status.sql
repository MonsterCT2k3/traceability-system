ALTER TABLE pallets ADD COLUMN IF NOT EXISTS input_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';
ALTER TABLE pallets DROP CONSTRAINT IF EXISTS ck_pallets_input_status;
ALTER TABLE pallets ADD CONSTRAINT ck_pallets_input_status
    CHECK (input_status IN ('AVAILABLE', 'RESERVED', 'CONSUMED'));
CREATE INDEX IF NOT EXISTS idx_pallets_owner_input_status ON pallets(owner_id, input_status);
