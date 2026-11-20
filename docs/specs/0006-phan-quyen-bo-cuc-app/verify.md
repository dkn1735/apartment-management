# Verify: Phân quyền theo vai trò và bố cục app · spec 0006 · updated 2026-11-20
_Steps derived from spec 0006 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chạy backend với profile `dev,seed` và `pnpm dev`. Mật khẩu mọi tài khoản seed: `ChungCu@2026`.

## UI / manual
- [ ] Đăng nhập `0900000002` (BQL) → về `/` (không phải `/ho-so`), thấy "Xin chào, Trần Ban Quản Lý" và thẻ "Tòa nhà" trong nhóm "Ban quản lý" → AC-14
- [ ] Menu của `0900000002`: nhóm đầu "Tổng quan", "Hồ sơ", rồi nhóm "Ban quản lý" có "Tòa nhà" → AC-10
- [ ] `0900000001` (Admin) và `0900000007` (cư dân): menu chỉ có nhóm đầu; Tổng quan hiện "Các chức năng cho vai trò của bạn đang được xây dựng" có link "Xem hồ sơ" → AC-10, AC-14
- [ ] `0900000006` (BQT + cư dân): nhóm "Ban quản trị" có "Tòa nhà", không có nhóm "Cư dân"; không href nào lặp → AC-10
- [ ] Mở `/toa-nha` bằng `0900000002`: bảng có cột Mã, Tên, Số tầng, Địa chỉ khu vực, Số căn hộ; tòa A có 3 tầng, 3 căn hộ; Network: request `GET /api/v1/buildings` không có `sort` → AC-7, AC-15
- [ ] Bấm tiêu đề "Tên tòa nhà": URL có `sort=tenToaNha,asc` và không có `page`; đổi số dòng mỗi trang: URL có `size` → AC-15
- [ ] `0900000007` gõ `/toa-nha`: hiện "Không có quyền truy cập" mức h1, nút "Về trang chủ"; Network không có `GET /api/v1/buildings`; menu và thanh trên giữ nguyên → AC-11
- [ ] `0900000007` gõ `/toa-nha/abc` và `/doi-soat` → cũng hiện "Không có quyền truy cập" → AC-11
- [ ] Bật throttle mạng, tải lại `/toa-nha`: menu hiện skeleton, trang hiện skeleton, bảng chưa render → AC-13
- [ ] Chặn `GET /api/v1/auth/profile` (trả 500) rồi mở `/toa-nha`: menu chỉ có nhóm đầu, trang hiện "Không tải được quyền truy cập" có "Thử lại"; `/` và `/ho-so` vẫn hiện khung của chúng → AC-13
- [ ] Token lệch vai trò: ký token chỉ có `CU_DAN` cho tài khoản BQL (hoặc đổi vai trò trong DB sau khi đăng nhập), mở `/toa-nha`: trang render, bảng hiện "Không có quyền truy cập" không có "Thử lại", không mã tra cứu, không chuyển trang → AC-12
- [ ] Chỉ dùng bàn phím trên `/`, `/toa-nha` và trạng thái "Không có quyền truy cập": Tab tới mọi link, nút và vùng bảng, focus luôn nhìn thấy được; trình đọc màn hình đọc tên nhóm menu và tên thẻ lối tắt là tên mục → AC-16

## Commands
- [ ] `cd backend && mvnw.cmd verify` → xanh, gồm `PhanQuyenCoverageTest`, `PhanQuyenCoverageMauTest`, `ToaNhaIntegrationTest`, `PhamViTruyCapServiceTest`, `TrangYeuCauTest`, `PhanQuyenOpenApiTest` → AC-1 đến AC-9
- [ ] Thêm tạm một method `@GetMapping` không có annotation phân quyền vào một controller, chạy `PhanQuyenCoverageTest` → đỏ, nêu tên lớp và method; bỏ thay đổi → AC-1
- [ ] Đăng nhập `0900000007` rồi `GET /api/v1/buildings?page=abc&size=0` → 403 `FORBIDDEN`, không phải 400; log backend có một dòng WARN `Từ chối quyền ... path=/api/v1/buildings canCo=BAN_QUAN_LY,BAN_QUAN_TRI,KE_TOAN` kèm requestId, không có query string → AC-2, AC-4
- [ ] Không cookie `GET /api/v1/buildings` → 401 `UNAUTHENTICATED` → AC-2
- [ ] `0900000003`, `0900000005`, `0900000001` gọi `/api/v1/buildings` → 403; `0900000002`, `0900000004`, `0900000006` → 200 → AC-3, AC-8
- [ ] BQL gọi `sort=diaChiKhuVuc,asc`, `sort=tenToaNha,ASC`, `sort=a&sort=b`, `size=101`, `page=0` → 400 `VALIDATION_FAILED` đúng `field`; `page=0&size=0&sort=x,y` → ba mục theo thứ tự `page`, `size`, `sort`; `page=5&size=1` → 200, `items` rỗng, `totalItems` 1 → AC-7, AC-9
- [ ] Mở `/swagger-ui.html`: `GET /api/v1/buildings` có dòng "Vai trò được phép: BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN" và response 403; `/auth/profile` có "mọi người đã đăng nhập"; `/auth/login` có "Công khai, không cần đăng nhập" → AC-5
- [ ] `GET /api/v1/auth/profile` bằng `0900000001` → `phamVi.maToaNha` rỗng (lệch có chủ ý spec 0004 AC-8) → AC-6
- [ ] `cd frontend && pnpm typecheck && pnpm lint && pnpm vitest run && pnpm build` → xanh, gồm test axe của `/`, `/toa-nha`, `KhongCoQuyen` → AC-10 đến AC-16

## Value sourcing
- [ ] Vai trò người gọi lấy từ token, không từ DB: token chỉ `CU_DAN` của tài khoản BQL nhận 403 ở `/buildings` (test `vaiTroLayTuTokenKhongDocDb`)
- [ ] Vai trò được phép lấy từ `@ChoPhep.value()`: dòng log `canCo=` và mô tả OpenAPI khớp annotation
- [ ] Log từ chối: `maNguoiDung` là claim `sub`, `path` là `getRequestURI()` không query string, có requestId của header `X-Request-Id`
- [ ] Phạm vi `maToaNha`: thêm tòa B vào DB, BQL thấy 2 tòa ở `/buildings`; Admin vẫn rỗng
- [ ] Phạm vi `maCanHo`: hồ sơ cư trú hết hạn hôm qua theo giờ Việt Nam không tính (`PhamViTruyCapServiceTest`, ngày 16/11 VN khi UTC còn 15/11)
- [ ] `soCanHo`: thêm một căn hộ vào tầng A-01 → số căn hộ của tòa A tăng lên 4; tòa không có tầng → 0
- [ ] `totalPages` = trần(`totalItems` / `size`): `size=1` với 1 tòa → 1 trang
- [ ] Menu và chặn trang đọc `vaiTro` từ `/auth/profile` (DB), nên token lệch vẫn render trang và để API chặn (AC-12)
- [ ] Nhãn và thứ tự nhóm lấy từ `NHAN_VAI_TRO`, `THU_TU_VAI_TRO`: `0900000006` thấy "Ban quản trị", không thấy "Cư dân"

## Acceptance-criteria coverage
- AC-1: Commands 1, 2 · AC-2: Commands 3, 4 · AC-3: Commands 5 · AC-4: Commands 3 · AC-5: Commands 7 · AC-6: Commands 8, Value sourcing 4, 5 · AC-7: UI 5, Commands 6 · AC-8: Commands 5 · AC-9: Commands 6 · AC-10: UI 2, 3, 4 · AC-11: UI 7, 8 · AC-12: UI 11 · AC-13: UI 9, 10 · AC-14: UI 1, 3 · AC-15: UI 5, 6 · AC-16: UI 12, Commands 9
