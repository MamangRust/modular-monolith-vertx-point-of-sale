-- Align the database schema with the current Vert.x service repositories.
-- All statements are idempotent so local databases that already contain part
-- of the schema can be upgraded safely.

ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS merchant_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS api_key VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_merchants_merchant_no_active
    ON merchants (merchant_no)
    WHERE merchant_no IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_merchants_api_key_active
    ON merchants (api_key)
    WHERE api_key IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS merchant_documents (
    document_id SERIAL PRIMARY KEY,
    merchant_id INT NOT NULL REFERENCES merchants (merchant_id) ON DELETE CASCADE,
    document_type VARCHAR(100) NOT NULL,
    document_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    note TEXT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_merchant_documents_merchant_id
    ON merchant_documents (merchant_id);
CREATE INDEX IF NOT EXISTS idx_merchant_documents_status
    ON merchant_documents (status);
CREATE INDEX IF NOT EXISTS idx_merchant_documents_created_at
    ON merchant_documents (created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_roles_user_role_active
    ON user_roles (user_id, role_id)
    WHERE deleted_at IS NULL;
