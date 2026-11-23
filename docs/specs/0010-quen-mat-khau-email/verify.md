# Verify: Quên mật khẩu qua email · spec 0010 · updated 2026-11-23
_Steps derived from spec 0010 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: `docker compose up -d` (PostgreSQL và Mailpit), backend `mvnw.cmd spring-boot:run` với profile `dev,seed`, frontend `pnpm dev`. Thư xem ở http://localhost:8025. Tài khoản mẫu: `cudan@chungcu.test` (0900000007), mật khẩu `ChungCu@2026`. Đặt lại về đúng `ChungCu@2026` để không đổi dữ liệu mẫu (trùng mật khẩu hiện tại được phép, AC-9).

## UI / manual
- [x] `/dang-nhap` có link "Quên mật khẩu?" tới `/quen-mat-khau`; mở `/quen-mat-khau` khi chưa đăng nhập thì không bị chuyển về `/dang-nhap` → AC-13
- [x] Nhập `  CuDan@ChungCu.Test ` rồi bấm Enter → câu phản hồi chung trong vùng `role="status"` kèm "Link có hiệu lực 30 phút.", focus nằm ở vùng đó → AC-2, AC-13
- [x] Mailpit có đúng một thư tới `cudan@chungcu.test`, tiêu đề "Đặt lại mật khẩu tài khoản chung cư", người gửi là `MAIL_FROM`; cả phần chữ thường và HTML có link `http://localhost:3000/dat-lai-mat-khau#token=<43 ký tự>`; giờ hết hạn theo giờ Việt Nam, bằng lúc gửi cộng 30 phút → AC-1
- [x] Nhập email không có tài khoản, và email của một tài khoản `Tạm khóa` → cùng câu, Mailpit không có thư mới → AC-2
- [x] Gửi lại ngay cho `cudan@chungcu.test` trong vòng 60 giây → cùng câu, không có thư mới → AC-4
- [x] Mở link → thanh địa chỉ không còn `#token=...`; trang hiện "Link có hiệu lực tới HH:mm ngày dd/MM/yyyy", focus ở tiêu đề → AC-13, AC-15
- [x] Nhập hai ô không khớp → lỗi "Mật khẩu nhập lại chưa khớp" ở ô thứ hai, không có request `confirm` → AC-13
- [x] Nhập `ngan` ở cả hai ô → lỗi ở ô "Mật khẩu mới"; link vẫn dùng được sau đó → AC-9
- [x] Đăng nhập trên một máy khác (hoặc tab ẩn danh) trước, rồi đặt mật khẩu mới qua link → chuyển tới `/dang-nhap?ly-do=dat-lai-mat-khau` với câu "Đã đặt lại mật khẩu..."; máy kia bị đăng xuất ở lần làm mới phiên tiếp theo → AC-7
- [x] Mở lại đúng link cũ → "Không dùng được link này" kèm câu lỗi chung và link "Yêu cầu link mới" → AC-6, AC-8
- [x] Tải lại trang `/dat-lai-mat-khau` sau khi đã mở link → câu lỗi chung (token đã bị xóa khỏi URL) → AC-13
- [x] Yêu cầu link, rồi đợi hơn 61 giây và yêu cầu lần nữa → link đầu báo lỗi chung, link sau dùng được → AC-5
- [x] Yêu cầu link, rồi Admin khóa tài khoản đó (hoặc sửa email, hoặc đặt lại mật khẩu) → link báo lỗi chung → AC-10
- [x] `/nhat-ky` có nhãn "Yêu cầu đặt lại mật khẩu qua email" và "Đặt lại mật khẩu qua email" trong bộ lọc; dòng yêu cầu không có người thực hiện, dòng đặt lại có người thực hiện là chính tài khoản và `soPhienThuHoi` → AC-11
- [x] Làm hết hai trang chỉ bằng bàn phím (Tab, Enter, Space trên nút hiện mật khẩu); focus luôn nhìn thấy được → AC-13

## Commands
- [x] `curl -sI http://localhost:3000/dat-lai-mat-khau | grep -i referrer` → `Referrer-Policy: no-referrer` → AC-15
- [x] `docker exec project2-db-1 psql -U chungcu -d chungcu -c "SELECT token_hash, tao_luc, het_han_luc, da_dung_luc, huy_luc FROM yeu_cau_dat_lai_mat_khau ORDER BY ma_yeu_cau DESC LIMIT 3"` → chỉ có hash 64 ký tự hex, `het_han_luc = tao_luc + 30 phút`, dòng đã dùng có `da_dung_luc`, dòng bị thay có `huy_luc` → AC-1, AC-5, AC-8
- [x] Dừng Mailpit (`docker compose stop mailpit`) rồi yêu cầu link → trang vẫn hiện câu chung ngay; log backend có dòng ERROR "Gửi email đặt lại mật khẩu thất bại: maYeuCau=..." không kèm token, link hay email → AC-3
- [x] Gọi `POST /api/v1/auth/password-reset/check` với `{"token":"sai"}` và header `X-Requested-With: XMLHttpRequest` → 400 `AUTH_RESET_TOKEN_INVALID` → AC-6
- [x] Khởi động backend với `SPRING_PROFILES_ACTIVE=prod` mà thiếu lần lượt `MAIL_HOST`, `MAIL_FROM`, `APP_PUBLIC_URL` → dừng lúc khởi động, thông báo nêu tên biến → AC-14
- [x] `cd backend && mvnw.cmd test -Dtest="QuenMatKhau*,EmailDatLaiMatKhauListenerTest,AppPropertiesTest,MailCauHinhProdTest"` → xanh (gồm 10 request song song, hai `confirm` song song, trần chung 100/giờ, job dọn 7 ngày) → AC-4, AC-8, AC-12, AC-14
- [x] `cd frontend && pnpm vitest run src/app/quen-mat-khau src/app/dat-lai-mat-khau src/app/dang-nhap src/proxy.test.ts` → xanh → AC-13

## Demo với Gmail (trước buổi demo)
- [ ] Bật xác minh hai bước cho tài khoản Gmail gửi và tạo App Password; đặt `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` (trùng `MAIL_USERNAME`) và `APP_PUBLIC_URL` (địa chỉ mà máy người dùng mở được) trong `.env` gốc
- [ ] `docker compose -f docker-compose.prod.yml up --build`; Admin sửa email của một tài khoản mẫu sang một hộp thư thật, rồi yêu cầu link từ `/quen-mat-khau` → thư tới hộp thư thật (xem cả thư mục thư rác), link mở được trên máy người dùng
- [ ] Demo qua `http` trong mạng LAN: link vẫn chạy, nhớ đặt `COOKIE_SECURE=false` để đăng nhập lại được

## Value sourcing
- [x] Email gõ hoa thường lẫn lộn, có khoảng trắng vẫn khớp tài khoản (chuẩn hóa `strip` + viết thường) → AC-2
- [x] Đổi `APP_PUBLIC_URL` thành `http://192.168.1.10:3000/` (có `/` cuối) → link trong thư là `http://192.168.1.10:3000/dat-lai-mat-khau#token=...`, không có hai dấu `/`, không lấy từ header `Host` → AC-1, AC-14
- [x] Giờ hết hạn trong thư và trên trang là giờ Việt Nam (UTC+7), không phải UTC → AC-1, AC-13
- [x] `soPhienThuHoi` trong nhật ký bằng số máy đang đăng nhập trước khi đặt lại → AC-11

## Acceptance-criteria coverage
- AC-1: thư Mailpit, DB, value sourcing · AC-2: email không có tài khoản, tạm khóa, chuẩn hóa · AC-3: dừng Mailpit · AC-4: gửi lại trong 60 giây, test song song · AC-5: yêu cầu hai lần, DB · AC-6: link cũ, check sai · AC-7: đặt lại khi có máy khác đăng nhập · AC-8: link đã dùng, test song song · AC-9: mật khẩu ngắn không tiêu token · AC-10: khóa, sửa email, đặt lại · AC-11: `/nhat-ky` · AC-12: test job dọn · AC-13: hai trang, bàn phím, tải lại · AC-14: thiếu biến prod, URL gốc · AC-15: header, fragment bị xóa
