CREATE TABLE blockchain_gas_usage (
    id UUID PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    tx_hash VARCHAR(100),
    operation VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    billing_actor_id VARCHAR(100) NOT NULL,
    billing_role VARCHAR(30) NOT NULL,
    initiated_by_user_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    gas_used NUMERIC(78, 0),
    effective_gas_price_wei NUMERIC(78, 0),
    fee_wei NUMERIC(78, 0),
    block_number NUMERIC(78, 0),
    error_code VARCHAR(100),
    error_message TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE,
    mined_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_blockchain_gas_usage_request_id UNIQUE (request_id),
    CONSTRAINT ck_blockchain_gas_usage_billing_role
        CHECK (billing_role IN ('SUPPLIER', 'MANUFACTURER')),
    CONSTRAINT ck_blockchain_gas_usage_status
        CHECK (status IN (
            'PENDING',
            'SUCCESS',
            'FAILED_ON_CHAIN',
            'SUBMISSION_FAILED',
            'RECEIPT_UNKNOWN'
        ))
);

CREATE UNIQUE INDEX uk_blockchain_gas_usage_tx_hash
    ON blockchain_gas_usage(tx_hash)
    WHERE tx_hash IS NOT NULL;

CREATE INDEX idx_gas_usage_actor_created
    ON blockchain_gas_usage(billing_actor_id, created_at DESC);

CREATE INDEX idx_gas_usage_role_created
    ON blockchain_gas_usage(billing_role, created_at DESC);

CREATE INDEX idx_gas_usage_operation_created
    ON blockchain_gas_usage(operation, created_at DESC);

CREATE INDEX idx_gas_usage_status_updated
    ON blockchain_gas_usage(status, updated_at);
