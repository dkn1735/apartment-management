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

## Database và dữ liệu mẫu

Backend khởi động là Flyway tự tạo schema từ `backend/src/main/resources/db/migration` ([spec 0002](docs/specs/0002-mo-hinh-du-lieu/index.md)). Không sửa file `V<n>` đã chạy; thay đổi schema là một file `V<n>` mới.

Dữ liệu mẫu nằm ở `db/seed` và chỉ được nạp khi bật profile `seed`: `SPRING_PROFILES_ACTIVE=dev,seed` trong `backend/.env` (có sẵn trong bản mẫu), `prod,seed` trong `docker-compose.prod.yml`. Test không bật seed.

Mọi tài khoản mẫu dùng chung mật khẩu **`ChungCu@2026`**. Mật khẩu này công khai trong repo, nên chỉ bật seed trên máy cá nhân, không bao giờ trên máy chủ mở ra Internet.

| Số điện thoại | Email                | Vai trò                      |
| ------------- | -------------------- | ---------------------------- |
| 0900000001    | admin@chungcu.test   | Admin                        |
| 0900000002    | bql@chungcu.test     | Ban quản lý                  |
| 0900000003    | kythuat@chungcu.test | Kỹ thuật                     |
| 0900000004    | ketoan@chungcu.test  | Kế toán                      |
| 0900000005    | baove@chungcu.test   | Bảo vệ                       |
| 0900000006    | bqt@chungcu.test     | Cư dân và Ban quản trị       |
| 0900000007    | cudan@chungcu.test   | Cư dân                       |
| 0900000008    | bikhoa@chungcu.test  | Cư dân, tài khoản `Tạm khóa` |

Seed chỉ thêm dòng còn thiếu, không cập nhật dòng đã có. Muốn làm sạch database và nạp lại từ đầu: `docker compose down -v` (xóa volume dữ liệu dev), rồi `docker compose up -d` và chạy lại backend. Với bản demo: `docker compose -f docker-compose.prod.yml down -v`.

## Đăng nhập

Mở http://localhost:3000, trang được bảo vệ sẽ chuyển về `/dang-nhap`. Đăng nhập bằng số điện thoại hoặc email của một tài khoản mẫu ở bảng trên, mật khẩu `ChungCu@2026` (cần bật profile `seed`). Sau khi đăng nhập bạn vào `/ho-so`. Tài khoản `0900000008` đang `Tạm khóa`, dùng để thử thông báo bị khóa. Cách hoạt động nằm ở [spec 0004](docs/specs/0004-dang-nhap-ho-so/index.md).

- Token nằm trong cookie `HttpOnly` (`access_token` sống 15 phút, `refresh_token` và `phien` sống 7 ngày); JavaScript không đọc được. Chưa có làm mới phiên và đăng xuất (tính năng 6): sau 15 phút bạn được đưa về trang đăng nhập; muốn đổi tài khoản thì đăng nhập lại ở `/dang-nhap`.
- `JWT_SECRET` phải dài ít nhất 32 byte và khác chuỗi mẫu trong `.env.example`, nếu không backend dừng lúc khởi động. Sinh một khóa bằng `openssl rand -base64 48`.
- `COOKIE_SECURE`: cookie có cờ `Secure` theo mặc định (prod), dev tắt sẵn. Chạy bản demo qua `http://` trên một máy khác `localhost` (ví dụ mạng LAN) thì đặt `COOKIE_SECURE=false` trong `.env`, nếu không trình duyệt bỏ cookie và bạn không đăng nhập được.
- Mọi request `POST`, `PUT`, `PATCH`, `DELETE` tới `/api/**` cần header `X-Requested-With: XMLHttpRequest` (chống CSRF). Hàm `api` ở frontend tự gắn. Trong Swagger, bấm **Authorize**, nhập `XMLHttpRequest` vào `csrfHeader` một lần trước khi thử. Gọi bằng curl hay Postman thì tự thêm header này.

## Đồng bộ kiểu API

1. `cd backend` rồi `mvnw.cmd verify` (cần Docker đang chạy): test `OpenApiExportTest` ghi `docs/api/openapi.json`
2. `cd frontend` rồi `pnpm gen:api`: sinh `src/lib/api/schema.d.ts`

Commit cả hai file kèm thay đổi API.

## Chạy bản demo

`docker compose -f docker-compose.prod.yml up --build`, mở http://localhost:3000. Cần `.env` ở gốc có `POSTGRES_PASSWORD` và `JWT_SECRET`.

## Kiểm tra code

- Frontend: `pnpm lint`, `pnpm format:check`, `pnpm typecheck` (sửa nhanh: `pnpm lint:fix`, `pnpm format`)
- Backend: `mvnw.cmd verify` đã gồm kiểm tra định dạng; sửa nhanh bằng `mvnw.cmd spring-javaformat:apply`
- Trước mỗi commit, Lefthook tự chạy các kiểm tra trên. Hook được cài khi bạn chạy `pnpm install` trong `frontend/`.
- CI (`.github/workflows/ci.yml`) chạy lại tất cả khi push lên `main` và khi mở PR, kể cả kiểm tra `openapi.json` và `schema.d.ts` có bị quên cập nhật không.
