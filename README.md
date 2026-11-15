# Hệ thống Quản lý Chung cư

Đồ án IT3930. Backend Spring Boot 4.1 (Java 25), frontend Next.js 16, PostgreSQL 18. Quyết định stack và kiến trúc nằm ở [spec 0001](docs/specs/0001-stack-kien-truc/index.md).

## Cần có trên máy

Java 25, Node.js 24, pnpm 12.17.0 (`npm i -g pnpm@12.17.0`), Docker Desktop.

## Chạy local

1. Tạo file cấu hình từ các bản mẫu và điền mật khẩu, khóa của bạn (ba file này không commit):
   - `.env.example` → `.env`
   - `backend/.env.example` → `backend/.env` (mật khẩu DB phải trùng `POSTGRES_PASSWORD`)
   - `frontend/.env.example` → `frontend/.env.local`
2. Bật PostgreSQL (cổng 5433 trên máy): `docker compose up -d`
3. Chạy backend: `cd backend` rồi `mvnw.cmd spring-boot:run` (http://localhost:8080, Swagger ở `/swagger-ui.html`)
4. Chạy frontend: `cd frontend`, `pnpm install`, rồi `pnpm dev` (http://localhost:3000)

Trình duyệt chỉ gọi `http://localhost:3000/api/v1/...`; Next.js chuyển tiếp sang backend, nên backend không bật CORS.

## Đồng bộ kiểu API

1. `cd backend` rồi `mvnw.cmd verify` (cần Docker đang chạy): test `OpenApiExportTest` ghi `docs/api/openapi.json`
2. `cd frontend` rồi `pnpm gen:api`: sinh `src/lib/api/schema.d.ts`

Commit cả hai file kèm thay đổi API.

## Chạy bản demo

`docker compose -f docker-compose.prod.yml up --build`, mở http://localhost:3000. Cần `.env` ở gốc có `POSTGRES_PASSWORD` và `JWT_SECRET`.
