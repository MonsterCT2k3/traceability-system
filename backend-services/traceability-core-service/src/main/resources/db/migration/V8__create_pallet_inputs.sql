CREATE TABLE IF NOT EXISTS pallet_inputs (
    id VARCHAR(36) PRIMARY KEY,
    output_pallet_id VARCHAR(36) NOT NULL,
    input_type VARCHAR(20) NOT NULL,
    input_id VARCHAR(36) NOT NULL,
    input_batch_id_hex VARCHAR(66) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pallet_inputs_output FOREIGN KEY (output_pallet_id) REFERENCES pallets(id),
    CONSTRAINT ck_pallet_inputs_type CHECK (input_type IN ('RAW_BATCH', 'PALLET')),
    CONSTRAINT ck_pallet_inputs_not_self CHECK (NOT (input_type = 'PALLET' AND output_pallet_id = input_id)),
    CONSTRAINT uk_pallet_inputs_output_type_input UNIQUE (output_pallet_id, input_type, input_id)
);

CREATE INDEX IF NOT EXISTS idx_pallet_inputs_output ON pallet_inputs(output_pallet_id);
CREATE INDEX IF NOT EXISTS idx_pallet_inputs_input ON pallet_inputs(input_type, input_id);
