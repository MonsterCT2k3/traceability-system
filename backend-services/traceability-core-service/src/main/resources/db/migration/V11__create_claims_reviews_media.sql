CREATE TABLE product_unit_claims (
    id VARCHAR(36) PRIMARY KEY,
    product_unit_id VARCHAR(36) NOT NULL UNIQUE REFERENCES product_units(id),
    claim_token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    claimed_by_user_id VARCHAR(36),
    claimed_at TIMESTAMP,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT product_unit_claims_status_check CHECK (status IN ('AVAILABLE', 'CLAIMED', 'REVOKED'))
);

CREATE INDEX idx_product_unit_claims_user ON product_unit_claims(claimed_by_user_id, claimed_at DESC);

CREATE TABLE product_reviews (
    id VARCHAR(36) PRIMARY KEY,
    claim_id VARCHAR(36) NOT NULL UNIQUE REFERENCES product_unit_claims(id),
    product_unit_id VARCHAR(36) NOT NULL UNIQUE REFERENCES product_units(id),
    product_id VARCHAR(36) NOT NULL,
    reviewer_id VARCHAR(36) NOT NULL,
    rating SMALLINT NOT NULL,
    content TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT product_reviews_rating_check CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT product_reviews_status_check CHECK (status IN ('PUBLISHED', 'HIDDEN'))
);

CREATE INDEX idx_product_reviews_product ON product_reviews(product_id, status, created_at DESC);
CREATE INDEX idx_product_reviews_reviewer ON product_reviews(reviewer_id, created_at DESC);

CREATE TABLE review_media (
    id VARCHAR(36) PRIMARY KEY,
    review_id VARCHAR(36) REFERENCES product_reviews(id),
    uploader_id VARCHAR(36) NOT NULL,
    media_type VARCHAR(10) NOT NULL,
    media_url TEXT NOT NULL,
    thumbnail_url TEXT,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    duration_seconds INTEGER,
    sort_order INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    attached_at TIMESTAMP,
    CONSTRAINT review_media_type_check CHECK (media_type IN ('IMAGE', 'VIDEO')),
    CONSTRAINT review_media_status_check CHECK (status IN ('UPLOADED', 'ATTACHED', 'DELETED'))
);

CREATE INDEX idx_review_media_review ON review_media(review_id, sort_order);
CREATE INDEX idx_review_media_orphan ON review_media(status, created_at);
