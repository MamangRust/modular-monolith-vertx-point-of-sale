# =============================================================================
# hello.hurl — Smoke & Contract Test untuk API Gateway (Vert.x REST, :5000)
# =============================================================================
# Diverifikasi 2026-08-07 terhadap working tree & stack lokal:
#   - Gateway REST di http://localhost:5000 (bukan 8080)
#   - Route /health publik; /api/products*, /me dll. dilindungi JWT (JwtMiddleware)
#   - Envelope response = JSON protobuf (snake_case): {status, message, data}
#   - Jalankan:  hurl --test hello.hurl   (butuh stack up: docker compose up -d)
#
# Bagian SMOKE di bawah ini bisa dijalankan sekarang (tanpa seed).
# Bagian FLOW LENGKAP (di bawah) butuh seed ROLE_ADMIN + user verified (gap #18)
# dan service user terverifikasi (gap #18). Error mapping auth sudah selesai.
# =============================================================================

# ── 1. SMOKE (runnable sekarang) ─────────────────────────────────────────
GET http://localhost:5000/health

HTTP 200
[Asserts]
jsonpath "$.status" == "UP"

# ── Semua route JWT-protected tanpa token → 401 (bukti middleware aktif) ──
GET http://localhost:5000/me

HTTP 401

GET http://localhost:5000/api/products

HTTP 401

GET http://localhost:5000/users

HTTP 401

GET http://localhost:5000/roles

HTTP 401

GET http://localhost:5000/api/merchants

HTTP 401

GET http://localhost:5000/transactions

HTTP 401

GET http://localhost:5000/api/cashiers

HTTP 401

GET http://localhost:5000/api/categories

HTTP 401

GET http://localhost:5000/api/orders

HTTP 401

GET http://localhost:5000/api/order-items

HTTP 401

# Route tidak dikenal → 404 (bukan 401/500)
GET http://localhost:5000/tidak-ada

HTTP 404

# Login kredensial salah → 401 setelah error mapping gRPC dan Redis cluster
# tervalidasi end-to-end (gap #17 dan gap #21 sudah ditutup).
POST http://localhost:5000/login
Content-Type: application/json

{
  "email": "tidak-ada@example.com",
  "password": "salah"
}

HTTP 401

# =============================================================================
# 2. FLOW LENGKAP (kontrak) — TIDAK dijalankan otomatis
#    Prasyarat: seed ROLE_ADMIN + user verified (gap #18) + fix gap #17.
#    Kontrak: register/login = JSON; product create = FORM-DATA; semua route
#    /api/* butuh "Authorization: Bearer <token>".
# =============================================================================

# # Register user baru (email unik via {{uuid}})
# POST http://localhost:5000/register
# Content-Type: application/json
#
# {
#   "firstname": "Hurl",
#   "lastname": "Test",
#   "email": "hurl-{{uuid}}@example.com",
#   "password": "P@ssw0rd123!"
# }
#
# HTTP 201
# [Asserts]
# jsonpath "$.status" == "success"
# [Captures]
# registered_email: jsonpath "$.data.email"
#
# # Login → capture access_token (JSON, bukan form)
# POST http://localhost:5000/login
# Content-Type: application/json
#
# {
#   "email": "{{registered_email}}",
#   "password": "P@ssw0rd123!"
# }
#
# HTTP 200
# [Asserts]
# jsonpath "$.status" == "success"
# [Captures]
# access_token: jsonpath "$.data.access_token"
#
# # /me dengan token
# GET http://localhost:5000/me
# Authorization: Bearer {{access_token}}
#
# HTTP 200
# [Asserts]
# jsonpath "$.status" == "success"
#
# # List product (JWT)
# GET http://localhost:5000/api/products
# Authorization: Bearer {{access_token}}
#
# HTTP 200
# [Asserts]
# jsonpath "$.status" == "success"
#
# # Create product (FORM-DATA — lihat ProductProxyHandler.create)
# # Wajib ada merchant & category valid di DB (FK).
# POST http://localhost:5000/api/products
# Authorization: Bearer {{access_token}}
# Content-Type: multipart/form-data; boundary=boundary123
#
# --boundary123
# Content-Disposition: form-data; name="merchant_id"
#
# 1
# --boundary123
# Content-Disposition: form-data; name="category_id"
#
# 1
# --boundary123
# Content-Disposition: form-data; name="name"
#
# Produk Hurl Test
# --boundary123
# Content-Disposition: form-data; name="description"
#
# Produk untuk testing Hurl
# --boundary123
# Content-Disposition: form-data; name="price"
#
# 150000
# --boundary123
# Content-Disposition: form-data; name="count_in_stock"
#
# 50
# --boundary123
# Content-Disposition: form-data; name="brand"
#
# TestBrand
# --boundary123
# Content-Disposition: form-data; name="weight"
#
# 100
# --boundary123--
#
# HTTP 201
# [Asserts]
# jsonpath "$.status" == "success"
# jsonpath "$.data.id" exists
# [Captures]
# product_id: jsonpath "$.data.id"
#
# # Get by id
# GET http://localhost:5000/api/products/{{product_id}}
# Authorization: Bearer {{access_token}}
#
# HTTP 200
# [Asserts]
# jsonpath "$.data.id" == {{product_id}}
