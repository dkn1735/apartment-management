# 0014. Hồ sơ cư trú căn hộ: ghi nhận và kết thúc dưới khóa dòng căn hộ, chặn chồng ngày ở DB, đồng bộ hai chiều trạng thái căn

**Date**: 2026-11-26
**Status**: Accepted

## Summary

Spec này chốt phần đầu của phân hệ cư trú (UC02): Ban quản lý (BQL) ghi nhận, chỉnh sửa và kết thúc hồ sơ cư trú cho cư dân đã có tài khoản; BQL và cư dân xem danh sách nhân khẩu đang ở cùng lịch sử cư trú ngay trên trang chi tiết căn hộ. Mọi thao tác ghi đều khóa dòng căn hộ trước để tự động đồng bộ hai chiều cột trạng thái sử dụng của căn hộ với số người đang ở hôm nay, đồng thời PostgreSQL tự chặn trùng khoảng ngày bằng ràng buộc loại trừ (`EXCLUDE USING gist`, tức cơ chế để cơ sở dữ liệu từ chối hai dòng có khoảng ngày đè lên nhau). Khi người xem chỉ là cư dân của căn hộ, hệ thống ẩn toàn bộ số căn cước, điện thoại, email, người xác minh và lịch sử của những người không phải chính họ.

## Requirements

**User stories**:
- Là nhân viên BQL, tôi muốn tìm cư dân theo họ tên, số điện thoại hoặc số CCCD rồi ghi nhận hồ sơ cư trú vào một căn hộ, để gán đúng người vào đúng căn và tự động chuyển căn sang trạng thái đang sử dụng khi có người vào ở.
- Là nhân viên BQL, tôi muốn điều chỉnh vai trò cư trú, tình trạng cư trú, khoảng ngày hoặc kết thúc hồ sơ khi cư dân chuyển đi, kèm nhật ký kiểm toán truy vết được.
- Là nhân viên BQL, tôi muốn xem toàn bộ nhân khẩu đang cư trú, hồ sơ sắp bắt đầu và lịch sử cư trú của căn hộ ngay trên trang chi tiết căn.
- Là cư dân đang cư trú hợp lệ tại căn hộ, tôi muốn xem những ai đang sống cùng căn với mình và xem lịch sử cư trú của riêng mình mà không bị lộ số CCCD, số điện thoại hay email cá nhân cho người ở cùng.
- Là thành viên vận hành hệ thống, tôi muốn các hồ sơ hết hạn ngày kết thúc được tự động chuyển sang hết hiệu lực đầu mỗi ngày và trạng thái sử dụng của căn hộ luôn khớp với thực tế cư trú.

**Thuật ngữ dùng trong spec**:
- *Ngày hôm nay (`homNay`)*: `LocalDate.now(clock)` với bean `Clock` mang múi giờ `app.timezone` (`Asia/Ho_Chi_Minh`, quy ước từ spec 0001 và `PhamViTinhToan`).
- *Hồ sơ đang cư trú hôm nay*: dòng `ho_so_cu_tru` có `trang_thai_ho_so = 'Hợp lệ'`, `ngay_bat_dau <= homNay` và `(ngay_ket_thuc IS NULL OR ngay_ket_thuc >= homNay)` (khớp định nghĩa của `CuTruService.maCanHoDangCuTru`).
- *Hồ sơ sắp bắt đầu*: dòng có `trang_thai_ho_so = 'Hợp lệ'` và `ngay_bat_dau > homNay`.
- *Hồ sơ thuộc lịch sử*: dòng có `trang_thai_ho_so = 'Hết hiệu lực'`, hoặc dòng còn mang `trang_thai_ho_so = 'Hợp lệ'` nhưng `ngay_ket_thuc < homNay` (trong khoảng ngắn đầu ngày trước khi job quét chạy). Dòng `Chờ xác minh` (dành cho tính năng 18) không nằm trong ba nhóm này.
- *Vai trò cư trú hợp lệ*: đúng một trong ba chuỗi NFC `'Chủ căn hộ'`, `'Khách thuê'`, `'Thành viên gia đình'`.
- *Tình trạng cư trú hợp lệ*: đúng một trong hai chuỗi NFC `'Thường trú'`, `'Tạm trú'`.
- *Trạng thái hồ sơ ghi được ở lát này*: `'Hợp lệ'` hoặc `'Hết hiệu lực'` (chuỗi NFC).

**Acceptance criteria**:

*Tra cứu nhân khẩu và lịch sử cư trú của căn hộ (API-UC02-01)*

- **AC-1**: `GET /api/v1/apartments/{apartmentId}/residents`, `@ChoPhep({ BAN_QUAN_LY, CU_DAN })`, chuẩn hóa mã căn như spec 0012, trả 200 `message` `"Tra cứu hồ sơ cư trú của căn hộ thành công"`, `data` là `HoSoCuTruCanHoResponse` gồm ba mảng không bao giờ `null`: `{ dangCuTru, sapBatDau, lichSuCuTru }`. Thứ tự sắp xếp: `dangCuTru` sắp theo thứ tự vai trò (`Chủ căn hộ` trước, rồi `Thành viên gia đình`, rồi `Khách thuê`), sau đó `ngay_bat_dau ASC`, `ma_ho_so ASC`; `sapBatDau` sắp theo `ngay_bat_dau ASC`, `ma_ho_so ASC`; `lichSuCuTru` sắp ưu tiên các đợt đã bắt đầu (`ngay_bat_dau <= homNay` đứng trước các hồ sơ tương lai bị hủy sớm), sau đó `COALESCE(ngay_ket_thuc, ngay_bat_dau) DESC`, `ma_ho_so DESC`. Không trả dòng có `trang_thai_ho_so = 'Chờ xác minh'`. `CanHoResponse` của `GET /api/v1/apartments` và `GET /api/v1/apartments/{apartmentId}` giữ nguyên không đổi.
- **AC-2**: Phạm vi xem và lọc dữ liệu cá nhân (PII) trên `GET /api/v1/apartments/{apartmentId}/residents`:
  - Người gọi có `BAN_QUAN_LY` và căn thuộc tòa trong `PhamViTruyCap.maToaNha()`: thấy đủ mọi dòng của cả ba mảng, đủ mọi trường trên mỗi phần tử `HoSoCuTruResponse` (`maHoSo`, `maCanHo`, `maCuDan`, `maNguoiDung`, `hoTen`, `soDienThoai`, `email`, `soCccd`, `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau`, `ngayKetThuc`, `trangThaiHoSo`, `maNvbqlXacMinh`, `hoTenNguoiXacMinh`, `taoLuc`, `capNhatLuc`).
  - Người gọi không đạt điều kiện `BAN_QUAN_LY` của tòa nhưng có `CU_DAN` và mã căn nằm trong `PhamViTruyCap.maCanHo()` (đang cư trú hợp lệ hôm nay tại căn): thấy toàn bộ dòng của `dangCuTru`, nhưng trong `sapBatDau` và `lichSuCuTru` chỉ giữ các dòng có `ma_cu_dan` của chính người gọi; đồng thời trên mọi dòng trả về, các trường `maNguoiDung`, `soDienThoai`, `email`, `soCccd`, `maNvbqlXacMinh`, `hoTenNguoiXacMinh` đều bằng `null`.
  - Không đạt cả hai điều kiện (căn không tồn tại, ngoài phạm vi tòa, cư dân xem căn người khác hoặc căn mình đã hết hiệu lực cư trú, mã căn sai định dạng): trả 404 `NOT_FOUND` `"Không tìm thấy dữ liệu yêu cầu"`.

*Tìm chọn cư dân khi tạo hồ sơ*

- **AC-3**: `GET /api/v1/residency-records/resident-options?q`, chỉ `@ChoPhep(BAN_QUAN_LY)`, trả 200 `message` `"Tra cứu danh sách cư dân thành công"`, `data` là mảng tối đa 20 phần tử `CuDanLuaChonResponse` (`{ maCuDan, maNguoiDung, hoTen, soDienThoai, email, soCccd, trangThaiTaiKhoan }`), luôn sắp theo `ho_ten ASC, ma_cu_dan ASC`, chỉ lấy người dùng có dòng trong bảng `cu_dan` (kể cả tài khoản kiêm thêm vai trò khác như `BAN_QUAN_TRI`) và có `trang_thai_tai_khoan IN ('Hoạt động', 'Tạm khóa')`. `q` tùy chọn: đổi NFC và cắt khoảng trắng hai đầu trước khi kiểm độ dài; rỗng hoặc vắng thì trả 20 cư dân đầu tiên; dài quá 100 ký tự (tính theo code point sau khi cắt) trả 400 `VALIDATION_FAILED`, mục `q` `"Từ khóa tối đa 100 ký tự"`; có chữ trong khoảng 1 đến 100 ký tự thì khớp một trong các điều kiện `unaccent(lower(ho_ten))` chứa `unaccent(lower(q))`, `so_dien_thoai` chứa `qSdt` (`q` sau khi bỏ khoảng trắng, dấu chấm, gạch nối và đổi `+84`/`84` đầu chuỗi thành `0`, chỉ đưa vào `WHERE` khi khác rỗng và toàn chữ số), `so_cccd` chứa `qCccd` (`q` sau khi bỏ khoảng trắng và gạch nối, không đổi đầu `84`, chỉ đưa vào `WHERE` khi khác rỗng và toàn chữ số), `lower(email)` chứa `lower(q)`, hoặc `upper(ma_cu_dan)` chứa `upper(q)` (thoát `%`, `_`, `\`).

*Thêm mới hồ sơ cư trú (API-UC02-02)*

- **AC-4**: `POST /api/v1/residency-records` với body `{ maCanHo, maCuDan, vaiTroCuTru, tinhTrangCuTru, ngayBatDau, ngayKetThuc? }`, chỉ `@ChoPhep(BAN_QUAN_LY)`. Ngay đầu hàm (trước kiểm 400), server lấy `ma_nvbql_xac_minh` từ bảng `nhan_vien_bql` của `PhamViTruyCap.maNguoiDung()`; nếu người gọi đã bị gỡ dòng `nhan_vien_bql` trước khi token hết hạn thì trả 403 `FORBIDDEN`. Mọi trường chuỗi được đổi về NFC và cắt khoảng trắng hai đầu (`maCanHo` và `maCuDan` đổi thêm sang chữ hoa `Locale.ROOT`). Server đặt `trang_thai_ho_so = 'Hợp lệ'`, `tao_luc` và `cap_nhat_luc` bằng `Instant.now(clock).truncatedTo(ChronoUnit.MICROS)`, chèn dòng dưới khóa dòng `can_ho`, đồng bộ `trang_thai_su_dung` của căn theo AC-10, ghi nhật ký theo AC-12, và trả 201 `message` `"Thêm mới hồ sơ cư trú thành công"`, `data` là `HoSoCuTruResponse` đầy đủ của hồ sơ vừa tạo.
- **AC-5**: Dữ liệu đầu vào của `POST /api/v1/residency-records` sai trả 400 `VALIDATION_FAILED` `"Dữ liệu không hợp lệ"`, một mục `errors` cho mỗi trường sai theo thứ tự `maCanHo`, `maCuDan`, `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau`, `ngayKetThuc` (mọi trường đọc qua `JsonNode`, chỉ nhận chuỗi JSON `isString()` trừ `ngayKetThuc` nhận thêm `null` hoặc vắng; kiểm sau khi đổi NFC và cắt khoảng trắng hai đầu):
  - `maCanHo` vắng hoặc rỗng sau chuẩn hóa: `"Vui lòng chọn căn hộ"`; không phải chuỗi JSON: `"Căn hộ không hợp lệ"`
  - `maCuDan` vắng hoặc rỗng sau chuẩn hóa: `"Vui lòng chọn cư dân"`; không khớp mẫu `^CD[0-9]{8}$`: `"Mã cư dân không hợp lệ"`
  - `vaiTroCuTru` vắng hoặc rỗng: `"Vui lòng chọn vai trò cư trú"`; khác ba giá trị hợp lệ: `"Vai trò cư trú không hợp lệ"`
  - `tinhTrangCuTru` vắng hoặc rỗng: `"Vui lòng chọn tình trạng cư trú"`; khác hai giá trị hợp lệ: `"Tình trạng cư trú không hợp lệ"`
  - `ngayBatDau` vắng hoặc rỗng: `"Vui lòng chọn ngày bắt đầu"`; không đúng định dạng ngày thực `YYYY-MM-DD` (`LocalDate.parse`): `"Ngày bắt đầu không hợp lệ"`
  - `ngayKetThuc` có mặt và khác `null` nhưng rỗng hoặc sai định dạng `YYYY-MM-DD`: `"Ngày kết thúc không hợp lệ"`; vắng hoặc `null` khi `tinhTrangCuTru` là `'Tạm trú'`: `"Tạm trú bắt buộc có ngày kết thúc"`; cả hai ngày hợp lệ mà `ngayKetThuc < ngayBatDau`: `"Ngày kết thúc phải từ ngày bắt đầu trở đi"`; `ngayKetThuc >= ngayBatDau` nhưng `ngayKetThuc < homNay`: `"Ngày kết thúc của hồ sơ hợp lệ không được nhỏ hơn hôm nay"`
- **AC-6**: Sau khi qua kiểm 403 (`nhan_vien_bql`) và 400 của `POST`, kiểm tra theo đúng thứ tự:
  1. Căn hộ không tồn tại hoặc tòa ngoài `PhamViTruyCap.maToaNha()`: trả 404 `NOT_FOUND` `"Không tìm thấy dữ liệu yêu cầu"`.
  2. Khóa dòng `can_ho` (`SELECT ... FOR UPDATE`).
  3. Tra `maCuDan` qua service công khai của `auth`: không tồn tại trả 404 `NOT_FOUND` `"Không tìm thấy cư dân"`; tài khoản gắn với cư dân đang ở trạng thái `'Ngừng hoạt động'` trả 422 `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` `"Tài khoản cư dân đã ngừng hoạt động, không thể thêm hồ sơ cư trú"`.
  4. Trùng khoảng ngày `daterange(ngay_bat_dau, ngay_ket_thuc, '[]')` với một hồ sơ `'Hợp lệ'` đang có của cùng `(maCanHo, maCuDan)`: trả 409 `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN` `"Cư dân đã có hồ sơ cư trú hợp lệ giao khoảng ngày tại căn hộ này"`.
  5. `vaiTroCuTru` là `'Chủ căn hộ'` và khoảng ngày giao với một hồ sơ `'Hợp lệ'` khác có `vai_tro_cu_tru = 'Chủ căn hộ'` tại cùng `maCanHo`: trả 409 `CUTRU_DA_CO_CHU_CAN_HO` `"Căn hộ đã có chủ căn hộ hợp lệ trong khoảng ngày này"`.

*Cập nhật và kết thúc hồ sơ cư trú (API-UC02-03)*

- **AC-7**: `PUT /api/v1/residency-records/{recordId}` với body `{ vaiTroCuTru, tinhTrangCuTru, ngayBatDau, ngayKetThuc?, trangThaiHoSo }`, chỉ `@ChoPhep(BAN_QUAN_LY)`. Mọi trường chuỗi trong body đổi về NFC và cắt khoảng trắng hai đầu; `recordId` cắt khoảng trắng và viết hoa. Không nhận đổi `maCanHo` hay `maCuDan` (trường lạ trong body bị bỏ qua). Trong một transaction: kiểm tra người gọi còn dòng `nhan_vien_bql` (403), kiểm body (400), kiểm phạm vi căn của hồ sơ (404), khóa dòng `can_ho` trước rồi khóa dòng `ho_so_cu_tru` (`SELECT ... FOR UPDATE`), kiểm trạng thái hiện tại của hồ sơ (409 nếu đã `'Hết hiệu lực'` hoặc `'Chờ xác minh'`); sau đó nếu cả năm trường giữ nguyên so với DB thì trả ngay 200 mà không `UPDATE`, không đổi `cap_nhat_luc` hay `ma_nvbql_xac_minh`, không ghi nhật ký; nếu có ít nhất một trường đổi thì kiểm không chồng ngày (khi trạng thái mới là `'Hợp lệ'`), cập nhật dòng cùng `ma_nvbql_xac_minh` (theo BQL đang gọi) và `cap_nhat_luc = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)`, đồng bộ `trang_thai_su_dung` của căn theo AC-10, ghi nhật ký theo AC-12. Trả 200 `message` `"Cập nhật hồ sơ cư trú thành công"`, `data` là `HoSoCuTruResponse` sau khi xử lý.
- **AC-8**: Kiểm lỗi của `PUT /api/v1/residency-records/{recordId}` theo đúng thứ tự: 403 vai trò và dòng `nhan_vien_bql`, 400 body, 404 phạm vi, khóa dòng, 409 trạng thái hiện tại, ngắn mạch 200 không đổi, rồi 409 chồng ngày:
  - 400 `VALIDATION_FAILED` theo thứ tự trường `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau`, `ngayKetThuc`, `trangThaiHoSo`: ba trường đầu kiểm như AC-5; `trangThaiHoSo` vắng hoặc rỗng: `"Vui lòng chọn trạng thái hồ sơ"`, khác `'Hợp lệ'` và `'Hết hiệu lực'`: `"Trạng thái hồ sơ không hợp lệ"`; `ngayKetThuc` sai định dạng như AC-5; khi `trangThaiHoSo = 'Hợp lệ'` thì ràng buộc `ngayKetThuc` với `'Tạm trú'`, `ngayBatDau` và `homNay` giống hệt AC-5; khi `trangThaiHoSo = 'Hết hiệu lực'` thì `ngayKetThuc` vắng hoặc `null` báo `"Kết thúc hồ sơ bắt buộc có ngày kết thúc"`, nhỏ hơn `ngayBatDau` báo `"Ngày kết thúc phải từ ngày bắt đầu trở đi"`, còn khi `ngayBatDau <= homNay` mà `ngayKetThuc > homNay` báo `"Ngày kết thúc khi đóng hồ sơ không được lớn hơn hôm nay"`, riêng trường hợp hủy sớm hồ sơ chưa bắt đầu (`ngayBatDau > homNay`) bắt buộc `ngayKetThuc` bằng đúng `ngayBatDau` (khác thì báo `"Hủy hồ sơ chưa tới ngày bắt đầu cần đặt ngày kết thúc bằng ngày bắt đầu"`).
  - 404 `NOT_FOUND` `"Không tìm thấy dữ liệu yêu cầu"` khi `recordId` không khớp `^HS[0-9]{8}$`, không tồn tại, hoặc căn hộ của hồ sơ thuộc tòa ngoài `PhamViTruyCap.maToaNha()`.
  - Hồ sơ hiện tại đang ở `'Hết hiệu lực'`: trả 409 `CUTRU_HO_SO_DA_HET_HIEU_LUC` `"Hồ sơ cư trú đã hết hiệu lực, không thể chỉnh sửa"`.
  - Hồ sơ hiện tại đang ở `'Chờ xác minh'`: trả 409 `CUTRU_HO_SO_DANG_CHO_XAC_MINH` `"Hồ sơ đang chờ xác minh, chưa thể chỉnh sửa tại đây"`.
  - Khi `trangThaiHoSo` mới vẫn là `'Hợp lệ'`, kiểm tra không giao khoảng ngày với hồ sơ `'Hợp lệ'` khác (trừ chính `ma_ho_so` này) theo đúng hai mã 409 `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN` và `CUTRU_DA_CO_CHU_CAN_HO` của AC-6.

*Đồng bộ hai chiều với trạng thái căn hộ và tự động hết hạn*

- **AC-9**: Migration `V18__ho_so_cu_tru.sql` bật `CREATE EXTENSION IF NOT EXISTS btree_gist`, thêm hai cột `tao_luc TIMESTAMPTZ NOT NULL DEFAULT now()` và `cap_nhat_luc TIMESTAMPTZ NOT NULL DEFAULT now()` vào `ho_so_cu_tru` (không đặt `CHECK (cap_nhat_luc >= tao_luc)` giống `nguoi_dung_he_thong` ở `V1` để tránh vỡ khi test tích hợp dùng `Clock` cố định nhỏ hơn thời điểm nạp seed), thêm `chk_hsct_ngay_ket_thuc_bat_buoc CHECK ((tinh_trang_cu_tru <> 'Tạm trú' AND trang_thai_ho_so <> 'Hết hiệu lực') OR ngay_ket_thuc IS NOT NULL)`, cùng hai ràng buộc loại trừ chỉ xét dòng `'Hợp lệ'`: `ex_hsct_cu_dan_khong_chong_ngay` chặn trùng `(ma_can_ho, ma_cu_dan)` giao `daterange(ngay_bat_dau, ngay_ket_thuc, '[]')` và `ex_hsct_mot_chu_can_ho` chặn hai `'Chủ căn hộ'` cùng `ma_can_ho` giao `daterange(ngay_bat_dau, ngay_ket_thuc, '[]')`. Helper `LoiCoSoDuLieu` bổ sung hằng SQLSTATE `LOAI_TRU_GIST = "23P01"`, và service dịch vi phạm hai ràng buộc loại trừ này về đúng hai mã lỗi 409 của AC-6 khi có hai request lọt qua kiểm trước.
- **AC-10**: Đồng bộ hai chiều giữa hồ sơ cư trú và `can_ho.trang_thai_su_dung` dưới khóa dòng `can_ho`:
  - Chiều từ hồ sơ sang căn hộ (trong cùng transaction của `POST`, `PUT` và job đầu ngày): sau khi ghi `ho_so_cu_tru`, đếm số *hồ sơ đang cư trú hôm nay* của căn đó. Nếu số lượng `>= 1` và `trang_thai_su_dung` hiện tại là `'Trống'` hoặc `'Đã bàn giao'` thì đổi thành `'Đang sử dụng'`. Nếu số lượng `= 0` và `trang_thai_su_dung` hiện tại là `'Đang sử dụng'` thì đổi thành `'Trống'`. Nếu `trang_thai_su_dung` hiện tại là `'Đang sửa chữa'` thì giữ nguyên không đổi ở cả hai trường hợp (căn đang sửa chữa vẫn cho phép thêm, sửa và kết thúc hồ sơ cư trú). Bảng `can_ho` không có cột thời điểm sửa chung nên chỉ cập nhật cột `trang_thai_su_dung`.
  - Chiều từ căn hộ sang hồ sơ: ở `PUT /api/v1/apartments/{apartmentId}` (spec 0012), sau các bước 403 vai trò, 400 body, 404 phạm vi và `CanHoGhi.khoaDong(ma)` (`SELECT ... FOR UPDATE`), gọi `CuTruService.coNguoiDangCuTru(ma, homNay)` ngay dưới khóa dòng đó: nếu căn đang có người cư trú hôm nay mà yêu cầu đặt `'Trống'` hoặc `'Đã bàn giao'` thì trả 422 `CANHO_TRANG_THAI_MAU_THUAN_CU_TRU` `"Trạng thái sử dụng không khớp với hồ sơ cư trú hiện tại"`, kèm một mục `errors` trường `trangThaiSuDung` `"Căn hộ đang có người cư trú, không thể đặt trạng thái Trống hoặc Đã bàn giao"`; nếu căn chưa có người cư trú hôm nay mà yêu cầu đặt `'Đang sử dụng'` thì trả cùng mã 422 với mục `trangThaiSuDung` `"Căn hộ chưa có người đang cư trú, không thể đặt trạng thái Đang sử dụng"`. Ở `POST /api/v1/apartments` (căn vừa tạo chưa có hồ sơ nào), kiểm ngay sau bước 400 (trước kiểm trùng 409): nếu gửi `trangThaiSuDung = 'Đang sử dụng'` thì trả đúng lỗi 422 này.
- **AC-11**: Job đầu ngày `HoSoCuTruHetHanJob` chạy theo lịch `@Scheduled(cron = "${app.cutru.residency-expiry-cron}", zone = "${app.timezone}")` (mặc định `0 5 0 * * *`, tức 00:05 hằng ngày giờ Việt Nam) và chạy bù thêm một lần khi nhận `ApplicationReadyEvent` (để không lệch `trang_thai_su_dung` nếu server tắt qua mốc 00:05): tìm danh sách `ma_can_ho` có dòng `trang_thai_ho_so = 'Hợp lệ'` mà `ngay_ket_thuc < homNay`; xử lý từng căn hộ trong một transaction riêng theo thứ tự `ma_can_ho`: khóa dòng `can_ho`, chạy `UPDATE ho_so_cu_tru SET trang_thai_ho_so = 'Hết hiệu lực', cap_nhat_luc = :luc WHERE ma_can_ho = :ma AND trang_thai_ho_so = 'Hợp lệ' AND ngay_ket_thuc < :homNay RETURNING ma_ho_so, ma_cu_dan, ngay_ket_thuc` sắp theo `ma_ho_so ASC` (giữ nguyên `ma_nvbql_xac_minh`); nếu 0 dòng được cập nhật (do tiến trình khác vừa chạy xong) thì bỏ qua không đồng bộ và không ghi nhật ký; nếu có từ 1 dòng trở lên thì đồng bộ `trang_thai_su_dung` của căn theo AC-10 và ghi nhật ký theo AC-12 với `ma_nguoi_dung = null` (khi nhiều dòng của cùng căn cùng hết hạn trong một transaction, `trangThaiCanHo` chỉ đính vào dòng nhật ký của `ma_ho_so` cuối cùng). Lỗi ở một căn được log mức `ERROR` (tên lớp lỗi và thông điệp, không PII) và không làm dừng các căn còn lại.

*Nhật ký kiểm toán, phân quyền và dữ liệu mẫu*

- **AC-12**: Mỗi thao tác thay đổi thật sự trên hồ sơ cư trú ghi đúng một dòng nhật ký qua `NhatKyService.ghi` (sau commit), đối tượng `HoSoCuTru:<maHoSo>`, tuyệt đối không chứa họ tên, số điện thoại, email hay số CCCD (vượt qua `ChiTietNhatKyAnToanTest`). Bổ sung ba giá trị `LoaiThaoTac`:
  - `CUTRU_THEM_HO_SO` (`"Thêm hồ sơ cư trú"`): `{ maCanHo, maCuDan, vaiTroCuTru, tinhTrangCuTru, ngayBatDau, ngayKetThuc, trangThaiCanHo? }` (`trangThaiCanHo` dạng `{ cu, moi }`, chỉ xuất hiện khi căn hộ tự động đổi trạng thái sử dụng).
  - `CUTRU_CAP_NHAT_HO_SO` (`"Cập nhật hồ sơ cư trú"`): khi hồ sơ vẫn ở `'Hợp lệ'` và có ít nhất một trường đổi, ghi `maCanHo`, `maCuDan`, các trường đổi dạng `{ cu, moi }`, và `trangThaiCanHo?` nếu căn đổi trạng thái.
  - `CUTRU_KET_THUC_HO_SO` (`"Kết thúc hồ sơ cư trú"`): khi chuyển từ `'Hợp lệ'` sang `'Hết hiệu lực'` (do BQL gọi `PUT` hoặc do job hết hạn với `ma_nguoi_dung = null`), ghi `{ maCanHo, maCuDan, ngayKetThuc, tuDong: boolean, vaiTroCuTru?, tinhTrangCuTru?, ngayBatDau?, trangThaiCanHo? }`, trong đó `ngayKetThuc` luôn là chuỗi `YYYY-MM-DD` của ngày kết thúc cuối cùng, không ghi trường `trangThaiHoSo`, và ba trường `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau` chỉ xuất hiện dưới dạng `{ cu, moi }` khi BQL vừa sửa trường đó vừa kết thúc trong cùng một lần `PUT`.
- **AC-13**: Bốn endpoint mới mang đúng `@ChoPhep` như bảng *API surface*; vai trò khác (như `KE_TOAN`, `BAN_QUAN_TRI`, `KY_THUAT`, `BAO_VE`, `QUAN_TRI_VIEN` trên các endpoint cư trú, hoặc `CU_DAN` gọi `POST`, `PUT`, `resident-options`) nhận 403 `FORBIDDEN`; chưa đăng nhập nhận 401. Khi hồ sơ của cư dân bị kết thúc hoặc qua `ngay_ket_thuc`, ngay ở request kế tiếp `PhamViTruyCapService` không còn trả căn đó trong `maCanHo` nữa, nên cư dân gọi `GET /apartments/{id}` và `GET /apartments/{id}/residents` nhận 404. `PhanQuyenCoverageTest` xanh.
- **AC-14**: File seed mới `db/seed/R__seed_04_cu_tru.sql` (chạy sau `R__seed_03_can_ho.sql`, an toàn khi chạy lại bằng `ON CONFLICT DO NOTHING` và `NOT EXISTS`) thêm hai cư dân mẫu có tài khoản `'Hoạt động'` (mật khẩu chung `ChungCu@2026`, người xác minh là BQL `0900000002`, mốc `tao_luc` và `cap_nhat_luc` đặt cố định `TIMESTAMPTZ '2026-01-01 08:00:00+07'`): cư dân `0900000009` (`'Chủ căn hộ'`, `'Thường trú'`, `'Hợp lệ'`, bắt đầu `2026-01-01`, `ngay_ket_thuc` `NULL` tại căn `A-0103`, đồng thời đứng tên 1 dòng lịch sử `'Khách thuê'`, `'Tạm trú'`, `'Hết hiệu lực'` từ `2025-01-01` đến `2025-12-31` tại căn `A-0101` để cư dân `0900000006` đang ở `A-0101` không thấy dòng lịch sử của người khác) và cư dân `0900000010` (`'Khách thuê'`, `'Tạm trú'`, `'Hợp lệ'` từ `2026-01-01` đến `2027-12-31` tại căn `A-0204`). Nhờ vậy toàn bộ 12 căn seed thỏa bất biến đồng bộ trạng thái của AC-10. Cập nhật các assert đếm dòng seed bị ảnh hưởng trong `SchemaMigrationTest` (số tài khoản, số hồ sơ `'Hợp lệ'` thành 5 và danh sách file chạy lại seed) và đổi dòng sửa trạng thái căn rỗng ở `ThemSuaCanHoIntegrationTest:93` từ `'Đang sử dụng'` sang `'Đã bàn giao'`.

*Giao diện*

- **AC-15**: Component dùng chung `ChiTietCanHo` (hiển thị trên `/can-ho/[maCanHo]` với `choPhepSua = true` và trên `/can-ho-cua-toi` / `/can-ho-cua-toi/[maCanHo]` với `choPhepSua = false`) chỉ tải và hiển thị khối `"Hồ sơ cư trú"` khi thỏa `(choPhepSua && coVaiTro("BAN_QUAN_LY")) || (!choPhepSua && coVaiTro("CU_DAN"))`; nhờ vậy người dùng `BAN_QUAN_TRI`, `KE_TOAN` (kể cả khi kiêm thêm vai trò `CU_DAN`) mở `/can-ho/[maCanHo]` không gọi API nhân khẩu và không thấy khối này. Khối `"Hồ sơ cư trú"` có trạng thái đang tải (`Skeleton`) và báo lỗi kèm nút `"Thử lại"` (`QueryErrorState`) hoàn toàn độc lập với thẻ thông tin căn hộ phía trên.
- **AC-16**: Khối `"Hồ sơ cư trú"` chia hai tab (shadcn `Tabs`):
  - Tab mặc định `"Hiện tại"` hiển thị bảng hoặc danh sách nhân khẩu `dangCuTru` (họ tên, vai trò cư trú, tình trạng cư trú, ngày bắt đầu, ngày kết thúc hoặc `"Không thời hạn"`, và riêng BQL thấy thêm số điện thoại, số CCCD, người xác minh) cùng mục con `"Sắp bắt đầu"` (nếu `sapBatDau` khác rỗng); khi cả hai rỗng thì hiện trạng thái rỗng `"Căn hộ chưa có người đang cư trú"`.
  - Tab `"Lịch sử cư trú"` hiển thị `lichSuCuTru` kèm badge trạng thái hồ sơ (với cư dân có chú thích rõ `"Chỉ hiển thị lịch sử cư trú của bạn"`); khi rỗng hiện `"Chưa có lịch sử cư trú"`.
- **AC-17**: Trên `/can-ho/[maCanHo]` (`choPhepSua = true`) khi người dùng có `BAN_QUAN_LY`:
  - Khối `"Hồ sơ cư trú"` có nút `"Thêm hồ sơ cư trú"`, mở `ThemHoSoCuTruDialog` gồm combobox tìm chọn cư dân (tự tải 20 cư dân đầu ngay khi mở dialog và gọi lại có trì hoãn khi gõ phím, hiển thị họ tên, SĐT, CCCD cùng badge nếu tài khoản đang `'Tạm khóa'`), ô vai trò cư trú và ô tình trạng cư trú để trống ban đầu bắt chọn, ngày bắt đầu mặc định hôm nay (theo giờ `Asia/Ho_Chi_Minh`) và ngày kết thúc (bắt buộc khi chọn `'Tạm trú'`).
  - Mỗi dòng ở `dangCuTru` và `sapBatDau` có nút `"Sửa"` (mở `SuaHoSoCuTruDialog` chỉ gồm 4 trường `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau`, `ngayKetThuc` đã điền sẵn và gửi ngầm `trangThaiHoSo = 'Hợp lệ'`) và nút `"Kết thúc cư trú"` (hoặc `"Hủy hồ sơ"` với dòng `sapBatDau`, mở `KetThucCuTruDialog` gửi `trangThaiHoSo = 'Hết hiệu lực'`, điền sẵn `ngayKetThuc = homNay` cho `dangCuTru` và khóa chỉ đọc `ngayKetThuc = ngayBatDau` cho `sapBatDau`). Trong tab `"Lịch sử cư trú"`, mọi dòng đều ẩn nút `"Sửa"`; dòng đã `'Hết hiệu lực'` ẩn cả nút kết thúc, riêng dòng quá hạn đầu ngày chưa kịp chạy job (còn mang `'Hợp lệ'`) vẫn hiện nút `"Kết thúc cư trú"` cho BQL.
  - Thành công ở bất kỳ dialog nào: hiện toast tiếng Việt tương ứng, đóng dialog, invalidate `["ho-so-cu-tru", maCanHo]`, `CAN_HO_QUERY_KEY` (để badge trạng thái sử dụng trên thẻ căn hộ cập nhật ngay khi tự đồng bộ) và `HO_SO_QUERY_KEY`. Lỗi 400 và 422 có trường gắn ngay dưới ô nhập, lỗi 409/404/422 chung hiện trong `FormAlert` của dialog. Trong `ThemCanHoDialog`, bỏ lựa chọn `'Đang sử dụng'` khỏi ô trạng thái ban đầu vì căn mới chưa có cư dân; trong `SuaCanHoDialog`, lỗi 422 từ server gắn dưới ô trạng thái sử dụng.
- **AC-18**: Mọi tab, bảng, ô tìm chọn cư dân và hộp thoại mới dùng được trọn vẹn bằng bàn phím, nhìn rõ focus, mọi ô nhập có nhãn, lỗi gắn qua `aria-describedby`, trả focus về nút mở sau khi đóng hộp thoại, đạt chuẩn WCAG AA.

## Decision

**Chosen option**: Option 2: Ghi và truy vấn `ho_so_cu_tru` bằng `JdbcClient` dưới khóa dòng `can_ho`, kết hợp ràng buộc `EXCLUDE USING gist` ở PostgreSQL và đồng bộ hai chiều `trang_thai_su_dung`.

Mọi thay đổi cư trú của một căn hộ đi qua khóa dòng của chính căn hộ đó để vừa giữ bất biến khoảng ngày vừa cập nhật tức thì trạng thái sử dụng của căn trong cùng một transaction.

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

### Data model sketch

Không thêm bảng mới. Migration `V18__ho_so_cu_tru.sql` bổ sung hai cột thời điểm và siết ràng buộc trên `ho_so_cu_tru`:

| Bảng | Khóa chính | Cột và tính bắt buộc | Quan hệ | Thay đổi ở V18 |
|---|---|---|---|---|
| `ho_so_cu_tru` | `ma_ho_so` VARCHAR(30), mặc định `'HS' \|\| lpad(nextval('seq_ho_so_cu_tru')::text, 8, '0')` | `ma_can_ho` VARCHAR(20) NOT NULL; `ma_cu_dan` VARCHAR(30) NOT NULL; `ma_nvbql_xac_minh` VARCHAR(30) NOT NULL; `vai_tro_cu_tru` VARCHAR(40) NOT NULL; `tinh_trang_cu_tru` VARCHAR(40) NOT NULL; `ngay_bat_dau` DATE NOT NULL; `ngay_ket_thuc` DATE NULL; `trang_thai_ho_so` VARCHAR(40) NOT NULL; `tao_luc` TIMESTAMPTZ NOT NULL; `cap_nhat_luc` TIMESTAMPTZ NOT NULL | N:1 `can_ho`, N:1 `cu_dan`, N:1 `nhan_vien_bql` (đều `ON DELETE RESTRICT`) | Bật `btree_gist`; thêm `tao_luc`, `cap_nhat_luc`; thêm `chk_hsct_ngay_ket_thuc_bat_buoc`, `ex_hsct_cu_dan_khong_chong_ngay`, `ex_hsct_mot_chu_can_ho` |
| `can_ho` | `ma_can_ho` VARCHAR(20) | `trang_thai_su_dung` VARCHAR(40) NOT NULL | 1:N `ho_so_cu_tru` | Không đổi cấu trúc; khóa dòng và đồng bộ trạng thái ở tầng service |
| `cu_dan`, `nhan_vien_bql`, `nguoi_dung_he_thong` | như `V1` | chỉ đọc qua service công khai của `auth` | 1:1 `nguoi_dung_he_thong` | Không đổi cấu trúc |

DDL chính trong `V18__ho_so_cu_tru.sql` (truyền trực tiếp `ngay_ket_thuc` vào `daterange(ngay_bat_dau, ngay_ket_thuc, '[]')` để giá trị `NULL` biểu diễn đầu trên mở vô hạn của PostgreSQL mà không tràn kiểu `date`):

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE ho_so_cu_tru
    ADD COLUMN tao_luc TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN cap_nhat_luc TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD CONSTRAINT chk_hsct_ngay_ket_thuc_bat_buoc CHECK (
        (tinh_trang_cu_tru <> 'Tạm trú' AND trang_thai_ho_so <> 'Hết hiệu lực')
        OR ngay_ket_thuc IS NOT NULL),
    ADD CONSTRAINT ex_hsct_cu_dan_khong_chong_ngay EXCLUDE USING gist (
        ma_can_ho WITH =,
        ma_cu_dan WITH =,
        daterange(ngay_bat_dau, ngay_ket_thuc, '[]') WITH &&
    ) WHERE (trang_thai_ho_so = 'Hợp lệ'),
    ADD CONSTRAINT ex_hsct_mot_chu_can_ho EXCLUDE USING gist (
        ma_can_ho WITH =,
        daterange(ngay_bat_dau, ngay_ket_thuc, '[]') WITH &&
    ) WHERE (trang_thai_ho_so = 'Hợp lệ' AND vai_tro_cu_tru = 'Chủ căn hộ');
```

Entity `HoSoCuTru` (`@Immutable`) và `HoSoCuTruRepository` hiện có được giữ nguyên cho `CuTruService` để không phá đường tính `PhamViTruyCap`; toàn bộ lệnh ghi và truy vấn chi tiết mới nằm ở `HoSoCuTruGhi` và `HoSoCuTruQuery` dùng `JdbcClient`.

### State transitions

Vòng đời của `ho_so_cu_tru` trong lát cắt 13 (trạng thái `'Chờ xác minh'` do lát cắt 14 bổ sung):

```
[Tạo mới bởi BQL: POST /residency-records]
        │
        ▼
     Hợp lệ ──(PUT giữ Hợp lệ: sửa vai trò, tình trạng, ngày)──▶ Hợp lệ
        │
        ├──(PUT đặt Hết hiệu lực + ngayKetThuc)────────────────▶ Hết hiệu lực (kết thúc, không mở lại)
        └──(Job đầu ngày khi ngay_ket_thuc < homNay)───────────▶ Hết hiệu lực (kết thúc, không mở lại)
```

Đồng bộ với `can_ho.trang_thai_su_dung`:
- Ghi hồ sơ cư trú xong, đếm *hồ sơ đang cư trú hôm nay* của căn:
  - `>= 1` người: `'Trống'` hoặc `'Đã bàn giao'` chuyển sang `'Đang sử dụng'`; `'Đang sửa chữa'` và `'Đang sử dụng'` giữ nguyên.
  - `0` người: `'Đang sử dụng'` chuyển về `'Trống'`; `'Trống'`, `'Đã bàn giao'` và `'Đang sửa chữa'` giữ nguyên.
- Sửa tay căn hộ (`PUT /apartments/{id}`): chặn đặt `'Trống'` / `'Đã bàn giao'` khi số người đang cư trú hôm nay `>= 1`; chặn đặt `'Đang sử dụng'` khi số người đang cư trú hôm nay `= 0` (kể cả khi `POST /apartments`).

### Kiến trúc phân hệ và ranh giới phụ thuộc (không tạo vòng bean)

- `auth`: thêm `CuTruDinhDanhService` (chỉ phụ thuộc `JdbcClient`, không phụ thuộc `PhamViTinhToan`), cung cấp:
  - `maNvbqlCua(String maNguoiDung)` trả `Optional<String>` (mã `BQL...` từ `nhan_vien_bql`).
  - `timCuDanLuaChon(String q, int gioiHan)` phục vụ `resident-options`.
  - `chiTietCuDanTheoMa(Collection<String> maCuDan)` trả map `{ maCuDan -> ThongTinCuDan(maCuDan, maNguoiDung, hoTen, soDienThoai, email, soCccd, trangThaiTaiKhoan) }` bằng một câu SQL, và `hoTenNvbqlTheoMa(Collection<String> maNvbql)` trả map `{ maNv -> hoTen }` bằng một câu SQL.
- `canho`: thêm `CanHoCuTruService` (chỉ phụ thuộc `CanHoQuery` và `CanHoGhi`, không phụ thuộc `SuaCanHoService`), cung cấp `maToaNhaCua(maCanHo)`, `khoaVaDocTrangThai(maCanHo)` (`SELECT trang_thai_su_dung FROM can_ho WHERE ma_can_ho = :ma FOR UPDATE`) và `capNhatTrangThaiSuDung(maCanHo, trangThaiMoi)`. `SuaCanHoService` inject thêm `CuTruService` và `Clock` để kiểm bất biến AC-10 dưới khóa dòng; `ThemCanHoService` chặn `'Đang sử dụng'` ngay sau bước kiểm 400.
- `cutru`:
  - `CuTruService`: giữ `maCanHoDangCuTru(maCuDan, ngay)` và thêm `coNguoiDangCuTru(maCanHo, ngay)` cùng `maCuDanCuaNguoiDung` (hoặc nhận từ `CuTruDinhDanhService`), chỉ phụ thuộc `HoSoCuTruRepository` nên `PhamViTinhToan` và `SuaCanHoService` gọi vào không tạo vòng phụ thuộc.
  - `HoSoCuTruCanHoController` (`/api/v1/apartments/{apartmentId}/residents`) và `HoSoCuTruController` (`/api/v1/residency-records`, `/resident-options`, `/{recordId}`).
  - `TraCuuHoSoCuTruService`, `ThemHoSoCuTruService`, `CapNhatHoSoCuTruService`, `HetHanHoSoCuTruService`, `HoSoCuTruHetHanJob`, `DongBoTrangThaiCanHo` (helper nội bộ tính trạng thái căn mới), `NhapLieuCuTru`, `CuTruErrorCodes`, `CuTruLoi`.
- `nhatky`: `LoaiThaoTac` thêm 3 mã của AC-12; `DoiTuongTacDong` thêm `hoSoCuTru(maHoSo)` (`"HoSoCuTru:" + maHoSo`).
- `common`: `LoiCoSoDuLieu` thêm `LOAI_TRU_GIST = "23P01"`; `AppProperties` (hoặc nhánh `app.cutru.residency-expiry-cron` trong `application.yml`) cấp lịch chạy job đầu ngày.

### API surface

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/apartments/{apartmentId}/residents` (UC02-01) | GET | `apartmentId`: path string (req) | 200, `HoSoCuTruCanHoResponse { dangCuTru, sapBatDau, lichSuCuTru }` (ẩn PII và lọc lịch sử với `CU_DAN`) | `@ChoPhep(BAN_QUAN_LY, CU_DAN)` | 403 `FORBIDDEN`, 404 `NOT_FOUND` |
| `/api/v1/residency-records/resident-options` | GET | `q`: query string 0..100 (opt) | 200, `List<CuDanLuaChonResponse>` (tối đa 20) | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 403 `FORBIDDEN` |
| `/api/v1/residency-records` (UC02-02) | POST | `maCanHo`, `maCuDan`, `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau` (req); `ngayKetThuc` (opt, bắt buộc khi `Tạm trú`) | 201, `HoSoCuTruResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 404 `NOT_FOUND`, 409 `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN` / `CUTRU_DA_CO_CHU_CAN_HO`, 422 `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` |
| `/api/v1/residency-records/{recordId}` (UC02-03) | PUT | `recordId`: path; `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau`, `trangThaiHoSo` (req); `ngayKetThuc` (opt/req) | 200, `HoSoCuTruResponse` | `@ChoPhep(BAN_QUAN_LY)` | 400 `VALIDATION_FAILED`, 404 `NOT_FOUND`, 409 `CUTRU_HO_SO_DA_HET_HIEU_LUC` / `CUTRU_HO_SO_DANG_CHO_XAC_MINH` / 2 mã trùng ngày |
| `/api/v1/apartments/{apartmentId}` (UC01-08, bổ sung luật) | PUT | như spec 0012 | 200, `CanHoResponse` | `@ChoPhep(BAN_QUAN_LY)` | thêm 422 `CANHO_TRANG_THAI_MAU_THUAN_CU_TRU` |
| `/api/v1/apartments` (UC01-07, bổ sung luật) | POST | như spec 0012 | 201, `CanHoResponse` | `@ChoPhep(BAN_QUAN_LY)` | thêm 422 `CANHO_TRANG_THAI_MAU_THUAN_CU_TRU` khi gửi `'Đang sử dụng'` |

### Value sourcing

| Action | Value produced / displayed | Source |
|---|---|---|
| Mọi luồng tính ngày cư trú | `homNay` (`LocalDate`) | `LocalDate.now(clock)` với bean `Clock` mang múi giờ `app.timezone` (`Asia/Ho_Chi_Minh`) |
| Thêm / sửa hồ sơ | `ma_ho_so` | PostgreSQL sinh từ `seq_ho_so_cu_tru`, lấy qua `INSERT ... RETURNING ma_ho_so` |
| Thêm / sửa hồ sơ | `ma_nvbql_xac_minh` | `CuTruDinhDanhService.maNvbqlCua(phamVi.maNguoiDung())` tra bảng `nhan_vien_bql` |
| Thêm / sửa / job hết hạn | `tao_luc`, `cap_nhat_luc` | `Instant.now(clock).truncatedTo(ChronoUnit.MICROS)` |
| Thêm / sửa / job hết hạn | `trang_thai_su_dung` mới của căn | tính trong `DongBoTrangThaiCanHo` từ trạng thái căn đang khóa và số *hồ sơ đang cư trú hôm nay* sau khi ghi |
| Nhật ký kiểm toán | `ma_nguoi_dung` người thực hiện | `PhamViTruyCap.maNguoiDung()` khi BQL gọi API; `null` khi chạy trong `HoSoCuTruHetHanJob` |
| Nhật ký kiểm toán | `trangThaiCanHo { cu, moi }` | so sánh trạng thái căn trước và sau bước đồng bộ trong cùng transaction; không đổi thì `null` (ẩn khỏi JSON) |
| Xem nhân khẩu căn hộ | nhóm `dangCuTru`, `sapBatDau`, `lichSuCuTru` | phân loại từ các dòng `ho_so_cu_tru` khác `'Chờ xác minh'` của căn theo `trang_thai_ho_so`, `ngay_bat_dau`, `ngay_ket_thuc` và `homNay` |
| Xem nhân khẩu căn hộ | `hoTen`, `soDienThoai`, `email`, `soCccd`, `maNguoiDung` | `CuTruDinhDanhService.chiTietCuDanTheoMa` gom một lần cho toàn response; đặt `null` các trường PII khi người gọi chỉ đạt quyền `CU_DAN` |
| Xem nhân khẩu căn hộ | `hoTenNguoiXacMinh` | `CuTruDinhDanhService.hoTenNvbqlTheoMa` gom một lần; đặt `null` cùng `maNvbqlXacMinh` khi người gọi chỉ đạt quyền `CU_DAN` |
| Xem nhân khẩu căn hộ (quyền `CU_DAN`) | `ma_cu_dan` của chính người gọi để lọc lịch sử | tra qua `CuTruDinhDanhService` từ `PhamViTruyCap.maNguoiDung()` |
| UI hộp thoại kết thúc nhanh | giá trị điền sẵn của `ngayKetThuc` | ngày hôm nay theo múi giờ `Asia/Ho_Chi_Minh` trên trình duyệt, hoặc `ngayBatDau` của hồ sơ nếu `ngayBatDau` lớn hơn hôm nay |

### Key invariants

- Không bao giờ có hai dòng `ho_so_cu_tru` trạng thái `'Hợp lệ'` của cùng `(ma_can_ho, ma_cu_dan)` giao khoảng ngày (cả mức service dưới khóa dòng `can_ho` lẫn mức DB qua `ex_hsct_cu_dan_khong_chong_ngay`).
- Tại mỗi ngày, mỗi căn hộ có tối đa một dòng `'Hợp lệ'` mang vai trò `'Chủ căn hộ'` (cả mức service lẫn mức DB qua `ex_hsct_mot_chu_can_ho`).
- Mọi hồ sơ `'Tạm trú'` và mọi hồ sơ `'Hết hiệu lực'` đều có `ngay_ket_thuc >= ngay_bat_dau` (`chk_hsct_ngay` và `chk_hsct_ngay_ket_thuc_bat_buoc`).
- Chuyển sang `'Hết hiệu lực'` là chiều cuối cùng; không thể sửa hay mở lại hồ sơ đã `'Hết hiệu lực'`.
- Mọi transaction đổi `ho_so_cu_tru` của một căn hoặc đổi `can_ho.trang_thai_su_dung` đều lấy khóa `SELECT ... FOR UPDATE` trên dòng `can_ho` trước tiên theo cùng một thứ tự khóa (`can_ho` rồi mới tới `ho_so_cu_tru`), nên không bao giờ deadlock và không bao giờ để căn có người đang cư trú hôm nay mang trạng thái `'Trống'` hay `'Đã bàn giao'`, cũng không để căn không có người đang cư trú hôm nay mang trạng thái `'Đang sử dụng'`.
- Không xóa vật lý dòng `ho_so_cu_tru`.

### Security model

- Tính năng xử lý dữ liệu định danh cá nhân nhạy cảm (số CCCD 12 chữ số, số điện thoại, email, lịch sử nơi ở).
- Ghi dữ liệu (`POST`, `PUT`): chỉ `BAN_QUAN_LY` còn dòng `nhan_vien_bql`, giới hạn nghiêm ngặt trong các tòa thuộc `PhamViTruyCap.maToaNha()` (ngoài phạm vi trả 404). Endpoint tìm chọn cư dân (`resident-options`) chỉ dành cho `BAN_QUAN_LY` và tra cứu trên tập cư dân có tài khoản `'Hoạt động'` hoặc `'Tạm khóa'` toàn hệ thống (vì người chưa có hồ sơ cư trú chưa thuộc tòa nhà nào).
- Đọc nhân khẩu căn hộ (`GET /apartments/{id}/residents`): chỉ `BAN_QUAN_LY` (theo tòa) và `CU_DAN` (theo căn đang cư trú hôm nay). `BAN_QUAN_TRI` và `KE_TOAN` không có quyền gọi endpoint này (trả 403) vì nghiệp vụ tài chính và giám sát của họ ở lát này chưa cần danh tính nhân khẩu.
- Tối thiểu hóa PII cho `CU_DAN`: người chỉ có quyền cư dân của căn không bao giờ nhận được `soCccd`, `soDienThoai`, `email`, `maNguoiDung`, `maNvbqlXacMinh`, `hoTenNguoiXacMinh` từ API (server gán `null` trước khi trả, không chỉ ẩn ở giao diện), và không bao giờ thấy `sapBatDau` hay `lichSuCuTru` của người khác.
- Nhật ký kiểm toán bắt buộc cho mọi lần thêm, sửa, kết thúc (kể cả tự động đầu ngày), chỉ ghi mã định danh (`maCanHo`, `maCuDan`, `maHoSo`) và trường nghiệp vụ cư trú, không bao giờ ghi PII.

### Configuration required

Không thêm biến môi trường bắt buộc mới. Chỉ bổ sung thuộc tính cấu hình nội bộ có giá trị mặc định trong `backend/src/main/resources/application.yml`:
- `app.cutru.residency-expiry-cron`: biểu thức cron chạy `HoSoCuTruHetHanJob` theo `app.timezone`, mặc định `"0 5 0 * * *"`.

### Critical test scenarios

- Happy path (integration): BQL tìm cư dân qua `resident-options`, thêm hồ sơ `'Chủ căn hộ'` `'Thường trú'` bắt đầu hôm nay vào căn `'Trống'` `A-0102` nhận 201; `GET /apartments/A-0102` trả `trangThaiSuDung = 'Đang sử dụng'`; cư dân đó đăng nhập hoặc gọi `GET /apartments/A-0102/residents` nhận 200; BQL kết thúc hồ sơ sang `'Hết hiệu lực'` hôm nay nhận 200, căn `A-0102` tự về `'Trống'`, cư dân gọi lại `GET /apartments/A-0102` nhận 404, có đủ hai dòng nhật ký kèm `trangThaiCanHo`, verifies **AC-1**, **AC-3**, **AC-4**, **AC-7**, **AC-10**, **AC-12**, **AC-13**
- Failure case (chồng ngày & chạy song song): hai luồng `CountDownLatch` cùng thêm `'Chủ căn hộ'` (hoặc cùng một `maCuDan`) giao khoảng ngày vào `A-0102` trả đúng một 201 và một 409 (`CUTRU_DA_CO_CHU_CAN_HO` hoặc `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN`); chèn SQL tay vi phạm khoảng ngày hoặc `'Tạm trú'` thiếu `ngay_ket_thuc` bị `V18` từ chối, verifies **AC-6**, **AC-8**, **AC-9**
- Failure case (đồng bộ ngược & vòng đời): BQL gọi `PUT /apartments/A-0101` (đang có cư dân) đặt `'Trống'` hoặc `'Đã bàn giao'` nhận 422 `CANHO_TRANG_THAI_MAU_THUAN_CU_TRU` ở trường `trangThaiSuDung`, nhưng đặt `'Đang sửa chữa'` nhận 200; gọi `PUT /apartments/A-0102` (đang trống) đặt `'Đang sử dụng'` hoặc `POST /apartments` với `'Đang sử dụng'` nhận 422; sửa lại hồ sơ đã `'Hết hiệu lực'` nhận 409 `CUTRU_HO_SO_DA_HET_HIEU_LUC`; thêm hồ sơ cho tài khoản `'Ngừng hoạt động'` nhận 422 `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG`; hủy sớm hồ sơ `sapBatDau` đặt `ngayKetThuc = ngayBatDau` nhận 200, verifies **AC-5**, **AC-6**, **AC-8**, **AC-10**
- Job hết hạn đầu ngày (integration): hồ sơ `'Hợp lệ'` có `ngay_ket_thuc = homNay.minusDays(1)` trên căn đang `'Đang sử dụng'` sau khi chạy `HetHanHoSoCuTruService` chuyển sang `'Hết hiệu lực'`, căn về `'Trống'`, nhật ký ghi `CUTRU_KET_THUC_HO_SO` với `ma_nguoi_dung = null` và `tuDong = true`, verifies **AC-11**, **AC-12**
- Auth/permission & PII: `CU_DAN` của `A-0101` gọi `GET /apartments/A-0101/residents` thấy người cùng ở trong `dangCuTru` với `soCccd`, `soDienThoai`, `email`, `maNvbqlXacMinh`, `hoTenNguoiXacMinh` đều `null` và không thấy dòng lịch sử `'Hết hiệu lực'` của người khác trong `lichSuCuTru`; `KE_TOAN` và `BAN_QUAN_TRI` gọi `GET /apartments/A-0101/residents` nhận 403; `CU_DAN` gọi `POST`, `PUT`, `resident-options` nhận 403, verifies **AC-2**, **AC-13**
- UI (Vitest + Testing Library): trên `/can-ho/A-0101`, BQL thấy hai tab nhân khẩu và các nút Thêm, Sửa, Kết thúc cư trú, còn Kế toán và BQT không thấy khối nhân khẩu; trên `/can-ho-cua-toi`, cư dân thấy nhân khẩu đang ở không lộ CCCD/SĐT và không thấy nút sửa; lỗi API nhân khẩu hiện nút Thử lại mà không ẩn thông tin căn hộ, verifies **AC-15**, **AC-16**, **AC-17**, **AC-18**

## Build plan

Thứ tự theo Tracer Bullet và quy trình tách backend trước, frontend sau của scope từ tính năng 17 (3 mốc backend rồi tới 2 mốc frontend):

1. **Backend đường mỏng: V18, tra cứu nhân khẩu căn hộ và thêm mới hồ sơ.** Viết `V18__ho_so_cu_tru.sql`, bổ sung `LoiCoSoDuLieu.LOAI_TRU_GIST`; thêm `CuTruDinhDanhService` (`auth`) và `CanHoCuTruService` (`canho`); viết `HoSoCuTruQuery`, `HoSoCuTruGhi`, `DongBoTrangThaiCanHo`, `TraCuuHoSoCuTruService`, `ThemHoSoCuTruService`, `GET /apartments/{id}/residents`, `GET /residency-records/resident-options`, `POST /residency-records` và nhật ký `CUTRU_THEM_HO_SO`. Viết unit test và integration test cho đường đọc (lọc PII cư dân), tạo mới, đồng bộ căn sang `'Đang sử dụng'` và chạy song song. Satisfies **AC-1**, **AC-2**, **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-9**, **AC-10**, **AC-12**, **AC-13**
2. **Backend làm dày: sửa, kết thúc hồ sơ và chặn sửa tay căn hộ mâu thuẫn.** Viết `CapNhatHoSoCuTruService`, `PUT /residency-records/{recordId}`, hai mã nhật ký `CUTRU_CAP_NHAT_HO_SO` và `CUTRU_KET_THUC_HO_SO`; bổ sung `CuTruService.coNguoiDangCuTru` và kiểm 422 `CANHO_TRANG_THAI_MAU_THUAN_CU_TRU` trong `SuaCanHoService` cùng `ThemCanHoService` (cập nhật `ThemSuaCanHoIntegrationTest:93`). Viết unit test và integration test cho sửa không đổi, kết thúc cư trú, hủy sớm hồ sơ `sapBatDau` và chặn sửa tay trạng thái căn. Satisfies **AC-7**, **AC-8**, **AC-10**, **AC-12**, **AC-13**
3. **Backend job hết hạn, dữ liệu mẫu và khóa OpenAPI.** Viết `HetHanHoSoCuTruService` và `HoSoCuTruHetHanJob` (`app.cutru.residency-expiry-cron`), thêm `R__seed_04_cu_tru.sql` và cập nhật `SchemaMigrationTest`; kiểm `PhanQuyenCoverageTest`, `ChiTietNhatKyAnToanTest` và chạy `mvnw verify` sinh `docs/api/openapi.json`. Satisfies **AC-11**, **AC-12**, **AC-13**, **AC-14**
4. **Frontend đường mỏng hiển thị: khối Hồ sơ cư trú trên `ChiTietCanHo`.** Chạy `pnpm gen:api`; thêm `frontend/src/lib/cu-tru.ts` và khối `"Hồ sơ cư trú"` hai tab (`Hiện tại`, `Lịch sử cư trú`) tích hợp vào `ChiTietCanHo` cho cả `/can-ho/[maCanHo]` (chỉ hiện với `BAN_QUAN_LY`) và `/can-ho-cua-toi` (cho `CU_DAN`), kèm trạng thái `Skeleton`, rỗng và `QueryErrorState` độc lập. Viết test hiển thị theo vai trò và kiểm tra không lộ PII. Satisfies **AC-15**, **AC-16**, **AC-18**
5. **Frontend thao tác BQL và hoàn tất.** Viết `them-ho-so-cu-tru-dialog.tsx` (combobox tìm cư dân từ `resident-options`), `sua-ho-so-cu-tru-dialog.tsx`, `ket-thuc-cu-tru-dialog.tsx`, bỏ lựa chọn `'Đang sử dụng'` trong `ThemCanHoDialog`; kiểm tra invalidate cache đồng bộ thẻ căn hộ, bàn phím, focus và WCAG AA; chạy `pnpm typecheck`, `pnpm lint`, `pnpm format:check`, `pnpm test` và `pnpm build`. Satisfies **AC-17**, **AC-18**

## Consequences

**Positive**:
- Trạng thái sử dụng của căn hộ và quyền truy cập căn hộ của cư dân luôn đồng nhất với hồ sơ cư trú thực tế, không còn cảnh căn hộ `'Trống'` mà vẫn có người đang ở hoặc căn hộ `'Đang sử dụng'` mà không có ai.
- Ràng buộc `EXCLUDE USING gist` ngay trong PostgreSQL bảo đảm không bao giờ lọt hai hồ sơ chồng ngày cho cùng một người hoặc hai chủ căn hộ cùng thời điểm, kể cả khi nhiều request chạy đua hoặc chèn dữ liệu ngoài ứng dụng.
- Cư dân xem được thành viên cùng căn và lịch sử của riêng mình mà không rò rỉ CCCD, SĐT, email hay thông tin cư dân cũ.

**Negative / tradeoffs**:
- Mọi thao tác thêm, sửa, kết thúc hồ sơ cư trú của cùng một căn hộ phải xếp hàng qua khóa dòng `can_ho` (nhưng số nhân khẩu mỗi căn rất nhỏ nên độ tranh chấp gần như bằng không).
- Bật thêm extension chuẩn `btree_gist` trong PostgreSQL 18 (có sẵn trong gói cài mặc định của `postgres:18.7`, không cần cài thêm gói hệ điều hành).
- Khi BQL nhập nhầm căn hộ hoặc chọn nhầm cư dân trên một hồ sơ đã tạo, không thể đổi `maCanHo` hay `maCuDan` tại chỗ mà phải kết thúc hồ sơ nhầm rồi tạo hồ sơ mới (đổi lại lịch sử kiểm toán luôn minh bạch).
- Cột `ma_nvbql_xac_minh` ghi đè theo nhân viên BQL sửa lần gần nhất; muốn tra người tạo ban đầu sau khi hồ sơ đã sửa thì xem dòng `CUTRU_THEM_HO_SO` trong nhật ký kiểm toán.

**Neutral**:
- Seed tăng từ 8 lên 10 tài khoản và từ 3 lên 5 hồ sơ cư trú `'Hợp lệ'` (cộng 1 hồ sơ `'Hết hiệu lực'`), để hai căn `'Đang sử dụng'` `A-0103` và `A-0204` có cư dân thật.
- Không thêm trang mới trong ma trận điều hướng `muc-chuc-nang.ts`; màn hình danh sách tập trung `/ho-so-cu-tru` sẽ mở ở tính năng 18.

## Follow-up

- [ ] Tính năng 18 (Đề nghị cư trú và tra cứu hồ sơ): mở luồng `'Chờ xác minh'` cho `API-UC02-04` (cần chốt `ma_nvbql_xac_minh` cho phép `NULL` khi cư dân mới gửi đề nghị), duyệt hoặc từ chối đề nghị dưới cùng khóa dòng `can_ho` và hàm `DongBoTrangThaiCanHo` của spec này, đồng thời dựng trang `/ho-so-cu-tru`.
- [ ] Tính năng 19 (Phương tiện gửi xe căn hộ): dùng `CuTruService` để kiểm tra điều kiện chủ xe cư trú tại căn hộ khi đăng ký phương tiện (`API-UC02-06`).
- [ ] Giải quyết mục Follow-up đầu tiên và một nửa mục thứ hai của spec 0012 (luật đồng bộ `trang_thai_su_dung` và hiển thị nhân khẩu trên trang chi tiết căn hộ); đối chiếu khi chạy `/sync`.
- [ ] Đặc tả Excel của `API-UC02-01` đến `03` dùng payload mẫu chưa khớp bảng `cu_dan` (truyền `hoTen` thay vì `maCuDan` và trả `maDinhDanh`); cập nhật lại file Excel nếu nhóm cần nộp tài liệu đặc tả đồng bộ code.
