-- V6: Add is_used flag and assigned_unit_serial FK to banknote_serial table
-- This establishes a proper 1-1 relationship with product_units for DB integrity

ALTER TABLE banknote_serial
    ADD COLUMN IF NOT EXISTS is_used BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS assigned_unit_serial VARCHAR(255) NULL;

-- Add FK constraint: banknote_serial.assigned_unit_serial -> product_units.unit_serial
ALTER TABLE banknote_serial
    ADD CONSTRAINT fk_banknote_serial_product_unit
        FOREIGN KEY (assigned_unit_serial)
        REFERENCES product_units (unit_serial)
        ON DELETE SET NULL;

-- Add index for fast availability queries: WHERE is_used = false
CREATE INDEX IF NOT EXISTS idx_banknote_serial_is_used ON banknote_serial (registered_by_user_id, is_used);

-- Backfill: mark existing serials as used if they are already assigned to a product unit
UPDATE banknote_serial b
SET is_used = true,
    assigned_unit_serial = (
        SELECT u.unit_serial FROM product_units u WHERE u.unit_serial = b.serial_value LIMIT 1
    )
WHERE EXISTS (
    SELECT 1 FROM product_units u WHERE u.unit_serial = b.serial_value
);
