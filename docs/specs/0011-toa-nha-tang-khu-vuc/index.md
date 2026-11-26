# 0011. Tòa nhà, tầng và khu vực dùng chung: thêm tòa tự tạo đủ tầng, thêm tầng chỉ nối tiếp, khu vực chỉ xem từ seed

**Date**: 2026-11-23
**Status**: Accepted

## Summary

Spec này chốt phần đầu của UC01: BQL thêm tòa nhà và thêm tầng, BQL cùng Kỹ thuật xem tầng của một tòa và danh mục khu vực dùng chung. Khi thêm một tòa có N tầng, hệ thống tạo luôn N dòng tầng; thêm tầng thì chỉ được thêm đúng tầng kế tiếp (số tầng hiện có cộng một), và số tầng của tòa tăng theo trong cùng một transaction (một khối ghi DB hoặc xong hết, hoặc không có gì). Nhờ vậy số tầng của tòa luôn bằng số dòng tầng thật, và slice căn hộ sau có sẵn tầng để gắn căn. Cần một migration `V15` (CHECK định dạng mã, giới hạn 1 đến 99 tầng, tên tòa không trùng kể cả khác hoa thường), hai loại nhật ký mới, một endpoint xem mới ngoài danh mục 81 API (chi tiết tòa), trang chi tiết `/toa-nha/[maToaNha]` và trang `/khu-vuc-chung`.

## Requirements

**User stories**:
- Là BQL, tôi muốn thêm một tòa nhà mới với mã, tên, số tầng và địa chỉ, để tòa đó có ngay khung tầng cho việc nhập căn hộ.
- Là BQL, tôi muốn thêm một tầng mới lên trên cùng của tòa khi tòa được nâng tầng, mà không thể tạo tầng trùng hay tầng nhảy cóc.
- Là BQL hoặc Kỹ thuật, tôi muốn xem danh sách tầng của một tòa kèm số căn hộ mỗi tầng, để nắm cấu trúc không gian.
- Là BQL hoặc Kỹ thuật, tôi muốn xem danh mục khu vực dùng chung (sảnh, hành lang, phòng kỹ thuật), lọc theo tòa và tìm theo tên, để biết các vị trí có thiết bị dùng chung.
- Là Kỹ thuật, tôi muốn xem được danh mục tòa nhà, để có đường đi tới trang tầng.

**Thuật ngữ dùng trong spec**:
- *Phạm vi tòa của người gọi*: `PhamViTruyCap.maToaNha()` (spec 0006). Hiện mọi nhân viên phụ trách mọi tòa, nên tòa vừa thêm có trong phạm vi ngay ở request kế tiếp.
- *Chuẩn hóa chữ*: cắt khoảng trắng hai đầu, gộp khoảng trắng liên tiếp thành một, đổi về NFC. Kiểm rỗng và độ dài **sau** chuẩn hóa.
- *Tòa ngoài phạm vi*: mã tòa không có trong phạm vi tòa của người gọi, kể cả mã không tồn tại. Luôn trả 404 `NOT_FOUND` "Không tìm thấy dữ liệu yêu cầu" (`PhamViTruyCap.yeuCauToaNha`), không phân biệt hai trường hợp.

**Acceptance criteria**

*Thêm tòa nhà (API-UC01-02)*

- **AC-1**: `POST /api/v1/buildings` với body `{ maToaNha, tenToaNha, soTang, diaChiKhuVuc }`, chỉ `BAN_QUAN_LY`, tạo một dòng `toa_nha` và đúng `soTang` dòng `tang` (số tầng 1 đến `soTang`, mã theo AC-6) trong một transaction. Trả 201 `message` "Thêm mới tòa nhà thành công", `data` là `ToaNhaResponse` của tòa vừa tạo (`soCanHo` 0). Chuẩn hóa trước khi kiểm và lưu: `maToaNha` cắt khoảng trắng hai đầu rồi đổi chữ hoa; `tenToaNha` và `diaChiKhuVuc` theo *chuẩn hóa chữ*.
- **AC-2**: Dữ liệu sai trả 400 `VALIDATION_FAILED` `message` "Dữ liệu không hợp lệ", một mục `errors` cho mỗi trường sai:
  - `maToaNha` rỗng: "Vui lòng nhập mã tòa nhà"; không khớp `^[A-Z0-9]{1,10}$` sau chuẩn hóa: "Mã tòa nhà gồm 1 đến 10 chữ cái không dấu hoặc chữ số"
  - `tenToaNha` rỗng: "Vui lòng nhập tên tòa nhà"; quá 100 ký tự: "Tên tòa nhà tối đa 100 ký tự"
  - `soTang` thiếu, không phải số nguyên, hoặc ngoài 1 đến 99: "Số tầng phải là số nguyên từ 1 đến 99"
  - `diaChiKhuVuc` rỗng: "Vui lòng nhập địa chỉ khu vực"; quá 255 ký tự: "Địa chỉ khu vực tối đa 255 ký tự"
  - `soTang` được đọc thành `JsonNode` rồi kiểm ở service, để giá trị sai ra lỗi trường này thay vì 400 `MALFORMED_REQUEST`. Chỉ nhận số nguyên JSON (`isIntegralNumber()` và `canConvertToInt()`); `3.0`, `"3"`, `true`, `null`, mảng, đối tượng đều là lỗi trường `soTang`.
  - Trường chữ vắng hoặc `null` tính như rỗng. Body vắng hoặc không phải JSON đối tượng trả 400 `MALFORMED_REQUEST` như hiện có. Độ dài đếm theo code point sau chuẩn hóa NFC. `errors` theo thứ tự cố định `maToaNha`, `tenToaNha`, `soTang`, `diaChiKhuVuc`.
  - Có lỗi 400 thì trả 400, không kiểm trùng (AC-3) nữa.
- **AC-3**: Mã tòa đã có, hoặc tên tòa đã có (so sau chuẩn hóa, không phân biệt hoa thường theo `lower()` của PostgreSQL với collation `vi-VN-x-icu`, nên "Tòa A" và "TÒA A" là trùng), trả 409 `CANHO_TOA_NHA_DA_TON_TAI` `message` "Tòa nhà đã tồn tại", `errors` một mục cho mỗi trường trùng theo thứ tự `maToaNha` ("Mã tòa nhà đã được dùng"), `tenToaNha` ("Tên tòa nhà đã được dùng"). Service kiểm trước bằng một truy vấn (`ma_toa_nha = :ma` và `lower(ten_toa_nha) = lower(:ten)`), báo một mục cho mỗi trường trùng. Hai request cùng lúc vượt qua bước kiểm thì ràng buộc của DB chặn một bên: lệnh `INSERT INTO toa_nha` bằng `JdbcClient` nổ lỗi ngay tại lệnh (không chờ tới commit), service bắt `DataIntegrityViolationException`, lấy tên ràng buộc từ `org.hibernate.exception.ConstraintViolationException#getConstraintName()` hoặc `PSQLException.getServerErrorMessage().getConstraint()` trong chuỗi cause, và ném đúng response 409 này (một mục `errors`): `toa_nha_pkey` → `maToaNha`, `uq_toa_nha_ten_lower` → `tenToaNha`. Không đọc được tên, hoặc tên lạ, thì ném lại như cũ. Không có dòng `tang` nào sót lại khi tòa không tạo được (cùng transaction). Thêm tòa không cần khóa: khóa chính và unique index đủ chặn trùng. Response không có header `Location`.

*Thêm tầng (API-UC01-04)*

- **AC-4**: `POST /api/v1/buildings/{buildingId}/floors` với body `{ soTang }`, chỉ `BAN_QUAN_LY`. `buildingId` được đổi chữ hoa trước khi tra; mã sai định dạng cũng là tòa ngoài phạm vi (404), không phải 400. Tòa ngoài phạm vi trả 404. Trong một transaction: khóa dòng `toa_nha` (`SELECT ... FOR UPDATE`), kiểm `soTang` bằng `toa_nha.so_tang + 1`, thêm dòng `tang` (mã theo AC-6), tăng `toa_nha.so_tang` lên một. Trả 201 `message` "Thêm tầng mới vào tòa nhà thành công", `data` là `TangResponse` của tầng mới (`soCanHo` 0).
- **AC-5**: `soTang` thiếu, không phải số nguyên hoặc ngoài 1 đến 99 trả 400 `VALIDATION_FAILED`, mục `soTang` "Số tầng phải là số nguyên từ 1 đến 99". `soTang` hợp lệ nhưng khác `so_tang + 1` (trùng tầng đã có, hoặc nhảy cóc) trả 422 `CANHO_TANG_SAI_THU_TU` `message` "Tầng mới phải nối tiếp tầng cao nhất của tòa", `errors` mục `soTang` "Tầng tiếp theo của tòa này là tầng <so_tang + 1>". Tòa đã có 99 tầng trả 422 cùng mã với câu mục `soTang` "Tòa đã đạt tối đa 99 tầng". Thứ tự kiểm: `soTang` trong 1 đến 99 (400), rồi phạm vi (404), rồi dưới khóa: trần 99 tầng, rồi tầng kế tiếp; trần thắng khi cả hai cùng sai. Quy tắc đọc `soTang` như AC-2. Hai BQL cùng thêm tầng 4: người sau chờ khóa, đọc `so_tang` đã là 4 và nhận 422 này; không bao giờ có hai tầng mới hay 409 `CONFLICT` chung.

*Mã tầng và quan hệ phân cấp*

- **AC-6**: Mã tầng do server ghép, người dùng không nhập: `<maToaNha>-<soTang>` với số tầng đệm hai chữ số (`A-01`, `A-12`), khớp seed hiện có. Với giới hạn 1 đến 99 tầng, mã tầng luôn đúng hai chữ số và dài tối đa 13 ký tự (vừa `VARCHAR(20)`).
- **AC-7**: Sau mọi thao tác của spec này, mỗi tòa có đúng `so_tang` dòng `tang`, số tầng liên tục từ 1 tới `so_tang`. DB chặn mã sai định dạng và mã tầng không khớp tòa và số tầng của chính dòng đó (CHECK ở `V15`), nên dữ liệu chèn tay sai cũng bị từ chối.

*Xem tầng và chi tiết tòa*

- **AC-8**: `GET /api/v1/buildings/{buildingId}/floors?page&size&sort`, `BAN_QUAN_LY` và `KY_THUAT`, trả 200 `message` "Lấy danh sách tầng theo tòa nhà thành công", `data` là `PageResponse<TangResponse>` (`maTang`, `soTang`, `soCanHo`). `soCanHo` đếm `can_ho` của tầng lúc đọc. Sắp xếp được theo `soTang` hoặc `maTang` (mặc định `soTang,asc`), khóa phụ `ma_tang`. `page`, `size` (mặc định 20, tối đa 100) và `sort` theo `TrangYeuCau`; tham số sai hoặc trường sắp xếp lạ trả 400 (trước kiểm phạm vi), tòa ngoài phạm vi trả 404.
- **AC-9**: `GET /api/v1/buildings/{buildingId}`, `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`, `KY_THUAT`, trả 200 `message` "Lấy thông tin tòa nhà thành công", `data` là `ToaNhaResponse` (cùng record của danh mục: `maToaNha`, `tenToaNha`, `soTang`, `diaChiKhuVuc`, `soCanHo`). Tòa ngoài phạm vi (kể cả mã sai định dạng) trả 404.
- **AC-10**: `GET /api/v1/buildings` (API-UC01-01, đã có) mở thêm cho `KY_THUAT`. Mục `/toa-nha` của ma trận chức năng thêm `KY_THUAT`.

*Khu vực dùng chung (API-UC01-10)*

- **AC-11**: `GET /api/v1/common-areas?maToaNha&q&page&size&sort`, `BAN_QUAN_LY` và `KY_THUAT`, trả 200 `message` "Lấy danh mục khu vực dùng chung thành công", `data` là `PageResponse<KhuVucDungChungResponse>` (`maKhuVuc`, `tenKhuVuc`, `viTri`, `maToaNha`, `tenToaNha`). Chỉ khu vực thuộc tòa trong phạm vi người gọi. `maToaNha` tùy chọn (đổi chữ hoa; chuỗi rỗng coi như không gửi); có mà ngoài phạm vi thì 404. `q` tùy chọn, chuẩn hóa chữ, rỗng sau chuẩn hóa thì không lọc, tối đa 100 ký tự (quá thì 400 mục `q` "Từ khóa tối đa 100 ký tự"), tìm chứa trong `ten_khu_vuc` không phân biệt hoa thường (`ILIKE`, ký tự `%` `_` `\` của người dùng được thoát). Có chủ ý không bỏ dấu: "sanh" không khớp "Sảnh". Sắp xếp theo `tenKhuVuc`, `maKhuVuc` hoặc `tenToaNha` (mặc định `tenKhuVuc,asc`), khóa phụ `ma_khu_vuc`. Phạm vi rỗng trả trang rỗng.
- **AC-12**: File seed mới `db/seed/R__seed_02_khong_gian.sql` (chạy sau `R__seed_01_xac_thuc.sql`, nơi đã có tòa `A`) thêm ba khu vực cho tòa `A`: "Sảnh chính" (vị trí "Tầng 1"), "Hành lang tầng 2" ("Tầng 2"), "Phòng kỹ thuật điện nước" ("Tầng 1"). Mỗi lệnh `INSERT ... SELECT ... WHERE NOT EXISTS` theo (`ma_toa_nha`, `ten_khu_vuc`), mã để DB tự sinh, chạy lại không nhân đôi. Không có API thêm, sửa, xóa khu vực.

*Phân quyền, nhật ký*

- **AC-13**: Mỗi handler mang đúng một `@ChoPhep` theo bảng API surface; sai vai trò trả 403 `FORBIDDEN`, chưa đăng nhập trả 401, theo thứ tự kiểm của spec 0006 (vai trò, rồi tham số và body, rồi phạm vi 404, rồi nghiệp vụ 409 hoặc 422). `PhanQuyenCoverageTest` xanh.
- **AC-14**: Thêm tòa thành công ghi một dòng nhật ký `CANHO_THEM_TOA_NHA` ("Thêm tòa nhà"), đối tượng `ToaNha:<ma>`, chi tiết `{ soTang }`. Thêm tầng thành công ghi `CANHO_THEM_TANG` ("Thêm tầng"), đối tượng `Tang:<maTang>`, chi tiết `{ maToaNha, soTang }`. Mã và số ghi là giá trị đã chuẩn hóa. Ghi qua `NhatKyService.ghi` (tự chờ sau commit); lỗi 400, 404, 409, 422 không ghi dòng nào.

*Giao diện*

- **AC-15**: Trang `/toa-nha` có nút "Thêm tòa nhà" chỉ hiện khi người dùng có `BAN_QUAN_LY`. Nút mở dialog bốn ô có nhãn (Mã tòa nhà, Tên tòa nhà, Số tầng, Địa chỉ khu vực). Ô mã tự đổi chữ hoa khi gõ; ô số tầng là ô chữ `inputMode="numeric"` (không dùng `type="number"`), gửi số nguyên khi giá trị là chữ số, gửi nguyên chuỗi khi không phải để server trả lỗi trường. Lỗi 400 và 409 gắn vào đúng ô theo `errors[].field` (`aria-invalid`, `aria-describedby`), focus về ô sai đầu tiên. Thành công thì đóng dialog, báo toast "Đã thêm tòa nhà <tên>", làm mới danh mục (invalidate query `toa-nha`) và trả focus về nút.
- **AC-16**: Ở `/toa-nha`, tên tòa là link tới `/toa-nha/<maToaNha>` khi người dùng có `BAN_QUAN_LY` hoặc `KY_THUAT`; vai trò khác thấy tên dạng chữ thường. Trang chi tiết hiện tên, mã, địa chỉ, số tầng, số căn hộ (từ AC-9) và bảng tầng có phân trang (AC-8), `page`, `size`, `sort` nằm trên URL. Mở trực tiếp URL của tòa ngoài phạm vi (404) thì hiện trạng thái "Không tìm thấy tòa nhà" có link về danh mục; người không có quyền xem tầng (403 từ AC-8) thấy câu "Bạn không có quyền xem danh sách tầng" thay cho bảng tầng.
- **AC-17**: Trên trang chi tiết, nút "Thêm tầng" chỉ hiện cho `BAN_QUAN_LY`, mở dialog xác nhận "Thêm tầng <so_tang + 1> vào <tên tòa>?" (giá trị lấy từ dữ liệu AC-9 đang hiển thị). Xác nhận gửi `soTang` đó. Thành công thì toast "Đã thêm tầng <n>", làm mới chi tiết tòa, bảng tầng và danh mục tòa. Nhận 422 `CANHO_TANG_SAI_THU_TU` thì làm mới chi tiết tòa để số tầng mới hiện ra và báo: "Tòa đã đạt tối đa 99 tầng" khi số tầng mới tải về là 99, ngược lại "Tòa vừa được cập nhật, vui lòng thử lại". Tòa có 99 tầng thì nút bị vô hiệu kèm lời giải thích "Tòa đã đạt tối đa 99 tầng".
- **AC-18**: Trang `/khu-vuc-chung` (mục ma trận đã có, bật `sanSang`, thêm icon và mô tả "Sảnh, hành lang, phòng kỹ thuật của từng tòa") có combobox tòa có nhãn (mặc định "Tất cả tòa nhà", danh sách lấy từ `GET /api/v1/buildings?size=100`), ô tìm theo tên có nhãn, bảng mã, tên, vị trí, tòa có phân trang. `maToaNha`, `q`, `page`, `size`, `sort` nằm trên URL; đổi bộ lọc thì về trang 1; ô tìm gửi sau khi ngừng gõ 300 ms. Không có kết quả thì hiện trạng thái rỗng "Không có khu vực nào khớp bộ lọc". `maToaNha` trên URL trả 404 thì hiện "Không tìm thấy tòa nhà" kèm nút "Xóa bộ lọc" (bỏ `maToaNha` khỏi URL).
- **AC-19**: Mọi dialog, combobox, bảng mới dùng được hoàn toàn bằng bàn phím, focus nhìn thấy được, đạt WCAG AA như quy tắc chung của dự án.

## Decision

**Chosen option**: Option 2: Tòa và tầng luôn khớp nhau (tạo đủ tầng khi thêm tòa, thêm tầng chỉ nối tiếp dưới khóa dòng).

`toa_nha.so_tang` là số tầng thật của tòa và luôn bằng số dòng `tang`; service giữ quy tắc này bằng transaction và khóa dòng tòa, DB giữ định dạng mã và giới hạn bằng CHECK ở `V15`.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`, riêng lỗi theo vỏ bọc spec 0001) · `ui-styling` (`.claude/skills/ui-styling/`, dialog và combobox shadcn/ui)

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

### Data model sketch

Không thêm bảng hay cột. `V15__toa_nha_tang.sql` chỉ siết ràng buộc:

| Bảng | Khóa | Trường | Quan hệ | Thay đổi ở V15 |
|---|---|---|---|---|
| `toa_nha` | `ma_toa_nha` VARCHAR(20) PK, BQL nhập | `ten_toa_nha` VARCHAR(100) NOT NULL; `so_tang` INTEGER NOT NULL; `dia_chi_khu_vuc` VARCHAR(255) NOT NULL | 1 : N `tang`, 1 : N `khu_vuc_dung_chung` | `chk_toa_nha_ma CHECK (ma_toa_nha ~ '^[A-Z0-9]{1,10}$')`; bỏ `chk_toanha_sotang`, thêm `chk_toa_nha_so_tang CHECK (so_tang BETWEEN 1 AND 99)`; `CREATE UNIQUE INDEX uq_toa_nha_ten_lower ON toa_nha (lower(ten_toa_nha))`; `ALTER TABLE toa_nha DROP CONSTRAINT toa_nha_ten_toa_nha_key` (tên Postgres tự đặt cho `UNIQUE` viết liền cột ở V2; không dùng `IF EXISTS`, để tên sai làm migration báo lỗi rõ) vì index mới chặt hơn và bao trùm nó |
| `tang` | `ma_tang` VARCHAR(20) PK, server ghép | `ma_toa_nha` FK NOT NULL; `so_tang` INTEGER NOT NULL; UNIQUE (`ma_toa_nha`, `so_tang`) đã có | N : 1 `toa_nha`, 1 : N `can_ho` | `chk_tang_so_tang CHECK (so_tang BETWEEN 1 AND 99)`; `chk_tang_ma CHECK (ma_tang = ma_toa_nha \|\| '-' \|\| lpad(so_tang::text, 2, '0'))` |
| `khu_vuc_dung_chung` | `ma_khu_vuc` tự sinh `KV` + 8 chữ số | `ma_toa_nha` FK NOT NULL (đã có index); `ten_khu_vuc` VARCHAR(150); `vi_tri` VARCHAR(150) | N : 1 `toa_nha` | không đổi |

Seed hiện có (`A`, `A-01` đến `A-03`, `so_tang` 3) thỏa mọi CHECK mới.

**Không ghi bằng JPA.** Entity `ToaNha` hiện là `@Immutable` và chỉ ánh xạ `maToaNha`: Hibernate sẽ lặng lẽ bỏ UPDATE, và `save()` với khóa do người dùng đặt chạy `merge` thay vì báo trùng. Vì vậy mọi lệnh ghi của spec này dùng `JdbcClient` trong `@Transactional` của service, `ToaNha` giữ nguyên là entity chỉ đọc, không thêm entity `Tang`. Đọc cũng bằng SQL thuần như `ToaNhaQuery`.

### State transitions

Không có máy trạng thái. Một chuyển duy nhất: `toa_nha.so_tang` chỉ tăng, mỗi lần đúng một, kèm một dòng `tang` mới.

### Backend: lớp và trách nhiệm (package `canho`)

- `ToaNhaController`: thêm `them` (POST), `chiTiet` (GET `/{buildingId}`); sửa `@ChoPhep` của `danhSach` thêm `KY_THUAT`.
- `TangController` (`/api/v1/buildings/{buildingId}/floors`): `danhSach`, `them`.
- `KhuVucDungChungController` (`/api/v1/common-areas`): `danhSach`.
- `ToaNhaService`: giữ phần công khai hiện có (`danhSachMaToaNha`, `danhSach`), thêm `chiTiet(ma)`. Không gọi `PhamViTruyCapService` (vòng phụ thuộc, spec 0006); controller kiểm phạm vi rồi gọi service.
- `ThemToaNhaService`: chuẩn hóa, kiểm (AC-2), kiểm trùng (AC-3), gọi `ToaNhaGhi` trong một transaction, dịch ràng buộc DB, ghi nhật ký.
- `ToaNhaGhi` (`@Repository`, `JdbcClient`): `themToaNha(...)` là `INSERT INTO toa_nha`; `themTangTu1Den(ma, n)` là một lệnh `INSERT INTO tang (ma_tang, ma_toa_nha, so_tang) SELECT :ma || '-' || lpad(g::text, 2, '0'), :ma, g FROM generate_series(1, :n) g`; `soTangKhoaDong(ma)` là `SELECT so_tang FROM toa_nha WHERE ma_toa_nha = :ma FOR UPDATE`; `themTang(ma, soTang, maTang)` là `INSERT INTO tang` rồi `UPDATE toa_nha SET so_tang = :soTang WHERE ma_toa_nha = :ma`.
- `TangService`: `danhSach(maToaNha, trang)` và `them(maToaNha, soTang, maNguoiThucHien)` theo AC-4, AC-5; khóa qua `ToaNhaGhi.soTangKhoaDong`.
- `MaTang.cua(maToaNha, soTang)`: hàm tĩnh ghép mã tầng cho lần thêm một tầng; SQL `generate_series` dùng cùng mẫu, và CHECK `chk_tang_ma` bảo đảm hai nơi không lệch nhau.
- `KhuVucDungChungQuery`, `TangQuery`: SQL thuần có phân trang, sắp xếp qua bảng cột được phép (giống `ToaNhaQuery.COT_SAP_XEP`).
- `CanHoErrorCode` (hoặc hằng trong lớp lỗi riêng): `CANHO_TOA_NHA_DA_TON_TAI`, `CANHO_TANG_SAI_THU_TU`.
- `LoaiThaoTac`: thêm `CANHO_THEM_TOA_NHA("Thêm tòa nhà")`, `CANHO_THEM_TANG("Thêm tầng")`. `DoiTuongTacDong`: thêm `toaNha(ma)`, `tang(ma)`. Chi tiết: record `ChiTietToaNha.Them(int soTang)` và `ChiTietToaNha.ThemTang(String maToaNha, int soTang)`.

### API surface

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/buildings` (UC01-01, đã có) | GET | `page`, `size`, `sort` | `PageResponse<ToaNhaResponse>` | `@ChoPhep(BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, KY_THUAT)` | 400 tham số trang |
| `/api/v1/buildings` (UC01-02) | POST | `maToaNha`: string (req), `tenToaNha`: string (req), `soTang`: integer 1..99 (req), `diaChiKhuVuc`: string (req) | 201, `ToaNhaResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 409 `CANHO_TOA_NHA_DA_TON_TAI` |
| `/api/v1/buildings/{buildingId}` (mới, ngoài 81 API) | GET | `buildingId` | `ToaNhaResponse` | `@ChoPhep(BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, KY_THUAT)` | 404 |
| `/api/v1/buildings/{buildingId}/floors` (UC01-03) | GET | `buildingId`, `page`, `size`, `sort` (`soTang`) | `PageResponse<TangResponse>` | `@ChoPhep(BAN_QUAN_LY, KY_THUAT)` | 400, 404 |
| `/api/v1/buildings/{buildingId}/floors` (UC01-04) | POST | `buildingId`, `soTang`: integer 1..99 (req) | 201, `TangResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400, 404, 422 `CANHO_TANG_SAI_THU_TU` |
| `/api/v1/common-areas` (UC01-10) | GET | `maToaNha` (opt), `q` (opt), `page`, `size`, `sort` | `PageResponse<KhuVucDungChungResponse>` | `@ChoPhep(BAN_QUAN_LY, KY_THUAT)` | 400, 404 |

Record mới: `ThemToaNhaRequest(String maToaNha, String tenToaNha, JsonNode soTang, String diaChiKhuVuc)`, `ThemTangRequest(JsonNode soTang)`, `TangResponse(String maTang, int soTang, long soCanHo)`, `KhuVucDungChungResponse(String maKhuVuc, String tenKhuVuc, String viTri, String maToaNha, String tenToaNha)`. Mọi trường response mang `@Schema(requiredMode = REQUIRED)` để `openapi.json` sinh kiểu không tùy chọn. Sinh lại `docs/api/openapi.json`, chạy `pnpm gen:api`.

### Value sourcing

| Action | Value produced / displayed | Source |
|---|---|---|
| Thêm tòa | `ma_toa_nha`, `ten_toa_nha`, `so_tang`, `dia_chi_khu_vuc` | body, sau chuẩn hóa (AC-1) |
| Thêm tòa | N dòng `tang` | derived: `generate_series(1, soTang)` trong `ToaNhaGhi.themTangTu1Den`, mã theo mẫu của AC-6 |
| Thêm tòa | `soCanHo` trong response | hằng 0 (tòa mới chưa có căn) |
| Thêm tòa, tầng | `maNguoiThucHien` của nhật ký | `PhamViTruyCap.maNguoiDung()` (claim `sub`) |
| Thêm tầng | tầng kế tiếp mong đợi | `toa_nha.so_tang + 1`, đọc dưới khóa dòng |
| Thêm tầng | `ma_tang` | `MaTang.cua(maToaNha, soTang)` |
| Kiểm trùng tên | khóa so sánh | `lower(ten_toa_nha)` trong PostgreSQL, cùng biểu thức với `uq_toa_nha_ten_lower` (không tự hạ chữ ở Java, để luật so ở service và DB là một) |
| Danh sách tầng | `soCanHo` | `count(can_ho)` theo `ma_tang`, lúc đọc |
| Chi tiết tòa | `soCanHo` | `count(can_ho)` qua `tang`, cùng SQL với danh mục (`ToaNhaQuery`) |
| Khu vực | `tenToaNha` | join `toa_nha` |
| Mọi đọc theo tòa | phạm vi | `PhamViTruyCapService.cuaNguoiDangGoi().maToaNha()` |
| UI dialog thêm tầng | số tầng đề xuất | `soTang` của `GET /buildings/{id}` đang hiển thị, cộng 1 |
| UI combobox tòa | danh sách tòa | `GET /api/v1/buildings?size=100&sort=tenToaNha,asc` |
| UI nút theo vai trò | vai trò người dùng | `useHoSo().data?.vaiTro` (`frontend/src/lib/auth/use-ho-so.ts`, cùng cách trang tài khoản dùng) |

### Key invariants

- `toa_nha.so_tang` = số dòng `tang` của tòa, và tập số tầng là {1, ..., `so_tang`} (service, AC-7).
- `ma_tang` = `ma_toa_nha` + `-` + số tầng hai chữ số (CHECK `chk_tang_ma`).
- `1 ≤ so_tang ≤ 99` ở cả `toa_nha` và `tang` (CHECK).
- `ma_toa_nha` khớp `^[A-Z0-9]{1,10}$` (CHECK); không chứa gạch nối, nên tách mã tầng luôn rõ ràng.
- `lower(ten_toa_nha)` duy nhất (unique index).
- Thêm tầng luôn đọc `so_tang` dưới `FOR UPDATE`; không có đường ghi `tang` nào khác trong spec này.

### Security model

- Ghi (thêm tòa, thêm tầng): chỉ `BAN_QUAN_LY`.
- Đọc tòa (danh mục, chi tiết): `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`, `KY_THUAT`. Đọc tầng và khu vực: `BAN_QUAN_LY`, `KY_THUAT`.
- Mọi đọc ghi theo tòa đi qua phạm vi tòa của người gọi; ngoài phạm vi là 404 như không tồn tại (spec 0006).
- Không có dữ liệu cá nhân. Nhật ký kiểm toán cho mọi thay đổi cấu trúc không gian (AC-14).

### Configuration required

Không có biến môi trường hay khóa bí mật mới.

### Critical test scenarios

- Happy path: BQL thêm tòa `B` 5 tầng, rồi thêm tầng 6; `GET .../B/floors` trả `B-01` đến `B-06`, `GET /buildings/B` có `soTang` 6, có hai dòng nhật ký, verifies **AC-1**, **AC-4**, **AC-6**, **AC-7**, **AC-8**, **AC-14**
- Failure case: hai request thêm tầng 4 cho tòa `A` chạy song song (integration test, hai luồng, `CountDownLatch`); đúng một 201 và một 422 `CANHO_TANG_SAI_THU_TU`, tòa có 4 tầng, verifies **AC-5**, **AC-7**
- Failure case: thêm tòa tên "tòa a" khi đã có "Tòa A" trả 409 mục `tenToaNha`; chèn tay `tang` mã `A-7` hoặc `toa_nha` mã `a-1` bị CHECK từ chối, verifies **AC-3**, **AC-7**
- Failure case: `soTang` 0, 100, `"abc"`, `"3"`, 3.0, 3.5, `null` trả 400 mục `soTang`; `soTang` 3 hoặc 6 cho tòa 3 tầng trả 422, verifies **AC-2**, **AC-5**
- Auth/permission: Kế toán gọi `POST /buildings` và `GET .../floors` nhận 403; Cư dân gọi `GET /common-areas` nhận 403; Kỹ thuật gọi `GET /buildings` nhận 200, verifies **AC-10**, **AC-13**
- UI: dialog thêm tòa báo lỗi 409 ở ô Tên; dialog thêm tầng nhận 422 thì làm mới số tầng; bộ lọc khu vực giữ trên URL (Vitest + Testing Library), verifies **AC-15**, **AC-17**, **AC-18**

## Build plan

Thứ tự theo Tracer Bullet: lát đầu chạy thật từ DB tới giao diện cho một thao tác ghi, các lát sau làm dày thêm.

1. **Đường mỏng: thêm tòa.** `V15__toa_nha_tang.sql` (mọi ràng buộc của bảng Data model, viết một lần vì nhỏ và cả spec dựa vào nó); `ToaNhaGhi`, `MaTang`; `ThemToaNhaService` với chuẩn hóa, kiểm, kiểm trùng, dịch ràng buộc DB; `POST /buildings`; hai loại nhật ký và đối tượng mới; dialog "Thêm tòa nhà" trên `/toa-nha`. Unit test service với fake, integration test API và CHECK. Satisfies **AC-1**, **AC-2**, **AC-3**, **AC-6**, **AC-7**, **AC-13**, **AC-14**, **AC-15**, **AC-19**
2. **Chi tiết tòa và danh sách tầng.** `GET /buildings/{id}`, `GET .../floors`, `TangQuery`; mở UC01-01 cho `KY_THUAT` (controller và ma trận); trang `/toa-nha/[maToaNha]` với link từ danh mục, trạng thái 404 và 403. Satisfies **AC-8**, **AC-9**, **AC-10**, **AC-13**, **AC-16**, **AC-19**
3. **Thêm tầng.** `ToaNhaGhi.soTangKhoaDong` và `themTang`, `TangService.them`, `POST .../floors`; dialog xác nhận và xử lý 422; test chạy song song. Satisfies **AC-4**, **AC-5**, **AC-7**, **AC-14**, **AC-17**, **AC-19**
4. **Khu vực dùng chung.** `R__seed_02_khong_gian.sql` với ba khu vực; `KhuVucDungChungQuery`, `GET /common-areas`; trang `/khu-vuc-chung` và bật mục ma trận. Satisfies **AC-11**, **AC-12**, **AC-13**, **AC-18**, **AC-19**
5. **Hoàn tất.** Sinh lại `openapi.json`, `pnpm gen:api`, `pnpm typecheck`, `mvnw verify`; cập nhật test ma trận và `PhanQuyenCoverageTest`. Satisfies **AC-13**

## Consequences

**Positive**:
- Số tầng hiển thị luôn đúng với tầng thật; slice căn hộ (tính năng 15) chỉ việc chọn tầng có sẵn, không phải xử lý tòa thiếu tầng.
- Mã tầng không bao giờ sai hay trùng vì người dùng không nhập; DB tự bảo vệ cả khi có người chèn tay.
- Hai BQL thao tác cùng lúc nhận một kết quả rõ ràng, một mã lỗi duy nhất cho mọi trường hợp tầng sai.

**Negative / tradeoffs**:
- Không có tầng hầm, tầng lửng hay đánh số tầng kiểu bỏ số 13; tòa có cấu trúc như vậy phải chờ một spec mở rộng mô hình.
- Không sửa, không xóa được tòa và tầng: BQL nhập sai tên hay số tầng thì chỉ sửa được trong DB cho tới khi có tính năng sửa.
- Khóa dòng tòa làm các lần thêm tầng của cùng một tòa chạy tuần tự; không đáng kể ở tần suất thao tác này.
- Ra ngoài danh mục 81 API một endpoint (`GET /buildings/{id}`) và mở UC01-01 cho Kỹ thuật; cần ghi chú khi đối chiếu báo cáo đồ án với file xlsx.
- Combobox tòa ở trang khu vực chỉ lấy 100 tòa đầu; đủ cho một khu chung cư, không đủ nếu hệ thống quản lý rất nhiều khu.

**Neutral**:
- `V15` bỏ ràng buộc unique cũ của `ten_toa_nha` để thay bằng unique index trên `lower(ten_toa_nha)`; migration nhỏ, không đổi dữ liệu.
- Hai loại thao tác mới tự hiện ở bộ lọc màn hình nhật ký (spec 0007), không cần sửa màn hình đó.

## Follow-up

- [ ] Sửa tên, địa chỉ tòa và xóa tòa hoặc tầng chưa có căn hộ: cần spec riêng khi có nhu cầu (ngoài 81 API).
- [ ] Tầng hầm và cách đánh số tầng đặc biệt, nếu khách hàng của đồ án cần.
- [ ] Ghi chú trong báo cáo đồ án: endpoint `GET /api/v1/buildings/{buildingId}` thêm mới và UC01-01 mở cho Kỹ thuật, lệch khỏi file đặc tả xlsx.
- [ ] Khi có bảng gán nhân viên vào tòa (mục Deferred của scope), tòa vừa thêm cần được gán cho người tạo, nếu không BQL tạo xong sẽ không thấy tòa của mình.
