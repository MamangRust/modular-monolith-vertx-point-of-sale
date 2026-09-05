-- =============================================================================
-- V12 — Partial unique indexes for soft-delete correctness
--
-- Problem: the original schema used plain UNIQUE constraints (e.g.
-- users.email, roles.role_name, categories.slug_category,
-- products.slug_product / barcode). With soft-delete, a trashed row still
-- holds its unique value, so re-creating an entity with the same value fails
-- even though the old one is "deleted" (deleted_at IS NOT NULL).
--
-- Fix: drop the plain UNIQUE constraints and replace them with partial unique
-- indexes WHERE deleted_at IS NULL. Only live rows are constrained, so a
-- trashed value can be reused while duplicates among active rows stay
-- impossible at the DB level.
-- =============================================================================

-- users.email
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_active
    ON users (email) WHERE deleted_at IS NULL;

-- roles.role_name
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_role_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_roles_role_name_active
    ON roles (role_name) WHERE deleted_at IS NULL;

-- categories.slug_category
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_slug_category_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_slug_active
    ON categories (slug_category) WHERE deleted_at IS NULL;

-- products.slug_product
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_slug_product_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_slug_active
    ON products (slug_product) WHERE deleted_at IS NULL;

-- products.barcode
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_barcode_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode_active
    ON products (barcode) WHERE deleted_at IS NULL;
