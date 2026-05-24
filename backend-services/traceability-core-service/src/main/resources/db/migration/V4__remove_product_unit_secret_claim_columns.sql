-- Dọn mô hình cũ secret/claim; chuyển sang truy xuất công khai theo serial + scanCount.
ALTER TABLE product_units
    DROP COLUMN IF EXISTS secret_hash,
    DROP COLUMN IF EXISTS owner_id,
    DROP COLUMN IF EXISTS claimed_at,
    DROP COLUMN IF EXISTS owner_name_snapshot;
