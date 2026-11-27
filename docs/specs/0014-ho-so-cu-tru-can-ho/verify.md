# Verify: Hồ sơ cư trú căn hộ (backend) · spec 0014 · updated 2026-11-26

_Steps derived from spec 0014 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chạy với `docker compose up -d` và `cd backend && mvnw.cmd spring-boot:run` (profile `dev,seed`). Tài khoản seed dùng mật khẩu `ChungCu@2026`: BQL `0900000002`, kế toán `0900000004`, BQT kiêm cư dân A-0101 `0900000006`, cư dân A-0201 `0900000007`, cư dân A-0103 `0900000009`, cư dân A-0204 `0900000010`.

## API / manual

- [x] BQL gọi `GET /api/v1/apartments/A-0101/residents` → 200, ba mảng `dangCuTru`, `sapBatDau`, `lichSuCuTru`; `lichSuCuTru` có dòng `Hết hiệu lực` của `Bùi Chủ Hộ`; mọi trường PII có giá trị → AC-1, AC-2
- [x] Cư dân `0900000006` gọi cùng endpoint cho `a-0101` → 200, `dangCuTru` chỉ có chính họ, `lichSuCuTru` rỗng, `soCccd`, `soDienThoai`, `email`, `maNguoiDung`, `maNvbqlXacMinh`, `hoTenNguoiXacMinh` đều `null` → AC-2
- [x] Cư dân `0900000006` gọi cho `A-0201` → 404 `NOT_FOUND`; kế toán gọi cho `A-0101` → 403 → AC-2, AC-13
- [x] BQL gọi `GET /api/v1/residency-records/resident-options?q=vu cu dan`, `q=+84 900 000 007`, `q=001026-000007` → mỗi lần đều có `Vũ Cư Dân`; `q` dài 101 ký tự → 400 mục `q` "Từ khóa tối đa 100 ký tự"; cư dân gọi → 403 → AC-3, AC-13
- [x] BQL `POST /api/v1/residency-records` cho `A-0102` (đang `Trống`) với cư dân `0900000007`, `Chủ căn hộ`, `Thường trú`, `ngayBatDau` hôm nay → 201; `GET /api/v1/apartments/A-0102` trả `Đang sử dụng`; nhật ký `CUTRU_THEM_HO_SO` có `trangThaiCanHo { cu: Trống, moi: Đang sử dụng }`, không có họ tên hay CCCD → AC-4, AC-10, AC-12
- [x] Gửi `POST` với body `{}` → 400, mục lỗi theo thứ tự `maCanHo`, `maCuDan`, `vaiTroCuTru`, `tinhTrangCuTru`, `ngayBatDau`; `Tạm trú` không có `ngayKetThuc` → "Tạm trú bắt buộc có ngày kết thúc" → AC-5
- [x] Gửi lại `POST` chủ căn hộ khác cho `A-0102` giao ngày → 409 `CUTRU_DA_CO_CHU_CAN_HO`; cùng cư dân giao ngày → 409 `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN`; `maCuDan` `CD99999999` → 404 "Không tìm thấy cư dân" → AC-6
- [x] BQL `PUT /api/v1/residency-records/{maHoSo}` (hồ sơ vừa tạo) với `trangThaiHoSo` `Hết hiệu lực`, `ngayKetThuc` hôm nay → 200; `A-0102` về `Trống`; `0900000007` gọi `GET /api/v1/apartments/A-0102` → 404; nhật ký `CUTRU_KET_THUC_HO_SO` có `tuDong: false` → AC-7, AC-10, AC-12, AC-13
- [x] `PUT` lại hồ sơ đó → 409 `CUTRU_HO_SO_DA_HET_HIEU_LUC`; `PUT` y nguyên một hồ sơ còn hợp lệ → 200, `capNhatLuc` không đổi, không thêm dòng nhật ký → AC-7, AC-8
- [x] Tạo hồ sơ bắt đầu ngày mai rồi `PUT` `Hết hiệu lực` với `ngayKetThuc` khác `ngayBatDau` → 400 "Hủy hồ sơ chưa tới ngày bắt đầu..."; bằng `ngayBatDau` → 200 và hồ sơ nằm cuối `lichSuCuTru` → AC-8
- [x] BQL `PUT /api/v1/apartments/A-0101` đặt `Trống` → 422 `CANHO_TRANG_THAI_MAU_THUAN_CU_TRU` mục `trangThaiSuDung`; đặt `Đang sửa chữa` → 200 (trả lại `Đang sử dụng` sau đó); `PUT /api/v1/apartments/A-0102` đặt `Đang sử dụng` khi căn trống → 422; `POST /api/v1/apartments` với `Đang sử dụng` → 422 → AC-10
- [x] Trong DB dev, chèn tay hồ sơ `Hợp lệ` có `ngay_ket_thuc` là hôm qua cho một căn `Đang sử dụng` chỉ có người đó, rồi khởi động lại backend → log "Hết hạn hồ sơ cư trú: soHoSo=1", hồ sơ thành `Hết hiệu lực`, căn về `Trống`, nhật ký `CUTRU_KET_THUC_HO_SO` có `ma_nguoi_dung` `NULL` và `tuDong: true` → AC-11, AC-12
- [x] Swagger `/swagger-ui.html` có nhóm "Cư trú" với 4 endpoint, đúng vai trò trong mô tả → AC-13

## Value sourcing

- [ ] `homNay` theo giờ Việt Nam: lúc 00:30 giờ Việt Nam (17:30 UTC hôm trước), hồ sơ có `ngayKetThuc` là ngày hôm trước phải nằm ở `lichSuCuTru`, không ở `dangCuTru`; hồ sơ bắt đầu hôm nay (giờ Việt Nam) phải ở `dangCuTru` → Value sourcing `homNay`
- [x] `maHoSo` khớp `^HS[0-9]{8}$` và tăng dần theo sequence → Value sourcing `ma_ho_so`
- [x] `maNvbqlXacMinh` của hồ sơ là mã `BQL...` của BQL vừa thêm hoặc sửa gần nhất; job hết hạn giữ nguyên giá trị này → Value sourcing `ma_nvbql_xac_minh`
- [x] `taoLuc` và `capNhatLuc` có tối đa 6 chữ số thập phân giây; `PUT` không đổi gì thì `capNhatLuc` giữ nguyên → Value sourcing `tao_luc`, `cap_nhat_luc`
- [x] Căn `Đang sửa chữa` không bao giờ tự đổi trạng thái khi thêm hay kết thúc hồ sơ → Value sourcing `trang_thai_su_dung`
- [x] `ma_nguoi_dung` của nhật ký là BQL khi gọi API, `NULL` khi job chạy; `trangThaiCanHo` vắng khỏi chi tiết khi căn không đổi → Value sourcing nhật ký
- [x] Cư dân chỉ thấy `sapBatDau` và `lichSuCuTru` của chính `ma_cu_dan` mình, kể cả khi người dùng có thêm vai trò khác như `BAN_QUAN_TRI` → Value sourcing `ma_cu_dan` của người gọi
- [x] Giá trị điền sẵn của `ngayKetThuc` trên hộp thoại kết thúc nhanh: đổi múi giờ máy sang `America/New_York` lúc 20:30 ngày trước (tức 08:30 sáng hôm sau giờ Việt Nam), mở hộp thoại `Kết thúc cư trú` cho hồ sơ đang ở sẽ điền sẵn ngày hôm nay theo giờ `Asia/Ho_Chi_Minh`; mở `Hủy hồ sơ` cho hồ sơ `sapBatDau` sẽ điền sẵn và khóa chỉ đọc bằng đúng `ngayBatDau` → Value sourcing `ngayKetThuc` trên UI

## UI / manual

- [x] Đăng nhập BQL (`0900000002`), mở `/can-ho/A-0101` → thấy khối `Hồ sơ cư trú` độc lập bên dưới thẻ thông tin căn hộ với nút `Thêm hồ sơ cư trú`, hai tab `Hiện tại` và `Lịch sử cư trú`; bảng `Hiện tại` hiện họ tên, vai trò, tình trạng, số điện thoại, số CCCD, ngày bắt đầu, `Không thời hạn` và người xác minh; tab `Lịch sử cư trú` hiện dòng `Hết hiệu lực` của `Bùi Chủ Hộ` và ẩn nút `Sửa` lẫn nút kết thúc → AC-15, AC-16, AC-17
- [x] Đăng nhập Kế toán (`0900000004`) và BQT kiêm cư dân (`0900000006`), mở `/can-ho/A-0101` → không thấy khối `Hồ sơ cư trú` và trình duyệt không gọi `GET /api/v1/apartments/A-0101/residents` → AC-15
- [x] Vẫn tài khoản `0900000006`, mở `/can-ho-cua-toi` → thấy khối `Hồ sơ cư trú` của `A-0101`, không có cột số điện thoại, số CCCD, người xác minh và không có nút thao tác; sang tab `Lịch sử cư trú` thấy dòng `Chỉ hiển thị lịch sử cư trú của bạn.` và trạng thái rỗng `Chưa có lịch sử cư trú` (ẩn dòng lịch sử của người khác) → AC-15, AC-16
- [x] Khi backend tắt hoặc giả lập lỗi trên `GET /api/v1/apartments/A-0101/residents` → thẻ thông tin căn hộ phía trên vẫn hiện đủ, chỉ khối `Hồ sơ cư trú` hiện trạng thái lỗi kèm nút `Thử lại`, bấm `Thử lại` tải lại được dữ liệu → AC-15
- [x] Đăng nhập BQL, mở `/can-ho` rồi bấm `Thêm căn hộ` → ô `Trạng thái sử dụng` chỉ có `Trống`, `Đang sửa chữa`, `Đã bàn giao` (không có `Đang sử dụng`); mở `/can-ho/A-0101` bấm `Sửa` căn hộ và chọn `Trống` → lỗi 422 gắn ngay dưới ô `Trạng thái sử dụng` → AC-17
- [x] Trên `/can-ho/A-0102` (đang `Trống`), BQL bấm `Thêm hồ sơ cư trú` → combobox `Cư dân` tự tải 20 cư dân đầu và lọc lại khi gõ phím (hiện họ tên, SĐT, CCCD và nhãn `Tạm khóa` nếu có); chọn `Tạm trú` mà bỏ trống ngày kết thúc bị báo `Tạm trú bắt buộc có ngày kết thúc`; lưu hồ sơ bắt đầu hôm nay → hiện toast `Đã thêm hồ sơ cư trú`, hộp thoại đóng, trả focus về nút mở và thẻ căn hộ phía trên tự đổi sang `Đang sử dụng` → AC-17, AC-18
- [x] Trên cùng căn `A-0102`, BQL bấm `Sửa` để cập nhật vai trò hoặc tình trạng cư trú → lưu thành công với toast `Đã cập nhật hồ sơ cư trú`; thêm một hồ sơ bắt đầu ngày mai để thấy mục con `Sắp bắt đầu` rồi bấm `Hủy hồ sơ` (ô ngày kết thúc khóa chỉ đọc bằng ngày bắt đầu) → lưu thành công; sau đó bấm `Kết thúc cư trú` cho hồ sơ đang ở → thẻ căn hộ tự quay về `Trống` → AC-16, AC-17
- [x] Kiểm tra bằng bàn phím trên `/can-ho/A-0101` và `/dev/ui`: dùng `Tab`, `ArrowLeft`/`ArrowRight` trên `Tabs`, `ArrowUp`/`ArrowDown`/`Enter`/`Escape` trên `Combobox`, nhìn rõ vòng focus ở mọi phần tử tương tác, đóng hộp thoại bằng `Escape` trả focus về đúng nút đã bấm → AC-18

## Commands

- [x] `cd backend && mvnw.cmd verify` → BUILD SUCCESS, 714 tests, `docs/api/openapi.json` có `/api/v1/residency-records` và `/api/v1/apartments/{apartmentId}/residents` → AC-13, AC-14
- [x] `mvnw.cmd test -Dtest='HoSoCuTru*IntegrationTest,HetHanHoSoCuTruIntegrationTest,CuTruLogicTest'` → xanh, gồm ca chạy song song hai chủ căn hộ (đúng một 201, một 409) → AC-6, AC-9, AC-11
- [x] `cd frontend && pnpm gen:api && pnpm lint && pnpm format:check && pnpm typecheck && pnpm test && pnpm build` → sinh kiểu OpenAPI sạch, 0 lỗi lint/format/typecheck, 60/60 tệp test Vitest (464 tests gồm `axe-core`, tương phản và chặn màu thô) xanh, build sản phẩm thành công → AC-15, AC-16, AC-17, AC-18

## Acceptance-criteria coverage

- AC-1, AC-2: các bước xem nhân khẩu đầu tiên và mục Value sourcing về `ma_cu_dan` · AC-3: bước `resident-options` · AC-4, AC-5, AC-6: các bước `POST` · AC-7, AC-8: các bước `PUT` · AC-9: lệnh test chạy song song (ràng buộc loại trừ) · AC-10: bước `POST`, `PUT` hồ sơ và `PUT/POST /apartments` · AC-11: bước job khi khởi động · AC-12: các bước kiểm nhật ký · AC-13: các bước 403, 404 và Swagger · AC-14: lệnh `mvnw verify` và các tài khoản seed `0900000009`, `0900000010` · AC-15: bước ẩn/hiện theo vai trò và lỗi độc lập trên UI · AC-16: bước hai tab `Hiện tại`, `Sắp bắt đầu`, `Lịch sử cư trú` và ẩn PII cư dân · AC-17: các bước hộp thoại BQL (`Thêm`, `Sửa`, `Kết thúc`, `Hủy hồ sơ`), làm mới cache và sửa hộp thoại căn hộ · AC-18: bước điều hướng bàn phím, focus, `axe-core` trong bộ lệnh frontend
