# 0013. Tạm dừng quyền tiện ích căn hộ: bật tắt tay kèm lý do, lưu lần đổi gần nhất trên căn, lịch sử ở nhật ký

**Date**: 2026-11-26
**Status**: In Progress

## Summary

Spec này chốt API-UC01-09: BQL hoặc Kế toán tạm dừng hay mở khóa quyền tiện ích của một căn hộ bằng tay, lần nào cũng phải ghi lý do. Chưa có phân hệ công nợ (UC04), nên cờ chưa tự bật theo nợ; service được viết để UC04 gọi lại sau. Lý do, thời điểm và người đổi của lần gần nhất nằm ngay trên dòng `can_ho` (ba cột mới ở `V17`), để trang chi tiết và phân hệ tiện ích (UC09) đọc thẳng; lịch sử đầy đủ nằm ở nhật ký kiểm toán. Cư dân thấy trạng thái và lý do nhưng không thấy tên nhân viên đã đổi.

## Requirements

**User stories**:
- Là BQL hoặc Kế toán, tôi muốn tạm dừng quyền tiện ích của một căn đang nợ phí quá hạn và ghi rõ lý do, để phân hệ tiện ích chặn đặt chỗ của căn đó.
- Là BQL hoặc Kế toán, tôi muốn mở khóa lại khi căn đã thanh toán, kèm lý do, để mọi lần đổi đều giải thích được.
- Là Kế toán, tôi muốn lọc danh sách căn đang bị tạm dừng, để đối chiếu với công nợ.
- Là BQL, BQT hoặc Kế toán, tôi muốn thấy trên trang chi tiết căn ai đã đổi, lúc nào, vì sao.
- Là cư dân, tôi muốn biết căn của mình có đang bị tạm dừng tiện ích không và vì sao.

**Thuật ngữ dùng trong spec**:
- *Vai trò đổi tiện ích*: `BAN_QUAN_LY`, `KE_TOAN`.
- *Vai trò nhân viên đọc căn hộ*, *phạm vi tòa*, *phạm vi căn*, *chuẩn hóa mã*: như spec 0012.
- *Lần đổi tiện ích*: lần gần nhất cờ `bi_tam_dung_tien_ich` đổi giá trị qua API này; gồm lý do, thời điểm, người đổi. Vì yêu cầu không đổi bị từ chối (AC-5), chiều của lần đổi gần nhất luôn suy ra được từ cờ hiện tại: cờ bật thì đó là lần tạm dừng, cờ tắt thì đó là lần mở khóa.
- *Lý do hợp lệ*: chuỗi JSON, sau khi đổi về NFC và cắt khoảng trắng hai đầu thì không rỗng và dài tối đa 500 ký tự tính theo code point (`String.codePointCount`, khớp `char_length` của PostgreSQL). Giá trị lưu là chuỗi đã cắt.

**Acceptance criteria**

*Tạm dừng và mở khóa (API-UC01-09)*

- **AC-1**: `PATCH /api/v1/apartments/{apartmentId}/facility-suspension` với body `{ biTamDungTienIch, lyDo }` (bắt buộc cả hai), *vai trò đổi tiện ích*. `apartmentId` được chuẩn hóa mã. `biTamDungTienIch: true` là tạm dừng, `false` là mở khóa. Thành công trả 200, `message` "Tạm dừng quyền tiện ích căn hộ thành công" hoặc "Mở khóa quyền tiện ích căn hộ thành công" theo chiều, `data` là `CanHoResponse` sau khi đổi (đủ `lanDoiTienIch` với người đổi, AC-8). Trường khác trong body bị bỏ qua.
- **AC-2**: Trong một transaction: đọc cờ hiện tại bằng `SELECT ... FOR UPDATE`; nếu khác giá trị yêu cầu thì một lệnh `UPDATE` đặt `bi_tam_dung_tien_ich` theo yêu cầu, `tien_ich_ly_do` là *lý do hợp lệ* đã cắt, `tien_ich_doi_luc` là `Instant.now(clock).truncatedTo(ChronoUnit.MICROS)` (bean `Clock`, cắt về micro giây như `NhatKyService` để khớp độ chính xác của `TIMESTAMPTZ`), `tien_ich_doi_boi` là `PhamViTruyCap.maNguoiDung()`. Các cột khác của căn không đổi.
- **AC-3**: Dữ liệu sai trả 400 `VALIDATION_FAILED` "Dữ liệu không hợp lệ", một mục `errors` mỗi trường sai, thứ tự `biTamDungTienIch`, `lyDo`:
  - `biTamDungTienIch` vắng, `null` hoặc không phải boolean JSON (`isBoolean()`; chuỗi `"true"`, số `1` đều sai): "Vui lòng chọn tạm dừng hoặc mở khóa"
  - `lyDo` vắng, `null`, không phải chuỗi JSON (`isTextual()`), hoặc rỗng sau khi cắt: "Vui lòng nhập lý do"; dài hơn 500 ký tự sau khi cắt: "Lý do tối đa 500 ký tự"
  - Hai trường đọc thành `JsonNode` và kiểm ở service, như spec 0012. Body vắng hoặc không phải JSON đối tượng trả 400 `MALFORMED_REQUEST` như hiện có.
- **AC-4**: Thứ tự kiểm: vai trò 403, rồi body 400, rồi phạm vi 404, rồi trạng thái 409. Căn ngoài *phạm vi tòa* của người gọi (kể cả không tồn tại, mã sai định dạng) trả 404 `NOT_FOUND` "Không tìm thấy dữ liệu yêu cầu". Nếu khóa dòng không thấy dòng thì cũng 404.
- **AC-5**: Yêu cầu trùng trạng thái hiện có trả 409 `CANHO_TIEN_ICH_KHONG_DOI`: tạm dừng căn đang bị tạm dừng có `message` "Căn hộ đang bị tạm dừng tiện ích"; mở khóa căn đang bình thường có `message` "Căn hộ đang dùng tiện ích bình thường". Không có mục `errors`. Không `UPDATE`, không ghi nhật ký, lý do cũ trên căn giữ nguyên.
- **AC-6**: Hai yêu cầu cùng chiều cho cùng một căn chạy song song cho đúng một 200 và một 409 `CANHO_TIEN_ICH_KHONG_DOI`, và đúng một dòng nhật ký. Hai yêu cầu ngược chiều chạy song song được xử lý lần lượt theo khóa dòng; kết quả cuối và nhật ký khớp với thứ tự đó (mỗi dòng nhật ký là một lần cờ thật sự đổi).

*Nhật ký kiểm toán*

- **AC-7**: Mỗi lần đổi thành công ghi đúng một dòng: `CANHO_TAM_DUNG_TIEN_ICH` ("Tạm dừng tiện ích căn hộ") khi bật, `CANHO_MO_KHOA_TIEN_ICH` ("Mở khóa tiện ích căn hộ") khi tắt; đối tượng `CanHo:<maCanHo>`; chi tiết `{ "lyDo": "<lý do đã cắt>" }`. Ghi qua `NhatKyService.ghi` (tự chờ sau commit), người thực hiện là `PhamViTruyCap.maNguoiDung()`. Hai loại mới hiện trong ô lọc loại thao tác của màn hình tra cứu nhật ký (spec 0007) mà không cần sửa giao diện đó.

*Hiển thị lần đổi*

- **AC-8**: `CanHoResponse` thêm trường `lanDoiTienIch` (luôn có trong JSON, giá trị `null` khi căn chưa từng đổi), là object `{ lyDo, doiLuc, maNguoiDoi, hoTenNguoiDoi }`: `lyDo` và `doiLuc` (thời điểm theo quy ước ngày giờ spec 0001) luôn có; `maNguoiDoi` từ cột `tien_ich_doi_boi`; `hoTenNguoiDoi` là họ tên hiện tại lấy qua `NguoiDungTraCuuService.hoTenTheoMa` (một truy vấn cho cả trang danh sách, một cho chi tiết hay response của PATCH), `null` nếu mã không có trong map trả về (dùng `get`, không giả định có khóa). Áp dụng cho `GET /apartments`, `GET /apartments/{id}`, response của PATCH, và cả response của `POST /apartments` và `PUT /apartments/{id}` (spec 0012) vì chúng cùng trả `CanHoQuery.cua(ma)`; người gọi hai endpoint này luôn là BQL nên không ẩn người đổi. `doiLuc` trong mọi response là giá trị đọc lại từ DB.
- **AC-9**: Ở `GET /apartments/{id}`, nếu người gọi xem được căn chỉ nhờ điều kiện cư dân của spec 0012 AC-6 (không đạt điều kiện nhân viên của tòa), thì `maNguoiDoi` và `hoTenNguoiDoi` là `null`; `lyDo`, `doiLuc` và `biTamDungTienIch` vẫn trả đủ. Người vừa là nhân viên của tòa vừa là cư dân của căn thấy đủ. Khi đó không gọi `hoTenTheoMa`.

*Dữ liệu*

- **AC-10**: Migration `V17__tam_dung_tien_ich.sql` thêm ba cột và ràng buộc ở *Data model sketch*, để DB tự chặn dữ liệu sai kể cả khi sửa tay: ba cột cùng `NULL` hoặc cùng có giá trị; cờ bật thì ba cột phải có giá trị; lý do không rỗng sau `btrim` và dài tối đa 500; `tien_ich_doi_boi` là khóa ngoại tới `nguoi_dung_he_thong`. Trước khi thêm ràng buộc, V17 chạy `UPDATE can_ho SET bi_tam_dung_tien_ich = FALSE WHERE bi_tam_dung_tien_ich`: trước spec này không có đường ghi cờ nào, nên dòng có cờ bật chỉ có thể do sửa tay trên DB dev, và không có lý do để thỏa CHECK mới. Sau lệnh đó mọi dòng (cờ false, ba cột `NULL`) thỏa ràng buộc.
- **AC-11**: `GET /apartments` nhận thêm tham số tùy chọn `biTamDungTienIch`: sau khi cắt khoảng trắng, `true` chỉ trả căn đang bị tạm dừng, `false` chỉ trả căn bình thường, rỗng tính như vắng (không lọc). Giá trị khác (kể cả `TRUE`, `1`) trả 400 `VALIDATION_FAILED`, mục `biTamDungTienIch` "Giá trị lọc tiện ích không hợp lệ", xếp sau mục `q` trong thứ tự lỗi của spec 0012 AC-3. Kết hợp AND với các bộ lọc khác.
- **AC-12**: Seed `R__seed_03_can_ho.sql` thêm sau lệnh `INSERT` hiện có một lệnh `UPDATE` đặt căn `A-0204` thành tạm dừng: lý do "Nợ phí dịch vụ quá hạn 2 kỳ (dữ liệu mẫu)", `tien_ich_doi_luc` `2026-11-01 09:00:00+07`, `tien_ich_doi_boi` là mã người dùng có số điện thoại `0900000002` (BQL seed), chỉ khi `tien_ich_doi_luc IS NULL` để chạy lại không đổi gì. Bảng `INSERT` giữ nguyên. Các căn khác vẫn bình thường.

*Phân quyền*

- **AC-13**: Handler PATCH mang `@ChoPhep({ BAN_QUAN_LY, KE_TOAN })`; `BAN_QUAN_TRI`, `KY_THUAT`, `CU_DAN`, `BAO_VE`, `QUAN_TRI_VIEN` gọi nhận 403 `FORBIDDEN`, chưa đăng nhập nhận 401. Quyền đọc của các endpoint căn hộ không đổi. `PhanQuyenCoverageTest` xanh.

*Giao diện*

- **AC-14**: Trang chi tiết dùng chung của spec 0012 AC-19 giữ dòng "Trạng thái tiện ích" và, khi `lanDoiTienIch` khác `null`, thêm hai dòng: "Lý do tạm dừng" (khi cờ bật) hoặc "Lý do mở khóa" (khi cờ tắt) hiện `lyDo`; "Cập nhật lúc" hiện `formatNgayGio(doiLuc)`, nối thêm " bởi <hoTenNguoiDoi> (<maNguoiDoi>)" khi `maNguoiDoi` có (khi `hoTenNguoiDoi` `null` thì chỉ hiện mã). Lý do hiện nguyên văn, giữ xuống dòng, không render HTML.
- **AC-15**: Trên `/can-ho/<maCanHo>` (không phải trang cư dân), người có *vai trò đổi tiện ích* thấy nút "Tạm dừng tiện ích" (khi cờ tắt) hoặc "Mở khóa tiện ích" (khi cờ bật) cạnh nút "Sửa". Nút mở dialog tiêu đề "Tạm dừng tiện ích căn hộ <maCanHo>" hoặc "Mở khóa tiện ích căn hộ <maCanHo>", có một ô nhiều dòng có nhãn "Lý do" (bắt buộc, đếm ký tự còn lại trên 500, đọc được bởi trình đọc màn hình). Gửi `{ biTamDungTienIch: !hiện tại, lyDo }`. Thành công: toast "Đã tạm dừng tiện ích căn hộ <maCanHo>" hoặc "Đã mở khóa tiện ích căn hộ <maCanHo>", đóng dialog, invalidate `CAN_HO_QUERY_KEY`. Lỗi 400 hiện dưới ô lý do; 409 hiện `message` trong dialog và invalidate `CAN_HO_QUERY_KEY` để nút đổi theo trạng thái mới; 404 hiện thông báo lỗi trong dialog. Không có xác nhận thứ hai (ô lý do bắt buộc đã đủ là bước cân nhắc).
- **AC-16**: Thanh lọc `/can-ho` thêm ô chọn có nhãn "Tiện ích" với ba lựa chọn "Tất cả" (mặc định, không ghi lên URL), "Đang tạm dừng" (`biTamDungTienIch=true`), "Bình thường" (`false`), giữ trên URL đúng tên tham số API, đổi thì về trang 1 như các bộ lọc khác. Giá trị lạ trên URL thì giao diện bỏ trước khi gọi API. Nhãn "Tạm dừng tiện ích" trên dòng bảng giữ như spec 0012.
- **AC-17**: Nút, dialog, ô lý do và ô lọc mới dùng được hoàn toàn bằng bàn phím, focus nhìn thấy được, mọi ô có nhãn, lỗi gắn với ô (`aria-describedby`), focus trở về nút mở sau khi đóng dialog, đạt WCAG AA.

## Decision

**Chosen option**: Option 2: Bật tắt tay qua một PATCH có lý do bắt buộc, lần đổi gần nhất lưu trên `can_ho`, lịch sử ở nhật ký kiểm toán.

Cờ chỉ đổi khi người có quyền yêu cầu một trạng thái khác trạng thái hiện có, dưới khóa dòng; mỗi lần đổi để lại lý do trên căn và một dòng nhật ký.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`, riêng lỗi theo vỏ bọc spec 0001) · `ui-styling` (`.claude/skills/ui-styling/`, dialog, textarea, select shadcn/ui)

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

### Data model sketch

`V17__tam_dung_tien_ich.sql` chỉ thêm cột vào `can_ho`, không thêm bảng:

| Cột | Kiểu | Null | Ghi chú |
|---|---|---|---|
| `bi_tam_dung_tien_ich` | BOOLEAN | NOT NULL, mặc định false | có từ V2, giữ nguyên |
| `tien_ich_ly_do` | VARCHAR(500) COLLATE `"vi-VN-x-icu"` | NULL | lý do của *lần đổi tiện ích* |
| `tien_ich_doi_luc` | TIMESTAMPTZ | NULL | thời điểm của lần đổi |
| `tien_ich_doi_boi` | VARCHAR(30) | NULL | người đổi; FK `nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT` |

Ràng buộc và index (tên theo quy ước spec 0002):

```sql
-- Dữ liệu sửa tay trên DB dev (không có lý do) sẽ vi phạm CHECK mới (AC-10).
UPDATE can_ho SET bi_tam_dung_tien_ich = FALSE WHERE bi_tam_dung_tien_ich;

ALTER TABLE can_ho
    ADD COLUMN tien_ich_ly_do VARCHAR(500) COLLATE "vi-VN-x-icu" NULL,
    ADD COLUMN tien_ich_doi_luc TIMESTAMPTZ NULL,
    ADD COLUMN tien_ich_doi_boi VARCHAR(30) NULL,
    ADD CONSTRAINT fk_can_ho_tien_ich_doi_boi FOREIGN KEY (tien_ich_doi_boi)
        REFERENCES nguoi_dung_he_thong (ma_nguoi_dung) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_can_ho_tien_ich_dong_bo CHECK (
        (tien_ich_ly_do IS NULL) = (tien_ich_doi_luc IS NULL)
        AND (tien_ich_doi_luc IS NULL) = (tien_ich_doi_boi IS NULL)),
    ADD CONSTRAINT chk_can_ho_tien_ich_tam_dung CHECK (
        NOT bi_tam_dung_tien_ich OR tien_ich_doi_luc IS NOT NULL),
    ADD CONSTRAINT chk_can_ho_tien_ich_ly_do CHECK (
        tien_ich_ly_do IS NULL OR (btrim(tien_ich_ly_do) <> '' AND char_length(tien_ich_ly_do) <= 500));

CREATE INDEX idx_can_ho_tien_ich_doi_boi ON can_ho (tien_ich_doi_boi);
CREATE INDEX idx_can_ho_tam_dung_tien_ich ON can_ho (ma_can_ho) WHERE bi_tam_dung_tien_ich;
```

Quan hệ: `nguoi_dung_he_thong` 1 : N `can_ho` qua `tien_ich_doi_boi`. Không có bảng lịch sử; lịch sử là các dòng nhật ký `CANHO_TAM_DUNG_TIEN_ICH` và `CANHO_MO_KHOA_TIEN_ICH` với đối tượng `CanHo:<ma>`.

**Không ghi bằng JPA**, như spec 0012: dùng `JdbcClient` trong `CanHoGhi`.

### State transitions

```
Bình thường (false) ──PATCH true + lý do──▶ Tạm dừng (true)
Tạm dừng (true)     ──PATCH false + lý do─▶ Bình thường (false)
cùng trạng thái     ──PATCH───────────────▶ 409 CANHO_TIEN_ICH_KHONG_DOI, không đổi gì
```

Căn mới luôn bắt đầu ở Bình thường với `lanDoiTienIch` `null`. Ai trong *vai trò đổi tiện ích* cũng làm được cả hai chiều.

### Backend: lớp và trách nhiệm (package `canho`)

- `CanHoController`: thêm `doiTienIch` (`PATCH /{apartmentId}/facility-suspension`, `@ChoPhep({ BAN_QUAN_LY, KE_TOAN })`), lấy phạm vi qua `PhamViTruyCapService.cuaNguoiDangGoi()`, chọn `message` theo `biTamDungTienIch` của response. Thêm `@RequestParam(required = false) String biTamDungTienIch` cho `timKiem` (OpenAPI `@Schema(type = "boolean")`).
- `TamDungTienIchService` (mới, `@Transactional`, inject `CanHoQuery`, `CanHoGhi`, `NhatKyService`, `NguoiDoiTienIch`, `Clock`): `doi(PhamViTruyCap, apartmentId, DoiTienIchRequest)`: kiểm body (AC-3), kiểm phạm vi bằng `CanHoQuery.maToaNhaCua` như `SuaCanHoService` (AC-4), `CanHoGhi.khoaCoTienIch(ma)` dưới `FOR UPDATE`, so cờ (AC-5), `CanHoGhi.capNhatTienIch(...)`, ghi nhật ký (AC-7), rồi trả `CanHoQuery.cua(ma)` đã qua `NguoiDoiTienIch.ganHoTen` (AC-8). Đây là điểm vào duy nhất để đổi cờ; UC04 sau này gọi cùng hàm này (xem Follow-up).
- `NhapLieuTienIch` (mới, hàm tĩnh): `coTamDung(JsonNode, List<FieldError>)`, `lyDo(JsonNode, List<FieldError>)` (NFC, `strip()`, đếm code point), `boLoc(String, List<FieldError>)` cho AC-11. Tách riêng để `NhapLieuCanHo` không vượt khoảng 200 dòng.
- `NguoiDoiTienIch` (mới, `@Component`, inject `NguoiDungTraCuuService`): `ganHoTen(List<CanHoResponse>)` gom các `maNguoiDoi` khác `null`, gọi `hoTenTheoMa` một lần, trả danh sách mới có `hoTenNguoiDoi`; `anNguoiDoi(CanHoResponse)` trả bản có `maNguoiDoi`, `hoTenNguoiDoi` `null` (AC-9).
- `TimCanHoService`: `timKiem` đưa kết quả trang qua `ganHoTen`; `chiTiet` qua `anNguoiDoi` khi chỉ đạt điều kiện cư dân, ngược lại qua `ganHoTen`. `kiem` thêm `biTamDungTienIch` vào lỗi sau `q` và vào `BoLocDaKiem`.
- `SuaCanHoService`, `ThemCanHoService`: inject thêm `NguoiDoiTienIch`, đưa kết quả `CanHoQuery.cua(ma)` qua `ganHoTen` trước khi trả (AC-8).
- `hoTenTheoMa` là `@Transactional(readOnly = true)`; gọi trong transaction ghi của PATCH, PUT, POST thì nó nhập vào transaction đang có, không sao.
- `CanHoQuery`: `COT` thêm `c.tien_ich_ly_do, c.tien_ich_doi_luc, c.tien_ich_doi_boi`; `dong` dựng `LanDoiTienIchResponse` khi `tien_ich_doi_luc` khác `null`, đọc thời điểm bằng `rs.getObject("tien_ich_doi_luc", OffsetDateTime.class).toInstant()` (họ tên `null`, để `NguoiDoiTienIch` điền); `DieuKien` thêm `Boolean biTamDungTienIch`, `where` thêm `AND c.bi_tam_dung_tien_ich = :tamDung` khi khác `null`.
- `CanHoGhi`: `khoaCoTienIch(ma)` (`SELECT bi_tam_dung_tien_ich FROM can_ho WHERE ma_can_ho = :ma FOR UPDATE`, trả `Optional<Boolean>`); `capNhatTienIch(ma, boolean tamDung, String lyDo, Instant luc, String maNguoiDoi)` (một `UPDATE` bốn cột). `luc` truyền dạng `OffsetDateTime` UTC (`luc.atOffset(ZoneOffset.UTC)`).
- `CanHoErrorCodes`: thêm `CANHO_TIEN_ICH_KHONG_DOI`. `CanHoLoi`: thêm `tienIchKhongDoi(boolean dangTamDung)` (409, chọn `message` theo AC-5).
- `LoaiThaoTac`: thêm `CANHO_TAM_DUNG_TIEN_ICH("Tạm dừng tiện ích căn hộ")`, `CANHO_MO_KHOA_TIEN_ICH("Mở khóa tiện ích căn hộ")`. `ChiTietCanHo`: thêm record `TienIch(String lyDo)`.
- DTO mới: `DoiTienIchRequest(JsonNode biTamDungTienIch, JsonNode lyDo)` (OpenAPI `@Schema(type = "boolean")` và `@Schema(type = "string", maxLength = 500)`, cả hai `requiredMode = REQUIRED`); `LanDoiTienIchResponse(String lyDo, Instant doiLuc, String maNguoiDoi, String hoTenNguoiDoi)` (hai trường người đổi `@Schema(nullable = true)`, mọi trường `requiredMode = REQUIRED`). `CanHoResponse` thêm `@Schema(requiredMode = REQUIRED, nullable = true) LanDoiTienIchResponse lanDoiTienIch` ở cuối. `BoLocCanHo` thêm `String biTamDungTienIch` ở cuối (controller truyền theo cùng thứ tự). Sau khi sinh lại, kiểm bằng mắt `openapi.json` và `frontend/src/lib/api/schema.d.ts`: query `biTamDungTienIch` là `boolean`, `lanDoiTienIch` là `LanDoiTienIchResponse | null` (không tùy chọn), `maNguoiDoi`, `hoTenNguoiDoi` là `string | null`. Nếu springdoc không sinh dạng nullable cho `$ref`, sửa annotation tới khi đúng, không sửa tay `schema.d.ts`.

### Frontend: file thay đổi

- `frontend/src/lib/can-ho.ts`: hàm `duocDoiTienIch(vaiTro)` (có `BAN_QUAN_LY` hoặc `KE_TOAN`), kiểu `LanDoiTienIch` lấy từ schema.
- `frontend/src/app/(app)/can-ho/chi-tiet-can-ho.tsx`: hai dòng mới trong `thongTin` (AC-14), nút đổi tiện ích cạnh nút "Sửa" khi `choPhepSua` và `duocDoiTienIch`.
- `frontend/src/app/(app)/can-ho/doi-tien-ich-dialog.tsx` (mới): dialog AC-15, theo mẫu `sua-can-ho-dialog.tsx` (`api`, `ApiError` theo `errorCode`, toast, invalidate `CAN_HO_QUERY_KEY`).
- `frontend/src/app/(app)/can-ho/bo-loc-can-ho.tsx` và `can-ho-view.tsx`: ô lọc "Tiện ích" và đọc ghi tham số trên URL (AC-16).
- Test cùng thư mục: `chi-tiet-can-ho.test.tsx`, `can-ho-view.test.tsx`, `doi-tien-ich-dialog.test.tsx` (mới); các fixture `CanHo` thêm `lanDoiTienIch: null`.
- Không đổi `frontend/src/lib/phan-quyen/muc-chuc-nang.ts`: không có trang mới.

### API surface

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/apartments/{apartmentId}/facility-suspension` (UC01-09) | PATCH | `apartmentId`; `biTamDungTienIch`: boolean (req); `lyDo`: string 1..500 (req) | 200, `CanHoResponse` | `@ChoPhep(BAN_QUAN_LY, KE_TOAN)` | 400 `VALIDATION_FAILED`, 404 `NOT_FOUND`, 409 `CANHO_TIEN_ICH_KHONG_DOI` |
| `/api/v1/apartments` (UC01-05, mở rộng) | GET | thêm `biTamDungTienIch`: `true` hoặc `false` (opt) | 200, `PageResponse<CanHoResponse>` có `lanDoiTienIch` | như spec 0012 | 400 mục `biTamDungTienIch` |
| `/api/v1/apartments/{apartmentId}` (UC01-06, mở rộng) | GET | như spec 0012 | 200, `CanHoResponse` có `lanDoiTienIch` (ẩn người đổi với cư dân) | như spec 0012 | 404 |

Response mẫu của PATCH:

```json
{
  "success": true, "code": 200,
  "message": "Tạm dừng quyền tiện ích căn hộ thành công",
  "data": {
    "maCanHo": "A-0204", "maTang": "A-02", "soTang": 2, "maToaNha": "A", "tenToaNha": "Tòa A",
    "dienTich": 95.75, "trangThaiSuDung": "Đang sử dụng", "biTamDungTienIch": true,
    "lanDoiTienIch": { "lyDo": "Nợ phí quý 3", "doiLuc": "2026-11-26T03:00:00Z",
                       "maNguoiDoi": "ND00000123", "hoTenNguoiDoi": "Phạm Kế Toán" }
  }
}
```

Mã và tên trong ví dụ chỉ để minh họa; test lấy mã người dùng seed bằng truy vấn theo số điện thoại, không ghi cứng. Body mẫu trong file Excel (chép từ API sửa căn) và `data` dạng `maDinhDanh` không dùng; xem Follow-up.

### Value sourcing

| Action | Value produced / displayed | Source |
|---|---|---|
| Đổi tiện ích | giá trị cờ mới | body `biTamDungTienIch` |
| Đổi tiện ích | cờ hiện tại để so (AC-5) | `CanHoGhi.khoaCoTienIch(ma)`, đọc dưới `FOR UPDATE` |
| Đổi tiện ích | `tien_ich_ly_do` | body `lyDo` sau NFC và `strip()` |
| Đổi tiện ích | `tien_ich_doi_luc` | `Instant.now(clock)` trong `TamDungTienIchService` |
| Đổi tiện ích | `tien_ich_doi_boi`, người thực hiện nhật ký | `PhamViTruyCap.maNguoiDung()` (claim `sub`) |
| Đổi tiện ích | tòa của căn để kiểm phạm vi | `CanHoQuery.maToaNhaCua(ma)` so với `PhamViTruyCap.maToaNha()` |
| Đổi tiện ích | `message` 200 và 409 | chiều yêu cầu (200) hoặc cờ hiện tại (409), hằng trong controller và `CanHoLoi` |
| Nhật ký | loại thao tác | `biTamDungTienIch` mới: true thành `CANHO_TAM_DUNG_TIEN_ICH`, false thành `CANHO_MO_KHOA_TIEN_ICH` |
| Nhật ký | thời điểm | `NhatKyService.ghi` lấy từ bean `Clock` (spec 0007) |
| Mọi response căn | `lanDoiTienIch.lyDo`, `doiLuc`, `maNguoiDoi` | cột `tien_ich_ly_do`, `tien_ich_doi_luc`, `tien_ich_doi_boi` |
| Mọi response căn | `lanDoiTienIch.hoTenNguoiDoi` | `NguoiDungTraCuuService.hoTenTheoMa`, họ tên hiện tại (cùng cách nhật ký, spec 0007 AC-13) |
| Xem chi tiết | ẩn người đổi hay không | điều kiện nhân viên của tòa trong `TimCanHoService.chiTiet` (spec 0012 AC-6) |
| Tìm căn | lọc cờ | query `biTamDungTienIch` qua `NhapLieuTienIch.boLoc` |
| Seed | căn bị tạm dừng mẫu, người đổi | `A-0204`; `SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000002'` |
| UI chi tiết | "Lý do tạm dừng" hay "Lý do mở khóa" | `biTamDungTienIch` hiện tại (xem *Lần đổi tiện ích*) |
| UI chi tiết | "Cập nhật lúc" | `formatNgayGio(lanDoiTienIch.doiLuc)` trong `lib/format.ts`, giờ Việt Nam |
| UI nút | hiện nút và nhãn nút | `useHoSo().data?.vaiTro` có `BAN_QUAN_LY` hoặc `KE_TOAN` (hàm mới `duocDoiTienIch` trong `lib/can-ho.ts`), cờ hiện tại, và prop `choPhepSua` của trang nhân viên |
| UI dialog | số ký tự còn lại | `500 - [...lyDo.trim()].length` (đếm code point như server) |

### Key invariants

- Ba cột `tien_ich_*` cùng `NULL` hoặc cùng có giá trị; cờ bật thì có giá trị (CHECK `chk_can_ho_tien_ich_dong_bo`, `chk_can_ho_tien_ich_tam_dung`).
- Mỗi căn bị tạm dừng luôn có lý do, thời điểm và người tạm dừng.
- Mỗi dòng nhật ký tiện ích ứng với đúng một lần cờ thật sự đổi giá trị; yêu cầu không đổi không ghi gì (AC-5, AC-6).
- Chiều của *lần đổi tiện ích* luôn bằng cờ hiện tại, vì chỉ API này ghi các cột `tien_ich_*` và nó từ chối yêu cầu không đổi.
- `TamDungTienIchService.doi` là đường duy nhất ghi `bi_tam_dung_tien_ich` và ba cột mới (ngoài seed). `SuaCanHoService` và `ThemCanHoService` không chạm các cột này.
- Mọi đọc ghi đi qua phạm vi tòa của người gọi; ngoài phạm vi là 404.

### Security model

- Đổi tiện ích: `BAN_QUAN_LY`, `KE_TOAN`, giới hạn theo phạm vi tòa. Không ai khác, kể cả `BAN_QUAN_TRI` (chỉ đọc).
- Đọc trạng thái, lý do, thời điểm: mọi ai đang đọc được căn theo spec 0012.
- Người đổi (`maNguoiDoi`, `hoTenNguoiDoi`): chỉ nhân viên đọc căn hộ của tòa; cư dân không thấy, để không lộ tên nhân viên với cư dân.
- Lý do là chữ do nhân viên nhập và cư dân đọc được: giao diện hiện như văn bản thường (React tự thoát), không render HTML hay Markdown. Ghi chú nội bộ không nên viết vào lý do (dialog có câu gợi ý "Cư dân của căn sẽ thấy lý do này").
- Không có dữ liệu cá nhân mới ngoài họ tên nhân viên (đã hiện ở nhật ký cho nhân viên).
- Nhật ký kiểm toán cho mọi lần đổi (AC-7).

### Configuration required

Không có biến môi trường hay khóa bí mật mới.

### Critical test scenarios

- Happy path (integration): Kế toán tạm dừng `A-0101` với lý do " Nợ phí quý 3 " nhận 200, `biTamDungTienIch` true, `lanDoiTienIch.lyDo` "Nợ phí quý 3", `doiLuc` bằng giờ của `Clock` cố định trong test, `hoTenNguoiDoi` "Phạm Kế Toán"; BQL mở khóa với lý do "Đã thanh toán" nhận 200 và `lanDoiTienIch` mới; có đúng hai dòng nhật ký đúng loại và lý do, verifies **AC-1**, **AC-2**, **AC-7**, **AC-8**
- Failure case: tạm dừng căn đang tạm dừng nhận 409 `CANHO_TIEN_ICH_KHONG_DOI` "Căn hộ đang bị tạm dừng tiện ích", lý do cũ giữ nguyên, không thêm dòng nhật ký; mở khóa căn bình thường nhận 409 với `message` còn lại, verifies **AC-5**
- Failure case (song song, hai luồng và `CountDownLatch`): hai yêu cầu tạm dừng cùng căn cho đúng một 200, một 409 và một dòng nhật ký, verifies **AC-6**
- Failure case: `biTamDungTienIch` vắng, `null`, `"true"`, `1`; `lyDo` vắng, `null`, `"   "`, `5`, chuỗi 501 ký tự (và chuỗi 500 emoji hợp lệ vì đếm code point) trả 400 đúng mục và đúng thứ tự; body sai và căn ngoài phạm vi cùng lúc trả 400; `apartmentId` `ZZ-9999` trả 404; `UPDATE` tay đặt cờ true mà thiếu lý do, hay lý do `'  '`, hay chỉ có một trong ba cột bị CHECK từ chối, verifies **AC-3**, **AC-4**, **AC-10**
- Auth/permission: BQT, Kỹ thuật, Cư dân gọi PATCH nhận 403; chưa đăng nhập nhận 401; cư dân của `A-0101` xem căn đã bị tạm dừng nhận `lyDo` và `doiLuc` nhưng `maNguoiDoi`, `hoTenNguoiDoi` `null`; BQL xem cùng căn thấy đủ, verifies **AC-9**, **AC-13**
- Search (integration, seed): `biTamDungTienIch=true` trả đúng `A-0204` có `lanDoiTienIch` của BQL seed; `false` trả 11 căn còn lại; `biTamDungTienIch=TRUE` và `=1` trả 400 mục `biTamDungTienIch` sau mục `q`; seed chạy lại không đổi `tien_ich_doi_luc`, verifies **AC-11**, **AC-12**
- Unit (`TamDungTienIchService` với fake qua constructor): thứ tự kiểm 400, 404, 409; không gọi `capNhatTienIch` hay `nhatKy.ghi` khi 409; `NguoiDoiTienIch.ganHoTen` gọi `hoTenTheoMa` đúng một lần cho cả trang và không gọi khi không có mã, verifies **AC-4**, **AC-5**, **AC-8**
- UI (Vitest + Testing Library): Kế toán thấy nút "Tạm dừng tiện ích", BQT và trang cư dân không thấy; dialog chặn gửi khi lý do rỗng, hiện lỗi 400 dưới ô, hiện 409 trong dialog; chi tiết hiện "Lý do tạm dừng" hoặc "Lý do mở khóa" đúng chiều và không có tên người đổi khi `maNguoiDoi` `null`; ô lọc "Tiện ích" ghi và đọc `biTamDungTienIch` trên URL, verifies **AC-14**, **AC-15**, **AC-16**, **AC-17**

## Build plan

Thứ tự theo Tracer Bullet: lát đầu chạy thật từ DB tới nút trên trang chi tiết cho chiều tạm dừng, các lát sau làm dày thêm.

1. **Đường mỏng: tạm dừng một căn.** `V17__tam_dung_tien_ich.sql`; `CanHoGhi.khoaCoTienIch`, `capNhatTienIch`; `NhapLieuTienIch` (cờ, lý do); `TamDungTienIchService` với phạm vi, khóa dòng, 409 và nhật ký hai loại mới; `PATCH /facility-suspension`; `LanDoiTienIchResponse` và `CanHoQuery` đọc ba cột (chưa có họ tên); `pnpm gen:api`; nút và dialog trên `/can-ho/[maCanHo]`. Integration test happy path, 400, 404, 409, 403 và CHECK của V17. Satisfies **AC-1**, **AC-2**, **AC-3**, **AC-4**, **AC-5**, **AC-7**, **AC-10**, **AC-13**, **AC-15**, **AC-17**
2. **Hiện lần đổi và ẩn người đổi với cư dân.** `NguoiDoiTienIch` (`ganHoTen`, `anNguoiDoi`), nối vào `TimCanHoService.timKiem`, `chiTiet`, response PATCH, và response của `SuaCanHoService`, `ThemCanHoService`; test PUT và POST trả đủ họ tên người đổi; hai dòng mới trên trang chi tiết dùng chung. Unit test số lần gọi `hoTenTheoMa`, integration test cư dân và nhân viên. Satisfies **AC-8**, **AC-9**, **AC-14**, **AC-17**
3. **Bộ lọc và seed.** `BoLocCanHo`, `DieuKien`, `where` và thứ tự lỗi cho `biTamDungTienIch`; `UPDATE` trong `R__seed_03_can_ho.sql`; ô lọc "Tiện ích" trên `/can-ho`. Integration test lọc trên seed và chạy lại seed. Satisfies **AC-11**, **AC-12**, **AC-16**, **AC-17**
4. **Song song và hoàn tất.** Test hai yêu cầu song song; sửa các test dựng record bằng constructor theo vị trí: `CanHoResponse` (thêm `null` cho `lanDoiTienIch`), `BoLocCanHo`, `CanHoQuery.DieuKien`, `BoLocDaKiem` (thêm `null` cho bộ lọc mới), ít nhất ở `TimCanHoServiceTest`; sinh lại `openapi.json` rồi kiểm kiểu sinh ra như ở mục DTO, `pnpm gen:api`, `pnpm typecheck`, `pnpm lint`, `mvnw verify`; `PhanQuyenCoverageTest`. Satisfies **AC-6**, **AC-13**

## Consequences

**Positive**:
- UC09 chỉ cần đọc `bi_tam_dung_tien_ich` (và lý do để báo cho cư dân) từ căn, không phụ thuộc nhật ký hay công nợ.
- UC04 bật tự động sau này chỉ cần gọi `TamDungTienIchService.doi` với một người thực hiện, mọi luật (khóa dòng, 409, nhật ký) dùng lại.
- Nhân viên và cư dân thấy ngay vì sao một căn bị khóa, không phải mở màn hình nhật ký.

**Negative / tradeoffs**:
- Cờ bật tay nên có thể lệch với công nợ thật: căn đã trả hết mà chưa ai mở khóa vẫn bị chặn tiện ích, và ngược lại.
- Trên `can_ho` chỉ còn lần đổi gần nhất; muốn biết lần tạm dừng trước đó phải tra nhật ký kiểm toán.
- Bảng `can_ho` thêm ba cột chỉ phục vụ một tính năng, và một khóa ngoại từ `canho` sang bảng của `auth` ở mức DB (đọc họ tên vẫn qua service công khai).
- Cư dân đọc được lý do, nên nhân viên phải viết lý do như viết cho cư dân.
- Mọi test dựng `CanHoResponse`, `BoLocCanHo`, `CanHoQuery.DieuKien` bằng constructor phải sửa theo trường mới.
- Nhật ký ghi sau commit và lỗi ghi bị nuốt (spec 0007), nên mỗi dòng nhật ký chắc chắn ứng với một lần đổi thật, nhưng một lần đổi có thể thiếu dòng nhật ký nếu ghi nhật ký lỗi. Lý do trên căn vẫn còn trong trường hợp đó.
- Khóa ngoại `ON DELETE RESTRICT` chặn xóa tài khoản nhân viên đã từng đổi cờ; hiện không có API xóa tài khoản nên chấp nhận được. `SET NULL` không dùng được vì CHECK đòi ba cột đi cùng nhau.
- Căn seed `A-0204` bị tạm dừng mà không có dòng nhật ký tương ứng (chỉ là dữ liệu mẫu).

**Neutral**:
- `lanDoiTienIch` có trong cả danh sách dù bảng chưa hiện; tốn thêm một truy vấn họ tên mỗi trang.
- Seed có một căn bị tạm dừng (`A-0204`); test nào giả định mọi căn seed bình thường phải đổi theo.

## Follow-up

- [ ] UC04: tự tạm dừng khi căn nợ quá hạn và tự mở khóa khi đã thanh toán, gọi `TamDungTienIchService.doi`; cần quyết người thực hiện của thao tác tự động (một tài khoản hệ thống, hay để `tien_ich_doi_boi` nhận `NULL` và nới CHECK) và có cho phép mở khóa tay khi còn nợ không.
- [ ] UC09: chặn đặt chỗ (API-UC09-03) khi `bi_tam_dung_tien_ich` bật, trả 403 kèm lý do như dòng "Căn hộ nợ phí quá hạn bị tạm dừng đặt chỗ tiện ích" ở bảng mã lỗi; đọc cờ qua một hàm công khai của `canho`, không đọc bảng trực tiếp.
- [ ] UC10: thông báo cho cư dân khi căn bị tạm dừng hoặc mở khóa, khi đã có hồ sơ cư trú (UC02) để biết gửi cho ai.
- [ ] Đặc tả Excel của API-UC01-09 có body chép nhầm từ API sửa căn và `data` dạng `maDinhDanh`; cập nhật `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx` theo spec này nếu nhóm cần nộp đặc tả.
- [ ] Spec 0012 Follow-up "Tính năng 16 (tạm dừng tiện ích): ghi `bi_tam_dung_tien_ich`" được spec này giải quyết; đánh dấu khi tính năng xong.
