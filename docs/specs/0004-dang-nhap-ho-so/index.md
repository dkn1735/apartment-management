# 0004. Đăng nhập và trang hồ sơ: JWT trong cookie HttpOnly, phiên lưu trong DB

**Date**: 2026-11-16
**Status**: In Progress

## Summary

Spec này chốt cách người dùng đăng nhập và cách hệ thống nhớ họ là ai. Người dùng nhập số điện thoại hoặc email cùng mật khẩu. Backend kiểm tra mật khẩu bằng bcrypt, cấp một access token (JWT, thẻ ký số sống 15 phút) và một refresh token (chuỗi ngẫu nhiên sống 7 ngày, DB chỉ lưu bản băm), cả hai nằm trong cookie `HttpOnly` mà JavaScript không đọc được. Trang `/ho-so` hiện thông tin, vai trò và phạm vi tòa nhà hoặc căn hộ của chính người đó; vào trang được bảo vệ khi chưa đăng nhập thì bị chuyển về `/dang-nhap`. Đây là spec gốc của phân hệ xác thực: làm mới phiên, phân quyền, đổi mật khẩu và khóa tài khoản ở các lát sau đều dựa trên token, cookie và mã vai trò chốt ở đây.

## Requirements

**User stories**:
- Là cư dân hoặc nhân viên, tôi muốn đăng nhập bằng số điện thoại hoặc email mà tôi quen dùng, để vào hệ thống mà không phải nhớ thêm tên đăng nhập.
- Là người dùng đã đăng nhập, tôi muốn xem hồ sơ của mình (họ tên, vai trò, tòa nhà hoặc căn hộ), để biết hệ thống đang nhận tôi là ai.
- Là ban quản lý, tôi muốn tài khoản `Tạm khóa` hoặc `Ngừng hoạt động` không vào được hệ thống, kể cả khi họ còn nhớ mật khẩu.
- Là người làm các tính năng 6 đến 13, tôi muốn cách cấp và kiểm tra token, tên cookie và mã vai trò đã cố định, để chỉ cần cắm thêm logic.

**Acceptance criteria**:
- **AC-1**: `POST /api/v1/auth/login` với tài khoản seed `0900000007` hoặc `cudan@chungcu.test` và mật khẩu `ChungCu@2026` trả 200, `data` có đúng `maNguoiDung`, `hoTen`, `vaiTro`, `accessTokenHetHanLuc`; response đặt ba cookie `access_token`, `refresh_token`, `phien` với thuộc tính như mục *Cookie*; body không chứa token nào.
- **AC-2**: `taiKhoan` được chuẩn hóa trước khi tìm: có `@` thì cắt khoảng trắng đầu cuối và đổi chữ thường (`  CuDan@ChungCu.Test ` đăng nhập được); không có `@` thì bỏ khoảng trắng, dấu chấm, gạch nối, đổi tiền tố `+84` (hoặc `84` khi dài 11 chữ số) thành `0` (`+84 900 000 007` đăng nhập được). Mọi dòng của bảng ví dụ ở *Luồng đăng nhập* cho đúng kết quả ghi trong bảng.
- **AC-3**: Sai mật khẩu, tài khoản không tồn tại, hoặc `taiKhoan` không ra được email hay số điện thoại hợp lệ đều trả 401 `AUTH_INVALID_CREDENTIALS` với cùng một `message`, không đặt cookie; nhánh tài khoản không tồn tại vẫn chạy một lần so khớp bcrypt (với hash giả cùng cost), để thời gian phản hồi không lộ tài khoản có tồn tại hay không.
- **AC-4**: Mật khẩu đúng nhưng `trang_thai_tai_khoan` là `Tạm khóa` hoặc `Ngừng hoạt động` thì trả 403 `AUTH_ACCOUNT_LOCKED`, không đặt cookie, không tạo phiên. Mật khẩu sai trên tài khoản bị khóa vẫn trả 401 `AUTH_INVALID_CREDENTIALS` (tài khoản seed `0900000008` kiểm được cả hai nhánh).
- **AC-5**: Thiếu hoặc rỗng `taiKhoan`, `matKhau`, hoặc `taiKhoan` dài quá 150 ký tự, `matKhau` dài quá 128 ký tự, trả 400 `VALIDATION_FAILED` có lỗi gắn đúng trường. Mật khẩu hợp lệ về độ dài ký tự nhưng quá 72 byte UTF-8 (giới hạn của bcrypt) được xử lý như sai mật khẩu (AC-3), không ném lỗi 500.
- **AC-6**: Mỗi lần đăng nhập thành công, trong một transaction: thêm đúng một dòng `phien_dang_nhap` (`ma_ho_phien` là UUID mới, `token_hash` là SHA-256 hex chữ thường của refresh token trong cookie, `tao_luc` từ `Clock`, `het_han_luc` = `tao_luc` + 7 ngày, `dia_chi_ip`, `user_agent` cắt còn 500 ký tự); ghi `dang_nhap_cuoi_luc` từ `Clock`; nếu `mat_khau_hash` có cost bcrypt dưới 12 thì thay bằng hash cost 12 của chính mật khẩu đó, và lần đăng nhập sau vẫn thành công. Không đụng `so_lan_dang_nhap_sai`, `khoa_tam_den`, `phien_ban`.
- **AC-7**: Access token là JWT ký HS256 bằng `JWT_SECRET`, có claim `iss` = `chungcu`, `sub` = `maNguoiDung`, `sid` = `ma_ho_phien`, `vaiTro` = mảng mã vai trò, `jti`, `iat`, `exp` = `iat` + 15 phút. Backend chỉ đọc token từ cookie `access_token` (bỏ qua header `Authorization`). Token hết hạn (quá 30 giây cho phép lệch giờ), sai chữ ký, sai `iss` hoặc dùng thuật toán khác HS256 thì endpoint cần đăng nhập trả 401 `UNAUTHENTICATED`. Cookie `access_token` cũ hay hỏng không làm `POST /api/v1/auth/login` thất bại.
- **AC-8**: `GET /api/v1/auth/profile` trả hồ sơ của người có `sub` trong token (không nhận mã từ URL hay body): `maNguoiDung`, `hoTen`, `soDienThoai`, `email`, `trangThaiTaiKhoan`, `vaiTro` (mã theo thứ tự cố định ở mục *Mã vai trò*), `phamVi.maToaNha` (mọi mã tòa nếu có ít nhất một vai trò khác `CU_DAN`, ngược lại rỗng), `phamVi.maCanHo` (căn hộ của các hồ sơ cư trú `Hợp lệ` còn hiệu lực hôm nay theo giờ Việt Nam nếu có vai trò `CU_DAN`, ngược lại rỗng), và `chiTietVaiTro`. Không bao giờ có `matKhauHash` hay `soCccd`. Tài khoản seed `0900000006` nhận cả `BAN_QUAN_TRI` lẫn `CU_DAN`, có cả `maToaNha` lẫn `maCanHo`.
- **AC-9**: Gọi `GET /api/v1/auth/profile` khi tài khoản đã chuyển sang `Tạm khóa` hoặc `Ngừng hoạt động` sau lúc đăng nhập thì trả 403 `AUTH_ACCOUNT_LOCKED` và response xóa cả ba cookie (`Max-Age=0`, đúng `Path` của từng cookie). Người dùng trong token không còn tồn tại thì trả 401 `UNAUTHENTICATED` và cũng xóa ba cookie.
- **AC-10**: Mọi request `POST`, `PUT`, `PATCH`, `DELETE` tới `/api/**` thiếu header `X-Requested-With: XMLHttpRequest` bị chặn với 403 `CSRF_REJECTED`, kể cả `POST /api/v1/auth/login`; `GET` không cần header. Hàm `api` phía frontend tự gắn header này cho mọi request, nên không màn hình nào phải tự thêm.
- **AC-11**: Backend không khởi động khi `JWT_SECRET` thiếu, ngắn hơn 32 byte UTF-8, hoặc bằng đúng chuỗi mẫu trong `backend/.env.example`; thông báo lỗi nói rõ biến nào sai. Mặc định cookie có cờ `Secure`; đặt `COOKIE_SECURE=false` thì ba cookie không có cờ này.
- **AC-12**: Trang `/dang-nhap` có ô "Số điện thoại hoặc email" (`autocomplete="username"`), ô "Mật khẩu" (`autocomplete="current-password"`) với nút hiện/ẩn mật khẩu (có `aria-pressed` và nhãn), nút "Đăng nhập"; dùng được hoàn toàn bằng bàn phím, axe không báo lỗi. Lỗi `AUTH_INVALID_CREDENTIALS` hoặc `AUTH_ACCOUNT_LOCKED` hiện trong `FormAlert` đầu form và focus chuyển tới đó; ô mật khẩu bị xóa, ô tài khoản giữ nguyên. Thành công thì chuyển tới `next` nếu `next` là đường dẫn nội bộ an toàn (mục *Quy tắc next*), ngược lại tới `/ho-so`.
- **AC-13**: Vào một trang được bảo vệ (mọi trang trừ `/dang-nhap` và `/dev/**`) khi không có cookie `phien` thì bị chuyển ngay (trước khi trang render) tới `/dang-nhap?next=<đường dẫn kèm query>`. Trang `/` chuyển tới `/ho-so`. `/dang-nhap` không bao giờ tự chuyển đi nơi khác dựa trên cookie (tránh vòng lặp).
- **AC-14**: Trang `/ho-so` nằm trong `AppShell` thật, menu có một mục "Hồ sơ" đang chọn, góc phải thanh trên hiện họ tên người dùng. Trang hiện ba phần: thông tin tài khoản (họ tên, mã người dùng, số điện thoại, email, trạng thái), vai trò (nhãn tiếng Việt của từng vai trò kèm chi tiết riêng: chức vụ và bộ phận, chuyên môn, phạm vi phụ trách, vị trí trực, nhiệm kỳ và vai trò trong BQT), phạm vi (tòa nhà phụ trách, căn hộ cư trú; danh sách rỗng hiện "Không có"). Có trạng thái đang tải (skeleton) và trạng thái lỗi có nút "Thử lại"; không hiện CCCD. axe không báo lỗi.
- **AC-15**: Bất kỳ lời gọi API nào phía client trả 401 `UNAUTHENTICATED` thì trình duyệt chuyển tới `/dang-nhap?next=<trang hiện tại>&ly-do=het-phien`, và trang đăng nhập hiện thông báo "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."; trả 403 `AUTH_ACCOUNT_LOCKED` thì chuyển tới `/dang-nhap?ly-do=khoa` và hiện thông báo tài khoản bị khóa. Lỗi của chính form đăng nhập không kích hoạt chuyển trang này. Không có vòng lặp chuyển trang khi cookie `phien` còn mà access token đã hết.
- **AC-16**: Log không bao giờ chứa mật khẩu, token, `token_hash`, `mat_khau_hash` hay giá trị `taiKhoan` người dùng gõ. Đăng nhập thành công ghi một dòng INFO có `maNguoiDung`; thất bại ghi một dòng INFO có lý do (`SAI_THONG_TIN` hoặc `TAI_KHOAN_KHOA`) và `maNguoiDung` chỉ khi tìm thấy tài khoản.

## Decision

**Chosen option**: Option 1: JWT ngắn hạn trong cookie `HttpOnly`, refresh token ngẫu nhiên lưu bản băm trong `phien_dang_nhap`, cookie dấu phiên cho `proxy.ts`.

Backend tự cấp và kiểm tra JWT HS256 bằng Spring Security OAuth2 Resource Server (đọc token từ cookie), băm mật khẩu bằng bcrypt cost 12 qua `DelegatingPasswordEncoder`, chống CSRF bằng `SameSite=Strict` cộng header bắt buộc; Next.js chặn trang bằng `proxy.ts` dựa trên cookie dấu phiên, còn client xử lý 401.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`) · `next-dev-loop` (`vercel/next.js`, `.agents/skills/next-dev-loop/`) · `ui-styling` (`.claude/skills/ui-styling/`). Riêng lỗi thì theo vỏ bọc của spec 0001, không theo `ProblemDetail`. Trước khi viết `proxy.ts`, đọc `frontend/node_modules/next/dist/docs/01-app/01-getting-started/16-proxy.md` và mục *Optimistic checks with Proxy* trong `02-guides/authentication.md` (theo `frontend/AGENTS.md`).

## Feature design

### Mã vai trò

Enum `VaiTro` trong package `auth`, xuất ra OpenAPI thành enum (frontend nhận kiểu union). Thứ tự này là thứ tự trong `vaiTro` của token và API.

| Mã | Nhãn hiển thị | Có vai trò khi có dòng trong | Chi tiết trả trong `chiTietVaiTro` |
|---|---|---|---|
| `QUAN_TRI_VIEN` | Quản trị viên | `quan_tri_vien` | `quanTriVien: { maQuanTriVien }` |
| `BAN_QUAN_LY` | Ban quản lý | `nhan_vien_bql` | `banQuanLy: { maNv, chucVu, boPhan }` |
| `BAN_QUAN_TRI` | Ban quản trị | `thanh_vien_ban_quan_tri` | `banQuanTri: { maThanhVien, nhiemKy, vaiTro }` |
| `KE_TOAN` | Kế toán | `ke_toan` | `keToan: { maNv, phamViPhuTrach }` |
| `KY_THUAT` | Kỹ thuật | `nhan_vien_ky_thuat` | `kyThuat: { maNv, chuyenMonKyThuat }` |
| `BAO_VE` | Bảo vệ | `nhan_vien_bao_ve` | `baoVe: { maNv, viTriTruc }` |
| `CU_DAN` | Cư dân | `cu_dan` | `cuDan: { maCuDan }` (không có `soCccd`) |

Mỗi trường trong `chiTietVaiTro` là object hoặc `null` khi người đó không có vai trò ấy. Nhãn tiếng Việt chỉ nằm ở frontend (`frontend/src/lib/vai-tro.ts`). Người không có vai trò nào vẫn đăng nhập được (`vaiTro` rỗng); trang hồ sơ hiện "Chưa được gán vai trò". Tính năng 7 dùng các mã này làm authority `ROLE_<mã>`.

### Token

- **Access token**: JWT HS256, claim như AC-7. Ký bằng `NimbusJwtEncoder` dựng từ `ImmutableSecret` của `SecretKeySpec(JWT_SECRET bytes UTF-8, "HmacSHA256")`, gọi `encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))` (phải ghi rõ header, mặc định của encoder là RS256). Kiểm bằng `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(MacAlgorithm.HS256)`, validator gồm `JwtIssuerValidator("chungcu")` và `JwtTimestampValidator(Duration.ofSeconds(30))` có `setClock(clock)` với bean `Clock`. `JwtAuthenticationConverter` đổi claim `vaiTro` thành authority `ROLE_<mã>`, tên principal là `sub`.
- **Refresh token**: 32 byte từ `SecureRandom`, mã hóa base64url không padding (43 ký tự). DB chỉ lưu SHA-256 hex chữ thường. Lát này chỉ cấp và lưu; tính năng 6 làm endpoint làm mới, xoay vòng và thu hồi.
- **Đọc token**: một `BearerTokenResolver` riêng chỉ đọc cookie `access_token`, và trả `null` cho mọi đường công khai: `POST /api/v1/auth/login`, `/error`, `/actuator/health/**`, Swagger (tính năng 6 thêm `/api/v1/auth/refresh-token`). Danh sách đường công khai là một hằng dùng chung cho cả resolver và `authorizeHttpRequests`, để cookie cũ hay hỏng không bao giờ làm hỏng một đường công khai.
- **Lỗi 401 và 403 của Resource Server**: `BearerTokenAuthenticationFilter` có entry point riêng (trả 401 body rỗng). Phải truyền cùng entry point và access denied handler đang dùng trong `exceptionHandling` vào `oauth2ResourceServer(o -> o.authenticationEntryPoint(...).accessDeniedHandler(...).bearerTokenResolver(...).jwt(...))`, để token hỏng hay hết hạn trả đúng vỏ bọc `UNAUTHENTICATED`.

### Cookie

Tạo bằng `ResponseCookie` trong một lớp `AuthCookies` (đặt và xóa). Mọi cookie: `HttpOnly`, `SameSite=Strict`, `Secure` theo `app.security.cookie-secure`.

| Cookie | Giá trị | Path | Max-Age |
|---|---|---|---|
| `access_token` | JWT | `/` | 900 (15 phút) |
| `refresh_token` | refresh token thô | `/api/v1/auth` | 604800 (7 ngày) |
| `phien` | `1` (không chứa bí mật) | `/` | 604800 (7 ngày) |

Xóa cookie: cùng tên, cùng `Path`, giá trị rỗng, `Max-Age=0`.

### Chống CSRF

- Cookie `SameSite=Strict` (lớp 1).
- Lớp 2: filter `CsrfHeaderFilter` trong chuỗi Spring Security. Tạo bằng `new` bên trong `SecurityConfig` và gắn bằng `addFilterBefore(..., BearerTokenAuthenticationFilter.class)`; không khai báo là `@Bean` hay `@Component` (Boot sẽ tự đăng ký thêm một lần nữa ngoài chuỗi bảo mật). Request `POST`, `PUT`, `PATCH`, `DELETE` có đường dẫn bắt đầu bằng `/api/` mà thiếu header `X-Requested-With` có giá trị đúng `XMLHttpRequest` (so khớp phân biệt hoa thường) thì ghi lỗi 403 `CSRF_REJECTED` qua `ErrorResponseFactory` (như entry point 401 hiện có). `GET`, `HEAD`, `OPTIONS` không bị kiểm. Trang web khác không gửi được header tùy chỉnh vì backend không mở CORS.
- Giữ `csrf.disable()` cho cơ chế token của Spring (không dùng).
- Frontend: một middleware của openapi-fetch trong `lib/api/api.ts` gắn header này cho mọi request của `client`.
- Swagger (dev): khai báo security scheme `apiKey` tên `csrfHeader` (header `X-Requested-With`) áp cho mọi operation, để bấm "Authorize" nhập `XMLHttpRequest` một lần. Scheme không đổi kiểu tham số mà `pnpm gen:api` sinh ra.

### Băm mật khẩu

`PasswordEncoder` là `DelegatingPasswordEncoder` chỉ có một id `bcrypt` trỏ tới `BCryptPasswordEncoder(app.security.bcrypt-cost)` (mặc định 12). Không dùng `PasswordEncoderFactories`, để hash `{noop}`, `{MD4}`, `{SHA-1}` hay id lạ lọt vào DB (CHECK của V1 nhận mọi `{id}`) bị coi là sai mật khẩu chứ không được chấp nhận; đặt `setDefaultPasswordEncoderForMatches` là một encoder luôn trả `false`. Sau khi `matches` đúng, nếu `upgradeEncoding(hash)` đúng thì băm lại và lưu. Hash giả cho nhánh không tìm thấy tài khoản được tạo một lần lúc khởi động bằng chính encoder này (cùng cost). Profile test đặt `app.security.bcrypt-cost: 4` cho nhanh; test nâng hash tự đặt cost cụ thể.

Thời gian phản hồi: tài khoản còn hash cost 10 (chỉ có ở dữ liệu seed, mỗi tài khoản tự lên cost 12 sau lần đăng nhập đúng đầu tiên) so khớp nhanh hơn hash giả cost 12, nên trên dữ liệu demo vẫn đoán được tài khoản có tồn tại. Chấp nhận vì chỉ ảnh hưởng dữ liệu seed công khai sẵn; tài khoản thật do tính năng 11 tạo đã là cost 12.

### Luồng đăng nhập (`DangNhapService`)

1. Bean Validation trên `DangNhapRequest` (AC-5).
2. Chuẩn hóa `taiKhoan` (AC-2) bằng `TaiKhoanDangNhap.parse(...)`, ra email, số điện thoại, hoặc không hợp lệ. Bước đầu: cắt khoảng trắng đầu cuối bằng `String.strip()` (gồm cả khoảng trắng Unicode).
   - Có `@`: đổi chữ thường bằng `toLowerCase(Locale.ROOT)`, là email. Không kiểm định dạng thêm (không tìm thấy thì như sai tài khoản).
   - Không có `@`: bỏ mọi ký tự khoảng trắng (`\s` và khoảng trắng Unicode), dấu chấm, gạch nối; rồi `+84` đầu chuỗi thành `0`, hoặc `84` đầu chuỗi khi dài đúng 11 chữ số thành `0`; kết quả phải khớp `^0[0-9]{9}$`, ngược lại không hợp lệ.

   | Nhập | Kết quả |
   |---|---|
   | `  CuDan@ChungCu.Test ` | email `cudan@chungcu.test` |
   | `0900000007`, `090 000 0007`, `090.000.0007`, `090-000-0007` | `0900000007` |
   | `+84 900 000 007`, `+84900000007`, `84900000007` | `0900000007` |
   | `0084900000007`, `(090) 0000007`, `+840900000007`, `090000000`, `abc` | không hợp lệ |
3. Tìm `NguoiDungHeThong` theo email hoặc số điện thoại. Không hợp lệ hoặc không thấy: so khớp với hash giả, ném 401 `AUTH_INVALID_CREDENTIALS`.
4. `matKhau` quá 72 byte UTF-8: so khớp hash giả, ném 401 như trên. Ngược lại `matches(matKhau, hash)` sai: ném 401.
5. Trạng thái khác `Hoạt động`: ném 403 `AUTH_ACCOUNT_LOCKED`.
6. Nếu cần nâng hash, gọi `encode` trước khi mở transaction (bcrypt khoảng 250 ms không giữ kết nối DB). Các bước 1 đến 6 nằm ngoài transaction; chỉ bước 7 là `@Transactional` (tách sang một bean riêng, ví dụ `PhienDangNhapService.taoPhien(...)`, để proxy transaction có tác dụng).
7. Trong transaction: ghi `dang_nhap_cuoi_luc`, `cap_nhat_luc` bằng một câu `@Modifying` UPDATE không tăng `phien_ban` (hai lần đăng nhập cùng lúc không gây lỗi khóa lạc quan); nếu có hash mới thì một câu UPDATE riêng `SET mat_khau_hash = :hashMoi WHERE ma_nguoi_dung = :ma AND mat_khau_hash = :hashCu` (đổi mật khẩu ở tính năng 10 xảy ra giữa chừng thì câu này không ghi gì, không ghi đè mật khẩu mới); thêm dòng `phien_dang_nhap`.
8. Lấy vai trò (`VaiTroQuery`), ký JWT. Service trả một record kết quả (dữ liệu response, JWT, refresh token thô, thời điểm hết hạn); controller đặt ba cookie từ record đó sau khi service đã commit, rồi trả `ApiResponse.ok`. Commit lỗi thì exception đi lên, không có cookie nào được đặt.

### Hồ sơ (`HoSoService`)

- Đọc `sub` từ `Jwt`, tải người dùng. Không thấy: 401 `UNAUTHENTICATED` kèm header xóa cookie. Khóa: 403 `AUTH_ACCOUNT_LOCKED` kèm header xóa cookie (AC-9).
- Vai trò và chi tiết: `VaiTroQuery`, một câu SQL qua `JdbcClient` với 7 `LEFT JOIN` theo `ma_nguoi_dung` (các bảng vai trò thuộc phân hệ `auth`). Vai trò luôn đọc từ DB ở đây, không lấy từ token.
- `phamVi.maToaNha`: gọi `ToaNhaService.danhSachMaToaNha()` của package mới `canho` (entity `ToaNha` chỉ đọc, sắp xếp theo `ma_toa_nha`).
- `phamVi.maCanHo`: gọi `CuTruService.maCanHoDangCuTru(maCuDan, LocalDate.now(clock))` của package mới `cutru` (entity `HoSoCuTru` chỉ đọc): `trang_thai_ho_so = 'Hợp lệ'`, `ngay_bat_dau <= hôm nay`, `ngay_ket_thuc` rỗng hoặc `>= hôm nay`, không trùng, sắp xếp tăng dần. `Clock` đã mang múi giờ `app.timezone` nên `LocalDate.now(clock)` là ngày Việt Nam.
- Để trả header xóa cookie mà vẫn đi qua `GlobalExceptionHandler`, `ApiException` (trong `common`) thêm trường tùy chọn `HttpHeaders headers`, và handler chép chúng vào response lỗi. Đây là khả năng chung, không biết gì về cookie.

### Quy tắc next

Một hàm `safeNext(value)` dùng chung cho `proxy.ts` và trang đăng nhập: nhận khi chuỗi bắt đầu bằng `/`, không bắt đầu bằng `//` hay `/\`, không chứa ký tự điều khiển, và không trỏ về `/dang-nhap`; còn lại trả `/ho-so`.

### Giao diện

- **Route**: `src/app/dang-nhap/page.tsx` (ngoài khung app); route group `src/app/(app)/layout.tsx` bọc `AppShell` với `nav` chỉ có mục "Hồ sơ" (icon `UserRound`) và `userSlot` là component client hiện `hoTen` từ query hồ sơ (không hiện gì khi đang tải hoặc lỗi); `src/app/(app)/ho-so/page.tsx`; `src/app/page.tsx` thành `redirect("/ho-so")`. Tính năng 7 thêm mục menu theo vai trò vào chính layout này.
- **Không có đăng xuất ở lát này**: nút và endpoint đăng xuất thuộc tính năng 6. Trong lúc chờ, muốn đổi tài khoản thì đăng nhập lại ở `/dang-nhap` (cookie mới ghi đè cookie cũ). Chấp nhận có chủ đích.
- **Trang đăng nhập**: một thẻ form giữa trang, `Brand` ở trên, tiêu đề `h1` "Đăng nhập", ghép `useApiForm` (zod: hai trường bắt buộc, câu lỗi "Vui lòng nhập số điện thoại hoặc email", "Vui lòng nhập mật khẩu"), `FormField`, `FormAlert`, `SubmitButton` theo *Form có lỗi API* trong `frontend/design.md`. Phần đọc `useSearchParams` nằm trong một component client bọc bằng `Suspense` (nếu không `pnpm build` báo lỗi). Tham số `ly-do=het-phien` hiện "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.", `ly-do=khoa` hiện câu của `AUTH_ACCOUNT_LOCKED`, giá trị khác thì bỏ qua; thông báo nằm phía trên form trong vùng `role="status"`. Trang này không gọi query nào (đặc biệt không gọi `["ho-so"]`), để không có lỗi 401 nào kích hoạt chuyển trang ở đây. Không có link "Quên mật khẩu" (tính năng 13 thêm). Thành công: `queryClient.clear()` rồi `router.replace(safeNext(next))`.
- **Trang hồ sơ**: `PageHeader` "Hồ sơ của tôi", ba thẻ như AC-14, `useQuery(["ho-so"], ...)` gọi `GET /api/v1/auth/profile`; đang tải dùng skeleton, lỗi khác 401 và 403 khóa thì dùng `ErrorState` có "Thử lại". Nhãn chi tiết vai trò: "Mã nhân viên", "Chức vụ", "Bộ phận", "Chuyên môn", "Phạm vi phụ trách", "Vị trí trực", "Mã thành viên", "Nhiệm kỳ", "Vai trò trong ban quản trị", "Mã cư dân", "Mã quản trị viên". `nhiemKy` hiện nguyên văn (là chuỗi tự do, ví dụ `2026-2028`).
- **Xử lý 401 toàn cục**: `QueryCache` và `MutationCache` trong `createQueryClient` có `onError`: `ApiError` với `errorCode` `UNAUTHENTICATED` thì `window.location.assign("/dang-nhap?next=" + encodeURIComponent(safeNext(location.pathname + location.search)) + "&ly-do=het-phien")`; `AUTH_ACCOUNT_LOCKED` mà không phải từ mutation đăng nhập (mutation đăng nhập đặt `meta: { boQuaChuyenTrang: true }`) thì chuyển tới `/dang-nhap?ly-do=khoa`. Một cờ cấp module đảm bảo chỉ chuyển trang một lần dù nhiều query lỗi cùng lúc. Không chuyển trang khi đang ở `/dang-nhap` hay `/dev/**`. Lời gọi đăng nhập trả `AUTH_INVALID_CREDENTIALS`, nên không bao giờ chạm nhánh 401 này.
- **Cookie `phien` còn mà access token đã hết**: trang được bảo vệ hiện khung app và skeleton một thoáng, rồi chuyển về `/dang-nhap`. Chấp nhận cho lát này; tính năng 6 làm mới âm thầm nên trường hợp này gần như biến mất.
- **Từ điển lỗi** (`error-messages.ts`): `AUTH_INVALID_CREDENTIALS` "Số điện thoại, email hoặc mật khẩu không đúng."; `AUTH_ACCOUNT_LOCKED` "Tài khoản đang bị khóa hoặc đã ngừng hoạt động. Vui lòng liên hệ ban quản lý."; `CSRF_REJECTED` "Yêu cầu bị từ chối vì lý do bảo mật. Vui lòng tải lại trang và thử lại.". Backend dùng cùng các câu đó làm `message`.

### `proxy.ts`

`src/proxy.ts`, export `proxy`. `config.matcher` loại trừ `/api`, `/_next/static`, `/_next/image`, `favicon.ico`, file tĩnh có đuôi, `/dang-nhap`, `/dev`. Không có cookie `phien` thì `NextResponse.redirect` tới `/dang-nhap?next=` + `encodeURIComponent(safeNext(...))`, trong đó đường dẫn lấy từ `request.nextUrl.pathname` và query từ `request.nextUrl.searchParams` sau khi xóa tham số nội bộ `_rsc` (request điều hướng phía client của Next.js mang tham số này). Kiểm hành vi thật bằng skill `next-dev-loop` khi `pnpm dev` đang chạy. Chỉ đọc cookie, không gọi backend, không kiểm chữ ký (đúng hướng dẫn *optimistic checks* của Next.js 16).

### Data model sketch

Không có migration mới. Các bảng đều có từ V1, V2 (spec 0002):

| Bảng | Khóa và quan hệ | Lát này làm gì |
|---|---|---|
| `nguoi_dung_he_thong` | PK `ma_nguoi_dung`; `email`, `so_dien_thoai` UNIQUE | Đọc; ghi `dang_nhap_cuoi_luc`, `mat_khau_hash` (khi nâng), `cap_nhat_luc` |
| `phien_dang_nhap` | PK `ma_phien`; FK `ma_nguoi_dung` N:1 | Thêm một dòng mỗi lần đăng nhập |
| 7 bảng vai trò | FK `ma_nguoi_dung` UNIQUE, 1:0..1 với người dùng | Chỉ đọc |
| `ho_so_cu_tru` | FK `ma_cu_dan` N:1 `cu_dan`, `ma_can_ho` N:1 `can_ho` | Chỉ đọc (package `cutru`) |
| `toa_nha` | PK `ma_toa_nha` | Chỉ đọc (package `canho`) |

Entity JPA mới: `NguoiDungHeThong` (`@Version phienBan`, `@DynamicUpdate`), `PhienDangNhap` (package `auth`); `ToaNha` (package `canho`), `HoSoCuTru` (package `cutru`), cả hai chỉ đọc và chỉ ánh xạ cột cần dùng. Các bảng vai trò đọc bằng `JdbcClient`, không có entity ở lát này.

### State transitions

Không có máy trạng thái mới. Lát này chỉ đọc `trang_thai_tai_khoan`: `Hoạt động` được đăng nhập; `Tạm khóa`, `Ngừng hoạt động` bị từ chối. Dòng `phien_dang_nhap` sinh ra ở trạng thái còn hiệu lực (`thu_hoi_luc` rỗng); thu hồi thuộc tính năng 6, 10, 11.

### API surface

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/auth/login` | POST | header `X-Requested-With` (req); body `taiKhoan: string` (req, tối đa 150), `matKhau: string` (req, tối đa 128) | 200 `data { maNguoiDung, hoTen, vaiTro: VaiTro[], accessTokenHetHanLuc: Instant }`, `message` "Đăng nhập thành công"; `Set-Cookie` ×3 | Public | 400 `VALIDATION_FAILED`, 401 `AUTH_INVALID_CREDENTIALS`, 403 `AUTH_ACCOUNT_LOCKED`, 403 `CSRF_REJECTED` |
| `/api/v1/auth/profile` | GET | cookie `access_token` | 200 `data { maNguoiDung, hoTen, soDienThoai, email, trangThaiTaiKhoan, vaiTro: VaiTro[], phamVi { maToaNha: string[], maCanHo: string[] }, chiTietVaiTro { quanTriVien, banQuanLy, banQuanTri, keToan, kyThuat, baoVe, cuDan } }`, `message` "Lấy thông tin tài khoản thành công" | Đã đăng nhập | 401 `UNAUTHENTICATED`, 403 `AUTH_ACCOUNT_LOCKED` (kèm xóa cookie) |

Đăng nhập trả 200 (không phải 201 như ví dụ trong file xlsx; spec 0001 đã ghi lệch này). Mã `AUTH_*` khai báo trong lớp `AuthErrorCodes` của package `auth`; `CSRF_REJECTED` thêm vào `ErrorCodes` chung.

### Value sourcing

| Action | Value produced / displayed | Source |
|---|---|---|
| Đăng nhập | email hoặc số điện thoại để tìm | input `taiKhoan`, chuẩn hóa theo AC-2 |
| Đăng nhập | kết quả so khớp mật khẩu | input `matKhau` so với cột `mat_khau_hash` (hoặc hash giả sinh lúc khởi động) |
| Đăng nhập | có bị khóa không | cột `trang_thai_tai_khoan` |
| Đăng nhập | `maNguoiDung`, `hoTen` | cột `ma_nguoi_dung`, `ho_ten` |
| Đăng nhập | `vaiTro` | `VaiTroQuery` trên 7 bảng vai trò |
| Đăng nhập | `iat`, `tao_luc`, `dang_nhap_cuoi_luc`, `cap_nhat_luc` | `Instant.now(clock)` |
| Đăng nhập | `exp`, `accessTokenHetHanLuc` | `iat` + `app.security.access-token-ttl` (PT15M) |
| Đăng nhập | `het_han_luc`, Max-Age cookie `refresh_token` và `phien` | `tao_luc` + `app.security.refresh-token-ttl` (P7D) |
| Đăng nhập | `sid`, `ma_ho_phien` | `UUID.randomUUID()` |
| Đăng nhập | `jti` | `UUID.randomUUID()` |
| Đăng nhập | refresh token, `token_hash` | `SecureRandom` 32 byte; SHA-256 hex của chuỗi base64url |
| Đăng nhập | `dia_chi_ip` | `HttpServletRequest.getRemoteAddr()` (đã qua `forward-headers-strategy: framework`, nên là IP trình duyệt mà Next.js chuyển tiếp) |
| Đăng nhập | `user_agent` | header `User-Agent`, cắt 500 ký tự, rỗng thì `null` |
| Đăng nhập | hash mới khi nâng | `PasswordEncoder.encode(matKhau)` với cost `app.security.bcrypt-cost` |
| Đăng nhập | cờ `Secure` của cookie | `app.security.cookie-secure` (env `COOKIE_SECURE`, mặc định `true`) |
| Hồ sơ | mã người dùng cần tải | claim `sub` của JWT |
| Hồ sơ | thông tin tài khoản | cột `ho_ten`, `so_dien_thoai`, `email`, `trang_thai_tai_khoan` |
| Hồ sơ | `vaiTro`, `chiTietVaiTro` | `VaiTroQuery` (cột `chuc_vu`, `bo_phan`, `chuyen_mon_ky_thuat`, `pham_vi_phu_trach`, `vi_tri_truc`, `nhiem_ky`, `vai_tro`, các mã vai trò) |
| Hồ sơ | `phamVi.maToaNha` | toàn bộ `toa_nha.ma_toa_nha` khi có vai trò khác `CU_DAN` (giả định: một ban quản lý cho cả khu; xem *Follow-up*) |
| Hồ sơ | `phamVi.maCanHo` | `ho_so_cu_tru.ma_can_ho` theo `cu_dan.ma_cu_dan` và ngày `LocalDate.now(clock)` (múi giờ `app.timezone`) |
| Trang hồ sơ | nhãn vai trò | `frontend/src/lib/vai-tro.ts` theo mã |
| `proxy.ts` | có phiên hay không | cookie `phien` có mặt |
| Trang đăng nhập | nơi chuyển tới sau khi đăng nhập | query `next` qua `safeNext`, mặc định `/ho-so` |
| Trang đăng nhập | thông báo lý do | query `ly-do` (`het-phien`, `khoa`) |

### Key invariants

- Refresh token và access token không bao giờ nằm trong body, log, hay DB dạng thô; DB chỉ có SHA-256 của refresh token.
- Sai tài khoản và sai mật khẩu không phân biệt được qua mã lỗi, `message` hay thời gian phản hồi. `AUTH_ACCOUNT_LOCKED` chỉ xuất hiện khi mật khẩu đúng.
- Mọi thời điểm nghiệp vụ lấy từ bean `Clock`.
- `GET /api/v1/auth/profile` chỉ trả dữ liệu của `sub` trong token.
- API không bao giờ trả `mat_khau_hash`, `so_cccd`, `token_hash`.
- Đăng nhập là một transaction: hoặc có đủ dòng phiên và `dang_nhap_cuoi_luc`, hoặc không có gì; cookie chỉ được đặt sau khi transaction commit.
- Cookie `access_token` không bao giờ chặn được `POST /api/v1/auth/login`.

### Security model

- `POST /api/v1/auth/login`: công khai, nhưng phải có header chống CSRF. `SecurityConfig` hiện cho qua cả `/api/v1/auth/**`; phải thu hẹp lại đúng `POST /api/v1/auth/login` (tính năng 6 thêm `POST /api/v1/auth/refresh-token`), nếu không `/profile` thành công khai và trả 500 khi không có token.
- `GET /api/v1/auth/profile`: mọi người đã đăng nhập, chỉ thấy hồ sơ của mình.
- Mọi đường khác dưới `/api/**` vẫn yêu cầu đăng nhập như bộ khung hiện có; phân quyền theo vai trò thuộc tính năng 7.
- Dữ liệu nhạy cảm: mật khẩu, token, hash, CCCD (dữ liệu cá nhân). Không ghi vào log, không trả qua API (trừ token trong cookie `HttpOnly`).
- Chưa có giới hạn số lần thử đăng nhập: tính năng 12 thêm khóa tạm; chưa có nhật ký kiểm toán đăng nhập: tính năng 8 thêm. Đây là lỗ hổng chấp nhận có thời hạn, ghi ở *Consequences*.

### Configuration required

- `JWT_SECRET` (đã có): nay bắt buộc ít nhất 32 byte UTF-8 và khác chuỗi mẫu `doi-thanh-chuoi-ngau-nhien-du-dai`. Chuỗi mẫu là một hằng trong code (backend không đọc `.env.example`); kiểm bằng một validator trên `AppProperties.Security`. Sửa chú thích trong `backend/.env.example` gợi ý sinh bằng `openssl rand -base64 48`.
- `COOKIE_SECURE` (mới, tùy chọn): mặc định `true` ở `application.yml` và profile `prod`; `application-dev.yml` đặt `app.security.cookie-secure: false`, vì Safari và trình duyệt tự động hóa (Playwright) không nhận cookie `Secure` qua `http://localhost`. Đặt `false` ở prod chỉ khi demo qua http trên mạng LAN. Thêm vào `backend/.env.example` (để trống, có chú thích) và service `backend` trong `docker-compose.prod.yml` (`COOKIE_SECURE: ${COOKIE_SECURE:-true}`).
- Test: profile test đặt sẵn một `app.security.jwt-secret` dài ít nhất 32 byte, khác chuỗi mẫu, cùng `bcrypt-cost: 4`. Test cần đổi giờ dùng một bean `Clock` chỉnh được (ví dụ một lớp `MutableClock` trong `src/test`) thay cho `Clock` hệ thống, để vừa cố định giờ (AC-6) vừa tua giờ (token hết hạn, AC-7).
- Thuộc tính trong `application.yml` (không phải env): `app.security.access-token-ttl: PT15M`, `app.security.refresh-token-ttl: P7D`, `app.security.bcrypt-cost: 12`, `app.security.cookie-secure: ${COOKIE_SECURE:true}`, khai báo trong `AppProperties.Security` có validate.

### Critical test scenarios

Backend (Testcontainers, `SeedTestcontainersConfiguration` cho test cần tài khoản seed):
- Đăng nhập seed bằng số điện thoại, bằng email chữ hoa có khoảng trắng, bằng `+84 900 000 007`: 200, ba cookie đúng thuộc tính, body không có token, verifies **AC-1**, **AC-2**.
- Giải mã cookie `access_token` bằng khóa test: đủ claim, `exp - iat` = 900 giây, `vaiTro` của `0900000006` là `["BAN_QUAN_TRI","CU_DAN"]`, verifies **AC-7**.
- Sai mật khẩu, tài khoản không tồn tại, `taiKhoan` = `abc`: cùng 401 và `message`; một `PasswordEncoder` giả đếm số lần `matches` thấy đúng 1 lần ở mỗi nhánh, verifies **AC-3**.
- `0900000008` đúng mật khẩu: 403, không có dòng phiên mới; sai mật khẩu: 401, verifies **AC-4**.
- Body rỗng, `matKhau` 129 ký tự: 400 lỗi theo trường; `matKhau` 40 ký tự `ệ` (120 byte): 401, verifies **AC-5**.
- Sau đăng nhập: đúng một dòng `phien_dang_nhap` mới, `token_hash` = SHA-256 của cookie, `het_han_luc - tao_luc` = 7 ngày, `dang_nhap_cuoi_luc` = giờ của `Clock` cố định; hash seed cost 10 đổi thành `{bcrypt}$2a$12$...` và đăng nhập lần hai vẫn 200; `phien_ban` không đổi; hai lần đăng nhập song song đều 200, verifies **AC-6**.
- Gọi `/profile` không có cookie, với token hết hạn (tua `MutableClock` tới 16 phút sau), token ký bằng khóa khác, token `alg: none`, token đặt trong header `Authorization` thay vì cookie: đều 401 với body JSON `errorCode` `UNAUTHENTICATED` (không phải body rỗng). Gọi `/login`, `/actuator/health` kèm cookie `access_token` rác: vẫn thành công, verifies **AC-7**.
- `PasswordEncoder`: hash `{noop}ChungCu@2026` đặt bằng SQL cho một tài khoản test thì đăng nhập bằng `ChungCu@2026` trả 401, verifies **AC-3**.
- `TaiKhoanDangNhap.parse` chạy mọi dòng của bảng ví dụ ở *Luồng đăng nhập*, verifies **AC-2**.
- Nâng hash có điều kiện: đổi `mat_khau_hash` bằng SQL giữa lúc `matches` và lúc ghi (fake encoder chặn ở `encode`) thì hash mới không ghi đè, verifies **AC-6**.
- `CsrfHeaderFilter` chạy đúng một lần mỗi request (không bị đăng ký thành servlet filter riêng); `X-Requested-With: xmlhttprequest` bị chặn; `OPTIONS` không bị chặn, verifies **AC-10**.
- `/profile` của `0900000002` có mọi mã tòa và `maCanHo` rỗng; của `0900000007` có căn hộ seed và `maToaNha` rỗng; của `0900000006` có cả hai; hồ sơ cư trú `Hết hiệu lực` hoặc `ngay_ket_thuc` hôm qua không được tính; JSON không có `matKhauHash`, `soCccd`, verifies **AC-8**.
- Đăng nhập, rồi đổi `trang_thai_tai_khoan` sang `Tạm khóa` bằng SQL, gọi `/profile`: 403 và ba header `Set-Cookie` có `Max-Age=0` và đúng `Path` của từng cookie. Ký một token hợp lệ cho `sub` = `ND99999999` (không tồn tại), gọi `/profile`: 401 và cũng ba header xóa cookie, verifies **AC-9**.
- `POST /login` không có `X-Requested-With`: 403 `CSRF_REJECTED`; `GET /profile` không có header vẫn 200, verifies **AC-10**.
- Unit test `AppProperties`: thiếu, 31 byte, chuỗi mẫu đều làm context không khởi động; `COOKIE_SECURE=false` cho cookie không có `Secure`, verifies **AC-11**.
- Bắt log trong test đăng nhập thành công, thất bại, và lỗi validation (`matKhau` 129 ký tự, `taiKhoan` rỗng): không chứa mật khẩu, cookie, hash hay `taiKhoan`, verifies **AC-16**.
- Unit test `TaiKhoanDangNhap.parse` và `HoSoService` (fake qua constructor) cho các nhánh chuẩn hóa và phạm vi, verifies **AC-2**, **AC-8**.

Frontend (Vitest + Testing Library, axe):
- Form đăng nhập: nhãn, `autocomplete`, nút hiện/ẩn có `aria-pressed`, lỗi 401 hiện `FormAlert` có focus và xóa ô mật khẩu, thành công gọi `router.replace` với `next` an toàn hoặc `/ho-so`, `ly-do` lạ không hiện gì, axe sạch, verifies **AC-12**.
- Layout `(app)`: mục "Hồ sơ" có `aria-current="page"` ở `/ho-so`; `userSlot` hiện `hoTen` khi có dữ liệu, trống khi đang tải, verifies **AC-14**.
- `safeNext` với `/ho-so?x=1`, `//evil.com`, `/\evil.com`, `https://evil.com`, `/dang-nhap`, chuỗi rỗng, verifies **AC-12**, **AC-13**.
- `proxy` với và không có cookie `phien`, cho `/ho-so`, `/`, `/dang-nhap`, `/dev/ui`, verifies **AC-13**.
- Trang hồ sơ với dữ liệu hai vai trò, danh sách rỗng, đang tải, lỗi; axe sạch; không có chữ CCCD, verifies **AC-14**.
- `QueryClient`: query trả 401 gọi chuyển trang đúng URL (đã mã hóa `next`); hai query cùng lỗi 401 chỉ chuyển trang một lần; mutation có `meta.boQuaChuyenTrang` thì không; middleware gắn `X-Requested-With`, verifies **AC-10**, **AC-15**.
- Kiểm tay với `pnpm dev` (theo `verify.md`): đăng nhập, xóa riêng cookie `access_token` trong DevTools, tải lại `/ho-so`: chuyển về `/dang-nhap?...&ly-do=het-phien` đúng một lần, không lặp; `proxy` chuyển hướng khi điều hướng phía client không mang `_rsc` vào `next`, verifies **AC-13**, **AC-15**.

## Build plan

Tracer Bullet: lát 1 dựng một đường mỏng chạy thật từ trang đăng nhập qua cookie, JWT, DB tới trang hồ sơ; các lát sau làm dày từng đoạn của chính đường đó.

1. **Đường mỏng: đăng nhập bằng số điện thoại tới trang hồ sơ.** `AppProperties.Security` mới (TTL, bcrypt cost, cookie secure, validate `JWT_SECRET`), `application-dev.yml` tắt cookie secure, cấu hình test và `MutableClock`; `JwtConfig` (encoder với header HS256, decoder, validator có `Clock`, converter, `BearerTokenResolver` cookie bỏ qua mọi đường công khai); `PasswordEncoder` chỉ nhận `bcrypt`; sửa `SecurityConfig` (thu hẹp đường công khai về `POST /api/v1/auth/login`, resource server dùng chung entry point và access denied handler, `CsrfHeaderFilter` tạo bằng `new`, mã `CSRF_REJECTED`); entity `NguoiDungHeThong`, `PhienDangNhap` và repository; `VaiTro`, `VaiTroQuery`; `DangNhapService` nhánh thành công (tìm theo số điện thoại, mật khẩu đúng, tạo phiên, ký JWT); `AuthCookies`; `AuthController` với `/login` và `/profile` (thông tin tài khoản và `vaiTro`); sinh lại `openapi.json` và `pnpm gen:api`; middleware `X-Requested-With`; `safeNext`; `proxy.ts`; `/dang-nhap` với form tối thiểu; route group `(app)` với `AppShell` và `/ho-so` tối thiểu; `/` chuyển tới `/ho-so`; `.env.example`, `docker-compose.prod.yml`; test tích hợp happy path và test JWT. Satisfies **AC-1**, **AC-7**, **AC-10**, **AC-11**, **AC-13**.
2. **Làm dày đăng nhập: quy tắc và ghi DB.** `TaiKhoanDangNhap.parse` (email, `+84`); hash giả và nhánh không tìm thấy; giới hạn 72 byte; kiểm trạng thái khóa; validation; câu UPDATE `dang_nhap_cuoi_luc` và nâng hash; IP, user agent; log INFO; mã `AUTH_*`; test các nhánh, test song song, test log. Satisfies **AC-2**, **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-16**.
3. **Làm dày hồ sơ: phạm vi, chi tiết vai trò, khóa giữa chừng.** Package `canho` (`ToaNha`, `ToaNhaService`) và `cutru` (`HoSoCuTru`, `CuTruService`); `chiTietVaiTro`, `phamVi` trong `HoSoService`; `ApiException` có `headers` và `GlobalExceptionHandler` chép chúng; nhánh 403 và 401 xóa cookie; trang `/ho-so` đủ ba thẻ, nhãn `vai-tro.ts`, skeleton, `ErrorState`, `userSlot` họ tên; test backend và frontend. Satisfies **AC-8**, **AC-9**, **AC-14**.
4. **Làm dày giao diện đăng nhập và khóa lại.** Form đăng nhập đủ (hiện/ẩn mật khẩu, xóa ô mật khẩu khi lỗi, thông báo `ly-do`, chuyển `next`); từ điển lỗi; `onError` toàn cục trong `createQueryClient` (401 và khóa, bỏ qua mutation đăng nhập); test a11y với axe; security scheme Swagger `csrfHeader`; README (cách đăng nhập seed, `COOKIE_SECURE`); kiểm tay cả đường với `pnpm dev` theo `verify.md`; `mvnw.cmd verify`, `pnpm typecheck`, `pnpm lint`, `pnpm build`. Satisfies **AC-12**, **AC-13**, **AC-15**.

## Consequences

**Positive**:
- JavaScript không đọc được token (cookie `HttpOnly`), nên lỗi XSS (chèn script) không lấy cắp được phiên.
- Kiểm tra access token không cần DB, nên mọi API sau này không tốn thêm truy vấn để biết ai đang gọi và có vai trò gì.
- Tính năng 6 chỉ cần thêm endpoint làm mới và đăng xuất: bảng phiên, `ma_ho_phien`, cookie `refresh_token` và `phien` đã có, `proxy.ts` không phải sửa.
- Mã vai trò cố định, không dấu, nên tính năng 7 so sánh an toàn trong `@PreAuthorize`.

**Negative / tradeoffs**:
- Đổi vai trò hoặc khóa tài khoản mất tối đa 15 phút mới có hiệu lực ở các API dùng claim của token (trang hồ sơ thì có hiệu lực ngay vì đọc DB). Thu hồi phiên ở tính năng 11 rút ngắn khoảng này cho refresh, không cho access token đang sống.
- Trong lát 1 (chưa có tính năng 6), sau 15 phút người dùng bị đưa về trang đăng nhập dù phiên 7 ngày còn hạn.
- Chưa có giới hạn số lần thử và chưa ghi nhật ký kiểm toán cho đăng nhập cho tới tính năng 12 và 8; không bật bản demo ra Internet trước khi hai tính năng đó xong.
- bcrypt cost 12 tốn khoảng 250 ms CPU mỗi lần đăng nhập; lần đăng nhập đầu của mỗi tài khoản seed còn tốn thêm một lần băm lại.
- Swagger cần bấm "Authorize" nhập header trước khi thử request ghi.
- Phạm vi tòa của nhân viên là giả định (mọi tòa), sai khi khu có nhiều ban quản lý tách biệt.
- Trên dữ liệu seed (hash cost 10 chưa được nâng), thời gian phản hồi vẫn lộ tài khoản có tồn tại hay không.
- Chưa có đăng xuất cho tới tính năng 6.
- `dia_chi_ip` lấy qua `X-Forwarded-For` mà backend tin từ mọi nguồn, nên giả được nếu gọi thẳng cổng 8080; cột này chỉ để tham khảo, không dùng cho quyết định bảo mật nào.
- Không giới hạn tốc độ nên spam `/login` đúng mật khẩu làm `phien_dang_nhap` phình to và tốn CPU bcrypt; xử lý cùng tính năng 6 (dọn dòng) và 12 (giới hạn).
- Lệch file đặc tả: `vaiTro` là mã chứ không phải nhãn như ví dụ trong xlsx, và thêm `chiTietVaiTro`.

**Neutral**:
- Có thêm hai package `canho` và `cutru` chỉ với phần đọc; UC01 và UC02 sau này làm dày chính các package đó.
- `ApiException` có thêm trường `headers` dùng chung.
- `phien_dang_nhap` tích lũy dòng hết hạn; dọn dẹp đã ghi cho tính năng 6 (spec 0002).
- Khóa HS256 là bí mật dùng chung: nếu sau này một dịch vụ khác cần kiểm token mà không được ký, phải chuyển sang cặp khóa.

## Follow-up

- [ ] Tính năng 6: thêm `/api/v1/auth/refresh-token` vào danh sách đường mà `BearerTokenResolver` bỏ qua; `onError` 401 trong `createQueryClient` thử làm mới một lần trước khi chuyển trang; đăng xuất xóa ba cookie bằng `AuthCookies`.
- [ ] Tính năng 7: dùng authority `ROLE_<mã>` từ claim `vaiTro`; thêm mục menu vào `src/app/(app)/layout.tsx`; thay chuyển hướng `/` tới `/ho-so` bằng trang chủ theo vai trò.
- [ ] Tính năng 8: ghi nhật ký đăng nhập thành công và thất bại tại `DangNhapService` (file đặc tả yêu cầu).
- [ ] Tính năng 12: khóa tạm theo `so_lan_dang_nhap_sai`, `khoa_tam_den`, mã `AUTH_TOO_MANY_ATTEMPTS`; cân nhắc thêm giới hạn theo IP.
- [ ] Phạm vi tòa của nhân viên: khi khu có nhiều tòa do các nhóm khác nhau phụ trách, cần một spec (qua `/architect`) cho bảng gán nhân viên vào tòa, rồi sửa `phamVi.maToaNha`.
- [ ] Cập nhật file `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx` cho khớp: `vaiTro` là mã, có `chiTietVaiTro`, có cookie `phien`, có header `X-Requested-With`, mã `CSRF_REJECTED`.
- [ ] `/sync`: ghi vào `AGENTS.md` quy ước header `X-Requested-With`, cookie xác thực và mã vai trò khi tính năng này xong.

## Rationale

Lý do và các phương án đã cân nhắc: xem [rationale.md](rationale.md).
