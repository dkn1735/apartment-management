# 0015. Đề nghị cư trú và tra cứu hồ sơ: lưu chung trên `ho_so_cu_tru` với người xác minh rỗng khi chờ duyệt, đối chiếu cặp CCCD kèm số điện thoại và lọc phân trang toàn tòa

**Date**: 2026-11-26
**Status**: Accepted

## Summary

Spec này chốt phần thứ hai của phân hệ cư trú (UC02): cư dân đang ở hợp lệ tại căn hộ gửi đề nghị thêm nhân khẩu hoặc đăng ký tạm trú ngay trên web ở trạng thái chờ xác minh, tự theo dõi kết quả hoặc tự rút lại khi chưa xử lý, còn Ban quản lý (BQL) dùng màn hình tập trung `/ho-so-cu-tru` để lọc hồ sơ toàn tòa có phân trang và duyệt hoặc từ chối đề nghị. Toàn bộ vòng đời đề nghị lưu trực tiếp trên bảng `ho_so_cu_tru` (cho phép cột nhân viên xác minh để trống lúc mới gửi) thay vì tách bảng phụ, nhờ vậy một mã hồ sơ đi xuyên suốt từ lúc đề nghị tới khi có hiệu lực và màn hình tra cứu chỉ đọc trên một nguồn dữ liệu duy nhất. Khi đề nghị cho người khác, cư dân nhập khớp đồng thời số căn cước công dân và số điện thoại của tài khoản cư dân đã có để không phải mở danh sách tìm kiếm tài khoản toàn khu và chống đoán mò danh tính.

## Requirements

**User stories**:
- Là cư dân đang cư trú hợp lệ hôm nay tại căn hộ, tôi muốn gửi đề nghị thêm thành viên gia đình hoặc khách thuê (hoặc đăng ký đợt cư trú mới cho chính mình) từ `/can-ho-cua-toi`, xem lý do nếu bị từ chối và tự hủy được khi đề nghị còn chờ xác minh.
- Là nhân viên BQL, tôi muốn mở màn hình tập trung `/ho-so-cu-tru` để lọc hồ sơ cư trú trên các tòa thuộc phạm vi có phân trang, nhìn thấy ngay hàng đợi chờ xác minh và duyệt hoặc từ chối đề nghị mà không phải mở từng căn hộ.
- Là nhân viên BQL, tôi muốn thêm, sửa và kết thúc hồ sơ cư trú ngay từ màn hình `/ho-so-cu-tru` bằng cùng bộ hộp thoại của trang căn hộ, với đầy đủ nhật ký kiểm toán truy vết được.
- Là cư dân sống chung căn hộ với người khác, tôi muốn các đề nghị cư trú chỉ hiển thị cho người gửi và người được đề nghị mà không lộ số căn cước, số điện thoại hay email cá nhân trên đường truyền mạng.

**Thuật ngữ dùng trong spec**:
- *Ngày hôm nay (`homNay`)*: `LocalDate.now(clock)` với bean `Clock` mang múi giờ `app.timezone` (`Asia/Ho_Chi_Minh`, quy ước từ spec 0001 và spec 0014).
- *Năm trạng thái hồ sơ hợp lệ*: `'Chờ xác minh'`, `'Hợp lệ'`, `'Hết hiệu lực'`, `'Từ chối'`, `'Đã hủy'` (chuỗi NFC).
- *Đề nghị cư trú*: dòng `ho_so_cu_tru` do cư dân gửi qua `API-UC02-04`, luôn có `ma_cu_dan_de_nghi IS NOT NULL` và khởi đầu ở `'Chờ xác minh'`; khi BQL duyệt thì chuyển sang `'Hợp lệ'`, khi BQL từ chối thì chuyển sang `'Từ chối'`, khi người gửi tự rút lúc còn chờ thì chuyển sang `'Đã hủy'`.
- *Vai trò cư trú cư dân được đề nghị*: chỉ gồm `'Thành viên gia đình'` và `'Khách thuê'` (vai trò `'Chủ căn hộ'` chỉ do BQL trực tiếp ghi nhận qua `POST /api/v1/residency-records`).

**Acceptance criteria**:

*Cư dân gửi đề nghị cư trú (API-UC02-04)*

- **AC-1**: `POST /api/v1/residency-records/resident-request` với body `{ maCanHo, laChinhToi, soCccd?, soDienThoai?, vaiTroCuTru, tinhTrangCuTru, ngayBatDau, ngayKetThuc?, ghiChuDeNghi? }`, chỉ `@ChoPhep(CU_DAN)`. Ngay đầu hàm (trước kiểm 400), server tra `ma_cu_dan_de_nghi` từ bảng `cu_dan` của `PhamViTruyCap.maNguoiDung()`; nếu người gọi đã bị gỡ dòng `cu_dan` trước khi token hết hạn thì trả 403 `FORBIDDEN`. Mọi trường chuỗi đổi về NFC và cắt khoảng trắng hai đầu (`maCanHo` viết hoa `Locale.ROOT`; `ghiChuDeNghi` chỉ gồm khoảng trắng thì chuẩn hóa thành `null`). Dưới khóa dòng `can_ho`, server chèn dòng vào `ho_so_cu_tru` với `trang_thai_ho_so = 'Chờ xác minh'`, `ma_nvbql_xac_minh = null`, `ma_cu_dan_de_nghi`, `ghi_chu_de_nghi`, `ly_do_tu_choi = null`, `tao_luc` và `cap_nhat_luc` bằng `Instant.now(clock).truncatedTo(ChronoUnit.MICROS)`, không đổi `trang_thai_su_dung` của căn hộ, ghi nhật ký theo AC-13 và trả 201 `message` `"Cư dân gửi yêu cầu cập nhật hồ sơ thành công"`, `data` là `HoSoCuTruResponse` đã ẩn PII theo quyền `CU_DAN` (có `coTheHuy = true`).
- **AC-2**: Dữ liệu đầu vào của `POST /api/v1/residency-records/resident-request` sai trả 400 `VALIDATION_FAILED` `"Dữ liệu không hợp lệ"`, một mục `errors` cho mỗi trường sai theo đúng thứ tự `maCanHo`, `laChinhToi`, `soCccd`, `soDienThoai`, `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau`, `ngayKetThuc`, `ghiChuDeNghi` (đọc qua `JsonNode`):
  - `maCanHo` vắng hoặc rỗng sau chuẩn hóa: `"Vui lòng chọn căn hộ"`; không phải chuỗi JSON: `"Căn hộ không hợp lệ"`.
  - `laChinhToi` vắng hoặc không phải boolean JSON (`isBoolean()`): `"Vui lòng chọn đối tượng cư trú"`.
  - Khi `laChinhToi = false`: `soCccd` vắng hoặc rỗng sau khi bỏ khoảng trắng và gạch nối báo `"Vui lòng nhập số CCCD"`, không phải chuỗi JSON hoặc không khớp `^[0-9]{12}$` báo `"Số CCCD phải gồm đúng 12 chữ số"`; `soDienThoai` vắng hoặc rỗng sau khi bỏ khoảng trắng, dấu chấm, gạch nối và đổi đầu `+84` hoặc `84` (khi phía sau còn chữ số) thành `0` báo `"Vui lòng nhập số điện thoại"`, không phải chuỗi JSON hoặc không khớp `^0[0-9]{9}$` báo `"Số điện thoại không hợp lệ"`. Khi `laChinhToi` khác `false` (tức `true`, vắng hoặc sai kiểu), server bỏ qua hai trường này.
  - `vaiTroCuTru` vắng hoặc rỗng: `"Vui lòng chọn vai trò cư trú"`; không phải chuỗi JSON hoặc khác `'Thành viên gia đình'` và `'Khách thuê'`: `"Cư dân chỉ được đề nghị vai trò Thành viên gia đình hoặc Khách thuê"`.
  - `tinhTrangCuTru` vắng hoặc rỗng: `"Vui lòng chọn tình trạng cư trú"`; không phải chuỗi JSON hoặc khác `'Thường trú'` và `'Tạm trú'`: `"Tình trạng cư trú không hợp lệ"`.
  - `ngayBatDau` vắng hoặc rỗng: `"Vui lòng chọn ngày bắt đầu"`; không đúng định dạng ngày thực `YYYY-MM-DD`: `"Ngày bắt đầu không hợp lệ"`; nhỏ hơn `homNay`: `"Ngày bắt đầu của đề nghị phải từ hôm nay trở đi"`.
  - `ngayKetThuc` có mặt và khác `null` nhưng rỗng hoặc sai định dạng `YYYY-MM-DD`: `"Ngày kết thúc không hợp lệ"`; vắng hoặc `null` khi `tinhTrangCuTru = 'Tạm trú'`: `"Tạm trú bắt buộc có ngày kết thúc"`; cả hai ngày đều đúng định dạng `YYYY-MM-DD` mà `ngayKetThuc < ngayBatDau`: `"Ngày kết thúc phải từ ngày bắt đầu trở đi"` (vẫn gắn lỗi trên `ngayKetThuc` độc lập với việc `ngayBatDau < homNay`).
  - `ghiChuDeNghi` có mặt và khác `null` nhưng không phải chuỗi JSON: `"Ghi chú đề nghị không hợp lệ"`; sau khi đổi NFC và cắt khoảng trắng mà dài quá 500 ký tự (tính theo code point): `"Ghi chú đề nghị tối đa 500 ký tự"`.
- **AC-3**: Sau khi qua kiểm 403 (`cu_dan`) và 400 của `resident-request`, kiểm tra theo đúng thứ tự:
  1. `maCanHo` không nằm trong `PhamViTruyCap.maCanHo()` (căn không tồn tại hoặc người gọi không đang cư trú hợp lệ hôm nay tại căn đó): trả 404 `NOT_FOUND` `"Không tìm thấy dữ liệu yêu cầu"`.
  2. Khóa dòng `can_ho` (`SELECT ... FOR UPDATE`).
  3. Xác định `maCuDan`: nếu `laChinhToi = true` thì dùng chính `ma_cu_dan_de_nghi` (nếu tài khoản đang `'Ngừng hoạt động'` trả 422 `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` `"Tài khoản cư dân đã ngừng hoạt động, không thể ghi nhận hoặc duyệt hồ sơ cư trú"`, dùng thống nhất câu này cho cả `POST /residency-records`, `resident-request`, `approve` và `error-messages.ts`); nếu `laChinhToi = false` thì đối chiếu `CuTruDinhDanhService.timTheoCccdVaSdt(soCccd, soDienThoai)`, khi không tìm thấy dòng `cu_dan` khớp đồng thời cả hai trường hoặc tài khoản tìm được đang `'Ngừng hoạt động'` đều trả chung một mã 422 `CUTRU_KHONG_KHOP_CU_DAN` `"Không tìm thấy tài khoản cư dân khớp đồng thời số CCCD và số điện thoại đã nhập"` (không phân biệt lý do để chống dò tài khoản). Tài khoản `'Hoạt động'` hoặc `'Tạm khóa'` đều được phép.
  4. Đã có dòng `'Chờ xác minh'` cho cùng `(maCanHo, maCuDan)`: trả 409 `CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH` `"Cư dân này đã có đề nghị cư trú đang chờ xác minh tại căn hộ"`.
  5. Số dòng `'Chờ xác minh'` hiện có của `maCanHo` đạt từ 10 trở lên: trả 422 `CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH` `"Căn hộ đang có tối đa 10 đề nghị chờ xác minh, vui lòng chờ Ban quản lý xử lý hoặc hủy đề nghị cũ"`.
  6. Khoảng ngày `daterange(ngay_bat_dau, ngay_ket_thuc, '[]')` giao với một hồ sơ `'Hợp lệ'` đang có của cùng `(maCanHo, maCuDan)`: trả 409 `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN` `"Cư dân đã có hồ sơ cư trú hợp lệ giao khoảng ngày tại căn hộ này"`.

*Duyệt, từ chối và tự hủy đề nghị cư trú*

- **AC-4**: `POST /api/v1/residency-records/{recordId}/approve`, chỉ `@ChoPhep(BAN_QUAN_LY)`, không nhận body. Trong một transaction, kiểm tra theo đúng thứ tự: người gọi còn dòng `nhan_vien_bql` (403); `recordId` chuẩn hóa khớp `^HS[0-9]{8}$`, hồ sơ tồn tại và căn hộ thuộc tòa trong `PhamViTruyCap.maToaNha()` (404); khóa dòng `can_ho` trước rồi khóa dòng `ho_so_cu_tru` (`SELECT ... FOR UPDATE`); `trang_thai_ho_so` khác `'Chờ xác minh'` trả 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH` `"Đề nghị cư trú không còn ở trạng thái chờ xác minh"`; tài khoản gắn với `ma_cu_dan` đang `'Ngừng hoạt động'` trả 422 `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` `"Tài khoản cư dân đã ngừng hoạt động, không thể ghi nhận hoặc duyệt hồ sơ cư trú"`; `ngay_ket_thuc` khác `null` và `ngay_ket_thuc < homNay` trả 422 `CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC` `"Đề nghị cư trú đã qua ngày kết thúc, vui lòng từ chối để cư dân gửi lại"` (nếu `ngay_bat_dau < homNay` nhưng chưa qua `ngay_ket_thuc` vẫn duyệt nguyên trạng, và vẫn cho phép BQL duyệt kể cả khi người gửi ban đầu `ma_cu_dan_de_nghi` đã kết thúc cư trú tại căn); giao khoảng ngày với hồ sơ `'Hợp lệ'` đang có của cùng `(maCanHo, maCuDan)` trả 409 `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN`. Hợp lệ thì cập nhật `trang_thai_ho_so = 'Hợp lệ'`, `ma_nvbql_xac_minh` theo BQL đang gọi, `cap_nhat_luc = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)`, đồng bộ `trang_thai_su_dung` của căn theo luật spec 0014 AC-10, ghi nhật ký theo AC-13 và trả 200 `message` `"Duyệt đề nghị cư trú thành công"`, `data` là `HoSoCuTruResponse` đầy đủ.
- **AC-5**: `POST /api/v1/residency-records/{recordId}/reject`, chỉ `@ChoPhep(BAN_QUAN_LY)`, nhận body `{ lyDoTuChoi }` đọc qua `JsonNode`. Trong một transaction, kiểm tra theo đúng thứ tự: người gọi còn dòng `nhan_vien_bql` (403); kiểm body 400 `VALIDATION_FAILED` mục `lyDoTuChoi` (vắng hoặc rỗng sau khi đổi NFC và cắt khoảng trắng báo `"Vui lòng nhập lý do từ chối"`, không phải chuỗi JSON báo `"Lý do từ chối không hợp lệ"`, dài quá 500 ký tự theo code point báo `"Lý do từ chối tối đa 500 ký tự"`); kiểm `recordId` và phạm vi tòa của căn hộ (404); khóa dòng `can_ho` trước rồi khóa dòng `ho_so_cu_tru`; `trang_thai_ho_so` khác `'Chờ xác minh'` trả 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH`. Hợp lệ thì cập nhật `trang_thai_ho_so = 'Từ chối'`, `ma_nvbql_xac_minh` theo BQL đang gọi, `ly_do_tu_choi`, `cap_nhat_luc = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)` (không đổi trạng thái căn hộ), ghi nhật ký theo AC-13 và trả 200 `message` `"Từ chối đề nghị cư trú thành công"`, `data` là `HoSoCuTruResponse` đầy đủ.
- **AC-6**: `POST /api/v1/residency-records/{recordId}/cancel`, chỉ `@ChoPhep(CU_DAN)`, không nhận body. Trong một transaction, kiểm tra theo đúng thứ tự: người gọi còn dòng `cu_dan` (403); `recordId` không khớp `^HS[0-9]{8}$`, hồ sơ không tồn tại, `ma_can_ho` không nằm trong `PhamViTruyCap.maCanHo()`, hoặc `ma_cu_dan_de_nghi` khác mã cư dân của người gọi đều trả chung 404 `NOT_FOUND` `"Không tìm thấy dữ liệu yêu cầu"`; khóa dòng `can_ho` trước rồi khóa dòng `ho_so_cu_tru` (kiểm lại `ma_cu_dan_de_nghi`, khác thì trả 404); `trang_thai_ho_so` khác `'Chờ xác minh'` trả 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH`. Hợp lệ thì cập nhật `trang_thai_ho_so = 'Đã hủy'`, `cap_nhat_luc = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)` (`ma_nvbql_xac_minh` giữ `null`), ghi nhật ký theo AC-13 và trả 200 `message` `"Hủy đề nghị cư trú thành công"`, `data` là `HoSoCuTruResponse` đã ẩn PII theo quyền `CU_DAN` (`coTheHuy = false`).
- **AC-7**: Tương thích với luồng sửa và hết hạn của spec 0014: trên `PUT /api/v1/residency-records/{recordId}`, khi dòng đang khóa có `trang_thai_ho_so` là `'Từ chối'` hoặc `'Đã hủy'` thì trả 409 `CUTRU_DE_NGHI_DA_KET_THUC` `"Đề nghị cư trú đã kết thúc xử lý, không thể chỉnh sửa"` (`'Chờ xác minh'` vẫn trả `CUTRU_HO_SO_DANG_CHO_XAC_MINH`, `'Hết hiệu lực'` vẫn trả `CUTRU_HO_SO_DA_HET_HIEU_LUC`); khi `PUT` sửa hoặc kết thúc một hồ sơ `'Hợp lệ'` từng đi từ đề nghị cư dân thì giữ nguyên `ma_cu_dan_de_nghi` và `ghi_chu_de_nghi`. `HoSoCuTruHetHanJob` đầu ngày chỉ quét các dòng `trang_thai_ho_so = 'Hợp lệ' AND ngay_ket_thuc < homNay`, không tự đóng các dòng `'Chờ xác minh'`.

*Đọc đề nghị trên căn hộ và tra cứu tập trung toàn tòa*

- **AC-8**: Mở rộng `HoSoCuTruResponse` thêm năm trường (`maCuDanDeNghi`, `hoTenNguoiDeNghi`, `ghiChuDeNghi`, `lyDoTuChoi` đều `nullable = true`, cùng `coTheHuy: boolean`), mở rộng `allowableValues` của `trangThaiHoSo` thành năm trạng thái hợp lệ, và mở rộng `GET /api/v1/apartments/{apartmentId}/residents` trả bốn mảng không bao giờ `null`: `{ dangCuTru, sapBatDau, lichSuCuTru, deNghiCuTru }`. Ba mảng đầu giữ nguyên quy tắc lọc và sắp xếp của spec 0014 (chỉ gồm dòng `'Hợp lệ'` và `'Hết hiệu lực'`). Mảng `deNghiCuTru` gồm các dòng `'Chờ xác minh'`, `'Từ chối'`, `'Đã hủy'` của căn, sắp ưu tiên `'Chờ xác minh'` lên trước (bên trong `'Chờ xác minh'` sắp `tao_luc ASC, ma_ho_so ASC`), tiếp theo các dòng `'Từ chối'` và `'Đã hủy'` sắp `cap_nhat_luc DESC, ma_ho_so DESC`:
  - Người gọi đạt quyền `BAN_QUAN_LY` của tòa: thấy toàn bộ dòng của cả bốn mảng với đủ mọi trường, `coTheHuy = false`.
  - Người gọi chỉ đạt quyền `CU_DAN` của căn: trong `dangCuTru`, `sapBatDau` và `lichSuCuTru`, các trường `maCuDanDeNghi`, `hoTenNguoiDeNghi`, `ghiChuDeNghi`, `lyDoTuChoi` cũng đặt bằng `null` và `coTheHuy = false` (để không lộ người từng đứng đơn cho thành viên cùng căn); trong `deNghiCuTru`, chỉ giữ các dòng mà mã cư dân của người gọi bằng `ma_cu_dan_de_nghi` hoặc bằng `ma_cu_dan`, đặt `maNguoiDung`, `soDienThoai`, `email`, `soCccd`, `maNvbqlXacMinh`, `hoTenNguoiXacMinh` bằng `null`, giữ `maCuDanDeNghi`, `hoTenNguoiDeNghi`, `ghiChuDeNghi`, `lyDoTuChoi`, và đặt `coTheHuy = true` khi và chỉ khi `trangThaiHoSo = 'Chờ xác minh'` và người gọi là `ma_cu_dan_de_nghi`.
- **AC-9**: `GET /api/v1/residency-records?maToaNha&soTang&trangThaiHoSo&vaiTroCuTru&tinhTrangCuTru&q&page&size&sort`, chỉ `@ChoPhep(BAN_QUAN_LY)`, trả 200 `message` `"Tra cứu danh sách hồ sơ cư trú thành công"`, `data` là `PageResponse<HoSoCuTruResponse>` chỉ gồm hồ sơ thuộc các căn hộ nằm trong `PhamViTruyCap.maToaNha()` của người gọi. Phân trang theo spec 0001 (`page` từ 1, `size` mặc định 20, tối đa 100); `sort` nhận `taoLuc`, `capNhatLuc`, `ngayBatDau`, `ngayKetThuc`, `maCanHo` (mặc định `taoLuc,desc`; `ngayKetThuc` đặt `NULLS LAST` khi `asc` và `NULLS FIRST` khi `desc`; khóa phụ luôn là `ma_ho_so` cùng chiều `asc` hoặc `desc` để phân trang ổn định). Thông tin cư dân (gộp cả `ma_cu_dan` và `ma_cu_dan_de_nghi`) và họ tên BQL cho cả trang được đọc theo lô bằng tối đa một câu SQL mỗi loại.
- **AC-10**: Nghĩa và thứ tự kiểm lỗi của bộ lọc trên `GET /api/v1/residency-records` (mọi chuỗi kể cả `page`, `size`, `sort` và từng phần tử `trangThaiHoSo` đều đổi NFC và cắt khoảng trắng hai đầu trước khi kiểm, rỗng sau cắt tính như vắng):
  - Tham số sai trả 400 `VALIDATION_FAILED` `"Dữ liệu không hợp lệ"`, một mục `errors` cho mỗi tham số sai theo thứ tự `page`, `size`, `sort` (qua `TrangYeuCau.tu`), rồi `soTang`, `trangThaiHoSo`, `vaiTroCuTru`, `tinhTrangCuTru`, `q`:
    - `soTang` không khớp `^[1-9][0-9]?$`: `"Số tầng phải là số nguyên từ 1 đến 99"`; có `soTang` mà không có `maToaNha`: `"Chọn tòa nhà trước khi lọc theo tầng"`.
    - `trangThaiHoSo` (lặp lại được, bỏ qua phần tử rỗng, khớp một trong các giá trị là đạt) có phần tử ngoài 5 trạng thái hợp lệ: `"Trạng thái hồ sơ không hợp lệ"`.
    - `vaiTroCuTru` ngoài 3 vai trò hợp lệ: `"Vai trò cư trú không hợp lệ"`.
    - `tinhTrangCuTru` ngoài 2 tình trạng hợp lệ: `"Tình trạng cư trú không hợp lệ"`.
    - `q` dài quá 100 ký tự (theo code point): `"Từ khóa tối đa 100 ký tự"`; khi hợp lệ thì khớp dòng có `ma_cu_dan` thuộc tập cư dân khớp `q` (họ tên không dấu `unaccent`, SĐT đã chuẩn hóa, CCCD đã chuẩn hóa, email, `ma_cu_dan`, kể cả tài khoản `'Ngừng hoạt động'`; nếu tập cư dân khớp rỗng thì bỏ nhánh `ma_cu_dan IN (...)` để không sinh `IN ()`), hoặc `upper(ma_can_ho)` chứa `upper(q)`, hoặc `upper(ma_ho_so)` chứa `upper(q)` (thoát `%`, `_`, `\`).
  - Sau khi không có lỗi 400: nếu có `maToaNha` (đã viết hoa) mà tòa ngoài `PhamViTruyCap.maToaNha()` thì trả 404 `NOT_FOUND` `"Không tìm thấy dữ liệu yêu cầu"`. Endpoint đọc này không gọi `maNvbqlCua` (đồng nhất với các API đọc của spec 0012 và 0014). Nếu phạm vi tòa rỗng hoặc tập căn hộ khớp `(maToaNha, soTang)` rỗng thì trả `PageResponse` rỗng (`totalItems = 0`) mà không chạy câu SQL `IN ()`.

*Cơ sở dữ liệu, phân quyền, nhật ký kiểm toán và dữ liệu mẫu*

- **AC-11**: Migration `V19__de_nghi_cu_tru.sql` chỉnh bảng `ho_so_cu_tru`: bỏ `NOT NULL` trên `ma_nvbql_xac_minh` và đặt lại khóa ngoại `fk_hsct_nvbql` với `ON DELETE RESTRICT`; thêm ba cột `ma_cu_dan_de_nghi VARCHAR(30) NULL`, `ghi_chu_de_nghi VARCHAR(500) NULL`, `ly_do_tu_choi VARCHAR(500) NULL` cùng khóa ngoại `fk_hsct_cudan_denghi` tới `cu_dan(ma_cu_dan) ON DELETE RESTRICT`; thay `chk_hsct_trangthai` bằng tập 5 trạng thái hợp lệ; thêm ba ràng buộc `chk_hsct_xac_minh_theo_trang_thai`, `chk_hsct_nguon_de_nghi`, `chk_hsct_ly_do_tu_choi`; thêm unique index một phần `uq_hsct_cho_xac_minh_canho_cudan` trên `(ma_can_ho, ma_cu_dan) WHERE trang_thai_ho_so = 'Chờ xác minh'`, index khóa ngoại `idx_ho_so_cu_tru_ma_cu_dan_de_nghi` và index `idx_ho_so_cu_tru_trangthai_taoluc` trên `(trang_thai_ho_so, tao_luc DESC)`. Khi hai request gửi đề nghị `'Chờ xác minh'` cho cùng `(ma_can_ho, ma_cu_dan)` chạy đua, vi phạm `uq_hsct_cho_xac_minh_canho_cudan` được dịch qua `LoiCoSoDuLieu` về đúng mã 409 `CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH`. Cập nhật các câu `INSERT` dòng `'Chờ xác minh'` trong `HoSoCuTruXemIntegrationTest` và `HoSoCuTruCapNhatIntegrationTest` của spec 0014 cho thỏa ràng buộc `V19` (`ma_nvbql_xac_minh = NULL`, có `ma_cu_dan_de_nghi` và vai trò hợp lệ).
- **AC-12**: Năm endpoint mới mang đúng `@ChoPhep` như bảng *API surface*: `POST /resident-request` và `POST /{recordId}/cancel` mang `@ChoPhep(CU_DAN)`; `GET /residency-records`, `POST /{recordId}/approve` và `POST /{recordId}/reject` mang `@ChoPhep(BAN_QUAN_LY)`. Vai trò khác (bao gồm `BAN_QUAN_TRI`, `KE_TOAN`, `KY_THUAT`, `BAO_VE`, `QUAN_TRI_VIEN`, hoặc `CU_DAN` gọi API của BQL và ngược lại) nhận 403 `FORBIDDEN`; chưa đăng nhập nhận 401. `PhanQuyenCoverageTest` xanh.
- **AC-13**: Mỗi thao tác ghi đề nghị thành công ghi đúng một dòng nhật ký sau commit qua `NhatKyService.ghi` trên đối tượng `HoSoCuTru:<maHoSo>`, không chứa họ tên, SĐT, email, CCCD hay nội dung văn bản tự do `ghiChuDeNghi` của cư dân (vượt qua `ChiTietNhatKyAnToanTest`). Bổ sung bốn giá trị `LoaiThaoTac`:
  - `CUTRU_GUI_DE_NGHI` (`"Gửi đề nghị cư trú"`): `{ maCanHo, maCuDan, maCuDanDeNghi, vaiTroCuTru, tinhTrangCuTru, ngayBatDau, ngayKetThuc, coGhiChu: boolean }`.
  - `CUTRU_DUYET_DE_NGHI` (`"Duyệt đề nghị cư trú"`): `{ maCanHo, maCuDan, maCuDanDeNghi, vaiTroCuTru, tinhTrangCuTru, ngayBatDau, ngayKetThuc, trangThaiCanHo? }` (`trangThaiCanHo` dạng `{ cu, moi }` khi căn tự đổi trạng thái sử dụng).
  - `CUTRU_TU_CHOI_DE_NGHI` (`"Từ chối đề nghị cư trú"`): `{ maCanHo, maCuDan, maCuDanDeNghi, lyDoTuChoi }`.
  - `CUTRU_HUY_DE_NGHI` (`"Hủy đề nghị cư trú"`): `{ maCanHo, maCuDan, maCuDanDeNghi }`.
- **AC-14**: Mở rộng `db/seed/R__seed_04_cu_tru.sql` (an toàn khi chạy lại bằng `NOT EXISTS` so theo `(ma_can_ho, ma_cu_dan, trang_thai_ho_so)` để không trùng với dòng lịch sử `'Hết hiệu lực'` sẵn có của `0900000009` tại `A-0101`) thêm hai dòng đề nghị mẫu tại căn `A-0101` do cư dân `0900000006` đứng đơn (`ma_cu_dan_de_nghi`): một dòng `'Chờ xác minh'` cho cư dân `0900000010` (`'Khách thuê'`, `'Tạm trú'`, từ `2027-01-01` đến `2027-06-30`, có `ghi_chu_de_nghi`, `ma_nvbql_xac_minh = NULL`, mốc `2026-01-02 09:00:00+07`) và một dòng `'Từ chối'` cho cư dân `0900000009` (`'Thành viên gia đình'`, `'Tạm trú'`, từ `2026-06-01` đến `2026-12-31`, `ma_nvbql_xac_minh` là BQL `0900000002`, có `ly_do_tu_choi`, mốc `2026-01-02 10:00:00+07`). Cập nhật các assert đếm dòng trong `SchemaMigrationTest` nếu bị ảnh hưởng.

*Giao diện*

- **AC-15**: Mở rộng `KhoiHoSoCuTru` trên trang chi tiết căn hộ và `/can-ho-cua-toi`:
  - Thêm tab thứ ba `"Đề nghị cư trú"` (hiển thị kèm con số dòng `'Chờ xác minh'` trong ngoặc trên nhãn tab khi lớn hơn 0), trình bày danh sách `deNghiCuTru` gồm họ tên người được đề nghị, vai trò, tình trạng, khoảng ngày, họ tên người gửi, ghi chú đề nghị, badge trạng thái hồ sơ, lý do từ chối (khi `'Từ chối'`) và người xác minh (chỉ hiện với `BAN_QUAN_LY`); khi rỗng hiện trạng thái rỗng rõ ràng theo vai trò.
  - Trên trang cư dân (`!laBql`): thanh tiêu đề khối `"Hồ sơ cư trú"` có nút `"Gửi đề nghị cư trú"`, mở `GuiDeNghiCuTruDialog` gồm chọn đối tượng (`"Chính tôi"` hoặc `"Người khác"`, mặc định `"Người khác"`), hai ô số CCCD và số điện thoại (chỉ hiện và bắt buộc khi chọn `"Người khác"`), vai trò cư trú (`'Thành viên gia đình'`, `'Khách thuê'`), tình trạng cư trú, ngày bắt đầu (mặc định hôm nay theo giờ `Asia/Ho_Chi_Minh`), ngày kết thúc (bắt buộc khi `'Tạm trú'`) và ghi chú đề nghị tùy chọn; mỗi dòng có `coTheHuy = true` hiện nút `"Hủy đề nghị"` mở hộp thoại xác nhận trước khi gọi `POST .../cancel`.
  - Trên trang nhân viên (`laBql`): mỗi dòng `'Chờ xác minh'` trong tab `"Đề nghị cư trú"` hiện hai nút `"Duyệt"` (mở hộp thoại xác nhận tóm tắt thông tin đề nghị rồi gọi `POST .../approve`) và `"Từ chối"` (mở `TuChoiDeNghiDialog` có ô `lyDoTuChoi` bắt buộc rồi gọi `POST .../reject`).
- **AC-16**: Bật `sanSang: true` cho mục `/ho-so-cu-tru` trong `frontend/src/lib/phan-quyen/muc-chuc-nang.ts` (giữ `vaiTro: ["BAN_QUAN_LY"]`, thêm icon và mô tả `"Tra cứu hồ sơ cư trú toàn tòa và xử lý đề nghị chờ xác minh"`). Trang `/ho-so-cu-tru` có:
  - Thanh lọc nhanh giữa `"Tất cả hồ sơ"` và `"Chờ xác minh"` (con số trong ngoặc trên `"Chờ xác minh"` chỉ hiện khi `totalItems` của `GET /api/v1/residency-records?trangThaiHoSo=Chờ xác minh&size=1` lớn hơn 0; bấm `"Chờ xác minh"` chỉ đặt `trangThaiHoSo=Chờ xác minh` và đưa `page` về 1, bấm `"Tất cả hồ sơ"` chỉ xóa `trangThaiHoSo` và đưa `page` về 1, giữ nguyên các bộ lọc khác trên URL; `"Chờ xác minh"` sáng khi `trangThaiHoSo` chỉ gồm đúng `'Chờ xác minh'`, `"Tất cả hồ sơ"` sáng khi không lọc `trangThaiHoSo`).
  - Bộ lọc đồng bộ trên URL (`maToaNha`, `soTang` chỉ bật khi đã chọn tòa, nhóm `trangThaiHoSo`, `vaiTroCuTru`, `tinhTrangCuTru`, `q`, `page`, `size`, `sort`; giá trị mặc định `page=1`, `size=20`, `sort=taoLuc,desc` không ghi lên URL; đổi bộ lọc về trang 1; đổi hoặc bỏ tòa thì bỏ tầng). Khi URL không có `sort`, bảng vẫn nhận `{ field: "taoLuc", dir: "desc" }` để cột Tạo lúc mang `aria-sort="descending"` ngay lúc vào trang (bấm lại cột mặc định đảo chiều `desc` sang `asc` và ngược lại).
  - Bảng `DataTable` gồm chín cột: Mã hồ sơ, Căn hộ (link tới `/can-ho/[maCanHo]`), Cư dân (họ tên, mã cư dân, SĐT, CCCD), Vai trò và tình trạng, Thời gian cư trú (`ngayBatDau` đến `ngayKetThuc` hoặc `"Không thời hạn"`), Trạng thái (badge kèm dòng phụ nêu họ tên người gửi và ghi chú đề nghị khi có, lý do từ chối khi `'Từ chối'`, họ tên người xác minh khi có), Tạo lúc, Cập nhật lúc và Hành động; sắp xếp được trên năm cột `taoLuc`, `capNhatLuc`, `ngayBatDau`, `ngayKetThuc`, `maCanHo` (có `aria-sort`), kèm trạng thái `Skeleton`, trạng thái rỗng và `QueryErrorState`.
- **AC-17**: Thao tác của BQL trên `/ho-so-cu-tru`: đầu trang có nút `"Thêm hồ sơ cư trú"` (mở `ThemHoSoCuTruDialog` ở chế độ chọn tòa nhà rồi tìm chọn căn hộ qua combobox truyền `q` có trì hoãn tới `GET /api/v1/apartments?maToaNha=...&q=...&size=50&sort=maCanHo,asc`, đổi tòa thì xóa `maCanHo` đang chọn); trên mỗi dòng hiện đúng hành động theo trạng thái và ngày hôm nay (`Asia/Ho_Chi_Minh`): dòng `'Chờ xác minh'` có `"Duyệt"` và `"Từ chối"`, dòng `'Hợp lệ'` hiện `"Sửa"` khi `ngayKetThuc == null || ngayKetThuc >= homNay` và hiện nút đóng mang nhãn `"Hủy hồ sơ"` khi `ngayBatDau > homNay` hoặc `"Kết thúc cư trú"` ở các trường hợp còn lại (tái sử dụng hộp thoại của lát 13), dòng `'Hết hiệu lực'`, `'Từ chối'` và `'Đã hủy'` không có nút thao tác ghi. Thành công ở bất kỳ hộp thoại nào đều hiện toast tiếng Việt tương ứng, đóng hộp thoại và invalidate gốc `HO_SO_CU_TRU_QUERY_KEY` (phủ cả chi tiết căn, danh sách `/ho-so-cu-tru` và số đếm chờ xác minh), `CAN_HO_QUERY_KEY` và `HO_SO_QUERY_KEY`. Lỗi 400 gắn dưới ô nhập, lỗi 409, 422 và 404 hiện trong `FormAlert`.
- **AC-18**: Toàn bộ trang `/ho-so-cu-tru`, tab `"Đề nghị cư trú"`, thanh lọc và các hộp thoại gửi, duyệt, từ chối, hủy đề nghị dùng được trọn vẹn bằng bàn phím, focus nhìn thấy rõ, mọi ô nhập có nhãn, lỗi gắn qua `aria-describedby`, trả focus về nút mở sau khi đóng hộp thoại, đạt chuẩn WCAG AA.

## Decision

**Chosen option**: Option 2: Lưu trực tiếp vòng đời đề nghị trên `ho_so_cu_tru` (cho phép `ma_nvbql_xac_minh` rỗng khi chưa xác minh), định danh cư dân bằng tùy chọn bản thân hoặc cặp `(soCccd, soDienThoai)` và tách ba endpoint hành động `approve`, `reject`, `cancel` dưới khóa dòng `can_ho`.

Đề nghị cư trú và hồ sơ chính thức dùng chung một bảng và một kiểu phản hồi duy nhất, giúp BQL lọc toàn tòa lẫn xử lý hàng đợi trên `/ho-so-cu-tru` bằng một câu truy vấn phân trang mà vẫn khóa chặt toàn vẹn trạng thái ngay trong PostgreSQL.

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

### Data model sketch

Không tạo bảng mới. Migration `V19__de_nghi_cu_tru.sql` mở rộng bảng `ho_so_cu_tru`:

| Bảng | Khóa chính | Cột và tính bắt buộc | Quan hệ | Thay đổi ở V19 |
|---|---|---|---|---|
| `ho_so_cu_tru` | `ma_ho_so` `VARCHAR(30)`, mặc định `'HS' \|\| lpad(nextval('seq_ho_so_cu_tru')::text, 8, '0')` | Giữ từ V2 và V18: `ma_can_ho` `VARCHAR(20) NOT NULL`, `ma_cu_dan` `VARCHAR(30) NOT NULL`, `vai_tro_cu_tru` `VARCHAR(40) NOT NULL`, `tinh_trang_cu_tru` `VARCHAR(40) NOT NULL`, `ngay_bat_dau` `DATE NOT NULL`, `ngay_ket_thuc` `DATE NULL`, `trang_thai_ho_so` `VARCHAR(40) NOT NULL`, `tao_luc` `TIMESTAMPTZ NOT NULL`, `cap_nhat_luc` `TIMESTAMPTZ NOT NULL`. Chỉnh và thêm ở V19: `ma_nvbql_xac_minh` `VARCHAR(30) NULL`, `ma_cu_dan_de_nghi` `VARCHAR(30) NULL`, `ghi_chu_de_nghi` `VARCHAR(500) NULL`, `ly_do_tu_choi` `VARCHAR(500) NULL` | N:1 `can_ho`, N:1 `cu_dan` (`ma_cu_dan`), N:1 `nhan_vien_bql` (`ma_nvbql_xac_minh`), N:1 `cu_dan` (`ma_cu_dan_de_nghi`), tất cả `ON DELETE RESTRICT` | Bỏ `NOT NULL` trên `ma_nvbql_xac_minh`, đặt `ON DELETE RESTRICT` cho `fk_hsct_nvbql`; thêm 3 cột đề nghị và `fk_hsct_cudan_denghi`; mở rộng `chk_hsct_trangthai` lên 5 giá trị; thêm `chk_hsct_xac_minh_theo_trang_thai`, `chk_hsct_nguon_de_nghi`, `chk_hsct_ly_do_tu_choi`, `uq_hsct_cho_xac_minh_canho_cudan`, `idx_ho_so_cu_tru_ma_cu_dan_de_nghi`, `idx_ho_so_cu_tru_trangthai_taoluc` |
| `can_ho` | `ma_can_ho` `VARCHAR(20)` | Không đổi cột | 1:N `ho_so_cu_tru` | Không đổi cấu trúc; chỉ đồng bộ `trang_thai_su_dung` khi dòng chuyển sang `'Hợp lệ'` hoặc `'Hết hiệu lực'` |
| `cu_dan`, `nhan_vien_bql`, `nguoi_dung_he_thong` | Như V1 | Chỉ đọc qua `CuTruDinhDanhService` của `auth` | 1:N `ho_so_cu_tru` | Không đổi cấu trúc |

DDL chính trong `V19__de_nghi_cu_tru.sql`:

```sql
ALTER TABLE ho_so_cu_tru
    ALTER COLUMN ma_nvbql_xac_minh DROP NOT NULL,
    DROP CONSTRAINT fk_hsct_nvbql,
    ADD CONSTRAINT fk_hsct_nvbql FOREIGN KEY (ma_nvbql_xac_minh)
        REFERENCES nhan_vien_bql (ma_nv) ON DELETE RESTRICT,
    ADD COLUMN ma_cu_dan_de_nghi VARCHAR(30) NULL,
    ADD COLUMN ghi_chu_de_nghi VARCHAR(500) NULL,
    ADD COLUMN ly_do_tu_choi VARCHAR(500) NULL,
    ADD CONSTRAINT fk_hsct_cudan_denghi FOREIGN KEY (ma_cu_dan_de_nghi)
        REFERENCES cu_dan (ma_cu_dan) ON DELETE RESTRICT,
    DROP CONSTRAINT chk_hsct_trangthai,
    ADD CONSTRAINT chk_hsct_trangthai CHECK (
        trang_thai_ho_so IN ('Chờ xác minh', 'Hợp lệ', 'Hết hiệu lực', 'Từ chối', 'Đã hủy')),
    ADD CONSTRAINT chk_hsct_xac_minh_theo_trang_thai CHECK (
        (trang_thai_ho_so IN ('Chờ xác minh', 'Đã hủy') AND ma_nvbql_xac_minh IS NULL)
        OR (trang_thai_ho_so IN ('Hợp lệ', 'Hết hiệu lực', 'Từ chối') AND ma_nvbql_xac_minh IS NOT NULL)),
    ADD CONSTRAINT chk_hsct_nguon_de_nghi CHECK (
        trang_thai_ho_so NOT IN ('Chờ xác minh', 'Từ chối', 'Đã hủy')
        OR (ma_cu_dan_de_nghi IS NOT NULL AND vai_tro_cu_tru IN ('Thành viên gia đình', 'Khách thuê'))),
    ADD CONSTRAINT chk_hsct_ly_do_tu_choi CHECK (
        ((trang_thai_ho_so = 'Từ chối' AND ly_do_tu_choi IS NOT NULL AND btrim(ly_do_tu_choi) <> '')
         OR (trang_thai_ho_so <> 'Từ chối' AND ly_do_tu_choi IS NULL))
        AND (ghi_chu_de_nghi IS NULL OR btrim(ghi_chu_de_nghi) <> ''));

CREATE UNIQUE INDEX uq_hsct_cho_xac_minh_canho_cudan
    ON ho_so_cu_tru (ma_can_ho, ma_cu_dan)
    WHERE (trang_thai_ho_so = 'Chờ xác minh');

CREATE INDEX idx_ho_so_cu_tru_ma_cu_dan_de_nghi ON ho_so_cu_tru (ma_cu_dan_de_nghi);
CREATE INDEX idx_ho_so_cu_tru_trangthai_taoluc ON ho_so_cu_tru (trang_thai_ho_so, tao_luc DESC);
```

### State transitions

Bảng chuyển trạng thái đầy đủ của `ho_so_cu_tru` (gộp cả lát 13 và lát 14):

| Trạng thái hiện tại | Hành động và người thực hiện | Trạng thái đích | Hệ quả trên dữ liệu và căn hộ |
|---|---|---|---|
| Chưa có dòng | Cư dân gọi `POST /residency-records/resident-request` | `'Chờ xác minh'` | Ghi `ma_cu_dan_de_nghi`, `ma_nvbql_xac_minh = NULL`; không đổi `can_ho.trang_thai_su_dung` |
| Chưa có dòng | BQL gọi `POST /residency-records` | `'Hợp lệ'` | Ghi `ma_nvbql_xac_minh`, `ma_cu_dan_de_nghi = NULL`; đồng bộ `can_ho.trang_thai_su_dung` |
| `'Chờ xác minh'` | BQL gọi `POST /residency-records/{id}/approve` | `'Hợp lệ'` | Gán `ma_nvbql_xac_minh`, kiểm lại không chồng ngày và chưa qua `ngay_ket_thuc`; đồng bộ `can_ho.trang_thai_su_dung` |
| `'Chờ xác minh'` | BQL gọi `POST /residency-records/{id}/reject` | `'Từ chối'` (kết thúc) | Gán `ma_nvbql_xac_minh` và `ly_do_tu_choi`; không đổi trạng thái căn hộ |
| `'Chờ xác minh'` | Người gửi gọi `POST /residency-records/{id}/cancel` | `'Đã hủy'` (kết thúc) | Giữ `ma_nvbql_xac_minh = NULL`; giải phóng chỗ trên `uq_hsct_cho_xac_minh_canho_cudan` |
| `'Hợp lệ'` | BQL gọi `PUT /residency-records/{id}` giữ `'Hợp lệ'` | `'Hợp lệ'` | Cập nhật trường cư trú và `ma_nvbql_xac_minh`, giữ nguyên `ma_cu_dan_de_nghi`; đồng bộ căn hộ |
| `'Hợp lệ'` | BQL gọi `PUT` đặt `'Hết hiệu lực'` hoặc job đầu ngày chạy | `'Hết hiệu lực'` (kết thúc) | Đóng hồ sơ; đồng bộ `can_ho.trang_thai_su_dung` |
| `'Hết hiệu lực'`, `'Từ chối'`, `'Đã hủy'` | Mọi thao tác sửa, duyệt, từ chối, hủy | Bị chặn 409 | Trạng thái cuối cùng, không mở lại |

### Kiến trúc phân hệ và ranh giới phụ thuộc

- `auth`: mở rộng `CuTruDinhDanhService` (vẫn chỉ phụ thuộc `CuTruDinhDanhQuery`, không tạo vòng bean):
  - `timTheoCccdVaSdt(String soCccd, String soDienThoai)` trả `Optional<ThongTinCuDan>` khớp chính xác cả hai cột.
  - `maCuDanKhopTuKhoa(String q)` trả `Set<String>` mã cư dân khớp từ khóa `q` (tái sử dụng `CuTruDinhDanhService.tuKhoa`, không lọc bỏ tài khoản `'Ngừng hoạt động'` và không giới hạn 20 dòng, phục vụ lọc `GET /residency-records`).
- `canho`: mở rộng `CanHoCuTruService` thêm `maCanHoThuocBoLoc(Collection<String> maToaNhaPhamVi, String maToaNha, Integer soTang)` trả `Set<String>` mã căn hộ thuộc phạm vi và bộ lọc không gian (nhờ vậy `HoSoCuTruQuery` chỉ truy vấn bảng `ho_so_cu_tru`, không đọc trực tiếp bảng `can_ho` hay `tang`).
- `cutru`: bổ sung `GuiDeNghiCuTruService`, `DuyetDeNghiCuTruService`, `TuChoiDeNghiCuTruService`, `HuyDeNghiCuTruService`, `DanhSachHoSoCuTruService` cùng các DTO request và hằng mã lỗi mới trong `CuTruErrorCodes`; mở rộng `HoSoCuTruController`, `HoSoCuTruQuery`, `HoSoCuTruGhi`, `DungHoSoCuTru`, `TraCuuHoSoCuTruService`, `CapNhatHoSoCuTruService`.
- `nhatky`: `LoaiThaoTac` thêm 4 hằng của AC-13.

### API surface

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/residency-records/resident-request` (UC02-04) | POST | `maCanHo`, `laChinhToi`, `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau` (req); `soCccd`, `soDienThoai` (req khi `laChinhToi=false`); `ngayKetThuc`, `ghiChuDeNghi` (opt) | 201, `HoSoCuTruResponse` (ẩn PII, `coTheHuy=true`) | `@ChoPhep(CU_DAN)` | 400 `VALIDATION_FAILED`, 404 `NOT_FOUND`, 409 `CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH` / `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN`, 422 `CUTRU_KHONG_KHOP_CU_DAN` / `CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH` / `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` |
| `/api/v1/residency-records/{recordId}/approve` | POST | `recordId`: path string (req) | 200, `HoSoCuTruResponse` | `@ChoPhep(BAN_QUAN_LY)` | 404 `NOT_FOUND`, 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH` / `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN`, 422 `CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC` / `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` |
| `/api/v1/residency-records/{recordId}/reject` | POST | `recordId`: path; `lyDoTuChoi`: string 1..500 (req) | 200, `HoSoCuTruResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 404 `NOT_FOUND`, 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH` |
| `/api/v1/residency-records/{recordId}/cancel` | POST | `recordId`: path string (req) | 200, `HoSoCuTruResponse` (ẩn PII, `coTheHuy=false`) | `@ChoPhep(CU_DAN)` | 404 `NOT_FOUND`, 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH` |
| `/api/v1/residency-records` | GET | `maToaNha`, `soTang`, `trangThaiHoSo` (lặp được), `vaiTroCuTru`, `tinhTrangCuTru`, `q`, `page`, `size`, `sort` (opt) | 200, `PageResponse<HoSoCuTruResponse>` | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 403 `FORBIDDEN`, 404 `NOT_FOUND` |
| `/api/v1/apartments/{apartmentId}/residents` (UC02-01, mở rộng) | GET | `apartmentId`: path string (req) | 200, `HoSoCuTruCanHoResponse { dangCuTru, sapBatDau, lichSuCuTru, deNghiCuTru }` | `@ChoPhep(BAN_QUAN_LY, CU_DAN)` | 403 `FORBIDDEN`, 404 `NOT_FOUND` |
| `/api/v1/residency-records/{recordId}` (UC02-03, bổ sung luật) | PUT | như spec 0014 | 200, `HoSoCuTruResponse` | `@ChoPhep(BAN_QUAN_LY)` | thêm 409 `CUTRU_DE_NGHI_DA_KET_THUC` cho dòng `'Từ chối'` và `'Đã hủy'` |

### Value sourcing

| Action | Value produced / displayed | Source |
|---|---|---|
| Cư dân gửi hoặc tự hủy đề nghị | `ma_cu_dan_de_nghi` (và mã người gọi để kiểm quyền hủy) | `CuTruDinhDanhService.maCuDanCua(phamVi.maNguoiDung())` tra bảng `cu_dan` |
| Cư dân gửi đề nghị (`laChinhToi = true`) | `ma_cu_dan` của hồ sơ | lấy bằng chính `ma_cu_dan_de_nghi` của người đang gọi |
| Cư dân gửi đề nghị (`laChinhToi = false`) | `ma_cu_dan` của hồ sơ | `CuTruDinhDanhService.timTheoCccdVaSdt(soCccd, soDienThoai)` sau khi chuẩn hóa hai chuỗi nhập |
| Duyệt hoặc từ chối đề nghị | `ma_nvbql_xac_minh` | `CuTruDinhDanhService.maNvbqlCua(phamVi.maNguoiDung())` tra bảng `nhan_vien_bql` |
| Đọc hồ sơ (mọi endpoint trả `HoSoCuTruResponse`) | `hoTenNguoiDeNghi` và `hoTenNguoiXacMinh` | tra chung theo lô qua `CuTruDinhDanhService.chiTietCuDanTheoMa` và `hoTenNvbqlTheoMa` sau khi lọc bỏ phần tử `null` khỏi tập mã (`Objects::nonNull`); đặt `null` trên ba mảng nhân khẩu khi người xem chỉ có quyền `CU_DAN` |
| Đọc hồ sơ cho `CU_DAN` | `coTheHuy` | biểu thức boolean trên server: `trang_thai_ho_so == 'Chờ xác minh' && maCuDanNguoiGoi.equals(ma_cu_dan_de_nghi)` (với `BAN_QUAN_LY` luôn là `false`) |
| Tra cứu `/api/v1/residency-records` | tập `ma_can_ho` thuộc phạm vi và bộ lọc tòa, tầng | `CanHoCuTruService.maCanHoThuocBoLoc(phamVi.maToaNha(), maToaNha, soTang)` |
| Tra cứu `/api/v1/residency-records` có `q` | tập `ma_cu_dan` khớp thông tin cá nhân | `CuTruDinhDanhService.maCuDanKhopTuKhoa(q)` kết hợp `OR` với `LIKE` trên `ma_can_ho` và `ma_ho_so` (bỏ nhánh `IN` khi tập rỗng) |
| Màn hình `/ho-so-cu-tru` | con số đếm trên nút lọc nhanh `"Chờ xác minh"` | trường `totalItems` của truy vấn `GET /api/v1/residency-records?trangThaiHoSo=Chờ xác minh&size=1` (chỉ hiển thị khi lớn hơn 0) |
| Bảng `/ho-so-cu-tru` (client) | điều kiện hiện nút `"Sửa"` và nhãn `"Hủy hồ sơ"` / `"Kết thúc cư trú"` cho dòng `'Hợp lệ'` | so với ngày hôm nay theo múi giờ `Asia/Ho_Chi_Minh` trên trình duyệt: hiện `"Sửa"` khi `ngayKetThuc == null \|\| ngayKetThuc >= homNay`; nút đóng ghi `"Hủy hồ sơ"` khi `ngayBatDau > homNay`, còn lại ghi `"Kết thúc cư trú"` |
| Hộp thoại thêm hồ sơ mở từ `/ho-so-cu-tru` | danh sách tòa nhà và căn hộ trong combobox | tái sử dụng `GET /api/v1/buildings?size=100&sort=tenToaNha,asc` và `GET /api/v1/apartments?maToaNha=...&q=...&size=50&sort=maCanHo,asc` (gửi từ khóa `q` có trì hoãn, đổi tòa thì xóa `maCanHo`) |

### Key invariants

- Giữ nguyên toàn bộ bất biến khoảng ngày, một chủ căn hộ và đồng bộ trạng thái sử dụng của spec 0014; chỉ dòng `'Hợp lệ'` mới tính vào nhân khẩu đang cư trú và ràng buộc loại trừ `EXCLUDE USING gist`.
- Một dòng `'Chờ xác minh'` hoặc `'Đã hủy'` luôn có `ma_nvbql_xac_minh IS NULL` và `ma_cu_dan_de_nghi IS NOT NULL`; một dòng `'Hợp lệ'`, `'Hết hiệu lực'` hoặc `'Từ chối'` luôn có `ma_nvbql_xac_minh IS NOT NULL` (`chk_hsct_xac_minh_theo_trang_thai`).
- Mọi dòng `'Từ chối'` bắt buộc có `ly_do_tu_choi` không rỗng; bốn trạng thái còn lại bắt buộc `ly_do_tu_choi IS NULL` (`chk_hsct_ly_do_tu_choi`).
- Tại mỗi thời điểm, mỗi cặp `(ma_can_ho, ma_cu_dan)` có tối đa một dòng `'Chờ xác minh'` (`uq_hsct_cho_xac_minh_canho_cudan`), và mỗi căn hộ có tối đa 10 dòng `'Chờ xác minh'` đang mở (kiểm dưới khóa dòng `can_ho`).
- Mọi thao tác ghi trên đề nghị (`resident-request`, `approve`, `reject`, `cancel`) đều khóa dòng `can_ho` trước rồi mới khóa dòng `ho_so_cu_tru`, khớp đúng thứ tự khóa của spec 0014 nên không bao giờ xảy ra deadlock giữa cư dân và BQL.
- Không xóa vật lý bất kỳ đề nghị cư trú nào; từ chối và tự hủy đều lưu lại trạng thái kết thúc để truy vết.

### Security model

- Tính năng tiếp tục xử lý dữ liệu định danh cá nhân nhạy cảm (số CCCD 12 chữ số, số điện thoại, email, quan hệ nơi ở).
- Chống lộ danh bạ và chống dò danh tính cư dân: cư dân không có API tìm kiếm tài khoản; khi gửi đề nghị cho người khác phải nhập khớp đồng thời cả số CCCD và số điện thoại trên cùng một dòng `cu_dan`, và mọi lý do không khớp (CCCD chưa tồn tại, SĐT thuộc người khác, chỉ là nhân viên không có dòng `cu_dan`, tài khoản `'Ngừng hoạt động'`) đều trả chung một thông báo 422 `CUTRU_KHONG_KHOP_CU_DAN`. Phản hồi 201 của `resident-request` cũng ẩn toàn bộ `soCccd`, `soDienThoai`, `email`, `maNguoiDung` để người gọi không đọc ngược được email của người khác.
- Cách ly đề nghị giữa người ở cùng căn hộ: khi người gọi chỉ mang quyền `CU_DAN` tại căn, server chỉ trả các phần tử `deNghiCuTru` mà chính người gọi là người gửi (`ma_cu_dan_de_nghi`) hoặc người được đề nghị (`ma_cu_dan`), ẩn thông tin người gửi trên ba mảng nhân khẩu còn lại, và trả 404 khi cư dân khác cùng căn thử gọi `cancel` trên mã hồ sơ không phải do mình gửi.
- Phạm vi nhân viên: `GET /api/v1/residency-records` chỉ mở cho `BAN_QUAN_LY` và giới hạn nghiêm ngặt trong `PhamViTruyCap.maToaNha()` (ngoài phạm vi trả 404); hai endpoint ghi `approve` và `reject` kiểm thêm người gọi còn dòng `nhan_vien_bql` (mất dòng trả 403).
- Nhật ký kiểm toán bắt buộc cho cả bốn hành động gửi, duyệt, từ chối, hủy, không ghi PII và chỉ ghi cờ boolean `coGhiChu` thay cho chuỗi `ghiChuDeNghi` tự do.

### Configuration required

Không yêu cầu thêm biến môi trường, khóa bí mật hay thuộc tính cấu hình mới (tái sử dụng `app.timezone` và `app.cutru.residency-expiry-cron` đã có ở spec 0014).

### Critical test scenarios

- Happy path (integration): cư dân `0900000006` tại `A-0101` gửi `POST /residency-records/resident-request` cho người khác bằng cặp `(soCccd, soDienThoai)` hợp lệ nhận 201 ở `'Chờ xác minh'` (không lộ email trong JSON); BQL gọi `GET /residency-records?trangThaiHoSo=Chờ xác minh` thấy đề nghị, gọi `POST .../approve` nhận 200 chuyển sang `'Hợp lệ'`, đồng bộ trạng thái căn nếu bắt đầu hôm nay và ghi đủ hai dòng nhật ký `CUTRU_GUI_DE_NGHI`, `CUTRU_DUYET_DE_NGHI`, verifies **AC-1**, **AC-4**, **AC-8**, **AC-9**, **AC-13**
- Happy path từ chối, tự hủy và gửi lại (integration): cư dân gửi đề nghị rồi tự gọi `POST .../cancel` nhận 200 `'Đã hủy'`, sau đó gửi lại đề nghị mới cho cùng cư dân tại căn đó nhận 201 (nhờ unique index chỉ chặn dòng đang `'Chờ xác minh'`); BQL gọi `POST .../reject` kèm `lyDoTuChoi` nhận 200 `'Từ chối'`, cư dân gọi `GET /apartments/A-0101/residents` thấy `lyDoTuChoi` và `coTheHuy = false`, verifies **AC-1**, **AC-5**, **AC-6**, **AC-8**, **AC-11**, **AC-13**
- Failure case (đồng thời và lệch thời gian): hai luồng `CountDownLatch` gồm BQL bấm `approve` và cư dân bấm `cancel` cùng lúc trên một đề nghị `'Chờ xác minh'` trả đúng một 200 và một 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH`; đề nghị gửi hôm trước nay có `ngay_bat_dau < homNay` nhưng `ngay_ket_thuc >= homNay` vẫn duyệt 200, còn đề nghị đã qua `ngay_ket_thuc < homNay` khi duyệt trả 422 `CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC` và không bị `HoSoCuTruHetHanJob` tự đóng; gọi `PUT /residency-records/{id}` trên dòng `'Từ chối'` hoặc `'Đã hủy'` trả 409 `CUTRU_DE_NGHI_DA_KET_THUC`, verifies **AC-4**, **AC-5**, **AC-6**, **AC-7**, **AC-11**
- Failure case (chống dò danh tính, vai trò và giới hạn): cư dân gửi `vaiTroCuTru = 'Chủ căn hộ'` hoặc `ngayBatDau < homNay` nhận 400; nhập CCCD có thật nhưng sai SĐT, CCCD không tồn tại, hoặc cặp của tài khoản `'Ngừng hoạt động'` đều nhận cùng mã 422 `CUTRU_KHONG_KHOP_CU_DAN`; căn đã có 10 đề nghị `'Chờ xác minh'` gửi thêm cái thứ 11 nhận 422 `CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH`, verifies **AC-2**, **AC-3**
- Auth/permission & PII (integration): cư dân B ở cùng căn `A-0101` không thấy đề nghị của cư dân A trong `deNghiCuTru` và gọi `POST .../cancel` trên mã hồ sơ của A nhận 404; cư dân gọi `GET /residency-records`, `approve`, `reject` nhận 403; BQL gọi `resident-request` hoặc `cancel` nhận 403; `KE_TOAN` và `BAN_QUAN_TRI` gọi `GET /residency-records` nhận 403; lọc `maToaNha` ngoài phạm vi trả 404, verifies **AC-6**, **AC-8**, **AC-10**, **AC-12**
- UI (Vitest + Testing Library): trên `/can-ho-cua-toi`, cư dân mở hộp thoại gửi đề nghị (ẩn/hiện CCCD và SĐT theo lựa chọn `"Chính tôi"` / `"Người khác"`), thấy tab `"Đề nghị cư trú"`, lý do từ chối và nút `"Hủy đề nghị"`; trên `/ho-so-cu-tru`, BQL thấy số đếm `"Chờ xác minh"`, lọc đồng bộ URL, duyệt hoặc từ chối ngay trên bảng và mở hộp thoại thêm hồ sơ có chọn căn hộ, đạt yêu cầu bàn phím và nhãn trợ năng, verifies **AC-15**, **AC-16**, **AC-17**, **AC-18**

## Build plan

Thứ tự theo Tracer Bullet và quy trình tách backend trước, frontend sau của scope (3 mốc backend rồi tới 2 mốc frontend):

1. **Backend đường mỏng đề nghị cư trú: `V19`, gửi đề nghị và đọc trên căn hộ.** Viết `V19__de_nghi_cu_tru.sql` (cập nhật câu `INSERT` dòng `'Chờ xác minh'` trong 2 integration test sẵn có của spec 0014), mở rộng `HoSoCuTruDong`, `HoSoCuTruResponse`, `HoSoCuTruCanHoResponse`, câu thông báo dùng chung của `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` và `CuTruDinhDanhService.timTheoCccdVaSdt`; cập nhật `DungHoSoCuTru` (lọc `Objects::nonNull` trước khi truy vấn lô) và `TraCuuHoSoCuTruService` trả thêm mảng `deNghiCuTru` (ẩn PII và lọc người liên quan cho `CU_DAN`); viết `GuiDeNghiCuTruService`, `POST /api/v1/residency-records/resident-request`, dịch lỗi `uq_hsct_cho_xac_minh_canho_cudan` và nhật ký `CUTRU_GUI_DE_NGHI`. Viết unit test và integration test cho đường gửi đề nghị, chống dò danh tính, trần 10 đề nghị và đọc tab đề nghị trên căn hộ. Satisfies **AC-1**, **AC-2**, **AC-3**, **AC-8**, **AC-11**, **AC-12**, **AC-13**
2. **Backend xử lý vòng đời đề nghị: duyệt, từ chối, tự hủy và khóa trạng thái kết thúc.** Viết `DuyetDeNghiCuTruService`, `TuChoiDeNghiCuTruService`, `HuyDeNghiCuTruService` cùng ba endpoint `POST .../approve`, `POST .../reject`, `POST .../cancel` dưới khóa dòng `can_ho` rồi `ho_so_cu_tru`, tích hợp `DongBoTrangThaiCanHo` khi duyệt; cập nhật `CapNhatHoSoCuTruService` trả 409 `CUTRU_DE_NGHI_DA_KET_THUC` cho dòng `'Từ chối'` và `'Đã hủy'` và giữ nguyên thông tin đề nghị khi sửa dòng `'Hợp lệ'`; bổ sung ba mã nhật ký còn lại. Viết unit test và integration test cho duyệt, từ chối, tự hủy, lệch ngày qua mốc hôm nay và tranh chấp đồng thời. Satisfies **AC-4**, **AC-5**, **AC-6**, **AC-7**, **AC-11**, **AC-12**, **AC-13**
3. **Backend tra cứu tập trung toàn tòa, dữ liệu mẫu và khóa OpenAPI.** Mở rộng `CuTruDinhDanhService.maCuDanKhopTuKhoa` và `CanHoCuTruService.maCanHoThuocBoLoc`, viết `DanhSachHoSoCuTruService` cùng `GET /api/v1/residency-records` có phân trang, bộ lọc và sắp xếp ổn định; mở rộng `R__seed_04_cu_tru.sql` thêm hai dòng đề nghị mẫu và cập nhật `SchemaMigrationTest`; kiểm `PhanQuyenCoverageTest`, `ChiTietNhatKyAnToanTest` và chạy `mvnw verify` sinh `docs/api/openapi.json`. Satisfies **AC-9**, **AC-10**, **AC-12**, **AC-13**, **AC-14**
4. **Frontend luồng đề nghị trên trang căn hộ và `/can-ho-cua-toi`.** Chạy `pnpm gen:api`; cập nhật `frontend/src/lib/cu-tru.ts` và `error-messages.ts`, mở rộng `KhoiHoSoCuTru` thêm tab `"Đề nghị cư trú"`, viết các hộp thoại gửi đề nghị, xác nhận hủy, xác nhận duyệt và nhập lý do từ chối để cư dân và BQL thao tác ngay trên trang căn hộ. Viết test giao diện kiểm tra ẩn/hiện trường theo đối tượng, không lộ PII và xử lý lỗi API. Satisfies **AC-15**, **AC-18**
5. **Frontend màn hình tập trung `/ho-so-cu-tru` và hoàn tất.** Bật `sanSang: true` cho `/ho-so-cu-tru` trong `muc-chuc-nang.ts`, dựng trang `frontend/src/app/(app)/ho-so-cu-tru/` với thanh lọc nhanh `"Chờ xác minh"` kèm số đếm, bộ lọc đồng bộ URL, bảng `DataTable` có sắp xếp, mở rộng `ThemHoSoCuTruDialog` hỗ trợ chọn tòa nhà và căn hộ qua combobox khi mở từ `/ho-so-cu-tru`, gắn trọn bộ hành động theo dòng và invalidate cache liên thông; kiểm tra bàn phím, focus, WCAG AA, chạy `pnpm typecheck`, `pnpm lint`, `pnpm format:check`, `pnpm test` và `pnpm build`. Satisfies **AC-16**, **AC-17**, **AC-18**

## Consequences

**Positive**:
- Giữ đúng một bảng `ho_so_cu_tru`, một chuỗi mã `HS...` và một kiểu `HoSoCuTruResponse` cho cả đề nghị lẫn hồ sơ chính thức, nên màn hình `/ho-so-cu-tru` lọc xuyên suốt năm trạng thái bằng phân trang chuẩn mà không phải ghép bảng bằng `UNION ALL`.
- Ràng buộc `CHECK` kép và unique index một phần ở PostgreSQL khóa chặt mối liên hệ giữa trạng thái hồ sơ, người xác minh, người đề nghị và lý do từ chối ngay tại tầng cơ sở dữ liệu.
- Cư dân tự gửi được đề nghị cho người thân hoặc khách thuê mà không cần mở quyền tìm kiếm tài khoản cư dân toàn khu và không tạo lỗ hổng dò số CCCD.

**Negative / tradeoffs**:
- Người sẽ vào ở bắt buộc phải có sẵn tài khoản `cu_dan` (do Admin hoặc BQL cấp trước theo spec 0008) và người gửi đề nghị phải hỏi trước đúng cặp số CCCD kèm số điện thoại của người đó; trẻ nhỏ chưa có tài khoản riêng vẫn phải chờ tính năng nhân khẩu phụ thuộc ở mục Deferred.
- BQL duyệt nguyên trạng hoặc từ chối kèm lý do chứ không sửa ngày trực tiếp lúc bấm duyệt; nếu cư dân khai nhầm ngày bắt đầu hoặc vai trò thì cần một vòng từ chối và gửi lại đơn mới (đổi lại hồ sơ được duyệt phản ánh trung thực 100% nội dung cư dân đã nộp).
- Trang `/ho-so-cu-tru` gọi thêm một request nhẹ `size=1` để hiển thị con số đếm trên thanh lọc nhanh `"Chờ xác minh"` khi đang đứng ở chế độ lọc khác, đổi lấy việc giữ nguyên vỏ bọc `PageResponse` dùng chung của spec 0001.

**Neutral**:
- Mở khóa mục menu `/ho-so-cu-tru` đã khai chờ sẵn từ spec 0006 cho vai trò `BAN_QUAN_LY`.
- Dữ liệu seed trong `R__seed_04_cu_tru.sql` tăng thêm 2 dòng đề nghị mẫu tại căn `A-0101` (`'Chờ xác minh'` và `'Từ chối'`), không làm thay đổi số người đang cư trú hôm nay hay trạng thái sử dụng của bất kỳ căn hộ mẫu nào.

## Follow-up

- [ ] Tính năng 19 (Phương tiện gửi xe căn hộ): hoàn tất lát cắt cuối cùng của phân hệ `cutru` (`API-UC02-05` đến `07`) trên nhánh `feat/cutru`.
- [ ] Mục Deferred *"Nhân khẩu phụ thuộc không có tài khoản"*: khi triển khai ở giai đoạn sau, cần quyết định cách mở rộng `cu_dan` hoặc bảng nhân khẩu phụ thuộc để cư dân khai báo trẻ nhỏ chưa có số điện thoại và CCCD riêng.
- [ ] Giải quyết mục Follow-up đầu tiên của spec 0014; đối chiếu khi chạy `/sync`.
- [ ] Đặc tả Excel của `API-UC02-04` minh họa payload mẫu truyền `hoTen` tự do và `vaiTroCuTru = "Chủ căn hộ"`, chưa khớp ràng buộc khóa ngoại `cu_dan` và phân quyền chủ hộ thực tế; cập nhật lại file Excel nếu nhóm cần đồng bộ tài liệu nộp cuối kỳ.
