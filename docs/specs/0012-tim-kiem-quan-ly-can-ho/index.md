# 0012. Tìm kiếm và quản lý căn hộ: mã căn do server ghép từ tầng, sửa diện tích và trạng thái dưới khóa dòng

**Date**: 2026-11-26
**Status**: In Progress

## Summary

Spec này chốt phần căn hộ của UC01: BQL, BQT và Kế toán tìm căn hộ có lọc và phân trang; BQL tạo và sửa căn; nhân viên và cư dân xem chi tiết một căn. Mã căn hộ không do người dùng gõ mà do server ghép từ tầng và số thứ tự căn (tầng `A-01`, căn số 5 thành `A-0105`), nên mã luôn khớp tầng và DB tự chặn mã sai. Sửa chỉ đổi diện tích và trạng thái sử dụng, khóa dòng (giữ dòng đó cho một transaction tại một thời điểm) để nhật ký kiểm toán luôn ghi đúng giá trị cũ. Không thêm bảng hay cột; chỉ một migration siết ràng buộc và một file seed thêm căn để demo bộ lọc.

## Requirements

**User stories**:
- Là BQL, BQT hoặc Kế toán, tôi muốn tìm căn hộ theo tòa, tầng, khoảng diện tích, trạng thái sử dụng và một phần mã căn, có phân trang, để nhanh chóng tra cứu căn cần xử lý.
- Là BQL, tôi muốn thêm một căn vào một tầng có sẵn chỉ bằng số thứ tự căn, để mã căn luôn đúng quy ước mà không phải tự gõ.
- Là BQL, tôi muốn sửa diện tích hoặc trạng thái sử dụng của một căn và có nhật ký ghi lại giá trị cũ và mới, để mọi thay đổi đều truy vết được.
- Là BQL, BQT hoặc Kế toán, tôi muốn mở chi tiết một căn từ danh sách.
- Là cư dân, tôi muốn xem thông tin căn hộ mình đang cư trú, và không xem được căn của người khác.

**Thuật ngữ dùng trong spec**:
- *Vai trò nhân viên đọc căn hộ*: `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`.
- *Phạm vi tòa*, *phạm vi căn*: `PhamViTruyCap.maToaNha()` và `PhamViTruyCap.maCanHo()` (spec 0006). `maCanHo()` là các căn người gọi đang cư trú hôm nay theo hồ sơ cư trú `Hợp lệ`.
- *Chuẩn hóa mã*: cắt khoảng trắng hai đầu rồi đổi chữ hoa (`Locale.ROOT`), như `NhapLieu.chuanHoaMaToaNha`.
- *Trạng thái hợp lệ*: đúng một trong bốn chuỗi của CHECK `chk_canho_trangthai` (V2): `Trống`, `Đang sử dụng`, `Đang sửa chữa`, `Đã bàn giao`, so sau khi đổi về NFC.
- *Diện tích hợp lệ*: số JSON (không nhận chuỗi, `true`, `null`, mảng, đối tượng), lớn hơn 0, tối đa 1000, tối đa 2 chữ số thập phân sau khi bỏ số 0 thừa ở cuối (`75.50` hợp lệ, `75.505` không). Đọc thành `JsonNode` rồi kiểm ở service như `soTang` của spec 0011, để giá trị sai ra lỗi trường thay vì 400 `MALFORMED_REQUEST`.
- *Thứ tự trạng thái*: `Trống` < `Đang sử dụng` < `Đang sửa chữa` < `Đã bàn giao` (dùng khi sắp xếp).

**Acceptance criteria**

*Tìm và lọc căn hộ (API-UC01-05)*

- **AC-1**: `GET /api/v1/apartments?maToaNha&soTang&dienTichTu&dienTichDen&trangThaiSuDung&q&page&size&sort`, vai trò nhân viên đọc căn hộ, trả 200 `message` "Tìm kiếm và lọc danh sách căn hộ thành công", `data` là `PageResponse<CanHoResponse>`. Chỉ có căn thuộc tòa trong phạm vi tòa của người gọi. Mọi tham số lọc tùy chọn và kết hợp bằng AND. Phân trang theo spec 0001 (`page` từ 1, `size` mặc định 20, tối đa 100).
- **AC-2**: Nghĩa của từng tham số lọc:
  - `maToaNha`: chuẩn hóa mã; chuỗi rỗng sau chuẩn hóa tính như vắng. Tòa ngoài phạm vi (kể cả mã không tồn tại hay sai định dạng) trả 404 `NOT_FOUND` "Không tìm thấy dữ liệu yêu cầu".
  - `soTang`: số nguyên 1 đến 99, chỉ dùng được khi có `maToaNha`; lọc căn thuộc tầng có `tang.so_tang` bằng giá trị này trong tòa đó. Tầng chưa có trong tòa thì trả trang rỗng, không lỗi.
  - `dienTichTu`, `dienTichDen`: số từ 0 đến 1000, tối đa 2 chữ số thập phân, bao gồm hai đầu.
  - `trangThaiSuDung`: lặp lại được (`?trangThaiSuDung=Trống&trangThaiSuDung=Đang sửa chữa`), căn khớp một trong các giá trị là đạt. Giá trị lặp lại trùng nhau không lỗi.
  - `q`: chuẩn hóa mã, tối đa 20 ký tự; khớp khi `ma_can_ho` chứa `q` (ký tự `%`, `_`, `\` trong `q` được thoát, nên khớp đúng nghĩa đen). `q` khớp mọi vị trí của mã, nên `q=03` ra cả căn số 3 lẫn mọi căn tầng 3 (`A-03xx`); đây là hành vi mong đợi.
  - Mọi tham số (kể cả từng giá trị `trangThaiSuDung`) cắt khoảng trắng hai đầu xong mà rỗng thì tính như vắng.
  - Dạng chữ được nhận trên query: `soTang` khớp `^[1-9][0-9]?$`; `dienTichTu`, `dienTichDen` khớp `^[0-9]{1,4}(\.[0-9]{1,2})?$` (không nhận dấu phẩy, dấu `+`, số mũ hay `.5`), rồi mới kiểm khoảng.
  - Phạm vi tòa của người gọi rỗng thì trả trang rỗng (`totalItems` 0) mà không chạy SQL, để không sinh `IN ()`.
- **AC-3**: Tham số sai trả 400 `VALIDATION_FAILED` "Dữ liệu không hợp lệ", một mục `errors` cho mỗi tham số sai, theo thứ tự `soTang`, `dienTichTu`, `dienTichDen`, `trangThaiSuDung`, `q`:
  - `soTang` không phải số nguyên hoặc ngoài 1 đến 99: "Số tầng phải là số nguyên từ 1 đến 99"; có `soTang` mà không có `maToaNha`: "Chọn tòa nhà trước khi lọc theo tầng"
  - `dienTichTu` hoặc `dienTichDen` không phải số, âm, lớn hơn 1000 hoặc quá 2 chữ số thập phân: "Diện tích lọc phải từ 0 đến 1000, tối đa 2 chữ số thập phân" (ở đúng mục sai)
  - cả hai hợp lệ mà `dienTichTu` > `dienTichDen`: mục `dienTichDen` "Diện tích đến phải lớn hơn hoặc bằng diện tích từ"
  - `trangThaiSuDung` có một giá trị không phải trạng thái hợp lệ: "Trạng thái sử dụng không hợp lệ"
  - `q` quá 20 ký tự: "Từ khóa tìm kiếm tối đa 20 ký tự"
  - Lỗi phân trang và `sort` theo spec 0001. Có lỗi 400 thì không kiểm phạm vi (404) nữa, theo thứ tự kiểm của spec 0006.
- **AC-4**: `sort` nhận `maCanHo`, `dienTich`, `trangThaiSuDung` (mặc định `maCanHo,asc`); trường khác, chiều khác `asc`/`desc` hay trường lặp xử lý theo spec 0001 (400). `trangThaiSuDung` sắp theo *thứ tự trạng thái*, không theo bảng chữ cái. Khóa phụ luôn là `ma_can_ho` tăng dần, để phân trang ổn định.

*Xem chi tiết căn hộ (API-UC01-06)*

- **AC-5**: `GET /api/v1/apartments/{apartmentId}`, `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`, `CU_DAN`, trả 200 `message` "Lấy chi tiết căn hộ thành công", `data` là `CanHoResponse`. `apartmentId` được chuẩn hóa mã trước khi tra.
- **AC-6**: Người gọi xem được căn khi một trong hai điều sau đúng: có một vai trò nhân viên đọc căn hộ và căn thuộc tòa trong phạm vi tòa; hoặc có `CU_DAN` và mã căn nằm trong phạm vi căn. Người có nhiều vai trò chỉ cần đạt một điều. Không đạt (kể cả căn không tồn tại, mã sai định dạng, cư dân xem căn người khác hay căn mình đã hết cư trú) trả 404 `NOT_FOUND` "Không tìm thấy dữ liệu yêu cầu", không phân biệt các trường hợp.

*Tạo căn hộ (API-UC01-07)*

- **AC-7**: `POST /api/v1/apartments` với body `{ maTang, soCan, dienTich, trangThaiSuDung? }`, chỉ `BAN_QUAN_LY`. `maTang` được chuẩn hóa mã. Server ghép mã căn `<maTang><soCan hai chữ số>` (`A-01` và 5 thành `A-0105`) rồi thêm một dòng `can_ho`; `trangThaiSuDung` vắng hoặc `null` thì là `Trống`; `bi_tam_dung_tien_ich` là `false`. Trả 201 `message` "Tạo mới căn hộ thành công", `data` là `CanHoResponse` của căn vừa tạo.
- **AC-8**: Dữ liệu sai trả 400 `VALIDATION_FAILED` "Dữ liệu không hợp lệ", một mục `errors` mỗi trường sai, thứ tự `maTang`, `soCan`, `dienTich`, `trangThaiSuDung`:
  - `maTang` vắng hoặc rỗng: "Vui lòng chọn tầng"; không có tầng nào mã đó thuộc tòa trong phạm vi tòa của người gọi (kể cả mã sai định dạng): "Tầng không tồn tại"
  - `soCan` không phải số nguyên JSON (`isIntegralNumber()` và `canConvertToInt()`) hoặc ngoài 1 đến 99: "Số căn phải là số nguyên từ 1 đến 99"
  - `dienTich` không phải *diện tích hợp lệ* (kể cả vắng): "Diện tích phải lớn hơn 0, tối đa 1000 m² và tối đa 2 chữ số thập phân"
  - `trangThaiSuDung` có mặt, khác `null`, mà không phải *trạng thái hợp lệ* (kể cả chuỗi rỗng): "Trạng thái sử dụng không hợp lệ"
  - `maTang` và `trangThaiSuDung` đọc thành `JsonNode` và chỉ nhận chuỗi JSON (`isTextual()`); số, `true`, mảng hay đối tượng là lỗi của đúng trường đó (`maTang`: "Tầng không tồn tại"; `trangThaiSuDung`: "Trạng thái sử dụng không hợp lệ"), không để Jackson tự đổi số thành chuỗi.
  - Body vắng hoặc không phải JSON đối tượng trả 400 `MALFORMED_REQUEST` như hiện có. Có lỗi 400 thì không kiểm trùng (AC-9).
- **AC-9**: Mã căn ghép ra đã có trả 409 `CANHO_CAN_HO_DA_TON_TAI` `message` "Căn hộ đã tồn tại", `errors` một mục `soCan` "Tầng này đã có căn số <soCan> (<maCanHo>)". Hai request tạo cùng một căn chạy song song cho đúng một 201 và một 409 này (vi phạm khóa chính ở DB được dịch về cùng lỗi, không ra 500). Chỉ dịch khi `DuplicateKeyException` có tên ràng buộc `can_ho_pkey`; lỗi khác ném tiếp. Sau khi bắt, transaction đã bị PostgreSQL hủy, nên khối `catch` chỉ ném `ApiException` 409, không chạy thêm SQL hay ghi nhật ký.
- **AC-10**: Migration `V16__can_ho.sql` để DB tự chặn dữ liệu sai kể cả khi chèn tay: mã căn phải khớp `^[A-Z0-9]{1,10}-[0-9]{4}$`, bắt đầu đúng bằng `ma_tang` của chính dòng đó, dài hơn `ma_tang` đúng 2 ký tự, và hai số cuối khác `00`; diện tích lớn hơn 0 và tối đa 1000. Seed hiện có thỏa mọi CHECK mới.

*Sửa căn hộ (API-UC01-08)*

- **AC-11**: `PUT /api/v1/apartments/{apartmentId}` với body `{ dienTich, trangThaiSuDung }` (bắt buộc cả hai), chỉ `BAN_QUAN_LY`. `apartmentId` chuẩn hóa mã; căn ngoài phạm vi tòa (kể cả không tồn tại) trả 404. Trong một transaction: đọc dòng căn bằng `SELECT ... FOR UPDATE`, so với giá trị mới, cập nhật khi có trường đổi. Trả 200 `message` "Cập nhật thông tin căn hộ thành công", `data` là `CanHoResponse` sau khi sửa. Mã căn, tầng và `bi_tam_dung_tien_ich` không đổi; trường khác trong body bị bỏ qua. `trangThaiSuDung` đọc thành `JsonNode` như AC-8. Nếu `khoaDong` không thấy dòng (thực tế không xảy ra vì không có API xóa căn) thì trả 404 như ngoài phạm vi.
- **AC-12**: Dữ liệu sai trả 400 `VALIDATION_FAILED`, thứ tự `dienTich`, `trangThaiSuDung`: `dienTich` như AC-8; `trangThaiSuDung` vắng hoặc `null`: "Vui lòng chọn trạng thái sử dụng"; có mà không phải *trạng thái hợp lệ*: "Trạng thái sử dụng không hợp lệ". Thứ tự kiểm: vai trò 403, rồi body 400, rồi phạm vi 404.
- **AC-13**: Hai BQL sửa cùng một căn gần như cùng lúc thì cả hai nhận 200, lần ghi sau thắng. Vì đọc dưới khóa dòng, giá trị cũ trong mỗi dòng nhật ký bằng đúng giá trị căn có ngay trước lần sửa đó.

*Nhật ký kiểm toán*

- **AC-14**: Tạo căn thành công ghi một dòng `CANHO_THEM_CAN_HO` ("Thêm căn hộ"), đối tượng `CanHo:<maCanHo>`, chi tiết `{ maTang, dienTich, trangThaiSuDung }`. Sửa có ít nhất một trường đổi ghi một dòng `CANHO_SUA_CAN_HO` ("Sửa căn hộ"), cùng đối tượng, chi tiết chỉ chứa trường đổi, mỗi trường dạng `{ cu, moi }` (ví dụ `{ "dienTich": { "cu": 75.50, "moi": 80.00 } }`). Diện tích so theo giá trị số (`75.5` bằng `75.50`, không tính là đổi), và mọi diện tích ghi vào nhật ký đều qua `setScale(2)` nên JSON luôn có đúng 2 chữ số thập phân (`70.50`, không phải `70.5`). Sửa không đổi gì thì không ghi nhật ký. Ghi qua `NhatKyService.ghi` (tự chờ sau commit), người thực hiện là `PhamViTruyCap.maNguoiDung()`.

*Phân quyền*

- **AC-15**: Mỗi handler mang đúng một `@ChoPhep` theo bảng API surface; sai vai trò trả 403 `FORBIDDEN` (ví dụ Cư dân gọi `GET /apartments`, Kế toán gọi `POST` hoặc `PUT`), chưa đăng nhập trả 401. `PhanQuyenCoverageTest` xanh.

*Dữ liệu mẫu*

- **AC-16**: File seed mới `db/seed/R__seed_03_can_ho.sql` (chạy sau `R__seed_01_xac_thuc.sql`) thêm 9 căn cho tòa `A`, để mỗi tầng có 4 căn (`A-0101` đến `A-0104`, `A-0201` đến `A-0204`, `A-0301` đến `A-0304`). Chín căn mới đủ cả bốn trạng thái và diện tích khác nhau trong khoảng 45 đến 120 m² (bảng ở *Data model sketch*). Ba căn cũ giữ nguyên. Dùng `ON CONFLICT (ma_can_ho) DO NOTHING`, chạy lại không đổi số dòng. Các assert đang ghi cứng 3 căn seed đổi theo: `SchemaMigrationTest:196` (`count(*) FROM can_ho` thành 12), `ToaNhaIntegrationTest:194` (`soCanHo` của tòa A thành 12), `TangIntegrationTest:194` (`soCanHo` các tầng thành 4) và `TangIntegrationTest:204` (`soCanHo` thành 12).

*Giao diện*

- **AC-17**: Trang `/can-ho` (mục ma trận đã có, bật `sanSang`, thêm icon và mô tả "Tìm và quản lý căn hộ theo tòa, tầng, trạng thái") có thanh lọc gồm: combobox tòa có nhãn (mặc định "Tất cả tòa nhà", từ `GET /api/v1/buildings?size=100&sort=tenToaNha,asc`); ô chọn tầng có nhãn, chỉ bật khi đã chọn tòa, lựa chọn "Tầng 1" đến "Tầng N" với N là `soTang` của `GET /api/v1/buildings/{maToaNha}`; hai ô "Diện tích từ", "Diện tích đến" (ô chữ `inputMode="decimal"`, đổi dấu phẩy thành dấu chấm trước khi gửi); nhóm checkbox trạng thái trong `fieldset` có `legend`; ô tìm theo mã căn. Mọi bộ lọc cùng `page`, `size`, `sort` nằm trên URL với đúng tên tham số của API; giá trị mặc định (`page=1`, `size=20`, `sort=maCanHo,asc`, bộ lọc trống) không ghi lên URL; đổi bộ lọc thì về trang 1; đổi hay bỏ tòa thì bỏ tầng (URL có `soTang` mà không có `maToaNha` thì giao diện bỏ `soTang` trước khi gọi API). Bảng (`DataTable`) có cột mã căn (link tới `/can-ho/<maCanHo>`), tòa, tầng, diện tích (dạng `75,50 m²` theo `vi-VN`), trạng thái (badge ghi đúng chuỗi DB), và nhãn "Tạm dừng tiện ích" khi `biTamDungTienIch` đúng. Chỉ ba cột mã căn, diện tích, trạng thái sắp được (bấm tiêu đề, có `aria-sort`); cỡ trang chọn 20, 50, 100 như các bảng hiện có. Lỗi 400 hiện ngay dưới ô tương ứng. Không có kết quả thì hiện "Không có căn hộ nào khớp bộ lọc".
- **AC-18**: Nút "Thêm căn hộ" chỉ hiện khi người dùng có `BAN_QUAN_LY`, mở dialog có các ô có nhãn: tòa nhà (combobox), tầng (chọn từ `GET /api/v1/buildings/{maToaNha}/floors?size=100&sort=soTang,asc`, chỉ bật khi đã chọn tòa), số căn (ô chữ `inputMode="numeric"`, gửi số nguyên khi giá trị là chữ số, gửi nguyên chuỗi khi không, như dialog thêm tòa), diện tích (như AC-17), trạng thái (mặc định `Trống`). Khi có đủ tầng và số căn hợp lệ, dialog hiện "Mã căn sẽ tạo: <maCanHo>". Thành công: toast "Đã thêm căn hộ <maCanHo>", đóng dialog, invalidate các query key gốc `["can-ho"]` (danh sách và chi tiết căn, key mới của spec này) và các key mà dialog thêm tầng của spec 0011 đang làm mới (danh mục tòa, chi tiết tòa, bảng tầng; dùng lại đúng hằng key ở `frontend/src/app/(app)/toa-nha/`, không đặt key mới), vì `soCanHo` đổi. Lỗi 400 và 409 hiện dưới đúng ô (409 ở ô số căn).
- **AC-19**: Trang chi tiết `/can-ho/<maCanHo>` (nhân viên) và `/can-ho-cua-toi/<maCanHo>` (cư dân) dùng chung một component, hiện mã căn, tòa (tên và mã), tầng, diện tích, trạng thái sử dụng, và trạng thái tiện ích ("Đang dùng bình thường" hoặc "Tạm dừng tiện ích"). 404 hiện "Không tìm thấy căn hộ" cùng link quay lại; component nhận prop đích quay lại: `/can-ho` ở trang nhân viên, `/can-ho-cua-toi` ở trang cư dân. Nút "Sửa" chỉ hiện cho `BAN_QUAN_LY` trên `/can-ho/<maCanHo>`, mở dialog hai ô có nhãn đã điền sẵn giá trị hiện tại; thành công thì toast "Đã cập nhật căn hộ <maCanHo>", làm mới chi tiết và danh sách căn; lỗi 400 hiện dưới ô; 404 hiện thông báo lỗi trong dialog.
- **AC-20**: Trang `/can-ho-cua-toi` (mục ma trận đã có, bật `sanSang`) đọc `phamVi.maCanHo` từ hồ sơ (`useHoSo`): không có căn nào thì hiện "Bạn chưa có hồ sơ cư trú hợp lệ"; đúng một căn thì hiện luôn chi tiết căn đó (AC-19); nhiều căn thì hiện danh sách thẻ (mã, tòa, tầng, diện tích), mỗi thẻ link tới `/can-ho-cua-toi/<maCanHo>`. Mỗi thẻ tự tải (`GET /api/v1/apartments/{ma}` riêng), có trạng thái đang tải riêng; thẻ nhận 404 hiện "Không còn quyền xem căn này", thẻ lỗi khác hiện "Không tải được thông tin căn hộ"; các thẻ khác không bị ảnh hưởng.
- **AC-21**: Mọi bộ lọc, dialog, combobox, bảng và thẻ mới dùng được hoàn toàn bằng bàn phím, focus nhìn thấy được, mọi ô có nhãn, lỗi gắn với ô (`aria-describedby`), đạt WCAG AA như quy tắc chung.

## Decision

**Chosen option**: Option 2: Mã căn do server ghép từ tầng và số căn, ghi bằng `JdbcClient` dưới khóa dòng, DB giữ định dạng bằng CHECK.

BQL chỉ chọn tầng và nhập số căn 1 đến 99; mã căn luôn là `<maTang><số căn hai chữ số>`, và `V16` bảo đảm mọi dòng `can_ho` đều theo đúng mẫu đó.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`, riêng lỗi theo vỏ bọc spec 0001) · `ui-styling` (`.claude/skills/ui-styling/`, dialog, combobox, checkbox shadcn/ui) · `tanstack-table` (`.claude/skills/tanstack-table/`, cột và sắp xếp của `DataTable`)

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

### Data model sketch

Không thêm bảng hay cột. `V16__can_ho.sql` chỉ siết ràng buộc của `can_ho`:

| Bảng | Khóa | Trường | Quan hệ | Thay đổi ở V16 |
|---|---|---|---|---|
| `can_ho` | `ma_can_ho` VARCHAR(20) PK, server ghép | `ma_tang` FK NOT NULL (đã có `idx_canho_matang`); `dien_tich` NUMERIC(8,2) NOT NULL; `trang_thai_su_dung` VARCHAR(40) NOT NULL mặc định `Trống`, CHECK 4 giá trị (giữ nguyên); `bi_tam_dung_tien_ich` BOOLEAN NOT NULL mặc định false (spec này không ghi) | N : 1 `tang`; `tang` N : 1 `toa_nha` | thêm `chk_can_ho_ma CHECK (ma_can_ho ~ '^[A-Z0-9]{1,10}-[0-9]{4}$' AND left(ma_can_ho, length(ma_tang)) = ma_tang AND length(ma_can_ho) = length(ma_tang) + 2 AND right(ma_can_ho, 2) <> '00')`; bỏ `chk_canho_dientich`, thêm `chk_can_ho_dien_tich CHECK (dien_tich > 0 AND dien_tich <= 1000)` |
| `tang`, `toa_nha` | như spec 0011 | chỉ đọc | | không đổi |

Không thêm index: dữ liệu vài trăm căn, lọc theo tòa đi qua `idx_canho_matang` và khóa chính của `tang`; lọc diện tích, trạng thái và `q` (`LIKE '%x%'` luôn quét tuần tự) chạy trên tập đã thu hẹp, chấp nhận được ở cỡ dữ liệu này. Không dùng `IF EXISTS` khi bỏ ràng buộc cũ (tên sai thì migration báo lỗi rõ), như `V15`.

Seed `R__seed_03_can_ho.sql` thêm 9 căn sau (ba căn cũ `A-0101`, `A-0201`, `A-0301` vẫn 75.50 m², `Đang sử dụng`):

| Căn | Diện tích | Trạng thái |
|---|---|---|
| `A-0102` | 45.00 | `Trống` |
| `A-0103` | 60.25 | `Đang sử dụng` |
| `A-0104` | 120.00 | `Đã bàn giao` |
| `A-0202` | 52.50 | `Đang sửa chữa` |
| `A-0203` | 68.00 | `Trống` |
| `A-0204` | 95.75 | `Đang sử dụng` |
| `A-0302` | 82.00 | `Đã bàn giao` |
| `A-0303` | 110.40 | `Đang sửa chữa` |
| `A-0304` | 48.30 | `Trống` |

Mọi căn seed có `bi_tam_dung_tien_ich` false. Không thêm hồ sơ cư trú, nên các căn `Đang sử dụng` mới chưa có cư dân (chấp nhận được cho tới UC02).

**Không ghi bằng JPA.** Như spec 0011: khóa do server ghép nên `save()` sẽ chạy `merge` thay vì báo trùng. Mọi lệnh ghi dùng `JdbcClient` trong `@Transactional` của service; không thêm entity `CanHo`.

### State transitions

Không có máy trạng thái. BQL đặt `trang_thai_su_dung` sang bất kỳ giá trị nào trong bốn giá trị, từ bất kỳ giá trị nào. Căn mới mặc định `Trống`. Luật giữa trạng thái và hồ sơ cư trú để UC02 quyết (xem Follow-up).

### Backend: lớp và trách nhiệm (package `canho`)

- `CanHoController` (`/api/v1/apartments`): `timKiem` (GET), `chiTiet` (GET `/{apartmentId}`), `them` (POST), `sua` (PUT `/{apartmentId}`). Lấy phạm vi qua `PhamViTruyCapService.cuaNguoiDangGoi()` rồi truyền `PhamViTruyCap` xuống service (cùng cách spec 0011).
- `TimCanHoService`: `timKiem(BoLocCanHo, trang, phamVi)` kiểm tham số theo AC-2, AC-3 (thu lỗi rồi ném một `VALIDATION_FAILED`), rồi `phamVi.yeuCauToaNha` khi có `maToaNha`; `chiTiet(ma, phamVi)` theo AC-6.
- `ThemCanHoService`: chuẩn hóa, kiểm theo AC-8 (tra tầng qua `TangQuery`, tầng phải thuộc tòa trong phạm vi), ghép mã bằng `MaCanHo`, kiểm trùng trước (để trường hợp thường gặp trả 409 mà không cần rollback), gọi `CanHoGhi.them`, bắt `DuplicateKeyException` của khóa chính `can_ho_pkey` để trả cùng 409 cho trường hợp chạy song song (AC-9; `catch` chỉ ném lỗi), ghi nhật ký.
- `SuaCanHoService`: kiểm body (AC-12), tra tòa của căn để kiểm phạm vi (404), `CanHoGhi.khoaDong(ma)` lấy giá trị cũ dưới `FOR UPDATE`, so sánh (`BigDecimal.compareTo` cho diện tích), `CanHoGhi.capNhat` khi có đổi, ghi nhật ký khi có đổi (AC-13, AC-14).
- `CanHoGhi` (`@Repository`, `JdbcClient`): `them(ma, maTang, dienTich, trangThai)`, `khoaDong(ma)` (`SELECT dien_tich, trang_thai_su_dung FROM can_ho WHERE ma_can_ho = :ma FOR UPDATE`), `capNhat(ma, dienTich, trangThai)`.
- `CanHoQuery` (`@Repository`, SQL thuần): `tim(boLoc, maToaNhaPhamVi, pageable)` dựng `WHERE` từ các điều kiện có mặt, tham số đặt tên (không nối chuỗi giá trị vào SQL), `count(*)` cho `totalItems`; `cua(ma)` trả một `CanHoResponse` hoặc rỗng; `maToaNhaCua(ma)`. Sắp xếp qua bảng cột được phép (`COT_SAP_XEP` như `ToaNhaQuery`): `maCanHo` → `c.ma_can_ho`, `dienTich` → `c.dien_tich`, `trangThaiSuDung` → `CASE c.trang_thai_su_dung WHEN 'Trống' THEN 1 WHEN 'Đang sử dụng' THEN 2 WHEN 'Đang sửa chữa' THEN 3 ELSE 4 END`; luôn nối thêm `c.ma_can_ho ASC`.
- `MaCanHo.cua(maTang, soCan)`: hàm tĩnh `maTang + String.format("%02d", soCan)`; CHECK `chk_can_ho_ma` bảo đảm không lệch.
- `NhapLieuCanHo` (lớp mới, để `NhapLieu` không vượt khoảng 200 dòng): `dienTich(JsonNode, String truong, List<FieldError>)` (đọc `decimalValue()` khi `isNumber()`, `stripTrailingZeros().scale() <= 2`, trả giá trị đã `setScale(2)`), `dienTichLoc(String, ...)` (regex của AC-2 rồi `new BigDecimal`), `soCan(JsonNode, ...)`, `chuoi(JsonNode)` (chỉ nhận `isTextual()`), `trangThai(String, ...)` (NFC rồi so bốn giá trị). Hằng `TrangThaiSuDung` giữ bốn chuỗi và thứ tự.
- `CanHoErrorCodes`: thêm `CANHO_CAN_HO_DA_TON_TAI`.
- `LoaiThaoTac`: thêm `CANHO_THEM_CAN_HO("Thêm căn hộ")`, `CANHO_SUA_CAN_HO("Sửa căn hộ")`. `DoiTuongTacDong`: thêm `canHo(ma)` (`CanHo:<ma>`). Chi tiết: record `ChiTietCanHo.Them(String maTang, BigDecimal dienTich, String trangThaiSuDung)` và `ChiTietCanHo.Sua(ThayDoi<BigDecimal> dienTich, ThayDoi<String> trangThaiSuDung)` với `ThayDoi(cu, moi)`, trường không đổi là `null` và không xuất ra JSON (`@JsonInclude(NON_NULL)`).

### API surface

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/apartments` (UC01-05) | GET | `maToaNha`, `soTang`: int, `dienTichTu`, `dienTichDen`: decimal, `trangThaiSuDung`: string lặp, `q`, `page`, `size`, `sort` (tất cả opt) | 200, `PageResponse<CanHoResponse>` | `@ChoPhep(BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN)` | 400 `VALIDATION_FAILED`, 404 tòa ngoài phạm vi |
| `/api/v1/apartments/{apartmentId}` (UC01-06) | GET | `apartmentId` | 200, `CanHoResponse` | `@ChoPhep(BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, CU_DAN)` | 404 |
| `/api/v1/apartments` (UC01-07) | POST | `maTang`: string (req), `soCan`: integer 1..99 (req), `dienTich`: number (req), `trangThaiSuDung`: string (opt) | 201, `CanHoResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 409 `CANHO_CAN_HO_DA_TON_TAI` |
| `/api/v1/apartments/{apartmentId}` (UC01-08) | PUT | `apartmentId`, `dienTich`: number (req), `trangThaiSuDung`: string (req) | 200, `CanHoResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 404 |

Tham số lọc diện tích nhận dạng chuỗi trên query (`@RequestParam String`) rồi parse ở service bằng `new BigDecimal`, để giá trị sai ra lỗi trường thay vì lỗi chuyển kiểu của Spring. `soTang` cũng nhận `String` vì cùng lý do.

Record mới: `CanHoResponse(String maCanHo, String maTang, int soTang, String maToaNha, String tenToaNha, BigDecimal dienTich, String trangThaiSuDung, boolean biTamDungTienIch)` (`trangThaiSuDung` có `@Schema(allowableValues = { "Trống", "Đang sử dụng", "Đang sửa chữa", "Đã bàn giao" })`), `ThemCanHoRequest(JsonNode maTang, JsonNode soCan, JsonNode dienTich, JsonNode trangThaiSuDung)`, `SuaCanHoRequest(JsonNode dienTich, JsonNode trangThaiSuDung)` (trong OpenAPI khai `@Schema(type = "string")` hoặc `"number"` cho từng trường để kiểu sinh ra ở frontend đúng), `BoLocCanHo(String maToaNha, String soTang, String dienTichTu, String dienTichDen, List<String> trangThaiSuDung, String q)`. Mọi trường response mang `@Schema(requiredMode = REQUIRED)` để `openapi.json` sinh kiểu không tùy chọn.

### Value sourcing

| Action | Value produced / displayed | Source |
|---|---|---|
| Tạo căn | `ma_can_ho` | `MaCanHo.cua(maTang đã chuẩn hóa, soCan)` |
| Tạo căn | `ma_tang`, `dien_tich` | body (AC-7); `dien_tich` là `BigDecimal` từ `JsonNode.decimalValue()` |
| Tạo căn | `trang_thai_su_dung` | body sau NFC; vắng hoặc `null` thì hằng `Trống` |
| Tạo căn | `bi_tam_dung_tien_ich` | `DEFAULT FALSE` của cột |
| Tạo căn | tầng thuộc phạm vi | `TangQuery` tra `tang.ma_toa_nha` theo `maTang`, rồi so với `PhamViTruyCap.maToaNha()` |
| Tạo căn | `soCan`, `maCanHo` trong lỗi 409 | giá trị đã kiểm và mã vừa ghép |
| Mọi response căn | `soTang`, `maToaNha`, `tenToaNha` | join `tang` và `toa_nha` trong `CanHoQuery` |
| Tìm căn | tập tòa được xem | `PhamViTruyCap.maToaNha()`; có `maToaNha` thì chỉ tòa đó (sau `yeuCauToaNha`) |
| Tìm căn | `totalItems`, `totalPages` | `count(*)` cùng `WHERE`, dựng qua `PageResponse.from` |
| Xem chi tiết | quyền cư dân | `PhamViTruyCap.maCanHo()` (hồ sơ `Hợp lệ`, cư trú hôm nay theo giờ Việt Nam, spec 0006) |
| Xem chi tiết, sửa | tòa của căn để kiểm phạm vi | `CanHoQuery.maToaNhaCua(ma)` (join `tang`) |
| Sửa căn | giá trị cũ cho nhật ký | `CanHoGhi.khoaDong(ma)`, đọc dưới `FOR UPDATE` |
| Sửa căn | "có đổi" | `dienTichCu.compareTo(dienTichMoi) != 0` hoặc chuỗi trạng thái khác |
| Nhật ký | `maNguoiThucHien` | `PhamViTruyCap.maNguoiDung()` (claim `sub`) |
| Nhật ký | thời điểm | `NhatKyService.ghi` lấy từ bean `Clock` (spec 0007) |
| UI lọc tòa | danh sách tòa | `GET /api/v1/buildings?size=100&sort=tenToaNha,asc` |
| UI lọc tầng | "Tầng 1" đến "Tầng N" | `soTang` của `GET /api/v1/buildings/{maToaNha}`; đúng vì tầng luôn liên tục 1 đến `so_tang` (spec 0011, AC-7) |
| UI dialog thêm căn | danh sách tầng và `maTang` | `GET /api/v1/buildings/{maToaNha}/floors?size=100&sort=soTang,asc` (BQL đã có quyền) |
| UI dialog thêm căn | "Mã căn sẽ tạo" | `maTang` đã chọn nối số căn đệm hai chữ số; chỉ để xem trước, server vẫn tự ghép |
| UI nút theo vai trò | vai trò | `useHoSo().data?.vaiTro` |
| UI căn của tôi | danh sách căn | `useHoSo().data?.phamVi.maCanHo`, mỗi căn gọi `GET /api/v1/apartments/{ma}` |
| UI hiển thị diện tích | `75,50 m²` | `Intl.NumberFormat("vi-VN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })` |

### Key invariants

- `ma_can_ho` = `ma_tang` + số căn hai chữ số `01` đến `99` (CHECK `chk_can_ho_ma`); vì `ma_tang` đã theo `chk_tang_ma`, mã căn luôn chỉ ra đúng tòa, tầng và số căn.
- `0 < dien_tich ≤ 1000`, tối đa 2 chữ số thập phân (CHECK và kiểu cột `NUMERIC(8,2)`; service từ chối số lẻ thứ ba thay vì để DB làm tròn).
- `trang_thai_su_dung` thuộc bốn giá trị (CHECK V2).
- Mã căn và tầng của một căn không bao giờ đổi sau khi tạo; không có API xóa căn.
- Mỗi dòng nhật ký sửa có giá trị cũ đọc dưới `FOR UPDATE` trong cùng transaction với `UPDATE`.
- Mọi đọc ghi căn đi qua phạm vi của người gọi; ngoài phạm vi là 404 như không tồn tại.

### Security model

- Đọc danh sách: `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`, giới hạn theo phạm vi tòa.
- Đọc chi tiết: ba vai trò trên theo phạm vi tòa; `CU_DAN` chỉ căn trong phạm vi căn (đang cư trú hôm nay).
- Tạo, sửa: chỉ `BAN_QUAN_LY`, giới hạn theo phạm vi tòa.
- `KY_THUAT` không có quyền nào với căn hộ (403 ở cả bốn endpoint), dù đọc được tòa và tầng (spec 0011).
- `CanHoResponse` không có dữ liệu cá nhân. Tham số lọc đi vào SQL qua tham số đặt tên; `sort` đi qua bảng cột được phép, nên không có đường chèn SQL.
- Nhật ký kiểm toán cho mọi lần tạo và mọi lần sửa có đổi (AC-14).

### Configuration required

Không có biến môi trường hay khóa bí mật mới.

### Critical test scenarios

- Happy path: BQL tạo căn `A-01` số 5 diện tích 70.5, nhận `A-0105` trạng thái `Trống`; sửa thành 72 và `Đang sử dụng`; `GET /apartments/A-0105` trả giá trị mới; có hai dòng nhật ký, dòng sửa có `cu` 70.50 `moi` 72.00, verifies **AC-5**, **AC-7**, **AC-11**, **AC-14**
- Search (integration, seed): `maToaNha=A&soTang=2` trả 4 căn tầng 2; `dienTichTu=50&dienTichDen=70` trả đúng các căn trong khoảng kể cả hai đầu; `trangThaiSuDung=Trống&trangThaiSuDung=Đã bàn giao` trả 5 căn; `q=03` trả `A-0103`, `A-0203`, `A-0301` đến `A-0304`; `q=%` trả trang rỗng; `sort=trangThaiSuDung,asc` theo *thứ tự trạng thái*, verifies **AC-1**, **AC-2**, **AC-4**, **AC-16**
- Failure case: hai request tạo `A-0105` song song (hai luồng, `CountDownLatch`) cho đúng một 201 và một 409 `CANHO_CAN_HO_DA_TON_TAI`; hai lần sửa song song cho hai dòng nhật ký nối nhau (`moi` của dòng trước là `cu` của dòng sau), verifies **AC-9**, **AC-13**
- Failure case: `dienTich` 0, 1000.01, 75.505, `"75"`, `null` và `soCan` 0, 100, 1.0, `"5"` trả 400 đúng mục; `soTang=2` không có `maToaNha`, `dienTichTu=80&dienTichDen=50`, `dienTichTu=1e2`, `dienTichTu=50,5`, `trangThaiSuDung=Sửa chữa` trả 400 đúng mục; `dienTichTu=` và `trangThaiSuDung=` tính như vắng; `"trangThaiSuDung": 5` trong body trả 400 mục `trangThaiSuDung`; PUT gửi đúng giá trị cũ (75.5 với cột 75.50) trả 200 và không ghi nhật ký; chèn tay `can_ho` mã `A-0100`, `A-02-01` (ở tầng `A-01`), `B-0101` (ở tầng `A-01`) hay diện tích 1500 bị CHECK từ chối, verifies **AC-3**, **AC-8**, **AC-10**, **AC-12**, **AC-14**
- Auth/permission: cư dân 0900000006 xem `A-0101` nhận 200, xem `A-0201` nhận 404; Cư dân gọi `GET /apartments` nhận 403; Kế toán gọi `POST` và `PUT` nhận 403, gọi `GET /apartments/A-0101` nhận 200; Kỹ thuật gọi cả bốn endpoint nhận 403; `maToaNha=ZZ` nhận 404, verifies **AC-2**, **AC-6**, **AC-15**
- UI (Vitest + Testing Library): bộ lọc giữ trên URL và đổi tòa thì bỏ tầng; ô diện tích "75,5" gửi 75.5; dialog thêm căn hiện "Mã căn sẽ tạo: A-0105" và hiện lỗi 409 dưới ô số căn; nút Thêm và Sửa ẩn với Kế toán; trang căn của tôi hiện trạng thái rỗng, một căn và nhiều căn đúng như AC-20, verifies **AC-17**, **AC-18**, **AC-19**, **AC-20**

## Build plan

Thứ tự theo Tracer Bullet: lát đầu chạy thật từ DB tới giao diện cho đường đọc chính, các lát sau làm dày thêm.

1. **Đường mỏng: danh sách căn.** `CanHoResponse`, `CanHoQuery.tim` chỉ với phạm vi tòa, phân trang và `sort` (cả `CASE` trạng thái); `TimCanHoService.timKiem` chưa có bộ lọc; `GET /apartments`; trang `/can-ho` với `DataTable`, bật `sanSang`, icon, mô tả; `pnpm gen:api`. Integration test phân trang, sort và 403 của Cư dân. Satisfies **AC-1**, **AC-4**, **AC-15**, **AC-21**
2. **Tạo căn.** `V16__can_ho.sql`; `MaCanHo`, `NhapLieu` (diện tích, số căn, trạng thái), `CanHoGhi.them`, `ThemCanHoService` với kiểm trùng và dịch lỗi khóa chính; `POST /apartments`; `CANHO_THEM_CAN_HO`, `DoiTuongTacDong.canHo`; dialog "Thêm căn hộ". Unit test service với fake, integration test CHECK và chạy song song. Satisfies **AC-7**, **AC-8**, **AC-9**, **AC-10**, **AC-14**, **AC-15**, **AC-18**, **AC-21**
3. **Bộ lọc và seed.** `R__seed_03_can_ho.sql`, đổi bốn assert ghi cứng số căn seed theo AC-16; `BoLocCanHo`, kiểm tham số và lỗi theo AC-2, AC-3, `WHERE` động trong `CanHoQuery`; thanh lọc trên `/can-ho` với URL. Satisfies **AC-1**, **AC-2**, **AC-3**, **AC-16**, **AC-17**, **AC-21**
4. **Chi tiết và sửa.** `CanHoQuery.cua`, `maToaNhaCua`; `TimCanHoService.chiTiet` với hai luật quyền; `GET /apartments/{id}`; `CanHoGhi.khoaDong`, `capNhat`, `SuaCanHoService`, `PUT /apartments/{id}`, `CANHO_SUA_CAN_HO` với chi tiết cũ mới; trang `/can-ho/[maCanHo]` và dialog "Sửa". Test sửa song song và sửa không đổi. Satisfies **AC-5**, **AC-6**, **AC-11**, **AC-12**, **AC-13**, **AC-14**, **AC-15**, **AC-19**, **AC-21**
5. **Căn hộ của tôi.** Trang `/can-ho-cua-toi` và `/can-ho-cua-toi/[maCanHo]` dùng lại component chi tiết, bật `sanSang`; test ba trạng thái và test `mucCuaDuongDan` để `/can-ho-cua-toi/...` không khớp nhầm `/can-ho`. Satisfies **AC-6**, **AC-19**, **AC-20**, **AC-21**
6. **Hoàn tất.** Sinh lại `openapi.json`, `pnpm gen:api`, `pnpm typecheck`, `pnpm lint`, `mvnw verify`; cập nhật test ma trận và `PhanQuyenCoverageTest`. Satisfies **AC-15**

## Consequences

**Positive**:
- Mã căn luôn đúng tòa và tầng; các phân hệ sau (cư trú, điện nước, phí) có thể đọc tòa và tầng từ mã mà không sợ dữ liệu lệch.
- Nhật ký sửa luôn có giá trị cũ đúng, kể cả khi hai BQL cùng sửa.
- Kế toán và BQT lọc được theo tầng mà không cần mở thêm quyền xem danh sách tầng.

**Negative / tradeoffs**:
- Không có căn đánh số đặc biệt (`A-0105B`, căn ghép, quá 99 căn một tầng); tòa như vậy cần một spec mở rộng mô hình.
- Không đổi được tầng hay mã căn, không xóa được căn: BQL tạo nhầm tầng thì chỉ sửa được trong DB.
- Ghi sau thắng: BQL thứ hai ghi đè thay đổi của người thứ nhất mà không được báo; chỉ nhật ký thấy cả hai.
- Trạng thái `Trống` vẫn đặt được cho căn còn cư dân; dữ liệu có thể mâu thuẫn tới khi UC02 thêm luật.
- Bộ lọc tầng của giao diện dựa vào bất biến tầng liên tục của spec 0011; nếu sau này có tầng hầm hay bỏ số tầng, phải đổi sang lấy danh sách tầng thật.

**Neutral**:
- Seed có 12 căn thay vì 3; test nào đếm số căn seed phải đổi theo.
- Trang chi tiết chưa có nhân khẩu, phương tiện, đồng hồ; UC02, UC03 sẽ thêm trường vào `CanHoResponse` (thêm trường không phá client hiện có).

## Follow-up

- [ ] UC02: quyết luật giữa `trang_thai_su_dung` và hồ sơ cư trú (chặn `Trống` khi còn hồ sơ `Hợp lệ`, hay tự đổi trạng thái khi thêm hoặc kết thúc hồ sơ), qua một service công khai của phân hệ cư trú.
- [ ] UC02, UC03: thêm nhân khẩu đang cư trú, phương tiện, đồng hồ vào chi tiết căn (API-UC01-06 đầy đủ).
- [ ] Tính năng 16 (tạm dừng tiện ích): ghi `bi_tam_dung_tien_ich`; spec này chỉ đọc và hiển thị cờ.
- [ ] Đặc tả Excel ghi trạng thái "Sửa chữa"; DB và spec này dùng "Đang sửa chữa" và thêm "Đã bàn giao". Cập nhật file `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx` cho khớp nếu nhóm cần nộp đặc tả.
- [ ] Ngoài 81 API: xóa căn chưa có dữ liệu liên quan, đổi tầng của căn nhập nhầm (needs a decision).
