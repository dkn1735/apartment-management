# 0008. Quản lý tài khoản (Admin): mật khẩu tạm do hệ thống sinh và bắt đổi lần đầu, endpoint riêng cho từng vai trò, BQL chỉ quản lý cư dân

**Date**: 2026-11-22
**Status**: Proposed

## Summary

Spec này chốt cách Admin (và BQL, trong phạm vi cư dân) tạo, tìm, sửa, khóa, mở khóa tài khoản và đặt lại mật khẩu, vì cư dân không tự đăng ký được. Khi tạo hoặc đặt lại, hệ thống tự sinh một mật khẩu tạm, hiện đúng một lần cho người quản lý, và người dùng bị bắt đổi mật khẩu ở lần đăng nhập đầu (backend chặn mọi API khác cho tới khi đổi xong). Vai trò được thêm, sửa, gỡ bằng endpoint riêng cho từng vai trò; khóa hay ngừng hoạt động thu hồi mọi phiên ngay, còn access token đang cầm sống tối đa 15 phút như đã chấp nhận ở spec 0005. Cần một migration `V12` (thêm cột `phai_doi_mat_khau`, bật extension `unaccent` để tìm tên không dấu), tám loại nhật ký mới, ba màn hình dưới `/tai-khoan`.

## Requirements

**User stories**:
- Là Admin, tôi muốn tạo tài khoản cho nhân viên và cư dân với đúng vai trò, để họ đăng nhập được mà không cần tự đăng ký.
- Là Admin, tôi muốn tìm tài khoản theo tên, số điện thoại, email hay mã, lọc theo vai trò và trạng thái, để xử lý nhanh khi có người cần hỗ trợ.
- Là Admin, tôi muốn khóa, ngừng hoạt động, mở lại tài khoản và đặt lại mật khẩu, để chặn người đã nghỉ và giúp người quên mật khẩu.
- Là BQL, tôi muốn tạo và quản lý tài khoản cư dân, vì tôi là người xác minh cư trú, nhưng tôi không được đụng tới tài khoản nhân viên.
- Là người dùng mới, tôi muốn được dẫn thẳng tới trang đổi mật khẩu khi đăng nhập bằng mật khẩu tạm, để mật khẩu chỉ mình tôi biết.

**Thuật ngữ dùng trong spec**:
- *Người quản lý*: người gọi API, có vai trò `QUAN_TRI_VIEN` hoặc `BAN_QUAN_LY` trong access token.
- *Tài khoản cư dân thuần*: tài khoản có dòng ở `cu_dan` và không có dòng ở sáu bảng vai trò còn lại.
- *Phạm vi của người quản lý*: người có `QUAN_TRI_VIEN` thấy mọi tài khoản; người chỉ có `BAN_QUAN_LY` (không có `QUAN_TRI_VIEN`) chỉ thấy tài khoản cư dân thuần. Có cả hai vai trò thì theo Admin.
- *Admin đang hoạt động*: tài khoản có dòng ở `quan_tri_vien` và `trang_thai_tai_khoan = 'Hoạt động'`.

**Acceptance criteria**

*Tạo tài khoản và mật khẩu tạm*

- **AC-1**: `POST /api/v1/accounts` với body `{ hoTen, soDienThoai, email, vaiTro: [VaiTroRequest] }` tạo một dòng `nguoi_dung_he_thong` (`trang_thai_tai_khoan` `Hoạt động`, `phai_doi_mat_khau` `true`, `so_lan_dang_nhap_sai` 0) và đúng một dòng ở bảng con cho mỗi vai trò, trong một transaction. Trả 201 `message` "Tạo tài khoản thành công", `data` `{ taiKhoan: TaiKhoanChiTietResponse, matKhauTam }`, header `Cache-Control: no-store`. `vaiTro` có từ 1 tới 7 phần tử, không trùng mã; mỗi phần tử mang đúng các trường của vai trò đó (bảng *VaiTroRequest* trong Feature design), trường của vai trò khác phải vắng hoặc `null`. Chuẩn hóa trước khi kiểm và lưu: `hoTen` cắt khoảng trắng hai đầu, gộp khoảng trắng liên tiếp, đổi về NFC; `soDienThoai` qua helper mới `SoDienThoaiChuanHoa` (bỏ khoảng trắng, dấu chấm, gạch nối; `+84` hoặc `84` đầu chuỗi dài 11 hoặc 12 ký tự đổi thành `0`; kết quả phải khớp `^0[0-9]{9}$`), tách logic số điện thoại ra khỏi `TaiKhoanDangNhap.parse` để cả đăng nhập lẫn quản lý tài khoản dùng chung; `email` cắt khoảng trắng và đổi chữ thường, phải khớp `^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,}$` (không dùng `@Email` vì nó nhận `a@b`); mọi trường chữ của vai trò cắt khoảng trắng hai đầu và đổi về NFC. Kiểm rỗng và độ dài **sau** chuẩn hóa (họ tên chỉ toàn khoảng trắng là rỗng). Phần tử `null` trong `vaiTro` là lỗi `vaiTro[<chỉ số>]` "Vai trò không tồn tại".
- **AC-2**: Mật khẩu tạm dài 12 ký tự, sinh bằng `SecureRandom` từ bảng chữ `ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789` (bỏ `0 O 1 l I`), luôn có ít nhất một chữ cái và một chữ số, nên đạt quy tắc độ mạnh của `DoiMatKhauRequest`. Lưu bằng `PasswordEncoder` hiện có (`{bcrypt}`), băm ngoài transaction. Mật khẩu tạm chỉ có trong response của lần tạo hoặc lần đặt lại đó; không bao giờ có trong log ứng dụng, nhật ký kiểm toán, `toString()` của record, hay response nào khác.
- **AC-3**: Số điện thoại, email, hoặc số CCCD (khi có vai trò `CU_DAN`) đã thuộc tài khoản khác thì trả 409 `AUTH_IDENTITY_TAKEN` `message` "Thông tin đã được dùng cho tài khoản khác", `errors` một mục cho mỗi trường trùng theo thứ tự `soDienThoai`, `email`, `soCccd`, câu lần lượt "Số điện thoại đã được dùng cho tài khoản khác", "Email đã được dùng cho tài khoản khác", "Số CCCD đã được dùng cho cư dân khác". Áp dụng cho tạo tài khoản, sửa thông tin chung (AC-7) và thêm hoặc sửa vai trò `CU_DAN` (AC-8). So với chính tài khoản đang sửa thì không tính là trùng. Hai request cùng lúc vượt qua bước kiểm trước thì ràng buộc UNIQUE của DB chặn một bên, và bên đó nhận đúng response 409 này, không phải 500 và không phải `CONFLICT` chung của `GlobalExceptionHandler`: service bọc `saveAndFlush` (hoặc lệnh ghi bảng con) trong `try`, bắt `DataIntegrityViolationException`, đọc tên ràng buộc trong chuỗi cause và ném `ApiException`. Map: `nguoi_dung_he_thong_so_dien_thoai_key` → `soDienThoai`, `nguoi_dung_he_thong_email_key` → `email`, `cu_dan_so_cccd_key` → `soCccd`; ràng buộc `<bảng con>_ma_nguoi_dung_key` (hai request cùng thêm một vai trò) → 409 `CONCURRENT_MODIFICATION`. Trong trường hợp chạy đua, `errors` chỉ có một mục (DB báo một ràng buộc), chấp nhận được. Tên ràng buộc lạ thì ném lại như cũ.
- **AC-4**: Dữ liệu sai trả 400 `VALIDATION_FAILED` `message` "Dữ liệu không hợp lệ", một mục `errors` cho mỗi trường sai, với câu:
  - `hoTen` rỗng: "Vui lòng nhập họ tên"; dài quá 100 ký tự: "Họ tên tối đa 100 ký tự"
  - `soDienThoai` rỗng: "Vui lòng nhập số điện thoại"; sai dạng sau chuẩn hóa: "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0"
  - `email` rỗng: "Vui lòng nhập email"; sai dạng hoặc quá 150 ký tự: "Email không hợp lệ"
  - `vaiTro` rỗng: "Vui lòng chọn ít nhất một vai trò"; trùng mã: "Mỗi vai trò chỉ chọn một lần"; mã không có trong enum `VaiTro`: "Vai trò không tồn tại"
  - trường của vai trò thiếu: "Vui lòng nhập <nhãn trường>" (nhãn ở bảng *VaiTroRequest*); quá độ dài cột: "<Nhãn trường> tối đa <n> ký tự"; `soCccd` không đủ 12 chữ số: "Số CCCD phải gồm 12 chữ số"; trường không thuộc vai trò mà có giá trị: "Trường này không dùng cho vai trò đã chọn"
  - Tên trường trong `errors` của phần tử vai trò ở body tạo là `vaiTro[<chỉ số>].<trường>` (ví dụ `vaiTro[0].soCccd`, và `vaiTro[0].vaiTro` cho mã vai trò sai; mã sai được đọc thành chuỗi rồi kiểm ở service, không để Jackson báo 400 `MALFORMED_REQUEST`); ở body `PUT .../roles/{vaiTro}` là tên trường trần (`soCccd`).
  - `phienBan` thiếu hoặc không phải số nguyên không âm (body hay query): "Thiếu phiên bản dữ liệu, vui lòng tải lại".

*Tra cứu*

- **AC-5**: `GET /api/v1/accounts` trả 200 `message` "Tra cứu tài khoản thành công", `data` dạng `PageResponse` với mỗi phần tử `{ maNguoiDung, hoTen, soDienThoai, email, trangThaiTaiKhoan, phaiDoiMatKhau, vaiTro, dangNhapCuoiLuc, taoLuc }`; `vaiTro` là mảng mã theo thứ tự khai báo của enum `VaiTro`; không có `soCccd`, `matKhauHash`, hay mã bảng con. Chỉ gồm tài khoản trong phạm vi của người quản lý. Phân trang qua `TrangYeuCau`: `sort` chỉ nhận `taoLuc`, mặc định `taoLuc,desc`, tiêu chí phụ `ma_nguoi_dung` cùng chiều. Bộ lọc không bắt buộc, thiếu hoặc chuỗi rỗng coi như không gửi, kết hợp bằng AND:
  - `q` (cắt khoảng trắng, tối đa 100 ký tự, một ký tự trở lên là được, kể cả "ND"): giữ dòng có `unaccent(lower(ho_ten))` chứa `unaccent(lower(q))`, hoặc `so_dien_thoai` chứa `qSo`, hoặc `email` chứa `lower(q)`, hoặc `ma_nguoi_dung` chứa `upper(q)`. `qSo` là `q` sau khi bỏ khoảng trắng, dấu chấm, gạch nối và đổi `+84`/`84` đầu chuỗi thành `0`; `qSo` không chỉ gồm chữ số thì bỏ điều kiện số điện thoại. Ký tự `%`, `_`, `\` trong `q` được thoát, nên được so như chữ thường. Gõ "nguyen van" tìm thấy "Nguyễn Văn", gõ "dang" tìm thấy "Đặng".
  - `vaiTro`: đúng tên một giá trị `VaiTro`; giữ tài khoản có vai trò đó. BQL gửi tham số này thì vẫn được kiểm và áp dụng (lọc trên phạm vi cư dân thuần, nên khác `CU_DAN` cho kết quả rỗng), không bị bỏ qua.
  - `trangThai`: đúng một trong `Hoạt động`, `Tạm khóa`, `Ngừng hoạt động`.
  - Sai trả 400 `VALIDATION_FAILED` theo thứ tự `page`, `size`, `sort`, `q`, `vaiTro`, `trangThai`; câu của `page`, `size`, `sort` và giới hạn `size` (mặc định, tối đa 100) lấy nguyên từ `TrangYeuCau`; câu riêng: `q` "Từ khóa tối đa 100 ký tự", `vaiTro` "Vai trò không tồn tại", `trangThai` "Trạng thái không tồn tại".
  - Mỗi lần gọi chạy tối đa ba truy vấn: đếm, lấy trang, lấy vai trò của các tài khoản trong trang.
- **AC-6**: `GET /api/v1/accounts/{maNguoiDung}` trả 200 `message` "Lấy thông tin tài khoản thành công", `data` `TaiKhoanChiTietResponse` `{ maNguoiDung, hoTen, soDienThoai, email, trangThaiTaiKhoan, phaiDoiMatKhau, dangNhapCuoiLuc, doiMatKhauLuc, taoLuc, capNhatLuc, phienBan, vaiTro: [VaiTroTaiKhoan] }`. `vaiTro` theo thứ tự enum, mỗi phần tử có `vaiTro` (mã), `maVaiTro` (khóa chính của bảng con, ví dụ `BQL00000001`) và các trường của vai trò đó, kể cả `soCccd` đầy đủ cho `CU_DAN`. Không có `matKhauHash`. Mã sai dạng `^ND[0-9]{8}$`, không tồn tại, hoặc ngoài phạm vi của người quản lý thì trả 404 `NOT_FOUND` "Không tìm thấy tài khoản" (cùng câu cho cả ba, để BQL không dò được tài khoản nhân viên).

*Sửa thông tin và vai trò*

- **AC-7**: `PUT /api/v1/accounts/{maNguoiDung}` với `{ hoTen, soDienThoai, email, phienBan }` (cả bốn bắt buộc, chuẩn hóa và kiểm như AC-1, AC-4) trả 200 `message` "Cập nhật tài khoản thành công", `data` `TaiKhoanChiTietResponse` mới. `phienBan` khác giá trị trong DB thì 409 `CONCURRENT_MODIFICATION` "Tài khoản vừa được thay đổi ở nơi khác, vui lòng tải lại và thử lại." Không trường nào đổi sau chuẩn hóa thì 200 với dữ liệu hiện tại, `phienBan` giữ nguyên, không có dòng nhật ký. Đổi số điện thoại hay email không thu hồi phiên; lần đăng nhập sau dùng giá trị mới.
- **AC-8**: `PUT /api/v1/accounts/{maNguoiDung}/roles/{vaiTro}` với body `VaiTroCapNhatRequest` `{ phienBan, <trường của vai trò> }` (không có trường `vaiTro`, mã vai trò chỉ lấy từ path; với `QUAN_TRI_VIEN` body chỉ có `phienBan`) trả 200 `message` "Cập nhật vai trò thành công", `data` `TaiKhoanChiTietResponse` mới. Tài khoản chưa có vai trò đó thì thêm một dòng bảng con (mã mới từ sequence), nhật ký `AUTH_GAN_VAI_TRO`; đã có thì sửa các trường, nhật ký `AUTH_CAP_NHAT_VAI_TRO`, và không trường nào đổi thì 200 không ghi gì. Mọi lần thêm hoặc sửa tăng `phien_ban` và cập nhật `cap_nhat_luc` của `nguoi_dung_he_thong`: service nạp entity bằng `LockModeType.OPTIMISTIC_FORCE_INCREMENT` sau khi so `phienBan`, nên phiên bản luôn tăng kể cả khi `cap_nhat_luc` không đổi (ví dụ `Clock` cố định trong test). Lệnh ghi bảng con là SQL thuần qua `JdbcClient` (không có entity cho bảng con). `{vaiTro}` không có trong enum thì 400 `VALIDATION_FAILED` (trường `vaiTro`, "Vai trò không tồn tại"), kiểm trước bước 404 tài khoản. Vai trò mới có hiệu lực ở lần làm mới phiên kế tiếp (tối đa 15 phút).
- **AC-9**: `DELETE /api/v1/accounts/{maNguoiDung}/roles/{vaiTro}?phienBan=<n>` (chỉ Admin, xem AC-15) xóa dòng bảng con bằng `DELETE` SQL thuần rồi flush, tăng `phien_ban` như AC-8, trả 200 `message` "Gỡ vai trò thành công", `data` `TaiKhoanChiTietResponse` mới, nhật ký `AUTH_GO_VAI_TRO`. Sau khi `{vaiTro}` hợp lệ (400) và tài khoản tồn tại (404 "Không tìm thấy tài khoản"), kiểm theo thứ tự, lỗi đầu tiên thắng:
  1. Tài khoản không có vai trò đó: 404 `NOT_FOUND` "Tài khoản không có vai trò này".
  2. `phienBan` lệch: 409 `CONCURRENT_MODIFICATION` như AC-7.
  3. Đó là vai trò cuối cùng: 422 `AUTH_LAST_ROLE` "Tài khoản phải có ít nhất một vai trò. Nếu người này không còn dùng hệ thống, hãy chuyển sang Ngừng hoạt động."
  4. Gỡ `QUAN_TRI_VIEN` của chính mình: 422 `AUTH_SELF_PROTECTED` "Bạn không thể tự gỡ vai trò quản trị hoặc tự khóa tài khoản của mình."
  5. Gỡ `QUAN_TRI_VIEN` khiến không còn Admin đang hoạt động: 422 `AUTH_LAST_ADMIN` "Hệ thống phải luôn còn ít nhất một quản trị viên đang hoạt động."
  6. Dòng bảng con đang bị bảng khác tham chiếu (DB báo SQLSTATE `23503`, đọc từ `PSQLException` trong chuỗi cause, service ném `ApiException`; transaction rollback nên không có thay đổi nào được giữ, kể cả `phien_ban`): 409 `AUTH_ROLE_IN_USE` "Vai trò này đang gắn với dữ liệu khác nên không gỡ được. Nếu người này không còn làm việc, hãy chuyển tài khoản sang Ngừng hoạt động."
  Gỡ vai trò không thu hồi phiên; vai trò mất khỏi token ở lần làm mới kế tiếp (tối đa 15 phút).

*Trạng thái và đặt lại mật khẩu*

- **AC-10**: `PUT /api/v1/accounts/{maNguoiDung}/status` với `{ trangThai, lyDo }` trả 200 `message` "Cập nhật trạng thái thành công", `data` `TaiKhoanChiTietResponse` mới. `trangThai` là một trong ba giá trị, sai thì 400 ("Trạng thái không tồn tại"). `lyDo` cắt khoảng trắng, tối đa 500 ký tự ("Lý do tối đa 500 ký tự"), bắt buộc khi `trangThai` khác `Hoạt động` ("Vui lòng nhập lý do"), không bắt buộc khi mở lại. Trạng thái mới bằng trạng thái hiện tại thì 200 không đổi gì, không ghi nhật ký. Chuyển giữa các trạng thái:
  - Sang `Tạm khóa` hoặc `Ngừng hoạt động`: cập nhật trạng thái, thu hồi mọi họ phiên còn hiệu lực của tài khoản với lý do `Tài khoản bị khóa` (mỗi họ dưới khóa của nó, như `GhiMatKhauMoiService`), nhật ký `AUTH_KHOA_TAI_KHOAN`. Sau đó: đăng nhập trả 403 `AUTH_ACCOUNT_LOCKED`, làm mới trả 401 `AUTH_REFRESH_INVALID`, `GET /auth/profile` trả 403 và xóa cookie (spec 0004 AC-9). Access token đã cấp còn dùng được với các API khác tới khi hết hạn (tối đa 15 phút).
  - Sang `Hoạt động`: cập nhật trạng thái, đặt `so_lan_dang_nhap_sai = 0` và `khoa_tam_den = NULL`, nhật ký `AUTH_MO_KHOA_TAI_KHOAN`.
  - Rời `Ngừng hoạt động` (sang `Hoạt động` hoặc `Tạm khóa`) chỉ người có `QUAN_TRI_VIEN` làm được (AC-15). Từ `Ngừng hoạt động` sang `Tạm khóa` không có phiên nào để thu hồi, nhật ký vẫn là `AUTH_KHOA_TAI_KHOAN` với `soPhienThuHoi` 0.
  - Mọi lần đổi trạng thái tăng `phien_ban`, không cần gửi `phienBan` (lệnh một trường, ghi đè là đúng ý). Ghi bằng một câu `UPDATE ... SET trang_thai_tai_khoan = ?, phien_ban = phien_ban + 1, cap_nhat_luc = ? ...` trên dòng đã khóa (AC-15), không qua `save` của entity, nên không bao giờ sinh `OptimisticLockingFailureException`.
  - Thứ tự trong transaction: (advisory lock nếu cần, AC-11) → khóa dòng tài khoản đích → ghi trạng thái → khóa và thu hồi từng họ phiên. `LamMoiPhienService` đã đọc lại trạng thái dưới khóa họ phiên, nên một lần làm mới chạy song song hoặc chờ khóa họ rồi thấy họ đã thu hồi, hoặc chạy xong trước và họ đó bị thu hồi ngay sau; không có token mới nào sống sót sau commit.
- **AC-11**: Tự bảo vệ và giữ Admin cuối cùng. Người quản lý đổi trạng thái của chính mình sang khác `Hoạt động` thì 422 `AUTH_SELF_PROTECTED` (câu ở AC-9). Đổi trạng thái của một Admin đang hoạt động sang khác `Hoạt động`, hoặc gỡ `QUAN_TRI_VIEN` của một Admin đang hoạt động, mà sau đó không còn Admin đang hoạt động nào thì 422 `AUTH_LAST_ADMIN`. Hai thao tác như vậy chạy cùng lúc (Admin A gỡ quyền B trong khi B gỡ quyền A) không bao giờ để lại 0 Admin đang hoạt động: transaction có thể làm giảm số Admin đang hoạt động (khóa hoặc ngừng một tài khoản có `QUAN_TRI_VIEN`, gỡ `QUAN_TRI_VIEN`) lấy `pg_advisory_xact_lock(<hằng KHOA_ADMIN>)` **trước** khi khóa dòng đích và đọc trạng thái của nó, rồi đếm Admin đang hoạt động **trừ tài khoản đích**; kết quả phải từ 1 trở lên. Thao tác trên tài khoản không có `QUAN_TRI_VIEN`, mở lại, và đặt lại mật khẩu không lấy khóa này. "Đang hoạt động" chỉ xét `trang_thai_tai_khoan`, không xét `khoa_tam_den`.
- **AC-12**: `POST /api/v1/accounts/{maNguoiDung}/reset-password` (không có body) trả 200 `message` "Đặt lại mật khẩu thành công", `data` `{ matKhauTam }`, header `Cache-Control: no-store`. Mật khẩu tạm sinh theo AC-2; `mat_khau_hash` thay bằng hash mới, `phai_doi_mat_khau` `true`, `doi_mat_khau_luc` bằng giờ của `Clock`, `phien_ban` tăng 1, `cap_nhat_luc` cập nhật (một câu `UPDATE` trên dòng đã khóa, như AC-10); thu hồi mọi họ phiên còn hiệu lực với lý do `Đặt lại mật khẩu`; nhật ký `AUTH_DAT_LAI_MAT_KHAU`. Đặt lại mật khẩu của chính mình trả 422 `AUTH_SELF_PROTECTED` "Bạn không thể tự đặt lại mật khẩu của mình. Hãy dùng trang Đổi mật khẩu." (câu riêng cho nhánh này); giao diện ẩn nút này trên tài khoản của chính mình. Làm được với tài khoản ở bất kỳ trạng thái nào; tài khoản đang khóa vẫn không đăng nhập được cho tới khi mở lại.

*Bắt đổi mật khẩu lần đầu*

- **AC-13**: Access token có claim boolean `phaiDoiMatKhau` (hằng `JwtConfig.CLAIM_PHAI_DOI_MAT_KHAU`), đọc từ DB mỗi lần cấp. `AccessTokenService.cap` nhận thêm tham số này; mọi nơi gọi truyền giá trị đọc từ DB: `DangNhapService`, `LamMoiPhienService` (cả nhánh xoay vòng lẫn nhánh khoan dung trả lại token đang sống), và đổi mật khẩu (đọc lại sau khi ghi, không giả định `false`). `DangNhapResponse`, `LamMoiPhienResponse` và `HoSoResponse` có thêm trường `phaiDoiMatKhau`. Khi claim là `true`, mọi handler không mang `@CongKhai` và không mang annotation đánh dấu `@ChoPhepKhiPhaiDoiMatKhau` trả 403 `AUTH_PASSWORD_CHANGE_REQUIRED` "Bạn cần đổi mật khẩu tạm trước khi tiếp tục." Hai handler mang annotation đánh dấu: `GET /api/v1/auth/profile` và `PUT /api/v1/auth/change-password`; đăng xuất và làm mới đã là `@CongKhai`. Token thiếu claim (cấp trước khi có tính năng này) hoặc claim không phải boolean coi như `false`. `PhaiDoiMatKhauInterceptor` đọc claim từ principal `Jwt` (không qua `VaiTroTrongToken`), đăng ký trong `PhanQuyenConfig` trên `/api/**` với `order` nhỏ hơn `ChoPhepInterceptor`; nó cho qua handler không phải `HandlerMethod` và request chưa xác thực (để 401 của Spring Security vẫn thắng). `PhanQuyenCoverageTest` chấp nhận `@ChoPhepKhiPhaiDoiMatKhau` đứng cùng `@DaDangNhap` (annotation đánh dấu không tính là annotation phân quyền thứ hai). Đổi mật khẩu thành công đặt `phai_doi_mat_khau = false` trong cùng câu `UPDATE` với việc thay hash (`doiMatKhauHash`, vẫn tăng `phien_ban` như hiện tại), và response đặt lại riêng cookie `access_token` qua một hàm mới của `AuthCookies` (cùng `sid`, vai trò đọc lại từ DB, claim `false`, `Max-Age` bằng thời hạn token mới), không đụng cookie `refresh_token` và `phien`; `DoiMatKhauResponse` giữ nguyên. Request kế tiếp chạy được ngay mà không phải đăng nhập lại.
- **AC-14**: Giao diện: đăng nhập thành công với `phaiDoiMatKhau` `true` thì chuyển tới `/doi-mat-khau?next=<next đã qua safeNext>` thay vì `next`. Trong khung app, hồ sơ có `phaiDoiMatKhau` `true` mà đường dẫn khác `/doi-mat-khau` thì thay trang (`router.replace`) sang `/doi-mat-khau`; mọi `ApiError` có `errorCode` `AUTH_PASSWORD_CHANGE_REQUIRED` cũng chuyển như vậy. Trang `/doi-mat-khau` khi đang bị bắt đổi hiện thông báo "Bạn đang dùng mật khẩu tạm. Hãy đặt mật khẩu mới để tiếp tục." ở đầu form, gắn với form qua `aria-describedby`. Khi đang bị bắt đổi, khung app ẩn menu điều hướng nhưng vẫn giữ menu người dùng có nút Đăng xuất; trang `/doi-mat-khau` và lời gọi `/auth/profile` không bao giờ bị chuyển trang, nên không có vòng lặp. Đổi xong thì làm mới cache hồ sơ và chuyển tới `next` qua `safeNext` (hoặc `/`). Khi cờ là `false`, `/doi-mat-khau` chạy như hiện tại (không có thông báo, không chuyển trang).

*Phân quyền và nhật ký*

- **AC-15**: Mọi endpoint dưới `/api/v1/accounts` mang `@ChoPhep({QUAN_TRI_VIEN, BAN_QUAN_LY})`; vai trò khác nhận 403 `FORBIDDEN`. Người chỉ có `BAN_QUAN_LY`:
  - danh sách và chi tiết chỉ gồm tài khoản cư dân thuần; tài khoản khác trả 404 ở mọi endpoint có `{maNguoiDung}` (AC-6);
  - tạo tài khoản với `vaiTro` khác đúng một phần tử `CU_DAN`: 403 `FORBIDDEN`;
  - `PUT .../roles/{vaiTro}` chỉ được với `vaiTro` `CU_DAN` và chỉ để sửa `soCccd` của tài khoản cư dân thuần (tài khoản đã có `CU_DAN`, nên không bao giờ là thêm); `vaiTro` khác: 403 `FORBIDDEN`, kiểm sau bước 400 `{vaiTro}` và 404 phạm vi;
  - `DELETE .../roles/{vaiTro}`: 403 `FORBIDDEN` với mọi `vaiTro` (gỡ vai trò chỉ Admin), kiểm sau bước 400 và 404 phạm vi;
  - đổi trạng thái của tài khoản đang `Ngừng hoạt động`: 403 `FORBIDDEN`; mọi chuyển trạng thái khác, sửa thông tin chung, sửa `soCccd`, đặt lại mật khẩu thì được.
  BQL không bao giờ thấy tài khoản của chính mình (có dòng `nhan_vien_bql`), nên luật tự bảo vệ chỉ chạm tới Admin. Phạm vi được quyết định trong `TaiKhoanPhamViService` (một chỗ duy nhất), đọc vai trò người quản lý từ token và vai trò tài khoản đích từ DB. Mọi thao tác ghi khóa dòng `nguoi_dung_he_thong` của tài khoản đích (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`) **trước** khi kiểm phạm vi, và giữ khóa tới hết transaction; mọi thao tác thêm hoặc gỡ vai trò lấy cùng khóa đó. Nhờ vậy Admin không thể thêm vai trò nhân viên vào tài khoản giữa lúc BQL kiểm phạm vi và lúc BQL khóa hay đặt lại mật khẩu. Thứ tự khóa cố định: advisory lock Admin (nếu cần) → dòng tài khoản → họ phiên.
- **AC-16**: Mỗi thao tác thay đổi thành công ghi đúng một dòng nhật ký qua `NhatKyService.ghi`, sau commit: `ma_nguoi_dung` là người quản lý (`sub`), `doi_tuong_tac_dong` `NguoiDungHeThong:<maNguoiDung đích>`. Thao tác không đổi gì (AC-7, AC-8, AC-10) và thao tác thất bại không ghi dòng nào. Enum `LoaiThaoTac` thêm tám giá trị và chi tiết tương ứng:
  | Mã | Nhãn | Chi tiết |
  |---|---|---|
  | `AUTH_TAO_TAI_KHOAN` | Tạo tài khoản | `{ vaiTro: [mã] }` |
  | `AUTH_CAP_NHAT_TAI_KHOAN` | Cập nhật thông tin tài khoản | `{ truongThayDoi: [tên trường] }` |
  | `AUTH_GAN_VAI_TRO` | Gán vai trò | `{ vaiTro }` |
  | `AUTH_CAP_NHAT_VAI_TRO` | Cập nhật vai trò | `{ vaiTro, truongThayDoi: [tên trường] }` |
  | `AUTH_GO_VAI_TRO` | Gỡ vai trò | `{ vaiTro }` |
  | `AUTH_KHOA_TAI_KHOAN` | Khóa tài khoản | `{ trangThaiCu, trangThaiMoi, lyDo, soPhienThuHoi }` |
  | `AUTH_MO_KHOA_TAI_KHOAN` | Mở khóa tài khoản | `{ trangThaiCu, trangThaiMoi, lyDo }` (`lyDo` có thể `null`) |
  | `AUTH_DAT_LAI_MAT_KHAU` | Đặt lại mật khẩu | `{ soPhienThuHoi }` |
  `truongThayDoi` chỉ là tên trường (`hoTen`, `soDienThoai`, `email`, `chucVu`, `soCccd`, ...), không bao giờ có giá trị cũ hay mới; chi tiết không bao giờ chứa mật khẩu tạm, số CCCD, số điện thoại hay email. Mọi record chi tiết qua được `ChiTietNhatKyAnToanTest`. Đối tượng dùng `DoiTuongTacDong.nguoiDung(<mã đích>)` có sẵn. Test của spec 0007 kiểm số giá trị `LoaiThaoTac` (nếu có) được sửa ngay trong lát build thêm enum, không chờ `/sync`.

*Giao diện và migration*

- **AC-17**: Ba màn hình, mục `/tai-khoan` của `MUC_CHUC_NANG` bật `sanSang: true` (icon `UsersRound`, mô tả "Tạo, tìm, khóa tài khoản và đặt lại mật khẩu"), vai trò giữ `QUAN_TRI_VIEN`, `BAN_QUAN_LY`; trang con được chặn theo cùng mục (khớp theo đoạn):
  - `/tai-khoan`: ô tìm (có nhãn, gửi sau 300 ms ngừng gõ hoặc khi nhấn Enter), chọn vai trò (BQL không thấy ô này), chọn trạng thái, tất cả nằm trên URL (`q`, `vaiTro`, `trangThai`, `page`) như `/nhat-ky`; `DataTable` cột Họ tên (link tới chi tiết), Số điện thoại, Email, Vai trò (nhãn từ `vai-tro.ts`), Trạng thái (badge, kèm "Chờ đổi mật khẩu" khi `phaiDoiMatKhau`), Đăng nhập gần nhất; nút "Tạo tài khoản"; trạng thái đang tải, rỗng ("Không có tài khoản nào khớp bộ lọc"), lỗi có nút thử lại.
  - BQL trên trang chi tiết chỉ thấy nút Sửa ở dòng Cư dân (sửa số CCCD), không có Thêm vai trò hay Gỡ.
  - `/tai-khoan/moi`: phần thông tin chung; phần vai trò là nhóm checkbox (Admin thấy bảy, BQL chỉ thấy "Cư dân" đã chọn sẵn và không bỏ được), tick vai trò nào thì hiện ô của vai trò đó. Lỗi 400 và 409 gắn vào đúng ô (`aria-invalid`, `aria-describedby`). Chọn "Quản trị viên" thì hỏi xác nhận trước khi gửi. Tạo xong mở hộp thoại mật khẩu tạm.
  - `/tai-khoan/[maNguoiDung]`: thẻ Thông tin chung (sửa tại chỗ, gửi `phienBan`), thẻ Vai trò (mỗi vai trò một dòng có Sửa và Gỡ, nút Thêm vai trò mở form các vai trò chưa có), thẻ Trạng thái và bảo mật (trạng thái hiện tại, nút Tạm khóa, Ngừng hoạt động, Mở lại, mỗi nút mở hộp thoại có ô lý do; nút Đặt lại mật khẩu có hộp thoại xác nhận). Nút nào backend chắc chắn từ chối (tự khóa mình, BQL đổi trạng thái tài khoản `Ngừng hoạt động`, gỡ vai trò cuối) thì ẩn; backend vẫn là nơi quyết định. 404 hiện `ErrorState` "Không tìm thấy tài khoản".
  - Hộp thoại mật khẩu tạm: hiện mật khẩu bằng chữ đơn cách, nút "Sao chép" (thông báo "Đã sao chép" qua vùng `aria-live`), cảnh báo "Mật khẩu chỉ hiện một lần. Hãy giao cho người dùng trước khi đóng.", nút "Tôi đã lưu mật khẩu" để đóng; mật khẩu không được giữ trong cache TanStack Query hay URL: gọi `mutateAsync`, chép `matKhauTam` vào `useState` của hộp thoại, mutation đặt `gcTime: 0` và không dùng `data` của mutation để hiển thị; xóa state khi đóng hộp thoại. Không log body response ở frontend. Ví dụ OpenAPI cho `matKhauTam` là chuỗi giả rõ ràng (`"XXXXXXXXXXXX"`). Record `TaoTaiKhoanResponse` và `DatLaiMatKhauResponse` ghi đè `toString()` để không in mật khẩu.
  - `CONCURRENT_MODIFICATION` hiện toast "Tài khoản vừa được thay đổi ở nơi khác, đã tải lại dữ liệu mới nhất." và tải lại chi tiết. Mọi lỗi xử lý theo `errorCode`. Dùng được hoàn toàn bằng bàn phím, focus về nút đã mở khi đóng hộp thoại, đạt WCAG AA.
- **AC-18**: `V12__quan_ly_tai_khoan.sql` thêm `phai_doi_mat_khau BOOLEAN NOT NULL DEFAULT false` vào `nguoi_dung_he_thong` và chạy `CREATE EXTENSION IF NOT EXISTS unaccent`. Seed lặp lại `R__seed_01_xac_thuc.sql` không ghi cột này (danh sách cột tường minh không có `phai_doi_mat_khau`), nên tài khoản seed giữ `false` qua mọi lần chạy lại. Entity `NguoiDungHeThong` map thêm các cột lệnh mới cần đọc (`phaiDoiMatKhau`, và `trangThaiTaiKhoan` đã có); các cột chỉ được ghi bằng `UPDATE` thuần không cần setter. Hibernate `validate` khởi động được; `SchemaMigrationTest` kiểm cột và extension, chạy với chính user DB của Testcontainers (kiểm `CREATE EXTENSION` có đủ quyền; ở `docker-compose.prod.yml` user là chủ DB nên cũng đủ).

## Decision

**Chosen option**: Option 1: người quản lý tạo tài khoản, hệ thống sinh mật khẩu tạm và bắt đổi lần đầu, mỗi vai trò một endpoint.

Thêm vào phân hệ `auth` (không tạo package mới): `TaiKhoanController` cho `/api/v1/accounts`, các service theo từng việc (`TaoTaiKhoanService`, `TaiKhoanTraCuuService`, `CapNhatTaiKhoanService`, `VaiTroTaiKhoanService`, `TrangThaiTaiKhoanService`, `DatLaiMatKhauService`), `TaiKhoanPhamViService` giữ luật phạm vi BQL, `MatKhauTamGenerator`, và `ThuHoiPhienService` tách từ `GhiMatKhauMoiService` để ba nơi (đổi mật khẩu, khóa, đặt lại) cùng thu hồi phiên một cách. Bắt đổi mật khẩu đi qua claim `phaiDoiMatKhau` và một interceptor mới. Mã lỗi mới có tiền tố `AUTH_`. Migration `V12`.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`), theo quy ước controller, JPA và cấu hình; riêng lỗi theo vỏ bọc của spec 0001 · `ui-styling` (`.claude/skills/ui-styling/`), form, hộp thoại, bảng bằng shadcn/ui · `tanstack-table` (`.claude/skills/tanstack-table/`), nếu `DataTable` cần thêm cột hay hành vi.

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

**Data model sketch** (một migration `V12__quan_ly_tai_khoan.sql`; mọi bảng khác giữ nguyên):

| Bảng | Thay đổi | Ghi chú |
|---|---|---|
| `nguoi_dung_he_thong` (PK `ma_nguoi_dung`) | **thêm** `phai_doi_mat_khau BOOLEAN NOT NULL DEFAULT false` | `true` khi tạo và khi đặt lại; `false` khi đổi mật khẩu thành công. Entity `NguoiDungHeThong` thêm field `phaiDoiMatKhau`. |
| `nguoi_dung_he_thong` | giữ | `ho_ten` (100, NOT NULL, collation `vi-VN-x-icu`), `so_dien_thoai` (UNIQUE, CHECK `^0[0-9]{9}$`), `email` (UNIQUE, NOT NULL, CHECK chữ thường), `trang_thai_tai_khoan` (3 giá trị), `so_lan_dang_nhap_sai`, `khoa_tam_den`, `doi_mat_khau_luc`, `tao_luc`, `cap_nhat_luc`, `phien_ban` (`@Version` có sẵn) |
| 7 bảng con vai trò | giữ | Quan hệ 1:1 với `nguoi_dung_he_thong` qua `ma_nguoi_dung` UNIQUE; một tài khoản có 0 tới 7 dòng bảng con (luật nghiệp vụ bắt ít nhất 1). Mã bảng con sinh từ sequence riêng; gỡ rồi thêm lại thì có mã mới. `cu_dan.so_cccd` UNIQUE, CHECK 12 chữ số. Khoảng 20 khóa ngoại từ các phân hệ khác trỏ vào các bảng này với `ON DELETE RESTRICT` hoặc mặc định, nên xóa dòng đang bị tham chiếu bị DB từ chối (mã SQLSTATE `23503`). |
| `phien_dang_nhap` | giữ | Lý do thu hồi `Tài khoản bị khóa` và `Đặt lại mật khẩu` đã có trong CHECK. |
| `nhat_ky_he_thong` | giữ | Loại mới chỉ thêm vào enum Java (CHECK chỉ là mẫu chữ). |
| extension | **thêm** `unaccent` | Có sẵn trong PostgreSQL 18; quy tắc mặc định đổi `đ`, `Đ` thành `d`, `D`. Không tạo index (vài nghìn tài khoản, quét tuần tự đủ nhanh). |

**VaiTroRequest** (phần tử của body tạo: có `vaiTro` cộng các trường dưới). **VaiTroCapNhatRequest** (body `PUT .../roles/{vaiTro}`: `phienBan` cộng các trường dưới, không có `vaiTro`). Hai record dùng chung một hàm kiểm trường theo vai trò; trường không thuộc vai trò phải vắng hoặc `null`. Cột `maVaiTro` là khóa chính của bảng con:

| `vaiTro` | Bảng con | `maVaiTro` từ cột | Trường bắt buộc (nhãn, độ dài tối đa) |
|---|---|---|---|
| `QUAN_TRI_VIEN` | `quan_tri_vien` | `ma_quan_tri_vien` | không có |
| `BAN_QUAN_LY` | `nhan_vien_bql` | `ma_nv` | `chucVu` (Chức vụ, 100), `boPhan` (Bộ phận, 100) |
| `BAN_QUAN_TRI` | `thanh_vien_ban_quan_tri` | `ma_thanh_vien` | `nhiemKy` (Nhiệm kỳ, 50), `vaiTroBqt` (Vai trò trong ban quản trị, 100), lưu vào cột `vai_tro` |
| `KE_TOAN` | `ke_toan` | `ma_nv` | `phamViPhuTrach` (Phạm vi phụ trách, 150) |
| `KY_THUAT` | `nhan_vien_ky_thuat` | `ma_nv` | `chuyenMonKyThuat` (Chuyên môn kỹ thuật, 150) |
| `BAO_VE` | `nhan_vien_bao_ve` | `ma_nv` | `viTriTruc` (Vị trí trực, 150) |
| `CU_DAN` | `cu_dan` | `ma_cu_dan` | `soCccd` (Số CCCD, đúng 12 chữ số) |

Trường của `BAN_QUAN_TRI` đặt tên `vaiTroBqt` trong JSON để không đụng với trường `vaiTro` (mã vai trò) của cùng record. `VaiTroTaiKhoan` trong response là **một record phẳng** `{ vaiTro, maVaiTro, chucVu, boPhan, nhiemKy, vaiTroBqt, phamViPhuTrach, chuyenMonKyThuat, viTriTruc, soCccd }`, trường không thuộc vai trò là `null` (không dùng `oneOf`), để `pnpm gen:api` sinh một kiểu đơn giản.

**State transitions** (`trang_thai_tai_khoan`):

```
Hoạt động  ──khóa (lyDo)──▶  Tạm khóa
Hoạt động  ──ngừng (lyDo)──▶ Ngừng hoạt động
Tạm khóa   ──ngừng (lyDo)──▶ Ngừng hoạt động
Tạm khóa   ──mở lại──▶       Hoạt động          (Admin hoặc BQL trong phạm vi)
Ngừng hoạt động ──mở lại──▶  Hoạt động          (chỉ Admin)
Ngừng hoạt động ──khóa──▶    Tạm khóa           (chỉ Admin)
```

Mọi mũi tên sang `Tạm khóa` hoặc `Ngừng hoạt động` thu hồi phiên. Không có xóa cứng. `phai_doi_mat_khau` độc lập với trạng thái: `false` → `true` khi tạo hoặc đặt lại, `true` → `false` khi đổi mật khẩu thành công.

**API surface** (mọi endpoint `@ChoPhep({QUAN_TRI_VIEN, BAN_QUAN_LY})`, trừ hai endpoint `auth` có sẵn; thành công bọc bằng `ApiResponse`):

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/accounts` | GET | `q`, `vaiTro`, `trangThai`, `page`, `size`, `sort` (đều không bắt buộc) | `PageResponse<TaiKhoanTomTat>` | Admin, BQL | 400 `VALIDATION_FAILED` |
| `/api/v1/accounts` | POST | `hoTen` (req), `soDienThoai` (req), `email` (req), `vaiTro: VaiTroRequest[]` (req, 1..7) | 201 `{ taiKhoan, matKhauTam }`, `no-store` | Admin, BQL (chỉ `[CU_DAN]`) | 400, 403 `FORBIDDEN`, 409 `AUTH_IDENTITY_TAKEN` |
| `/api/v1/accounts/{maNguoiDung}` | GET | path | `TaiKhoanChiTietResponse` | Admin, BQL (phạm vi) | 404 `NOT_FOUND` |
| `/api/v1/accounts/{maNguoiDung}` | PUT | `hoTen`, `soDienThoai`, `email`, `phienBan` (đều req) | `TaiKhoanChiTietResponse` | Admin, BQL (phạm vi) | 400, 404, 409 `AUTH_IDENTITY_TAKEN`, 409 `CONCURRENT_MODIFICATION` |
| `/api/v1/accounts/{maNguoiDung}/roles/{vaiTro}` | PUT | `phienBan` (req), trường của vai trò | `TaiKhoanChiTietResponse` | Admin, BQL (chỉ sửa `CU_DAN`) | 400, 403, 404, 409 `AUTH_IDENTITY_TAKEN`, 409 `CONCURRENT_MODIFICATION` |
| `/api/v1/accounts/{maNguoiDung}/roles/{vaiTro}` | DELETE | `phienBan` (query, req) | `TaiKhoanChiTietResponse` | Admin (BQL nhận 403) | 403, 404, 409 `CONCURRENT_MODIFICATION`, 409 `AUTH_ROLE_IN_USE`, 422 `AUTH_LAST_ROLE`, `AUTH_SELF_PROTECTED`, `AUTH_LAST_ADMIN` |
| `/api/v1/accounts/{maNguoiDung}/status` | PUT | `trangThai` (req), `lyDo` (req khi khác `Hoạt động`) | `TaiKhoanChiTietResponse` | Admin, BQL (phạm vi, không rời `Ngừng hoạt động`) | 400, 403, 404, 422 `AUTH_SELF_PROTECTED`, `AUTH_LAST_ADMIN` |
| `/api/v1/accounts/{maNguoiDung}/reset-password` | POST | không | `{ matKhauTam }`, `no-store` | Admin, BQL (phạm vi) | 404, 422 `AUTH_SELF_PROTECTED` |
| `/api/v1/auth/login`, `/refresh-token`, `/profile` | có sẵn | không đổi | thêm `phaiDoiMatKhau` | có sẵn | có sẵn |
| `/api/v1/auth/change-password` | PUT (có sẵn) | không đổi | thêm cookie `access_token` mới | `@DaDangNhap` + `@ChoPhepKhiPhaiDoiMatKhau` | có sẵn |
| mọi API khác không `@CongKhai` | | | | | 403 `AUTH_PASSWORD_CHANGE_REQUIRED` khi claim `true` |

Thứ tự kiểm chung cho endpoint có `{maNguoiDung}` (AC nào ghi thứ tự riêng thì theo AC đó, nhưng không trái thứ tự này): bắt đổi mật khẩu (`PhaiDoiMatKhauInterceptor`, 403) → quyền theo `@ChoPhep` (interceptor, 403) → bean validation body, tham số query và `{vaiTro}` trên path: 400 → khóa dòng đích, rồi mã sai dạng, không tồn tại hoặc ngoài phạm vi: 404 → quyền chi tiết theo vai trò của người quản lý (403) → luật nghiệp vụ (404 vai trò không có, 409, 422) theo thứ tự ghi ở từng AC. `ApiResponse.created` và `ok` được mở rộng (hoặc controller dùng `ResponseEntity` builder) để thêm header `Cache-Control: no-store`.

Mã lỗi mới trong `AuthErrorCodes`: `AUTH_IDENTITY_TAKEN` (409), `AUTH_ROLE_IN_USE` (409), `AUTH_LAST_ROLE` (422), `AUTH_SELF_PROTECTED` (422), `AUTH_LAST_ADMIN` (422), `AUTH_PASSWORD_CHANGE_REQUIRED` (403). Câu thông báo trùng với frontend như các mã hiện có.

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| Tạo | `maNguoiDung`, mã bảng con | DEFAULT từ sequence của từng bảng (spec 0002) |
| Tạo | `matKhauTam` | `MatKhauTamGenerator` (AC-2), `SecureRandom` |
| Tạo, đặt lại | `mat_khau_hash` | `PasswordEncoder` có sẵn (`{bcrypt}`) |
| Tạo | `trang_thai_tai_khoan`, `phai_doi_mat_khau`, `tao_luc`, `cap_nhat_luc` | hằng `Hoạt động`, `true`; giờ của bean `Clock` (không dùng DEFAULT `now()` để test điều khiển được) |
| Mọi thao tác | người quản lý và vai trò của họ | `sub` và claim `vaiTro` của access token |
| Mọi thao tác | vai trò của tài khoản đích | `VaiTroQuery.cua` (đọc bảng con, có sẵn) |
| Phạm vi BQL | "cư dân thuần" | derived: có dòng `cu_dan` và không có dòng ở 6 bảng còn lại |
| Danh sách | `vaiTro` từng dòng | một truy vấn `UNION ALL` trên 7 bảng con cho các `ma_nguoi_dung` của trang |
| Danh sách | `dangNhapCuoiLuc` | cột `dang_nhap_cuoi_luc` (đăng nhập đã ghi, spec 0004) |
| Danh sách, chi tiết | nhãn vai trò, nhãn trạng thái | frontend `vai-tro.ts`; trạng thái là chính chuỗi tiếng Việt trong DB |
| Sửa | `phienBan` | client gửi lại giá trị nhận ở chi tiết; so với `@Version` |
| Sửa vai trò, gỡ vai trò | tăng `phien_ban` | cập nhật `cap_nhat_luc` qua entity `NguoiDungHeThong` để `@Version` tăng |
| Khóa, đặt lại | `soPhienThuHoi` | giá trị trả về của `ThuHoiPhienService` |
| Đặt lại | `doi_mat_khau_luc` | giờ của bean `Clock`, cắt tới micro giây |
| Bắt đổi | claim `phaiDoiMatKhau` | cột `phai_doi_mat_khau` đọc lúc cấp token |
| Admin cuối | số Admin đang hoạt động | `COUNT` join `quan_tri_vien` và `nguoi_dung_he_thong` với `trang_thai_tai_khoan = 'Hoạt động'`, chạy sau `pg_advisory_xact_lock(<hằng KHOA_ADMIN>)` |
| Trùng | trường nào trùng | kiểm trước bằng truy vấn tồn tại; dự phòng map tên ràng buộc UNIQUE (`nguoi_dung_he_thong_so_dien_thoai_key`, `nguoi_dung_he_thong_email_key`, `cu_dan_so_cccd_key`, tên thật lấy từ DB khi build) sang tên trường |
| Gỡ vai trò bị chặn | vi phạm khóa ngoại | `DataIntegrityViolationException` có SQLSTATE `23503` khi xóa dòng bảng con |
| Nhật ký | người thực hiện, IP, request id, thời điểm | `NhatKyService.ghi` (spec 0007) |

**Key invariants**:
- Mỗi tài khoản luôn có ít nhất một vai trò (kiểm ở service; tạo cần 1..7, gỡ vai trò cuối bị chặn).
- Luôn còn ít nhất một Admin đang hoạt động; mọi thao tác có thể làm giảm số đó lấy cùng advisory lock rồi mới đếm.
- Người quản lý không tự khóa, tự ngừng, tự gỡ `QUAN_TRI_VIEN`, hay tự đặt lại mật khẩu của mình.
- Mọi thao tác ghi khóa dòng tài khoản đích trước khi kiểm phạm vi; thứ tự khóa luôn là advisory lock Admin → dòng tài khoản → họ phiên.
- Mật khẩu tạm chỉ tồn tại ở dạng chữ trong một response duy nhất; DB chỉ có hash.
- Tài khoản không ở `Hoạt động` không còn họ phiên nào còn hiệu lực sau khi transaction đổi trạng thái commit.
- Khi `phai_doi_mat_khau` là `true`, token chỉ gọi được hồ sơ, đổi mật khẩu, đăng xuất, làm mới.
- Không xóa cứng `nguoi_dung_he_thong`; không có endpoint xóa tài khoản.
- Hai bước băm bcrypt (tạo, đặt lại) chạy ngoài transaction DB, như `DoiMatKhauService`.

**Security model**:
- Admin (`QUAN_TRI_VIEN`) làm mọi thao tác trên mọi tài khoản, trừ luật tự bảo vệ và Admin cuối.
- BQL (chỉ `BAN_QUAN_LY`) chỉ thấy và thao tác trên tài khoản cư dân thuần, chỉ sửa `soCccd` của vai trò `CU_DAN`, không gỡ vai trò, không đổi trạng thái tài khoản `Ngừng hoạt động`. Tài khoản ngoài phạm vi trả 404, không phải 403, để không lộ sự tồn tại.
- Khoảng hở đã chấp nhận: lỗi 409 `AUTH_IDENTITY_TAKEN` cho BQL biết một số điện thoại hay email đã thuộc một tài khoản nào đó (có thể là nhân viên). BQL cần biết vì sao không lưu được, và câu lỗi không nêu tài khoản nào, vai trò gì; đây là cùng mức lộ mà form đăng ký nào cũng có.
- Vai trò khác: 403 ở mọi endpoint `/accounts`.
- Dữ liệu nhạy cảm: số CCCD (dữ liệu cá nhân theo Nghị định 13/2023 về bảo vệ dữ liệu cá nhân) chỉ có ở chi tiết, không ở danh sách, không ở nhật ký, không ở log. Số điện thoại và email có ở danh sách vì đó là cách tìm người; không vào nhật ký (BQT cũng đọc được nhật ký).
- Log ứng dụng mức INFO cho mỗi thao tác: tên thao tác, `maNguoiThucHien`, `maNguoiDung` đích, `requestId`; không bao giờ có mật khẩu tạm, số CCCD, số điện thoại, email. Mọi record request có mật khẩu hay CCCD ghi đè `toString()`.
- Response chứa `matKhauTam` có `Cache-Control: no-store`.
- Mọi thay đổi quyền truy cập đều có nhật ký kiểm toán (AC-16).
- Khoảng hở đã chấp nhận: access token đã cấp sống tối đa 15 phút sau khi khóa hay gỡ vai trò (spec 0005).

**Configuration required**: không có biến môi trường mới. Độ dài mật khẩu tạm (12) và bảng chữ là hằng trong `MatKhauTamGenerator`.

**Critical test scenarios**:
- Happy path: Admin seed tạo tài khoản `BAN_QUAN_LY` + `CU_DAN`, nhận mật khẩu tạm; người đó đăng nhập, `GET /buildings` bị 403 `AUTH_PASSWORD_CHANGE_REQUIRED`, đổi mật khẩu bằng mật khẩu tạm làm mật khẩu cũ, rồi `GET /buildings` chạy được ngay bằng cookie mới, verifies **AC-1**, **AC-2**, **AC-13**.
- Happy path giao diện: tạo tài khoản ở `/tai-khoan/moi`, sao chép mật khẩu trong hộp thoại, tìm lại bằng "nguyen" không dấu ở `/tai-khoan`, verifies **AC-5**, **AC-17**.
- Failure case: tạo hai tài khoản cùng số điện thoại song song, một bên 201, một bên 409 `AUTH_IDENTITY_TAKEN` (không 500), verifies **AC-3**.
- Failure case: hai Admin gỡ `QUAN_TRI_VIEN` của nhau cùng lúc (hai thread, hai transaction), kết quả còn đúng một Admin đang hoạt động và một bên nhận 422 `AUTH_LAST_ADMIN`, verifies **AC-11**.
- Failure case: gỡ `CU_DAN` của tài khoản có hồ sơ cư trú trỏ vào thì 409 `AUTH_ROLE_IN_USE` và dòng `cu_dan` còn nguyên; gỡ vai trò cuối thì 422 `AUTH_LAST_ROLE`, verifies **AC-9**.
- Failure case: hai lần `PUT /accounts/{ma}` với cùng `phienBan`, lần sau 409 `CONCURRENT_MODIFICATION`, verifies **AC-7**.
- Khóa: đăng nhập, Admin khóa tài khoản đó, làm mới bị 401, `GET /profile` bị 403 và xóa cookie, có dòng `AUTH_KHOA_TAI_KHOAN` với `soPhienThuHoi` đúng; mở lại thì `so_lan_dang_nhap_sai` về 0, verifies **AC-10**.
- Đặt lại: mọi họ phiên bị thu hồi với `Đặt lại mật khẩu`, mật khẩu cũ không đăng nhập được, mật khẩu tạm đăng nhập được và bị bắt đổi, verifies **AC-12**.
- Auth/permission: `0900000002` (BQL) không thấy `0900000006` (BQT + cư dân) trong danh sách, `GET` chi tiết của nó trả 404, tạo tài khoản `KE_TOAN` trả 403, mở lại tài khoản cư dân `Ngừng hoạt động` trả 403; `0900000003` (kỹ thuật) nhận 403 ở mọi endpoint, verifies **AC-15**.
- Nhật ký: chạy mọi thao tác, mỗi thao tác đúng một dòng với đúng loại; quét mọi cột của mọi dòng không chứa mật khẩu tạm, số CCCD, số điện thoại, email đã gửi, verifies **AC-16**, **AC-2**.
- Chạy đua phạm vi BQL: BQL bắt đầu đặt lại mật khẩu một cư dân thuần trong khi Admin thêm `KE_TOAN` cho tài khoản đó (hai thread, `CyclicBarrier`); kết quả hoặc đặt lại xong trước khi vai trò được thêm, hoặc BQL nhận 404, không bao giờ đặt lại được tài khoản đã có `KE_TOAN`, verifies **AC-15**.
- Làm mới chạy đua với khóa: một lần làm mới và một lần khóa chạy song song; sau khi cả hai xong, tài khoản không có họ phiên nào còn hiệu lực, verifies **AC-10**.
- Tự đặt lại mật khẩu: Admin gọi `reset-password` cho chính mình, nhận 422 `AUTH_SELF_PROTECTED`, phiên còn nguyên, verifies **AC-12**.
- Các test song song (trùng số điện thoại, Admin cuối, phạm vi BQL) dùng `CyclicBarrier` để hai transaction thật sự chồng nhau, không dựa vào may rủi thời gian.
- Unit: `MatKhauTamGenerator` sinh 10 000 lần, mọi chuỗi dài 12, đúng bảng chữ, có chữ và số; `TaiKhoanPhamViService` với fake cho mọi tổ hợp vai trò người quản lý và tài khoản đích, verifies **AC-2**, **AC-15**.

## Build plan

Theo Tracer Bullet: lát đầu chạy thật từ DB tới màn hình cho một luồng tạo tài khoản, các lát sau làm dày từng đoạn.

1. **Đường mỏng: Admin tạo tài khoản và thấy nó trong danh sách.** `V12` (cột, extension); field `phaiDoiMatKhau` trong entity; `MatKhauTamGenerator`; `POST /accounts` đủ bảy vai trò với chuẩn hóa và validation (AC-1, AC-4) nhưng chưa có nhánh trùng và chưa có phạm vi BQL; `GET /accounts` tối thiểu (không bộ lọc, sắp `taoLuc,desc`); nhật ký `AUTH_TAO_TAI_KHOAN`; bật mục `/tai-khoan`; trang danh sách đơn giản, trang `/tai-khoan/moi`, hộp thoại mật khẩu tạm. Test tích hợp tạo và đọc lại. Satisfies **AC-1**, **AC-2**, **AC-4**, **AC-16** (phần tạo), **AC-17** (phần danh sách, tạo, hộp thoại), **AC-18**.
2. **Bắt đổi mật khẩu xuyên suốt.** Claim `phaiDoiMatKhau` khi đăng nhập và làm mới; trường trong ba response; `@ChoPhepKhiPhaiDoiMatKhau` và `PhaiDoiMatKhauInterceptor` (đăng ký trước `ChoPhepInterceptor`); đổi mật khẩu xóa cờ và đặt lại cookie access token; frontend chuyển trang ở trang đăng nhập, khung app và khi gặp `AUTH_PASSWORD_CHANGE_REQUIRED`; thông báo trên `/doi-mat-khau`. Satisfies **AC-13**, **AC-14**.
3. **Chi tiết, sửa thông tin và vai trò.** `GET /accounts/{ma}`, `PUT /accounts/{ma}` với `phienBan`, `PUT` và `DELETE` `.../roles/{vaiTro}`; kiểm trùng trước và map ràng buộc UNIQUE (cho cả tạo); luật vai trò cuối và khóa ngoại; nhật ký ba loại vai trò và cập nhật; trang `/tai-khoan/[ma]` thẻ Thông tin chung và Vai trò, lỗi gắn ô, xử lý `CONCURRENT_MODIFICATION`. Satisfies **AC-3**, **AC-6**, **AC-7**, **AC-8**, **AC-9** (trừ luật Admin), **AC-16**.
4. **Trạng thái và đặt lại mật khẩu.** Tách `ThuHoiPhienService` từ `GhiMatKhauMoiService` (đổi mật khẩu dùng lại, test cũ vẫn xanh); `PUT .../status` với thu hồi phiên và đặt lại bộ đếm; `POST .../reset-password`; luật tự bảo vệ (gồm cấm tự đặt lại mật khẩu) và Admin cuối với advisory lock (cho cả gỡ `QUAN_TRI_VIEN` ở bước 3); khóa dòng tài khoản đích ở mọi thao tác ghi; test chạy đua làm mới với khóa; thẻ Trạng thái và bảo mật với các hộp thoại. Satisfies **AC-9** (luật Admin), **AC-10**, **AC-11**, **AC-12**, **AC-16**.
5. **Phạm vi BQL, tìm kiếm đủ, khóa lại.** `TaiKhoanPhamViService` gắn vào mọi endpoint (sau khóa dòng đích), test chạy đua phạm vi BQL; bộ lọc `q` (unaccent, thoát ký tự), `vaiTro`, `trangThai` và lỗi 400; truy vấn vai trò theo lô (tối đa ba truy vấn); giao diện BQL (ẩn ô vai trò, chỉ checkbox Cư dân); test quét nhật ký; kiểm bàn phím và a11y; `verify.md`; cập nhật README nếu có mục tài khoản seed. Satisfies **AC-5**, **AC-15**, **AC-16**, **AC-17**.

## Consequences

**Positive**:
- Có đường duy nhất, có kiểm toán, để đưa người vào hệ thống; không ai phải sửa DB tay.
- Người quản lý không bao giờ chọn hay giữ mật khẩu của người khác, và mật khẩu tạm không sống lâu vì bị bắt đổi.
- Endpoint mở lại tài khoản đã đặt lại bộ đếm đăng nhập sai, nên tính năng 12 dùng lại được cho "Admin mở khóa sớm".
- `ThuHoiPhienService` dùng chung cho đổi mật khẩu, khóa, đặt lại, và sau này quên mật khẩu (tính năng 13).

**Negative / tradeoffs**:
- Khóa hay gỡ vai trò không cắt access token đang cầm; người bị khóa còn gọi được API tối đa 15 phút. Chấp nhận để không tra DB ở mọi request.
- Mật khẩu tạm đi qua tay người quản lý (giao trực tiếp hoặc qua tin nhắn). Không có hạn dùng, nên mật khẩu tạm bị lộ mà người dùng chưa đổi thì vẫn dùng được; cách xử lý là đặt lại lần nữa.
- Gỡ vai trò đã có dữ liệu trỏ vào thì không làm được; người đã nghỉ phải chuyển sang `Ngừng hoạt động`, nên vai trò cũ vẫn hiện trong chi tiết của họ.
- Gỡ rồi thêm lại vai trò sinh mã bảng con mới; dữ liệu cũ (nếu có) trỏ vào mã cũ đã bị chặn ở bước gỡ, nên không mất liên kết, nhưng mã người đó đổi.
- BQL không tìm thấy cư dân đồng thời là BQT hay nhân viên; những người này do Admin quản lý.
- Thêm một interceptor chạy ở mọi request; nó chỉ đọc claim, không tra DB.
- Claim `phaiDoiMatKhau` có thể cũ tối đa 15 phút theo cả hai chiều: access token cấp trước một lần đặt lại vẫn mang `false` (refresh của nó đã bị thu hồi, nhưng access token sống tới hết hạn), và sau khi đổi mật khẩu ở máy này, token ở máy khác vẫn mang `true` tới khi hết hạn (refresh của chúng cũng đã bị thu hồi). Cùng khoảng hở 15 phút đã chấp nhận ở spec 0005.
- BQL không gỡ được vai trò `CU_DAN` và không thêm được vai trò nào; việc đó luôn qua Admin.
- Mọi thao tác ghi khóa dòng tài khoản đích, nên hai người thao tác cùng một tài khoản chờ nhau (vài chục mili giây); không ảnh hưởng tài khoản khác.
- `HoSoResponse`, `DangNhapResponse`, `LamMoiPhienResponse` đổi hình (thêm trường), nên `openapi.json` và kiểu frontend sinh lại.

**Neutral**:
- Một migration `V12`, một extension PostgreSQL có sẵn.
- `PhanQuyenCoverageTest` không đổi: `@ChoPhepKhiPhaiDoiMatKhau` là annotation đánh dấu bổ sung, không thay ba annotation phân quyền.
- Tám loại nhật ký mới hiện ngay trong bộ lọc của `/nhat-ky` qua `GET /audit-logs/action-types`.

## Follow-up

- [ ] AC-10 của spec 0007 ghi "đúng bốn giá trị" của `LoaiThaoTac`; tính năng 10 đã thêm `AUTH_DOI_MAT_KHAU` và spec này thêm tám giá trị. Cập nhật câu đó (hoặc để `/sync` đánh dấu) khi chốt tính năng này.
- [ ] Spec 0004 (AC-8, mô tả `HoSoResponse`) và spec 0005 (`LamMoiPhienResponse`) có thêm trường `phaiDoiMatKhau`; `/sync` nên ghi nhận.
- [ ] Tính năng 12 (khóa tạm khi đăng nhập sai) dùng lại `PUT /accounts/{ma}/status` sang `Hoạt động` cho "Admin mở khóa sớm" thay vì làm endpoint mới; khi thiết kế, xem lại có cần phân biệt tài khoản đang bị khóa tạm (`khoa_tam_den` trong tương lai) trên màn hình danh sách không.
- [ ] UC02 (cư trú) sẽ gắn tài khoản cư dân với hồ sơ cư trú và căn hộ; khi đó cân nhắc cho BQL tạo tài khoản cư dân ngay từ màn hình hồ sơ cư trú, gọi lại `POST /accounts`.
- [ ] Mục Deferred "Gán nhân viên vào tòa nhà" sẽ thu hẹp phạm vi của BQL theo tòa; khi làm, `TaiKhoanPhamViService` là nơi duy nhất cần sửa.
- [ ] Nếu sau này mật khẩu tạm được gửi qua kênh không an toàn (tin nhắn, email), cân nhắc thêm hạn dùng 72 giờ (cột `mat_khau_tam_het_han_luc`), phương án đã cân nhắc và để lại.
