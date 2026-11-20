# Verify: Làm mới phiên & đăng xuất · spec 0005 · updated 2026-11-20
_Steps derived from spec 0005 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: `docker compose up -d` (nếu cổng 5433 bận, đặt `POSTGRES_PORT=5434` và `DB_URL=jdbc:postgresql://localhost:5434/chungcu`), backend với `SPRING_PROFILES_ACTIVE=dev,seed`, `pnpm dev`. Tài khoản cư dân seed `0900000007` / `ChungCu@2026`.

## UI / manual
- [ ] Đăng nhập, chờ trang `/ho-so` tải xong, xóa riêng cookie `access_token` trong DevTools, tải lại `/ho-so` → trang hiện đủ, không đi qua `/dang-nhap`, tab Network có đúng một `POST refresh-token` trả 200 `lamMoi: true` kèm ba `Set-Cookie` → AC-12, AC-1, AC-10
- [ ] Như bước trên nhưng tải lại trang ngay lúc request `refresh-token` còn đang chạy (Network throttling "Slow 4G") → ghi lại kết quả; lần kiểm ngày 2026-11-20 từng thấy người dùng bị đưa về `het-phien` trong tình huống này (xem ghi chú cuối file) → AC-12
- [ ] Đặt tạm `access-token-ttl: PT1M`, mở hai tab `/ho-so`, chờ hơn 1 phút, thao tác ở cả hai tab → không tab nào bị đá ra; DB có đúng một dòng mới cho mỗi lần xoay → AC-5, AC-10
- [ ] Bấm họ tên ở thanh trên bằng bàn phím: Enter mở menu, mũi tên xuống đi giữa "Hồ sơ" và "Đăng xuất", Escape đóng và focus về nút, viền focus nhìn thấy được → AC-13
- [ ] Chặn `GET /api/v1/auth/profile` (DevTools "Block request URL"), tải lại → nút hiện "Tài khoản", menu vẫn mở và đăng xuất được → AC-13
- [ ] Chọn "Đăng xuất" → mục hiện "Đang đăng xuất…" và bị vô hiệu; về `/dang-nhap?ly-do=dang-xuat` với "Bạn đã đăng xuất." trong vùng `role="status"`; không còn cookie nào; gõ `/ho-so` thì về `/dang-nhap` → AC-14
- [ ] Tắt backend rồi chọn "Đăng xuất" → toast "Không đăng xuất được, vui lòng thử lại.", ở lại trang → AC-14
- [ ] Đăng xuất rồi bấm Back → trang cũ có thể hiện từ bfcache; mọi thao tác hay tải lại sau đó về `/dang-nhap` → AC-14

## Commands
- [ ] `cd backend && mvnw.cmd verify` → BUILD SUCCESS, gồm `LamMoiPhienIntegrationTest`, `LamMoiPhienDungLaiIntegrationTest`, `DangXuatIntegrationTest`, `DonPhienIntegrationTest`, `PhienDangNhapCleanupJobTest`, `AppPropertiesTest`; `docs/api/openapi.json` không đổi → AC-1 tới AC-9, AC-15, AC-16
- [ ] `cd frontend && pnpm test` → `refresh.test.ts`, `menu-nguoi-dung.test.tsx`, `login-form.test.tsx` xanh → AC-10, AC-11, AC-13, AC-14
- [ ] `psql`: `\d phien_dang_nhap` có `bat_dau_luc timestamptz not null`, `chk_phien_dang_nhap_bat_dau`, `uq_phien_dang_nhap_ho_con_hieu_luc` → AC-9
- [ ] `curl -X POST localhost:8080/api/v1/auth/refresh-token` không header `X-Requested-With` → 403 `CSRF_REJECTED`; có header, không cookie → 401 `AUTH_REFRESH_INVALID` kèm ba cookie `Max-Age=0` → AC-3, AC-7

## Value sourcing (mỗi dòng của bảng nguồn giá trị)
- [ ] Token cần kiểm từ cookie `refresh_token`: gửi token ở body hay header khác → vẫn 401 → AC-3
- [ ] Dòng phiên theo SHA-256 của cookie: `token_hash` của dòng mới bằng SHA-256 của cookie mới → AC-1
- [ ] Giờ từ `Clock`: test tua `MutableClock` (T + 6, 12, 18, 24, 29, 30 ngày 1 giây) cho đúng `het_han_luc` → AC-2
- [ ] Khoảng khoan dung `refresh-reuse-grace`: dùng lại token cũ ở giây 5 và giây 10 → `lamMoi: false`; giây 11 → thu hồi cả họ → AC-4, AC-5
- [ ] `bat_dau_luc` chép từ dòng cũ qua mọi lần xoay; `het_han_luc` không vượt `bat_dau_luc + 30 ngày` → AC-2, AC-9
- [ ] `now` cắt tới micro giây: đồng hồ có phần nano giây, còn 0,5 giây tới hạn → 200, `Max-Age=1`, không lỗi CHECK → AC-2
- [ ] `Max-Age` của `refresh_token` và `phien` bằng thời gian còn lại (làm mới lúc T + 29 ngày → 86400) → AC-2
- [ ] `accessTokenHetHanLuc` = giờ làm mới + 15 phút, bằng `exp` của JWT; `null` ở nhánh khoan dung → AC-1, AC-5
- [ ] Khóa theo họ: đăng xuất bằng T1 song song làm mới bằng T2, lặp 10 lần → họ không còn dòng chưa thu hồi → AC-8
- [ ] `sid` của JWT mới = `ma_ho_phien` cũ; `vaiTro` đọc lại DB (thêm vai trò bằng SQL rồi làm mới → JWT có vai trò mới) → AC-1
- [ ] `hoTen`, trạng thái tài khoản đọc từ `nguoi_dung_he_thong`: đổi sang `Tạm khóa` → 403 `AUTH_ACCOUNT_LOCKED`, họ bị thu hồi `Tài khoản bị khóa` → AC-6
- [ ] `dia_chi_ip`, `user_agent` của dòng mới là của request làm mới, không chép từ dòng cũ → AC-1
- [ ] Đăng xuất thu hồi đúng họ của cookie; họ khác (máy khác) còn nguyên → AC-8
- [ ] `ly_do_thu_hoi` theo nhánh: `Đã xoay vòng`, `Dùng lại token cũ`, `Đăng xuất`, `Tài khoản bị khóa` → AC-1, AC-4, AC-6, AC-8
- [ ] Đăng nhập ghi `bat_dau_luc = tao_luc` → AC-9
- [ ] Job dọn dẹp xóa theo mốc `now − session-retention`: dòng 31 ngày bị xóa, 29 ngày còn → AC-15
- [ ] Lịch job `0 0 3 * * *` theo `app.timezone`; `session-retention` < `session-max-lifetime` thì backend không khởi động → AC-15
- [ ] Client chỉ làm mới khi 401 `UNAUTHENTICATED`, không phải ba đường auth, và không đang đăng xuất → AC-10, AC-14
- [ ] Chờ 400 ms trước khi gửi lại khi `lamMoi: false` → AC-10
- [ ] Body gửi lại của request ghi đúng như bản gốc → AC-10
- [ ] Thông báo sau đăng xuất từ `ly-do=dang-xuat` → AC-14
- [ ] Họ tên trên nút menu từ query `["ho-so"]` → AC-13

## Acceptance-criteria coverage
- AC-1 · Commands 1, Value sourcing 2, 8, 10, 12, 14 · AC-2 · Value sourcing 3, 5, 6, 7 · AC-3 · Commands 4, Value sourcing 1 · AC-4 · Value sourcing 4 · AC-5 · UI 3, Value sourcing 4 · AC-6 · Value sourcing 11 · AC-7 · Commands 4 · AC-8 · Value sourcing 9, 13 · AC-9 · Commands 3, Value sourcing 15 · AC-10 · UI 1, Value sourcing 18 tới 20 · AC-11 · Commands 2 · AC-12 · UI 1, 2 · AC-13 · UI 4, 5 · AC-14 · UI 6 tới 8 · AC-15 · Value sourcing 16, 17 · AC-16 · Commands 1

## Ghi chú từ lần kiểm ngày 2026-11-20 (/develop)
- AC-12 qua khi lời gọi làm mới hoàn tất trước khi trang điều hướng. Hai lần đầu, khi xóa `access_token` ngay lúc trang `/ho-so` vừa đổi URL rồi tải lại, người dùng bị đưa về `/dang-nhap?ly-do=het-phien`: DB cho thấy token đã được xoay (dòng mới `con`), nhưng trình duyệt vẫn giữ token cũ, nên hai lần làm mới sau đều nhận `lamMoi: false` và request gửi lại vẫn 401. Nguyên nhân rất có thể: trang tải lại hủy response làm mới khi server đã commit, nên `Set-Cookie` không tới trình duyệt. Chưa tái hiện được một cách chắc chắn bằng Playwright.
