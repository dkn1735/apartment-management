# 0009. Khóa tạm khi đăng nhập sai nhiều lần: giữ lượt trước khi so mật khẩu, đếm cả định danh không tồn tại, trả 429 kèm `Retry-After`

**Date**: 2026-11-23
**Status**: Accepted

## Summary

Spec này chốt cách chặn dò mật khẩu: sai 5 lần liên tiếp (mỗi lần cách lần trước không quá 15 phút) thì đăng nhập bị chặn 15 phút, kể cả khi nhập đúng mật khẩu, tới hạn thì tự mở. Để kẻ dò không biết số điện thoại hay email nào có tài khoản, định danh không tồn tại cũng bị đếm và khóa y hệt (lưu ở một bảng nhỏ, khóa là `SHA-256` của định danh). Mỗi lần thử được "giữ lượt" bằng một câu SQL khóa dòng (chạy trọn, không chen ngang được) trước khi so mật khẩu, nên gửi song song nhiều request cũng không được quá 5 lượt đoán. Cần một migration `V13`, mã lỗi mới `AUTH_TOO_MANY_ATTEMPTS` (HTTP 429), một endpoint cho Admin và BQL mở khóa sớm, hai loại nhật ký mới, và câu báo có số phút còn lại trên trang đăng nhập.

## Requirements

**User stories**:
- Là cư dân, tôi muốn tài khoản của mình không bị ai đoán mật khẩu bằng cách thử liên tục.
- Là cư dân lỡ gõ sai nhiều lần, tôi muốn biết phải chờ bao lâu, hoặc gọi BQL để được mở ngay.
- Là Admin hoặc BQL, tôi muốn thấy tài khoản nào đang bị khóa đăng nhập và mở sớm được.
- Là nhóm phát triển, tôi muốn phản hồi khi bị khóa không tiết lộ một số điện thoại hay email có tài khoản hay không.

**Thuật ngữ dùng trong spec**:
- **Khóa tạm**: `khoa_tam_den` lớn hơn giờ hiện tại của `Clock`. Tự hết hạn, độc lập với `trang_thai_tai_khoan = 'Tạm khóa'` do người quản lý đặt (spec 0002).
- **Định danh**: chuỗi tài khoản đã chuẩn hóa bởi `TaiKhoanDangNhap.parse` (email chữ thường, hoặc số điện thoại dạng `0xxxxxxxxx`), trong giới hạn độ dài sẵn có của `DangNhapRequest` và `parse`.
- **Định danh ảo**: định danh đúng định dạng nhưng không có tài khoản nào mang nó.
- **Bộ đếm hiệu lực**: số lần sai đã lưu nếu `lan_sai_cuoi_luc` lớn hơn giờ `Clock` trừ `T`, ngược lại là 0. Tức là 15 phút không có lần sai mới thì bộ đếm coi như về 0.
- **Giữ lượt**: câu SQL khóa dòng (`FOR NO KEY UPDATE`) rồi tăng bộ đếm hiệu lực, chạy trước khi so mật khẩu, và tự đặt khóa tạm khi lượt đó là lượt thứ 5.
- `N` = 5 (`KhoaTamDangNhap.SO_LAN_SAI_TOI_DA`), `T` = 15 phút (`KhoaTamDangNhap.THOI_GIAN_KHOA`, dùng cho cả thời gian khóa lẫn cửa sổ đếm).

**Acceptance criteria**

- **AC-1**: Với tài khoản có thật và không đang khóa tạm, lần sai thứ 1 tới 4 liên tiếp trả 401 `AUTH_INVALID_CREDENTIALS` như spec 0004 AC-3 (cùng mã, cùng câu), và `so_lan_dang_nhap_sai` lần lượt là 1 tới 4, `lan_sai_cuoi_luc` là giờ `Clock` của lần đó. Lần sai thứ 5 trả 429 `AUTH_TOO_MANY_ATTEMPTS` (AC-7), đặt `khoa_tam_den` = giờ `Clock` (cắt tới micro giây) cộng 15 phút và `so_lan_dang_nhap_sai` = 0. Không lần nào thay đổi `phien_ban`, `cap_nhat_luc`, `trang_thai_tai_khoan`, hay tạo phiên.
- **AC-2**: Khi tài khoản đang khóa tạm, mọi lần đăng nhập (mật khẩu đúng hay sai, trạng thái tài khoản nào cũng vậy) trả 429 `AUTH_TOO_MANY_ATTEMPTS` với `Retry-After` là số giây còn lại; không chạy bcrypt, không ghi gì vào DB (kể cả nhật ký), không đặt cookie, không tạo phiên. Khóa không bị kéo dài thêm bởi các lần thử trong lúc khóa.
- **AC-3**: Từ thời điểm `khoa_tam_den` trở đi (so bằng `<=` với giờ `Clock`), đăng nhập đúng thì thành công; đăng nhập sai thì trả 401 và bộ đếm bắt đầu lại từ 1.
- **AC-4**: Mật khẩu khớp thì đặt `so_lan_dang_nhap_sai = 0`, `khoa_tam_den = NULL`, `lan_sai_cuoi_luc = NULL`, ở cả nhánh thành công lẫn nhánh 403 `AUTH_ACCOUNT_LOCKED` (`Tạm khóa`, `Ngừng hoạt động`). Câu đặt lại chỉ ghi khi có gì để xóa (`WHERE so_lan_dang_nhap_sai <> 0 OR khoa_tam_den IS NOT NULL`). Nhánh thành công đặt lại trong cùng transaction với `taoPhien` (thay cho câu "không đụng `so_lan_dang_nhap_sai`, `khoa_tam_den`" ở spec 0004 AC-6). Nhánh 403 đặt lại bằng một câu riêng trước khi ném lỗi.
- **AC-5**: Định danh ảo được đếm và khóa đúng như AC-1, AC-2, AC-3, AC-18 trong bảng `dang_nhap_sai_dinh_danh`, khóa là `RefreshTokenGenerator.sha256Hex(<định danh đã chuẩn hóa>)`. Lần 1 tới 4 trả 401 sau một lần `MatKhauService.soKhopGia`; lần 5 trả 429; trong lúc khóa trả 429 không chạy bcrypt. Chuỗi không đúng định dạng email hay số điện thoại (`TaiKhoanDangNhap.parse` rỗng) vẫn trả 401 sau `soKhopGia` như hôm nay và không được đếm. Số điện thoại và email của cùng một tài khoản dùng chung một bộ đếm (các cột trên `nguoi_dung_he_thong`).
- **AC-6**: Gửi song song 20 request sai mật khẩu cho cùng một tài khoản (hoặc cùng một định danh ảo) đang ở bộ đếm 0: tối đa 4 response là 401, còn lại là 429; bcrypt (hoặc `soKhopGia`) chạy tối đa 5 lần; sau cùng `khoa_tam_den` khác `NULL`. Giới hạn này tính giữa hai lần đặt lại: một lần mật khẩu đúng chạy xen vào được phép đặt bộ đếm về 0 (AC-4), vì người gửi nó đã biết mật khẩu.
- **AC-7**: Response 429 theo vỏ bọc lỗi của spec 0001: `errorCode` `AUTH_TOO_MANY_ATTEMPTS`, `message` "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút hoặc liên hệ ban quản lý.", `errors` `[]`, có `requestId`, không có `data`. Header `Retry-After` là số nguyên giây, bằng số giây từ giờ `Clock` của lượt đó tới `khoa_tam_den`, làm tròn lên, nhỏ nhất là 1. Với tài khoản có thật và định danh ảo ở cùng thời điểm khóa, status, body (trừ `requestId`) và tập header giống hệt nhau.
- **AC-8**: `DELETE /api/v1/accounts/{maNguoiDung}/login-lock` (không có body) đặt `so_lan_dang_nhap_sai = 0`, `khoa_tam_den = NULL`, `lan_sai_cuoi_luc = NULL` bằng một câu `UPDATE ... RETURNING old.khoa_tam_den` (PostgreSQL 18), không đổi `phien_ban`, `cap_nhat_luc`, `trang_thai_tai_khoan`, không thu hồi phiên. Sau đó đọc lại bằng câu đọc chi tiết thường (không khóa dòng) trong cùng transaction và trả 200 `message` "Mở khóa đăng nhập thành công", `data` `TaiKhoanChiTietResponse` (AC-9). Nếu giá trị cũ của `khoa_tam_den` lớn hơn giờ `Clock` thì ghi nhật ký `AUTH_MO_KHOA_TAM_DANG_NHAP` (AC-11); không thì vẫn 200, không ghi nhật ký. Mã sai dạng, không tồn tại, hoặc ngoài phạm vi của người quản lý trả 404 `NOT_FOUND` "Không tìm thấy tài khoản" (như spec 0008 AC-6), kiểm trước câu `UPDATE`. Người quản lý mở khóa cho chính mình được (không phải `AUTH_SELF_PROTECTED`).
- **AC-9**: `TaiKhoanChiTietResponse` (spec 0008 AC-6) có thêm `khoaTamDen` (`Instant`, có thể `null`): bằng `khoa_tam_den` khi giá trị đó lớn hơn giờ `Clock`, ngược lại `null`. Cột chỉ được thêm vào câu đọc chi tiết, không vào câu danh sách. Không lộ `so_lan_dang_nhap_sai` hay `lan_sai_cuoi_luc`. `TaiKhoanTomTat` của danh sách không đổi.
- **AC-10**: Đặt lại mật khẩu (spec 0008 AC-12) cũng đặt `so_lan_dang_nhap_sai = 0`, `khoa_tam_den = NULL`, `lan_sai_cuoi_luc = NULL` trong cùng câu `UPDATE`, nên người dùng đăng nhập được ngay bằng mật khẩu tạm. Mở lại từ `Tạm khóa` sang `Hoạt động` (spec 0008 AC-10) giữ nguyên hành vi đã có (bộ đếm về 0 và khóa xóa, nên `lan_sai_cuoi_luc` còn lại không có tác dụng). Đổi mật khẩu của chính mình không đổi gì.
- **AC-11**: Nhật ký (spec 0007):
  - Lần sai thứ 5 (lượt vừa đặt khóa) của tài khoản có thật: một dòng `AUTH_DANG_NHAP_THAT_BAI` lý do `SAI_THONG_TIN` như mọi lần sai, cộng một dòng `AUTH_KHOA_TAM_DANG_NHAP` với người thực hiện và đối tượng là tài khoản đó, chi tiết `ChiTietKhoaTam { khoaTamDen }`. Định danh ảo không có dòng `AUTH_KHOA_TAM_DANG_NHAP`.
  - Lần thử bị chặn vì đang khóa: không ghi nhật ký kiểm toán (chỉ log ứng dụng, AC-17), để kẻ dò không làm phình bảng nhật ký.
  - Mở khóa sớm có tác dụng (AC-8): `AUTH_MO_KHOA_TAM_DANG_NHAP`, người thực hiện là người quản lý, đối tượng là tài khoản, chi tiết `ChiTietKhoaTam { khoaTamDen }` (giá trị cũ).
  - Chi tiết nhật ký không bao giờ chứa định danh, hash của định danh, hay mật khẩu.
  - Trang `/nhat-ky` hiện đúng nhãn tiếng Việt của hai loại mới và có chúng trong bộ lọc loại thao tác, theo cách trang đang làm với các loại có sẵn.
- **AC-12**: Job dọn chạy theo `app.security.session-cleanup-cron` (dùng chung lịch với `PhienDangNhapCleanupJob`) xóa các dòng `dang_nhap_sai_dinh_danh` đã hết tác dụng: `lan_sai_cuoi_luc <= giờ Clock trừ T` và không đang khóa (`khoa_tam_den IS NULL OR khoa_tam_den <= now`). Dòng như vậy có bộ đếm hiệu lực 0, nên xóa nó không đổi hành vi. Lỗi được log ERROR (tên lỗi và thông điệp) rồi bỏ qua, như job dọn phiên.
- **AC-13**: Trang `/dang-nhap`: khi nhận `ApiError` `AUTH_TOO_MANY_ATTEMPTS`, vùng lỗi có sẵn (`role="alert"`) hiện "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau {m} phút hoặc liên hệ ban quản lý.", với `m = ceil(retryAfter / 60)`, nhỏ nhất 1; thiếu `retryAfter` thì hiện câu mặc định ở AC-7. Sau lần đầu nhận 401 `AUTH_INVALID_CREDENTIALS` kể từ khi component form được dựng (state của component, tải lại trang thì mất), hiện thêm dòng chữ cố định dưới nút đăng nhập "Nhập sai 5 lần liên tiếp sẽ bị tạm khóa đăng nhập 15 phút." (đủ tương phản, gắn với form qua `aria-describedby`), và giữ nguyên kể cả sau khi nhận 429. Nút đăng nhập không bị vô hiệu hóa.
- **AC-14**: Trang `/tai-khoan/[maNguoiDung]`, thẻ Trạng thái và bảo mật: khi `khoaTamDen` khác `null`, hiện dòng "Đang khóa đăng nhập đến HH:mm dd/MM/yyyy" (giờ theo `app.timezone`, định dạng có sẵn của dự án) và nút "Mở khóa đăng nhập". Bấm nút gọi AC-8 (không có hộp thoại xác nhận), thành công thì hiện toast "Đã mở khóa đăng nhập", dòng và nút biến mất, focus chuyển tới tiêu đề của thẻ (`tabIndex={-1}`), không rơi về đầu trang. Khi `khoaTamDen` là `null` thì không hiện cả hai. Dùng được hoàn toàn bằng bàn phím.
- **AC-15**: `ApiError` có thêm trường `retryAfter?: number` (giây), do hàm `api` đọc từ header `Retry-After` khi giá trị là số nguyên dương; giá trị khác (thiếu, dạng ngày HTTP, không phải số) thì để `undefined`. Frontend gọi `/api` cùng tên miền qua rewrites của Next.js, nên không cần cấu hình CORS để đọc header. Hàm `api` vẫn là đường duy nhất gọi API.
- **AC-16**: Phân quyền endpoint AC-8: `@ChoPhep({QUAN_TRI_VIEN, BAN_QUAN_LY})`, phạm vi qua `TaiKhoanPhamViService` như các endpoint của spec 0008 (BQL chỉ với tài khoản cư dân thuần, ngoài phạm vi trả 404). Vai trò khác trả 403 `FORBIDDEN`, chưa đăng nhập trả 401 `UNAUTHENTICATED`, đang phải đổi mật khẩu trả 403 `AUTH_PASSWORD_CHANGE_REQUIRED`. `PhanQuyenCoverageTest` vẫn xanh.
- **AC-17**: Log ứng dụng: lần bị chặn ghi `Đăng nhập thất bại: lyDo=KHOA_TAM` ở mức INFO (kèm `maNguoiDung=` khi có tài khoản), lượt đặt khóa ghi `Khóa tạm đăng nhập: maNguoiDung=...` ở mức INFO. Không log nào chứa định danh, hash của định danh, hay mật khẩu (giữ spec 0004 AC-16).
- **AC-18**: Bộ đếm tự về 0 khi không có lần sai mới trong `T`: sai 4 lần, tăng `Clock` thêm 15 phút, sai lần nữa thì trả 401 và bộ đếm là 1 (không bị khóa). Sai 4 lần, tăng `Clock` thêm 14 phút 59 giây, sai lần nữa thì bị khóa.
- **AC-19**: Tạo tài khoản (spec 0008 AC-1) và sửa số điện thoại hoặc email (spec 0008 AC-7) xóa dòng `dang_nhap_sai_dinh_danh` ứng với số điện thoại và email mới của tài khoản (nếu có), trong cùng transaction, để định danh vừa có chủ không mang theo trạng thái khóa của định danh ảo và kẻ dò không thấy 429 đổi thành 401.

## Decision

**Chosen option**: Option 1: sửa tại chỗ trong `DangNhapService`, giữ lượt trên dòng bị khóa trước bcrypt, các cột trên `nguoi_dung_he_thong` cho tài khoản thật cộng một bảng cho định danh ảo.

Sai 5 lần liên tiếp trong cửa sổ trượt 15 phút thì khóa đăng nhập 15 phút cố định; định danh ảo bị đối xử y hệt để không lộ tài khoản có tồn tại; khi khóa trả 429 `AUTH_TOO_MANY_ATTEMPTS` kèm `Retry-After`; Admin và BQL mở sớm bằng endpoint riêng không đụng máy trạng thái của spec 0008.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`), theo quy ước service, JdbcClient và cấu hình; riêng lỗi theo vỏ bọc của spec 0001 · `ui-styling` (`.claude/skills/ui-styling/`), dòng trạng thái, nút và toast bằng shadcn/ui.

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

**Data model sketch** (một migration `V13__khoa_tam_dang_nhap.sql`):

| Bảng | Thay đổi | Cột và ràng buộc |
|---|---|---|
| `nguoi_dung_he_thong` | thêm một cột | Có sẵn từ V1: `so_lan_dang_nhap_sai` INTEGER NOT NULL DEFAULT 0, CHECK `>= 0`; `khoa_tam_den` TIMESTAMPTZ NULL. Thêm: `lan_sai_cuoi_luc` TIMESTAMPTZ NULL (lần giữ lượt gần nhất; nullable nên an toàn với dòng đang có). Một bộ đếm cho mỗi tài khoản, dùng chung cho số điện thoại và email. Entity `NguoiDungHeThong` không map ba cột này; mọi đọc ghi đi qua `JdbcClient`, nên lưu entity không ghi đè chúng. |
| `dang_nhap_sai_dinh_danh` | mới | `dinh_danh_hash` VARCHAR(64) PRIMARY KEY, CHECK `dinh_danh_hash ~ '^[0-9a-f]{64}$'`; `so_lan_sai` INTEGER NOT NULL DEFAULT 0, CHECK `so_lan_sai >= 0`; `khoa_tam_den` TIMESTAMPTZ NULL; `lan_sai_cuoi_luc` TIMESTAMPTZ NULL. Index `idx_dang_nhap_sai_dinh_danh_lan_sai_cuoi_luc` trên `lan_sai_cuoi_luc` cho job dọn. Không có khóa ngoại. Comment trên bảng ghi rõ: chỉ chứa định danh không có tài khoản, không có cột nào chứa định danh gốc. |

**Câu giữ lượt** (một câu, tự commit, ngoài transaction, trước bcrypt). Tham số bind kiểu `OffsetDateTime` UTC như `TaiKhoanGhi`, và ép kiểu rõ trong SQL: `:now`, `:den` (= `:now + T`), `:moc` (= `:now` trừ `T`, tính ở Java) đều là `CAST(... AS timestamptz)`; `:n` = `N`.

```sql
-- Tài khoản có thật (KhoaTamTaiKhoanRepository.giuLuot)
WITH cu AS (
    SELECT ma_nguoi_dung, khoa_tam_den,
           CASE WHEN lan_sai_cuoi_luc > CAST(:moc AS timestamptz) THEN so_lan_dang_nhap_sai ELSE 0 END AS dem
    FROM nguoi_dung_he_thong
    WHERE ma_nguoi_dung = :ma
    FOR NO KEY UPDATE
), giu AS (
    UPDATE nguoi_dung_he_thong n
    SET so_lan_dang_nhap_sai = CASE WHEN cu.dem + 1 >= :n THEN 0 ELSE cu.dem + 1 END,
        khoa_tam_den         = CASE WHEN cu.dem + 1 >= :n THEN CAST(:den AS timestamptz) END,
        lan_sai_cuoi_luc     = CAST(:now AS timestamptz)
    FROM cu
    WHERE n.ma_nguoi_dung = cu.ma_nguoi_dung
      AND (cu.khoa_tam_den IS NULL OR cu.khoa_tam_den <= CAST(:now AS timestamptz))
    RETURNING (n.khoa_tam_den IS NOT NULL) AS vua_khoa
)
SELECT cu.khoa_tam_den AS khoa_cu, giu.vua_khoa
FROM cu LEFT JOIN giu ON true;
```

Định danh ảo (`KhoaTamDinhDanhRepository.giuLuot`) chạy hai câu: trước tiên `INSERT INTO dang_nhap_sai_dinh_danh (dinh_danh_hash) VALUES (:hash) ON CONFLICT (dinh_danh_hash) DO NOTHING` để chắc chắn có dòng, rồi đúng câu trên với tên bảng và cột của bảng này (`dinh_danh_hash`, `so_lan_sai`). Cùng một mẫu SQL cho hai nhánh nên cùng ngữ nghĩa khi chạy song song: `FOR NO KEY UPDATE` xếp hàng các lượt trên cùng một dòng, và mỗi lượt đọc giá trị mới nhất sau khi được khóa.

Đọc kết quả (một dòng, hai cột, map vào record `KetQuaGiuLuot(Instant khoaCu, Boolean vuaKhoa)`):
- Không có dòng nào (tài khoản bị xóa giữa chừng): coi như lượt thường, đi tiếp như không tìm thấy tài khoản ở lần sau.
- `vuaKhoa` là `null` (bước `giu` không cập nhật): đang khóa, `Retry-After` tính từ `khoaCu` và `:now` của lượt này.
- `vuaKhoa` là `true`: lượt cuối, khóa đến `:den`.
- `vuaKhoa` là `false`: lượt thường.

**Luồng đăng nhập mới** (`DangNhapService.dangNhap`, thứ tự cố định; `now` lấy một lần ở đầu và dùng cho mọi bước):

1. `TaiKhoanDangNhap.parse`. Rỗng: `soKhopGia`, nhật ký `SAI_THONG_TIN` với `null`, 401. Không đếm.
2. Tìm tài khoản theo định danh.
3. `KhoaTamDangNhapService.giuLuot(...)` trả một trong ba kết quả (sealed interface `LuotThu`): `DangKhoa(khoaDen)`, `LuotCuoi(khoaDen)`, `LuotThuong`. Có tài khoản thì giữ lượt trên `nguoi_dung_he_thong`, không thì trên `dang_nhap_sai_dinh_danh`.
4. `DangKhoa`: log `lyDo=KHOA_TAM`, ném 429. Không bcrypt, không nhật ký.
5. Không có tài khoản: `soKhopGia`; nhật ký `SAI_THONG_TIN` với `null`; `LuotCuoi` thì 429, `LuotThuong` thì 401.
6. Có tài khoản, mật khẩu sai: nhật ký `SAI_THONG_TIN`; `LuotCuoi` thì log và nhật ký `AUTH_KHOA_TAM_DANG_NHAP`, rồi 429; `LuotThuong` thì 401.
7. Có tài khoản, mật khẩu đúng: nếu `!dangHoatDong()` thì `KhoaTamDangNhapService.datLai` (AC-4) rồi 403 như cũ. Ngược lại đi tiếp như spec 0004; `PhienDangNhapService` nhận thêm dependency `KhoaTamTaiKhoanRepository` và gọi `datLai` trong transaction của `taoPhien` (`JpaTransactionManager` cho `JdbcClient` dùng chung kết nối, có test kiểm rollback cùng nhau). Nếu lượt này là `LuotCuoi`, đặt lại cũng xóa luôn khóa vừa đặt.

**State transitions** (bộ đếm hiệu lực `c` và khóa tạm, cho một tài khoản hoặc một định danh ảo):

```
Mở (c = 0..N-2) ──giữ lượt, sai──▶ Mở (c + 1, lan_sai_cuoi_luc = now)
Mở (c = N-1)    ──giữ lượt thứ N──▶ Khóa (c = 0, khoa_tam_den = now + T)
Mở (c > 0)      ──T trôi qua không có lượt mới──▶ Mở (c = 0)   (không cần ghi, tính lúc đọc)
Khóa            ──lượt thứ N là mật khẩu đúng──▶ Mở (c = 0)     (chỉ tài khoản có thật)
Khóa            ──mọi lần thử──▶ Khóa (không đổi, không ghi)    (429)
Khóa            ──tới khoa_tam_den──▶ Mở (c = 0)                (không cần ghi, so bằng giờ Clock)
Mở              ──mật khẩu đúng──▶ Mở (c = 0, xóa cả ba cột)
Bất kỳ          ──Admin mở sớm, đặt lại mật khẩu, mở lại từ Tạm khóa──▶ Mở (c = 0)
```

**API surface**:

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/auth/login` (có sẵn) | POST | không đổi | không đổi khi thành công | `@CongKhai` | 401 `AUTH_INVALID_CREDENTIALS`, 403 `AUTH_ACCOUNT_LOCKED`, **429 `AUTH_TOO_MANY_ATTEMPTS` + `Retry-After`** |
| `/api/v1/accounts/{maNguoiDung}/login-lock` | DELETE | path | 200 `TaiKhoanChiTietResponse` | `@ChoPhep({QUAN_TRI_VIEN, BAN_QUAN_LY})`, phạm vi | 401, 403 `FORBIDDEN`, 403 `AUTH_PASSWORD_CHANGE_REQUIRED`, 404 `NOT_FOUND` |
| `/api/v1/accounts/{maNguoiDung}` (có sẵn) | GET | không đổi | thêm `khoaTamDen` | không đổi | không đổi |
| `/api/v1/accounts` (có sẵn) | POST | không đổi | không đổi; thêm xóa dòng định danh ảo (AC-19) | không đổi | không đổi |
| `/api/v1/accounts/{maNguoiDung}` (có sẵn) | PUT | không đổi | không đổi; thêm xóa dòng định danh ảo (AC-19) | không đổi | không đổi |
| `/api/v1/accounts/{maNguoiDung}/reset-password` (có sẵn) | POST | không đổi | không đổi; thêm đặt lại bộ đếm | không đổi | không đổi |

OpenAPI: khai báo response 429 cho `/auth/login` (có header `Retry-After`) và response của endpoint mới, rồi sinh lại `docs/api/openapi.json` và `frontend/src/lib/api/schema.d.ts`.

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| Đăng nhập | `N` = 5, `T` = 15 phút | hằng trong `KhoaTamDangNhap` (backend); hằng cùng giá trị trong `frontend/src/lib/khoa-tam.ts` cho câu gợi ý |
| Đăng nhập | giờ hiện tại `:now` | `Instant.now(Clock)` cắt tới micro giây, lấy một lần mỗi request |
| Đăng nhập | `:den`, `:moc` | `:now` cộng `T`, `:now` trừ `T` |
| Đăng nhập | `dinh_danh_hash` | `RefreshTokenGenerator.sha256Hex(value)` của `TaiKhoanDangNhap.Email` hoặc `.SoDienThoai` (đã chuẩn hóa) |
| Đăng nhập | lượt cuối, đang khóa, hay lượt thường | cột `vua_khoa` và `khoa_cu` của câu giữ lượt (xem *Đọc kết quả*) |
| Đăng nhập 429 | `Retry-After` | `KhoaTamDangNhap.retryAfter(now, khoaDen)` = `max(1, ceil(Duration.between(now, khoaDen).toMillis() / 1000))` |
| Đăng nhập 429 | `message` | hằng `AuthErrorCodes.AUTH_TOO_MANY_ATTEMPTS_MESSAGE` |
| Trang đăng nhập | số phút `m` | `ApiError.retryAfter` (header `Retry-After`), `ceil(giây / 60)`, nhỏ nhất 1 |
| Trang đăng nhập | hiện câu gợi ý hay không | state của component form: đã nhận ít nhất một 401 `AUTH_INVALID_CREDENTIALS` |
| Chi tiết tài khoản | `khoaTamDen` | cột `khoa_tam_den` trong câu đọc chi tiết của `TaiKhoanQuery`, đổi thành `null` khi `<= Clock` ở `TaiKhoanTraCuuService` |
| Chi tiết tài khoản | giờ hiển thị "HH:mm dd/MM/yyyy" | `khoaTamDen` định dạng theo múi giờ `app.timezone` bằng helper định dạng ngày giờ có sẵn ở frontend |
| Mở khóa sớm | có ghi nhật ký hay không, giá trị cũ | `RETURNING old.khoa_tam_den` so với giờ `Clock` |
| Mở khóa sớm | người thực hiện | `maNguoiDung` của principal đang đăng nhập (như spec 0008) |
| Nhật ký khóa tạm | `khoaTamDen` trong chi tiết | `:den` của lượt cuối |
| Trang nhật ký | nhãn hai loại mới | `LoaiThaoTac.nhan()` (`"Tạm khóa đăng nhập do nhập sai nhiều lần"`, `"Mở khóa đăng nhập"`), theo đường trang `/nhat-ky` đang lấy nhãn cho các loại có sẵn |
| Tạo, sửa tài khoản | hash cần xóa | `sha256Hex` của `soDienThoai` và `email` sau chuẩn hóa (spec 0008 AC-1) |
| Job dọn | mốc xóa | giờ `Clock` trừ `T` |

**Key invariants**:
- Giữa hai lần đặt lại bộ đếm, không bao giờ có quá `N` lần so mật khẩu (thật hay giả) cho một tài khoản hoặc một định danh ảo, kể cả khi request chạy song song: giữ lượt khóa dòng, xảy ra trước bcrypt, và chỉ được khi không đang khóa.
- Trong lúc khóa, phản hồi không phụ thuộc mật khẩu gửi lên, trạng thái tài khoản, hay việc tài khoản có tồn tại; và không có lần ghi DB nào.
- `so_lan_dang_nhap_sai` và `so_lan_sai` luôn nằm trong `0..N-1` sau mỗi câu giữ lượt.
- Đọc ghi bộ đếm không bao giờ tăng `phien_ban` hay đổi `cap_nhat_luc` của `nguoi_dung_he_thong`, để người gõ nhầm không làm form của người quản lý bị 409.
- `khoa_tam_den` và `trang_thai_tai_khoan` độc lập: endpoint AC-8 chỉ đụng ba cột bộ đếm; đổi trạng thái chỉ đặt lại bộ đếm khi mở lại (như spec 0008).
- Mọi so sánh thời gian dùng giờ `Clock` truyền vào câu SQL, không dùng `now()` của PostgreSQL, để test điều khiển được thời gian.
- Một định danh đang có chủ không có dòng trong `dang_nhap_sai_dinh_danh` sau khi tạo hoặc sửa tài khoản (AC-19).

**Lớp mới và lớp sửa** (mỗi lớp một trách nhiệm, dưới khoảng 200 dòng):
- `auth.service.KhoaTamDangNhap`: hằng `SO_LAN_SAI_TOI_DA`, `THOI_GIAN_KHOA`, và hàm tĩnh `retryAfter`.
- `auth.service.KhoaTamDangNhapService`: `giuLuot(Optional<String> maNguoiDung, TaiKhoanDangNhap dinhDanh, Instant now)` trả `LuotThu`; `datLai(String maNguoiDung)`; `xoaDinhDanh(String soDienThoai, String email)` cho AC-19.
- `auth.repository.KhoaTamTaiKhoanRepository` và `auth.repository.KhoaTamDinhDanhRepository`: `JdbcClient`, câu giữ lượt, đặt lại, (bảng định danh) đảm bảo dòng, xóa theo hash, xóa dòng hết tác dụng. Hai lớp riêng, không interface chung (chỉ `KhoaTamDangNhapService` và `PhienDangNhapService` gọi, và chúng biết lúc nào dùng cái nào).
- `auth.service.MoKhoaDangNhapService`: kiểm phạm vi, chạy `UPDATE ... RETURNING old.khoa_tam_den`, ghi nhật ký, đọc lại chi tiết.
- `auth.service.DangNhapSaiCleanupJob`: như `PhienDangNhapCleanupJob`.
- `auth.service.ChiTietKhoaTam(Instant khoaTamDen)` cài `ChiTietNhatKy`.
- Sửa backend: `DangNhapService` (luồng mới), `PhienDangNhapService.taoPhien` (đặt lại bộ đếm), `TaiKhoanGhi.datLaiMatKhau` (thêm ba cột vào câu `UPDATE`), `TaoTaiKhoanService` và `CapNhatTaiKhoanService` (gọi `xoaDinhDanh`), `TaiKhoanQuery` câu chi tiết và `TaiKhoanChiTietResponse` (`khoaTamDen`), `TaiKhoanController` (handler mới), `AuthErrorCodes` (`AUTH_TOO_MANY_ATTEMPTS` và câu), `LoaiThaoTac` (`AUTH_KHOA_TAM_DANG_NHAP`, `AUTH_MO_KHOA_TAM_DANG_NHAP`). `ChiTietDangNhapThatBai` không đổi.
- Sửa frontend: `src/lib/api/api.ts` (`ApiError.retryAfter`), `src/lib/api/error-messages.ts` (`AUTH_TOO_MANY_ATTEMPTS` và hàm `thongBaoKhoaTam(retryAfter)`), `src/lib/khoa-tam.ts` (hằng `SO_LAN_SAI_TOI_DA`, `PHUT_KHOA_TAM`), `src/app/dang-nhap/login-form.tsx`, thẻ Trạng thái và bảo mật ở `src/app/(app)/tai-khoan/[maNguoiDung]/`, hook mutation mở khóa, nhãn và bộ lọc ở `src/app/(app)/nhat-ky/`.
- Test: helper dùng chung (ví dụ `KhoaTamTestSupport`) đặt lại ba cột trên mọi tài khoản và `TRUNCATE dang_nhap_sai_dinh_danh`, gọi trong `@BeforeEach` của mọi integration test gửi đăng nhập sai (các test của spec 0004, 0005, 0007, 0008 và spec này).

**Security model**:
- `POST /auth/login` công khai như cũ. Khóa tạm là cơ chế chống dò theo từng tài khoản hoặc định danh; giới hạn theo IP để ngoài phạm vi (Follow-up).
- Không lộ tồn tại: định danh ảo đi cùng đường với tài khoản thật (cùng số lượt, cùng cửa sổ đếm, cùng mã lỗi, cùng câu, cùng header, cùng việc chạy hay không chạy bcrypt). Còn chênh lệch nhỏ về số câu SQL (định danh ảo thêm một câu `INSERT ... DO NOTHING`), rất nhỏ so với độ dao động của mạng và bcrypt.
- Endpoint mở khóa: Admin với mọi tài khoản, BQL chỉ với tài khoản cư dân thuần trong phạm vi (qua `TaiKhoanPhamViService`); ngoài phạm vi là 404 cùng câu, như spec 0008.
- Bảng `dang_nhap_sai_dinh_danh` không chứa định danh gốc. `SHA-256` của số điện thoại bẻ ngược được bằng thử hết (khoảng 10^9 khả năng), nên hash ở đây chỉ để không có chuỗi thô trong DB và bản dump, không phải lớp bảo vệ mật mã. Dòng chỉ sống tối đa khoảng một ngày (tới lần dọn kế tiếp sau khi hết tác dụng).
- Đổi mật khẩu với mật khẩu cũ sai (`AUTH_OLD_PASSWORD_INCORRECT`) không bị đếm, vì người gọi đã đăng nhập.

**Configuration required**: không có biến môi trường mới. `N` và `T` là hằng trong code (xem rationale.md, mục quyết định nhỏ). Job dọn dùng lại `app.security.session-cleanup-cron` và `app.timezone`.

**Critical test scenarios**:
- Integration (Testcontainers, `Clock` điều khiển được): sai 4 lần được 401 và bộ đếm 1 tới 4; lần 5 được 429, `Retry-After: 900`, `khoa_tam_den` đúng, `phien_ban` và `cap_nhat_luc` không đổi, verifies **AC-1**, **AC-7**.
- Đang khóa, gửi mật khẩu đúng: 429, không có `Set-Cookie`, không có dòng `phien_dang_nhap` hay `nhat_ky_he_thong` mới, `@MockitoSpyBean` trên `MatKhauService` cho thấy không có lần gọi; tăng `Clock` thêm 10 phút thì `Retry-After: 300`, verifies **AC-2**, **AC-7**.
- Tăng `Clock` tới đúng `khoa_tam_den`: mật khẩu đúng đăng nhập được; làm lại từ đầu, sai một lần thì bộ đếm là 1, verifies **AC-3**.
- Sai 4 lần, tăng `Clock` 15 phút, sai lần nữa: 401 và bộ đếm 1; làm lại với 14 phút 59 giây: 429, verifies **AC-18**.
- Sai 3 lần, đúng một lần: ba cột về 0 và `NULL`. Tài khoản seed `0900000008` (`Tạm khóa`) sai 3 lần rồi đúng: 403 và bộ đếm 0. Ép `taoPhien` lỗi sau khi đặt lại: bộ đếm cũng rollback, verifies **AC-4**.
- Định danh ảo `0999999999`: sai 5 lần thì 429; so status, body (bỏ `requestId`) và header với một tài khoản thật bị khóa cùng lúc thì giống hệt; trong bảng có đúng một dòng với `dinh_danh_hash` là `SHA-256` của `0999999999` và không có cột nào chứa chuỗi đó; `abc` (sai định dạng) sai 10 lần vẫn 401 và không có dòng nào, verifies **AC-5**, **AC-7**.
- Sai 3 lần bằng số điện thoại rồi 2 lần bằng email của cùng tài khoản: lần thứ 5 là 429, verifies **AC-5**.
- Song song: 20 luồng gửi mật khẩu sai cùng lúc (latch bắt đầu chung): số 401 không quá 4, phần còn lại 429, `khoa_tam_den` khác `NULL`, `@MockitoSpyBean` đếm `khop` cộng `soKhopGia` không quá 5; làm lại với định danh ảo chưa có dòng (để thử cả câu `INSERT ... DO NOTHING` chạy song song), verifies **AC-6**.
- Mở khóa sớm: Admin gọi khi đang khóa thì 200, `khoaTamDen` `null`, đăng nhập được ngay, có dòng `AUTH_MO_KHOA_TAM_DANG_NHAP`, `phien_ban` không đổi; gọi lần nữa thì 200 không có nhật ký; BQL gọi cho tài khoản nhân viên thì 404; Cư dân gọi thì 403, verifies **AC-8**, **AC-9**, **AC-16**.
- Đặt lại mật khẩu khi đang khóa: đăng nhập bằng mật khẩu tạm được ngay; mở lại từ `Tạm khóa` vẫn đặt lại bộ đếm, verifies **AC-10**.
- Khóa định danh ảo `0988888888`, rồi Admin tạo tài khoản với số đó: dòng trong bảng biến mất, đăng nhập sai với số đó trả 401; tương tự khi sửa email của một tài khoản sang một email đang bị khóa ảo, verifies **AC-19**.
- Nhật ký: chuỗi sai 5 lần rồi bị chặn 3 lần cho ra đúng 5 dòng `SAI_THONG_TIN` và 1 dòng `AUTH_KHOA_TAM_DANG_NHAP`, không thêm dòng nào cho 3 lần bị chặn; log bắt được (`OutputCaptureExtension`) có 3 dòng `lyDo=KHOA_TAM`; quét `chi_tiet_thay_doi` và log không chứa định danh hay hash của nó, verifies **AC-11**, **AC-17**.
- Job dọn: dòng có `lan_sai_cuoi_luc` cũ hơn 15 phút và không đang khóa bị xóa; dòng 14 phút và dòng đang khóa còn lại; repository ném lỗi thì job log ERROR và không ném ra ngoài, verifies **AC-12**.
- Vitest + Testing Library: `api` đọc `Retry-After: 720` thành `retryAfter` 720, `"Wed, 21 Oct 2026 07:28:00 GMT"` thành `undefined`; form hiện "thử lại sau 12 phút" trong `role="alert"`; câu gợi ý chỉ xuất hiện sau lần 401 đầu, còn nguyên sau 429, và được `aria-describedby` trỏ tới, verifies **AC-13**, **AC-15**.
- Vitest: thẻ Trạng thái và bảo mật hiện dòng khóa và nút khi `khoaTamDen` khác `null`, ẩn khi `null`; bấm bằng Enter gọi mutation, sau thành công focus nằm ở tiêu đề thẻ; trang nhật ký hiện nhãn hai loại mới và có chúng trong bộ lọc, verifies **AC-14**, **AC-11**.

## Build plan

Tracer Bullet: lát đầu chạy thật một đường mảnh từ DB tới trang đăng nhập cho tài khoản có thật, các lát sau làm dày (định danh ảo, chạy song song, phía quản lý, nhật ký), cuối cùng khóa lại.

1. [x] **Đường mảnh cho tài khoản có thật**: migration `V13__khoa_tam_dang_nhap.sql` phần cột `lan_sai_cuoi_luc`, `KhoaTamDangNhap`, `KhoaTamTaiKhoanRepository` (giữ lượt, đặt lại), `KhoaTamDangNhapService` (chỉ nhánh có tài khoản), sửa `DangNhapService` và `PhienDangNhapService.taoPhien`, mã `AUTH_TOO_MANY_ATTEMPTS` với header `Retry-After` qua `ApiException(..., headers)`, khai báo 429 trong OpenAPI và sinh lại kiểu; helper test dùng chung và gắn vào các test đăng nhập có sẵn; frontend `ApiError.retryAfter` và câu có số phút trên `/dang-nhap`. Integration test sai 5 lần, bị chặn khi đúng mật khẩu, tự mở khi tới hạn, cửa sổ 15 phút, satisfies **AC-1**, **AC-2**, **AC-3**, **AC-4**, **AC-7**, **AC-13**, **AC-15**, **AC-18**
2. [x] **Định danh ảo**: phần bảng `dang_nhap_sai_dinh_danh` của `V13` (cùng file, vì `V13` chưa chạy ở đâu khi lát 1 và 2 còn trên một nhánh; nếu lát 1 đã merge thì tách thành `V14`), `KhoaTamDinhDanhRepository`, nhánh không có tài khoản trong `KhoaTamDangNhapService`, xóa dòng khi tạo hoặc sửa tài khoản, test so khớp phản hồi giữa tài khoản thật và định danh ảo, test dùng chung bộ đếm giữa số điện thoại và email, satisfies **AC-5**, **AC-7**, **AC-19**
3. [x] **Chạy song song**: test 20 luồng cho cả hai nhánh với spy đếm số lần so mật khẩu; sửa nếu có lượt lọt qua, satisfies **AC-6**
4. [x] **Phía người quản lý**: `khoaTamDen` trong câu chi tiết của `TaiKhoanQuery` và `TaiKhoanChiTietResponse`, `MoKhoaDangNhapService` và handler `DELETE .../login-lock`, sửa `TaiKhoanGhi.datLaiMatKhau`, sinh lại kiểu API, dòng khóa và nút trên thẻ Trạng thái và bảo mật, satisfies **AC-8**, **AC-9**, **AC-10**, **AC-14**, **AC-16**
5. [x] **Nhật ký, log và job dọn**: hai `LoaiThaoTac` mới, `ChiTietKhoaTam`, nhãn và bộ lọc ở `/nhat-ky`, các dòng log, `DangNhapSaiCleanupJob`, test quét bí mật, satisfies **AC-11**, **AC-12**, **AC-17**
6. [x] **Khóa lại**: câu gợi ý trên trang đăng nhập và `aria-describedby`, kiểm bàn phím và focus, `verify.md` cho spec này, cập nhật README phần tài khoản seed nếu cần ghi cách thử khóa tạm, satisfies **AC-13**, **AC-14**

## Consequences

**Positive**:
- Dò mật khẩu một tài khoản bị giới hạn ở 5 lượt mỗi 15 phút (khoảng 480 lượt mỗi ngày), kể cả khi kẻ dò gửi song song.
- Không lộ số điện thoại hay email nào có tài khoản qua cơ chế khóa, kể cả với kẻ dò kiên nhẫn: sau 15 phút yên, tài khoản thật và định danh ảo ở cùng trạng thái.
- Gõ nhầm rải rác qua nhiều ngày không cộng dồn thành một lần khóa bất ngờ.
- Người dùng biết chính xác phải chờ bao lâu; BQL mở sớm được mà không phải đặt lại mật khẩu.
- Trong lúc khóa, backend không chạy bcrypt và không ghi DB, nên một đợt dò dồn dập tốn ít CPU và không làm phình bảng nhật ký.

**Negative / tradeoffs**:
- Ai biết số điện thoại của một cư dân có thể giữ tài khoản đó bị khóa mãi bằng cách gửi 5 lần sai mỗi 15 phút (từ chối dịch vụ theo tài khoản). Đây là cái giá cố hữu của khóa theo tài khoản; cách giảm là giới hạn theo IP (Follow-up) và nút mở khóa của BQL, nhưng kẻ dò có thể khóa lại ngay.
- Vẫn còn một khe hẹp: nếu cư dân vừa gõ nhầm vài lần trong 15 phút gần nhất, kẻ dò thử đúng lúc đó sẽ bị khóa sớm hơn 5 lần và đoán được số đó có tài khoản. Cần trùng thời điểm với lỗi của chính chủ, nên chấp nhận.
- Mỗi lần đăng nhập thành công thêm một câu giữ lượt và một câu đặt lại; một lần đăng nhập thất bại khóa dòng tài khoản trong thời gian một câu SQL. Không đáng kể ở quy mô một chung cư.
- Ai biết cả số điện thoại lẫn email có thể biết chúng thuộc cùng một tài khoản (sai xen kẽ thì bị khóa ở lần 5 tổng cộng). Rò rỉ nhỏ, cần biết sẵn cả hai.
- `N` và `T` nằm ở hai nơi (hằng backend và hằng frontend cho câu gợi ý). Đổi một nơi mà quên nơi kia thì câu gợi ý sai; số phút ở câu báo khi khóa thì luôn đúng vì lấy từ `Retry-After`.
- Bảng `dang_nhap_sai_dinh_danh` ghi theo dữ liệu kẻ dò gửi lên; một đợt gửi hàng loạt định danh ngẫu nhiên làm bảng to ra (mỗi dòng khoảng 100 byte) cho tới lần dọn kế tiếp, tối đa khoảng một ngày.
- `taoPhien` lỗi sau khi đã giữ lượt (DB rớt giữa chừng) thì bộ đếm còn cao hơn một bậc, tự hết sau 15 phút.
- Các lần thử bị chặn không còn dấu vết trong nhật ký kiểm toán, chỉ có ở log ứng dụng (thường xoay vòng, không giữ lâu). Muốn điều tra một đợt dò dài phải dựa vào log.

**Neutral**:
- Spec 0004 AC-6 ("không đụng `so_lan_dang_nhap_sai`, `khoa_tam_den`") được thay bởi AC-4 của spec này.
- Hành vi mở lại từ `Tạm khóa` ở spec 0008 giữ nguyên; đặt lại mật khẩu thêm việc đặt lại bộ đếm; tạo và sửa tài khoản thêm việc xóa dòng định danh ảo.
- Dùng cú pháp `RETURNING old.` của PostgreSQL 18; dự án đã cố định PostgreSQL 18 (spec 0001).
- Test của các spec trước gửi nhiều lần sai cho cùng tài khoản seed cần helper đặt lại ở `@BeforeEach` (đã nằm trong lát 1).

## Follow-up

- [ ] Giới hạn theo IP cho `POST /auth/login` (chống dò rải trên nhiều tài khoản và giảm từ chối dịch vụ theo tài khoản). Cần quyết định trước việc tin `X-Forwarded-For` sau proxy ở bản demo; nên qua `/architect`.
- [ ] Spec 0004 AC-6 có câu đã bị AC-4 ở đây thay thế; `/sync` nên đánh dấu spec 0004 là đã được làm mới một phần.
- [ ] Tính năng 13 (quên mật khẩu qua email) nên quyết định xem đặt lại mật khẩu qua email có xóa khóa tạm không, cho khớp AC-10.
