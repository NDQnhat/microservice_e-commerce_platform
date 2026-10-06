# Docker Compose — E-Commerce Platform

## Profiles

Services are grouped into profiles so you can start only what you need.

| Profile | Services included |
|---|---|
| *(default)* | `postgres`, `eureka-server`, `api-gateway` |
| `flow-catalog` | `catalog-service`, `pricing-service`, `inventory-service`, `business-configuration-service` |
| `flow-cart` | `cart-service` |
| `flow-order` | `order-service`, `payment-service` |

---

## Quick Start

> **Quan trọng về Build Architecture:**
> Các `Dockerfile` của microservices sử dụng mô hình JRE siêu nhẹ (`COPY build/libs/*.jar app.jar`) thay vì build bên trong Docker để tiết kiệm RAM và tăng tốc độ khởi động.
> - **Khi có thay đổi code Java hoặc file SQL Migration**, bạn cần build lại file JAR trên máy host bằng Gradle trước khi chạy Docker:
>   ```bash
>   # Build tất cả các service (hoặc chỉ service cụ thể: gradle :services:catalog-service:bootJar)
>   gradle bootJar
>   
>   # Sau đó khởi động kèm cờ --build để cập nhật image
>   docker compose --profile flow-catalog up -d --build
>   ```

### 1. Infrastructure only (Postgres + Eureka + Gateway)

```bash
docker compose up -d
```

### 2. Full Storefront (recommended for frontend development)

Starts everything needed for the customer-facing storefront — catalog browsing, cart, checkout, and orders:

```bash
docker compose --profile flow-catalog --profile flow-cart --profile flow-order up -d
```

### 3. Catalog only (product listing / detail pages)

```bash
docker compose --profile flow-catalog up -d
```

### 4. Catalog + Cart (browsing and add-to-cart)

```bash
docker compose --profile flow-catalog --profile flow-cart up -d
```

---

## Verification

After starting, confirm all services are registered with Eureka:

```
http://localhost:8761
```

Test the API Gateway:

```bash
# Should return a list of categories from the DB seed
curl -s http://localhost:8080/api/v1/categories | jq .

# Should return seeded products with SKUs and prices
curl -s http://localhost:8080/api/v1/products | jq .

# Should return 400 Bad Request (not 500) for an invalid UUID
curl -i http://localhost:8080/api/v1/products/invalid-uuid
```

---

## Stopping

```bash
# Stop all running containers (preserves volumes)
docker compose --profile flow-catalog --profile flow-cart --profile flow-order down

# Stop and remove volumes (full reset including DB data)
docker compose --profile flow-catalog --profile flow-cart --profile flow-order down -v
```

---

## Port Reference

| Service | Port |
|---|---|
| API Gateway | `8080` |
| Eureka Dashboard | `8761` |
| PostgreSQL | `5432` |
| Catalog Service | `8081` |
| Pricing Service | `8082` |
| Inventory Service | `8083` |
| Business Config Service | `8084` |
| Cart Service | `8085` |
| Order Service | `8086` |
| Payment Service | `8087` |

---

## Frontend

Start the Next.js frontend separately:

```bash
cd apps/user-web
npm run dev
```

The frontend connects to the API Gateway at `http://localhost:8080` (configured via `NEXT_PUBLIC_API_URL`).
When the backend is unavailable or returns 503, the frontend automatically falls back to high-fidelity mock data and shows a **Demo Mode** banner.
