# Verify: Đề nghị cư trú & tra cứu hồ sơ · spec 0015 · updated 2026-11-26
_Steps derived from spec 0015 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

## Backend (API và database)
- [x] Cư dân `0900000006` gọi `POST /api/v1/residency-records/resident-request` cho `A-0101` với `laChinhToi=false` và cặp CCCD, SĐT của `0900000007` (SĐT dạng `+84 900.000.007`) → 201, `trangThaiHoSo='Chờ xác minh'`, `coTheHuy=true`, `soCccd`, `soDienThoai`, `email`, `maNguoiDung`, `maNvbqlXacMinh` đều `null`; `trang_thai_su_dung` của `A-0101` không đổi → AC-1
- [x] Cùng body với `laChinhToi` là chuỗi, `vaiTroCuTru='Chủ căn hộ'`, `ngayBatDau` hôm qua, `ngayKetThuc` trước `ngayBatDau` → 400, các mục `errors` đúng thứ tự trường và đúng câu của AC-2 → AC-2
- [x] CCCD đúng nhưng SĐT sai; CCCD không tồn tại; cặp của tài khoản `Ngừng hoạt động` → cả ba đều 422 `CUTRU_KHONG_KHOP_CU_DAN` cùng một câu → AC-3
- [x] Gửi lại đề nghị cho cùng người ở cùng căn khi đề nghị cũ còn chờ → 409 `CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH`; căn đã có 10 đề nghị chờ → 422 `CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH`; giao ngày với hồ sơ `Hợp lệ` của cùng người → 409 `CUTRU_TRUNG_KHOANG_NGAY_CU_DAN`; căn mình không ở → 404 → AC-3
- [x] BQL `POST .../{id}/approve` đề nghị bắt đầu hôm nay ở căn đang `Trống` → 200 `Hợp lệ`, có `maNvbqlXacMinh`, căn sang `Đang sử dụng`; duyệt lại → 409 `CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH` → AC-4
- [x] Đổi đồng hồ (hoặc sửa DB) để `ngay_ket_thuc < hôm nay` rồi duyệt → 422 `CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC`; `ngay_bat_dau < hôm nay <= ngay_ket_thuc` vẫn duyệt 200; tài khoản người được đề nghị `Ngừng hoạt động` → 422 `CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG` → AC-4
- [x] BQL `POST .../{id}/reject` thiếu `lyDoTuChoi`, sai kiểu, dài 501 ký tự → 400 đúng câu; có lý do → 200 `Từ chối`, `lyDoTuChoi` đã cắt khoảng trắng, căn không đổi → AC-5
- [x] Người gửi `POST .../{id}/cancel` → 200 `Đã hủy`, `coTheHuy=false`, `maNvbqlXacMinh=null`; sau đó gửi lại đề nghị cho cùng người → 201; cư dân khác cùng căn hủy đề nghị đó → 404 → AC-6, AC-11
- [x] BQL duyệt và người gửi hủy cùng lúc trên một đề nghị → đúng một 200 và một 409 → AC-4, AC-6
- [x] `PUT /residency-records/{id}` trên dòng `Từ chối` hoặc `Đã hủy` → 409 `CUTRU_DE_NGHI_DA_KET_THUC`; trên dòng `Chờ xác minh` vẫn 409 `CUTRU_HO_SO_DANG_CHO_XAC_MINH`; sửa dòng `Hợp lệ` từng là đề nghị → giữ `maCuDanDeNghi`, `ghiChuDeNghi`; job hết hạn không đóng dòng `Chờ xác minh` → AC-7
- [x] `GET /apartments/A-0101/residents` bằng BQL → có `deNghiCuTru` đủ trường, `coTheHuy=false`; bằng người gửi → chỉ đề nghị của mình, không PII, `coTheHuy=true` với dòng chờ; bằng cư dân khác cùng căn → không thấy đề nghị của người khác, ba mảng nhân khẩu có `maCuDanDeNghi=null` → AC-8
- [x] `GET /residency-records` mặc định `taoLuc,desc`, `size=20`; `sort=ngayKetThuc,asc` để ngày trống cuối, `desc` để ngày trống đầu; `trangThaiHoSo` lặp lại được; `q` khớp họ tên không dấu, SĐT, CCCD, mã căn, mã hồ sơ; `%` không khớp tất cả → AC-9, AC-10
- [x] `GET /residency-records` với `page=0&size=101&sort=hoTen,asc&soTang=0&trangThaiHoSo=Mới&vaiTroCuTru=X&tinhTrangCuTru=X&q=<101 ký tự>` → 400 đúng thứ tự mục; `soTang` thiếu `maToaNha` → 400; `maToaNha=ZZ` → 404 → AC-10
- [x] Trong DB: `ma_nvbql_xac_minh` cho phép NULL; có `ma_cu_dan_de_nghi`, `ghi_chu_de_nghi`, `ly_do_tu_choi`, `fk_hsct_cudan_denghi`, ba CHECK mới, `uq_hsct_cho_xac_minh_canho_cudan`, hai index mới; chèn `Từ chối` không lý do hoặc `Chờ xác minh` có người xác minh bị CHECK chặn → AC-11
- [x] `CU_DAN` gọi `GET /residency-records`, `approve`, `reject` → 403; BQL gọi `resident-request`, `cancel` → 403; `KE_TOAN`, `BAN_QUAN_TRI` gọi `GET /residency-records` → 403; chưa đăng nhập → 401 → AC-12
- [x] Mỗi thao tác gửi, duyệt, từ chối, hủy ghi đúng một dòng nhật ký `CUTRU_GUI_DE_NGHI`, `CUTRU_DUYET_DE_NGHI`, `CUTRU_TU_CHOI_DE_NGHI`, `CUTRU_HUY_DE_NGHI` trên `HoSoCuTru:<mã>`; chi tiết không có họ tên, SĐT, CCCD hay nội dung ghi chú (chỉ `coGhiChu`) → AC-13
- [x] Seed: `A-0101` có một đề nghị `Chờ xác minh` cho `0900000010` và một `Từ chối` cho `0900000009`, cả hai do `0900000006` đứng đơn; chạy lại seed không sinh thêm dòng → AC-14

## Value sourcing
- [x] `ma_cu_dan_de_nghi` lấy từ dòng `cu_dan` của người gọi, không từ body: gửi kèm trường lạ `maCuDanDeNghi` trong body → bị bỏ qua
- [x] `ma_cu_dan` khi `laChinhToi=true` là chính người gọi; khi `false` là người khớp cả CCCD và SĐT sau chuẩn hóa (thử SĐT có `+84`, dấu chấm, gạch nối)
- [x] `ma_nvbql_xac_minh` khi duyệt, từ chối là BQL đang gọi
- [x] `hoTenNguoiDeNghi`, `hoTenNguoiXacMinh` đọc theo lô; trong danh sách 20 dòng chỉ có một câu SQL cho cư dân và một câu cho BQL (xem log SQL)
- [x] `coTheHuy` chỉ `true` khi dòng `Chờ xác minh` và người xem là người gửi; BQL luôn `false`
- [x] Tập căn của `GET /residency-records` theo tòa trong phạm vi và tầng; BQL không có tòa nào → trang rỗng
- [x] `homNay` theo `Asia/Ho_Chi_Minh`: đặt đồng hồ 2026-11-15T17:30Z (đã là 16/11 giờ Việt Nam), gửi `ngayBatDau=2026-11-16` → 201; `2026-11-15` → 400
- [x] (Frontend) Số đếm `Chờ xác minh` lấy từ `totalItems` của `size=1`; nút `Sửa`, `Hủy hồ sơ`, `Kết thúc cư trú` so ngày theo giờ Việt Nam; combobox căn hộ gọi đúng `GET /apartments?maToaNha&q&size=50&sort=maCanHo,asc`

## Commands
- [x] `cd backend && mvnw.cmd verify` → BUILD SUCCESS, 748 test xanh, `docs/api/openapi.json` có 5 endpoint mới → AC-12, AC-14
- [x] `cd frontend && pnpm gen:api && git diff --exit-code src/lib/api/schema.d.ts` → `schema.d.ts` khớp hoàn toàn với `docs/api/openapi.json` → AC-14, AC-15, AC-16
- [x] `cd frontend && pnpm typecheck && pnpm lint && pnpm format:check && pnpm test && pnpm build` → toàn bộ test Vitest, ESLint, Prettier, kiểu TypeScript và bản build NextJS xanh → AC-15, AC-16, AC-17, AC-18

## UI / manual (sau khi làm frontend)
- [x] `/can-ho-cua-toi` → tab `Đề nghị cư trú`, gửi đề nghị (ẩn hiện CCCD và SĐT theo `Chính tôi` / `Người khác`), hủy đề nghị → AC-15
- [x] Đăng nhập cư dân `0900000006`, mở `/can-ho-cua-toi` → thấy nhãn tab `Đề nghị cư trú (1)` và nút `Gửi đề nghị cư trú`; mở hộp thoại mặc định chọn `Người khác` (hiện và bắt buộc `Số CCCD`, `Số điện thoại`), đổi sang `Chính tôi` thì ẩn hai ô này, `Ngày bắt đầu` điền sẵn hôm nay theo giờ `Asia/Ho_Chi_Minh`, chọn `Tạm trú` bắt buộc `Ngày kết thúc`; gửi thành công tự chuyển sang tab `Đề nghị cư trú`, không lộ PII hay người xác minh, dòng `Từ chối` hiện lý do từ chối và chỉ dòng `coTheHuy = true` có nút `Hủy đề nghị` → AC-15, AC-18
- [x] Đăng nhập cư dân khác cùng căn `A-0101` (sau khi có hồ sơ hợp lệ) mở `/can-ho-cua-toi` → không thấy đề nghị của cư dân `0900000006` trong tab `Đề nghị cư trú` và không thấy người đứng đơn ở tab `Hiện tại` hay `Lịch sử cư trú` → AC-8, AC-15
- [x] Đăng nhập BQL `0900000002`, mở `/can-ho/A-0101` chuyển sang tab `Đề nghị cư trú` → thấy đủ PII, người xác minh, nút `Duyệt` (mở hộp thoại xác nhận tóm tắt thông tin đề nghị) và nút `Từ chối` (mở hộp thoại bắt buộc nhập `Lý do từ chối`) trên dòng `Chờ xác minh` → AC-15, AC-18
- [x] `/ho-so-cu-tru` bằng BQL → thanh lọc nhanh kèm số đếm, lọc đồng bộ URL, bảng sắp xếp có `aria-sort`, duyệt và từ chối ngay trên dòng, thêm hồ sơ có chọn căn → AC-16, AC-17
- [x] Mở `/ho-so-cu-tru` bằng BQL → menu `Hồ sơ cư trú` hiển thị và sáng; thanh lọc nhanh `Tất cả hồ sơ` sáng khi chưa lọc trạng thái, bấm `Chờ xác minh (<số đếm>)` chỉ ghi `trangThaiHoSo=Chờ xác minh` và đưa `page` về 1 mà giữ nguyên các bộ lọc khác trên URL (ẩn số đếm khi bằng 0); vào trang không có `sort` thì cột `Tạo lúc` mang `aria-sort="descending"`, bấm lại đảo sang `ascending`; sắp xếp hoạt động trên đủ 5 trường `taoLuc`, `capNhatLuc`, `ngayBatDau`, `ngayKetThuc`, `maCanHo` trên bảng 9 cột; đổi tòa tự xóa tầng, lọc tòa ngoài phạm vi hiện `ErrorState` có nút `Xóa bộ lọc` → AC-16
- [x] Trên `/ho-so-cu-tru`, kiểm tra hành động theo dòng dựa trên ngày hôm nay giờ `Asia/Ho_Chi_Minh`: dòng `Chờ xác minh` hiện `Duyệt` và `Từ chối`; dòng `Hợp lệ` chưa quá hạn kết thúc hiện `Sửa`; dòng `Hợp lệ` có `ngayBatDau > homNay` hiện `Hủy hồ sơ`, còn lại hiện `Kết thúc cư trú`; dòng `Hết hiệu lực`, `Từ chối`, `Đã hủy` không có nút thao tác ghi; bấm `Thêm hồ sơ cư trú` ở đầu trang mở hộp thoại chọn tòa rồi tìm căn hộ qua combobox (đổi tòa tự xóa căn đang chọn), lưu thành công làm mới đồng bộ bảng và số đếm chờ xác minh → AC-17
- [x] Đăng nhập `CU_DAN`, `KE_TOAN`, `BAN_QUAN_TRI` truy cập thẳng `/ho-so-cu-tru` → không có mục menu và bị chặn bằng màn hình không có quyền truy cập → AC-12, AC-16
- [x] Toàn bộ các màn hình và hộp thoại trên dùng được bằng bàn phím, focus rõ, lỗi gắn với ô nhập → AC-18
- [x] Điều hướng bằng bàn phím (`Tab`, `Shift+Tab`, phím mũi tên trên `Tabs` và `Radio`, `Enter`, `Escape`) trên `/ho-so-cu-tru`, tab `Đề nghị cư trú` và bốn hộp thoại gửi, hủy, duyệt, từ chối → mọi phần tử nhận focus rõ ràng, lỗi 400 gắn qua `aria-describedby`, lỗi 409/422 hiện trong `FormAlert` và đóng hộp thoại trả focus về đúng nút mở → AC-18

## Acceptance-criteria coverage
- AC-1 đến AC-3: `DeNghiCuTruGuiIntegrationTest`, `DeNghiCuTruLogicTest` · AC-4 đến AC-7: `DeNghiCuTruVongDoiIntegrationTest` · AC-8: cả hai test tích hợp đề nghị và `DeNghiCuTruLogicTest` · AC-9, AC-10, AC-14: `DanhSachHoSoCuTruIntegrationTest`, `SchemaMigrationTest` · AC-11: unique index qua `DeNghiCuTruLogicTest`, gửi lại sau hủy · AC-12: `PhanQuyenCoverageTest` và các ca 403/401 · AC-13: assert nhật ký trong các test tích hợp, `ChiTietNhatKyAnToanTest`, `NhatKyTraCuuIntegrationTest` · AC-15, AC-18: các bước UI trên `/can-ho-cua-toi`, `/can-ho/[maCanHo]` và `frontend/src/app/(app)/can-ho/ho-so-cu-tru.test.tsx` · AC-16, AC-17, AC-18: các bước UI trên `/ho-so-cu-tru`, `frontend/src/app/(app)/ho-so-cu-tru/ho-so-cu-tru.test.tsx`, `cu-tru.test.ts`, `error-messages.test.ts`, `menu.test.ts`
