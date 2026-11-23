# Verify: Tòa nhà, tầng & khu vực dùng chung · spec 0011 · updated 2026-11-23

_Các bước rút ra từ acceptance criteria của spec 0011. `/check verify` chạy các bước này; `/test` khóa lại những bước bền._

Chuẩn bị: `docker compose up -d`; backend chạy profile `dev,seed`; `pnpm dev`. Tài khoản seed mật khẩu `ChungCu@2026`: BQL `0900000002`, Kỹ thuật `0900000003`, Kế toán `0900000004`, BQT `0900000006`, Cư dân `0900000007`.

## UI / manual

- [ ] BQL mở `/toa-nha` → thấy nút "Thêm tòa nhà", tên tòa "Tòa A" là link tới `/toa-nha/A` → AC-15, AC-16
- [ ] Kế toán mở `/toa-nha` → không có nút thêm, "Tòa A" là chữ thường; Kỹ thuật thấy mục "Tòa nhà" trong menu và tên tòa là link → AC-10, AC-15, AC-16
- [ ] BQL mở dialog, gõ mã `b` → ô hiện `B`; ô Số tầng là ô chữ, bàn phím số trên điện thoại → AC-15
- [ ] Nhập tên "tòa a" (đã có "Tòa A") rồi gửi → lỗi "Tên tòa nhà đã được dùng" dưới ô Tên, ô Tên nhận focus, dialog vẫn mở → AC-3, AC-15
- [ ] Nhập số tầng `abc` rồi gửi → lỗi "Số tầng phải là số nguyên từ 1 đến 99" dưới ô Số tầng (server trả, không chặn ở trình duyệt) → AC-2, AC-15
- [ ] Sửa thành hợp lệ (mã `B`, tên "Tòa B", 5 tầng) → dialog đóng, toast "Đã thêm tòa nhà Tòa B", danh mục có Tòa B, focus về nút "Thêm tòa nhà" → AC-1, AC-15
- [ ] Mở `/toa-nha/B` → tên, mã, địa chỉ, số tầng 5, số căn hộ 0; bảng tầng B-01 đến B-05; đổi sắp xếp và trang thì `sort`, `page` lên URL → AC-8, AC-9, AC-16
- [ ] Bấm "Thêm tầng" → hộp thoại "Thêm tầng 6 vào Tòa B?", focus ở "Hủy"; xác nhận → toast "Đã thêm tầng 6", số tầng và bảng tầng cập nhật → AC-4, AC-17
- [ ] Mở `/toa-nha/B` ở hai tab, thêm tầng ở tab 1, rồi xác nhận ở tab 2 → tab 2 tải lại số tầng mới và báo "Tòa vừa được cập nhật, vui lòng thử lại" → AC-5, AC-17
- [ ] Tòa có 99 tầng (tạo tòa 99 tầng) → nút "Thêm tầng" bị vô hiệu kèm câu "Tòa đã đạt tối đa 99 tầng" → AC-17
- [ ] Mở `/toa-nha/KHONGCO` → "Không tìm thấy tòa nhà" có link về danh mục → AC-16
- [ ] Kế toán mở `/toa-nha/A` → thấy thông tin tòa, phần tầng là "Bạn không có quyền xem danh sách tầng" → AC-16
- [ ] BQL mở `/khu-vuc-chung` → ba khu vực của tòa A, sắp theo tên: Hành lang tầng 2, Phòng kỹ thuật điện nước, Sảnh chính → AC-11, AC-12, AC-18
- [ ] Gõ "SẢNH" → sau khoảng 300 ms chỉ còn "Sảnh chính", `q` lên URL, về trang 1; gõ "sanh" → "Không có khu vực nào khớp bộ lọc" → AC-11, AC-18
- [ ] Chọn tòa "Tòa A" ở combobox → `maToaNha=A` lên URL; mở `/khu-vuc-chung?maToaNha=ZZ` → "Không tìm thấy tòa nhà" và nút "Xóa bộ lọc" bỏ `maToaNha` khỏi URL → AC-18
- [ ] Cư dân, Kế toán không thấy mục "Khu vực chung"; mở thẳng URL thì bị chặn trang → AC-13, AC-18
- [ ] Đi hết các dialog, combobox, bảng chỉ bằng bàn phím (Tab, Enter, Esc, phím mũi tên), vòng focus luôn thấy; ở bề rộng 390px không cuộn ngang → AC-19

## Commands

- [ ] `cd backend && mvnw.cmd verify` → xanh (cần `COOKIE_SECURE=false` và không đặt `MAIL_HOST` trong shell) → mọi AC phía API
- [ ] `cd frontend && pnpm test && pnpm typecheck && pnpm lint` → xanh → AC-15 đến AC-19
- [ ] `psql`: `SELECT conname FROM pg_constraint WHERE conname IN ('chk_toa_nha_ma','chk_toa_nha_so_tang','chk_tang_ma','chk_tang_so_tang')` → đủ 4 dòng; `toa_nha_ten_toa_nha_key` không còn; index `uq_toa_nha_ten_lower` có → AC-7
- [ ] `INSERT INTO tang VALUES ('A-7','A',7)` và `INSERT INTO toa_nha VALUES ('a-1','X',1,'K')` → bị CHECK từ chối → AC-7
- [ ] `POST /api/v1/buildings` hai lần cùng mã → lần hai 409 `CANHO_TOA_NHA_DA_TON_TAI` mục `maToaNha`; không có header `Location` → AC-3
- [ ] `POST /api/v1/buildings/A/floors` với `{"soTang":3}` → 422 `CANHO_TANG_SAI_THU_TU` "Tầng tiếp theo của tòa này là tầng 4"; `{"soTang":"4"}` → 400 mục `soTang`; tới `KHONGCO` → 404 → AC-4, AC-5
- [ ] Sau khi thêm tòa và tầng, `nhat_ky_he_thong` có `CANHO_THEM_TOA_NHA` (`ToaNha:B`, `{"soTang": 5}`) và `CANHO_THEM_TANG` (`Tang:B-06`, `{"soTang": 6, "maToaNha": "B"}`); lỗi không ghi dòng nào → AC-14

## Value sourcing

- [ ] Mã, tên, địa chỉ tòa lưu đúng giá trị đã chuẩn hóa: gửi `" b "`, `"  Tòa   B "` → lưu `B`, `Tòa B`; tên dạng NFD lưu NFC → AC-1
- [ ] N dòng tầng sinh từ `generate_series`: thêm tòa 12 tầng → mã `X-01` đến `X-12`, đúng hai chữ số → AC-6
- [ ] `soCanHo` của tòa mới luôn 0; của tòa A là 3 (đếm `can_ho` qua `tang`) → AC-1, AC-9
- [ ] `maNguoiThucHien` của nhật ký là mã người đang đăng nhập (đổi tài khoản BQL khác thì mã đổi theo) → AC-14
- [ ] Tầng kế tiếp đọc dưới khóa dòng: hai request song song cùng `soTang` → đúng một 201 và một 422 (test `TangIntegrationTest.haiRequestSongSongChiMotThanhCong`) → AC-5, AC-7
- [ ] Mã tầng của lần thêm một tầng là `MaTang.cua` → `B-06`, khớp CHECK `chk_tang_ma` → AC-6
- [ ] Kiểm trùng tên dùng `lower()` của PostgreSQL với collation ICU: "TÒA A", "tòa a" đều trùng "Tòa A" (chữ có dấu viết hoa) → AC-3
- [ ] `soCanHo` mỗi tầng đếm lúc đọc: tầng A-01 đến A-03 mỗi tầng 1 → AC-8
- [ ] `tenToaNha` của khu vực lấy từ join `toa_nha` → "Tòa A" → AC-11
- [ ] Phạm vi theo người gọi: tòa ngoài phạm vi và tòa không tồn tại cùng trả 404 `NOT_FOUND` → AC-4, AC-8, AC-9, AC-11
- [ ] Dialog thêm tầng đề xuất `soTang` đang hiển thị cộng 1 → tòa 6 tầng đề xuất "Thêm tầng 7" → AC-17
- [ ] Combobox tòa lấy `GET /api/v1/buildings?size=100&sort=tenToaNha,asc` → danh sách theo tên → AC-18
- [ ] Nút theo vai trò lấy từ `useHoSo().data.vaiTro` → đăng nhập tài khoản có cả BQT và BQL thì thấy nút thêm → AC-15, AC-17

## Acceptance-criteria coverage

- AC-1, AC-2, AC-3: thêm tòa (UI 3 đến 6, lệnh 5, value sourcing 1, 7) · AC-4, AC-5: thêm tầng (UI 8, 9, lệnh 6, value sourcing 5) · AC-6, AC-7: mã tầng và CHECK (lệnh 3, 4, value sourcing 2, 6) · AC-8, AC-9: xem tầng và chi tiết (UI 7, value sourcing 3, 8) · AC-10: Kỹ thuật xem danh mục (UI 2) · AC-11, AC-12: khu vực (UI 13, 14) · AC-13: phân quyền (UI 16, lệnh 1) · AC-14: nhật ký (lệnh 7) · AC-15 đến AC-18: giao diện (UI 1 đến 16) · AC-19: bàn phím và WCAG (UI 17)
