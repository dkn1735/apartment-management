# 0010. Quên mật khẩu qua email: link một lần trong fragment, gửi SMTP sau commit trên luồng nền, phản hồi giống nhau mọi trường hợp

**Date**: 2026-11-23
**Status**: Accepted

## Summary

Người dùng quên mật khẩu nhập email trên trang `/quen-mat-khau` và nhận một link đặt lại, sống 30 phút, chỉ dùng được một lần. Backend luôn trả cùng một câu, dù email có tài khoản hay không, và gửi email sau khi trả phản hồi (trên một luồng nền), để không ai dò được email nào có tài khoản qua nội dung hay thời gian phản hồi. Email đi qua SMTP bằng `spring-boot-starter-mail`: dev dùng Mailpit (hộp thư giả chạy trong Docker), bản demo dùng Gmail. Cần một migration `V14`, ba endpoint công khai, hai trang mới, hai loại nhật ký mới, và móc việc hủy link vào các luồng đổi mật khẩu, khóa và sửa tài khoản có sẵn.

## Requirements

**User stories**:
- Là cư dân hoặc nhân viên quên mật khẩu, tôi muốn tự đặt lại qua email đã đăng ký, để không phải chờ ban quản lý cấp mật khẩu tạm.
- Là nhóm phát triển, tôi muốn trang quên mật khẩu không tiết lộ email nào có tài khoản, và không bị dùng để spam hộp thư người khác.
- Là chủ tài khoản, tôi muốn đặt lại xong thì mọi máy đang đăng nhập bị đăng xuất, để ai đó đang giữ phiên của tôi mất quyền truy cập.

**Thuật ngữ dùng trong spec**:
- **Token**: chuỗi ngẫu nhiên 32 byte, mã hóa base64url không đệm (43 ký tự), sinh bằng `RefreshTokenGenerator.moi()`. Chỉ nằm trong email và trong bộ nhớ lúc gửi; DB chỉ lưu `RefreshTokenGenerator.sha256Hex(token)`.
- **Yêu cầu mở**: dòng `yeu_cau_dat_lai_mat_khau` có `da_dung_luc IS NULL AND huy_luc IS NULL` (chưa dùng, chưa bị hủy; có thể đã hết hạn).
- **Yêu cầu còn hiệu lực**: yêu cầu mở có `het_han_luc > now`, và tài khoản của nó đang `Hoạt động`.
- **Tài khoản nhận được email**: tài khoản có `trang_thai_tai_khoan = 'Hoạt động'`, mọi vai trò kể cả Admin.
- **Câu phản hồi chung**: `message` "Nếu email này thuộc một tài khoản đang hoạt động, link đặt lại mật khẩu sẽ được gửi tới trong vài phút."

**Acceptance criteria**:
- **AC-1**: `POST /api/v1/auth/password-reset/request` với email của một tài khoản nhận được email trả 200, `data` null, câu phản hồi chung. Sau đó đúng một email tới địa chỉ `email` của tài khoản, chứa link `${app.public-url}/dat-lai-mat-khau#token=<token>` ở cả phần HTML lẫn phần chữ thường. DB có đúng một dòng mới với `token_hash = sha256Hex(token)` và `het_han_luc = tao_luc + 30 phút`.
- **AC-2**: Email không thuộc tài khoản nào, email của tài khoản `Tạm khóa` hoặc `Ngừng hoạt động`, và yêu cầu bị giới hạn (AC-4) đều trả cùng status, cùng body và cùng header với AC-1 (trừ `requestId` và `Date`), không gửi email và không tạo dòng. Email được chuẩn hóa (`strip()`, `toLowerCase(Locale.ROOT)`) trong compact constructor của `YeuCauDatLaiRequest`, trước bean validation, nên `  CuDan@ChungCu.Test ` khớp `cudan@chungcu.test`. Ràng buộc định dạng và độ dài dùng lại đúng các annotation của trường `email` trong `TaoTaiKhoanRequest`, áp lên giá trị đã chuẩn hóa. Email rỗng, sai định dạng hoặc dài hơn 150 ký tự trả 400 `VALIDATION_FAILED` ở ô `email`, như nhau dù có tài khoản hay không.
- **AC-3**: Phản hồi của AC-1 trả về trước khi email được gửi. Máy chủ SMTP chậm hoặc lỗi không đổi status, body hay thời gian phản hồi; lỗi gửi được ghi log mức ERROR kèm `maYeuCau` và tên lớp lỗi, không kèm token, link hay địa chỉ email. Không có cơ chế gửi lại tự động; dòng yêu cầu vẫn còn, người dùng yêu cầu lại sau 60 giây. Email gửi trên executor riêng `emailExecutor` có giới hạn (2 luồng, hàng đợi 100); hàng đợi đầy thì email bị bỏ và ghi log ERROR kèm `maYeuCau`, phản hồi không đổi.
- **AC-4**: Với mỗi tài khoản: yêu cầu thứ hai trong vòng 60 giây kể từ dòng gần nhất không tạo dòng và không gửi email; khi đã có 5 dòng với `tao_luc` trong 60 phút gần nhất thì yêu cầu tiếp theo cũng vậy. Ngoài ra có trần chung cho cả hệ thống: khi toàn bảng đã có 100 dòng với `tao_luc` trong 60 phút gần nhất thì không tạo dòng nào nữa (log WARN `GIOI_HAN_CHUNG`), để không đốt hết hạn mức gửi mỗi ngày của Gmail. Mọi trường hợp bị giới hạn trả đúng như AC-2. 10 request song song cho cùng một email tạo đúng một dòng và một email.
- **AC-5**: Tạo yêu cầu mới thì mọi yêu cầu mở trước đó của tài khoản nhận `huy_luc = now` trong cùng transaction; link cũ từ đó trả `AUTH_RESET_TOKEN_INVALID`. DB bảo đảm mỗi tài khoản có nhiều nhất một yêu cầu mở (unique index một phần).
- **AC-6**: `POST /api/v1/auth/password-reset/check` với token của một yêu cầu còn hiệu lực trả 200 `data { hetHanLuc }` và không đổi dòng nào. Token sai định dạng, không có trong DB, đã hết hạn, đã dùng, đã bị hủy, hoặc thuộc tài khoản không còn `Hoạt động` đều trả 400 `AUTH_RESET_TOKEN_INVALID` với cùng một `message` "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu link mới.".
- **AC-7**: `POST /api/v1/auth/password-reset/confirm` với token còn hiệu lực và mật khẩu hợp lệ trả 200 `data` null, `message` "Đặt lại mật khẩu thành công". Sau đó: đăng nhập bằng mật khẩu mới được, bằng mật khẩu cũ thì không; dòng yêu cầu có `da_dung_luc = now`; tài khoản có `phai_doi_mat_khau = false`, `doi_mat_khau_luc = now`, `phien_ban` tăng 1, `so_lan_dang_nhap_sai = 0`, `khoa_tam_den` và `lan_sai_cuoi_luc` NULL; mọi họ phiên còn hiệu lực bị thu hồi với `ly_do_thu_hoi = 'Đặt lại mật khẩu'`, kể cả phiên của trình duyệt đang gửi request; response xóa cả ba cookie `access_token`, `refresh_token` và `phien` bằng `AuthCookies.xoa()`, và frontend gọi `queryClient.clear()` trước khi chuyển trang.
- **AC-8**: Token chỉ dùng được một lần: gọi `confirm` lần hai trả `AUTH_RESET_TOKEN_INVALID`. Hai `confirm` song song với cùng token cho đúng một 200 và một `AUTH_RESET_TOKEN_INVALID`, và mật khẩu cuối cùng là của request thành công. Qua `het_han_luc` (theo `Clock`) thì `check` và `confirm` đều trả `AUTH_RESET_TOKEN_INVALID`.
- **AC-9**: `matKhauMoi` theo đúng quy tắc của đổi mật khẩu: 8 tới 128 ký tự, có cả chữ và số, không quá 72 byte UTF-8. Vi phạm trả 400 `VALIDATION_FAILED` ở ô `matKhauMoi` và không tiêu token (gọi lại với mật khẩu đúng vẫn được). Mật khẩu mới được phép trùng mật khẩu hiện tại. Thứ tự kiểm cố định: validate body trước (thiếu ô, quy tắc mật khẩu, 72 byte), rồi mới kiểm token; nên mật khẩu sai quy tắc cộng token hỏng trả `VALIDATION_FAILED`. Body hợp lệ mà token không hợp lệ thì trả `AUTH_RESET_TOKEN_INVALID` mà không chạy bcrypt.
- **AC-10**: Mọi yêu cầu mở của một tài khoản nhận `huy_luc` trong cùng transaction khi: người dùng tự đổi mật khẩu (`GhiMatKhauMoiService`), Admin hoặc BQL đặt lại mật khẩu (`GhiDatLaiMatKhauService`), tài khoản chuyển sang `Tạm khóa` hoặc `Ngừng hoạt động` (`TrangThaiTaiKhoanService`), hoặc email của tài khoản bị sửa sang giá trị khác (`CapNhatTaiKhoanService`).
- **AC-11**: Nhật ký kiểm toán có hai loại mới: `AUTH_YEU_CAU_DAT_LAI_MAT_KHAU` ("Yêu cầu đặt lại mật khẩu qua email") ghi khi một dòng yêu cầu được tạo, người thực hiện NULL, đối tượng là tài khoản; `AUTH_DAT_LAI_MAT_KHAU_QUA_EMAIL` ("Đặt lại mật khẩu qua email") ghi khi `confirm` thành công, người thực hiện là chính tài khoản, chi tiết `{ soPhienThuHoi }`. Cả hai có `dia_chi_ip` và `request_id` như các loại khác. Yêu cầu không tạo dòng (AC-2) không ghi nhật ký, chỉ ghi log ứng dụng mức INFO với lý do (`KHONG_CO_TAI_KHOAN`, `KHONG_HOAT_DONG`, `GIOI_HAN`) và `maNguoiDung` nếu có. Không dòng nhật ký hay log nào chứa token, hash của token, link, hay email người dùng gõ. Trang `/nhat-ky` hiện nhãn hai loại mới và có chúng trong bộ lọc.
- **AC-12**: Job dọn chạy theo `app.security.session-cleanup-cron` xóa các dòng `yeu_cau_dat_lai_mat_khau` có `tao_luc` cũ hơn 7 ngày; dòng 6 ngày còn lại. Lỗi trong job được ghi log ERROR và không ném ra ngoài.
- **AC-13**: Giao diện: trang `/dang-nhap` có link "Quên mật khẩu?" tới `/quen-mat-khau`. `/quen-mat-khau` có ô email có nhãn, lỗi gắn với ô (`aria-describedby`), sau khi gửi thì hiện câu phản hồi chung trong vùng `role="status"` kèm câu "Link có hiệu lực 30 phút." và link về trang đăng nhập. `/dat-lai-mat-khau` đọc token từ fragment đúng một lần (giữ trong `useRef`, để effect chạy hai lần ở React StrictMode không thấy hash rỗng), xóa fragment khỏi thanh địa chỉ bằng `history.replaceState`, gọi `check`; tải lại trang sau đó thì không còn token và trang hiện câu của AC-6 (chấp nhận). Token không hợp lệ thì hiện câu của AC-6 và link tới `/quen-mat-khau`; hợp lệ thì hiện "Link có hiệu lực tới HH:mm ngày dd/MM/yyyy" (từ `hetHanLuc`, định dạng qua `format.ts` theo giờ Việt Nam) và form hai ô "Mật khẩu mới" và "Nhập lại mật khẩu mới" (không khớp thì báo lỗi ở ô thứ hai, không gọi API) cùng phần quy tắc mật khẩu dùng lại từ form đổi mật khẩu; thành công thì chuyển tới `/dang-nhap?ly-do=dat-lai-mat-khau`, trang đăng nhập hiện "Đã đặt lại mật khẩu. Vui lòng đăng nhập bằng mật khẩu mới.". Cả hai trang dùng được hoàn toàn bằng bàn phím, focus nhìn thấy được, mở được khi chưa đăng nhập (không bị `proxy.ts` chuyển đi) và cả khi đang đăng nhập.
- **AC-14**: Cấu hình: profile `prod` thiếu `MAIL_HOST`, `MAIL_FROM` hoặc `APP_PUBLIC_URL` thì backend dừng lúc khởi động với thông báo nêu tên biến. `MAIL_HOST` nằm trong `application-prod.yml` dạng `${MAIL_HOST}` không có giá trị mặc định (vì thiếu `spring.mail.host` thì Spring Boot chỉ lặng lẽ không tạo `JavaMailSender`); `app.public-url` và `app.mail.from` có `@NotBlank` trong `AppProperties`. `app.public-url` phải bắt đầu bằng `http://` hoặc `https://`, dấu `/` ở cuối bị bỏ khi nạp. Profile `dev` mặc định gửi tới Mailpit ở `localhost:1025` và dùng `http://localhost:3000` làm URL gốc. `docker compose up -d` khởi động thêm Mailpit, giao diện xem thư ở `http://localhost:8025`.
- **AC-15**: Token không bao giờ đi trong query string hay path: link dùng fragment, `check` và `confirm` nhận token trong body JSON. Trang `/dat-lai-mat-khau` trả header `Referrer-Policy: no-referrer`.

## Decision

**Chosen option**: Option 1: link một lần (token trong fragment, DB lưu SHA-256), gửi qua SMTP bằng `spring-boot-starter-mail` sau commit trên luồng nền, phản hồi giống nhau ở mọi nhánh, giới hạn theo tài khoản đếm bằng chính bảng yêu cầu.

Không thêm bảng mới: bảng `yeu_cau_dat_lai_mat_khau` có sẵn từ spec 0002 được thêm cột `huy_luc` và hai index. Phân hệ `auth` lo nghiệp vụ; việc gửi email là một khả năng kỹ thuật dùng chung, nằm ở `common.email` để các phân hệ sau (thông báo phí, sự cố) dùng lại.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`), cho cấu hình `spring.mail.*`, `@ConfigurationProperties`, `JdbcClient` và sự kiện sau commit; riêng lỗi theo vỏ bọc của spec 0001 · `ui-styling` (`.claude/skills/ui-styling/`), form, khung trạng thái và nút bằng shadcn/ui · `next-dev-loop` (`vercel/next.js`, `.agents/skills/next-dev-loop/`), kiểm hai trang công khai và header khi `pnpm dev` đang chạy.

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

**Data model sketch** (một migration `V14__dat_lai_mat_khau_email.sql`):

| Bảng | Thay đổi | Chi tiết |
|---|---|---|
| `yeu_cau_dat_lai_mat_khau` | thêm cột | `huy_luc` TIMESTAMPTZ NULL: lúc bị yêu cầu mới hoặc sự kiện ở AC-10 thay thế |
| | thêm CHECK | `chk_yeu_cau_dat_lai_mat_khau_ket_thuc CHECK (da_dung_luc IS NULL OR huy_luc IS NULL)` |
| | thêm unique index một phần | `uq_yeu_cau_dat_lai_mat_khau_mo ON (ma_nguoi_dung) WHERE da_dung_luc IS NULL AND huy_luc IS NULL` |
| | thay index | bỏ `idx_yeu_cau_dat_lai_mat_khau_ma_nguoi_dung`, tạo `idx_yeu_cau_dat_lai_mat_khau_nguoi_dung_tao ON (ma_nguoi_dung, tao_luc DESC)`; vẫn bắt đầu bằng cột khóa ngoại nên đúng quy ước "mọi khóa ngoại có index" của spec 0002 |
| | thêm index | `idx_yeu_cau_dat_lai_mat_khau_tao_luc ON (tao_luc)` cho job dọn |
| | giữ nguyên | `ma_yeu_cau` PK identity; `ma_nguoi_dung` FK RESTRICT (N:1 tới `nguoi_dung_he_thong`); `token_hash` UNIQUE + CHECK hex; `tao_luc`; `het_han_luc` + CHECK `> tao_luc`; `da_dung_luc` |
| `nguoi_dung_he_thong` | không đổi schema | `confirm` ghi `mat_khau_hash`, `phai_doi_mat_khau`, `doi_mat_khau_luc`, `phien_ban`, `cap_nhat_luc`, `so_lan_dang_nhap_sai`, `khoa_tam_den`, `lan_sai_cuoi_luc` |
| `phien_dang_nhap` | không đổi schema | `ly_do_thu_hoi = 'Đặt lại mật khẩu'` đã có trong CHECK và `LyDoThuHoi.DAT_LAI_MAT_KHAU` |

Đọc ghi bảng yêu cầu qua `JdbcClient` (như `TaiKhoanGhi`), không cần entity JPA. Tham số thời gian bind kiểu `OffsetDateTime` UTC.

**State transitions** (một dòng `yeu_cau_dat_lai_mat_khau`):

```
(tạo, mở) ──confirm thành công──▶ đã dùng   (da_dung_luc = now)
    │
    ├──yêu cầu mới / AC-10──────▶ đã hủy    (huy_luc = now)
    │
    └──qua het_han_luc──────────▶ hết hạn   (không ghi gì; vẫn là "mở" về mặt index cho tới khi bị hủy)
mọi trạng thái ──tao_luc cũ hơn 7 ngày──▶ bị xóa (job dọn)
```

Đã dùng và đã hủy là trạng thái cuối; CHECK chặn một dòng có cả hai. Một dòng hết hạn mà chưa bị hủy vẫn giữ chỗ trong unique index, nên yêu cầu mới luôn hủy các dòng mở trước khi chèn.

**Thứ tự khóa** (mọi luồng, để không deadlock): luôn khóa dòng `nguoi_dung_he_thong` của tài khoản (`FOR UPDATE`) trước, rồi mới ghi các dòng `yeu_cau_dat_lai_mat_khau` của nó. Luồng yêu cầu, luồng xác nhận và bốn luồng ở AC-10 (vốn đã khóa hoặc cập nhật dòng tài khoản trước) đều theo thứ tự này.

**Luồng yêu cầu** (`QuenMatKhauService.yeuCau`, `now` lấy một lần ở đầu, cắt tới micro giây):

1. Email đã được chuẩn hóa và validate trong DTO (AC-2). `QuenMatKhauRepository.timTheoEmail(email)` đọc `ma_nguoi_dung`, `ho_ten`, `email`, `trang_thai_tai_khoan` (so bằng trên giá trị đã chuẩn hóa; cột `email` vốn đã viết thường nhờ CHECK). Không có, hoặc không `Hoạt động`: log INFO lý do, trả câu phản hồi chung.
2. Transaction (`GhiYeuCauDatLaiService`, giữ ngắn nhất có thể vì các request song song chờ khóa cũng giữ kết nối pool):
   1. `SELECT ... FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = :ma FOR UPDATE` để các yêu cầu song song cho cùng tài khoản xếp hàng; đọc lại trạng thái, không `Hoạt động` thì thoát như bước 1.
   2. Đếm dòng toàn bảng có `tao_luc > now - 60 phút`; đủ 100: log WARN `GIOI_HAN_CHUNG`, thoát. Đếm dòng của tài khoản có `tao_luc > now - 60 phút` và lấy `max(tao_luc)`. Đủ 5 dòng, hoặc `max(tao_luc) > now - 60 giây`: log INFO `GIOI_HAN`, thoát (không ghi gì).
   3. `UPDATE yeu_cau_dat_lai_mat_khau SET huy_luc = :now WHERE ma_nguoi_dung = :ma AND da_dung_luc IS NULL AND huy_luc IS NULL`.
   4. Sinh token, chèn dòng mới (`het_han_luc = now + 30 phút`).
   5. `NhatKyService.ghi(AUTH_YEU_CAU_DAT_LAI_MAT_KHAU)` (hoãn tới sau commit như spec 0007).
   6. Phát sự kiện `YeuCauDatLaiDaTao(maYeuCau, maNguoiDung, email, hoTen, token, hetHanLuc)` qua `ApplicationEventPublisher`.
3. Trả câu phản hồi chung.

`EmailDatLaiMatKhauListener` nhận sự kiện bằng `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = false)` cộng `@Async("emailExecutor")`, dựng email bằng `EmailDatLaiMatKhau` rồi gọi `EmailSender.gui`. `emailExecutor` (trong `AsyncConfig`, có `@EnableAsync`) là `ThreadPoolTaskExecutor` 2 luồng, hàng đợi 100, `RejectedExecutionHandler` ghi log ERROR và bỏ email (AC-3); không dùng executor mặc định vì nó không giới hạn. Bắt mọi lỗi khi gửi, log ERROR theo AC-3. Transaction rollback thì sự kiện không bao giờ tới listener. Chỉ `GhiYeuCauDatLaiService` phát sự kiện này, luôn bên trong transaction. `toString()` của sự kiện không in token hay email.

**Luồng xác nhận** (`XacNhanQuenMatKhauService.xacNhan`):

1. Bean validation của `XacNhanDatLaiRequest` (quy tắc của `DoiMatKhauRequest.matKhauMoi`), rồi kiểm 72 byte như `DoiMatKhauService`; lỗi trả 400 `VALIDATION_FAILED` (AC-9). Bước này luôn chạy trước mọi kiểm tra token.
2. `RefreshTokenGenerator.dungDinhDang(token)` sai thì `AUTH_RESET_TOKEN_INVALID`. Tra yêu cầu còn hiệu lực theo `sha256Hex(token)` (một câu SELECT nối `nguoi_dung_he_thong`, không khóa), lấy `ma_nguoi_dung`. Không có thì `AUTH_RESET_TOKEN_INVALID`, chưa chạy bcrypt.
3. Băm mật khẩu mới bằng `PasswordEncoder` ngoài transaction.
4. Transaction (`GhiXacNhanQuenMatKhauService`), theo thứ tự khóa ở trên:
   1. Khóa dòng tài khoản `ma_nguoi_dung` (từ bước 2) `FOR UPDATE`; không `Hoạt động` thì ném `AUTH_RESET_TOKEN_INVALID`.
   2. `UPDATE yeu_cau_dat_lai_mat_khau SET da_dung_luc = :now WHERE token_hash = :h AND ma_nguoi_dung = :ma AND da_dung_luc IS NULL AND huy_luc IS NULL AND het_han_luc > :now`. 0 dòng: ném `AUTH_RESET_TOKEN_INVALID` (rollback; đây là chỗ chặn hai `confirm` song song và mọi lần hủy xen giữa bước 2 và bước 4, AC-8).
   3. `TaiKhoanGhi.datMatKhauQuaEmail(ma, hash, now)`: như `datLaiMatKhau` (cùng việc xóa `so_lan_dang_nhap_sai`, `khoa_tam_den`, `lan_sai_cuoi_luc`) nhưng `phai_doi_mat_khau = false`.
   4. `ThuHoiPhienService.thuHoi(ma, null, now, LyDoThuHoi.DAT_LAI_MAT_KHAU)`.
   5. `NhatKyService.ghi(AUTH_DAT_LAI_MAT_KHAU_QUA_EMAIL, người thực hiện = ma, ChiTietDatLaiQuaEmail(soHo))`.
5. Controller xóa cả ba cookie (`access_token`, `refresh_token`, `phien`) bằng `AuthCookies.xoa()` rồi trả 200.

`check` chạy đúng bước 2 và trả `hetHanLuc`.

**API surface** (cả ba trong `QuenMatKhauController`, thêm vào `PublicPaths.CONG_KHAI` với method `POST`, handler mang `@CongKhai`, đi qua `CsrfHeaderFilter` nên cần header `X-Requested-With: XMLHttpRequest` như `/login`; operationId lần lượt là `yeuCauDatLaiMatKhauEmail`, `kiemTraTokenDatLai`, `xacNhanDatLaiMatKhauEmail`):

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/auth/password-reset/request` | POST | `email`: string (req, ≤150, định dạng email) | 200 `data` null, câu phản hồi chung | công khai | 400 `VALIDATION_FAILED` (ô `email`), 403 `CSRF_REJECTED` |
| `/api/v1/auth/password-reset/check` | POST | `token`: string (req, ≤64) | 200 `data { hetHanLuc: Instant }`, `message` "Link đặt lại mật khẩu hợp lệ" | công khai | 400 `AUTH_RESET_TOKEN_INVALID`, 403 `CSRF_REJECTED` |
| `/api/v1/auth/password-reset/confirm` | POST | `token`: string (req, ≤64), `matKhauMoi`: string (req, 8 tới 128, chữ và số) | 200 `data` null, `message` "Đặt lại mật khẩu thành công", `Set-Cookie` xóa hai cookie phiên | công khai | 400 `VALIDATION_FAILED` (ô `matKhauMoi`), 400 `AUTH_RESET_TOKEN_INVALID`, 403 `CSRF_REJECTED` |

Mã lỗi mới: `AuthErrorCodes.AUTH_RESET_TOKEN_INVALID` (HTTP 400) và hằng message đi kèm; frontend thêm cùng câu vào `error-messages.ts`. `toString()` của hai request DTO có token và mật khẩu in `***`.

**Frontend**:
- `frontend/src/app/quen-mat-khau/page.tsx` và `frontend/src/app/dat-lai-mat-khau/page.tsx`, cùng bố cục với `/dang-nhap`. Gọi API chỉ qua hàm `api`, xử lý `ApiError` theo `errorCode`.
- `proxy.ts`: thêm `quen-mat-khau` và `dat-lai-mat-khau` vào danh sách loại trừ của `matcher`, như `dang-nhap`. Không khai trong `muc-chuc-nang.ts` vì là trang công khai ngoài khung app, như `/dang-nhap`.
- `login-form.tsx`: link "Quên mật khẩu?" cạnh ô mật khẩu; thêm `"dat-lai-mat-khau"` vào `LY_DO` với tone `success`.
- `next.config.ts`: `headers()` trả `Referrer-Policy: no-referrer` cho `/dat-lai-mat-khau`.

**Email** (`auth.service.EmailDatLaiMatKhau`, dựng từ text block, không template engine):
- Tiêu đề: "Đặt lại mật khẩu tài khoản chung cư".
- Nội dung: "Xin chào {hoTen}," · câu nói có người vừa yêu cầu đặt lại mật khẩu cho tài khoản này · nút và link dạng chữ tới link đặt lại · "Link có hiệu lực tới {HH:mm ngày dd/MM/yyyy} và chỉ dùng được một lần." · "Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu của bạn không thay đổi.".
- `hoTen` được escape HTML ở phần HTML. Giờ hết hạn đổi sang múi giờ `app.timezone`.
- `MimeMessageHelper` multipart, UTF-8, phần chữ thường và phần HTML; người gửi là `app.mail.from`.

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| Yêu cầu | email để tra | input `email`, chuẩn hóa `strip()` + `toLowerCase(Locale.ROOT)` |
| Yêu cầu | tài khoản, trạng thái | cột `nguoi_dung_he_thong.email` (UNIQUE), `trang_thai_tai_khoan` |
| Yêu cầu | đếm 60 giây, 5 lần/giờ | `count(*)`, `max(tao_luc)` trên `yeu_cau_dat_lai_mat_khau` của tài khoản; hằng `KHOANG_CACH = 60s`, `TOI_DA_MOI_GIO = 5`, `CUA_SO = 60 phút` trong `QuenMatKhauService` |
| Yêu cầu | trần chung 100/giờ | `count(*)` toàn bảng với `tao_luc > now - CUA_SO` (dùng index `tao_luc`); hằng `TRAN_CHUNG_MOI_GIO = 100` |
| Yêu cầu | token | `RefreshTokenGenerator.moi()` (32 byte ngẫu nhiên, base64url) |
| Yêu cầu | `token_hash` | `RefreshTokenGenerator.sha256Hex(token)` |
| Yêu cầu | `tao_luc`, `het_han_luc` | bean `Clock`; `het_han_luc = now + app.security.password-reset-ttl` (PT30M) |
| Email | địa chỉ nhận | cột `email` của tài khoản (bằng email đã chuẩn hóa) |
| Email | `hoTen` | cột `ho_ten` |
| Email | link | `app.public-url` + `/dat-lai-mat-khau#token=` + token |
| Email | giờ hết hạn hiển thị | `het_han_luc` đổi sang `app.timezone` |
| Email | người gửi | `app.mail.from` (`MAIL_FROM`) |
| Check | `hetHanLuc` | cột `het_han_luc` |
| Confirm | tài khoản cần đổi | `RETURNING ma_nguoi_dung` của câu tiêu token |
| Confirm | `mat_khau_hash` mới | `PasswordEncoder.encode(matKhauMoi)` (cost `app.security.bcrypt-cost`) |
| Confirm | `soPhienThuHoi` | giá trị trả về của `ThuHoiPhienService.thuHoi` |
| Nhật ký | `dia_chi_ip`, `request_id` | như spec 0007 (`NhatKyService` đọc từ request) |
| Trang đặt lại | token | `window.location.hash`, khóa `token` (`URLSearchParams` trên phần sau `#`), đọc một lần vào `useRef` |
| Trang đặt lại | giờ hết hạn hiển thị | `hetHanLuc` của `check`, định dạng bằng `format.ts` theo `Asia/Ho_Chi_Minh` |
| Job dọn | mốc xóa | `now - 7 ngày`; hằng `GIU_LAI = 7 ngày` trong `YeuCauDatLaiCleanupJob` |
| Trang quên mật khẩu | câu "Link có hiệu lực 30 phút." | hằng frontend `THOI_HAN_LINK_PHUT = 30`, cùng giá trị với `password-reset-ttl` |
| Trang đăng nhập | câu thành công | `LY_DO["dat-lai-mat-khau"]`, từ `?ly-do=dat-lai-mat-khau` |

**Key invariants**:
- DB không bao giờ chứa token thô; log, nhật ký và thông báo lỗi không bao giờ chứa token, hash của token, link, hay email người dùng gõ.
- Mỗi tài khoản có nhiều nhất một yêu cầu mở (unique index một phần), và một dòng không thể vừa đã dùng vừa đã hủy (CHECK).
- Một token đổi mật khẩu được nhiều nhất một lần: điều kiện `da_dung_luc IS NULL` nằm trong chính câu `UPDATE` tiêu token.
- Phản hồi của `request` chỉ phụ thuộc vào việc input có hợp lệ về định dạng hay không, không phụ thuộc vào dữ liệu trong DB.
- Đặt lại thành công luôn đi kèm thu hồi mọi họ phiên trong cùng transaction; không có đường nào đổi mật khẩu qua token mà phiên cũ còn sống.
- Mọi luồng đổi `mat_khau_hash`, khóa tài khoản hay đổi email đều hủy yêu cầu mở trong cùng transaction (AC-10), qua `HuyYeuCauDatLaiService.huyConMo(maNguoiDung, now)` có `Propagation.MANDATORY` như `ThuHoiPhienService`. Lớp này nhỏ và chỉ phụ thuộc repository, để bốn service kia gọi mà không kéo theo luồng email.
- Thứ tự khóa luôn là dòng tài khoản rồi tới dòng yêu cầu (xem *Thứ tự khóa*).
- Backend không đọc header `Host` hay `X-Forwarded-Host` để dựng link; chỉ dùng `app.public-url`.

**Lớp mới và lớp sửa** (mỗi lớp một trách nhiệm, dưới khoảng 200 dòng):
- `common.email.EmailSender` (interface: `gui(EmailGui)`), `common.email.SmtpEmailSender` (dùng `JavaMailSender`), `common.email.EmailGui` (record: người nhận, tiêu đề, chữ thường, HTML). Interface có lý do theo `AGENTS.md`: unit test của service thay bằng bản giả ghi vào bộ nhớ.
- `common.config.AsyncConfig` (`@EnableAsync`, bean `emailExecutor` có giới hạn).
- Mở rộng `AppProperties` (record): thêm `publicUrl` (`@NotBlank`, mẫu `^https?://`, bỏ `/` cuối trong compact constructor) và `mail` (record con `Mail(@NotBlank String from)`); thêm `passwordResetTtl` vào record con `Security`.
- `auth.controller.QuenMatKhauController` (ba handler; tách khỏi `AuthController` để giữ mỗi lớp dưới khoảng 200 dòng). Tiền tố `QuenMatKhau` để không nhầm với `DatLaiMatKhauService` có sẵn (Admin đặt lại, spec 0008).
- `auth.dto.YeuCauDatLaiRequest` (chuẩn hóa email trong compact constructor), `KiemTraDatLaiRequest`, `KiemTraDatLaiResponse`, `XacNhanDatLaiRequest`.
- `auth.repository.QuenMatKhauRepository` (`JdbcClient`: tìm theo email, khóa tài khoản, đếm theo tài khoản và toàn bảng, hủy mở, chèn, tra còn hiệu lực, tiêu token, xóa cũ).
- `auth.service.QuenMatKhauService` (luồng yêu cầu), `GhiYeuCauDatLaiService` (transaction yêu cầu), `XacNhanQuenMatKhauService` (check và confirm), `GhiXacNhanQuenMatKhauService` (transaction confirm), `HuyYeuCauDatLaiService` (`huyConMo`), `EmailDatLaiMatKhau` (dựng nội dung), `EmailDatLaiMatKhauListener`, `YeuCauDatLaiDaTao` (record sự kiện), `ChiTietDatLaiQuaEmail`, `YeuCauDatLaiCleanupJob`.
- Test: lớp gốc integration test đăng ký GreenMail (`GreenMailExtension` cổng ngẫu nhiên) và đặt `spring.mail.host`, `spring.mail.port` qua `@DynamicPropertySource`; `greenmail-junit5` ghim phiên bản cụ thể trong `pom.xml`.
- Sửa: `TaiKhoanGhi` (thêm `datMatKhauQuaEmail`), `GhiMatKhauMoiService`, `GhiDatLaiMatKhauService`, `TrangThaiTaiKhoanService`, `CapNhatTaiKhoanService` (gọi `HuyYeuCauDatLaiService.huyConMo` sau khi đã ghi dòng tài khoản), `LoaiThaoTac` (hai giá trị mới), `AuthErrorCodes`, `PublicPaths`, `AppProperties`.

**Security model**:
- Ba endpoint công khai; không ai cần đăng nhập. Ai cầm token còn hiệu lực là người được đặt mật khẩu cho tài khoản đó: sở hữu hộp thư là bằng chứng duy nhất. Token 256 bit nên không cần giới hạn số lần đoán ở `check` và `confirm`.
- Chống dò email: AC-2 (cùng phản hồi), AC-3 (gửi sau phản hồi). Chống spam hộp thư: AC-4. Phiên của kẻ đang giữ tài khoản bị cắt: AC-7.
- Dữ liệu cá nhân: email và họ tên chỉ đi tới chính hộp thư của tài khoản. Không lưu IP hay thiết bị trong bảng yêu cầu; IP nằm trong nhật ký kiểm toán như mọi sự kiện khác. Không thuộc một chuẩn tuân thủ cụ thể (giống spec 0001).
- Rủi ro chấp nhận: ai chiếm được hộp thư của một người thì chiếm được tài khoản, kể cả Admin (engineer đã chọn cho Admin dùng tính năng này). Nhật ký `AUTH_DAT_LAI_MAT_KHAU_QUA_EMAIL` là dấu vết để điều tra.
- Link nằm trong fragment nên không lọt vào log máy chủ, proxy hay header `Referer`; trang xóa fragment ngay khi đọc.

**Configuration required**:
- `MAIL_HOST`, `MAIL_PORT`: máy chủ SMTP (bản demo `smtp.gmail.com`, `587`). Ánh xạ tới `spring.mail.host`, `spring.mail.port`. Dev mặc định `localhost`, `1025`.
- `MAIL_USERNAME`, `MAIL_PASSWORD`: tài khoản Gmail và App Password 16 ký tự (bí mật, chỉ trong `backend/.env` hoặc biến môi trường, không commit). Dev để trống, Mailpit không cần xác thực.
- `MAIL_FROM`: địa chỉ người gửi, ánh xạ tới `app.mail.from`; với Gmail phải trùng `MAIL_USERNAME`, có thể kèm tên hiển thị (`Chung cư <...@gmail.com>`).
- `APP_PUBLIC_URL`: URL gốc của frontend, ánh xạ tới `app.public-url`; dev mặc định `http://localhost:3000`, bản demo là địa chỉ mà máy người dùng mở được.
- Không phải biến môi trường: `spring.mail.host: ${MAIL_HOST}` (không mặc định) trong `application-prod.yml`, `localhost` trong `application-dev.yml`; `spring.mail.properties.mail.smtp.auth` và `starttls.enable` bật ở profile `prod`, tắt ở `dev`; `spring.mail.properties.mail.smtp.connectiontimeout`, `timeout`, `writetimeout` 10000 ms trong `application.yml` (mọi profile); `spring.mail.default-encoding: UTF-8`; `app.security.password-reset-ttl: PT30M`; job dọn dùng lại `app.security.session-cleanup-cron` và `app.timezone`.
- `docker-compose.yml`: service `mailpit` (image `axllent/mailpit` ghim phiên bản cụ thể), cổng `1025` và `8025`. `docker-compose.prod.yml`: truyền năm biến `MAIL_*` và `APP_PUBLIC_URL` vào backend, thiếu thì báo lỗi như `POSTGRES_PASSWORD`. Cập nhật `backend/.env.example`.
- Điều kiện trước khi demo: bật xác minh hai bước cho tài khoản Gmail gửi và tạo App Password.

**Critical test scenarios** (integration test với PostgreSQL thật qua Testcontainers và GreenMail nhúng làm máy chủ SMTP; unit test service với `EmailSender` giả và `Clock` cố định):
- Happy path: yêu cầu cho tài khoản seed, GreenMail nhận đúng một email, lấy token từ link, `check` trả `hetHanLuc`, `confirm` thành công, đăng nhập bằng mật khẩu mới được, mật khẩu cũ bị 401, verifies **AC-1**, **AC-6**, **AC-7**.
- Không lộ tài khoản: so status, body và header của bốn trường hợp (có tài khoản, không có, `Tạm khóa`, bị giới hạn) sau khi bỏ `requestId` và `Date`; GreenMail chỉ nhận email ở trường hợp đầu; email viết hoa có khoảng trắng vẫn khớp; email sai định dạng trả 400 như nhau, verifies **AC-2**.
- SMTP hỏng: `EmailSender` giả ném lỗi (hoặc GreenMail tắt), `request` vẫn 200; log bắt được có dòng ERROR kèm `maYeuCau`, không chứa token hay email, verifies **AC-3**.
- Giới hạn: hai yêu cầu cách 30 giây (dời `Clock`) chỉ một email; sáu yêu cầu cách 61 giây trong một giờ chỉ năm dòng; 10 luồng song song cho đúng một dòng và một email; chèn sẵn 100 dòng trong giờ cho các tài khoản khác thì yêu cầu mới không tạo dòng và có log WARN `GIOI_HAN_CHUNG`; executor giả từ chối thì có log ERROR và phản hồi vẫn 200, verifies **AC-3**, **AC-4**.
- Không deadlock: chạy song song nhiều lần một `request` và một `confirm` cho cùng tài khoản, cùng một lần Admin khóa tài khoản; không lần nào ra lỗi deadlock (40P01), verifies **AC-5**, **AC-8**, **AC-10**.
- Một link: yêu cầu hai lần cách 61 giây, link đầu trả `AUTH_RESET_TOKEN_INVALID`, dòng đầu có `huy_luc`; chèn tay dòng mở thứ hai cho cùng tài khoản bị DB từ chối (23505), verifies **AC-5**.
- Token hết hiệu lực: sai định dạng, không có, quá 30 phút, đã dùng, đã hủy, tài khoản bị khóa sau khi yêu cầu, đều cho cùng body `AUTH_RESET_TOKEN_INVALID` ở cả `check` và `confirm`; spy `PasswordEncoder` không được gọi ở `confirm`, verifies **AC-6**, **AC-8**, **AC-9**.
- Song song: hai `confirm` cùng token với hai mật khẩu khác nhau chạy cùng lúc, đúng một 200, mật khẩu đăng nhập được là của request đó, verifies **AC-8**.
- Phiên và cờ: đăng nhập trên hai máy, cho tài khoản có `phai_doi_mat_khau = true` và đang bị khóa tạm, rồi đặt lại qua email: cả hai họ phiên bị thu hồi với lý do `Đặt lại mật khẩu`, làm mới phiên trả 401, response có `Set-Cookie` xóa đủ `access_token`, `refresh_token` và `phien`, cờ và khóa tạm đã xóa, verifies **AC-7**.
- Cấu hình: khởi động context profile `prod` lần lượt thiếu `MAIL_HOST`, `MAIL_FROM`, `APP_PUBLIC_URL` đều thất bại và thông báo nêu tên; `APP_PUBLIC_URL=https://x.vn/` nạp thành `https://x.vn`; `ftp://x` bị từ chối, verifies **AC-14**.
- Mật khẩu: 7 ký tự, chỉ chữ, 73 byte đều 400 ở ô `matKhauMoi` và token vẫn dùng được sau đó; trùng mật khẩu hiện tại thì thành công, verifies **AC-9**.
- Hủy từ luồng khác: với một yêu cầu mở, lần lượt đổi mật khẩu, Admin đặt lại, khóa tài khoản, sửa email; mỗi lần dòng có `huy_luc` và link trả `AUTH_RESET_TOKEN_INVALID`; sửa tài khoản mà giữ nguyên email thì link vẫn dùng được, verifies **AC-10**.
- Nhật ký: một yêu cầu thành công và một lần đặt lại cho đúng hai dòng với hai loại mới, người thực hiện đúng như AC-11, có IP; yêu cầu cho email không có tài khoản không ghi dòng nào; quét `chi_tiet_thay_doi` và log bắt được không thấy token, hash, link hay email, verifies **AC-11**.
- Job dọn: dòng 8 ngày bị xóa, dòng 6 ngày còn; repository ném lỗi thì job log ERROR, verifies **AC-12**.
- Vitest + Testing Library: `/quen-mat-khau` hiện câu chung trong `role="status"` cho mọi phản hồi 200; lỗi ô email gắn `aria-describedby`; `/dat-lai-mat-khau` đọc token từ hash, gọi `replaceState` bỏ hash, vẫn gọi `check` với đúng token khi render trong `StrictMode`, hiện giờ hết hạn, hiện câu lỗi khi `check` trả `AUTH_RESET_TOKEN_INVALID`, báo lỗi ô nhập lại khi không khớp mà không gọi API, chuyển trang khi `confirm` thành công; `/dang-nhap?ly-do=dat-lai-mat-khau` hiện câu thành công và có link "Quên mật khẩu?"; trang `/nhat-ky` có nhãn hai loại mới, verifies **AC-11**, **AC-13**.
- Header và URL: `curl -I` trang `/dat-lai-mat-khau` thấy `Referrer-Policy: no-referrer`; OpenAPI của ba endpoint không có tham số query hay path, verifies **AC-15**.

## Build plan

Tracer Bullet: lát đầu chạy thật một đường mảnh từ trang quên mật khẩu, qua DB và SMTP, tới email trong Mailpit và trang đặt lại, chỉ cho tài khoản có thật. Các lát sau làm dày đường đó: không lộ tài khoản, giới hạn, an toàn của token, móc vào các luồng khác, nhật ký, rồi khóa lại giao diện.

1. [x] **Đường mảnh**: migration `V14__dat_lai_mat_khau_email.sql` (toàn bộ mô hình đích, vì nhỏ); `spring-boot-starter-mail` trong `pom.xml`, `greenmail-junit5` scope test; cấu hình `spring.mail.*`, mở rộng `AppProperties` (`public-url`, `mail.from`, `password-reset-ttl`) cho `dev`, `prod` và test, GreenMail trong lớp test gốc; Mailpit trong `docker-compose.yml`, biến trong `docker-compose.prod.yml` và `backend/.env.example`; `EmailSender`, `SmtpEmailSender`, `AsyncConfig` với `emailExecutor`; `QuenMatKhauRepository`, luồng yêu cầu chỉ nhánh có tài khoản (chưa giới hạn, chưa hủy), sự kiện và listener sau commit, `EmailDatLaiMatKhau`; luồng `confirm` cơ bản theo thứ tự khóa tài khoản rồi tiêu token (đổi hash, thu hồi phiên, xóa cờ và khóa tạm, xóa ba cookie, `queryClient.clear()`); `QuenMatKhauController`, `PublicPaths`, mã `AUTH_RESET_TOKEN_INVALID`; sinh lại `openapi.json` và kiểu frontend; hai trang ở dạng tối thiểu, loại trừ trong `proxy.ts`, link "Quên mật khẩu?" và `ly-do=dat-lai-mat-khau`. Integration test happy path với GreenMail, test khởi động thiếu cấu hình, satisfies **AC-1**, **AC-7**, **AC-14**
2. [x] **Không lộ tài khoản**: nhánh không có tài khoản và không `Hoạt động` trả cùng phản hồi, chuẩn hóa và validate email, log INFO có lý do; test so khớp phản hồi bốn trường hợp; test SMTP hỏng và log không chứa bí mật, satisfies **AC-2**, **AC-3**
3. [x] **Giới hạn và một link**: khóa dòng tài khoản, đếm 60 giây, 5 lần/giờ và trần chung 100/giờ, hủy yêu cầu mở trước khi chèn, executor từ chối thì log; test dời `Clock`, test 10 luồng song song, test unique index một phần, satisfies **AC-3**, **AC-4**, **AC-5**
4. [x] **An toàn của token**: endpoint `check`; mọi nhánh token không hợp lệ cùng một lỗi và không chạy bcrypt; hết hạn theo `Clock`; kiểm trạng thái tài khoản trong transaction; quy tắc mật khẩu và 72 byte không tiêu token; test hai `confirm` song song, satisfies **AC-6**, **AC-8**, **AC-9**
5. [x] **Hủy từ các luồng khác**: `HuyYeuCauDatLaiService.huyConMo` với `Propagation.MANDATORY`, gọi từ `GhiMatKhauMoiService`, `GhiDatLaiMatKhauService`, `TrangThaiTaiKhoanService` (chỉ khi rời `Hoạt động`) và `CapNhatTaiKhoanService` (chỉ khi email đổi), luôn sau khi đã ghi dòng tài khoản; test từng luồng và test không deadlock, satisfies **AC-10**, **AC-8**
6. [x] **Nhật ký, log và job dọn**: hai `LoaiThaoTac` mới, `ChiTietDatLaiQuaEmail`, nhãn và bộ lọc ở `/nhat-ky`; `YeuCauDatLaiCleanupJob`; test nhật ký và test quét bí mật trong nhật ký và log, satisfies **AC-11**, **AC-12**
7. [x] **Khóa lại giao diện**: đủ trạng thái cho hai trang (đang tải, lỗi, thành công), ô nhập lại mật khẩu, quy tắc mật khẩu hiển thị, focus sau mỗi lần đổi trạng thái, kiểm bàn phím và tương phản; `Referrer-Policy` trong `next.config.ts`; `verify.md` cho spec này (gồm bước demo: Admin sửa email một tài khoản seed sang hộp thư thật, đặt `MAIL_*` của Gmail); README phần chạy local nhắc Mailpit ở cổng 8025, satisfies **AC-13**, **AC-15**

## Consequences

**Positive**:
- Người dùng tự lấy lại tài khoản trong vài phút, ban quản lý bớt việc cấp mật khẩu tạm.
- Không dò được email nào có tài khoản qua trang này, cả qua nội dung lẫn qua thời gian phản hồi của SMTP.
- Một hộp thư không bị dội quá 5 email mỗi giờ từ trang này.
- Đặt lại xong, mọi phiên cũ bị cắt, nên tính năng này cũng là cách tự cứu khi nghi ngờ tài khoản bị chiếm.
- Có sẵn một lớp gửi email dùng chung (`common.email`) và một hộp thư dev (Mailpit) cho các phân hệ sau như thông báo phí, sự cố.

**Negative / tradeoffs**:
- Email gửi trên luồng nền không bền: backend dừng ngay sau commit, hoặc SMTP lỗi, thì email mất và không tự gửi lại. Người dùng phải bấm gửi lại sau 60 giây. Chấp nhận ở quy mô một chung cư; nếu sau này cần bền thì chuyển sang bảng outbox (xem rationale.md).
- Thời gian phản hồi vẫn chênh vài mili giây giữa email có tài khoản (thêm một transaction ghi) và không có. Đo được qua nhiều mẫu trên mạng ổn định; chấp nhận vì khoảng chênh nhỏ hơn nhiều so với độ dao động mạng, và đã loại phần lớn nhất (SMTP).
- Ai chiếm được hộp thư là chiếm được tài khoản, kể cả Admin. Không có bước xác minh thứ hai.
- Gmail SMTP có hạn mức gửi mỗi ngày và có thể chặn nếu gửi bất thường; email từ một địa chỉ Gmail cá nhân dễ vào thư rác. Đủ cho demo, không đủ cho vận hành thật (cần tên miền riêng và nhà cung cấp email giao dịch).
- Trần chung 100 email mỗi giờ là một nút cổ chai cố ý: kẻ xấu biết nhiều email có thật có thể dùng hết trần, và trong giờ đó người dùng thật không nhận được email. Chọn vậy vì nếu không có trần, kẻ xấu đốt hết hạn mức Gmail và mọi người mất email cả ngày. Giới hạn theo IP sẽ giảm việc này.
- Ai biết email của một người có thể khiến họ nhận tới 5 email đặt lại mỗi giờ, và làm link hợp lệ của họ bị thay thế (link mới hủy link cũ). Giới hạn theo IP (đã nằm trong Deferred của scope) sẽ giảm việc này.
- Thêm một container (Mailpit) khi chạy dev và một dependency test (GreenMail).
- `password-reset-ttl` ở backend và `THOI_HAN_LINK_PHUT` ở frontend là hai nơi; đổi một nơi mà quên nơi kia thì câu trên trang quên mật khẩu sai (email thì luôn đúng vì lấy từ `het_han_luc`).

**Neutral**:
- Bảng `yeu_cau_dat_lai_mat_khau` từ spec 0002 lần đầu được dùng; thêm cột `huy_luc` và đổi index trong `V14`.
- Bốn service của spec 0008 và 0009 thêm một lời gọi `huyConMo` trong transaction có sẵn của chúng.
- Theo follow up của spec 0009: đặt lại qua email cũng xóa khóa tạm, khớp với Admin đặt lại (spec 0009 AC-10).
- Ứng dụng bắt đầu dùng `@Async`, với một executor riêng có giới hạn cho email; các phân hệ sau gửi email nên dùng lại `emailExecutor`.

## Follow-up

- [ ] `/sync` nên ghi vào `AGENTS.md`: lệnh `docker compose up -d` giờ có Mailpit (giao diện ở `http://localhost:8025`), biến `MAIL_*` và `APP_PUBLIC_URL`, và dòng `Declined:` cho các Agent Skill (`claude-dev-suite@spring-mail`, `testland/qa@mailpit-testing`) và MCP server Mailpit mà engineer đã từ chối.
- [ ] Giới hạn theo IP (mục Deferred của scope) nên tính cả `POST /auth/password-reset/request`, không chỉ `/auth/login`.
- [ ] Nếu bản demo chạy sau một proxy có tên miền, đặt `APP_PUBLIC_URL` theo địa chỉ đó; nếu chạy qua `http` trong mạng LAN thì link vẫn chạy, nhưng nên nhắc trong `verify.md`.
