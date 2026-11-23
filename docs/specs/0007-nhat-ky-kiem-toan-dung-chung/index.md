# 0007. Nhật ký kiểm toán dùng chung: `NhatKyService.ghi` trong transaction riêng, ghi sau commit, chi tiết JSONB có kiểu

**Date**: 2026-11-20
**Status**: Accepted

## Summary

Spec này chốt một cách ghi nhật ký kiểm toán (audit log, sổ ghi lại ai đã làm gì, lúc nào) mà mọi phân hệ cắm vào bằng một lời gọi `NhatKyService.ghi(...)`. Mỗi dòng được ghi trong một transaction riêng (một lần ghi DB độc lập), nên ghi log lỗi không bao giờ làm hỏng thao tác chính, và lần đăng nhập sai vẫn được lưu dù thao tác đó báo lỗi. Nếu nơi gọi đang ở trong một transaction thì dòng log chờ tới khi transaction đó commit (lưu xong) mới ghi, để không bao giờ có dòng "thành công" cho việc chưa xảy ra. Tính năng 8 dựng cơ chế này và gắn vào bốn sự kiện xác thực đã có (đăng nhập thành công, đăng nhập thất bại, đăng xuất, phát hiện dùng lại token); spec cũng chốt sẵn hợp đồng API đọc nhật ký để tính năng 9 làm thẳng.

## Requirements

**User stories**:
- Là Admin, ban quản trị hoặc ban quản lý, tôi muốn biết ai đã đăng nhập, ai đăng nhập sai và từ IP nào, để phát hiện dò mật khẩu và token bị đánh cắp.
- Là người làm các phân hệ sau (đổi mật khẩu, quản lý tài khoản, UC01 đến UC12), tôi muốn ghi một sự kiện bằng một lời gọi, không phải lo transaction, IP hay request id.
- Là người dùng, tôi muốn thao tác của mình không bao giờ thất bại chỉ vì hệ thống ghi nhật ký gặp lỗi.

**Acceptance criteria**

*Phía ghi (tính năng 8 làm):*

- **AC-1**: `POST /api/v1/auth/login` thành công tạo đúng một dòng `nhat_ky_he_thong`: `ma_nguoi_dung` là người vừa đăng nhập, `loai_thao_tac` `AUTH_DANG_NHAP_THANH_CONG`, `doi_tuong_tac_dong` `NguoiDungHeThong:<maNguoiDung>`, `chi_tiet_thay_doi` `{"maHoPhien": "<uuid họ phiên vừa tạo>"}`, `dia_chi_ip` bằng `getRemoteAddr()` của request (đã qua `forward-headers-strategy`) theo cùng quy tắc của `ThongTinThietBi` (cắt về 45 ký tự, chuỗi rỗng thành `NULL`), `request_id` bằng header `X-Request-Id` của response, `thoi_gian_ghi` bằng giờ của bean `Clock` lúc gọi `ghi`, cắt tới micro giây. `ghi` được gọi ngay trước `return` của `dangNhap`, tức sau khi transaction tạo phiên đã commit và access token đã được cấp; lỗi ở bước tra vai trò hay cấp token (500) thì không có dòng thành công.
- **AC-2**: Đăng nhập sai mật khẩu với tài khoản có thật tạo đúng một dòng `AUTH_DANG_NHAP_THAT_BAI`, `ma_nguoi_dung` là tài khoản đó, `doi_tuong_tac_dong` `NguoiDungHeThong:<maNguoiDung>`, chi tiết `{"lyDo": "SAI_THONG_TIN"}`. Đúng mật khẩu nhưng tài khoản không ở trạng thái hoạt động tạo một dòng như vậy với `{"lyDo": "TAI_KHOAN_KHOA"}`. Response (mã HTTP, body, cookie) giữ nguyên như spec 0004.
- **AC-3**: Đăng nhập với tài khoản không tồn tại, hoặc `taiKhoan` không phải số điện thoại hay email hợp lệ, tạo đúng một dòng `AUTH_DANG_NHAP_THAT_BAI` có `ma_nguoi_dung` `NULL`, `doi_tuong_tac_dong` `NguoiDungHeThong` (không có phần mã), chi tiết `{"lyDo": "SAI_THONG_TIN"}`, có `dia_chi_ip` và `request_id`. Không cột nào của dòng chứa chuỗi `taiKhoan` hay `matKhau` đã gửi. Mọi nhánh thất bại trong `DangNhapService` đều ghi đúng một dòng, nên thời gian phản hồi của nhánh "không có tài khoản" vẫn tương đương nhánh "sai mật khẩu" (giữ ý của spec 0004 AC-3). Request bị bean validation chặn trước service (thiếu hoặc rỗng `taiKhoan`, `matKhau`, trả 400) không tạo dòng nào.
- **AC-4**: `POST /api/v1/auth/logout` thu hồi được ít nhất một dòng phiên tạo đúng một dòng `AUTH_DANG_XUAT`: `ma_nguoi_dung` là chủ họ phiên, `doi_tuong_tac_dong` `NguoiDungHeThong:<maNguoiDung>`, chi tiết `{"maHoPhien": "<uuid>"}`, ghi sau khi transaction đăng xuất commit. Đăng xuất không có cookie, cookie sai định dạng, token không tìm thấy, hoặc họ phiên đã bị thu hồi hết (thu hồi 0 dòng) thì không tạo dòng nào.
- **AC-5**: Làm mới bằng token đã xoay vòng quá khoảng khoan dung (spec 0005 AC-4, cả họ phiên bị thu hồi) tạo đúng một dòng `AUTH_PHAT_HIEN_DUNG_LAI_TOKEN`: `ma_nguoi_dung` là chủ họ phiên, `doi_tuong_tac_dong` `NguoiDungHeThong:<maNguoiDung>`, chi tiết `{"maHoPhien": "<uuid>"}`, ghi sau khi transaction làm mới commit (transaction này commit dù ném `ApiException`, do `noRollbackFor`). Làm mới thành công, làm mới trong khoảng khoan dung, dùng lại token của họ phiên đã chết hẳn (không còn dòng còn hiệu lực, `timConHieuLuc` rỗng), và mọi lần làm mới thất bại khác không tạo dòng nào.
- **AC-6**: `NhatKyService.ghi` không bao giờ tham gia transaction của nơi gọi. Gọi khi không có transaction đang chạy thì ghi ngay trong một transaction mới. Gọi khi có transaction đang chạy thì ghi trong một transaction mới ngay sau khi transaction đó commit; transaction đó rollback thì không có dòng nào. `thoi_gian_ghi`, `request_id` và `dia_chi_ip` luôn lấy lúc gọi `ghi`, không lấy lúc ghi thật.
- **AC-7**: Ghi nhật ký lỗi vì bất kỳ lý do nào (DB từ chối do CHECK hay khóa ngoại, mất kết nối, hết thời gian chờ kết nối từ pool, lỗi chuyển chi tiết sang JSON) thì `ghi` không ném lỗi, không thử lại, và ghi một dòng log ứng dụng mức ERROR gồm `loaiThaoTac`, `doiTuongTacDong`, `maNguoiThucHien`, `requestId` và tên lớp exception (không gồm nội dung chi tiết). Thao tác chính trả đúng response như khi ghi thành công (cùng mã HTTP, body, cookie).
- **AC-8**: Nhật ký không bao giờ chứa mật khẩu, token hay hash. (a) Chi tiết là một record có kiểu cài interface `ChiTietNhatKy`, không nhận `Map` hay chuỗi tự do. (b) Lúc ghi, `NhatKyService` chuyển chi tiết sang cây JSON và kiểm mọi tên khóa ở mọi độ sâu bằng quy tắc `TenKhoaNhayCam`: tách tên theo ranh giới camelCase và dấu `_`, đổi về chữ thường; tên bị coi là nhạy cảm khi có một từ thuộc `{password, token, tokens, hash, secret, cookie, otp, matkhau}` hoặc hai từ liền nhau là `mat`, `khau`. Ví dụ `refreshToken`, `matKhauMoi`, `passwordHash` trúng; `maHoPhien`, `footprint`, `lyDo` không trúng. Trúng thì không ghi dòng đó và log ERROR như AC-7, kèm tên khóa trúng (không kèm giá trị). (c) Test `ChiTietNhatKyAnToanTest` tìm mọi lớp cài `ChiTietNhatKy` trong `vn.edu.hust.chungcu` bằng `ClassPathScanningCandidateComponentProvider` (tắt bộ lọc mặc định, thêm `AssignableTypeFilter(ChiTietNhatKy.class)`, gồm cả record lồng và record không public), và báo đỏ khi một thành phần record có tên trúng `TenKhoaNhayCam`, nêu tên lớp và tên thành phần. Test cũng kiểm bảng ví dụ ở (b). (d) Test tích hợp chạy đăng nhập thành công, đăng nhập sai, làm mới, dùng lại token và đăng xuất, rồi kiểm không dòng nào (mọi cột, kể cả `chi_tiet_thay_doi::text`) chứa mật khẩu đã gửi, refresh token, access token hay `mat_khau_hash`.
- **AC-9**: DB tự giữ tính bất biến: `UPDATE` hay `DELETE` một dòng `nhat_ky_he_thong` bị trigger từ chối với lỗi `Nhật ký hệ thống chỉ được thêm, không được sửa hay xóa`. Lớp ghi `NhatKyGhi` chỉ có một hàm thêm, không có hàm sửa hay xóa nào trong code. CHECK từ chối `loai_thao_tac` không khớp `^[A-Z][A-Z0-9_]*$`, `doi_tuong_tac_dong` không khớp `^[A-Z][A-Za-z]*(:[A-Za-z0-9-]+)?$`, và `chi_tiet_thay_doi` không phải JSON object.
- **AC-10**: Enum `LoaiThaoTac` (package `nhatky`) là danh mục duy nhất. Ở tính năng này có đúng bốn giá trị: `AUTH_DANG_NHAP_THANH_CONG` "Đăng nhập thành công", `AUTH_DANG_NHAP_THAT_BAI` "Đăng nhập thất bại", `AUTH_DANG_XUAT` "Đăng xuất", `AUTH_PHAT_HIEN_DUNG_LAI_TOKEN` "Phát hiện dùng lại token cũ". Test kiểm mỗi giá trị có tên khớp `^[A-Z]+_[A-Z0-9_]+$` (tiền tố là phân hệ), dài tối đa 100 ký tự, nhãn không rỗng và đã chuẩn hóa NFC, và không hai giá trị trùng nhãn.

*Phía đọc (hợp đồng cho tính năng 9, tính năng 8 không làm):*

- **AC-11**: `GET /api/v1/system/audit-logs` (API-AUTH-05) là `@ChoPhep({QUAN_TRI_VIEN, BAN_QUAN_LY, BAN_QUAN_TRI})`, trả 200 `message` "Tra cứu nhật ký kiểm toán thành công", `data` dạng `PageResponse` với mỗi phần tử `{ maLog, maNguoiDung, hoTen, loaiThaoTac, tenThaoTac, doiTuongTacDong, chiTietThayDoi, diaChiIp, requestId, thoiGianGhi }`. `chiTietThayDoi` là JSON object hoặc `null` (không phải chuỗi); trong OpenAPI nó là `type: object` với `additionalProperties: true` (Java `JsonNode` hoặc `Map<String, Object>`), để `pnpm gen:api` sinh `Record<string, unknown>`. `thoiGianGhi` là ISO 8601 UTC có `Z` (spec 0001). `tenThaoTac` là nhãn của enum; mã có trong DB mà không còn trong enum thì `tenThaoTac` bằng chính mã và việc đọc không lỗi. Phân trang qua `TrangYeuCau`: `sort` mặc định `thoiGianGhi,desc`, chỉ nhận `thoiGianGhi` và `loaiThaoTac`; `loaiThaoTac` sắp theo mã đã lưu, không theo nhãn; tiêu chí phụ luôn là `thoi_gian_ghi DESC, ma_log DESC` (với `thoiGianGhi,asc` thì `ma_log ASC`). `page` vượt trang cuối trả 200 với `items` rỗng và `totalItems`, `totalPages` đúng. Vai trò khác nhận 403 `FORBIDDEN`. Không có endpoint sửa hay xóa nhật ký, và việc đọc không tự ghi nhật ký.
- **AC-12**: Bộ lọc, mọi tham số không bắt buộc (khai `String`; thiếu hoặc chuỗi rỗng coi như không gửi, giống `TrangYeuCau`), kết hợp bằng AND: `maNguoiDung` (khớp đúng `^ND[0-9]{8}$`, so bằng; dòng có `ma_nguoi_dung` `NULL` chỉ thấy khi không lọc theo người), `loaiThaoTac` (đúng tên một giá trị `LoaiThaoTac`, phân biệt hoa thường), `tuNgay` và `denNgay` (dạng `yyyy-MM-dd`, đọc bằng `LocalDate.parse`, nên ngày không có thật như `2026-02-30` là sai; theo `app.timezone`; giữ dòng có `thoi_gian_ghi >= tuNgay 00:00` và `< (denNgay + 1 ngày) 00:00`, tức gồm cả ngày `denNgay`). Sai trả 400 `VALIDATION_FAILED` `message` "Dữ liệu không hợp lệ", một mục `errors` cho mỗi tham số sai theo thứ tự `page`, `size`, `sort`, `maNguoiDung`, `loaiThaoTac`, `tuNgay`, `denNgay`, với câu:
  - `maNguoiDung`: "Mã người dùng không đúng định dạng"
  - `loaiThaoTac`: "Loại thao tác không tồn tại"
  - `tuNgay`, `denNgay` sai dạng: "Ngày phải có dạng yyyy-MM-dd"
  - `denNgay` khi `tuNgay` sau `denNgay` (chỉ xét khi cả hai đúng dạng): "Đến ngày phải bằng hoặc sau từ ngày"
- **AC-13**: `hoTen` lấy qua một hàm công khai của `auth` nhận tập `maNguoiDung` của trang và trả `Map<maNguoiDung, hoTen>` bằng một truy vấn; `nhatky` không đọc bảng của `auth`. Dòng có `maNguoiDung` `NULL`, hoặc mã không có trong map trả về, thì `hoTen` `null`. Mỗi lần gọi danh sách chạy tối đa ba truy vấn: đếm, lấy trang, lấy họ tên.
- **AC-14**: `GET /api/v1/system/audit-logs/action-types` cùng ba vai trò, trả 200 `message` "Lấy danh mục loại thao tác thành công", `data` là mảng `{ ma, nhan }` theo thứ tự khai báo của enum.

## Decision

**Chosen option**: Option 1: ghi đồng bộ trong transaction riêng, hoãn tới sau commit khi đang có transaction.

Thêm phân hệ `vn.edu.hust.chungcu.nhatky` có `NhatKyService.ghi(SuKienNhatKy)` là cửa duy nhất để ghi. Service ghi bằng `JdbcClient` (không có entity JPA) qua `TransactionTemplate` với `PROPAGATION_REQUIRES_NEW`, đăng ký ghi ở `afterCommit` khi đang có transaction, bắt mọi lỗi. Bảng `nhat_ky_he_thong` được sửa bằng `V11`. Bốn chỗ trong `auth` gọi `ghi`. Phía đọc (controller, query) do tính năng 9 làm theo AC-11 đến AC-14.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`), theo quy ước controller, JPA và cấu hình; riêng lỗi theo vỏ bọc của spec 0001.

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

**Data model sketch** (bảng có sẵn từ V9, đang trống; sửa bằng `V11__nhat_ky_he_thong.sql`):

| Cột | Kiểu | Null | Ghi chú |
|---|---|---|---|
| `ma_log` | `VARCHAR(30)` PK | không | giữ nguyên, `LOG` + 8 chữ số từ `seq_nhat_ky_he_thong` |
| `ma_nguoi_dung` | `VARCHAR(30)` FK → `nguoi_dung_he_thong`, `ON DELETE RESTRICT` | **có** | bỏ `NOT NULL`; `NULL` khi không xác định được người (AC-3) |
| `loai_thao_tac` | `VARCHAR(100)` | không | thêm CHECK `^[A-Z][A-Z0-9_]*$`; lưu tên enum `LoaiThaoTac` |
| `doi_tuong_tac_dong` | `VARCHAR(150)` | không | thêm CHECK `^[A-Z][A-Za-z]*(:[A-Za-z0-9-]+)?$`; dạng `Loai:ma` hoặc `Loai` |
| `thoi_gian_ghi` | `TIMESTAMPTZ` | không | app luôn đặt từ `Clock`; giữ default `now()` |
| `chi_tiet_thay_doi` | `TEXT` → **`JSONB`** | có | `USING chi_tiet_thay_doi::jsonb`; CHECK `jsonb_typeof(chi_tiet_thay_doi) = 'object'` khi khác `NULL` |
| `dia_chi_ip` | `VARCHAR(45)` | có | **mới**; cắt về 45 ký tự như `ThongTinThietBi` |
| `request_id` | `VARCHAR(64)` | có | **mới**; `RequestIdFilter` đã giới hạn 64 ký tự |

- Quan hệ: `nguoi_dung_he_thong` 1 : N `nhat_ky_he_thong`, phía nhật ký không bắt buộc.
- Tên ràng buộc mới: `chk_nhat_ky_he_thong_loai_thao_tac`, `chk_nhat_ky_he_thong_doi_tuong`, `chk_nhat_ky_he_thong_chi_tiet`.
- Index: giữ `idx_nhatky_nguoidung_thoigian (ma_nguoi_dung, thoi_gian_ghi)`; thêm `idx_nhat_ky_he_thong_thoi_gian (thoi_gian_ghi DESC, ma_log DESC)` cho danh sách mặc định kèm tiêu chí phụ, và `idx_nhat_ky_he_thong_loai_thoi_gian (loai_thao_tac, thoi_gian_ghi)` cho lọc theo loại.
- Trigger `trg_nhat_ky_he_thong_chi_them` `BEFORE UPDATE OR DELETE FOR EACH ROW` gọi hàm `chan_sua_xoa_nhat_ky()` `RAISE EXCEPTION` với câu ở AC-9. `TRUNCATE` không bị chặn; đó là cách dọn bảng duy nhất được phép, chỉ dùng trong test. Migration sau này cũng không `UPDATE` hay `DELETE` được bảng này.
- Giữ vô thời hạn, không có job dọn.
- Không có entity JPA cho bảng này (Hibernate `validate` không xét nó). Ghi bằng `NhatKyGhi` (package `nhatky.repository`, `JdbcClient`): `INSERT INTO nhat_ky_he_thong (ma_nguoi_dung, loai_thao_tac, doi_tuong_tac_dong, thoi_gian_ghi, chi_tiet_thay_doi, dia_chi_ip, request_id) VALUES (..., CAST(:chiTiet AS jsonb), ...)`, để DB sinh `ma_log`. `thoi_gian_ghi` bind dạng `OffsetDateTime` ở UTC (pgjdbc không bind `Instant`). Đọc bằng `NhatKyQuery` (tính năng 9), lấy `chi_tiet_thay_doi::text` rồi `JsonMapper.readTree`, và đọc `thoi_gian_ghi` dạng `OffsetDateTime` rồi đổi sang `Instant`.

**Kiểu dữ liệu công khai** (package `nhatky`, phân hệ khác chỉ dùng những kiểu này):

```java
public enum LoaiThaoTac {
	AUTH_DANG_NHAP_THANH_CONG("Đăng nhập thành công"),
	AUTH_DANG_NHAP_THAT_BAI("Đăng nhập thất bại"),
	AUTH_DANG_XUAT("Đăng xuất"),
	AUTH_PHAT_HIEN_DUNG_LAI_TOKEN("Phát hiện dùng lại token cũ");
	// nhan()
}

public interface ChiTietNhatKy {}                          // đánh dấu, mỗi sự kiện một record

public record DoiTuongTacDong(String loai, String ma) {    // ma có thể null; toString() "loai:ma" hoặc "loai"
	public static DoiTuongTacDong nguoiDung(String maNguoiDung) { ... }   // maNguoiDung null cho ra "NguoiDungHeThong"
}

public record SuKienNhatKy(LoaiThaoTac loai, String maNguoiThucHien, DoiTuongTacDong doiTuong, ChiTietNhatKy chiTiet) {}
// maNguoiThucHien và chiTiet có thể null; loai và doiTuong bắt buộc

public class NhatKyService { public void ghi(SuKienNhatKy suKien) { ... } }   // không bao giờ ném lỗi
```

Kiểm tra ở constructor của record (ném `IllegalArgumentException`, để test của nơi gọi đỏ ngay): `SuKienNhatKy` cần `loai` và `doiTuong` khác `null`; `DoiTuongTacDong.loai` khớp `^[A-Z][A-Za-z]*$`, `ma` là `null` hoặc khớp `^[A-Za-z0-9-]+$`, và `toString()` dài tối đa 150 ký tự. Dựng record ở nơi gọi là chỗ duy nhất được phép ném lỗi; `ghi` chỉ nuốt lỗi.

Record chi tiết nằm ở phân hệ phát ra sự kiện. Của `auth` ở tính năng này (package `auth.service`): `ChiTietPhien(UUID maHoPhien)` cho AC-1, AC-4 và AC-5, `ChiTietDangNhapThatBai(String lyDo)` cho AC-2 và AC-3 (`lyDo` là `SAI_THONG_TIN` hoặc `TAI_KHOAN_KHOA`).

**Luồng `ghi`** (theo thứ tự):
1. Đọc ngay lúc gọi: `Instant.now(clock)` cắt micro giây, `NguCanhYeuCau.requestId()` và `NguCanhYeuCau.diaChiIp()`.
2. Chuyển `chiTiet` sang `JsonNode` bằng `JsonMapper` của app (Jackson 3, `tools.jackson`), kiểm tên khóa theo AC-8 (b), rồi ghi ra chuỗi. Lỗi ở bước này thì log ERROR và dừng.
3. `TransactionSynchronizationManager.isSynchronizationActive()` đúng thì `registerSynchronization` với `afterCommit` chạy bước 4; sai thì chạy bước 4 ngay.
4. `TransactionTemplate` (`PROPAGATION_REQUIRES_NEW`) gọi `NhatKyGhi.them(...)`. Mọi `RuntimeException` được bắt, log ERROR theo AC-7. Không dùng `@Transactional` trên chính `ghi` (gọi nội bộ trong cùng bean thì proxy không chạy, và `afterCommit` cần transaction mới).

`NguCanhYeuCau` là interface nhỏ hai hàm (`requestId()`, `diaChiIp()`), có bản web `NguCanhYeuCauWeb` (đọc `MDC.get(RequestIdFilter.MDC_KEY)` và `RequestContextHolder` → `HttpServletRequest.getRemoteAddr()`, cắt về 45 ký tự và chuỗi rỗng thành `null` như `ThongTinThietBi`; trả `null` khi không có request, ví dụ trong job) và bản giả trong unit test.

**Gắn vào `auth`** (tính năng 8):

| Nơi | Lúc gọi | Sự kiện |
|---|---|---|
| `DangNhapService.dangNhap` | ngay trước mỗi `throw` của ba nhánh thất bại (không có transaction, ghi ngay) | AC-2, AC-3 |
| `DangNhapService.dangNhap` | ngay trước `return`, sau khi cấp access token (transaction tạo phiên đã commit, ghi ngay) | AC-1 |
| `DangXuatService.dangXuat` | khi `soDong > 0`, trong transaction (hoãn tới sau commit) | AC-4 |
| `LamMoiPhienService.dungLaiTokenDaXoay` | sau `thuHoiHo` của nhánh quá khoảng khoan dung, trước `throw` (hoãn tới sau commit) | AC-5 |

`DangXuatService` cần `maNguoiDung` của họ phiên: đổi `timHoPhien` (hoặc thêm hàm bên cạnh) để trả cả `maHoPhien` và `maNguoiDung` trong một truy vấn. Các dòng log ứng dụng `log.info`/`log.warn` hiện có giữ nguyên.

**API surface**:

| Endpoint / hàm | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `NhatKyService.ghi` (Java, tính năng 8) | gọi trong process | `SuKienNhatKy` | không có | nơi gọi tự kiểm | không ném; lỗi thành log ERROR |
| `/api/v1/system/audit-logs` (tính năng 9) | GET | `page`, `size`, `sort`, `maNguoiDung`, `loaiThaoTac`, `tuNgay`, `denNgay` (đều không bắt buộc, khai `String`) | `PageResponse` các phần tử ở AC-11 | `@ChoPhep({QUAN_TRI_VIEN, BAN_QUAN_LY, BAN_QUAN_TRI})` | 400 `VALIDATION_FAILED`, 401 `UNAUTHENTICATED`, 403 `FORBIDDEN` |
| `/api/v1/system/audit-logs/action-types` (tính năng 9) | GET | không có | `[{ ma, nhan }]` | như trên | 401, 403 |
| Hàm công khai của `auth` (tính năng 9), ví dụ `NguoiDungTraCuuService.hoTenTheoMa(Collection<String>)` | gọi trong process | tập mã | `Map<String, String>` | không áp dụng | tập rỗng thì trả map rỗng, không truy vấn |

Phía đọc dùng `JdbcClient` như `ToaNhaQuery` (một lớp `NhatKyQuery`), không dùng JPA cho truy vấn danh sách.

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| `ghi` | `ma_log` | DB, `seq_nhat_ky_he_thong` |
| `ghi` | `ma_nguoi_dung` | `SuKienNhatKy.maNguoiThucHien`; đăng nhập: `tk.getMaNguoiDung()` hoặc `null`; đăng xuất: truy vấn họ phiên; dùng lại token: `dongCu.getMaNguoiDung()` |
| `ghi` | `loai_thao_tac` | `SuKienNhatKy.loai.name()` |
| `ghi` | `doi_tuong_tac_dong` | `SuKienNhatKy.doiTuong.toString()`; ở tính năng này luôn `DoiTuongTacDong.nguoiDung(maNguoiThucHien)` |
| `ghi` | `chi_tiet_thay_doi` | record chi tiết qua `JsonMapper` của app |
| `ghi` | `maHoPhien` trong chi tiết | đăng nhập: `maHoPhien` vừa sinh trong `DangNhapService`; đăng xuất: truy vấn họ phiên; dùng lại token: `dongCu.getMaHoPhien()` |
| `ghi` | `lyDo` | nhánh thất bại trong `DangNhapService`, cùng giá trị với log ứng dụng hiện có |
| `ghi` | `thoi_gian_ghi` | bean `Clock` lúc gọi `ghi` |
| `ghi` | `dia_chi_ip` | `NguCanhYeuCau` → `getRemoteAddr()`, cắt 45 ký tự |
| `ghi` | `request_id` | `NguCanhYeuCau` → MDC `requestId` của `RequestIdFilter` |
| danh sách | `hoTen` | hàm công khai của `auth` theo lô (AC-13) |
| danh sách | `tenThaoTac` | `LoaiThaoTac.valueOf(ma).nhan()`, mã lạ thì chính mã |
| danh sách | ranh giới `tuNgay`, `denNgay` | `app.timezone` (spec 0001), đổi sang `Instant` ở backend |
| danh sách | `totalItems`, `totalPages` | truy vấn đếm cùng điều kiện lọc, qua `PageResponse` |
| danh mục | `ma`, `nhan` | `LoaiThaoTac.values()` |

**Key invariants**:
- Một sự kiện nghiệp vụ tạo nhiều nhất một dòng; một thao tác rollback tạo không dòng nào.
- `ghi` không bao giờ ném lỗi và không bao giờ đổi kết quả của thao tác chính.
- Dòng đã ghi không sửa, không xóa (trigger + repository chỉ thêm và đọc).
- Không mật khẩu, token, hash hay chuỗi `taiKhoan` thô nào nằm trong bất kỳ cột nào.
- Mọi mã loại thao tác nằm trong enum `LoaiThaoTac`; phân hệ thêm loại mới bằng một giá trị enum có tiền tố của mình.

**Security model**:
- Ghi: chỉ code backend qua `NhatKyService`; không có API ghi.
- Đọc: `QUAN_TRI_VIEN`, `BAN_QUAN_LY`, `BAN_QUAN_TRI` xem toàn bộ, gồm IP và request id; không lọc theo phạm vi tòa nhà (sự kiện xác thực không gắn tòa). Vai trò khác 403.
- Dữ liệu cá nhân: IP và mã người dùng. Không lưu chuỗi người dùng gõ vào ô tài khoản (có thể là mật khẩu gõ nhầm ô).
- Độ tin cậy của IP: `server.forward-headers-strategy: framework` tin `X-Forwarded-For` của bất kỳ ai gửi tới backend. `dia_chi_ip` chỉ đúng khi chỉ Next.js gọi được backend (cổng 8080 không mở ra ngoài, như `docker-compose.prod.yml` cần giữ). Nếu backend lộ ra ngoài, kẻ tấn công ghi được IP bất kỳ vào nhật ký.
- Không thuộc chuẩn tuân thủ cụ thể nào (spec 0001); nhật ký này là phần bắt buộc cho thao tác chạm quyền truy cập và tiền về sau.

**Configuration required**: không có biến môi trường mới. Đặt rõ `spring.datasource.hikari.maximum-pool-size: 20` trong `application.yml` (mặc định là 10), vì mỗi sự kiện hoãn sau commit cần kết nối thứ hai trong khi kết nối đầu chưa trả về pool.

**Critical test scenarios**:
- Happy path: đăng nhập tài khoản seed qua API thật, rồi đọc DB thấy đúng một dòng với đủ cột như AC-1, verifies **AC-1**
- Tài khoản lạ: đăng nhập `0999999999`, thấy một dòng `ma_nguoi_dung` `NULL`, không cột nào chứa `0999999999` hay mật khẩu đã gửi, verifies **AC-3**, **AC-8**
- Sau commit: đăng xuất ghi dòng sau commit; một test gọi `ghi` trong `TransactionTemplate` rồi `setRollbackOnly` thấy không có dòng, verifies **AC-4**, **AC-6**
- Dùng lại token quá khoảng khoan dung (đặt giờ bằng `Clock` của test) ghi một dòng dù response là 401, verifies **AC-5**
- Ghi lỗi: thay `NhatKyGhi` bằng bản ném lỗi (hoặc chi tiết có khóa `refreshToken`) thì đăng nhập vẫn trả 200 với cookie như thường và có log ERROR, verifies **AC-7**, **AC-8**
- DB: `UPDATE` và `DELETE` bằng `JdbcClient` đều lỗi; chèn `loai_thao_tac` `dang nhap` bị CHECK từ chối, verifies **AC-9**
- Đọc (tính năng 9): `CU_DAN` gọi danh sách nhận 403; lọc `tuNgay=denNgay` lấy đúng các dòng trong ngày đó theo giờ Việt Nam, verifies **AC-11**, **AC-12**

## Build plan

Tracer Bullet cho một cơ chế backend: đường mỏng đầu tiên đi thật từ `POST /api/v1/auth/login` qua `NhatKyService` tới bảng đã sửa, rồi làm dày. Tính năng 8 không có giao diện; màn hình là tính năng 9. Mỗi mốc kết thúc bằng test tích hợp chạy được.

**Tính năng 8**

1. Đường mỏng: `V11__nhat_ky_he_thong.sql` (đủ mô hình đích một lần, vì bảng đang trống), `NhatKyGhi` (`JdbcClient`, chỉ thêm), pool 20 trong `application.yml`, `LoaiThaoTac`, `ChiTietNhatKy`, `DoiTuongTacDong`, `SuKienNhatKy`, `NguCanhYeuCau` và bản web, `NhatKyService.ghi` ghi ngay bằng `TransactionTemplate` `REQUIRES_NEW`; gắn ba nhánh đăng nhập thất bại và nhánh thành công; test tích hợp qua API thật, satisfies **AC-1**, **AC-2**, **AC-3**, **AC-10**
2. Sau commit và các sự kiện phiên: nhánh `afterCommit` trong `ghi`; truy vấn họ phiên trả cả `maNguoiDung`; gắn đăng xuất và dùng lại token; test rollback, satisfies **AC-4**, **AC-5**, **AC-6**
3. Chịu lỗi và giữ bí mật: bắt lỗi và log ERROR, `TenKhoaNhayCam` và kiểm tên khóa lúc ghi, kiểm tra ở constructor của record, `ChiTietNhatKyAnToanTest`, test quét mọi cột sau các luồng xác thực, test trigger và CHECK, test `jsonb_typeof` của dòng đã ghi là `object`, unit test `NhatKyService` với `NhatKyGhi` và `NguCanhYeuCau` giả, satisfies **AC-7**, **AC-8**, **AC-9**

**Tính năng 9** (để `/develop màn hình tra cứu nhật ký kiểm toán` dùng, chia mốc khi làm)

4. API đọc: `NhatKyQuery`, controller hai endpoint, đọc bộ lọc và báo lỗi theo từng tham số, hàm họ tên theo lô của `auth`, sinh lại `openapi.json`, satisfies **AC-11**, **AC-12**, **AC-13**, **AC-14**
5. Màn hình `/nhat-ky`: bật mục đã có trong `muc-chuc-nang.ts`, bảng theo mẫu *Bảng danh sách* của `frontend/design.md`, bộ lọc trên URL lấy danh mục từ `action-types`, theo "Done when" của tính năng 9 trong scope, satisfies **AC-11**, **AC-12**, **AC-14**

## Consequences

**Positive**:
- Phân hệ nào cũng ghi được bằng một dòng, không phải nghĩ về transaction, IP hay request id.
- Đăng nhập sai vẫn được lưu, và không có dòng "thành công" cho thao tác đã rollback.
- Dòng nhật ký nối được với log ứng dụng qua `request_id`.
- DB tự chặn sửa và xóa, nên một câu SQL tay hay code sai về sau không xóa dấu vết được.

**Negative / tradeoffs**:
- Mất dòng là có thể: tiến trình chết giữa commit thao tác và lúc ghi `afterCommit`, hoặc DB lỗi lúc ghi. Chỉ còn dòng log ERROR làm dấu vết. Chấp nhận vì quy mô nhỏ và yêu cầu "không làm hỏng thao tác chính".
- Mỗi sự kiện tốn thêm một transaction và một kết nối DB trong thời gian ngắn. Sự kiện hoãn sau commit (đăng xuất, dùng lại token) giữ hai kết nối cùng lúc; nếu số request như vậy chạy đồng thời bằng cỡ pool, chúng chờ nhau tới hết `connection-timeout` (30 giây), rồi dòng nhật ký bị bỏ và chỉ còn log ERROR. Pool 20 đủ xa cho quy mô này, nhưng đây là giới hạn thật, cần nhớ khi tăng tải.
- Dò nhiều tài khoản không tồn tại sinh nhiều dòng không xóa được; chưa có giới hạn theo IP. `seq_nhat_ky_he_thong` có trần 99.999.999 (`NO CYCLE`); chạm trần thì mọi lần ghi sau đều lỗi và bị bỏ.
- `dia_chi_ip` giả được nếu backend lộ ra ngoài (xem *Security model*).
- Danh mục `LoaiThaoTac` là một enum chung, nên mọi phân hệ sửa cùng một file (dễ xung đột khi merge, nhưng thấy ngay toàn bộ danh mục).
- `ON DELETE RESTRICT` cộng nhật ký giữ vô thời hạn nghĩa là người dùng đã có dòng nhật ký không xóa cứng được (chỉ khóa). Đúng hướng của hệ thống, nhưng test nào xóa người dùng phải chọn người chưa có dòng nhật ký.
- `hoTen` là tên hiện tại, không phải tên lúc thao tác.

**Neutral**:
- Thêm phân hệ `nhatky` ngang hàng với `auth`, `canho`; `auth` phụ thuộc `nhatky` (gọi `ghi`), và ở tính năng 9 `nhatky` gọi một hàm công khai của `auth`. Hai chiều phụ thuộc này đi qua service công khai, đúng quy ước, nhưng phải giữ để không thành vòng phụ thuộc giữa bean (nhatky không inject service nào của auth ở tính năng 8).
- `chi_tiet_thay_doi` đổi từ `TEXT` sang `JSONB`; file `sql/database_schema_quan_ly_chung_cu.sql` và file xlsx lệch thêm một chỗ.

## Follow-up

- [ ] Tính năng 10 thêm `AUTH_DOI_MAT_KHAU` "Đổi mật khẩu", `doi_tuong_tac_dong` `NguoiDungHeThong:<ma>`, chi tiết `{"soPhienThuHoi": n}`; không ghi mật khẩu cũ hay mới.
- [ ] Tính năng 11 thêm `AUTH_KHOA_TAI_KHOAN` "Khóa tài khoản" và `AUTH_MO_KHOA_TAI_KHOAN` "Mở khóa tài khoản": người thực hiện là Admin hoặc BQL, đối tượng là tài khoản bị tác động, chi tiết `{"truoc": {"trangThaiTaiKhoan": ...}, "sau": {...}}`; tạo và sửa tài khoản cũng cần loại riêng do spec của tính năng 11 chốt.
- [ ] Tính năng 12 (khóa tạm) nên xem cả giới hạn theo IP cho đăng nhập, vì dò tài khoản không tồn tại hiện sinh dòng nhật ký không giới hạn.
- [ ] Trước khi đưa bản demo ra Internet: khai rõ proxy tin cậy (`server.tomcat.remoteip.internal-proxies` hoặc tương đương) và kiểm cổng 8080 của backend không mở ra ngoài, để `dia_chi_ip` không bị giả.
- [ ] Endpoint `GET /api/v1/system/audit-logs/action-types` và các trường `tenThaoTac`, `diaChiIp`, `requestId` chưa có trong file đặc tả xlsx; cập nhật xlsx trước khi nộp, cùng việc đã ghi ở spec 0002.
- [ ] `/sync` sau khi tính năng 8 xong: thêm vào `AGENTS.md` `## Rules` một dòng "ghi nhật ký kiểm toán chỉ qua `NhatKyService.ghi`, chi tiết là record cài `ChiTietNhatKy`, loại mới là một giá trị `LoaiThaoTac` có tiền tố phân hệ".
- [ ] Spec 0002 Follow-up "Tính năng 8: chỉnh `nhat_ky_he_thong`", spec 0004 Follow-up "ghi nhật ký đăng nhập" và spec 0005 Follow-up "ghi nhật ký đăng xuất và dùng lại token" được đóng bởi spec này khi tính năng 8 xong.
