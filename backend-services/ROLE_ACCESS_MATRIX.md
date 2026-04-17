# Role Access Matrix

Ma trận này phản ánh đúng code hiện tại của `identity-service`, `api-gateway`, `product-service`.

## Roles

- `ADMIN`
- `SUPPLIER`
- `MANUFACTURER`
- `RETAILER`
- `TRANSPORTER`
- `USER`

## Identity Service (`/identity/...` qua gateway hoặc `:8081`)

| Endpoint | Access |
|---|---|
| `POST /api/v1/auth/register` | Public |
| `POST /api/v1/auth/login` | Public |
| `POST /api/v1/auth/refresh` | Public |
| `POST /api/v1/auth/logout` | Public |
| `POST /api/v1/auth/introspect` | Public |
| `PATCH /api/v1/admin/users/{userId}/role` | `ADMIN` |
| `GET /api/v1/admin/role-requests/pending` | `ADMIN` |
| `POST /api/v1/admin/role-requests/{requestId}/approve` | `ADMIN` |
| `POST /api/v1/admin/role-requests/{requestId}/reject` | `ADMIN` |
| `POST /api/v1/users/role-requests` | Authenticated |
| `GET /api/v1/users/role-requests` | Authenticated |
| `GET /actuator/health` | Public |
| `GET /actuator/info` | Public |
| Swagger (`/swagger-ui.html`, `/v3/api-docs`) | Public |

## Product Service (`/product/...` qua gateway hoặc `:8082`)

### Public endpoints

| Endpoint | Access |
|---|---|
| `GET /api/v1/products` | Public |
| `GET /api/v1/products/{id}` | Public |
| `GET /api/v1/products/{id}/qr` | Public |
| `GET /api/v1/units/{unitId}/trace` | Public |
| `GET /api/v1/units/{unitId}/qr` | Public |
| `GET /api/v1/units/trace/by-serial` | Public |
| `GET /api/v1/histories/product/{productId}` | Public |
| `POST /api/v1/units/{unitId}/secret-scan` | Public |
| `GET /actuator/health` | Public |
| `GET /actuator/info` | Public |
| Swagger (`/swagger-ui.html`, `/v3/api-docs`) | Public |

### Authenticated + role-based (`@PreAuthorize`)

| Endpoint | Allowed roles |
|---|---|
| `POST /api/v1/products` | `MANUFACTURER`, `ADMIN` |
| `POST /api/v1/products/{productId}/pallets/anchor` | `MANUFACTURER`, `ADMIN` |
| `POST /api/v1/raw-batches` | `SUPPLIER`, `MANUFACTURER`, `ADMIN` |
| `POST /api/v1/pallets/{palletId}/cartons` | `SUPPLIER`, `MANUFACTURER`, `ADMIN` |
| `POST /api/v1/pallets/{palletId}/packing-bulk` | `SUPPLIER`, `MANUFACTURER`, `ADMIN` |
| `POST /api/v1/cartons/{cartonId}/units/generate` | `SUPPLIER`, `MANUFACTURER`, `ADMIN` |
| `POST /api/v1/histories` | `SUPPLIER`, `MANUFACTURER`, `TRANSPORTER`, `ADMIN` |
| `POST /api/v1/units/{unitId}/claim` | `USER`, `RETAILER`, `ADMIN` |
| `POST /api/v1/transfers` | `SUPPLIER`, `MANUFACTURER`, `RETAILER`, `TRANSPORTER`, `ADMIN` |
| `POST /api/v1/transfers/{transferId}/respond` | `SUPPLIER`, `MANUFACTURER`, `RETAILER`, `TRANSPORTER`, `ADMIN` |
| `GET /api/v1/transfers/pending` | `SUPPLIER`, `MANUFACTURER`, `RETAILER`, `TRANSPORTER`, `ADMIN` |
| `POST /api/v1/orders` | `MANUFACTURER` (đơn M→NCC), `RETAILER` (đơn retailer→NSX) — kiểm tra `orderType` trong service |
| `GET /api/v1/orders/mine/buyer` | `MANUFACTURER`, `RETAILER` |
| `GET /api/v1/orders/mine/seller` | `SUPPLIER`, `MANUFACTURER` |
| `GET /api/v1/orders/mine/carrier` | `TRANSPORTER` |
| `POST /api/v1/orders/{id}/accept` | `SUPPLIER`, `MANUFACTURER` |
| `POST /api/v1/orders/{id}/reject` | `SUPPLIER`, `MANUFACTURER` |
| `POST /api/v1/orders/{id}/cancel` | `MANUFACTURER`, `RETAILER` |
| `POST /api/v1/orders/{id}/assign-carrier` | `SUPPLIER`, `MANUFACTURER` |
| `POST /api/v1/orders/{id}/confirm-picked-up` | `TRANSPORTER` (VC đã nhận hàng tại người bán, trạng thái `ASSIGNED_TO_CARRIER`) |
| `POST /api/v1/orders/{id}/confirm-delivered` | `TRANSPORTER`, `SUPPLIER`, `MANUFACTURER` (VC chỉ sau `PICKED_UP_FROM_SELLER`; người bán xác nhận khi giao trực tiếp không qua VC từ `ACCEPTED`) |

### Authenticated nhưng chưa gắn `@PreAuthorize` theo role cụ thể (chỉ cần token hợp lệ)

| Endpoint | Access |
|---|---|
| `GET /api/v1/orders/{id}` | Authenticated; quyền xem: người mua, người bán, VC (nếu có), hoặc `ADMIN` (kiểm tra trong service) |

## Gateway behavior (`:8080`)

- Public matcher cho phép:
  - tất cả `OPTIONS` (CORS preflight)
  - `/api/v1/auth/**`
  - các GET/POST public của product ở trên
  - `GET /actuator/health` và `GET /actuator/info`
- Các endpoint còn lại qua gateway yêu cầu `Authorization: Bearer ...` hợp lệ (thông qua `identity-service` introspect).

## Notes

- `role` được lấy từ JWT claim và map thành `ROLE_<ROLE_NAME>`.
- Sau khi admin đổi role cho user, nên đăng nhập lại để lấy access token mới chứa role mới.
