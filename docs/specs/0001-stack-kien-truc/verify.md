# Verify: Stack & kiến trúc · spec 0001 · updated 2026-11-16
_Các bước lấy từ Done when của tính năng 1 và các quy ước trong spec 0001 (spec này không đánh số AC, nên mỗi bước ghi mục spec nó kiểm). `/check verify` chạy các bước này; `/test` khóa những bước lâu dài._

## Commands
- [ ] `cd backend` rồi `mvnw.cmd verify` (Docker đang chạy) → BUILD SUCCESS, 2 test xanh, `docs/api/openapi.json` được ghi lại và có schema `ErrorResponse` → Done when (build), Đồng bộ kiểu API
- [ ] `cd frontend`, `pnpm gen:api`, `pnpm typecheck`, `pnpm build` → cả ba chạy sạch, không cần backend đang chạy → Done when (build), Đồng bộ kiểu API
- [ ] `docker compose up -d` → service `db` (postgres:18.7) healthy, mở cổng 5433 trên máy → Chạy local
- [ ] `mvnw.cmd spring-boot:run` → log có `PostgreSQL 18.7`, `Started ChungCuApplication`; `curl http://127.0.0.1:8080/actuator/health` → `{"status":"UP"...}` → Done when (khởi động)
- [ ] Chạy backend với `JWT_SECRET` rỗng → dừng ngay, báo `Thiếu JWT_SECRET (khóa ký access token)` → Cấu hình
- [ ] `pnpm dev` rồi mở `http://localhost:3000` → trang có tiêu đề "Hệ thống Quản lý Chung cư" → Done when (khởi động)

## Đường gọi API và vỏ bọc lỗi
- [ ] `curl -i http://127.0.0.1:3000/api/v1/can-ho` (qua proxy Next) → 401, body `{"success":false,"code":401,"errorCode":"UNAUTHENTICATED","errors":[],"requestId":...}`, header `X-Request-Id` trùng `requestId` → Đường gọi API, Response lỗi
- [ ] Gửi `X-Request-Id: abc-123` → `requestId` là `abc-123`; gửi `X-Request-Id: <script>` → mã mới tự sinh → Theo dõi request
- [ ] `curl http://127.0.0.1:8080/api/v1/auth/login` (đường được cho qua, chưa có route) → 404 `NOT_FOUND` theo vỏ bọc → Bảo mật tạm thời, mã lỗi chung
- [ ] `/swagger-ui.html` và `/v3/api-docs` trả 200 ở `dev`; `/actuator/env` trả 401 → Bảo mật tạm thời, Actuator chỉ mở health
- [ ] Gọi thẳng `:8080` với header `Origin` → response không có `Access-Control-*` → Không bật CORS
- [ ] Response 401 có `timestamp` dạng ISO 8601 kết thúc bằng `Z` → Ngày giờ (Instant UTC)

## Bản demo
- [ ] `docker compose -f docker-compose.prod.yml up --build` → 3 container healthy; `http://localhost:3000` trả 200; `/api/v1/can-ho` qua frontend trả 401 theo vỏ bọc → Triển khai và demo
- [ ] `docker compose -f docker-compose.prod.yml exec backend id` và `... exec frontend id` → không phải root; `curl http://127.0.0.1:8080` từ máy bị từ chối (backend chỉ trong mạng Docker); log backend dạng JSON (ECS) → Nền tảng khác (Docker), Theo dõi

## Acceptance-criteria coverage
- Done when "quyết định được ghi trong một spec": spec 0001 có, Status `Accepted`
- Done when "khởi động được trên máy local": bước khởi động db, backend, frontend
- Done when "build thành công": `mvnw.cmd verify`, `pnpm build`, image demo
- Hợp đồng API chung, Bảo mật tạm thời, Theo dõi request, Cấu hình: các bước ở mục Đường gọi API và Commands
