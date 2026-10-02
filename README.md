# 🛒 Enterprise Distributed E-Commerce Platform

> **Hệ thống Thương mại Điện tử Phân tán Chuẩn Doanh nghiệp**  
> Kiến trúc Microservices hiện đại dựa trên **Java 17 / Spring Boot 3.4.3**, **Spring Cloud 2024**, **PostgreSQL 16 (Database-per-Service)**, **Apache Kafka**, **Redis 7**, **Next.js 15**, **React 19 & Vite 7**, áp dụng đầy đủ các mẫu thiết kế phân tán nâng cao: **Saga Orchestration**, **Transactional Outbox**, **Idempotent Consumers**, **Resilience4j Circuit Breaker**, **OpenTelemetry Distributed Tracing** và **RBAC 5 vai trò**.

---

## 📑 Mục lục
1. [Tổng quan Dự án](#-tổng-quan-dự-án)
2. [Ngăn xếp Công nghệ (Tech Stack)](#-ngăn-xếp-công-nghệ-tech-stack)
3. [Cấu trúc Thư mục Monorepo](#-cấu-trúc-thư-mục-monorepo)
4. [Bảng Cổng Dịch vụ & Ánh xạ Cổng (Port Mapping)](#-bảng-cổng-dịch-vụ--ánh-xạ-cổng-port-mapping)
5. [Yêu cầu Tiên quyết (Prerequisites)](#-yêu-cầu-tiên-quyết-prerequisites)
6. [Hướng dẫn Cài đặt & Khởi chạy Từng bước](#-hướng-dẫn-cài-đặt--khởi-chạy-từng-bước)
   - [Bước 1: Khởi động Hạ tầng Middleware (Docker Compose)](#bước-1-khởi-động-hạ-tầng-middleware-docker-compose)
   - [Bước 2: Build & Kiểm thử Toàn bộ Source Code](#bước-2-build--kiểm-thử-toàn-bộ-source-code)
   - [Bước 3: Khởi chạy Backend Services](#bước-3-khởi-chạy-backend-services)
   - [Bước 4: Khởi chạy Frontend Applications](#bước-4-khởi-chạy-frontend-applications)
7. [Biến Môi trường & Cấu hình Hệ thống](#-biến-môi-trường--cấu-hình-hệ-thống)
8. [Tài khoản Mặc định & Phân quyền RBAC](#-tài-khoản-mặc-định--phân-quyền-rbac)
9. [Kịch bản Trải nghiệm Mẫu (Quickstart Flow)](#-kịch-bản-trải-nghiệm-mẫu-quickstart-flow)
10. [Công cụ Giám sát & Vận hành (Observability & Ops Tools)](#-công-cụ-giám-sát--vận-hành-observability--ops-tools)
11. [Hướng dẫn Chạy Kiểm thử (Testing Guide)](#-hướng-dẫn-chạy-kiểm-thử-testing-guide)
12. [Tài liệu Kiến trúc Bổ sung](#-tài-liệu-kiến-trúc-bổ-sung)

---

## 🌟 Tổng quan Dự án

Dự án này là một nền tảng thương mại điện tử phân tán hoàn chỉnh, mô phỏng môi trường sản xuất thực tế (production-grade), được thiết kế tuân thủ nghiêm ngặt hai tài liệu kỹ thuật cốt lõi:
- **SRS Kỹ thuật Phân tán**: [`docs/distributed_e-commerce_final-srs.md`](docs/distributed_e-commerce_final-srs.md)
- **Hồ sơ Kỹ thuật**: [`docs/tech_profile.yaml`](docs/tech_profile.yaml)

### Các Điểm Nhấn Kiến trúc Đặc sắc:
- **100% Database-per-Service**: 12 cơ sở dữ liệu logic riêng biệt cho 12 microservices nghiệp vụ; không chia sẻ bảng, không cross-join, không cross-foreign-key.
- **Transactional Outbox Pattern**: Lưu trữ các sự kiện nghiệp vụ vào bảng `outbox_events` trong cùng một giao dịch ACID với dữ liệu thực tế, giải quyết triệt để bài toán **Dual-Write Problem**.
- **Saga Orchestration**: Điều phối chu trình đặt hàng phức tạp (`order-service` -> `inventory-service` -> `payment-service` -> `fulfillment-service`) đi kèm cơ chế bồi hoàn tự động (Compensating Transactions) khi có lỗi phát sinh.
- **Idempotency Guarantees**: Đảm bảo an toàn tuyệt đối trước các lần gọi lặp lại (retry webhooks, thanh toán trùng) bằng `idempotency_key` và cơ chế chống trùng lặp.
- **Quan sát Phân tán (Full-Stack Observability)**: Đồng bộ mã tương quan `X-Correlation-Id` qua SLF4J MDC, OpenTelemetry Distributed Tracing, Prometheus Metrics và Grafana Tempo.
- **Đa Giao diện Hiện đại**: Cửa hàng Storefront khách hàng viết bằng **Next.js 15 (App Router)** và Cổng Vận hành Quản trị Backoffice viết bằng **React 19 + Vite 7 SPA**.

---

## 🛠 Ngăn xếp Công nghệ (Tech Stack)

| Phân hệ | Công nghệ cốt lõi | Phiên bản | Ghi chú & Trách nhiệm |
|---|---|---|---|
| **Ngôn ngữ Backend** | Java | 17 (LTS) | Biên dịch chuẩn `-parameters`, UTF-8 |
| **Backend Framework** | Spring Boot | 3.4.3 | Spring IoC, Spring Data JPA, Hibernate 6 |
| **Service Discovery** | Spring Cloud Netflix Eureka | 2024.0.0 | Đăng ký & phát hiện dịch vụ tự động |
| **API Gateway** | Spring Cloud Gateway | 2024.0.0 | Reactive WebFlux, Dynamic Routing, CORS, Trace Injection |
| **Cơ sở dữ liệu** | PostgreSQL | 16 (Alpine) | 12 Logical Databases, Flyway 10 quản lý migration |
| **Message Broker** | Apache Kafka & Zookeeper | 3.x / Confluent 7.5 | Truyền thông bất đồng bộ hướng sự kiện (Event-driven) |
| **Bộ nhớ đệm & Khóa** | Redis | 7 (Alpine) | Caching, Cart Session, Distributed Locking |
| **Lưu trữ Đối tượng** | MinIO | Latest (S3 API) | Lưu trữ hình ảnh sản phẩm, assets media |
| **Khả năng Phục hồi** | Resilience4j | 2.2.0 | Circuit Breaker, Exponential Backoff Retry, Fallback |
| **Bảo mật & Auth** | Spring Security & JJWT | 6.x / 0.12.6 | JWT Bearer, BCrypt password hashing, RBAC |
| **Storefront App** | Next.js, React, Tailwind CSS | 15.1.7 / 19 / 4.0 | Server Components, TanStack Query 5, Zustand 5, Zod |
| **Backoffice App** | Vite, React, React Router | 7.x / 19 / 7.1 | SPA Quản trị dữ liệu đậm đặc, 5 vai trò RBAC |
| **Giám sát & Tracing** | OTel, Prometheus, Tempo, Grafana | Latest | Tracing đầu cuối, Metrics cào tự động, Bảng điều khiển |

---

## 📂 Cấu trúc Thư mục Monorepo

```
e-commerce_platform/
├── .github/workflows/ci.yml         # CI Pipeline: Matrix build Java 17 & Node 22
├── apps/                            # Các ứng dụng giao diện người dùng (Frontend)
│   ├── admin-web/                   # Backoffice SPA (Vite 7, React 19, Tailwind 4, RBAC 5 Roles)
│   └── user-web/                    # Storefront Customer (Next.js 15 App Router, React 19)
├── docs/                            # Tài liệu phân tích nghiệp vụ & kỹ thuật
│   ├── distributed_e-commerce_final-srs.md  # Bản đặc tả yêu cầu phần mềm chính thức (SRS)
│   ├── tech_profile.yaml                    # Hồ sơ chuẩn công nghệ & nguyên tắc kỹ thuật
│   └── OUTPUT_2_1.md                        # Báo cáo nghiệm thu giai đoạn nền tảng
├── gradle/libs.versions.toml        # Centralized Gradle Version Catalog (khóa phiên bản)
├── infra/                           # Hạ tầng & Container Orchestration
│   ├── docker-compose/              # docker-compose.yml khởi chạy toàn bộ middleware
│   ├── postgresql/init-scripts/     # Script tạo 12 cơ sở dữ liệu riêng biệt
│   └── observability/               # Config OTel Collector, Prometheus & Grafana Tempo
├── services/                        # 15 Microservices Backend (Java / Spring Boot)
│   ├── common/                      # Thư viện dùng chung (RFC 7807 Error, Correlation MDC, Outbox, Event)
│   ├── service-discovery/           # Netflix Eureka Server (Port 8761)
│   ├── api-gateway/                 # Spring Cloud Gateway (Port 8080)
│   ├── identity-access-service/     # Auth, User Accounts, Addresses, RBAC (Port 8081)
│   ├── catalog-service/             # Danh mục, Sản phẩm, SKUs, Thuộc tính (Port 8082)
│   ├── pricing-service/             # Quản lý Giá niêm yết, Khuyến mãi (Port 8083)
│   ├── cart-service/                # Giỏ hàng Khách vãng lai/Thành viên, Redis Cache (Port 8084)
│   ├── inventory-service/           # Tồn kho, Giữ hàng (Reservation), Ledger FIFO (Port 8085)
│   ├── order-service/               # Máy trạng thái Đơn hàng, Saga Coordinator (Port 8086)
│   ├── payment-service/             # Xử lý Giao dịch, Idempotency, Đối soát (Port 8087)
│   ├── fulfillment-service/         # Đóng gói, Vận chuyển, Carrier Tracking Guard (Port 8088)
│   ├── notification-service/        # Mẫu thông báo, Gửi Email/SMS mô phỏng (Port 8089)
│   ├── audit-compliance-service/    # Nhật ký Kiểm toán Bất biến (Append-Only) (Port 8090)
│   ├── business-configuration-service/ # Cấu hình Động runtime có Versioning (Port 8091)
│   └── exception-management-service/   # Triage Board Ngoại lệ & Metrics Dashboard (Port 8092)
├── build.gradle                     # Root Gradle script
├── settings.gradle                  # Khai báo các module dự án
├── ARCHITECTURE.md                  # Tài liệu chi tiết kiến trúc & luồng hoạt động
└── README.md                        # Tài liệu hướng dẫn sử dụng (file này)
```

---

## 🔌 Bảng Cổng Dịch vụ & Ánh xạ Cổng (Port Mapping)

### 1. Backend Microservices & Gateways

| Dịch vụ | Port | Database Logic | Trách nhiệm chính |
|---|---|---|---|
| **Service Discovery** | `8761` | *N/A* | Eureka Service Registry |
| **API Gateway** | `8080` | *N/A* | Điểm vào duy nhất (Single External Ingress), Routing, CORS |
| **Identity & Access** | `8081` | `identity_access_db` | Đăng ký, Đăng nhập, JWT, Địa chỉ, RBAC |
| **Catalog Service** | `8082` | `catalog_db` | Quản lý Category, Product, SKU, Biến thể |
| **Pricing Service** | `8083` | `pricing_db` | Bảng giá, Khuyến mãi, Tính giá |
| **Cart Service** | `8084` | `cart_db` (+ Redis) | Quản lý Giỏ hàng người dùng |
| **Inventory Service** | `8085` | `inventory_db` (+ Redis) | Khóa giữ hàng tồn kho, Ledger xuất/nhập |
| **Order Service** | `8086` | `order_db` | Vòng đời đơn hàng, State Machine, Điều phối Saga |
| **Payment Service** | `8087` | `payment_db` | Xử lý thanh toán, Webhook Idempotency, Đối soát |
| **Fulfillment Service**| `8088` | `fulfillment_db` | Vận đơn, Đóng gói, Đơn vị vận chuyển |
| **Notification Service**| `8089` | `notification_db` | Gửi Email / SMS, Quản lý Template |
| **Audit Compliance** | `8090` | `audit_compliance_db`| Ghi nhận vết kiểm toán bất biến (Append-Only) |
| **Business Config** | `8091` | `business_configuration_db` | Cấu hình tham số động có version kiểm toán |
| **Exception Mgmt** | `8092` | `exception_management_db` | Bảng điều hành xử lý lỗi & Chỉ số sự cố |

### 2. Giao diện Người dùng (Frontend Apps)

| Ứng dụng | Cổng Mặc định | Framework | Đối tượng sử dụng |
|---|---|---|---|
| **Storefront Web** (`user-web`) | `3000` | Next.js 15 (App Router) | Khách hàng mua sắm, quản lý giỏ hàng, thanh toán |
| **Backoffice Portal** (`admin-web`) | `5173` | React 19 + Vite 7 SPA | Quản trị viên, Vận hành viên, Chăm sóc khách hàng |

### 3. Middleware & Công cụ Quản trị Hạ tầng

| Thành phần | Port Host | Giao thức / Truy cập | Tài khoản / Thông tin |
|---|---|---|---|
| **PostgreSQL 16** | `5432` | TCP / JDBC | User: `postgres` \| Password: `postgres` |
| **Redis 7** | `6379` | TCP / Redis CLI | No Auth (môi trường dev local) |
| **Apache Kafka** | `9092` | TCP (PLAINTEXT_HOST) | Kafka Broker (Internal container: `29092`) |
| **Zookeeper** | `2181` | TCP | Điều phối Kafka cluster |
| **Kafka UI** | `8088` | HTTP `http://localhost:8088` | Giao diện web trực quan hóa Topics & Messages |
| **MinIO API** | `9000` | HTTP (S3 API) | User: `minioadmin` \| Pass: `minioadminpassword` |
| **MinIO Console**| `9001` | HTTP `http://localhost:9001` | Web UI quản trị Bucket & Object Storage |
| **Prometheus** | `9090` | HTTP `http://localhost:9090` | Hệ thống thu thập metrics từ Actuator |
| **Grafana** | `3000`* | HTTP `http://localhost:3000` | Visual Dashboards (*Tránh trùng port với user-web) |
| **Grafana Tempo**| `3200`, `4317`| HTTP (3200) / OTLP gRPC (4317)| Lưu trữ Distributed Traces |
| **OTel Collector**| `4318`, `8889`| HTTP (4318) / Metrics (8889) | Thu thập trace/metrics từ các services |

> [!NOTE]  
> Nếu bạn chạy `user-web` trên port `3000`, cổng container Grafana trong file `docker-compose.yml` có thể được ánh xạ lại thành `3001:3000` hoặc bạn có thể chạy `user-web` bằng lệnh `npm run dev -- -p 3002` để tránh xung đột cổng.

---

## 📋 Yêu cầu Tiên quyết (Prerequisites)

Để cài đặt và chạy toàn bộ hệ thống trên máy trạm cục bộ, bạn cần chuẩn bị:

1. **Java Development Kit (JDK)**: Phiên bản **17** trở lên (Khuyến nghị Eclipse Temurin 17 hoặc OpenJDK 17).
   ```bash
   java -version
   ```
2. **Gradle**: Phiên bản **8.x** hoặc **9.x** (đã cài đặt trên PATH).
   ```bash
   gradle -v
   ```
3. **Node.js**: Phiên bản **20.x** hoặc **22.x** (kèm `npm`).
   ```bash
   node -v
   npm -v
   ```
4. **Docker Desktop & Docker Compose**: Đảm bảo Docker Daemon đang chạy.
   ```bash
   docker --version
   docker compose version
   ```
5. **Cấu hình RAM khuyến nghị**: Tối thiểu **8GB RAM khả dụng** (16GB RAM tổng thể hệ thống) để chạy ổn định Docker Compose và các JVM microservices.

---

## 🚀 Hướng dẫn Cài đặt & Khởi chạy Từng bước

### Bước 1: Khởi động Hạ tầng Middleware (Docker Compose)

Di chuyển đến thư mục chứa file `docker-compose.yml` và khởi động toàn bộ hạ tầng:

```bash
# Di chuyển vào thư mục hạ tầng và khởi động các container nền
docker compose -f infra/docker-compose/docker-compose.yml up -d
```

Sau khi chạy lệnh trên, Docker sẽ tự động tải các image và khởi chạy:
- PostgreSQL 16 và tự động chạy script `01-create-databases.sql` tạo đủ 12 logical databases.
- Redis 7 sẵn sàng nhận kết nối port 6379.
- Zookeeper & Kafka 3.x sẵn sàng tại port 9092.
- Kafka UI tại `http://localhost:8088`.
- MinIO tại `http://localhost:9001`.
- Prometheus, Tempo, OTel Collector.

**Kiểm tra trạng thái các container**:
```bash
docker compose -f infra/docker-compose/docker-compose.yml up -d
```

---

### Bước 2: Build & Kiểm thử Toàn bộ Source Code

Tại thư mục gốc của dự án (`e-commerce_platform`), biên dịch và kiểm tra tính toàn vẹn của tất cả 15 subprojects:

```bash
# Biên dịch toàn bộ các class và test class
gradle testClasses

# Chạy toàn bộ bộ kiểm thử tự động của backend
gradle test
```

---

### Bước 3: Khởi chạy Backend Services

Để các service kết nối chính xác với Service Discovery và phân giải định tuyến, **thứ tự khởi động khuyến nghị** như sau:

#### 1. Khởi động Service Discovery (Eureka Server)
Mở một cửa sổ Terminal:
```bash
gradle :services:service-discovery:bootRun
```
*Truy cập bảng điều khiển Eureka*: `http://localhost:8761` để xem danh sách dịch vụ đăng ký.

#### 2. Khởi động API Gateway
Mở cửa sổ Terminal thứ hai:
```bash
gradle :services:api-gateway:bootRun
```
*API Gateway lắng nghe tại*: `http://localhost:8080`.

#### 3. Khởi động các Microservices Nghiệp vụ
Khởi chạy lần lượt các dịch vụ nghiệp vụ cần thiết (hoặc khởi chạy toàn bộ trong các tab Terminal riêng biệt):

```bash
# Nhóm 1: Định danh, Sản phẩm & Giá
gradle :services:identity-access-service:bootRun
gradle :services:catalog-service:bootRun
gradle :services:pricing-service:bootRun

# Nhóm 2: Giỏ hàng, Tồn kho & Đơn hàng (Core Transaction Flow)
gradle :services:cart-service:bootRun
gradle :services:inventory-service:bootRun
gradle :services:order-service:bootRun

# Nhóm 3: Thanh toán, Giao vận & Thông báo
gradle :services:payment-service:bootRun
gradle :services:fulfillment-service:bootRun
gradle :services:notification-service:bootRun

# Nhóm 4: Kiểm toán, Cấu hình & Ngoại lệ
gradle :services:audit-compliance-service:bootRun
gradle :services:business-configuration-service:bootRun
gradle :services:exception-management-service:bootRun
```

> [!TIP]  
> Các dịch vụ khi khởi động sẽ tự động kết nối PostgreSQL và chạy các script **Flyway Migration** để khởi tạo bảng dữ liệu và nạp dữ liệu mẫu ban đầu (seed roles, permissions).

---

### Bước 4: Khởi chạy Frontend Applications

#### 1. Cửa hàng Storefront (`apps/user-web`)
Mở Terminal mới và thực hiện:
```bash
cd apps/user-web
npm install
npm run dev
```
Storefront sẽ khả dụng tại: `http://localhost:3000` (hoặc cổng tiếp theo nếu 3000 bận).

#### 2. Cổng Quản trị Backoffice (`apps/admin-web`)
Mở Terminal mới và thực hiện:
```bash
cd apps/admin-web
npm install
npm run dev
```
Backoffice Portal sẽ khả dụng tại: `http://localhost:5173`.

---

## ⚙️ Biến Môi trường & Cấu hình Hệ thống

Tất cả các dịch vụ đều sử dụng cơ chế cấu hình linh hoạt thông qua biến môi trường với giá trị mặc định tương thích hoàn toàn với file `docker-compose.yml`:

| Tên biến môi trường | Giá trị mặc định (Local) | Mô tả |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | Profile Spring Boot đang kích hoạt |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/<db_name>` | Chuỗi kết nối JDBC tới DB riêng của service |
| `DATABASE_USERNAME` | `postgres` | Tài khoản kết nối cơ sở dữ liệu |
| `DATABASE_PASSWORD` | `postgres` | Mật khẩu kết nối cơ sở dữ liệu |
| `REDIS_HOST` | `localhost` | Địa chỉ máy chủ Redis |
| `REDIS_PORT` | `6379` | Cổng dịch vụ Redis |
| `KAFKA_BOOTSTRAP_SERVERS`| `localhost:9092` | Địa chỉ Kafka cluster |
| `JWT_SECRET` | `c3VwZXItc2VjcmV0LWtleS1mb3ItZGlzdHJpYnV0ZWQtZWNvbW1lcmNl` | Khóa bí mật ký SHA-256 JWT Token |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Địa chỉ API của dịch vụ MinIO |
| `MINIO_ACCESS_KEY` | `minioadmin` | Access key xác thực MinIO |
| `MINIO_SECRET_KEY` | `minioadminpassword` | Secret key xác thực MinIO |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | URL Gateway trỏ từ Storefront Web |
| `VITE_API_BASE_URL` | `http://localhost:8080` | URL Gateway trỏ từ Backoffice Portal |

---

## 👥 Tài khoản Mặc định & Phân quyền RBAC

Hệ thống tích hợp sẵn mô hình **Role-Based Access Control (RBAC)** với 5 vai trò quản trị viên tuân thủ mục 2.3.1 trong SRS:

| Mã Vai trò (Role Code) | Mô tả Vai trò | Quyền hạn chính (Permissions) |
|---|---|---|
| `SUPER_ADMIN` | Quản trị viên Tối cao | Toàn quyền cấu hình hệ thống, quản lý tài khoản, phân quyền, xem nhật ký kiểm toán |
| `CATALOG_ADMIN` | Quản trị Danh mục | Quản lý danh mục, sản phẩm, SKU, cấu hình biểu giá khuyến mãi |
| `INVENTORY_ADMIN` | Quản trị Kho vận | Điều chỉnh mức tồn kho, quản lý kho hàng, tra cứu lịch sử xuất nhập |
| `ORDER_OPS_ADMIN` | Điều hành Đơn hàng | Xử lý chuyển trạng thái đơn hàng, xác nhận đóng gói, vận chuyển, đối soát thanh toán |
| `CUSTOMER_SUPPORT` | Chăm sóc Khách hàng | Tra cứu thông tin đơn hàng, hỗ trợ khiếu nại, xem trạng thái thanh toán |

### Dữ liệu Seed Ban đầu:
- Flyway Migration `V1` và `V2` của `identity-access-service` đã tự động chèn sẵn 5 Vai trò và 17 Quyền hạn (`permission`) vào bảng dữ liệu.
- Bạn có thể đăng ký tài khoản khách hàng mới ngay trên giao diện Storefront hoặc qua API `POST /api/v1/customers/register`.

---

## 🔄 Kịch bản Trải nghiệm Mẫu (Quickstart Flow)

Dưới đây là chuỗi API mẫu để kiểm tra toàn bộ luồng hoạt động từ đầu đến cuối thông qua **API Gateway** (`http://localhost:8080`):

### 1. Đăng ký & Đăng nhập Khách hàng
```bash
# 1. Đăng ký khách hàng mới
curl -X POST http://localhost:8080/api/v1/customers/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "Password123!",
    "fullName": "Nguyen Van A"
  }'

# 2. Đăng nhập để lấy Access Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "Password123!"
  }'
```
*Lưu lại chuỗi `accessToken` trả về từ phản hồi để gán vào header `Authorization: Bearer <TOKEN>` cho các bước tiếp theo.*

### 2. Xem Danh mục Sản phẩm & Tra cứu Giá
```bash
# Tra cứu danh sách sản phẩm
curl -X GET http://localhost:8080/api/v1/products

# Tra cứu giá sản phẩm theo SKU
curl -X GET http://localhost:8080/api/v1/prices/sku-12345
```

### 3. Thao tác Giỏ hàng
```bash
# Thêm sản phẩm vào giỏ hàng
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "skuId": "SKU-IPHONE-15",
    "quantity": 1
  }'
```

### 4. Đặt hàng & Kích hoạt Luồng Saga
```bash
# Tạo đơn hàng từ giỏ hàng (Khởi tạo Saga Orchestration)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddressId": "address-uuid-here",
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {
        "skuId": "SKU-IPHONE-15",
        "quantity": 1
      }
    ]
  }'
```
*Hệ thống sẽ giữ hàng tồn kho (`INVENTORY RESERVED`), ghi nhận bản ghi Order ở trạng thái `RESERVED` và phát sự kiện `OrderPlacedEvent`.*

### 5. Xử lý Thanh toán (Idempotent Webhook Callback)
```bash
curl -X POST http://localhost:8080/api/v1/payments/process \
  -H "Authorization: Bearer <TOKEN>" \
  -H "X-Idempotency-Key: pay-key-001" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "<ORDER_ID>",
    "amount": 24990000.00,
    "paymentMethod": "CREDIT_CARD"
  }'
```
*Khi thanh toán thành công, đơn hàng tự động chuyển sang trạng thái `PAID`, sự kiện `OrderPaidEvent` kích hoạt quy trình giữ tồn kho chính thức và chuyển sang chuẩn bị đóng gói.*

---

## 📊 Công cụ Giám sát & Vận hành (Observability & Ops Tools)

Hệ thống đi kèm đầy đủ bộ công cụ observability phục vụ công tác giám sát hiệu năng và xử lý sự cố trong môi trường phân tán:

| Công cụ | Địa chỉ URL | Chức năng chính |
|---|---|---|
| **Eureka Registry** | `http://localhost:8761` | Kiểm tra trạng thái UP/DOWN của toàn bộ microservices |
| **Kafka UI** | `http://localhost:8088` | Xem chi tiết message trên các topics (`order.events`, `payment.events`, v.v.) |
| **MinIO Console** | `http://localhost:9001` | Quản lý buckets, ảnh sản phẩm và tài nguyên tĩnh |
| **Prometheus** | `http://localhost:9090` | Tra cứu metrics, latency, HTTP request rates từ Actuator |
| **Grafana Tempo** | `http://localhost:3200` | Tra cứu vết phân tán (Distributed Traces) qua `traceId` |
| **Health Check** | `http://localhost:8080/actuator/health` | Kiểm tra tình trạng hoạt động tổng thể của Gateway |

---

## 🧪 Hướng dẫn Chạy Kiểm thử (Testing Guide)

### 1. Kiểm thử Backend (JUnit 5 & AssertJ)
```bash
# Chạy toàn bộ test suites của 15 module
gradle test

# Chạy test riêng cho một module (ví dụ: order-service)
gradle :services:order-service:test

# Xem báo cáo HTML sau khi test xong
# Mở file: services/<service-name>/build/reports/tests/test/index.html
```

### 2. Kiểm thử Frontend (Vitest & React Testing Library)
```bash
# Kiểm thử Storefront
cd apps/user-web
npm run test

# Kiểm thử Backoffice Admin
cd apps/admin-web
npm run test
```

---

## 📚 Tài liệu Kiến trúc Bổ sung

Để tìm hiểu sâu hơn về kiến trúc phân tán, cơ chế giao tiếp liên dịch vụ, chi tiết máy trạng thái, mẫu hình Transactional Outbox và sơ đồ tuần tự (Sequence Diagrams) của từng luồng nghiệp vụ, vui lòng xem tài liệu chuyên sâu:

👉 **[TÀI LIỆU KIẾN TRÚC & LUỒNG HOẠT ĐỘNG TOÀN DIỆN (ARCHITECTURE.md)](ARCHITECTURE.md)**

---

*Hệ thống được phát triển và vận hành theo tiêu chuẩn kỹ thuật phân tán cao cấp.*  
*Bản quyền © 2026. Mọi quyền được bảo lưu.*
