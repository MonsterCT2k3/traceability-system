-- Seri tờ tiền đăng ký từ NSX: mỗi dòng một seri (mobile gom theo tập chỉ để UX).
CREATE TABLE banknote_serial (
    id VARCHAR(36) PRIMARY KEY,
    serial_value VARCHAR(32) NOT NULL,
    registered_by_user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_banknote_serial_value UNIQUE (serial_value)
);

CREATE INDEX idx_banknote_serial_registered_by ON banknote_serial (registered_by_user_id);
