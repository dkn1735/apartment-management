# Verify: Tìm kiếm & quản lý căn hộ · spec 0012 · updated 2026-11-26

_Steps derived from spec 0012 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: `docker compose up -d`, backend chạy với `SPRING_PROFILES_ACTIVE=dev,seed`, `pnpm dev`. Mật khẩu seed `ChungCu@2026`. BQL 0900000002, Kỹ thuật 0900000003, Kế toán 0900000004, BQT kiêm cư dân A-0101 0900000006, cư dân A-0201 0900000007.

## UI / manual

- [x] BQL vào `/can-ho` → menu có mục "Căn hộ", bảng 12 căn tòa A, mặc định sắp theo mã căn, URL không có `page`, `size`, `sort` → AC-1, AC-4, AC-16, AC-17
- [x] Chọn tòa "Tòa A" → ô Tầng bật với "Tầng 1" đến "Tầng 3"; chọn Tầng 2, bấm Lọc → 4 căn A-0201 đến A-0204, URL có `maToaNha=A&soTang=2`, không có `page` → AC-2, AC-17
- [x] Đổi tòa về "Tất cả tòa nhà" rồi Lọc → URL mất cả `maToaNha` lẫn `soTang` → AC-17
- [x] Nhập "Diện tích từ" `45`, "Diện tích đến" `60,25`, Lọc → A-0102, A-0103, A-0202, A-0304; URL ghi `dienTichDen=60.25` → AC-2, AC-17
- [x] Nhập từ `80`, đến `50`, Lọc → lỗi "Diện tích đến phải lớn hơn hoặc bằng diện tích từ" dưới ô đến, URL không đổi → AC-3, AC-17
- [x] Tick "Trống" và "Đã bàn giao" → 5 căn; URL có `trangThaiSuDung` lặp hai lần → AC-2, AC-17
- [x] Ô mã căn gõ `03` → A-0103, A-0203, A-0301 đến A-0304; gõ `%` → "Không có căn hộ nào khớp bộ lọc" → AC-2, AC-17
- [x] Sửa tay URL thành `?q=ABCDEFGHIJKLMNOPQRSTU` → lỗi "Từ khóa tìm kiếm tối đa 20 ký tự" dưới ô mã căn; `?maToaNha=ZZ` → "Không tìm thấy tòa nhà" kèm nút Xóa bộ lọc → AC-2, AC-3, AC-17
- [x] Bấm tiêu đề cột Trạng thái → thứ tự Trống, Đang sử dụng, Đang sửa chữa, Đã bàn giao; tiêu đề có `aria-sort`; diện tích hiện dạng `75,50 m²` → AC-4, AC-17
- [x] BQL bấm "Thêm căn hộ", chọn Tòa A, Tầng 1 (A-01), số căn `5` → hiện "Mã căn sẽ tạo: A-0105"; diện tích `70,5`, Thêm → toast "Đã thêm căn hộ A-0105", bảng có căn mới, trang `/toa-nha` báo tòa A 13 căn → AC-7, AC-18
- [x] Thêm lại căn số 5 tầng A-01 → lỗi "Tầng này đã có căn số 5 (A-0105)" dưới ô Số căn → AC-9, AC-18
- [x] Thêm với diện tích `75,505` → lỗi diện tích dưới ô; số căn `abc` → lỗi số căn → AC-8, AC-18
- [x] Bấm A-0105 → `/can-ho/A-0105` hiện mã, "Tòa A (A)", tầng, `70,50 m²`, "Trống", "Đang dùng bình thường" → AC-5, AC-19
- [x] Bấm "Sửa": hai ô điền sẵn `70,50` và "Trống"; đổi `72`, "Đang sử dụng", Lưu → toast "Đã cập nhật căn hộ A-0105", chi tiết cập nhật → AC-11, AC-19
- [x] Mở `/nhat-ky` → hai dòng "Thêm căn hộ" và "Sửa căn hộ" cho `CanHo:A-0105`; dòng sửa có `dienTich` `{cu: 70.50, moi: 72.00}` → AC-14
- [x] Sửa lại đúng giá trị hiện có (`72`, "Đang sử dụng") → 200, không thêm dòng nhật ký → AC-14
- [x] Kế toán vào `/can-ho` → thấy bảng, không có nút "Thêm căn hộ"; mở chi tiết → không có nút "Sửa" → AC-15, AC-18, AC-19
- [x] Kỹ thuật → menu không có "Căn hộ"; gõ `/can-ho` → bị chặn trang → AC-15
- [x] Cư dân 0900000007 vào `/can-ho-cua-toi` → hiện luôn chi tiết A-0201, không có nút Sửa; gõ `/can-ho-cua-toi/A-0101` → "Không tìm thấy căn hộ" kèm link về `/can-ho-cua-toi` → AC-6, AC-19, AC-20
- [x] Toàn bộ thanh lọc, hai dialog, bảng và thẻ dùng được chỉ bằng bàn phím, focus thấy rõ, lỗi đọc được qua `aria-describedby` → AC-21

## Commands

- [x] `curl` (cookie BQL) `GET /api/v1/apartments?maToaNha=A&dienTichTu=1e2` → 400 `VALIDATION_FAILED` mục `dienTichTu` → AC-3
- [x] Cư dân gọi `GET /api/v1/apartments` → 403; Kế toán gọi `POST` hoặc `PUT` → 403; chưa đăng nhập → 401 → AC-15
- [x] Chèn tay `INSERT INTO can_ho (ma_can_ho, ma_tang, dien_tich) VALUES ('A-0100', 'A-01', 50)` hoặc diện tích 1500 → CHECK từ chối → AC-10
- [x] `cd backend && mvnw.cmd verify` → xanh (gồm `TimCanHoIntegrationTest`, `ThemSuaCanHoIntegrationTest`, test chạy song song tạo và sửa) → AC-9, AC-13
- [x] `cd frontend && pnpm test && pnpm typecheck && pnpm lint` → xanh → AC-17 đến AC-21

## Value sourcing

- [x] Mã căn tạo ra luôn là `<maTang><số căn hai chữ số>` dù gửi `maTang` chữ thường (`a-01`) → Value sourcing "ma_can_ho"
- [x] Bỏ `trangThaiSuDung` khi tạo → căn mới `Trống`, `biTamDungTienIch` false → Value sourcing "trang_thai_su_dung", "bi_tam_dung_tien_ich"
- [x] Tầng của tòa ngoài phạm vi (hoặc mã tầng không có) → "Tầng không tồn tại" → Value sourcing "tầng thuộc phạm vi"
- [x] `soTang`, `maToaNha`, `tenToaNha` trong response khớp bảng `tang`, `toa_nha` → Value sourcing "Mọi response căn"
- [x] `totalItems` đổi đúng khi thêm bộ lọc → Value sourcing "totalItems, totalPages"
- [x] Cư dân có hồ sơ hết hạn (ngày kết thúc trước hôm nay theo giờ Việt Nam) xem căn cũ → 404 → Value sourcing "quyền cư dân"
- [x] Hai BQL sửa song song → `moi` của dòng nhật ký trước bằng `cu` của dòng sau → Value sourcing "giá trị cũ cho nhật ký"
- [x] Sửa `75.5` trên cột `75.50` không tính là đổi → Value sourcing "có đổi"
- [x] Người thực hiện của nhật ký là mã người dùng BQL đăng nhập; thời điểm theo `Clock` → Value sourcing "Nhật ký"
- [x] Ô tầng ở thanh lọc có đúng N lựa chọn với N là `soTang` của tòa → Value sourcing "UI lọc tầng"
- [x] Danh sách tầng trong dialog thêm căn lấy từ `/buildings/{ma}/floors` → Value sourcing "UI dialog thêm căn"
- [x] Nút Thêm và Sửa theo `useHoSo().data.vaiTro`; trang căn hộ của tôi theo `phamVi.maCanHo` → Value sourcing "UI nút theo vai trò", "UI căn của tôi"

## Acceptance-criteria coverage

- AC-1, AC-2, AC-4, AC-16: các bước lọc và sắp xếp ở UI · AC-3: bước diện tích sai, URL sửa tay, `curl` · AC-5, AC-6: chi tiết nhân viên và cư dân · AC-7 đến AC-9: dialog thêm căn · AC-10: chèn tay · AC-11 đến AC-13: dialog Sửa, test song song · AC-14: nhật ký · AC-15: Kế toán, Kỹ thuật, Cư dân · AC-17 đến AC-20: các bước giao diện · AC-21: bàn phím
