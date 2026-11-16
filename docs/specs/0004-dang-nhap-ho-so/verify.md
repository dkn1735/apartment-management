# Verify: Đăng nhập & trang hồ sơ · spec 0004 · updated 2026-11-16
_Steps derived from spec 0004 acceptance criteria and its Value sourcing table. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: `docker compose up -d`; backend với `SPRING_PROFILES_ACTIVE=dev,seed`; `pnpm dev` trong `frontend/`. Mật khẩu mọi tài khoản seed: `ChungCu@2026`.

## UI / manual
- [x] Trình duyệt mới (không cookie), mở `http://localhost:3000/ho-so?tab=1` → chuyển ngay tới `/dang-nhap?next=%2Fho-so%3Ftab%3D1`, trang hồ sơ không render trước → AC-13
- [x] Mở `/` khi chưa đăng nhập → về `/dang-nhap?next=%2F`; sau khi đăng nhập thì `/` chuyển tới `/ho-so` → AC-13
- [x] Mở `/dang-nhap` và `/dev/ui` khi chưa đăng nhập → không bị chuyển đi đâu → AC-13
- [x] Trang `/dang-nhap`: ô "Số điện thoại hoặc email" có `autocomplete="username"`, ô "Mật khẩu" có `autocomplete="current-password"`, nút hiện/ẩn có nhãn "Hiện mật khẩu" và `aria-pressed` đổi khi bấm → AC-12
- [x] Chỉ dùng bàn phím (Tab, Enter, Space): điền form, bật/tắt hiện mật khẩu, gửi; vòng focus luôn thấy rõ; axe (DevTools) không báo lỗi → AC-12
- [x] Gửi form trống → lỗi "Vui lòng nhập số điện thoại hoặc email" và "Vui lòng nhập mật khẩu" dưới từng ô → AC-12
- [x] Đăng nhập `0900000007` sai mật khẩu → `FormAlert` "Số điện thoại, email hoặc mật khẩu không đúng." nhận focus, ô mật khẩu trống, ô tài khoản giữ nguyên → AC-12, AC-3
- [x] Đăng nhập `0900000008` đúng mật khẩu → `FormAlert` báo tài khoản bị khóa; không có cookie mới → AC-12, AC-4
- [x] Đăng nhập `0900000007` từ `/dang-nhap?next=%2Fho-so%3Ftab%3D1` → về `/ho-so?tab=1`; với `next=%2F%2Fevil.com` hay `next=https%3A%2F%2Fevil.com` → về `/ho-so` → AC-12
- [x] Đăng nhập bằng `  CuDan@ChungCu.Test ` và bằng `+84 900 000 007` → đều vào được hồ sơ của Vũ Cư Dân → AC-2
- [x] DevTools > Application > Cookies sau khi đăng nhập: `access_token` (Path `/`, 15 phút), `refresh_token` (Path `/api/v1/auth`, 7 ngày), `phien` (giá trị `1`, Path `/`, 7 ngày), cả ba `HttpOnly`, `SameSite=Strict`; Network: body của `/login` không có token → AC-1
- [x] `/ho-so` với `0900000006`: menu chỉ có "Hồ sơ" đang chọn, góc phải hiện "Đặng Ban Quản Trị"; ba phần Thông tin tài khoản, Phạm vi (tòa `A`, căn hộ `A-0101`), Vai trò (Ban quản trị với Nhiệm kỳ `2026-2028`, Vai trò trong ban quản trị; Cư dân với Mã cư dân); không có chữ CCCD → AC-14, AC-8
- [x] `/ho-so` với `0900000002` → Tòa nhà phụ trách `A`, Căn hộ cư trú "Không có"; với `0900000007` → tòa "Không có", căn hộ `A-0201` → AC-8, AC-14
- [x] Throttle mạng (Slow 3G), tải lại `/ho-so` → skeleton với chữ ẩn "Đang tải hồ sơ"; tắt backend rồi tải lại → "Chưa tải được hồ sơ" với câu "Không kết nối được tới máy chủ…" và nút "Thử lại", không có dòng "Mã tra cứu" (Next.js trả 500 thuần, không có `X-Request-Id`); bật lại backend, bấm "Thử lại" → hồ sơ hiện ra → AC-14
- [x] Đang đăng nhập, xóa riêng cookie `access_token` trong DevTools, tải lại `/ho-so?tab=1` → chuyển đúng một lần tới `/dang-nhap?next=%2Fho-so%3Ftab%3D1&ly-do=het-phien`, hiện "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.", không lặp → AC-15, AC-13
- [x] Đang đăng nhập bằng tài khoản thử, đổi `trang_thai_tai_khoan` sang `Tạm khóa` bằng SQL, tải lại `/ho-so` → chuyển tới `/dang-nhap?ly-do=khoa`, hiện thông báo bị khóa, ba cookie bị xóa → AC-15, AC-9
- [x] `/dang-nhap?ly-do=abc` → không hiện thông báo nào → AC-12
- [x] Đăng nhập sai ở `/dang-nhap` không bao giờ tự chuyển trang (lỗi của chính form) → AC-15

## Commands
- [x] `curl -i -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"taiKhoan":"0900000007","matKhau":"ChungCu@2026"}'` (không có `X-Requested-With`) → 403 `CSRF_REJECTED` → AC-10
- [x] Cùng lệnh thêm `-H 'X-Requested-With: xmlhttprequest'` (chữ thường) → 403; với `XMLHttpRequest` → 200 → AC-10
- [x] `curl -i localhost:8080/api/v1/auth/profile` không cookie → 401 JSON `UNAUTHENTICATED` (không phải body rỗng); với `-H 'Authorization: Bearer <access_token>'` thay cho cookie → vẫn 401 → AC-7
- [x] `curl` đăng nhập kèm `-b 'access_token=rac'` → vẫn 200; `curl -b 'access_token=rac' localhost:8080/actuator/health` → 200 → AC-7
- [x] Giải mã `access_token` (jwt.io, chỉ dùng tài khoản seed): header `alg: HS256`; claim `iss=chungcu`, `sub=ND...`, `sid` là UUID, `vaiTro`, `jti`, `exp - iat = 900` → AC-7
- [x] SQL sau một lần đăng nhập: `SELECT token_hash, tao_luc, het_han_luc, dia_chi_ip, user_agent FROM phien_dang_nhap ORDER BY ma_phien DESC LIMIT 1` → `token_hash` bằng SHA-256 hex của cookie `refresh_token`, `het_han_luc - tao_luc = 7 days`, có IP và user agent; `dang_nhap_cuoi_luc` đổi, `phien_ban` không đổi → AC-6
- [x] SQL: `SELECT mat_khau_hash FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000003'` trước và sau lần đăng nhập đầu → `{bcrypt}$2a$10$...` thành `{bcrypt}$2a$12$...`, đăng nhập lần hai vẫn được → AC-6
- [x] Mật khẩu 129 ký tự hay `taiKhoan` rỗng → 400 `VALIDATION_FAILED` có `errors[].field`; mật khẩu 40 ký tự `ệ` → 401 → AC-5
- [x] Đặt `JWT_SECRET` 31 ký tự, rồi bằng chuỗi mẫu `doi-thanh-chuoi-ngau-nhien-du-dai`, rồi xóa hẳn → backend không khởi động, lỗi nêu rõ `JWT_SECRET` → AC-11
- [x] Chạy profile `prod` (hoặc đặt `COOKIE_SECURE=true`) → ba cookie có `Secure`; `COOKIE_SECURE=false` → không có → AC-11
- [x] Log backend sau các lần đăng nhập đúng, sai, bị khóa, lỗi validation → mỗi lần một dòng INFO có `maNguoiDung` (hoặc `lyDo=SAI_THONG_TIN` / `TAI_KHOAN_KHOA`); không có mật khẩu, token, hash hay số điện thoại, email đã gõ → AC-16
- [x] `cd backend && mvnw.cmd verify` → BUILD SUCCESS; `cd frontend && pnpm typecheck && pnpm lint && pnpm test && pnpm build` → đều qua → all

## Value sourcing (đổi đầu vào ở chỗ dễ sai)
- [x] Phạm vi căn hộ theo ngày Việt Nam: tạo hồ sơ cư trú `Hợp lệ` kết thúc hôm qua (giờ Việt Nam), xem `/profile` lúc 00:30 giờ Việt Nam → căn hộ đó không còn được tính (theo UTC sẽ bị tính nhầm) → AC-8
- [x] Hồ sơ cư trú `Hết hiệu lực`, hay bắt đầu từ ngày mai → không được tính → AC-8
- [x] `phamVi.maToaNha` lấy toàn bộ `toa_nha` cho nhân viên: thêm một tòa `B` bằng SQL → `/profile` của `0900000002` có `["A","B"]`; của `0900000007` vẫn rỗng → AC-8
- [x] Vai trò đọc từ DB chứ không từ token: xóa dòng `thanh_vien_ban_quan_tri` của tài khoản thử sau khi đăng nhập → `/profile` bỏ `BAN_QUAN_TRI` ngay, claim `vaiTro` trong token cũ vẫn còn tới khi hết hạn → AC-8
- [x] `user_agent` dài 600 ký tự → lưu đúng 500; không gửi `User-Agent` → `null` → AC-6
- [x] `dia_chi_ip` qua Next.js là IP trình duyệt (từ `X-Forwarded-For`), gọi thẳng cổng 8080 là IP người gọi → AC-6
- [x] Hash `{noop}ChungCu@2026` đặt bằng SQL cho tài khoản thử → đăng nhập bằng `ChungCu@2026` trả 401 → AC-3
- [x] Tài khoản không tồn tại và sai mật khẩu: cùng mã, cùng câu, thời gian phản hồi xấp xỉ nhau (trừ tài khoản seed còn hash cost 10, đã ghi ở Consequences) → AC-3

## Acceptance-criteria coverage
- AC-1 … cookie và body · AC-2 … email chữ hoa, `+84` · AC-3 … sai thông tin, `{noop}`, thời gian · AC-4 … `0900000008` · AC-5 … validation, 72 byte · AC-6 … SQL phiên, nâng hash, user agent, IP · AC-7 … curl 401, token cũ, giải mã JWT · AC-8 … ba tài khoản seed, phạm vi theo ngày, tòa B · AC-9 … khóa giữa chừng · AC-10 … curl CSRF · AC-11 … `JWT_SECRET`, `COOKIE_SECURE` · AC-12 … form, bàn phím, `next` · AC-13 … `proxy.ts` · AC-14 … trang hồ sơ · AC-15 … xóa `access_token`, khóa, `ly-do` · AC-16 … log
