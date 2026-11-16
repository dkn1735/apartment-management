# 0004. Đăng nhập và trang hồ sơ: lý do và các phương án

Bản ghi quyết định cho [index.md](index.md). `/develop` không cần đọc file này.

## Context

Đây là lát cắt đầu tiên chạy thật xuyên DB, API và giao diện, và là spec gốc của phân hệ xác thực. Tính năng 6 (làm mới phiên, đăng xuất), 7 (phân quyền), 10 (đổi mật khẩu), 11 (khóa tài khoản), 12 (khóa tạm) và 13 (quên mật khẩu) đều dựa vào cách cấp token, tên cookie, mã vai trò và cách bảo vệ trang chốt ở đây. Sai ở đây thì phải sửa ở mọi lát sau.

Những gì đã cố định trước spec này:
- Spec 0001: backend Spring Boot tự ký JWT (OAuth2 Resource Server), refresh token lưu trong DB, trình duyệt gọi `/api` cùng tên miền với Next.js qua `rewrites` nên không có CORS và dùng được cookie `HttpOnly`. Biến `JWT_SECRET` đã có, là chuỗi thường.
- Spec 0002: bảng `phien_dang_nhap` (lưu SHA-256 của refresh token, có `ma_ho_phien` để nhóm các token xoay vòng của một lần đăng nhập), `mat_khau_hash` dạng `{id}hash` của `DelegatingPasswordEncoder`, seed 8 tài khoản dùng bcrypt cost 10, một người có thể giữ nhiều vai trò (mỗi vai trò một bảng con).
- File đặc tả API: cookie `access_token` (Path `/`) và `refresh_token` (Path `/api/v1/auth`) đều `HttpOnly; Secure; SameSite=Strict`; mã lỗi `AUTH_INVALID_CREDENTIALS`, `AUTH_ACCOUNT_LOCKED`; sai tài khoản và sai mật khẩu phải trả cùng thông báo; profile có `phamVi` theo tòa nhà hoặc căn hộ.

Các lực còn lại:
- Nhóm 2 người, đồ án môn học, chạy demo trên máy cá nhân; không có dịch vụ phụ nào ngoài PostgreSQL và nên giữ như vậy.
- Mức GA: dữ liệu cá nhân (số điện thoại, email, CCCD) và quyền truy cập nên phần bảo mật phải đúng ngay từ đầu.
- Cookie `refresh_token` có Path `/api/v1/auth`, nên khi người dùng mở một trang thường, máy chủ Next.js không thấy nó; chỉ thấy `access_token` sống 15 phút.
- Schema không có bảng gán nhân viên vào tòa nhà; chỉ `ke_toan.pham_vi_phu_trach` là chuỗi tự do.
- File đặc tả không định nghĩa danh sách giá trị của `vaiTro`.

## Options considered

### Option 1: JWT ngắn hạn trong cookie HttpOnly, refresh token trong DB, cookie dấu phiên cho proxy (chọn)

Access token JWT 15 phút và refresh token ngẫu nhiên 7 ngày, cả hai trong cookie `HttpOnly`. Backend kiểm JWT không cần DB. Thêm cookie `phien` (không chứa bí mật, sống như refresh token, Path `/`) để `proxy.ts` biết người dùng có phiên mà không cần đọc refresh token.

**Pros**:
- Đúng hướng spec 0001 và file đặc tả; dùng lại bảng đã có, không thêm dịch vụ.
- Token không lộ qua XSS; kiểm token không tốn truy vấn.
- `proxy.ts` hoạt động đúng cả khi tính năng 6 bật làm mới âm thầm, không phải sửa lại.

**Cons**:
- Khóa hay đổi vai trò chậm tối đa 15 phút ở các API dựa vào claim.
- Ba cookie thay vì hai; cookie `phien` chỉ là gợi ý, không chứng minh được gì.

### Option 2: Phiên phía máy chủ (Spring Session JDBC), một cookie session

Một cookie ID phiên; mỗi request tra bảng phiên trong DB.

**Pros**:
- Thu hồi tức thì, khóa tài khoản có hiệu lực ngay.
- Mô hình đơn giản, ít khái niệm (không có access và refresh).

**Cons**:
- Ngược với spec 0001 (đã chọn JWT tự ký) và với API-AUTH-02 (refresh token); phải sửa spec 0001 và file đặc tả.
- Mỗi request một truy vấn phiên; thêm bảng của Spring Session cạnh `phien_dang_nhap` đã có.

### Option 3: JWT trong localStorage, gửi qua header Authorization

Frontend nhận token trong body, lưu `localStorage`, tự gắn header.

**Pros**:
- Không cần chống CSRF; dễ thử bằng Swagger, Postman.
- Kiểm token ở backend như Option 1.

**Cons**:
- Một lỗi XSS là lấy được token; file đặc tả ghi rõ token không nằm trong body.
- `proxy.ts` không thấy token (localStorage chỉ có ở trình duyệt), nên trang được bảo vệ nháy lên trước khi bị chuyển đi.

### Option 4: Next.js giữ token phía máy chủ (BFF với một thư viện auth của Next.js)

Next.js gọi backend đăng nhập, cất token trong session mã hóa của riêng nó, rồi chuyển tiếp lời gọi API kèm token.

**Pros**:
- Trình duyệt không bao giờ thấy JWT; kiểm trang chặt ở máy chủ Next.js.

**Cons**:
- Thêm một tầng chuyển tiếp và một bí mật nữa ở Next.js; `rewrites` hiện có không gắn được token, phải viết Route Handler cho mọi API (spec 0001 cấm Route Handler dưới `/api`).
- Hai nơi giữ trạng thái phiên phải đồng bộ; quá sức với nhóm 2 người.

## Rationale

Option 1 là hướng duy nhất khớp với cả spec 0001, file đặc tả lẫn bảng `phien_dang_nhap` đã có, nên không phải sửa quyết định nào trước đó và không thêm dịch vụ chạy cùng. Option 2 cho thu hồi tức thì, nhưng cái giá là quay lại spec 0001 và API-AUTH-02; khoảng trễ 15 phút của Option 1 chấp nhận được với một hệ thống chung cư, và trang hồ sơ đã đọc trạng thái từ DB nên người bị khóa bị đẩy ra ngay khi tải trang. Option 3 đi ngược yêu cầu "token không nằm trong body" và để lộ token qua XSS. Option 4 an toàn nhất về lý thuyết nhưng tốn công vận hành không tương xứng với một đồ án.

Cookie dấu phiên giải quyết đúng một lực cụ thể: Path `/api/v1/auth` của refresh token khiến `proxy.ts` không thấy phiên. Nếu `proxy.ts` dựa vào `access_token`, sau 15 phút mọi người bị đẩy về trang đăng nhập và tính năng 6 phải vá lại. Mở rộng Path của refresh token ra `/` thì gửi kèm bí mật này cho mọi request, kém hơn một cookie không chứa gì.

Các quyết định nhỏ đi kèm (bạn chọn trong buổi thiết kế):
- **HS256 với `JWT_SECRET`** thay vì cặp khóa ES256/RS256: chỉ backend ký và kiểm, biến đã có. Cặp khóa chỉ đáng khi bên khác phải kiểm token mà không được ký.
- **`SameSite=Strict` cộng header `X-Requested-With` bắt buộc** thay vì chỉ `SameSite`, hay token CSRF của Spring: hai lớp với rất ít code; token của Spring cần lấy token trước khi đăng nhập và xử lý token hết hạn, nặng hơn mà không thêm bảo vệ đáng kể khi không có CORS.
- **Vai trò trong claim `vaiTro`** thay vì đọc DB mỗi request: phân quyền ở tính năng 7 không tốn truy vấn; đổi lại khoảng trễ 15 phút như trên.
- **Mã vai trò viết hoa không dấu** thay vì nhãn tiếng Việt như ví dụ trong file đặc tả: so sánh trong code không phụ thuộc chuẩn hóa Unicode (NFC), giống cách dự án làm với `errorCode`; nhãn nằm ở frontend.
- **bcrypt cost 12, nâng hash cũ khi đăng nhập đúng** thay vì giữ cost 10 hay chuyển Argon2id: khớp file đặc tả (BCrypt), mạnh hơn mà không thêm thư viện; seed vẫn dùng được nhờ `DelegatingPasswordEncoder`.
- **Nhân viên phụ trách mọi tòa** thay vì thêm bảng gán hay trả rỗng: một ban quản lý cho cả khu là thực tế phổ biến, không cần migration cho dữ liệu chưa có màn hình nhập. Ghi thành giả định có follow-up.
- **15 phút / 7 ngày, không có ô ghi nhớ; không giới hạn số phiên; `COOKIE_SECURE` mặc định bật**: đơn giản nhất mà vẫn an toàn theo mặc định; giới hạn số phiên sẽ cần thêm lý do thu hồi vào CHECK (migration).

Những điểm mình tự quyết khi viết spec (bạn có thể đổi):
- Đọc 7 bảng vai trò bằng một câu SQL qua `JdbcClient` thay vì 7 entity JPA: một truy vấn, ít code; đổi lại SQL viết tay. Phương án dự phòng: 7 entity với `@OneToOne` tùy chọn.
- `toa_nha` và `ho_so_cu_tru` đọc qua service công khai của hai package mới `canho`, `cutru` thay vì truy vấn thẳng trong `auth`: đúng luật "phân hệ khác chỉ gọi qua service công khai" của `AGENTS.md`. Phương án dự phòng: truy vấn trong `VaiTroQuery`, nhanh hơn nhưng vi phạm luật đó.
- Cập nhật `dang_nhap_cuoi_luc` bằng câu UPDATE riêng không tăng `phien_ban`: hai lần đăng nhập cùng lúc không bị lỗi khóa lạc quan. Phương án dự phòng: lưu entity và thử lại khi xung đột.
- Xóa cookie trong response lỗi qua trường `headers` mới của `ApiException`: vẫn đi qua `GlobalExceptionHandler` như luật dự án. Phương án dự phòng: để frontend tự bỏ qua cookie, nhưng như vậy không đúng lựa chọn "xóa cookie" của bạn.
- Bỏ qua cookie `access_token` ở `/login`: nếu không, Resource Server từ chối request đăng nhập khi trình duyệt còn cookie hỏng.
- Hash giả cùng cost cho nhánh không tìm thấy tài khoản, và coi mật khẩu quá 72 byte là sai: chặn dò tài khoản qua thời gian phản hồi và tránh lỗi 500 từ giới hạn của bcrypt.
- Security scheme `csrfHeader` cho Swagger: thử được request ghi trong Swagger mà không làm kiểu `openapi-fetch` bắt buộc thêm tham số header.
