# Verify: Tạm dừng quyền tiện ích căn hộ · spec 0013 · updated 2026-11-26
_Steps derived from spec 0013 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chạy với `docker compose up -d`, backend profile `dev,seed`, `pnpm dev`. Tài khoản seed: BQL `0900000002`, Kế toán `0900000004`, BQT `0900000006`, Cư dân `0900000007` (căn `A-0201`), mật khẩu `ChungCu@2026`.

## UI / manual
- [x] Đăng nhập Kế toán, mở `/can-ho/A-0101` → có nút "Tạm dừng tiện ích" cạnh nút "Sửa" (Kế toán không thấy "Sửa") → AC-15
- [x] Bấm nút → dialog "Tạm dừng tiện ích căn hộ A-0101", ô "Lý do" bắt buộc, gợi ý "Còn 500/500 ký tự" đổi khi gõ → AC-15, AC-17
- [x] Gửi khi lý do rỗng hoặc chỉ khoảng trắng → lỗi "Vui lòng nhập lý do" dưới ô, không gọi API → AC-3, AC-15
- [x] Nhập " Nợ phí quý 3 " rồi gửi → toast "Đã tạm dừng tiện ích căn hộ A-0101", dialog đóng, focus về nút, trang hiện "Tạm dừng tiện ích", "Lý do tạm dừng: Nợ phí quý 3", "Cập nhật lúc <giờ Việt Nam> bởi Phạm Kế Toán (<mã>)" → AC-1, AC-2, AC-8, AC-14, AC-15
- [x] Nút đổi thành "Mở khóa tiện ích"; mở khóa với lý do có xuống dòng và `<b>x</b>` → dòng "Lý do mở khóa" giữ xuống dòng, hiện `<b>x</b>` như chữ → AC-14
- [x] Mở hai tab cùng căn, tạm dừng ở tab 1, rồi tạm dừng ở tab 2 → dialog tab 2 hiện "Căn hộ đang bị tạm dừng tiện ích.", đóng dialog thì nút thành "Mở khóa tiện ích" → AC-5, AC-15
- [x] Đăng nhập BQT, mở `/can-ho/A-0101` → không có nút đổi tiện ích; vẫn thấy người đổi → AC-13, AC-9
- [x] Đăng nhập Cư dân, BQL tạm dừng `A-0201` trước; cư dân mở `/can-ho-cua-toi/A-0201` → thấy lý do và giờ, không có "bởi ..." và không có nút → AC-9, AC-14
- [x] `/can-ho`: ô lọc "Tiện ích" chọn "Đang tạm dừng" → URL có `biTamDungTienIch=true`, về trang 1, chỉ `A-0204` (seed) và các căn vừa tạm dừng → AC-11, AC-16
- [x] Sửa tay URL thành `biTamDungTienIch=TRUE` → ô lọc về "Tất cả", danh sách không lọc, không báo lỗi → AC-16
- [x] Toàn bộ nút, dialog, ô lý do, ô lọc dùng được chỉ bằng bàn phím, focus nhìn thấy được → AC-17
- [x] Màn hình nhật ký: ô lọc loại thao tác có "Tạm dừng tiện ích căn hộ" và "Mở khóa tiện ích căn hộ"; lọc ra các dòng vừa tạo, chi tiết `{ "lyDo": ... }`, đối tượng `CanHo:A-0101` → AC-7

## Commands
- [x] `cd backend && mvnw.cmd verify` → BUILD SUCCESS, gồm `TamDungTienIchIntegrationTest`, `TamDungTienIchServiceTest`, `PhanQuyenCoverageTest` → AC-1 đến AC-13
- [x] PATCH với `{"biTamDungTienIch":"true","lyDo":"x"}`, `{"biTamDungTienIch":1,...}`, `{}` → 400, mục theo thứ tự `biTamDungTienIch`, `lyDo` → AC-3
- [x] PATCH lý do 501 ký tự → 400 "Lý do tối đa 500 ký tự"; 500 emoji → 200 (đếm code point) → AC-3
- [x] PATCH `ZZ-9999` body đúng → 404; body sai → 400 (400 trước 404) → AC-4
- [x] PATCH bằng BQT, Kỹ thuật, Cư dân → 403; không cookie → 401 → AC-13
- [x] `GET /api/v1/apartments?biTamDungTienIch=false&maToaNha=A` trên seed mới → 11 căn; `=1` kèm `q` dài 21 ký tự → hai mục lỗi `q` rồi `biTamDungTienIch` → AC-11
- [x] Chạy lại backend profile `seed` → `tien_ich_doi_luc` của `A-0204` vẫn `2026-11-01 09:00+07` → AC-12
- [x] Trong psql: `UPDATE can_ho SET bi_tam_dung_tien_ich = TRUE WHERE ma_can_ho = 'A-0101'` khi chưa có lý do → bị CHECK từ chối; tương tự lý do `'  '`, chỉ một trong ba cột, người đổi không tồn tại → AC-10
- [x] `docs/api/openapi.json`: `lanDoiTienIch` là `oneOf [$ref, null]`; `schema.d.ts` có `LanDoiTienIchResponse | null`, `biTamDungTienIch?: boolean` trên query, body PATCH `biTamDungTienIch: boolean` → AC-8, AC-11

## Value sourcing
- [x] Cờ mới đúng giá trị body; cờ cũ đọc dưới khóa (hai request song song cùng chiều → một 200, một 409, một dòng nhật ký) → AC-6
- [x] `tien_ich_ly_do` là lý do đã NFC và cắt hai đầu (gửi dạng NFD "Nợ" → lưu NFC) → AC-2
- [x] `tien_ich_doi_luc` lấy từ bean `Clock`, cắt micro giây; giờ hiện trên UI là giờ Việt Nam (+7) → AC-2, AC-14
- [x] `tien_ich_doi_boi` và người thực hiện nhật ký là mã của người gọi, không phải từ body → AC-2, AC-7
- [ ] Phạm vi tòa: nhân viên ngoài tòa gọi PATCH → 404 → AC-4
- [x] Message 200 theo chiều yêu cầu; 409 theo cờ hiện tại → AC-1, AC-5
- [x] Loại nhật ký: true → `CANHO_TAM_DUNG_TIEN_ICH`, false → `CANHO_MO_KHOA_TIEN_ICH` → AC-7
- [x] `hoTenNguoiDoi` là họ tên hiện tại: đổi họ tên người đã đổi cờ ở màn tài khoản → trang căn hiện họ tên mới → AC-8
- [x] Ẩn người đổi chỉ khi xem nhờ điều kiện cư dân; người vừa là nhân viên tòa vừa là cư dân thấy đủ → AC-9
- [x] Nhãn "Lý do tạm dừng" hay "Lý do mở khóa" theo cờ hiện tại → AC-14
- [x] Nút chỉ hiện khi vai trò có `BAN_QUAN_LY` hoặc `KE_TOAN` và đang ở trang nhân viên → AC-15
- [x] Số ký tự còn lại đếm theo code point (gõ emoji giảm 1 mỗi emoji) → AC-15

## Acceptance-criteria coverage
- AC-1 UI 4, Cmd · AC-2 UI 4, VS · AC-3 UI 3, Cmd 2–3 · AC-4 Cmd 4, VS · AC-5 UI 6 · AC-6 VS 1 · AC-7 UI 12, VS · AC-8 UI 4, VS · AC-9 UI 7–8, VS · AC-10 Cmd 8 · AC-11 UI 9, Cmd 6 · AC-12 Cmd 7 · AC-13 UI 7, Cmd 5 · AC-14 UI 4–5, 8 · AC-15 UI 1–4, 6 · AC-16 UI 9–10 · AC-17 UI 2, 11
