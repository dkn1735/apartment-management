# Verify: Khóa tạm khi đăng nhập sai nhiều lần · spec 0009 · updated 2026-11-23

_Steps derived from spec 0009 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

Chuẩn bị: `docker compose up -d`, backend với `SPRING_PROFILES_ACTIVE=dev,seed`, `pnpm dev`. Mật khẩu seed `ChungCu@2026`. Muốn làm lại từ đầu thì Admin bấm "Mở khóa đăng nhập" hoặc chờ 15 phút.

## UI / manual

- [x] Vào `/dang-nhap`, nhập `0900000007` với mật khẩu sai → khung lỗi `role="alert"` báo "Số điện thoại, email hoặc mật khẩu không đúng.", dưới nút hiện "Nhập sai 5 lần liên tiếp sẽ bị tạm khóa đăng nhập 15 phút.", form có `aria-describedby` trỏ tới câu đó → AC-1, AC-13
- [x] Tải lại trang → câu gợi ý biến mất (state của form) → AC-13
- [x] Sai thêm tới lần thứ 5 → "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút hoặc liên hệ ban quản lý."; câu gợi ý vẫn còn; nút Đăng nhập không bị vô hiệu hóa → AC-1, AC-7, AC-13, AC-15
- [x] Nhập đúng mật khẩu ngay sau đó → vẫn bị chặn với cùng câu, không vào được → AC-2
- [x] Đăng nhập Admin `0900000001`, mở `/tai-khoan/ND00000007` → thẻ Trạng thái và bảo mật có "Đang khóa đăng nhập đến HH:mm dd/MM/yyyy" (giờ Việt Nam, bằng giờ lần sai thứ 5 cộng 15 phút) và nút "Mở khóa đăng nhập" → AC-9, AC-14
- [x] Tab tới nút, nhấn Enter (không có hộp thoại) → toast "Đã mở khóa đăng nhập", dòng và nút biến mất, focus nằm ở tiêu đề "Trạng thái và bảo mật" → AC-8, AC-14
- [x] `0900000007` đăng nhập đúng ngay sau đó → vào được → AC-8
- [x] Tài khoản không bị khóa → thẻ không có dòng khóa và nút → AC-14
- [x] `/nhat-ky`: bộ lọc loại thao tác có "Tạm khóa đăng nhập do nhập sai nhiều lần" và "Mở khóa đăng nhập"; lọc theo từng loại thấy đúng dòng → AC-11
- [x] Khóa `0900000007`, Admin bấm Đặt lại mật khẩu → đăng nhập ngay bằng mật khẩu tạm được → AC-10

## Commands

- [x] Sai 5 lần với `curl -i -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -H 'X-Requested-With: XMLHttpRequest' -d '{"taiKhoan":"0900000007","matKhau":"Sai1"}'` → 4 lần 401 `AUTH_INVALID_CREDENTIALS`, lần 5 là 429 `AUTH_TOO_MANY_ATTEMPTS` với `Retry-After: 900` → AC-1, AC-7
- [x] Lặp lại cho một số không có tài khoản (ví dụ `0999999999`) cùng lúc → status, body (trừ `requestId`) và header của lần 5 giống tài khoản thật → AC-5, AC-7
- [x] `abc` sai 10 lần → luôn 401, `SELECT count(*) FROM dang_nhap_sai_dinh_danh` không tăng → AC-5
- [x] Đang khóa: `SELECT so_lan_dang_nhap_sai, khoa_tam_den, lan_sai_cuoi_luc, phien_ban, cap_nhat_luc FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000007'` → `khoa_tam_den` = giờ lần 5 cộng 15 phút, bộ đếm 0, `phien_ban` và `cap_nhat_luc` không đổi so với trước khi sai → AC-1
- [x] Gửi thêm vài lần trong lúc khóa → `nhat_ky_he_thong` không có dòng mới, log backend có `Đăng nhập thất bại: lyDo=KHOA_TAM, maNguoiDung=ND00000007` → AC-2, AC-11, AC-17
- [x] `SELECT loai_thao_tac, chi_tiet_thay_doi FROM nhat_ky_he_thong WHERE loai_thao_tac LIKE '%KHOA_TAM%' ORDER BY ma_log DESC` → một dòng `AUTH_KHOA_TAM_DANG_NHAP` cho lần 5, một dòng `AUTH_MO_KHOA_TAM_DANG_NHAP` (người thực hiện là Admin) khi mở sớm; chi tiết chỉ có `khoaTamDen` → AC-11
- [x] `grep` log backend và `nhat_ky_he_thong` theo `0999999999` và SHA-256 của nó → không thấy → AC-11, AC-17
- [x] Gọi `DELETE /api/v1/accounts/{ma}/login-lock` bằng tài khoản Cư dân → 403 `FORBIDDEN`; BQL với tài khoản nhân viên → 404 "Không tìm thấy tài khoản"; không cookie → 401 → AC-8, AC-16
- [x] Gọi DELETE lần hai khi không còn khóa → 200, không thêm dòng nhật ký → AC-8
- [x] Khóa một số ảo, Admin tạo tài khoản với số đó → dòng của số đó trong `dang_nhap_sai_dinh_danh` biến mất, đăng nhập sai với số đó trả 401 → AC-19
- [x] `cd backend && mvnw.cmd verify` → xanh, gồm `KhoaTamDangNhapIntegrationTest`, `KhoaTamDinhDanhIntegrationTest`, `KhoaTamSongSongIntegrationTest` (20 luồng: tối đa 4 lần 401, tối đa 5 lần so mật khẩu), `MoKhoaDangNhapIntegrationTest`, `KhoaTamNhatKyIntegrationTest`, `DangNhapSaiDonDepIntegrationTest` → AC-1 tới AC-12, AC-16 tới AC-19
- [x] `cd frontend && pnpm vitest run` → xanh → AC-13, AC-14, AC-15

## Value sourcing

- [x] `N` = 5, `T` = 15 phút: lần thứ 5 (không phải 4 hay 6) bị khóa, `Retry-After` đầu tiên là 900 → hằng `KhoaTamDangNhap`
- [x] `:now` từ `Clock`: cửa sổ đếm sai 4 lần, chờ hơn 15 phút, sai lần nữa → 401 và bộ đếm 1 (test `muoiLamPhutKhongSaiThiBoDemVeKhong` dùng `MutableClock`, kể cả mốc 14 phút 59 giây) → AC-18
- [x] `dinh_danh_hash`: hash của dạng đã chuẩn hóa, nên `+84 999 999 999` và `0999999999` dùng chung một dòng; `ABC@X.TEST` và `abc@x.test` cũng vậy → AC-5
- [x] `Retry-After` làm tròn lên: đang khóa còn 299,5 giây thì header là 300; còn 0,5 giây thì là 1 → AC-7
- [x] Số phút trên trang đăng nhập lấy từ `Retry-After`: chờ 3 phút rồi thử lại khi đang khóa → câu báo "12 phút" → AC-13
- [x] `khoaTamDen` ở chi tiết đổi thành `null` khi đã tới hạn: chờ qua giờ khóa rồi tải lại trang chi tiết → dòng khóa biến mất dù cột trong DB còn giá trị → AC-9
- [x] Giờ hiển thị theo `Asia/Ho_Chi_Minh` dù máy đặt múi giờ khác → AC-14
- [x] Mở khóa sớm: ghi nhật ký chỉ khi giá trị cũ còn trong tương lai (gọi khi khóa đã hết hạn → 200, không ghi) → AC-8, AC-11
- [x] Người thực hiện của `AUTH_MO_KHOA_TAM_DANG_NHAP` là tài khoản đang đăng nhập (Admin hoặc BQL) → AC-11
- [x] Hash cần xóa khi tạo hay sửa tài khoản dùng số điện thoại và email sau chuẩn hóa (`+84...` và chữ hoa đều xóa đúng dòng) → AC-19
- [x] Job dọn: mốc là `Clock` trừ 15 phút; dòng 14 phút và dòng đang khóa còn lại → AC-12

## Acceptance-criteria coverage

- AC-1 UI 1, 3; Commands 1, 4 · AC-2 UI 4; Commands 5 · AC-3 `toiHanThiTuMo` · AC-4 `dungMatKhauThiDatLai`, `taiKhoanTamKhoaDungMatKhauThiDatLai`, `taoPhienLoiThiBoDemCungRollback` · AC-5 Commands 2, 3 · AC-6 `KhoaTamSongSongIntegrationTest` · AC-7 UI 3; Commands 1, 2 · AC-8 UI 6, 7; Commands 8, 9 · AC-9 UI 5; Value sourcing 6 · AC-10 UI 10 · AC-11 UI 9; Commands 5, 6, 7 · AC-12 Value sourcing 11 · AC-13 UI 1, 2, 3 · AC-14 UI 5, 6, 8 · AC-15 UI 3; `api.test.ts` · AC-16 Commands 8 · AC-17 Commands 5, 7 · AC-18 Value sourcing 2 · AC-19 Commands 10
