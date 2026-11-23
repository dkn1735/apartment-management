# Verify: Quản lý tài khoản (Admin) · spec 0008 · updated 2026-11-22

_Steps derived from spec 0008 acceptance criteria and the Value sourcing table. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: `docker compose up -d`, backend với profile `dev,seed`, `pnpm dev`. Mật khẩu seed `ChungCu@2026`. Admin `0900000001`, BQL `0900000002`, kỹ thuật `0900000003`.

## UI / manual

- [x] Đăng nhập Admin, mở menu **Tài khoản** → thấy `/tai-khoan`, bảng có 8 tài khoản seed, mới tạo trước, cột Vai trò là nhãn tiếng Việt → AC-5, AC-17
- [x] Gõ `dang ban` vào ô tìm (không dấu) → sau khoảng 300 ms chỉ còn "Đặng Ban Quản Trị", URL có `q=dang+ban`, focus vẫn ở ô tìm và gõ tiếp được → AC-5, AC-17
- [x] Gõ `+84 900 000 007` → ra đúng "Vũ Cư Dân" (số điện thoại được chuẩn hóa trước khi so) → AC-5
- [x] Gõ `%` → không ra mọi tài khoản (ký tự đại diện được thoát) → AC-5
- [x] Lọc Vai trò "Kỹ thuật" + Trạng thái "Hoạt động" → chỉ còn tài khoản khớp cả hai; "Xóa bộ lọc" làm trống ô tìm → AC-5, AC-17
- [x] Sửa tay URL `?vaiTro=ADMIN` → khung báo bộ lọc không hợp lệ, câu "Vai trò không tồn tại" → AC-5
- [x] **Tạo tài khoản**: nhập họ tên có khoảng trắng thừa, số `0987 654 321`, email chữ hoa, tick Kỹ thuật → ô "Chuyên môn kỹ thuật" hiện ra; bấm tạo trống thì lỗi hiện dưới từng ô và ô đầu tiên nhận focus → AC-1, AC-4, AC-17
- [x] Tạo thành công → hộp thoại "Mật khẩu tạm của …" hiện 12 ký tự không có `0 O 1 l I`, nút Sao chép báo "Đã sao chép", bấm ra ngoài không đóng; đóng bằng "Tôi đã lưu mật khẩu" thì sang trang chi tiết, số điện thoại đã thành `0987654321`, email chữ thường, badge "Chờ đổi mật khẩu" → AC-1, AC-2, AC-17
- [x] Tạo lại với cùng số điện thoại và email → lỗi "Số điện thoại đã được dùng cho tài khoản khác" và "Email đã được dùng…" hiện dưới đúng ô → AC-3
- [x] Tick "Quản trị viên" → hỏi "Cấp quyền quản trị viên?" trước khi chọn; Hủy thì ô không được tick → AC-17
- [x] Ở cửa sổ riêng, đăng nhập bằng tài khoản mới và mật khẩu tạm, kèm `?next=/nhat-ky` → vào `/doi-mat-khau?next=%2Fnhat-ky`, có thông báo "Bạn đang dùng mật khẩu tạm…", menu điều hướng ẩn, menu người dùng (Đăng xuất) còn → AC-13, AC-14
- [x] Vẫn ở cửa sổ đó, gõ `/ho-so` → bị đưa về `/doi-mat-khau?next=%2Fho-so` → AC-14
- [x] Đổi mật khẩu (mật khẩu tạm làm mật khẩu cũ) → sang thẳng trang `next`, gọi API thường được ngay, không phải đăng nhập lại → AC-13, AC-14
- [x] Trang chi tiết: **Sửa** thông tin chung, đổi họ tên → lưu, phiên bản tăng; mở cùng trang ở tab khác, sửa ở tab A rồi lưu ở tab B → toast "Tài khoản vừa được thay đổi ở nơi khác, đã tải lại dữ liệu mới nhất." và dữ liệu mới hiện ra → AC-7, AC-17
- [x] **Thêm vai trò** Bảo vệ, rồi **Sửa** vị trí trực, rồi **Gỡ** qua hộp thoại xác nhận → mỗi bước có toast, vai trò còn một thì không còn nút Gỡ → AC-8, AC-9, AC-17
- [x] Trang chi tiết của `0900000006`: gỡ Cư dân → lỗi "Vai trò này đang gắn với dữ liệu khác…" (hồ sơ cư trú trỏ vào), vai trò còn nguyên → AC-9
- [x] Trang chi tiết của chính Admin → không có nút khóa, đặt lại, gỡ quyền quản trị; có dòng "Đây là tài khoản của bạn…" → AC-11, AC-12, AC-17
- [x] **Tạm khóa** chỉ bằng bàn phím: Enter mở hộp thoại, gửi trống thì focus vào ô Lý do có lỗi; nhập lý do rồi gửi → badge "Tạm khóa", focus nằm ở nút "Mở lại" (không rơi về đầu trang) → AC-10, AC-17
- [x] Người bị khóa đang mở app ở cửa sổ khác → bị đưa về đăng nhập với thông báo khóa khi chuyển trang hoặc khi phiên làm mới (tối đa 15 phút); đăng nhập lại bị từ chối → AC-10
- [x] **Mở lại** (lý do không bắt buộc) → đăng nhập lại được → AC-10
- [x] **Đặt lại mật khẩu** → hộp thoại xác nhận rồi hộp thoại mật khẩu tạm; mật khẩu cũ không đăng nhập được, mật khẩu tạm đăng nhập được và bị bắt đổi → AC-12, AC-13
- [x] Đăng nhập BQL `0900000002` → `/tai-khoan` chỉ có tài khoản cư dân thuần (không có `0900000006`), không có ô lọc Vai trò; form tạo chỉ có ô "Cư dân" đã chọn sẵn → AC-15, AC-17
- [x] BQL mở thẳng `/tai-khoan/ND00000006` → "Không tìm thấy tài khoản" → AC-6, AC-15
- [x] BQL chuyển một cư dân sang Ngừng hoạt động → trang chi tiết không còn nút Mở lại hay Tạm khóa, vẫn có Đặt lại mật khẩu → AC-15
- [x] Đăng nhập kỹ thuật `0900000003`, mở `/tai-khoan` → trang Không có quyền, menu không có mục Tài khoản → AC-15
- [x] `/nhat-ky` bằng Admin → bộ lọc Loại thao tác có tám loại mới; các dòng của những thao tác trên có chi tiết chỉ là tên trường, mã vai trò, trạng thái, lý do, số phiên, không có số điện thoại, email, CCCD hay mật khẩu → AC-16

## Commands

- [x] `cd backend && mvnw.cmd verify` → BUILD SUCCESS, gồm `TaiKhoanTaoIntegrationTest`, `BatDoiMatKhauIntegrationTest`, `TaiKhoanSuaIntegrationTest`, `TaiKhoanTrangThaiIntegrationTest`, `TaiKhoanTimKiemIntegrationTest`, `MatKhauTamGeneratorTest`, `SchemaMigrationTest` → AC-1 đến AC-16, AC-18
- [x] `cd frontend && pnpm test && pnpm typecheck && pnpm lint` → xanh → AC-14, AC-17
- [x] Trên DB dev: `SELECT version, success FROM flyway_schema_history WHERE version = '12'` → `12 | t`; cột `phai_doi_mat_khau` kiểu boolean; `SELECT extname FROM pg_extension WHERE extname = 'unaccent'` có một dòng; mọi tài khoản seed có `phai_doi_mat_khau = false` → AC-18
- [x] Tạo tài khoản bằng curl có header `X-Requested-With: XMLHttpRequest` → response 201 có header `Cache-Control: no-store`; `POST .../reset-password` cũng vậy → AC-1, AC-12

## Value sourcing (mỗi giá trị kiểm từ đúng nguồn của nó)

- [x] `maNguoiDung`, mã bảng con: tạo hai tài khoản liền nhau → mã tăng dần theo sequence (`ND…`, `KTV…`), không do client gửi → AC-1
- [x] `matKhauTam`: tạo 3 lần → 3 chuỗi khác nhau, đúng bảng chữ; DB chỉ có `{bcrypt}…` → AC-2
- [x] `tao_luc`, `cap_nhat_luc`: lấy từ bean `Clock` (test chỉnh `MutableClock` thì giá trị trả về đổi theo), không từ `now()` của DB → AC-1
- [x] Người quản lý và vai trò: gọi API bằng token BQL rồi token Admin trên cùng một tài khoản nhân viên → BQL 404, Admin 200 (vai trò đọc từ token) → AC-15
- [x] Vai trò tài khoản đích đọc từ DB: Admin thêm `KE_TOAN` cho một cư dân thuần → BQL không còn thấy tài khoản đó ngay lập tức (không đợi token) → AC-15
- [x] "Cư dân thuần": tài khoản có `CU_DAN` + `BAN_QUAN_TRI` không có trong danh sách của BQL → AC-15
- [x] `vaiTro` ở danh sách: trang 20 dòng chỉ chạy 3 truy vấn (đếm, trang, vai trò theo lô), xem log SQL → AC-5
- [x] `dangNhapCuoiLuc`: đăng nhập một tài khoản rồi tải lại danh sách → cột Đăng nhập gần nhất đổi theo giờ Việt Nam → AC-5
- [x] `phienBan`: gửi `phienBan` cũ → 409; gửi đúng → tăng đúng 1; đổi trạng thái hay đặt lại cũng tăng 1 → AC-7, AC-8, AC-10, AC-12
- [x] `soPhienThuHoi`: đăng nhập 2 máy rồi khóa → chi tiết nhật ký `soPhienThuHoi: 2` → AC-10, AC-12
- [x] `doi_mat_khau_luc`: đặt lại mật khẩu → cột có giá trị bằng giờ của `Clock` → AC-12
- [x] Claim `phaiDoiMatKhau`: đặt `phai_doi_mat_khau = true` bằng SQL rồi làm mới phiên → token mới bị chặn (claim đọc lại từ DB lúc cấp) → AC-13
- [x] Số Admin đang hoạt động: chỉ còn hai Admin, hai người gỡ quyền nhau cùng lúc → một bên 422 `AUTH_LAST_ADMIN`, còn đúng một Admin → AC-11
- [x] Trường trùng khi chạy đua: hai request tạo cùng số điện thoại cùng lúc → một 201, một 409 `AUTH_IDENTITY_TAKEN` trường `soDienThoai` (map từ tên ràng buộc) → AC-3
- [x] Gỡ vai trò bị tham chiếu: SQLSTATE `23001` của `ON DELETE RESTRICT` → 409 `AUTH_ROLE_IN_USE`, không 500 → AC-9

## Acceptance-criteria coverage

- AC-1 tạo tài khoản và chuẩn hóa · AC-2 mật khẩu tạm · AC-3 trùng thông tin · AC-4 lỗi theo trường: UI bước tạo, Value sourcing mã và mật khẩu, `TaiKhoanTaoIntegrationTest`
- AC-5 danh sách và tìm kiếm: UI bước tìm và lọc, `TaiKhoanTimKiemIntegrationTest`
- AC-6 chi tiết và 404: UI BQL mở `ND00000006`, `TaiKhoanSuaIntegrationTest`
- AC-7, AC-8, AC-9 sửa và vai trò: UI các bước Sửa, Thêm, Gỡ, gỡ bị tham chiếu
- AC-10, AC-11, AC-12 trạng thái, tự bảo vệ, Admin cuối, đặt lại: UI khóa, mở lại, đặt lại; `TaiKhoanTrangThaiIntegrationTest` (gồm chạy đua)
- AC-13, AC-14 bắt đổi mật khẩu: UI cửa sổ riêng, `BatDoiMatKhauIntegrationTest`, test `layout`, `login-form`, `doi-mat-khau-form`
- AC-15 phạm vi BQL: UI bằng BQL và kỹ thuật, Value sourcing vai trò
- AC-16 nhật ký: UI `/nhat-ky`, test quét bí mật trong `TaiKhoanTimKiemIntegrationTest`
- AC-17 giao diện và a11y: UI toàn bộ, axe trong test Vitest
- AC-18 migration: Commands truy vấn DB, `SchemaMigrationTest`
