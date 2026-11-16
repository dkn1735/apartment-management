# Verify: Design system & nền tảng giao diện · spec 0003 · updated 2026-11-16

_Steps derived from spec 0003 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: `cd frontend && pnpm dev`, mở `http://localhost:3000/dev/ui` và `http://localhost:3000/dev/ui/shell`.

## UI / manual

- [x] Mở `/dev/ui`, mục Màu: mọi ô màu hiện tên biến và giá trị khớp `src/app/globals.css` → AC-1
- [x] Mục Chữ: chuỗi `Ẳ Ẵ Ặ Ỷ Ữ ặ ỡ ừ` hiện bằng Be Vietnam Pro ở mọi cỡ, dấu không bị cắt (kể cả `type-page-title` 24px dưới 640px) → AC-2
- [x] Mục Chữ số đều cột: ghi chú nói Be Vietnam Pro không có `tnum`; cột tiền và diện tích trong bảng mẫu căn phải → AC-2 (nhánh dự phòng của spec)
- [x] Đi hết `/dev/ui` chỉ bằng phím Tab, Shift+Tab, Enter, Space, phím mũi tên, Esc: mọi nút, ô nhập, Select, Checkbox, Switch, hộp thoại, bảng dùng được; vòng focus 2px màu đỏ gạch, cách 2px, luôn thấy → AC-3, AC-4
- [x] Rê chuột lên từng biến thể nút, mục Select, dòng bảng, mục menu: nền đổi theo token (`--primary-hover`, `--destructive-hover`, `--accent`) → AC-3
- [x] Trong `HeroBand` và mục menu đang chọn, Tab tới một phần tử: vòng focus màu sáng `--on-dark` → AC-4
- [x] Mở `ConfirmDialog` "Khóa tài khoản (thành công)": focus ở "Hủy"; Tab không thoát khỏi hộp thoại; Esc đóng, focus về nút mở → AC-4
- [x] Form mẫu, tình huống Thành công: nhập đủ, bấm Lưu, nút hiện vòng quay; bấm Lưu lần nữa ngay không gửi lần hai; toast "Đã lưu" hiện góc dưới phải, tự tắt sau khoảng 5 giây → AC-5, AC-8
- [x] Form mẫu: rời ô Số điện thoại khi trống thì lỗi kèm icon hiện ngay; gõ đúng thì lỗi biến mất theo từng phím → AC-5
- [x] Form mẫu: để trống Loại căn hộ rồi gửi, focus về nút mở Select; trình đọc màn hình đọc nhãn "Loại căn hộ * (bắt buộc)" và câu lỗi → AC-5
- [x] Tình huống VALIDATION_FAILED: lỗi "Số điện thoại đã được dùng…" dưới ô Số điện thoại, ô đó nhận focus; lỗi "Mã lạ…" nằm trong khung lỗi đầu form → AC-6
- [x] Tình huống INTERNAL_ERROR: khung lỗi đầu form nhận focus, có "Mã tra cứu: …" và nút sao chép; bấm sao chép báo "Đã sao chép"; gửi lại vẫn chuyển focus về khung → AC-6, AC-7
- [x] Chặn quyền clipboard của trang (DevTools) rồi bấm sao chép: mã được bôi chọn, báo "Hãy nhấn Ctrl+C để sao chép" → AC-7
- [x] Tình huống CONFLICT: có câu chung, không có dòng mã tra cứu; câu "Raw backend message…" không xuất hiện ở đâu → AC-6, AC-7
- [x] Mục Phản hồi: "Toast lỗi có mã tra cứu" không tự tắt sau 30 giây, có nút "Đóng" và mã tra cứu → AC-7, AC-8
- [x] Mục Bảng: tải đầu thấy skeleton; bấm "Trang sau" thấy dòng cũ mờ đi cùng thanh tải mỏng rồi trang mới; đổi "Số dòng mỗi trang" về trang 1 → AC-9
- [x] Mục Bảng: bấm tiêu đề "Mã căn hộ" ba lần: tăng, giảm, bỏ; tiêu đề "Phí tháng" không bấm được → AC-9
- [x] Mục Bảng: chọn "Rỗng" thấy "Chưa có dữ liệu", không có thanh phân trang; chọn "Lỗi" thấy trạng thái lỗi có mã tra cứu và "Thử lại" (sau khoảng 2 giây vì thử lại 1 lần) → AC-9, AC-19
- [x] Mục Bảng: Tab vào khung bảng rồi dùng phím mũi tên trái, phải để cuộn ngang; cột Ghi chú cắt ở 2 dòng; tên dài xuống dòng → AC-10
- [x] Mục Số liệu: `4.850.000 ₫`, `25/11/2026`, `16/11/2026 14:05`, và `—` cho ngày rỗng; đổi múi giờ máy (DevTools, Sensors, Location) sang New York rồi tải lại, giá trị không đổi → AC-11
- [x] `/dev/ui/shell` rộng từ 1024px: thanh bên trái cố định, mục đang chọn là viên thuốc tối; thương hiệu là icon tòa nhà màu chính cạnh "Quản lý Chung cư" → AC-12
- [x] `/dev/ui/shell` dưới 1024px: nút "Mở menu" mở ngăn kéo; bấm một mục thì ngăn kéo đóng và mục đó có `aria-current="page"` → AC-12
- [x] `/dev/ui/shell`: phím Tab đầu tiên là "Bỏ qua, tới nội dung chính"; Enter đưa focus vào nội dung chính → AC-4
- [x] `/dev/ui` và `/dev/ui/shell` ở 360px và khi phóng to 200% (cửa sổ 1280px): không cuộn ngang trang → AC-12
- [x] Mở `/khong-co`: trang 404 tiếng Việt, một `h1`, nút "Về trang chủ" → AC-13
- [x] Bật giảm chuyển động của hệ điều hành: Dialog, Sheet, toast mở không có hiệu ứng; nút đang tải vẫn có chữ ẩn "Đang tải" và `aria-busy` → AC-17
- [x] Link trong đoạn văn (mục lục, link tới trang AppShell) luôn gạch chân; trên điện thoại ô nhập không làm iOS phóng to (chữ 16px) → AC-18
- [x] Chạy axe DevTools trên `/dev/ui` và `/dev/ui/shell` trong trình duyệt thật, gồm cả tương phản: 0 vi phạm nghiêm trọng → AC-1, AC-15

## Commands

- [x] `cd frontend && pnpm test` → mọi test qua (98 lúc build), gồm tương phản, màu thô, QueryClient, form, bảng, AppShell, production → AC-1, AC-5 đến AC-12, AC-14 đến AC-16, AC-19
- [x] `cd frontend && pnpm lint && pnpm format:check && pnpm typecheck` → sạch
- [x] `cd frontend && pnpm build && pnpm exec next start -p 3100`, rồi `curl -s -o /dev/null -w "%{http_code}" http://localhost:3100/dev/ui` và `/dev/ui/shell`, `/dev/ui/shell/can-ho` → `404`; `/` → `200` → AC-14
- [x] Thêm tạm `className="text-gray-600"` vào một file `.tsx` rồi `pnpm test` → test màu thô đỏ; bỏ đi → xanh → AC-16
- [x] Đổi tạm `--muted-foreground` trong `globals.css` thành `#9a918a` rồi `pnpm test` → test tương phản đỏ → AC-1
- [x] CI (`.github/workflows/ci.yml`) có bước `pnpm test` trong job frontend → AC-15

## Value sourcing

- [x] Câu lỗi chung: tình huống `XYZ` với status 422 hiện "Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại." (test `error-messages.test.ts`) → từ điển `errorCode`, thiếu thì theo `ApiError.status`
- [x] Lỗi theo trường: `errors[].field` trùng tên ô (`soDienThoai`) vào ô; không trùng (`maLa`) vào `FormAlert` dạng danh sách
- [x] Mã tra cứu: có `requestId` mà lỗi 4xx thì không hiện; lỗi 5xx không có `requestId` thì không hiện dòng
- [x] Thanh phân trang: "135 kết quả", "Trang 2 / 14" (10 dòng) đúng với `totalItems`, `page`, `totalPages`
- [x] `aria-sort` trên `th` đổi theo prop `sort`, không theo trạng thái nội bộ
- [x] Ngày giờ: `formatNgayGio("2026-11-16T17:30:00Z")` ra `17/11/2026 00:30` (qua nửa đêm giờ Việt Nam)
- [x] Tiền: `formatTien(4850000)` có NBSP trước `₫`
- [x] Mục menu đang chọn: ở `/dev/ui/shell/can-ho/123` mục "Căn hộ" được chọn; ở `/dev/ui/shell` chỉ "Tổng quan"
- [x] Màu chip trạng thái: bảng mẫu ánh xạ "Đang ở" sang `success`, "Đang sửa chữa" sang `warning`
- [x] Tên app: tab trình duyệt và thương hiệu đều là "Quản lý Chung cư" từ `APP_NAME`
- [x] Ẩn `/dev/*`: theo `process.env.NODE_ENV` (bước curl ở trên)
- [x] Tham số `sort=`: bấm sắp xếp gửi `maCanHo,asc` (tên trong `meta.sortField`)
- [x] `page`, `size` từ URL: `parseTableParams` với `page=abc&size=100` ra trang 1, 20 dòng
- [x] Thử lại truy vấn: 404 gọi 1 lần, 500 gọi 2 lần (test `query-client.test.tsx`)
- [x] Ô nhận focus sau lỗi: lỗi gắn được vào ô thì ô đầu tiên theo thứ tự trong form; không có thì `FormAlert`

## Acceptance-criteria coverage

- AC-1: bảng màu, axe trình duyệt, test tương phản, bước đổi màu · AC-2: thử dấu, ghi chú tnum · AC-3: đi phím, rê chuột · AC-4: vòng focus, ConfirmDialog, liên kết bỏ qua
- AC-5: form mẫu (gửi, rời ô, Select) · AC-6: VALIDATION_FAILED, INTERNAL_ERROR, CONFLICT · AC-7: mã tra cứu, sao chép, chặn clipboard, toast lỗi · AC-8: toast thành công và lỗi
- AC-9: bảng (skeleton, chuyển trang, sắp xếp, rỗng, lỗi) · AC-10: cuộn ngang bằng phím, cắt 2 dòng · AC-11: định dạng và đổi múi giờ · AC-12: AppShell rộng, hẹp, 360px, 200%
- AC-13: trang 404 · AC-14: curl bản production · AC-15: `pnpm test`, CI, axe · AC-16: bước màu thô · AC-17: giảm chuyển động · AC-18: link gạch chân, chữ 16px · AC-19: thử lại 1 lần
