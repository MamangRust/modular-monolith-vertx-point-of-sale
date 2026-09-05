-- =============================================================================
-- V13 — Add user verification columns
-- =============================================================================
-- The auth service (UserRepositoryImpl) reads and writes `is_verified` and
-- `verification_code`, but V2 only created the base user columns. This
-- migration backfills the missing schema so `findByEmailAndVerify`,
-- `createUser` (verification_code + is_verified=false) and
-- `updateUserIsVerified` can run.
-- Idempotent: safe to re-run on any environment (dev, staging, prod).
-- =============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_users_verification_code
    ON users (verification_code) WHERE deleted_at IS NULL;
