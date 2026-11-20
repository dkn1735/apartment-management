# 0005. Làm mới phiên và đăng xuất: xoay vòng refresh token theo họ phiên, làm mới âm thầm khi gặp 401

**Date**: 2026-11-20
**Status**: In Progress

## Summary

Spec này cho người dùng ở lại trong hệ thống mà không phải nhập lại mật khẩu mỗi 15 phút, và cho họ đăng xuất thật sự. Mỗi lần làm mới, backend đổi refresh token cũ lấy một token mới (xoay vòng) trong cùng một họ phiên (`ma_ho_phien`, mã chung của mọi token sinh ra từ một lần đăng nhập). Phiên kéo dài thêm 7 ngày mỗi lần dùng nhưng không quá 30 ngày kể từ lần đăng nhập đầu. Nếu một token đã bị đổi mà lại được dùng lần nữa (dấu hiệu bị đánh cắp), cả họ phiên bị thu hồi. Token mới không ngẫu nhiên mà được tính từ token cũ bằng một khóa bí mật (HMAC), nên nếu trình duyệt không nhận được response làm mới (ví dụ trang tải lại đúng lúc đó), lần làm mới kế tiếp bằng token cũ trong 30 giây nhận lại đúng token server đã cấp, thay vì bị đá ra hay bị coi là đánh cắp. Phía trình duyệt, hàm `api` tự làm mới một lần khi gặp lỗi 401 rồi gửi lại request, người dùng không thấy gì. Đăng xuất nằm trong menu tên người dùng ở đầu trang, thu hồi phiên hiện tại và đưa về trang đăng nhập.

## Requirements

**User stories**:
- Là người dùng đang thao tác, tôi muốn phiên tự gia hạn, để không bị đá về trang đăng nhập giữa chừng mỗi 15 phút.
- Là người dùng, tôi muốn bấm đăng xuất và biết chắc phiên trên máy này đã chấm dứt, kể cả khi dùng máy chung.
- Là ban quản lý, tôi muốn một refresh token bị đánh cắp bị phát hiện và vô hiệu ngay khi kẻ trộm hoặc chủ thật dùng lại nó.
- Là người làm tính năng 10, 11, 13, tôi muốn một cách thu hồi phiên theo họ phiên đã có sẵn, để chỉ cần gọi lại.

**Acceptance criteria**:
- **AC-1**: `POST /api/v1/auth/refresh-token` có header `X-Requested-With: XMLHttpRequest` và cookie `refresh_token` còn hiệu lực trả 200, `data` là `{ lamMoi: true, maNguoiDung, hoTen, vaiTro, accessTokenHetHanLuc }`, `message` "Làm mới phiên thành công", đặt lại ba cookie `access_token`, `refresh_token`, `phien`. Trong một transaction: dòng cũ có `thu_hoi_luc` = giờ của `Clock` và `ly_do_thu_hoi` = `Đã xoay vòng`; thêm đúng một dòng mới cùng `ma_nguoi_dung`, cùng `ma_ho_phien`, cùng `bat_dau_luc`, `tao_luc` = giờ hiện tại, `token_hash` = SHA-256 của cookie mới, cookie mới = `keTiep(token cũ)` (xem *Token kế tiếp*), `dia_chi_ip` và `user_agent` của request này. JWT mới có `sid` = `ma_ho_phien` cũ và `vaiTro` đọc lại từ DB. Cookie chỉ được đặt sau khi transaction commit. Body không chứa token.
- **AC-2**: `het_han_luc` của dòng mới = min(`tao_luc` + 7 ngày, `bat_dau_luc` + 30 ngày). `Max-Age` của cookie `refresh_token` và `phien` = số giây từ giờ hiện tại tới `het_han_luc` đó, làm tròn lên và ít nhất là 1 (không còn cố định 604800). Đăng nhập lúc T rồi làm mới lúc T + 6 ngày thì hết hạn lúc T + 13 ngày; làm mới lúc T + 29 ngày thì hết hạn lúc T + 30 ngày và `Max-Age` là 86400; sau T + 30 ngày mọi lần làm mới đều thất bại theo AC-3.
- **AC-3**: Làm mới khi thiếu cookie `refresh_token`, khi cookie không đúng 43 ký tự base64url (`[A-Za-z0-9_-]{43}`, kiểm trước mọi truy cập DB), khi hash không có trong DB, khi dòng đã hết hạn (`het_han_luc` <= giờ hiện tại), hoặc khi dòng đã bị thu hồi vì lý do khác `Đã xoay vòng` đều trả 401 `AUTH_REFRESH_INVALID` với cùng một `message` "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.", xóa cả ba cookie (`Max-Age=0`, đúng `Path`), không thêm dòng nào.
- **AC-4**: Dùng lại một token đã `Đã xoay vòng` quá 30 giây sau lúc nó bị xoay (`thu_hoi_luc` + 30 giây < giờ hiện tại), khi họ phiên còn ít nhất một dòng còn hiệu lực, thì mọi dòng còn hiệu lực trong cùng `ma_ho_phien` bị thu hồi với `ly_do_thu_hoi` = `Dùng lại token cũ`, trả 401 `AUTH_REFRESH_INVALID` (cùng câu như AC-3) và xóa ba cookie. Token mới nhất của họ đó sau đó cũng bị từ chối theo AC-3. Log mức WARN ghi `maNguoiDung` và `maHoPhien`, không ghi token hay hash. Nếu họ phiên không còn dòng hiệu lực nào (đã đăng xuất, đã hết hạn) thì xử lý đúng như AC-3: không thu hồi, không log WARN.
- **AC-5**: Dùng lại token T đã `Đã xoay vòng` trong vòng 30 giây kể từ lúc xoay (`thu_hoi_luc` + 30 giây >= giờ hiện tại), khi họ phiên còn một dòng còn hiệu lực L, thì server tính lần lượt `keTiep(T)`, `keTiep(keTiep(T))`, tối đa 3 bước. Bước nào có SHA-256 bằng `token_hash` của L thì trả 200 với `data` `{ lamMoi: false, maNguoiDung, hoTen, vaiTro, accessTokenHetHanLuc }` (`hoTen`, `vaiTro` đọc lại từ DB, `accessTokenHetHanLuc` của access token mới), `message` "Làm mới phiên thành công", đặt ba cookie với `refresh_token` = đúng token vừa tính, `Max-Age` của `refresh_token` và `phien` = thời gian còn lại tới `het_han_luc` của L (cách làm tròn như AC-2), và không ghi gì vào DB. Hai request làm mới gửi cùng lúc với cùng một token đều trả 200 với cùng giá trị cookie `refresh_token`, DB có đúng một dòng mới, họ phiên không bị thu hồi. Nếu không bước nào khớp, hoặc họ phiên không còn dòng nào hiệu lực (ví dụ vừa đăng xuất), thì xử lý như AC-3 (401 `AUTH_REFRESH_INVALID`, xóa ba cookie), không ghi gì vào DB, không log WARN. Khớp nhưng tài khoản không còn `dangHoatDong()` thì xử lý đúng như AC-6 (thu hồi cả họ `Tài khoản bị khóa`, 403 `AUTH_ACCOUNT_LOCKED`, xóa ba cookie).
- **AC-6**: Làm mới khi tài khoản không đạt đúng điều kiện mà đăng nhập dùng (`NguoiDungHeThong.dangHoatDong()`, tức `trang_thai_tai_khoan` là `Tạm khóa` hoặc `Ngừng hoạt động`) thì thu hồi mọi dòng còn hiệu lực của `ma_ho_phien` đó với `ly_do_thu_hoi` = `Tài khoản bị khóa`, trả 403 `AUTH_ACCOUNT_LOCKED` và xóa ba cookie.
- **AC-7**: `POST /api/v1/auth/refresh-token` và `POST /api/v1/auth/logout` là đường công khai: cookie `access_token` thiếu, hết hạn hay rác đều không làm chúng thất bại. Cả hai vẫn phải có header chống CSRF; thiếu header thì trả 403 `CSRF_REJECTED`.
- **AC-8**: `POST /api/v1/auth/logout` luôn trả 200 với `message` "Đăng xuất thành công" và xóa ba cookie. Nếu cookie `refresh_token` khớp một dòng (ở bất kỳ trạng thái nào), mọi dòng còn hiệu lực của `ma_ho_phien` đó bị thu hồi với `ly_do_thu_hoi` = `Đăng xuất` và `thu_hoi_luc` từ `Clock`; các họ phiên khác của cùng người dùng (máy khác) không bị đụng. Thiếu cookie, hash lạ, hoặc phiên đã thu hồi thì vẫn 200 và xóa cookie, không đổi DB. Sau đăng xuất, làm mới bằng token cũ bị từ chối theo AC-3. Đăng xuất chạy song song với một lần làm mới của cùng họ phiên (bằng token khác của họ đó): sau khi cả hai xong, họ không còn dòng nào hiệu lực.
- **AC-9**: Migration `V10__phien_bat_dau_luc.sql` thêm cột `phien_dang_nhap.bat_dau_luc TIMESTAMPTZ NOT NULL`, điền giá trị cho dòng cũ bằng `tao_luc`, có `CHECK (bat_dau_luc <= tao_luc)`, và thêm index duy nhất một phần `UNIQUE (ma_ho_phien) WHERE thu_hoi_luc IS NULL` (mỗi họ phiên có tối đa một dòng chưa thu hồi). Đăng nhập (spec 0004, AC-6) ghi `bat_dau_luc` = `tao_luc`; mọi điều khác của AC-6 giữ nguyên.
- **AC-10**: Phía client, lời gọi API nào qua `client` trả 401 `UNAUTHENTICATED` (trừ lời gọi tới `/api/v1/auth/login`, `/api/v1/auth/refresh-token`, `/api/v1/auth/logout`) thì gọi làm mới một lần; làm mới trả 200 (`lamMoi` true hoặc false, cả hai đều kèm cookie) thì gửi lại ngay đúng request ban đầu một lần và trả kết quả của lần gửi lại cho nơi gọi, người dùng không thấy lỗi hay chuyển trang. Nhiều request cùng gặp 401 trong một tab dùng chung đúng một lời gọi làm mới. Request gửi lại mà vẫn 401 thì không làm mới lần hai. Lời gọi làm mới được gửi với `keepalive: true`, để trang tải lại hay điều hướng không hủy nó. Làm mới thất bại vì lỗi khác 401 và 403 khóa tài khoản (5xx, mất mạng, `CSRF_REJECTED`) thì nơi gọi nhận `ApiError` tương ứng (mất mạng hoặc body không phải vỏ bọc thì `BACKEND_UNAVAILABLE`), không chuyển trang, không thử lại.
- **AC-11**: Làm mới thất bại với `AUTH_REFRESH_INVALID`, hoặc request gửi lại vẫn trả `UNAUTHENTICATED`, thì trình duyệt chuyển tới `/dang-nhap?next=<trang hiện tại>&ly-do=het-phien` như spec 0004 AC-15; làm mới trả `AUTH_ACCOUNT_LOCKED` thì chuyển tới `/dang-nhap?ly-do=khoa`. Chỉ chuyển trang một lần dù nhiều request cùng lỗi.
- **AC-12**: Kiểm tay với `pnpm dev`: đăng nhập, xóa riêng cookie `access_token` trong DevTools, tải lại `/ho-so`: trang hồ sơ hiện đủ mà không đi qua `/dang-nhap`, tab Network có đúng một request `refresh-token`.
- **AC-13**: Ở đầu trang của khung app, họ tên người dùng là một nút mở menu (shadcn `DropdownMenu`) gồm hai mục "Hồ sơ" (tới `/ho-so`) và "Đăng xuất". Khi query `["ho-so"]` đang tải hoặc lỗi, nút hiện nhãn "Tài khoản" và menu vẫn mở được, để đăng xuất luôn dùng được. Mở được bằng Enter, Space hoặc mũi tên xuống; di chuyển bằng mũi tên; Escape đóng menu và trả focus về nút; focus nhìn thấy được; nút có tên truy cập được (accessible name, tên mà trình đọc màn hình đọc lên) gồm họ tên; test axe không báo lỗi.
- **AC-14**: Chọn "Đăng xuất" thì gọi `POST /api/v1/auth/logout`; trong lúc chờ mục bị vô hiệu và không có chuyển trang `het-phien` nào chen vào; thành công thì tải lại hẳn trang tới `/dang-nhap?ly-do=dang-xuat`, trang đăng nhập hiện "Bạn đã đăng xuất." trong vùng `role="status"` phía trên form. Một lần làm mới đang chạy lúc bấm đăng xuất không được đặt lại cookie sau khi đăng xuất xong: đăng xuất chờ nó kết thúc trước khi gọi `logout`, và từ lúc bấm, middleware không bắt đầu lần làm mới mới nào. Sau đó mở `/ho-so` (gõ địa chỉ hoặc điều hướng mới) thì bị `proxy.ts` chuyển về `/dang-nhap`. Lời gọi đăng xuất lỗi bất kỳ thì hiện toast "Không đăng xuất được, vui lòng thử lại." và ở lại trang.
- **AC-15**: Một job chạy mỗi ngày lúc 03:00 theo `app.timezone` (mặc định `Asia/Ho_Chi_Minh`) xóa các dòng `phien_dang_nhap` có `het_han_luc` < giờ hiện tại − 30 ngày hoặc `thu_hoi_luc` < giờ hiện tại − 30 ngày, giữ nguyên mọi dòng khác, và ghi log INFO số dòng đã xóa. Giờ hiện tại lấy từ `Clock`.
- **AC-16**: Log của làm mới, đăng xuất và job dọn dẹp không bao giờ chứa refresh token, access token, `token_hash`, giá trị cookie hay khóa dẫn xuất của `keTiep`.
- **AC-17**: Mất response làm mới không đăng xuất người dùng: server đã xoay T1 thành T2 và commit, nhưng trình duyệt không nhận `Set-Cookie` và vẫn giữ T1. Lần làm mới kế tiếp bằng T1 trong 30 giây kể từ lúc xoay nhận 200 `lamMoi: false` với cookie `refresh_token` = T2 (theo AC-5), request ban đầu được gửi lại thành công, DB không có dòng `Dùng lại token cũ`, log không có WARN. Kiểm tay với `pnpm dev`, Network throttling "Slow 4G": đăng nhập, xóa cookie `access_token`, tải lại `/ho-so`, rồi tải lại lần nữa ngay lúc request `refresh-token` còn đang chạy: `/ho-so` hiện đủ, không đi qua `/dang-nhap`.

## Decision

**Chosen option**: Option 1: xoay vòng refresh token mỗi lần làm mới trong cùng họ phiên, phát hiện dùng lại thì thu hồi cả họ (có 30 giây khoan dung cho các tab chạy đua và response bị mất), phiên trượt 7 ngày với trần 30 ngày; token kế tiếp tính tất định bằng HMAC từ token cũ, nên trong khoảng khoan dung server trả lại đúng token đang sống (làm mới idempotent, tức gọi lại bao nhiêu lần cũng ra cùng kết quả); client làm mới khi gặp 401 trong một middleware của `openapi-fetch`, lời gọi làm mới dùng `keepalive`.

Backend thêm `POST /api/v1/auth/refresh-token` và `POST /api/v1/auth/logout` (công khai, nhận diện phiên bằng cookie `refresh_token`), một cột `bat_dau_luc` và một job dọn dẹp hằng ngày; frontend thêm middleware làm mới, xử lý mã `AUTH_REFRESH_INVALID`, menu người dùng có mục đăng xuất.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`) · `next-dev-loop` (`vercel/next.js`, `.agents/skills/next-dev-loop/`) · `ui-styling` (`.claude/skills/ui-styling/`)

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

**Data model sketch**:

Không có bảng mới. Một cột mới trên `phien_dang_nhap` (bảng đã có từ spec 0002, migration `V1`):

| Cột | Kiểu | Null | Ghi chú |
|---|---|---|---|
| `ma_phien` | BIGINT identity | không | PK |
| `ma_nguoi_dung` | VARCHAR(30) | không | FK tới `nguoi_dung_he_thong` (N:1), có index |
| `ma_ho_phien` | UUID | không | mã họ phiên, giống nhau qua mọi lần xoay vòng, có index |
| `token_hash` | VARCHAR(64) | không | UNIQUE, SHA-256 hex chữ thường |
| **`bat_dau_luc`** | TIMESTAMPTZ | không | **mới (V10)**: lúc đăng nhập đầu tiên của họ phiên, chép sang mỗi dòng xoay vòng; điền cho dòng cũ bằng `tao_luc`; `CHECK (bat_dau_luc <= tao_luc)` |
| `tao_luc` | TIMESTAMPTZ | không | |
| `het_han_luc` | TIMESTAMPTZ | không | = min(`tao_luc` + 7 ngày, `bat_dau_luc` + 30 ngày); CHECK có sẵn `het_han_luc > tao_luc` |
| `thu_hoi_luc`, `ly_do_thu_hoi` | | có | lát này dùng `Đã xoay vòng`, `Dùng lại token cũ`, `Đăng xuất`, `Tài khoản bị khóa` (đều có sẵn trong CHECK) |
| `dia_chi_ip`, `user_agent` | | có | của request tạo ra dòng đó |

Migration V10: `ALTER TABLE ... ADD COLUMN bat_dau_luc TIMESTAMPTZ`, `UPDATE ... SET bat_dau_luc = tao_luc`, `ALTER COLUMN bat_dau_luc SET NOT NULL`, thêm `CONSTRAINT chk_phien_dang_nhap_bat_dau CHECK (bat_dau_luc <= tao_luc)`. Thêm `CREATE UNIQUE INDEX uq_phien_dang_nhap_ho_con_hieu_luc ON phien_dang_nhap (ma_ho_phien) WHERE thu_hoi_luc IS NULL`: chốt chặn ở DB cho bất biến "một dòng chưa thu hồi mỗi họ" (dòng hết hạn mà chưa thu hồi không bao giờ được xoay tiếp nên không đụng index này). Tra cứu khác dùng `token_hash` (UNIQUE) và `ma_ho_phien` (đã có index); job dọn dẹp quét cả bảng mỗi ngày một lần, chấp nhận ở quy mô này.

Entity `PhienDangNhap` thêm field `batDauLuc` và hai phương thức nghiệp vụ `thuHoi(Instant luc, String lyDo)` (ném `IllegalStateException` nếu dòng đã thu hồi), `conHieuLuc(Instant now)`. Repository `PhienDangNhapRepository` thêm:
- `findByTokenHash(String hash)`: đọc không khóa, chỉ để lấy `ma_ho_phien`.
- `khoaHo(UUID maHoPhien)`: native query `SELECT pg_advisory_xact_lock(hashtextextended(CAST(:ma AS text), 0))` (khóa tư vấn của PostgreSQL theo họ phiên, tự nhả khi transaction kết thúc). Mọi thao tác ghi lên một họ (làm mới, đăng xuất, thu hồi) lấy khóa này trước, nên chúng chạy tuần tự theo họ chứ không chỉ theo dòng. Đặt `SET LOCAL lock_timeout = '3s'` trước khi lấy khóa (cùng native query hoặc một câu riêng); hết giờ thì lỗi đi lên thành 500 qua `GlobalExceptionHandler`.
- `thuHoiHo(UUID maHoPhien, Instant luc, String lyDo)`: `@Modifying(clearAutomatically = true)` `UPDATE ... SET thu_hoi_luc = :luc, ly_do_thu_hoi = :lyDo WHERE ma_ho_phien = :ma AND thu_hoi_luc IS NULL`, trả số dòng.
- `hoConHieuLuc(UUID maHoPhien, Instant now)`: `EXISTS` dòng có `thu_hoi_luc IS NULL AND het_han_luc > :now`.
- `xoaCu(Instant moc)`: `@Modifying` `DELETE ... WHERE het_han_luc < :moc OR thu_hoi_luc < :moc`, trả số dòng.

Hằng lý do thu hồi đặt trong một lớp `LyDoThuHoi` của package `auth` (chuỗi đúng như CHECK).

**State transitions** (một dòng `phien_dang_nhap`):

```
còn hiệu lực ──làm mới thành công──▶ Đã xoay vòng
còn hiệu lực ──đăng xuất (cùng họ)──▶ Đăng xuất
còn hiệu lực ──dùng lại token cũ của họ──▶ Dùng lại token cũ
còn hiệu lực ──làm mới khi tài khoản khóa──▶ Tài khoản bị khóa
còn hiệu lực ──quá het_han_luc──▶ hết hạn (không ghi gì, chỉ so giờ)
mọi trạng thái kết thúc ──quá 30 ngày──▶ bị job xóa
```

Không có chuyển ngược: dòng đã thu hồi không bao giờ còn hiệu lực lại. Một họ phiên có tối đa một dòng còn hiệu lực tại một thời điểm.

**Luồng làm mới** (`LamMoiPhienService.lamMoi(String refreshToken, ThongTinThietBi thietBi)`, `@Transactional`):

1. Cookie thiếu, rỗng, hoặc không khớp `[A-Za-z0-9_-]{43}`: ném `AUTH_REFRESH_INVALID` (trước khi hash hay đọc DB; đăng xuất dùng cùng hàm kiểm và coi sai định dạng như không có cookie) (kèm header xóa cookie qua `ApiException.headers` của spec 0004).
2. Tính SHA-256, `findByTokenHash` (không khóa). Không thấy: `AUTH_REFRESH_INVALID`. Thấy: `khoaHo(maHoPhien)`, rồi đọc lại dòng đó (`entityManager.refresh` hoặc truy vấn lại) để có trạng thái mới nhất dưới khóa.
3. `now` = `Instant.now(clock).truncatedTo(ChronoUnit.MICROS)` (PostgreSQL lưu tới micro giây; không cắt thì CHECK `het_han_luc > tao_luc` và `bat_dau_luc <= tao_luc` có thể sai sau khi lưu). Mọi so sánh giờ trong luồng dùng `now` này. Dòng có `ly_do_thu_hoi` = `Đã xoay vòng`:
   - `timConHieuLuc` rỗng (họ đã chết, tức không có dòng `thu_hoi_luc IS NULL AND het_han_luc > now`): `AUTH_REFRESH_INVALID`, không ghi DB, không log WARN, dù trong hay quá grace. Cả nhánh grace và nhánh quá grace dùng chung kết quả của một lần gọi `timConHieuLuc` này.
   - `thu_hoi_luc` + `refresh-reuse-grace` >= `now` và họ còn hiệu lực: L là dòng `timConHieuLuc` vừa trả. Tính `s = keTiep(token gửi lên)`, so `sha256Hex(s)` với `L.tokenHash`; chưa khớp thì `s = keTiep(s)` và so tiếp, tối đa 3 lần (hằng `SO_BUOC_KE_TIEP_TOI_DA = 3`). Không khớp: `AUTH_REFRESH_INVALID`, không ghi DB, log INFO `Làm mới thất bại: lyDo=KHONG_KHOP_CHUOI, maHoPhien={}`. Khớp: tải người dùng; không `dangHoatDong()` thì làm đúng như bước 5 (`thuHoiHo(..., "Tài khoản bị khóa")`, 403 `AUTH_ACCOUNT_LOCKED`, `noRollbackFor`); còn lại đọc vai trò, ký access token mới (`cap(maNguoiDung, maHoPhien, vaiTro, now)`) và trả `KetQuaLamMoi` với `lamMoi = false`, refresh token = `s`, `refreshMaxAge = conLai(now, L.hetHanLuc)`. Không ghi DB (không cập nhật `dia_chi_ip`, `user_agent` của L). Log INFO `Làm mới trả lại token đã cấp: maNguoiDung={}, khacThietBi={}`, trong đó `khacThietBi` = `dia_chi_ip` hoặc `user_agent` của request khác của L (chỉ ghi cờ true hoặc false, không ghi IP hay user agent). Cờ này chỉ để lại dấu vết, không chặn gì. Vì sao 3 bước: luồng thường cần 1 bước, hai tab chạy đua cần tới 2; quá 3 lần xoay trong 30 giây là bất thường (có thể có người đang xoay liên tục bằng token bị lộ), nên trả 401 mà không thu hồi. Hệ quả: tab cầm token cũ bị đưa về `het-phien`, còn bên đang giữ token sống thì không bị chặn ở lần này; lần dùng lại ngoài 30 giây sau đó vẫn bị AC-4 bắt.
   - Quá grace và họ còn hiệu lực: `thuHoiHo(..., "Dùng lại token cũ")`, log WARN, rồi ném `AUTH_REFRESH_INVALID`. Việc thu hồi phải commit dù có lỗi: dùng `@Transactional(noRollbackFor = ApiException.class)` cho phương thức này (hoặc tách bước thu hồi sang một bean riêng chạy `REQUIRES_NEW`; chọn `noRollbackFor` vì đơn giản hơn).
4. Dòng bị thu hồi vì lý do khác, hoặc `het_han_luc` <= `now`: `AUTH_REFRESH_INVALID`, không ghi DB.
5. Tải `NguoiDungHeThong` qua service công khai của package `auth`. `dangHoatDong()` sai (cùng điều kiện với đăng nhập): `thuHoiHo(..., "Tài khoản bị khóa")`, ném 403 `AUTH_ACCOUNT_LOCKED` kèm header xóa cookie (cùng quy tắc `noRollbackFor`).
6. `hetHanMoi` = min(`now` + `refresh-token-ttl`, `batDauLuc` + `session-max-lifetime`). Nếu `hetHanMoi` <= `now`: `AUTH_REFRESH_INVALID` (không xảy ra khi bước 4 đúng, giữ làm chốt chặn).
7. `dongCu.thuHoi(now, "Đã xoay vòng")`, `flush()` (để index duy nhất một phần không thấy hai dòng chưa thu hồi), rồi lưu dòng mới (token mới = `RefreshTokenGenerator.keTiep(token gửi lên)`, không phải `moi()`).
8. Đọc vai trò (`VaiTroQuery`), ký JWT qua `AccessTokenService.cap(maNguoiDung, maHoPhien, vaiTro, now)`; `accessTokenHetHanLuc` trong response là `hetHanLuc()` của access token đó (= `now` + `access-token-ttl`, đúng `exp` của JWT). Trả record `KetQuaLamMoi` (dữ liệu response, JWT, refresh token thô, `refreshMaxAge`); controller đặt cookie sau khi service trả về (đã commit).

Khóa họ ở bước 2 làm mọi request ghi lên cùng một họ chạy tuần tự: hai lần làm mới cùng token thì lần sau thấy dòng đã `Đã xoay vòng` trong khoảng grace, rơi vào nhánh `lamMoi = false` và nhận đúng token mà lần trước đã cấp (AC-5), nên thứ tự hai response tới trình duyệt không quan trọng; response làm mới bị mất (AC-17) cũng đi đúng nhánh này; đăng xuất bằng T1 chạy cùng lúc làm mới bằng T2 thì bên chạy sau luôn thấy kết quả của bên trước, nên không có dòng T3 nào sống sót sau đăng xuất (AC-8). Thu hồi do dùng lại (AC-4) và do khóa tài khoản (AC-6) cũng chạy dưới khóa này.

**Token kế tiếp** (`RefreshTokenGenerator`, nhận `AppProperties` qua constructor):
- Khóa dẫn xuất `K` = HMAC-SHA256 với khóa là `app.security.jwt-secret` (byte UTF-8) trên chuỗi nhãn cố định `chungcu.refresh-token-ke-tiep.v1` (byte US-ASCII). Tính một lần khi tạo bean, giữ trong field `private final`. Đây là cách tách khóa: cùng một bí mật gốc nhưng JWT và token kế tiếp dùng hai khóa khác nhau, không thêm biến môi trường. `K` không bao giờ được log, không có `toString` lộ nó.
- `keTiep(String token)` = base64url không padding của HMAC-SHA256(`K`, byte US-ASCII của `token`): 32 byte, 43 ký tự, đúng định dạng của `moi()`, nên cookie, cột `token_hash` và test định dạng không đổi. Dùng `javax.crypto.Mac` mới cho mỗi lần gọi (`Mac` không an toàn khi dùng chung giữa các luồng).
- `moi()` (32 byte `SecureRandom`) chỉ còn dùng cho đăng nhập, tức token đầu tiên của mỗi họ phiên. Không ai đoán được token kế tiếp nếu không có `K`, nên độ khó đoán giữ nguyên 256 bit.
- Repository thêm `timConHieuLuc(UUID maHoPhien, Instant now)`: `Optional<PhienDangNhap>` của dòng `thu_hoi_luc IS NULL AND het_han_luc > :now` (index duy nhất một phần bảo đảm tối đa một dòng). Nó thay hẳn `hoConHieuLuc` (bỏ phương thức đó), để "họ còn sống" chỉ có một định nghĩa.
- Khóa gốc là chuỗi `app.security.jwt-secret` lấy nguyên byte UTF-8, không giải mã Base64, đúng cách `AccessTokenService` đang dùng nó.
- Dòng tạo trước khi có `keTiep` (token ngẫu nhiên) không khớp chuỗi; chỉ ảnh hưởng 30 giây quanh lần deploy, chấp nhận.

**Luồng đăng xuất** (`DangXuatService.dangXuat(String refreshToken)`, `@Transactional`): cookie có giá trị thì `findByTokenHash`; thấy dòng thì `khoaHo(maHoPhien)` rồi `thuHoiHo(maHoPhien, now, "Đăng xuất")` (câu này chỉ chạm dòng còn hiệu lực, nên phiên đã thu hồi không đổi). Không ném lỗi nghiệp vụ nào; controller luôn trả `ApiResponse.ok` với `AuthCookies.xoa()`.

**Cookie**: `AuthCookies.dat(accessToken, refreshToken, Duration refreshMaxAge)` thay cho chữ ký hai tham số. Đăng nhập truyền `refresh-token-ttl`; làm mới truyền `het_han_luc − now` làm tròn lên theo giây và ít nhất 1 giây (`Max-Age=0` sẽ xóa cookie của một phiên còn hạn). `Max-Age` của `phien` luôn bằng của `refresh_token`.

**Đường công khai**: `PublicPaths` thêm `POST /api/v1/auth/refresh-token` và `POST /api/v1/auth/logout`. Vì danh sách dùng chung cho `CookieBearerTokenResolver` và `authorizeHttpRequests`, cookie `access_token` không bao giờ được đọc trên hai đường này (AC-7). `CsrfHeaderFilter` không đổi.

**Job dọn dẹp**: lớp `PhienDangNhapCleanupJob` trong package `auth`, `@Scheduled(cron = "${app.security.session-cleanup-cron}", zone = "${app.timezone}")`, gọi `PhienDangNhapService.donDep()` (`@Transactional`, `xoaCu(now − session-retention)`), log INFO `Dọn phiên: daXoa={}`; job bắt lỗi ở mức ngoài cùng và log ERROR (chỉ tên lỗi và thông điệp, không dữ liệu token) để lỗi không bị nuốt im lặng, lần sau chạy lại bình thường. Bật `@EnableScheduling` trong một lớp `SchedulingConfig` của `common/config`. Một instance backend nên không cần khóa phân tán.

**Frontend**:
- `src/lib/api/refresh.ts`: `lamMoiPhien(fetchFn): Promise<KetQuaLamMoi>` gọi `fetchFn("/api/v1/auth/refresh-token", { method: "POST", headers: { "X-Requested-With": "XMLHttpRequest" }, credentials: "same-origin", keepalive: true })` (không đi qua `client`, để không tự gọi lại chính nó). `keepalive` là lớp phụ: trình duyệt hỗ trợ thì request sống qua lần tải lại và `Set-Cookie` vẫn tới; trình duyệt bỏ qua cờ này thì AC-17 vẫn đứng nhờ nhánh grace ở server. Một biến promise cấp module bảo đảm chỉ có một lời gọi đang chạy (single flight, mọi nơi chờ chung một lời gọi); xóa biến trong `finally`, kể cả khi `fetch` ném lỗi. Kết quả: `{ ok: true }` khi 200 (không còn đọc `lamMoi`, vì cả hai nhánh đều đặt cookie); `{ ok: false, response }` khi có response lỗi; `fetch` ném lỗi (mất mạng) thì ném tiếp. Export thêm `dangLamMoi(): Promise<unknown> | null` cho đăng xuất chờ.
- Cờ `dangDangXuat` cấp module trong `refresh.ts` (`batDauDangXuat()`, `ketThucDangXuat()`): khi bật, middleware không bắt đầu làm mới mà trả nguyên response 401.
- `refreshMiddleware` (thêm vào `client` sau `csrfHeaderMiddleware`), đã kiểm với `openapi-fetch` đang cài: `onRequest` và `onResponse` nhận cùng một `id` và `onResponse` được trả `Response` mới; body đã bị đọc khi tới `onResponse` nên phải chép trước.
  - `onRequest`: với method khác `GET` và `HEAD`, lưu `request.clone()` vào một `Map` theo `id` (GET gửi lại bằng `new Request(request)` không cần chép body). `onResponse` và `onError` luôn xóa mục của `id` đó.
  - `onResponse` chỉ xử lý khi `response.status === 401`, `schemaPath` không thuộc ba đường auth ở AC-10, `dangDangXuat` tắt, và body (đọc từ `response.clone()`) có `errorCode` `UNAUTHENTICATED`. Khi đó chờ `lamMoiPhien(options.fetch)`:
    - `ok`: trả `options.fetch(banSao)` ngay, kết quả là gì cũng trả nguyên (vẫn 401 thì `handleAuthError` chuyển `het-phien`). Bỏ hằng `CHO_SAU_KHOAN_DUNG_MS`, hàm `cho` và nhánh làm mới lần hai.
    - `ok: false`: trả `response` của lời gọi làm mới (để `AUTH_REFRESH_INVALID`, `AUTH_ACCOUNT_LOCKED`, 5xx hay `CSRF_REJECTED` đi tiếp vào `api` và thành `ApiError` tương ứng).
    - `lamMoiPhien` ném lỗi: ném tiếp, để `api` biến thành `BACKEND_UNAVAILABLE`.
  - Lần gửi lại đi thẳng qua `options.fetch`, không qua middleware, nên không thể tự kích hoạt làm mới thêm (không cần dấu "đã gửi lại"). Nhờ vậy mọi chỗ gọi `api(client.GET(...))` giữ nguyên.
- `handleAuthError` trong `query-client.ts`: thêm `AUTH_REFRESH_INVALID` vào cùng nhánh với `UNAUTHENTICATED` (chuyển tới `het-phien`), và bỏ qua mọi lỗi khi `dangDangXuat` bật. Không đụng cờ `dangChuyenTrang` khi đăng xuất, để một lần chuyển `het-phien` đã bắt đầu không bị kích hoạt lại.
- `error-messages.ts`: `AUTH_REFRESH_INVALID` "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.".
- Thêm component shadcn `dropdown-menu` (`pnpm dlx shadcn@latest add dropdown-menu`). `MenuNguoiDung` (`src/app/(app)/menu-nguoi-dung.tsx`) thay `TenNguoiDung` trong `userSlot`: nút kích hoạt hiện họ tên lấy từ query `["ho-so"]` như hiện nay; khi đang tải hoặc lỗi thì hiện "Tài khoản" (menu vẫn mở được). Hai mục "Hồ sơ" (`Link` tới `/ho-so`) và "Đăng xuất".
- Đăng xuất: mutation với `meta: { boQuaChuyenTrang: true }`. Thứ tự: `batDauDangXuat()`; `queryClient.cancelQueries()`; chờ `dangLamMoi()` nếu có (bỏ qua lỗi của nó); gọi `logout`. Thành công thì `queryClient.clear()` rồi `authNavigation.assign("/dang-nhap?ly-do=dang-xuat")` (cờ giữ nguyên vì trang tải lại hẳn). Lỗi bất kỳ thì `ketThucDangXuat()` và `toast.error("Không đăng xuất được, vui lòng thử lại.")` (sonner đã có).
- Trang đăng nhập: `ly-do=dang-xuat` hiện "Bạn đã đăng xuất." cùng vùng `role="status"` với `het-phien`, `khoa`.

**API surface**:

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/auth/refresh-token` | POST | header `X-Requested-With` (req); cookie `refresh_token` (opt); không có body | 200 `data { lamMoi: boolean, maNguoiDung, hoTen, vaiTro: VaiTro[], accessTokenHetHanLuc: Instant }`, `message` "Làm mới phiên thành công"; `Set-Cookie` ×3 ở mọi response 200 (`lamMoi` false: `refresh_token` là token đang sống được trả lại) | Public (cần cookie refresh) | 401 `AUTH_REFRESH_INVALID` (kèm xóa cookie), 403 `AUTH_ACCOUNT_LOCKED` (kèm xóa cookie), 403 `CSRF_REJECTED` |
| `/api/v1/auth/logout` | POST | header `X-Requested-With` (req); cookie `refresh_token` (opt); không có body | 200 `data null`, `message` "Đăng xuất thành công"; `Set-Cookie` ×3 xóa | Public | 403 `CSRF_REJECTED` |

DTO mới: record `LamMoiPhienResponse(boolean lamMoi, String maNguoiDung, String hoTen, List<VaiTro> vaiTro, Instant accessTokenHetHanLuc)`; `accessTokenHetHanLuc` không bao giờ null (đánh dấu bắt buộc trong schema OpenAPI). `lamMoi` true nghĩa là vừa xoay ra token mới, false nghĩa là trả lại token đã cấp trong khoảng khoan dung. Mã `AUTH_REFRESH_INVALID` và câu thông báo khai báo trong `AuthErrorCodes`. Sinh lại `docs/api/openapi.json` và chạy `pnpm gen:api`.

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| Làm mới | token cần kiểm | cookie `refresh_token` (`@CookieValue(required = false)`) |
| Làm mới | dòng phiên hiện tại | `phien_dang_nhap` theo `token_hash` = SHA-256 của cookie |
| Làm mới | giờ hiện tại | bean `Clock` |
| Làm mới | khoảng khoan dung | `app.security.refresh-reuse-grace` (PT30S) |
| Làm mới | khóa dẫn xuất `K` của `keTiep` | HMAC-SHA256(`app.security.jwt-secret`, `chungcu.refresh-token-ke-tiep.v1`) |
| Làm mới (nhánh grace) | refresh token trả lại | `keTiep` của token gửi lên, lặp tối đa 3 bước tới khi SHA-256 khớp `token_hash` của dòng còn hiệu lực |
| Làm mới (nhánh grace) | dòng còn hiệu lực L | `timConHieuLuc(ma_ho_phien, now)` |
| Làm mới (nhánh grace) | `Max-Age` cookie `refresh_token`, `phien` | `het_han_luc` của L − `now`, làm tròn như AC-2 |
| Làm mới | `bat_dau_luc` dòng mới | chép từ dòng cũ |
| Làm mới | `het_han_luc` dòng mới | min(`now` + `app.security.refresh-token-ttl` (P7D), `bat_dau_luc` + `app.security.session-max-lifetime` (P30D)) |
| Làm mới | `now` | `Instant.now(clock)` cắt tới micro giây |
| Làm mới | `Max-Age` cookie `refresh_token`, `phien` | `het_han_luc` − `now`, làm tròn lên theo giây, ít nhất 1 |
| Làm mới | `accessTokenHetHanLuc` | `now` + `app.security.access-token-ttl` (= `exp` của JWT mới), ở cả hai nhánh |
| Làm mới, đăng xuất | khóa theo họ | `pg_advisory_xact_lock` trên `ma_ho_phien`, chờ tối đa 3 giây |
| Làm mới | `Max-Age` cookie `access_token`, `exp` JWT | `app.security.access-token-ttl` (spec 0004) |
| Làm mới | `sid` JWT | `ma_ho_phien` của dòng cũ |
| Làm mới | `vaiTro` | `VaiTroQuery` đọc lại từ DB |
| Làm mới | `hoTen`, trạng thái tài khoản | `nguoi_dung_he_thong` theo `ma_nguoi_dung` của dòng |
| Làm mới | refresh token mới, `token_hash` | `RefreshTokenGenerator.keTiep(token cũ)`; `token_hash` = SHA-256 của nó |
| Đăng nhập | refresh token đầu tiên của họ | `RefreshTokenGenerator.moi()` (32 byte ngẫu nhiên, spec 0004) |
| Làm mới | `dia_chi_ip`, `user_agent` dòng mới | `ThongTinThietBi` từ request (spec 0004) |
| Làm mới | `lamMoi` | true khi xoay vòng, false khi rơi vào nhánh grace và trả lại token đã cấp |
| Đăng xuất | họ phiên cần thu hồi | `ma_ho_phien` của dòng khớp cookie `refresh_token` |
| Đăng xuất, thu hồi | `thu_hoi_luc` | bean `Clock` |
| Đăng xuất, thu hồi | `ly_do_thu_hoi` | hằng trong `LyDoThuHoi` theo nhánh |
| Đăng nhập | `bat_dau_luc` | = `tao_luc` |
| Job dọn dẹp | mốc xóa | `now` − `app.security.session-retention` (P30D) |
| Job dọn dẹp | lịch chạy | `app.security.session-cleanup-cron` (`0 0 3 * * *`), vùng giờ `app.timezone` |
| Client | có làm mới và gửi lại hay không | status 401 và `errorCode` `UNAUTHENTICATED` của response, `schemaPath` của request, cờ `dangDangXuat` |
| Client | lời gọi làm mới sống qua lần tải lại | `keepalive: true` trong `refresh.ts` |
| Client | body để gửi lại | bản `request.clone()` lưu theo `id` của middleware |
| Client | thông báo sau đăng xuất | query `ly-do=dang-xuat` |
| Menu người dùng | họ tên | query `["ho-so"]` (`/api/v1/auth/profile`, spec 0004) |

**Key invariants**:
- Một họ phiên có tối đa một dòng chưa thu hồi. Bảo đảm bằng khóa theo họ ở bước 2, việc thu hồi dòng cũ trong cùng transaction với dòng mới, và index duy nhất một phần ở DB làm chốt chặn cuối.
- Mọi thao tác ghi lên một họ phiên (xoay vòng, đăng xuất, thu hồi vì dùng lại hay khóa) lấy khóa họ trước; không thao tác nào ghi lên họ mà không có khóa.
- Mọi mốc giờ ghi vào `phien_dang_nhap` đã cắt tới micro giây trước khi so sánh và lưu.
- `het_han_luc` không bao giờ vượt `bat_dau_luc` + 30 ngày.
- `session-retention` >= `session-max-lifetime`: dòng `Đã xoay vòng` sống ít nhất bằng tuổi tối đa của họ phiên, nên dùng lại token cũ luôn bị phát hiện chừng nào họ còn sống. `AppProperties` validate điều này lúc khởi động.
- Dòng đã thu hồi không bao giờ được đặt lại `thu_hoi_luc = NULL`.
- Cookie chỉ được đặt sau khi transaction commit; lỗi thu hồi (AC-4, AC-6) vẫn commit việc thu hồi.
- Refresh token không bao giờ nằm trong body, log hay DB dạng thô; khóa `K` không bao giờ nằm trong log.
- Token của một dòng xoay vòng luôn bằng `keTiep` của token dòng trước trong cùng họ; chỉ token đầu tiên của họ là ngẫu nhiên. Vì vậy làm mới bằng một token trong khoảng khoan dung luôn ra cùng một cookie, gọi bao nhiêu lần cũng vậy.
- Mọi response 200 của làm mới đều đặt đủ ba cookie; không có 200 nào bắt client chờ cookie từ một response khác.
- Nhánh grace không ghi DB, trừ một trường hợp: tài khoản không còn hoạt động thì thu hồi cả họ như AC-6.
- Lời gọi làm mới của client không bao giờ đi qua `refreshMiddleware` (không đệ quy), và mỗi request chỉ được gửi lại tối đa một lần.

**Security model**:
- Hai endpoint mới là công khai về mặt Spring Security, nhưng chỉ làm được gì khi có refresh token hợp lệ (32 byte ngẫu nhiên, đoán không nổi). Không có vai trò nào liên quan: mỗi người chỉ làm mới hay đăng xuất được phiên mà cookie của chính họ trỏ tới.
- Chống CSRF giữ hai lớp của spec 0004 (`SameSite=Strict` và header `X-Requested-With`). Cookie `refresh_token` có `Path=/api/v1/auth` nên chỉ được gửi tới các đường auth.
- Phát hiện token bị đánh cắp: dùng lại token đã xoay ngoài khoảng 30 giây thu hồi cả họ, nên cả kẻ trộm lẫn chủ thật đều phải đăng nhập lại; mọi nguyên nhân thất bại trả cùng mã và câu, không lộ đó là dùng lại hay hết hạn.
- Khoảng khoan dung 30 giây trả lại token đang sống cho bất kỳ ai cầm token vừa xoay. Kẻ trộm dùng T1 trong 30 giây sau lần xoay của chủ thật nhận được đúng T2 của chủ thật (không phải một token riêng). Từ đó hai bên cùng giữ T2: bên nào làm mới trước thì xoay ra T3, bên kia dùng T2 quá 30 giây sau đó thì AC-4 thu hồi cả họ. Kẻ trộm làm mới liên tục (dưới 30 giây một lần) có thể đi theo phiên một lúc, nhưng ngay khi token của chủ thật cũ quá 30 giây, lần làm mới kế tiếp của chủ thật kích hoạt AC-4 và thu hồi cả họ, nên không có cách đi theo lâu dài mà không bị phát hiện. Ngược lại, ai cầm token bị lộ cũng luôn buộc được chủ thật đăng nhập lại (từ chối dịch vụ ở mức một phiên); điều này bản đầu đã có, nay vẫn giữ. Chấp nhận để đổi lấy việc mất response không còn đăng xuất người dùng hay gây báo động giả.
- `keTiep` dùng khóa dẫn xuất từ `JWT_SECRET`: lộ `JWT_SECRET` thì kẻ tấn công đã ký được access token tùy ý, nên token kế tiếp không làm thiệt hại tệ thêm. Không có `K` thì token kế tiếp không đoán được.
- Dữ liệu nhạy cảm: token, hash, IP, user agent. Không ghi token hay hash vào log.
- Chưa giới hạn tốc độ (rate limit) ở lát này; xem *Consequences*.

**Configuration required**:

Không có biến môi trường mới. Thuộc tính mới trong `application.yml`, khai báo trong `AppProperties.Security` có validate:
- `app.security.session-max-lifetime: P30D`: tuổi tối đa của một họ phiên.
- `app.security.refresh-reuse-grace: PT30S`: khoảng khoan dung cho các request làm mới chạy đua và cho response làm mới bị mất. `AppProperties` validate lớn hơn 0 và không quá `PT60S`, để một lỗi cấu hình không âm thầm nới rộng khoảng mà token bị lộ vẫn dùng được.
- Khóa của `keTiep` suy ra từ `JWT_SECRET` có sẵn, không thêm biến môi trường. Đổi `JWT_SECRET` thì token xoay trước lúc đổi không khớp chuỗi trong nhánh grace (chỉ trong 30 giây quanh lúc đổi), và mọi access token cũ đều mất hiệu lực như hiện nay.
- `app.security.session-retention: P30D`: giữ dòng đã hết hạn hoặc thu hồi bao lâu trước khi xóa; phải >= `session-max-lifetime`.
- `app.security.session-cleanup-cron: "0 0 3 * * *"`: lịch job dọn dẹp.

**Critical test scenarios**:

Backend (Testcontainers, `MutableClock`):
- Đăng nhập, làm mới: 200, ba cookie mới, body không có token; dòng cũ `Đã xoay vòng`, đúng một dòng mới cùng `ma_ho_phien` và `bat_dau_luc`; JWT mới có `sid` cũ; đổi vai trò bằng SQL trước khi làm mới thì JWT mới có vai trò mới; verifies **AC-1**.
- Tua giờ: làm mới lúc T + 6 ngày, T + 29 ngày, T + 30 ngày 1 giây: đúng `het_han_luc` và `Max-Age` như AC-2; lần cuối 401. `MutableClock` đặt ở một mốc có phần nano giây khác 0 và một lần làm mới còn 0,5 giây tới `het_han_luc`: không lỗi CHECK, `Max-Age` = 1; verifies **AC-2**.
- Dùng lại T1 quá 30 giây sau khi đã đăng xuất: 401, không có log WARN, DB không đổi; verifies **AC-4**.
- Thiếu cookie, cookie rác, dòng hết hạn, dòng `Đăng xuất`: đều 401 `AUTH_REFRESH_INVALID` cùng `message`, ba header xóa cookie, số dòng không đổi; verifies **AC-3**.
- Làm mới với T1 được T2; tua 31 giây; làm mới lại với T1: 401, mọi dòng của họ có `Dùng lại token cũ` hoặc `Đã xoay vòng`, không còn dòng hiệu lực; làm mới với T2: 401; log WARN có `maHoPhien`; verifies **AC-4**.
- Làm mới với T1 được T2; sau 5 giây và sau đúng 30 giây làm mới lại với T1: 200 `lamMoi` false, cookie `refresh_token` = T2, `Max-Age` = thời gian còn lại của dòng T2, `accessTokenHetHanLuc` khác null, DB không đổi. Hai luồng song song cùng T1: đều 200 với cùng cookie `refresh_token`, đúng một dòng mới. T1 → T2 → T3 trong 30 giây, làm mới với T1: 200 với cookie T3 (đi chuỗi 2 bước). Chèn tay dòng còn hiệu lực có `token_hash` không suy ra được từ T1: 401, DB không đổi, không WARN, có log INFO `KHONG_KHOP_CHUOI`. Đăng xuất rồi trong 30 giây làm mới với T1: 401; tài khoản bị khóa rồi trong 30 giây làm mới với T1: 403 `AUTH_ACCOUNT_LOCKED`, cả họ `Tài khoản bị khóa` đã commit; log INFO có `khacThietBi=true` khi gọi lại với IP khác, `false` khi cùng IP và user agent; cookie 42 ký tự hoặc có ký tự ngoài base64url: 401, không truy vấn DB; verifies **AC-3**, **AC-5**.
- Mất response: đăng nhập được T1, làm mới được T2 nhưng test bỏ qua `Set-Cookie` của response đó, rồi làm mới lại bằng T1 sau 3 giây: 200 với cookie T2; làm mới tiếp bằng T2: 200 `lamMoi` true, xoay ra T3 = `keTiep(T2)`; không có dòng `Dùng lại token cũ`, không có log WARN; verifies **AC-17**.
- Unit test `RefreshTokenGenerator`: `keTiep` tất định (cùng đầu vào ra cùng kết quả), 43 ký tự base64url, khác với đầu vào, đổi `jwt-secret` thì kết quả đổi; `moi()` vẫn ngẫu nhiên. `AppProperties` với `refresh-reuse-grace` bằng 0 hoặc `PT61S`: context không khởi động; verifies **AC-5**, **AC-16**.
- Đổi tài khoản sang `Tạm khóa` bằng SQL, làm mới: 403 `AUTH_ACCOUNT_LOCKED`, xóa cookie, dòng `Tài khoản bị khóa` đã commit; verifies **AC-6**.
- Làm mới và đăng xuất kèm cookie `access_token` rác hoặc hết hạn: thành công; thiếu `X-Requested-With`: 403 `CSRF_REJECTED`; verifies **AC-7**.
- Sau một 401 do dùng lại (AC-4) và một 403 do khóa (AC-6), đọc DB bằng một kết nối khác: các dòng đã thu hồi thật sự đã commit (kiểm `noRollbackFor`).
- Race theo họ: làm mới với T1 được T2; chạy song song đăng xuất bằng T1 (dòng đã xoay, cùng họ) và làm mới bằng T2, lặp nhiều lần với `CountDownLatch`: sau cả hai, họ không còn dòng nào chưa thu hồi; chèn tay dòng chưa thu hồi thứ hai cùng họ bị index duy nhất chặn; verifies **AC-8**, **AC-9**.
- Đăng nhập hai lần (hai họ), đăng xuất họ thứ nhất: họ đó `Đăng xuất`, họ thứ hai còn nguyên; đăng xuất lần nữa và đăng xuất không cookie: 200, xóa cookie, DB không đổi; làm mới sau đăng xuất: 401; verifies **AC-8**.
- Migration V10 chạy trên DB có sẵn dòng phiên: `bat_dau_luc` = `tao_luc`; chèn dòng có `bat_dau_luc > tao_luc` bị CHECK chặn; đăng nhập ghi `bat_dau_luc` = `tao_luc`; verifies **AC-9**.
- Gọi trực tiếp `donDep()` với dữ liệu: dòng hết hạn 31 ngày, thu hồi 31 ngày, thu hồi 29 ngày, còn hiệu lực: chỉ hai dòng đầu bị xóa, log có số 2; job với repository giả ném lỗi thì log ERROR và không ném ra ngoài; `AppProperties` với retention < max lifetime làm context không khởi động; verifies **AC-15**.
- Bắt log trong mọi test trên: không có token, hash hay giá trị cookie; verifies **AC-16**.

Frontend (Vitest, Testing Library, `fetch` giả):
- Hai query cùng nhận 401 `UNAUTHENTICATED`: đúng một lời gọi `refresh-token`, mỗi query được gửi lại một lần và nhận dữ liệu; một `POST` có body được gửi lại với đúng body; request gửi lại vẫn 401 thì không làm mới lần hai; làm mới trả 200 `lamMoi` false thì gửi lại ngay, không chờ, và chỉ một lời gọi `refresh-token`; lời gọi làm mới có `keepalive` true; làm mới trả 500 hoặc `fetch` ném lỗi thì nơi gọi nhận `ApiError` (`BACKEND_UNAVAILABLE` khi mất mạng), không chuyển trang, và lần 401 kế tiếp vẫn gọi làm mới được (promise đã được xóa); lời gọi tới `/login`, `/logout` trả 401 không kích hoạt làm mới; verifies **AC-10**.
- Làm mới trả `AUTH_REFRESH_INVALID`: chuyển tới `het-phien` đúng một lần; trả `AUTH_ACCOUNT_LOCKED`: chuyển tới `ly-do=khoa`; verifies **AC-11**.
- Menu người dùng: mở bằng Enter và mũi tên xuống, Escape trả focus về nút, axe không lỗi; khi query hồ sơ lỗi, nút hiện "Tài khoản" và mục "Đăng xuất" vẫn chọn được; verifies **AC-13**.
- Đăng xuất thành công: gọi `logout`, `assign("/dang-nhap?ly-do=dang-xuat")`, không có `assign` tới `het-phien` dù một query đang chạy trả 401; một lần làm mới đang chạy lúc bấm thì `logout` chỉ được gọi sau khi nó xong, và không có lần làm mới mới nào bắt đầu sau khi bấm; đăng xuất lỗi mạng hay lỗi 500: toast, không chuyển trang, cờ `dangDangXuat` tắt lại; trang đăng nhập với `ly-do=dang-xuat` hiện câu đúng; verifies **AC-14**.
- Kiểm tay với `pnpm dev` (ghi vào `verify.md`): xóa cookie `access_token`, tải lại `/ho-so`; như vậy nhưng với "Slow 4G" và tải lại lần nữa ngay lúc `refresh-token` còn đang chạy (AC-17), lặp ít nhất 5 lần; mở hai tab, chờ access token hết hạn (đặt tạm `access-token-ttl: PT1M` ở dev), thao tác ở cả hai tab: không tab nào bị đá ra; đăng xuất rồi bấm Back (trang cũ có thể hiện từ bộ nhớ đệm của trình duyệt, xem *Consequences*; mọi thao tác hay tải lại sau đó phải đưa về `/dang-nhap`); verifies **AC-12**, **AC-14**, **AC-17**.

## Build plan

Tracer Bullet: lát đầu dựng đường làm mới mỏng nhất chạy thật từ một query bị 401 trên trình duyệt, qua middleware, endpoint, DB rồi gửi lại request; các bước sau làm dày nhánh lỗi, thêm đăng xuất cùng đường đó, rồi tới dọn dẹp.

1. **Đường mỏng: làm mới âm thầm khi access token hết hạn.** Migration `V10__phien_bat_dau_luc.sql`; `PhienDangNhap.batDauLuc`, đăng nhập ghi `bat_dau_luc`; thuộc tính mới trong `AppProperties.Security` (có validate retention >= max lifetime); `PublicPaths` thêm hai đường; `AuthCookies.dat` nhận `refreshMaxAge`; `LyDoThuHoi`; index duy nhất một phần trong V10; repository `findByTokenHash`, `khoaHo` (khóa tư vấn theo họ, `lock_timeout` 3 giây); cắt `now` tới micro giây; `LamMoiPhienService` nhánh thành công (bước 1, 2, 6, 7, 8 của luồng làm mới); `LamMoiPhienResponse`; `AuthController` thêm `/refresh-token`; mã `AUTH_REFRESH_INVALID`; sinh lại `openapi.json`, `pnpm gen:api`; `refresh.ts` và `refreshMiddleware`; test tích hợp happy path và test middleware hai query cùng 401. Satisfies **AC-1**, **AC-7**, **AC-9**, **AC-10**, **AC-12**.
2. **Làm dày làm mới: hết hạn, trần 30 ngày, dùng lại, khoan dung, khóa tài khoản.** Bước 3, 4, 5 của luồng làm mới; `thuHoiHo`, `hoConHieuLuc`; `noRollbackFor`; log WARN; `handleAuthError` nhận `AUTH_REFRESH_INVALID`; nhánh `lamMoi` false chờ 400 ms ở middleware (bỏ ở bước 6); lỗi làm mới khác 401 và 403; từ điển lỗi; test tua giờ, dùng lại, song song, khóa, commit của thu hồi; test frontend chuyển trang khi làm mới thất bại; test log. Satisfies **AC-2**, **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-11**, **AC-16**.
3. **Đăng xuất xuyên suốt.** `DangXuatService`, `/logout` trong `AuthController`; shadcn `dropdown-menu`; `MenuNguoiDung` thay `TenNguoiDung` (nhãn dự phòng "Tài khoản"); cờ `dangDangXuat`; mutation đăng xuất với `batDauDangXuat`, `cancelQueries`, chờ `dangLamMoi`, `clear`, `assign`; toast lỗi; test race đăng xuất và làm mới theo họ; thông báo `ly-do=dang-xuat` ở trang đăng nhập; test backend AC-8 và test frontend menu, a11y, đăng xuất. Satisfies **AC-8**, **AC-13**, **AC-14**.
4. **Dọn dẹp phiên cũ.** `SchedulingConfig` với `@EnableScheduling`; `xoaCu`; `PhienDangNhapService.donDep()`; `PhienDangNhapCleanupJob`; test dọn dẹp và validate cấu hình. Satisfies **AC-15**.
5. **Kiểm cả đường.** Viết `verify.md` (các bước kiểm tay ở *Critical test scenarios*); chạy `mvnw.cmd verify`, `pnpm typecheck`, `pnpm lint`, `pnpm build`; kiểm tay với `pnpm dev` bằng `next-dev-loop`. Satisfies **AC-12**, **AC-14**.
6. **Làm mới chịu được response bị mất** (bổ sung ngày 2026-11-20, sau khi lần kiểm ở bước 5 thấy người dùng bị đưa về `het-phien` khi tải lại lúc đang làm mới). Một lát xuyên suốt, không đổi schema: `RefreshTokenGenerator` thêm khóa dẫn xuất và `keTiep`; xoay vòng dùng `keTiep` thay `moi()`; repository `timConHieuLuc` thay `hoConHieuLuc`; kiểm định dạng cookie ở bước 1 (làm mới và đăng xuất); nhánh grace đi chuỗi tối đa 3 bước và trả lại token kèm cookie và access token mới, thu hồi khi tài khoản bị khóa, log cờ `khacThietBi`; `refresh-reuse-grace` thành `PT30S` có validate; `LamMoiPhienResponse.accessTokenHetHanLuc` bắt buộc, sinh lại `openapi.json`, `pnpm gen:api`; `refresh.ts` thêm `keepalive`, bỏ nhánh chờ 400 ms và làm mới lần hai; sửa test AC-4, AC-5, AC-10 theo số mới, thêm test mất response; cập nhật `verify.md`, kiểm tay tải lại lúc đang làm mới và ép nhánh grace bằng Playwright. Satisfies **AC-1**, **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-10**, **AC-16**, **AC-17**.

## Consequences

**Positive**:
- Người dùng đang thao tác không còn bị đưa về trang đăng nhập mỗi 15 phút; trường hợp "cookie `phien` còn mà access token đã hết" ở spec 0004 gần như biến mất.
- Một refresh token bị đánh cắp có tuổi thọ hữu ích ngắn: chỉ tới lần làm mới kế tiếp của bên còn lại, rồi cả họ bị thu hồi.
- Khóa tài khoản hay đổi vai trò có hiệu lực ở lần làm mới kế tiếp (tối đa 15 phút), không phải chờ hết 7 ngày.
- Tính năng 10, 11, 13 chỉ cần gọi một câu thu hồi theo người dùng (cùng kiểu `thuHoiHo`) với lý do đã có trong CHECK.
- Mọi chỗ gọi `api(...)` hiện có giữ nguyên, nhờ làm mới nằm trong middleware.

**Negative / tradeoffs**:
- Mỗi lần làm mới thêm một dòng vào `phien_dang_nhap` (khoảng 4 dòng mỗi giờ thao tác cho một người), cần job dọn dẹp và giữ dòng 30 ngày.
- Khoảng 30 giây khoan dung là một kẽ hở: ai cầm token vừa xoay trong 30 giây đó nhận lại được token đang sống (xem *Security model*). Rộng hơn bản đầu của spec này (10 giây, không cấp cookie), đổi lấy việc mất response không còn đăng xuất người dùng.
- Mất response mà người dùng không làm mới lại trong 30 giây (đóng tab ngay rồi mở lại sau một phút) vẫn bị coi là dùng lại token cũ: cả họ bị thu hồi, log WARN báo sai. Hiếm, và hậu quả chỉ là phải đăng nhập lại.
- Token kế tiếp phụ thuộc `JWT_SECRET`; đổi bí mật này giữa chừng làm nhánh grace không khớp chuỗi trong 30 giây quanh lúc đổi.
- `keepalive` có hành vi khác nhau giữa các trình duyệt (chưa kiểm chắc `Set-Cookie` có được lưu sau khi trang rời đi ở mọi trình duyệt), nên chỉ là lớp phụ; không test nào dựa vào nó.
- Làm mới và đăng xuất lấy một khóa tư vấn của PostgreSQL theo họ phiên: gắn chặt vào PostgreSQL (dự án đã chốt PostgreSQL ở spec 0001), và một request có thể chờ tới 3 giây nếu họ đang bị giữ.
- Sau đăng xuất, bấm Back có thể hiện lại trang cũ từ bộ nhớ đệm của trình duyệt (bfcache) mà không gọi server. Chấp nhận: dữ liệu đó đã nằm trên máy, cookie đã bị xóa, và mọi thao tác hay tải lại sau đó đều về `/dang-nhap`. Không thêm `Cache-Control: no-store` ở lát này.
- Request ghi (`POST`, `PUT`) bị gửi lại một lần sau 401. An toàn vì 401 xảy ra ở bộ lọc xác thực, trước khi controller chạy, nên lần đầu chưa ghi gì.
- Hai endpoint công khai chưa có giới hạn tốc độ; tính năng 12 thêm. Rủi ro thấp vì token 256 bit không đoán được, nhưng spam làm mới với token hợp lệ vẫn tạo nhiều dòng.
- Đăng xuất chỉ chấm dứt máy hiện tại; access token đã cấp vẫn sống tới hết 15 phút nếu bị sao chép trước đó (cookie đã bị xóa trên trình duyệt này).
- Job dùng `@Scheduled` trong tiến trình; nếu sau này chạy nhiều instance backend thì job chạy trùng (vô hại vì chỉ xóa) và cần khóa phân tán nếu muốn chạy đúng một lần.

**Neutral**:
- `AuthCookies.dat` đổi chữ ký; `DangNhapService` truyền thêm `refresh-token-ttl`.
- `RefreshTokenGenerator` nhận `AppProperties` qua constructor; các test đang tạo nó bằng `new` phải truyền cấu hình.
- Thêm `@EnableScheduling` cho cả ứng dụng; các tính năng sau có thể dùng lại.
- `TenNguoiDung` được thay bằng `MenuNguoiDung`; trang `/dev/ui/shell` có thể cần cập nhật ví dụ `userSlot`.

## Follow-up

- [ ] Tính năng 8: ghi nhật ký kiểm toán cho đăng xuất và cho sự kiện phát hiện dùng lại token (hiện chỉ có log WARN).
- [ ] Tính năng 10, 11, 13: thu hồi mọi họ phiên của một người dùng với `Đổi mật khẩu`, `Tài khoản bị khóa`, `Đặt lại mật khẩu`; thêm vào repository một câu `thuHoiTheoNguoiDung`.
- [ ] Tính năng 12: giới hạn tốc độ cho `/login`, `/refresh-token`, `/logout`.
- [ ] Cập nhật `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx` (API-AUTH-02): refresh token đọc từ cookie, không có body, có trường `lamMoi`, mã `AUTH_REFRESH_INVALID`; thêm dòng cho đăng xuất nếu file chưa có.
- [ ] Spec 0002 Follow-up "dọn dòng `phien_dang_nhap`" và spec 0004 Follow-up "tính năng 6" được giải quyết ở spec này; đánh dấu khi tính năng xong.
- [ ] `/sync`: ghi vào `AGENTS.md` quy ước làm mới trong middleware (không tự bắt 401 ở màn hình) khi tính năng này xong.
