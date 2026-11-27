# Verify: phương tiện gửi xe căn hộ · spec 0016 · updated 2026-11-27
_Steps derived from spec 0016 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: backend chạy profile `dev` và `seed`, mật khẩu chung `ChungCu@2026`. BQL `0900000002`, kế toán `0900000004`, cư dân kiêm BQT `0900000006` (A-0101), cư dân `0900000007` (A-0201).

## API / manual
- [x] Khởi động trên database mới → Flyway chạy tới `V20`, `phuong_tien` không còn `phuong_tien_bien_so_key`, có `uq_phuong_tien_bien_so_hieu_luc` → AC-1
- [x] BQL `GET /api/v1/apartments/A-0101/vehicles` → `dangGui` 2 xe (cũ trước), `donVaLichSu` có đơn `Chờ duyệt` đứng đầu → AC-2, AC-13
- [x] `GET` với `a-0101` và căn không tồn tại → 200 và 404 `NOT_FOUND` → AC-2
- [x] Cư dân `0900000007` `GET` A-0101 → 404; kế toán `GET` A-0101 → 200 đủ dữ liệu, không có CCCD, số điện thoại, email → AC-2, AC-3
- [x] BQL tạm dừng xe của một cư dân, cư dân khác cùng căn (không có vai trò BQT, kế toán) xem → thấy xe trong `dangGui`, `lyDoTrangThai`, `maNvbqlCapNhat`, `hoTenNguoiCapNhat` là `null` → AC-3
- [x] BQL đăng ký ô tô biển `30a-123.99` cho chủ hộ → 201, `bienSo = "30A12399"`, `Đang hoạt động`, `maCuDanDangKy = null` → AC-5
- [x] Cư dân gửi đơn xe máy cho người cùng căn → 201 `Chờ duyệt`, `maCuDanDangKy` là chính mình, `coTheRutDon = true` → AC-5
- [x] Người vừa là BQL vừa là cư dân đăng ký → vào thẳng `Đang hoạt động` → AC-5
- [x] Body `{}` và body sai kiểu mọi trường → 400, lỗi đúng thứ tự `maCanHo`, `maCuDanSoHuu`, `loaiPhuongTien`, `bienSo`, `nhanHieuMauSac` → AC-6
- [x] Xe đạp không biển → 201; xe máy không biển → 400 → AC-6
- [x] Chủ xe không ở căn → 422 `PHUONGTIEN_CHU_XE_KHONG_CU_TRU` (lỗi trên `maCuDanSoHuu`); chủ xe tài khoản ngừng hoạt động → 422 `PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG` → AC-7
- [x] Đơn chờ thứ 11 của một căn → 422 `PHUONGTIEN_VUOT_SO_DON_CHO_DUYET`; BQL vẫn đăng ký được → AC-7
- [x] Đăng ký biển đang hoạt động ở căn khác: BQL thấy mã xe và mã căn đang giữ, cư dân chỉ thấy câu chung → AC-7
- [x] Hai request song song cùng biển trên hai căn → một 201, một 409 `PHUONGTIEN_TRUNG_BIEN_SO` → AC-8
- [x] `PUT .../status` thiếu lý do khi tạm dừng, từ chối, hủy → 400; lý do 501 ký tự → 400 → AC-9
- [x] Đi hết ma trận: duyệt, từ chối, tạm dừng, kích hoạt lại, hủy → 200; `Đã hủy` sang `Đang hoạt động` → 409 → AC-10
- [x] Hai đơn chờ cùng biển: duyệt đơn đầu 200, duyệt đơn sau 409 kèm mã xe và căn → AC-10
- [x] Kết thúc hồ sơ cư trú của chủ xe → xe giữ trạng thái, `chuXeConCuTru = false`; kích hoạt lại → 422, `errors` rỗng → AC-4, AC-10
- [x] Người gửi rút đơn chờ → 200 `Đã hủy`; rút lại → 409; người khác rút → 404 → AC-11
- [x] Mỗi thao tác ghi thành công có đúng một dòng nhật ký `PhuongTien:PT...`, chi tiết không có họ tên, CCCD; request lỗi không ghi → AC-12
- [x] Kế toán, BQT gọi `POST`/`PUT` (BQT không kèm cư dân) → 403; cư dân gọi `PUT .../status` → 403 → AC-9, AC-11

## Value sourcing
- [x] Đổi giờ máy hoặc `Clock` qua nửa đêm giờ Việt Nam → điều kiện cư trú và `taoLuc` theo `Asia/Ho_Chi_Minh` (hồ sơ kết thúc hôm qua thì bị chặn) → `homNay`, `bayGio`
- [x] Đăng ký hai xe liên tiếp → `maPhuongTien` dạng `PT` + 8 chữ số, tăng dần → mã phương tiện
- [x] Nhập `29 b1-123.45` (kể cả khoảng trắng NBSP) → lưu `29B112345` → biển số lưu trữ
- [x] Cùng body, gọi bằng BQL rồi bằng cư dân → `Đang hoạt động` và `Chờ duyệt` → trạng thái ban đầu
- [x] Hai cư dân khác nhau gửi đơn → `maCuDanDangKy` đúng người gọi → người gửi đơn
- [x] BQL đăng ký và đổi trạng thái → `maNvbqlCapNhat` là BQL đang gọi → người cập nhật
- [x] Chủ xe có hồ sơ bắt đầu ngày mai → 422 → cư trú hợp lệ hôm nay
- [x] Đổi tài khoản chủ xe sang `Tạm khóa` (được) và `Ngừng hoạt động` (422), cả khi kích hoạt lại → trạng thái tài khoản
- [x] Rút bớt một đơn trong 10 đơn → gửi được đơn mới → số đơn chờ
- [x] Biển của xe `Tạm dừng` → vẫn bị chặn; của xe `Đã hủy`, `Từ chối` → dùng lại được → xe đang giữ biển
- [x] Rút đơn và kích hoạt không kèm lý do → `lyDoTrangThai = null` → lý do trạng thái
- [x] Danh sách trả `hoTenChuXe`, `hoTenNguoiDangKy` đúng người → họ tên cư dân
- [x] Góc nhìn nhân viên có `hoTenNguoiCapNhat`, góc nhìn cư dân là `null` → họ tên BQL
- [x] Sửa hồ sơ cư trú của chủ xe → `chuXeConCuTru` đổi ngay lần xem sau → cờ còn cư trú
- [ ] BQL ngoài tòa phạm vi → 404; BQT kiêm cư dân xem căn tòa mình → góc nhìn nhân viên → góc nhìn
- [x] BQT kiêm cư dân là người gửi đơn → `coTheRutDon = true` dù ở góc nhìn nhân viên → mã cư dân người gọi
- [x] Nhật ký đổi trạng thái có `trangThai.cu` đúng trạng thái trước; đăng ký có `cu = null` → trạng thái cũ
- [x] (Frontend) Danh sách chủ xe trong hộp thoại đăng ký lấy từ mảng `dangCuTru` của `GET /apartments/{id}/residents`, cư dân mặc định chọn chính mình; các trạng thái đích của hộp thoại đổi trạng thái khớp ma trận AC-10

## Commands
- [x] `cd backend && mvnw.cmd verify` → xanh, gồm `PhanQuyenCoverageTest`, `ChiTietNhatKyAnToanTest` → AC-13
- [x] `git diff docs/api/openapi.json` → có `GET /apartments/{apartmentId}/vehicles`, `POST /vehicles`, `PUT /vehicles/{vehicleId}/status`, `POST /vehicles/{vehicleId}/cancel` → AC-13

## Acceptance-criteria coverage
- AC-1 đến AC-16: mỗi tiêu chí đều có bước kiểm chứng runtime và kiểm tra tự động ở trên cùng phần giao diện bên dưới

## UI / manual (bổ sung cho AC-14 đến AC-16)
- [x] Mở `/can-ho/A-0101` bằng tài khoản BQL → thấy khối `Phương tiện` với hai tab `Đang gửi` và `Đơn và lịch sử (N)`, nhãn cảnh báo `Chủ xe không còn cư trú` khi `chuXeConCuTru = false`, và nút `Đăng ký phương tiện` → AC-14, AC-15
- [x] Mở `/can-ho/A-0101` bằng tài khoản Kế toán hoặc BQT → xem được cả hai tab và nhãn cảnh báo, không thấy nút `Đăng ký phương tiện` hay `Đổi trạng thái` → AC-14, AC-15
- [x] Mở `/can-ho-cua-toi` bằng tài khoản Cư dân (kể cả kiêm Kế toán hoặc BQT) → thấy nút `Gửi đơn đăng ký xe`, không thấy nhãn `Chủ xe không còn cư trú`, mở tab `Đơn và lịch sử` thấy nút `Rút đơn` ở đơn chờ duyệt của mình → AC-14, AC-15, AC-16
- [x] Mở hộp thoại `Đăng ký phương tiện` và nhập biển số đang hoạt động → lỗi `409 PHUONGTIEN_TRUNG_BIEN_SO` gắn ngay dưới ô `Biển số`, chọn `Xe đạp` thì ô `Biển số` không bắt buộc → AC-15
- [x] BQL mở hộp thoại `Đổi trạng thái thẻ xe` trên một dòng → danh sách trạng thái đích khớp ma trận AC-10 và bắt buộc nhập `Lý do` khi chọn `Tạm dừng`, `Từ chối` hoặc `Đã hủy` → AC-16
- [x] Cư dân bấm `Rút đơn` và xác nhận trên hộp thoại xác nhận → đơn chuyển sang `Đã hủy` và danh sách tự làm mới → AC-16
- [x] Khi API `/apartments/{id}/vehicles` trả lỗi → thông tin căn hộ phía trên vẫn giữ nguyên, khối `Phương tiện` hiện `QueryErrorState` với nút `Thử lại` → AC-14

## Commands (frontend)
- [x] `cd frontend && pnpm gen:api && pnpm format:check && pnpm typecheck && pnpm lint && pnpm test && pnpm build` → sinh kiểu từ OpenAPI và mọi kiểm tra đều xanh → AC-14, AC-15, AC-16

## Acceptance-criteria coverage (frontend)
- AC-14: khối `KhoiPhuongTien` hai tab trên `ChiTietCanHo`, phân biệt góc nhìn nhân viên và cư dân, `Skeleton`, `EmptyState`, `QueryErrorState`.
- AC-15: `DangKyPhuongTienDialog` lấy chủ xe từ `dangCuTru`, mặc định chính mình cho cư dân, gắn lỗi trường 400/409/422 và lỗi chung 422.
- AC-16: `DoiTrangThaiXeDialog` theo ma trận AC-10, `RutDonXeDialog` qua `ConfirmDialog`, từ điển `PHUONGTIEN_...` trong `error-messages.ts`, kiểm tra bàn phím và WCAG AA qua Vitest + axe.
