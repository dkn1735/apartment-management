# 0016. Phương tiện gửi xe căn hộ: phân hệ `phuongtien` riêng, biển số chuẩn hóa chỉ cấm trùng với xe đang giữ biển, đơn cư dân chờ BQL duyệt qua chung một endpoint đổi trạng thái

**Date**: 2026-11-27
**Status**: Accepted

## Summary

Spec này chốt phần cuối của phân hệ cư trú (UC02): BQL và cư dân đăng ký xe gắn với một chủ xe đang ở hợp lệ tại căn, BQL đổi trạng thái thẻ xe, còn BQL, kế toán, BQT và cư dân xem danh sách xe ngay trên trang căn hộ. Xe do cư dân tự gửi nằm ở trạng thái `Chờ duyệt` cho tới khi BQL duyệt hoặc từ chối qua cùng endpoint đổi trạng thái (`API-UC02-07`), nên cư dân không tự cấp được thẻ xe tính phí. Biển số được server đưa về một dạng chuẩn (chữ in hoa và số, bỏ dấu cách, dấu chấm, gạch nối) và chỉ bị cấm trùng với xe đang hoạt động hoặc tạm dừng (đơn chờ duyệt không giữ chỗ biển, BQL kiểm lại lúc duyệt), nên biển của xe đã hủy được đăng ký lại; xe đạp được để trống biển số. Code nằm trong một phân hệ mới `phuongtien`, gọi sang `cutru`, `auth` và `canho` qua service công khai; bảng `phuong_tien` có sẵn được sửa bằng migration `V20`.

## Requirements

**User stories**:
- Là nhân viên BQL, tôi muốn đăng ký xe cho một người đang ở tại căn và thẻ xe có hiệu lực ngay, để bảo vệ và kế toán dùng được luôn.
- Là cư dân đang ở hợp lệ, tôi muốn tự gửi đơn đăng ký xe cho mình hoặc cho người cùng căn từ `/can-ho-cua-toi`, theo dõi kết quả, xem lý do khi bị từ chối và tự rút đơn khi BQL chưa xử lý.
- Là nhân viên BQL, tôi muốn duyệt, từ chối, tạm dừng, kích hoạt lại hoặc hủy thẻ xe kèm lý do, có nhật ký kiểm toán, để phục vụ đối soát vé tháng ở UC04.
- Là kế toán hoặc thành viên BQT, tôi muốn xem đầy đủ xe đang gửi và lịch sử thẻ xe của một căn (chỉ xem), để đối soát phí gửi xe.

**Thuật ngữ dùng trong spec**:
- *Ngày hôm nay (`homNay`)*: `LocalDate.now(clock)` với bean `Clock` mang múi giờ `app.timezone` (`Asia/Ho_Chi_Minh`), như spec 0014.
- *Thời điểm ghi (`bayGio`)*: `Instant.now(clock).truncatedTo(ChronoUnit.MICROS)`.
- *Năm trạng thái đăng ký*: `'Chờ duyệt'`, `'Đang hoạt động'`, `'Tạm dừng'`, `'Từ chối'`, `'Đã hủy'` (chuỗi NFC).
- *Phương tiện giữ biển*: dòng có `trang_thai_dang_ky` thuộc `'Đang hoạt động'` hoặc `'Tạm dừng'`. Đơn `'Chờ duyệt'` không giữ chỗ biển số: nhiều đơn chờ có thể mang cùng biển, đơn nào được BQL duyệt trước thì giữ biển, các đơn còn lại bị chặn lúc duyệt.
- *Câu lỗi chung*: 403 `FORBIDDEN` và 404 `NOT_FOUND` dùng câu chung đang có của `GlobalExceptionHandler` và `PhamViTruyCap.KHONG_TIM_THAY` (`"Không tìm thấy dữ liệu yêu cầu"`). Lỗi 422 không có mảng `errors` (mảng rỗng) trừ khi AC ghi rõ trường.
- *Biển số dạng chuẩn*: chuỗi người dùng nhập, đổi NFC, viết hoa theo `Locale.ROOT`, bỏ mọi ký tự khoảng trắng Unicode (khớp `[\s\p{Z}]`, kể cả NBSP), dấu chấm `.` và gạch nối `-`; hợp lệ khi khớp `^[A-Z0-9]{5,12}$`. Ví dụ `30a-123.45` thành `30A12345`. Server lưu và trả đúng dạng chuẩn.
- *Cư trú hợp lệ hôm nay tại căn*: có dòng `ho_so_cu_tru` của `(ma_can_ho, ma_cu_dan)` với `trang_thai_ho_so = 'Hợp lệ'`, `ngay_bat_dau <= homNay` và (`ngay_ket_thuc IS NULL` hoặc `ngay_ket_thuc >= homNay`). Cùng nghĩa với `CuTruService.maCanHoDangCuTru` đang cấp phạm vi cho cư dân.
- *Góc nhìn nhân viên*: người gọi có vai trò `BAN_QUAN_LY`, `BAN_QUAN_TRI` hoặc `KE_TOAN` và tòa của căn nằm trong `PhamViTruyCap.maToaNha()`. *Góc nhìn cư dân*: người gọi không đạt góc nhìn nhân viên nhưng có `CU_DAN` và căn nằm trong `PhamViTruyCap.maCanHo()`.
- *Luồng BQL* và *luồng cư dân* khi ghi: người gọi có `BAN_QUAN_LY` đi luồng BQL; người có `CU_DAN` mà không có `BAN_QUAN_LY` đi luồng cư dân (kể cả thành viên BQT hay kế toán đồng thời là cư dân, như tài khoản mẫu `0900000006`).

**Acceptance criteria**:

*Schema*

- **AC-1**: Migration `V20__phuong_tien_gui_xe.sql` sửa bảng `phuong_tien` (không tạo bảng mới, không sửa `V2`):
  - Bỏ ràng buộc UNIQUE cũ `phuong_tien_bien_so_key`; cho `bien_so` nhận `NULL`; chạy `UPDATE phuong_tien SET bien_so = upper(regexp_replace(normalize(bien_so, NFC), '[[:space:].-]', '', 'g'))` để đưa biển cũ (nếu có) về dạng chuẩn trước khi thêm CHECK; thêm `CONSTRAINT chk_phuongtien_bien_so_bat_buoc CHECK (bien_so IS NOT NULL OR loai_phuong_tien = 'Xe đạp')` và `CONSTRAINT chk_phuongtien_bien_so_dinh_dang CHECK (bien_so IS NULL OR bien_so ~ '^[A-Z0-9]{5,12}$')`.
  - Thêm unique index một phần `uq_phuong_tien_bien_so_hieu_luc ON phuong_tien (bien_so) WHERE bien_so IS NOT NULL AND trang_thai_dang_ky IN ('Đang hoạt động', 'Tạm dừng')`.
  - Thay `chk_phuongtien_trangthai` bằng CHECK năm trạng thái đăng ký; giữ `DEFAULT 'Đang hoạt động'` (server luôn ghi tường minh).
  - Thêm cột `ma_cu_dan_dang_ky VARCHAR(30) NULL` (khóa ngoại `fk_phuongtien_cudan_dangky` tới `cu_dan`), `ma_nvbql_cap_nhat VARCHAR(30) NULL` (khóa ngoại `fk_phuongtien_nvbql_capnhat` tới `nhan_vien_bql`), `ly_do_trang_thai VARCHAR(500) NULL`, `tao_luc TIMESTAMPTZ NOT NULL DEFAULT now()`, `cap_nhat_luc TIMESTAMPTZ NOT NULL DEFAULT now()`; mỗi khóa ngoại mới có index riêng (`idx_phuong_tien_ma_cu_dan_dang_ky`, `idx_phuong_tien_ma_nvbql_cap_nhat`). `SchemaMigrationTest` kiểm các ràng buộc này.

*Xem danh sách phương tiện của căn hộ (API-UC02-05)*

- **AC-2**: `GET /api/v1/apartments/{apartmentId}/vehicles`, `@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, CU_DAN })`. `apartmentId` đổi NFC, cắt khoảng trắng, viết hoa `Locale.ROOT`; căn không tồn tại hoặc người gọi không đạt góc nhìn nhân viên lẫn góc nhìn cư dân trả 404 `NOT_FOUND` `"Không tìm thấy dữ liệu yêu cầu"`. Thành công trả 200 `message` `"Danh sách phương tiện của căn hộ thành công"`, `data` là `PhuongTienCanHoResponse { dangGui, donVaLichSu }`, hai mảng không bao giờ `null`, không phân trang:
  - `dangGui`: các dòng `'Đang hoạt động'` và `'Tạm dừng'`, sắp `tao_luc ASC, ma_phuong_tien ASC`.
  - `donVaLichSu`: các dòng `'Chờ duyệt'` lên trước (sắp `tao_luc ASC, ma_phuong_tien ASC`), sau đó tối đa 50 dòng `'Từ chối'` và `'Đã hủy'` mới nhất (sắp `cap_nhat_luc DESC, ma_phuong_tien DESC`; giới hạn 50 áp sau khi lọc theo góc nhìn). Mọi đơn `'Chờ duyệt'` luôn được trả đủ.
  - Mỗi phần tử là `PhuongTienResponse { maPhuongTien, maCanHo, loaiPhuongTien, bienSo?, nhanHieuMauSac?, trangThaiDangKy, maCuDanSoHuu, hoTenChuXe, chuXeConCuTru, maCuDanDangKy?, hoTenNguoiDangKy?, lyDoTrangThai?, maNvbqlCapNhat?, hoTenNguoiCapNhat?, taoLuc, capNhatLuc, coTheRutDon }`. Họ tên cư dân và họ tên BQL cho cả response được đọc theo lô, mỗi loại tối đa một câu SQL (`CuTruDinhDanhService.chiTietCuDanTheoMa`, `hoTenNvbqlTheoMa`).
- **AC-3**: Lọc theo góc nhìn trên `GET .../vehicles` (server gán `null` hoặc bỏ dòng trước khi trả, không chỉ ẩn ở giao diện):
  - Góc nhìn nhân viên: thấy mọi dòng của cả hai mảng với đủ trường. Response không bao giờ chứa số CCCD, số điện thoại hay email.
  - Góc nhìn cư dân: `dangGui` đủ mọi dòng của căn (kèm `hoTenChuXe`); `donVaLichSu` chỉ giữ các dòng mà mã cư dân của người gọi bằng `ma_cu_dan_dang_ky` hoặc `ma_cu_dan_so_huu`. Ở mọi dòng trả cho cư dân, `maNvbqlCapNhat` và `hoTenNguoiCapNhat` là `null`; `lyDoTrangThai` chỉ giữ trên dòng mà người gọi là chủ xe hoặc người gửi đơn (để biết vì sao xe mình bị từ chối hay tạm dừng), các dòng khác đặt `null` (không lộ lý do của hộ khác, ví dụ nợ phí).
  - Ở mọi góc nhìn, `coTheRutDon = true` khi và chỉ khi `trangThaiDangKy = 'Chờ duyệt'`, người gọi có `CU_DAN` và mã cư dân của người gọi bằng `ma_cu_dan_dang_ky` (người gọi không có dòng `cu_dan` thì luôn `false`).
- **AC-4**: Cờ `chuXeConCuTru` trên mỗi dòng bằng `true` khi chủ xe đang cư trú hợp lệ hôm nay tại `maCanHo` của xe, ngược lại `false`; tính cho cả response bằng một lần gọi `CuTruService.maCuDanDangCuTru(maCanHo, homNay)`. Hệ thống không tự đổi trạng thái xe khi hồ sơ cư trú kết thúc (cả khi BQL kết thúc lẫn khi `HoSoCuTruHetHanJob` chạy); cờ này chỉ để BQL thấy và tự xử lý.

*Đăng ký phương tiện (API-UC02-06)*

- **AC-5**: `POST /api/v1/vehicles`, `@ChoPhep({ BAN_QUAN_LY, CU_DAN })`, body `{ maCanHo, maCuDanSoHuu, loaiPhuongTien, bienSo?, nhanHieuMauSac? }` đọc qua `JsonNode`. Luồng BQL hay luồng cư dân xác định theo thuật ngữ ở trên (có `BAN_QUAN_LY` thì đi luồng BQL kể cả khi đồng thời là cư dân). Hợp lệ thì dưới khóa dòng `can_ho` chèn một dòng `phuong_tien`:
  - Luồng BQL: `trang_thai_dang_ky = 'Đang hoạt động'`, `ma_cu_dan_dang_ky = null`, `ma_nvbql_cap_nhat` là mã BQL của người gọi.
  - Luồng cư dân: `trang_thai_dang_ky = 'Chờ duyệt'`, `ma_cu_dan_dang_ky` là mã cư dân của người gọi, `ma_nvbql_cap_nhat = null`.
  - Cả hai: `ly_do_trang_thai = null`, `tao_luc = cap_nhat_luc = bayGio`, `bien_so` ở dạng chuẩn hoặc `null`. Ghi nhật ký theo AC-12, trả 201 `message` `"Đăng ký phương tiện gửi xe thành công"`, `data` là `PhuongTienResponse` theo góc nhìn của người gọi (AC-3).
- **AC-6**: Body sai trả 400 `VALIDATION_FAILED` `"Dữ liệu không hợp lệ"`, một mục `errors` cho mỗi trường sai theo đúng thứ tự `maCanHo`, `maCuDanSoHuu`, `loaiPhuongTien`, `bienSo`, `nhanHieuMauSac`. Mọi chuỗi đổi NFC và cắt khoảng trắng hai đầu trước khi kiểm:
  - `maCanHo` (viết hoa `Locale.ROOT` sau khi cắt) vắng hoặc rỗng: `"Vui lòng chọn căn hộ"`; không phải chuỗi JSON: `"Căn hộ không hợp lệ"`.
  - `maCuDanSoHuu` vắng hoặc rỗng: `"Vui lòng chọn chủ xe"`; không phải chuỗi JSON hoặc (sau khi viết hoa) không khớp `^CD[0-9]{8}$`: `"Chủ xe không hợp lệ"`.
  - `loaiPhuongTien` vắng hoặc rỗng: `"Vui lòng chọn loại phương tiện"`; không phải chuỗi JSON hoặc ngoài `'Ô tô'`, `'Xe máy'`, `'Xe máy điện'`, `'Xe đạp'`: `"Loại phương tiện không hợp lệ"`.
  - `bienSo`: có mặt mà không phải chuỗi JSON (và khác `null`): `"Biển số không hợp lệ"`; rỗng sau khi đưa về dạng chuẩn được coi là vắng; vắng khi loại khác `'Xe đạp'` (hoặc khi `loaiPhuongTien` sai): `"Vui lòng nhập biển số"`; có giá trị mà dạng chuẩn không khớp `^[A-Z0-9]{5,12}$`: `"Biển số chỉ gồm chữ và số, dài từ 5 đến 12 ký tự"`. Xe đạp được để trống hoặc nhập biển (khi nhập thì kiểm như trên).
  - `nhanHieuMauSac`: có mặt, khác `null` mà không phải chuỗi JSON: `"Nhãn hiệu, màu sắc không hợp lệ"`; chỉ gồm khoảng trắng thì thành `null`; dài quá 100 ký tự (tính theo code point): `"Nhãn hiệu, màu sắc tối đa 100 ký tự"`.
- **AC-7**: Thứ tự kiểm của `POST /vehicles`:
  1. Luồng BQL: người gọi không còn dòng `nhan_vien_bql` trả 403 `FORBIDDEN`. Luồng cư dân: người gọi không còn dòng `cu_dan` trả 403 `FORBIDDEN`. Bước này chạy trước kiểm 400.
  2. Kiểm 400 theo AC-6.
  3. Phạm vi: luồng BQL cần tòa của `maCanHo` nằm trong `PhamViTruyCap.maToaNha()`; luồng cư dân cần `maCanHo` nằm trong `PhamViTruyCap.maCanHo()`. Căn không tồn tại hoặc ngoài phạm vi trả 404 `NOT_FOUND`.
  4. Khóa dòng `can_ho` (`CanHoCuTruService.khoaVaDocTrangThai`).
  5. Chủ xe không tồn tại hoặc không cư trú hợp lệ hôm nay tại căn (`CuTruService.dangCuTruHopLe(maCanHo, maCuDanSoHuu, homNay)`): 422 `PHUONGTIEN_CHU_XE_KHONG_CU_TRU` `"Chủ xe không đang cư trú hợp lệ tại căn hộ này"` (lỗi gắn trên trường `maCuDanSoHuu`).
  6. Tài khoản của chủ xe đang `'Ngừng hoạt động'`: 422 `PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG` `"Tài khoản của chủ xe đã ngừng hoạt động, không thể đăng ký hoặc kích hoạt phương tiện"`. Tài khoản `'Hoạt động'` hoặc `'Tạm khóa'` đều được phép.
  7. Chỉ luồng cư dân: căn đã có từ 10 dòng `'Chờ duyệt'` trở lên: 422 `PHUONGTIEN_VUOT_SO_DON_CHO_DUYET` `"Căn hộ đang có tối đa 10 đơn đăng ký xe chờ duyệt, vui lòng chờ Ban quản lý xử lý hoặc rút đơn cũ"`.
  8. `bien_so` khác `null` và đã có một phương tiện giữ biển mang cùng biển số ở bất kỳ căn nào (đơn `'Chờ duyệt'` cùng biển không chặn): 409 `PHUONGTIEN_TRUNG_BIEN_SO`, `message` `"Biển số này đang được đăng ký cho một phương tiện khác"`, kèm đúng một mục `errors` trên trường `bienSo` (giữ vỏ lỗi của spec 0001, không thêm trường mới). Luồng BQL mà tòa của căn đang giữ biển nằm trong `PhamViTruyCap.maToaNha()`: câu của mục lỗi là `"Biển số đang được giữ bởi phương tiện {maPhuongTien} của căn hộ {maCanHo}"`, để BQL mở đúng căn và xử lý đơn nhập nhầm. Mọi trường hợp khác (luồng cư dân): câu của mục lỗi giống `message`, không tiết lộ căn hay mã xe đang giữ biển.
- **AC-8**: Hai request chạy song song cùng đưa một biển số vào trạng thái giữ biển trên hai căn khác nhau (không chung khóa `can_ho`; ví dụ hai BQL cùng đăng ký, hoặc một BQL đăng ký trong lúc BQL khác duyệt đơn cùng biển) cho đúng một thành công và một 409 `PHUONGTIEN_TRUNG_BIEN_SO`: khi bước kiểm của service lọt, index `uq_phuong_tien_bien_so_hieu_luc` chặn lúc ghi và service đổi lỗi SQLSTATE `23505` có tên ràng buộc đó sang đúng mã 409 này qua `common.error.LoiCoSoDuLieu` (ở nhánh chạy đua này, mục lỗi `bienSo` luôn dùng câu chung, kể cả với BQL, vì transaction đã hỏng và không đọc lại dòng đang giữ biển). Việc đổi lỗi nằm ngoài phương thức `@Transactional` (bắt ở lớp gọi, theo mẫu đã có trong `GuiDeNghiCuTruService`), để transaction rollback sạch trước khi trả 409.

*Đổi trạng thái thẻ xe (API-UC02-07)*

- **AC-9**: `PUT /api/v1/vehicles/{vehicleId}/status`, chỉ `@ChoPhep(BAN_QUAN_LY)`, body `{ trangThaiDangKy, lyDo? }` đọc qua `JsonNode`. Kiểm 400 `VALIDATION_FAILED` theo thứ tự `trangThaiDangKy`, `lyDo`:
  - `trangThaiDangKy` vắng hoặc rỗng: `"Vui lòng chọn trạng thái"`; không phải chuỗi JSON hoặc ngoài `'Đang hoạt động'`, `'Tạm dừng'`, `'Từ chối'`, `'Đã hủy'`: `"Trạng thái không hợp lệ"`.
  - `lyDo` đổi NFC, cắt khoảng trắng, chỉ gồm khoảng trắng thì thành `null`; có mặt, khác `null` mà không phải chuỗi JSON: `"Lý do không hợp lệ"`; dài quá 500 ký tự (code point): `"Lý do tối đa 500 ký tự"`; vắng khi `trangThaiDangKy` là `'Tạm dừng'`, `'Từ chối'` hoặc `'Đã hủy'`: `"Vui lòng nhập lý do"`. Khi chuyển sang `'Đang hoạt động'` thì `lyDo` không bắt buộc.
- **AC-10**: Ma trận chuyển trạng thái của `PUT .../status` (mọi cặp khác, kể cả giữ nguyên trạng thái, trả 409 `PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE` `"Không thể chuyển phương tiện từ trạng thái hiện tại sang trạng thái đã chọn"`):

  | Từ | Sang được |
  |---|---|
  | `'Chờ duyệt'` | `'Đang hoạt động'` (duyệt), `'Từ chối'` |
  | `'Đang hoạt động'` | `'Tạm dừng'`, `'Đã hủy'` |
  | `'Tạm dừng'` | `'Đang hoạt động'`, `'Đã hủy'` |
  | `'Từ chối'`, `'Đã hủy'` | không có (trạng thái cuối) |

  Thứ tự kiểm trong một transaction: người gọi còn dòng `nhan_vien_bql` (403, trước kiểm 400); kiểm 400 theo AC-9; `vehicleId` đổi NFC, cắt, viết hoa không khớp `^PT[0-9]{8}$`, xe không tồn tại hoặc tòa của căn ngoài `PhamViTruyCap.maToaNha()` trả 404 `NOT_FOUND` (lần đọc `ma_can_ho` này chưa khóa, chỉ để tìm căn cần khóa; an toàn vì `ma_can_ho` không bao giờ đổi); khóa dòng `can_ho` rồi khóa dòng `phuong_tien` (`SELECT ... FOR UPDATE`); kiểm ma trận trên trạng thái đọc dưới khóa (409); nếu sang `'Đang hoạt động'` thì kiểm lại bước 5 và 6 của AC-7 cho `ma_cu_dan_so_huu` (422 `PHUONGTIEN_CHU_XE_KHONG_CU_TRU` hoặc `PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG`, `errors` rỗng); nếu đi từ `'Chờ duyệt'` sang `'Đang hoạt động'` và `bien_so` khác `null` mà đã có phương tiện giữ biển cùng biển số thì trả 409 `PHUONGTIEN_TRUNG_BIEN_SO` với mục lỗi `bienSo` theo đúng quy tắc câu của bước 8 AC-7 (BQL có phạm vi tòa đó thấy mã xe và mã căn đang giữ biển). Hợp lệ thì cập nhật `trang_thai_dang_ky`, `ly_do_trang_thai = lyDo` (có thể `null` khi kích hoạt), `ma_nvbql_cap_nhat` theo BQL đang gọi, `cap_nhat_luc = bayGio`; ghi nhật ký theo AC-12; trả 200 `message` `"Cập nhật trạng thái thẻ xe/phương tiện thành công"`, `data` là `PhuongTienResponse` góc nhìn nhân viên.

*Cư dân rút đơn*

- **AC-11**: `POST /api/v1/vehicles/{vehicleId}/cancel`, chỉ `@ChoPhep(CU_DAN)`, không nhận body. Trong một transaction: người gọi còn dòng `cu_dan` (403); `vehicleId` sai định dạng, xe không tồn tại, `ma_can_ho` ngoài `PhamViTruyCap.maCanHo()`, hoặc `ma_cu_dan_dang_ky` khác mã cư dân của người gọi đều trả chung 404 `NOT_FOUND`; khóa dòng `can_ho` rồi dòng `phuong_tien` (kiểm lại người gửi, khác thì 404); trạng thái khác `'Chờ duyệt'` trả 409 `PHUONGTIEN_DON_KHONG_CON_CHO_DUYET` `"Đơn đăng ký phương tiện không còn ở trạng thái chờ duyệt"`. Hợp lệ thì đặt `trang_thai_dang_ky = 'Đã hủy'`, `ly_do_trang_thai = null`, `cap_nhat_luc = bayGio` (`ma_nvbql_cap_nhat` giữ `null`), ghi nhật ký theo AC-12, trả 200 `message` `"Rút đơn đăng ký phương tiện thành công"`, `data` là `PhuongTienResponse` góc nhìn cư dân (`coTheRutDon = false`). Người gửi đã thôi cư trú tại căn thì không còn rút được đơn (404, chủ ý); đơn đó chờ BQL từ chối.

*Nhật ký, dữ liệu mẫu, hợp đồng API*

- **AC-12**: Mỗi thao tác ghi thành công ghi đúng một dòng nhật ký qua `NhatKyService.ghi` với `maNguoiThucHien = PhamViTruyCap.maNguoiDung()` và đối tượng `DoiTuongTacDong.phuongTien(maPhuongTien)` (`"PhuongTien:PT..."`): `PHUONGTIEN_DANG_KY` (`"Đăng ký phương tiện"`) cho AC-5, `PHUONGTIEN_DOI_TRANG_THAI` (`"Đổi trạng thái phương tiện"`) cho AC-10, `PHUONGTIEN_RUT_DON` (`"Rút đơn đăng ký phương tiện"`) cho AC-11. Chi tiết là record `ChiTietPhuongTien { maCanHo, maCuDanSoHuu, loaiPhuongTien, bienSo, trangThai { cu, moi }, lyDo }` (`trangThai.cu = null` khi đăng ký, `lyDo` ẩn khỏi JSON khi `null`); không ghi họ tên, CCCD, số điện thoại, email. Request thất bại không ghi nhật ký.
- **AC-13**: `R__seed_05_phuong_tien.sql` (chỉ nạp khi bật profile `seed`, chạy lại an toàn bằng `NOT EXISTS`) thêm xe trên các căn `A-0101`, `A-0103`, `A-0204` (chủ xe là cư dân có hồ sơ hợp lệ tại căn đó trong seed 04, tra theo số điện thoại như seed 04), phủ đủ bốn loại xe (có một xe đạp không biển) và đủ năm trạng thái đăng ký. Xe do BQL tạo hoặc đã qua tay BQL ghi `ma_nvbql_cap_nhat` từ tài khoản BQL của seed 01; đơn `'Chờ duyệt'`, `'Từ chối'` và đơn đã rút ghi `ma_cu_dan_dang_ky`; mọi dòng `'Tạm dừng'`, `'Từ chối'` và dòng hủy bởi BQL có `ly_do_trang_thai`; `mvnw verify` sinh lại `docs/api/openapi.json` có ba endpoint `UC02-05` đến `07` cùng `cancel`, `PhanQuyenCoverageTest` và `ChiTietNhatKyAnToanTest` xanh.

*Giao diện*

- **AC-14**: `ChiTietCanHo` thêm khối `"Phương tiện"` (component `KhoiPhuongTien`) ngay dưới khối hồ sơ cư trú, bên trong có hai tab `"Đang gửi"` và `"Đơn và lịch sử"` (tab thứ hai kèm số đơn `'Chờ duyệt'` mà người xem thấy được, khi lớn hơn 0). Khối hiện cho `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN` trên `/can-ho/[maCanHo]` và cho `CU_DAN` trên `/can-ho-cua-toi`. Mỗi dòng hiện loại xe, biển số (hoặc `"Không có biển"`), nhãn hiệu màu sắc, chủ xe, trạng thái dạng `Badge`, lý do khi có, thời điểm cập nhật (dạng `dd/MM/yyyy HH:mm` theo giờ `Asia/Ho_Chi_Minh`); với góc nhìn nhân viên, dòng có `chuXeConCuTru = false` hiện nhãn cảnh báo `"Chủ xe không còn cư trú"`. Khối có `Skeleton` khi tải, trạng thái rỗng riêng cho từng tab và `QueryErrorState` có nút Thử lại độc lập, không ẩn thông tin căn hộ khi API xe lỗi.
- **AC-15**: Hộp thoại `"Đăng ký phương tiện"` cho `BAN_QUAN_LY` (nút trên `/can-ho/[maCanHo]`) và cho người có `CU_DAN` mà không có `BAN_QUAN_LY` (nút trên `/can-ho-cua-toi`, nhãn `"Gửi đơn đăng ký xe"`, hiện cả khi người đó đồng thời là BQT hay kế toán): ô chọn chủ xe lấy từ mảng `dangCuTru` của `GET /apartments/{id}/residents` (cư dân mặc định chọn chính mình; không lọc theo tài khoản, chủ xe có tài khoản ngừng hoạt động bị server trả 422), ô chọn loại xe, ô biển số (nhãn ghi rõ không bắt buộc khi chọn `'Xe đạp'`), ô nhãn hiệu màu sắc. Kế toán và BQT không thấy nút. Mục `errors` của lỗi 400, 409 `PHUONGTIEN_TRUNG_BIEN_SO` và 422 `PHUONGTIEN_CHU_XE_KHONG_CU_TRU` gắn vào đúng ô (`aria-describedby`, hiện nguyên câu server trả); các lỗi 422 còn lại hiện ở đầu hộp thoại theo `errorCode`; thành công thì đóng hộp thoại, invalidate truy vấn xe của căn và báo `toast`.
- **AC-16**: BQL có hộp thoại `"Đổi trạng thái thẻ xe"` trên từng dòng, chỉ liệt kê các trạng thái đích hợp lệ theo ma trận AC-10 từ trạng thái hiện tại, ô lý do bắt buộc khi đích là `'Tạm dừng'`, `'Từ chối'`, `'Đã hủy'`; cư dân có nút `"Rút đơn"` kèm hộp thoại xác nhận trên dòng có `coTheRutDon = true`. Mọi mã lỗi `PHUONGTIEN_...` có câu tiếng Việt trong `frontend/src/lib/api/error-messages.ts`, giao diện xử lý theo `errorCode`. Toàn khối dùng được bằng bàn phím, focus nhìn thấy được, mọi ô có nhãn, đạt WCAG AA.

## Decision

**Chosen option**: Option 2: phân hệ `phuongtien` riêng, sửa bảng có sẵn bằng `V20`, đơn cư dân đi qua chung endpoint đổi trạng thái.

Phương tiện là một phân hệ ngang hàng mới, dùng đúng bốn endpoint (ba API gốc cộng `cancel` cho cư dân), biển số chuẩn hóa với unique index một phần trên hai trạng thái giữ biển (`Đang hoạt động`, `Tạm dừng`), và mọi bước duyệt, từ chối, tạm dừng, hủy đi qua một ma trận chuyển trạng thái trong `PUT /vehicles/{id}/status`.

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

### Data model sketch

Bảng `phuong_tien` sau `V20` (`*` là cột mới hoặc đổi):

| Cột | Kiểu | Null | Ràng buộc |
|---|---|---|---|
| `ma_phuong_tien` | VARCHAR(30) | không | PK, mặc định `'PT'` cộng 8 chữ số từ `seq_phuong_tien`, `chk_phuong_tien_ma` |
| `ma_can_ho` | VARCHAR(20) | không | FK `can_ho` `ON DELETE RESTRICT`, `idx_phuongtien_canho` |
| `ma_cu_dan_so_huu` | VARCHAR(30) | không | FK `cu_dan`, `idx_phuong_tien_ma_cu_dan_so_huu` |
| `loai_phuong_tien` | VARCHAR(40) | không | `chk_phuongtien_loai` (4 loại); là khóa phân loại biểu phí cho UC04 |
| `bien_so` * | VARCHAR(25) | có | `chk_phuongtien_bien_so_bat_buoc`, `chk_phuongtien_bien_so_dinh_dang`, `uq_phuong_tien_bien_so_hieu_luc` (partial, chỉ `'Đang hoạt động'` và `'Tạm dừng'`) |
| `nhan_hieu_mau_sac` | VARCHAR(100) | có | |
| `trang_thai_dang_ky` * | VARCHAR(40) | không | CHECK năm trạng thái |
| `ma_cu_dan_dang_ky` * | VARCHAR(30) | có | FK `cu_dan`, index; `null` khi BQL đăng ký |
| `ma_nvbql_cap_nhat` * | VARCHAR(30) | có | FK `nhan_vien_bql`, index; BQL tạo hoặc đổi trạng thái gần nhất |
| `ly_do_trang_thai` * | VARCHAR(500) | có | lý do của lần đổi gần nhất |
| `tao_luc` *, `cap_nhat_luc` * | TIMESTAMPTZ | không | `DEFAULT now()` chỉ để migration chạy được trên bảng đang có dữ liệu; ứng dụng luôn ghi từ `Clock` |

Quan hệ: `can_ho` 1:N `phuong_tien`; `cu_dan` 1:N `phuong_tien` theo vai chủ xe và 1:N theo vai người gửi đơn; `nhan_vien_bql` 1:N `phuong_tien` theo người cập nhật gần nhất. Không xóa vật lý dòng `phuong_tien`.

### State transitions

```
BQL đăng ký              ──► Đang hoạt động
Cư dân gửi đơn           ──► Chờ duyệt

Chờ duyệt      ── duyệt (BQL) ─────────► Đang hoạt động
Chờ duyệt      ── từ chối (BQL) ───────► Từ chối        (cuối)
Chờ duyệt      ── rút đơn (người gửi) ─► Đã hủy         (cuối)
Đang hoạt động ── tạm dừng (BQL) ──────► Tạm dừng
Tạm dừng       ── kích hoạt lại (BQL) ─► Đang hoạt động
Đang hoạt động ── hủy (BQL) ───────────► Đã hủy
Tạm dừng       ── hủy (BQL) ───────────► Đã hủy
```

Vào `'Đang hoạt động'` (duyệt hoặc kích hoạt lại) luôn kiểm lại chủ xe còn cư trú hợp lệ hôm nay và tài khoản chưa ngừng hoạt động. `'Từ chối'` và `'Đã hủy'` là trạng thái cuối; muốn gửi lại thì đăng ký dòng mới (biển số đã được giải phóng khỏi index).

### Kiến trúc phân hệ và ranh giới phụ thuộc

- `phuongtien` (mới, package `vn.edu.hust.chungcu.phuongtien`):
  - `controller`: `PhuongTienCanHoController` (`GET /api/v1/apartments/{apartmentId}/vehicles`), `PhuongTienController` (`POST /api/v1/vehicles`, `PUT /{vehicleId}/status`, `POST /{vehicleId}/cancel`).
  - `dto`: `PhuongTienResponse`, `PhuongTienCanHoResponse`, `DangKyPhuongTienRequest`, `DoiTrangThaiPhuongTienRequest` (record cho OpenAPI; controller đọc `JsonNode` như cư trú).
  - `repository`: `PhuongTienQuery` (đọc theo căn, khóa dòng, đếm đơn chờ, tìm dòng đang giữ biển), `PhuongTienGhi` (chèn `RETURNING ma_phuong_tien`, cập nhật trạng thái), viết bằng `JdbcClient` như `cutru`.
  - `service`: `TraCuuPhuongTienService`, `DangKyPhuongTienService`, `DoiTrangThaiPhuongTienService`, `RutDonPhuongTienService`, `NhapLieuPhuongTien` (kiểm 400), `BienSo` (chuẩn hóa), `MaTranTrangThai` (ma trận AC-10), `HienThiPhuongTien` (lọc theo góc nhìn AC-3), `KiemChuXe` (bước 5 và 6 của AC-7, dùng chung cho đăng ký và kích hoạt), `PhuongTienErrorCodes`, `PhuongTienLoi`, `ChiTietPhuongTien`.
- Phụ thuộc một chiều, không tạo vòng bean: `phuongtien` gọi `cutru.CuTruService` (thêm hai phương thức công khai `dangCuTruHopLe(maCanHo, maCuDan, ngay)` và `maCuDanDangCuTru(maCanHo, ngay)`, vẫn chỉ phụ thuộc `HoSoCuTruRepository`), `auth.CuTruDinhDanhService` (`maNvbqlCua`, `maCuDanCua`, `chiTietCuDanTheoMa`, `hoTenNvbqlTheoMa`), `auth.PhamViTruyCapService`, `canho.CanHoCuTruService` (`maToaNhaCua`, `khoaVaDocTrangThai`). Không phân hệ nào gọi ngược vào `phuongtien` ở lát này; UC04 sau này đọc xe qua một service công khai của `phuongtien`.
- `nhatky`: `LoaiThaoTac` thêm ba mã của AC-12; `DoiTuongTacDong` thêm `phuongTien(maPhuongTien)`.

### API surface

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/apartments/{apartmentId}/vehicles` (UC02-05) | GET | `apartmentId`: path | 200, `PhuongTienCanHoResponse { dangGui, donVaLichSu }` lọc theo góc nhìn | `@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, CU_DAN })` | 404 `NOT_FOUND` |
| `/api/v1/vehicles` (UC02-06) | POST | `maCanHo`, `maCuDanSoHuu`, `loaiPhuongTien` (req); `bienSo` (req trừ xe đạp), `nhanHieuMauSac` (opt) | 201, `PhuongTienResponse` | `@ChoPhep({ BAN_QUAN_LY, CU_DAN })` | 400 `VALIDATION_FAILED`, 403, 404, 409 `PHUONGTIEN_TRUNG_BIEN_SO`, 422 `PHUONGTIEN_CHU_XE_KHONG_CU_TRU` / `PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG` / `PHUONGTIEN_VUOT_SO_DON_CHO_DUYET` |
| `/api/v1/vehicles/{vehicleId}/status` (UC02-07) | PUT | `trangThaiDangKy` (req); `lyDo` (req khi tạm dừng, từ chối, hủy) | 200, `PhuongTienResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400, 403, 404, 409 `PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE`, 422 hai mã chủ xe |
| `/api/v1/vehicles/{vehicleId}/cancel` | POST | `vehicleId`: path | 200, `PhuongTienResponse` | `@ChoPhep(CU_DAN)` | 403, 404, 409 `PHUONGTIEN_DON_KHONG_CON_CHO_DUYET` |

### Value sourcing

| Action | Value produced / displayed | Source |
|---|---|---|
| Mọi luồng | `homNay`, `bayGio` | bean `Clock` (`app.timezone`), như thuật ngữ |
| Đăng ký | `ma_phuong_tien` | `seq_phuong_tien`, lấy qua `INSERT ... RETURNING ma_phuong_tien` |
| Đăng ký | `bien_so` lưu trữ | `BienSo.chuanHoa(bienSo)` từ input |
| Đăng ký | `trang_thai_dang_ky` ban đầu | luồng của người gọi: có `BAN_QUAN_LY` thì `'Đang hoạt động'`; có `CU_DAN` mà không có `BAN_QUAN_LY` thì `'Chờ duyệt'` |
| Đăng ký (cư dân) | `ma_cu_dan_dang_ky` | `CuTruDinhDanhService.maCuDanCua(phamVi.maNguoiDung())` |
| Đăng ký (BQL), đổi trạng thái | `ma_nvbql_cap_nhat` | `CuTruDinhDanhService.maNvbqlCua(phamVi.maNguoiDung())` |
| Đăng ký, kích hoạt | chủ xe cư trú hợp lệ hôm nay | `CuTruService.dangCuTruHopLe(maCanHo, maCuDanSoHuu, homNay)` |
| Đăng ký, kích hoạt | trạng thái tài khoản chủ xe | `trangThaiTaiKhoan` trong `CuTruDinhDanhService.chiTietCuDanTheoMa` |
| Đăng ký (cư dân) | số đơn chờ của căn | `PhuongTienQuery.demChoDuyet(maCanHo)` dưới khóa dòng `can_ho` |
| Đăng ký, duyệt đơn | biển đang bị giữ, kèm `maPhuongTien`, `maCanHo` của dòng giữ biển (cho câu lỗi của BQL) | `PhuongTienQuery.timBienConHieuLuc(bienSo)` trả dòng giữ biển; tòa của căn đó qua `CanHoCuTruService.maToaNhaCua`; chốt chặn cuối là `uq_phuong_tien_bien_so_hieu_luc` |
| Đổi trạng thái, rút đơn | `ly_do_trang_thai` | input `lyDo` (đổi trạng thái); `null` khi rút đơn hoặc kích hoạt không kèm lý do |
| Xem danh sách | `hoTenChuXe`, `hoTenNguoiDangKy` | `CuTruDinhDanhService.chiTietCuDanTheoMa` gom một lần |
| Xem danh sách | `hoTenNguoiCapNhat` | `CuTruDinhDanhService.hoTenNvbqlTheoMa` gom một lần; `null` cho góc nhìn cư dân |
| Xem danh sách | `chuXeConCuTru` | `ma_cu_dan_so_huu` thuộc `CuTruService.maCuDanDangCuTru(maCanHo, homNay)` |
| Xem danh sách | góc nhìn | `PhamViTruyCap.vaiTro()`, `maToaNha()` (qua `CanHoCuTruService.maToaNhaCua`), `maCanHo()` |
| Xem danh sách | mã cư dân người gọi để lọc `donVaLichSu` (góc nhìn cư dân) và tính `coTheRutDon` (mọi góc nhìn, khi người gọi có `CU_DAN`) | `CuTruDinhDanhService.maCuDanCua(phamVi.maNguoiDung())` |
| UI đăng ký | danh sách chủ xe chọn được | mảng `dangCuTru` của `GET /apartments/{id}/residents` (spec 0014), trường `maCuDan`, `hoTen`; không lọc theo trạng thái tài khoản vì response đó không có trường này, server chặn bằng 422 ở bước 6 của AC-7 |
| UI đổi trạng thái | các trạng thái đích | ma trận AC-10 nhân bản trong `frontend/src/lib/phuong-tien.ts` từ `trangThaiDangKy` hiện tại |
| Nhật ký | `trangThai.cu` | trạng thái đọc dưới khóa trước khi cập nhật; `null` khi đăng ký |

### Key invariants

- Tại mọi thời điểm, mỗi biển số dạng chuẩn có tối đa một phương tiện giữ biển (`'Đang hoạt động'` hoặc `'Tạm dừng'`) trên toàn hệ thống (mức service và mức DB qua `uq_phuong_tien_bien_so_hieu_luc`).
- Mọi xe không phải `'Xe đạp'` đều có biển số; mọi biển số lưu trữ đều ở dạng chuẩn (`chk_phuongtien_bien_so_bat_buoc`, `chk_phuongtien_bien_so_dinh_dang`).
- Một xe chỉ vào `'Đang hoạt động'` khi chủ xe đang cư trú hợp lệ hôm nay tại căn của xe và tài khoản chưa ngừng hoạt động, kiểm tại thời điểm chuyển.
- `'Từ chối'` và `'Đã hủy'` là trạng thái cuối; không có đường đi vào `'Chờ duyệt'` ngoài lúc cư dân gửi đơn.
- Mỗi căn có tối đa 10 đơn `'Chờ duyệt'` do cư dân gửi (kiểm dưới khóa dòng `can_ho`). Không giới hạn tổng số xe mỗi căn hay mỗi chủ xe (chủ ý: BQL kiểm soát khi đăng ký và duyệt).
- Mọi transaction ghi `phuong_tien` khóa dòng `can_ho` trước rồi mới khóa dòng `phuong_tien`, cùng thứ tự với cư trú, nên không deadlock.
- Không xóa vật lý dòng `phuong_tien`; `maCanHo` và `maCuDanSoHuu` không đổi sau khi tạo.

### Security model

- Đọc: nhân viên `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN` theo tòa trong phạm vi; cư dân theo căn đang cư trú hợp lệ hôm nay. Ngoài phạm vi luôn 404 để không lộ sự tồn tại của căn.
- Ghi: đăng ký cho `BAN_QUAN_LY` (theo tòa) và `CU_DAN` (theo căn, luôn vào `'Chờ duyệt'`); đổi trạng thái chỉ `BAN_QUAN_LY`; rút đơn chỉ người gửi đơn. `BAN_QUAN_TRI` và `KE_TOAN` chỉ xem.
- Tối thiểu hóa dữ liệu cá nhân: response xe không bao giờ chứa CCCD, số điện thoại, email; cư dân không thấy tên nhân viên BQL, không thấy đơn và lịch sử xe không liên quan tới mình. Biển số là dữ liệu nghiệp vụ, hiện cho mọi người có quyền xem căn.
- Lỗi trùng biển chỉ nói mã xe và mã căn đang giữ biển cho BQL có phạm vi tòa đó; cư dân chỉ nhận câu chung.
- Nhật ký kiểm toán bắt buộc cho mọi lần đăng ký, đổi trạng thái, rút đơn; chỉ ghi mã định danh và trường nghiệp vụ xe (AC-12).

### Configuration required

Không thêm biến môi trường hay thuộc tính cấu hình mới. Giới hạn 10 đơn chờ là hằng số trong `DangKyPhuongTienService`, giống giới hạn đề nghị cư trú của spec 0015.

### Critical test scenarios

- Happy path (integration): BQL đăng ký ô tô biển `30a-123.45` cho chủ hộ `A-0101` nhận 201, `bienSo = "30A12345"`, `'Đang hoạt động'`; cư dân `A-0101` gửi đơn xe máy cho người cùng căn nhận 201 `'Chờ duyệt'`; BQL duyệt qua `PUT .../status` nhận 200; `GET /apartments/A-0101/vehicles` có hai xe trong `dangGui`; có đủ ba dòng nhật ký, verifies **AC-2**, **AC-5**, **AC-10**, **AC-12**
- Failure case (trùng biển và chạy song song): đăng ký lại `30A12345` ở căn khác nhận 409 `PHUONGTIEN_TRUNG_BIEN_SO`; sau khi BQL hủy xe đầu, đăng ký lại biển đó nhận 201; hai luồng `CountDownLatch` cùng biển trên hai căn cho đúng một 201 và một 409; xe đạp không biển nhận 201, xe máy không biển nhận 400; hai cư dân ở hai căn cùng gửi đơn một biển đều nhận 201, BQL duyệt đơn đầu nhận 200, duyệt đơn sau nhận 409 kèm mã xe và mã căn đang giữ biển; chèn SQL tay hai dòng `Đang hoạt động` cùng biển bị `V20` từ chối, verifies **AC-1**, **AC-6**, **AC-7**, **AC-8**
- Failure case (vòng đời): kích hoạt lại xe `'Tạm dừng'` khi chủ xe đã hết cư trú nhận 422 `PHUONGTIEN_CHU_XE_KHONG_CU_TRU`; chuyển `'Đã hủy'` sang `'Đang hoạt động'` nhận 409; tạm dừng thiếu lý do nhận 400; cư dân gửi đơn thứ 11 nhận 422; cư dân rút đơn đã được duyệt nhận 409; đăng ký cho chủ xe tài khoản `'Ngừng hoạt động'` nhận 422; hồ sơ cư trú kết thúc thì xe giữ nguyên trạng thái và `chuXeConCuTru = false`, verifies **AC-4**, **AC-7**, **AC-9**, **AC-10**, **AC-11**
- Auth/permission: kế toán và BQT xem được cả hai mảng nhưng gọi `POST`, `PUT` nhận 403; cư dân gọi `PUT .../status` nhận 403; cư dân thấy `dangGui` đủ, `donVaLichSu` chỉ đơn liên quan tới mình, `hoTenNguoiCapNhat = null`; cư dân căn khác gọi `GET` nhận 404; cư dân rút đơn của người khác nhận 404, verifies **AC-2**, **AC-3**, **AC-11**
- UI (Vitest + Testing Library): khối `Phương tiện` hiện đúng theo vai trò trên hai trang; kế toán không thấy nút; hộp thoại đăng ký gắn lỗi trùng biển vào ô biển số; hộp thoại đổi trạng thái chỉ liệt kê đích hợp lệ và bắt lý do; nhãn `"Chủ xe không còn cư trú"` chỉ hiện với nhân viên; lỗi API xe hiện nút Thử lại mà không ẩn thông tin căn hộ, verifies **AC-14**, **AC-15**, **AC-16**

## Build plan

Thứ tự theo Tracer Bullet và quy trình tách backend trước, frontend sau của scope (3 mốc backend rồi 2 mốc frontend):

1. **Backend đường mỏng: V20, xem danh sách và BQL đăng ký.** Viết `V20__phuong_tien_gui_xe.sql` và cập nhật `SchemaMigrationTest`; tạo package `phuongtien` với `BienSo`, `NhapLieuPhuongTien`, `KiemChuXe`, `HienThiPhuongTien`, `PhuongTienQuery`, `PhuongTienGhi`, `TraCuuPhuongTienService`, `DangKyPhuongTienService` (luồng BQL), `GET /apartments/{id}/vehicles`, `POST /vehicles`; thêm `CuTruService.dangCuTruHopLe`, `maCuDanDangCuTru`; ánh xạ `uq_phuong_tien_bien_so_hieu_luc` qua `LoiCoSoDuLieu`; nhật ký `PHUONGTIEN_DANG_KY`, `DoiTuongTacDong.phuongTien`. Unit test `BienSo`, `NhapLieuPhuongTien`; integration test đọc theo góc nhìn, đăng ký, trùng biển, chạy song song. Satisfies **AC-1**, **AC-2**, **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-7**, **AC-8**, **AC-12**
2. **Backend làm dày: đơn cư dân, đổi trạng thái, rút đơn.** Thêm luồng cư dân vào `DangKyPhuongTienService` (giới hạn 10 đơn), `MaTranTrangThai`, `DoiTrangThaiPhuongTienService`, `RutDonPhuongTienService`, `PUT /vehicles/{id}/status`, `POST /vehicles/{id}/cancel`, hai mã nhật ký còn lại. Unit test ma trận; integration test duyệt, từ chối, tạm dừng, kích hoạt lại khi chủ xe đã đi, rút đơn, quyền. Satisfies **AC-5**, **AC-7**, **AC-9**, **AC-10**, **AC-11**, **AC-12**
3. **Backend dữ liệu mẫu và khóa OpenAPI.** Viết `R__seed_05_phuong_tien.sql`; chạy `PhanQuyenCoverageTest`, `ChiTietNhatKyAnToanTest` và `mvnw verify` để sinh `docs/api/openapi.json`. Satisfies **AC-13**
4. **Frontend đường mỏng hiển thị: khối Phương tiện.** Chạy `pnpm gen:api`; thêm `frontend/src/lib/phuong-tien.ts` (hook truy vấn, nhãn trạng thái, ma trận), `khoi-phuong-tien.tsx` với hai tab, tích hợp vào `ChiTietCanHo` cho hai trang; `Skeleton`, rỗng, `QueryErrorState`; test hiển thị theo vai trò. Satisfies **AC-14**
5. **Frontend thao tác và hoàn tất.** Viết `dang-ky-phuong-tien-dialog.tsx`, `doi-trang-thai-xe-dialog.tsx`, `rut-don-xe-dialog.tsx`, câu lỗi trong `error-messages.ts`; kiểm bàn phím, focus, WCAG AA; chạy `pnpm typecheck`, `pnpm lint`, `pnpm format:check`, `pnpm test`, `pnpm build`. Satisfies **AC-15**, **AC-16**

## Consequences

**Positive**:
- Cư dân không tự cấp được thẻ xe tính phí; mọi thẻ đang hoạt động đều qua tay BQL và gắn với người đang ở thật tại căn, nên UC04 có dữ liệu tin được để tính phí gửi xe theo `loai_phuong_tien`.
- Biển số dạng chuẩn cộng unique index một phần chặn trùng biển kể cả khi nhập khác kiểu (`30A-123.45` và `30a12345`) hay khi hai request chạy đua, mà vẫn cho dùng lại biển của xe đã hủy.
- Phân hệ `phuongtien` riêng giữ `cutru` gọn, có tiền tố mã lỗi riêng và một điểm vào rõ ràng cho UC04.

**Negative / tradeoffs**:
- Xe không tự dừng khi chủ xe chuyển đi; BQL phải tự nhìn nhãn cảnh báo và xử lý, nên có thể còn thẻ hoạt động cho người đã đi tới khi BQL để ý.
- Không sửa được thông tin xe; nhập nhầm thì phải hủy rồi đăng ký lại (hai thao tác, hai dòng nhật ký).
- Một người vừa là BQL vừa là cư dân tự đăng ký xe cho mình sẽ vào thẳng `'Đang hoạt động'`; nhật ký vẫn ghi rõ người làm.
- Mọi thao tác xe của cùng căn xếp hàng qua khóa dòng `can_ho` chung với cư trú (số thao tác mỗi căn rất nhỏ).

**Neutral**:
- Phần "phân loại biểu phí gửi xe" trong mô tả `API-UC02-06` nằm ngoài lát này: không tạo bảng phí nào, `loai_phuong_tien` là khóa để UC04 tra biểu phí sau.
- `V20` đổi ràng buộc trên bảng có sẵn; `DEFAULT now()` của hai cột thời gian chỉ để migration chạy trên dữ liệu cũ.
- Không thêm trang mới trong `muc-chuc-nang.ts`; khối xe nằm trong trang chi tiết căn hộ đã có.
- Code thuộc package mới `phuongtien` nhưng vẫn commit trên nhánh `feat/cutru` (cùng UC02), phạm vi commit `feat(phuongtien): ...`.

## Follow-up

- [ ] Sửa thông tin xe (nhãn hiệu màu sắc, biển số) cho BQL: cân nhắc thêm vào mục Deferred của scope (ngoài 81 API).
- [ ] Tra cứu phương tiện toàn tòa (đã có trong Deferred): dùng lại `PhuongTienQuery` và `HienThiPhuongTien` khi làm.
- [ ] UC04 Tài chính: đọc xe `'Đang hoạt động'` theo `loai_phuong_tien` qua một service công khai của `phuongtien`; cân nhắc tự tạm dừng thẻ xe khi căn nợ phí.
- [ ] Cân nhắc tự tạm dừng xe khi hồ sơ cư trú của chủ xe kết thúc (event sau commit từ `cutru`), nếu nhãn cảnh báo không đủ trong thực tế.
- [ ] Đặc tả Excel của `API-UC02-05` đến `07` dùng payload mẫu chung; cập nhật file Excel nếu nhóm cần nộp tài liệu khớp code (thêm endpoint `cancel` và trạng thái `Chờ duyệt`, `Từ chối`).
