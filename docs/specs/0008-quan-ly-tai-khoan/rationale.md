# 0008. Quản lý tài khoản (Admin): lý do và các phương án

Bản ghi quyết định cho [index.md](index.md). `/develop` không cần đọc file này.

## Context

Hệ thống không có đăng ký công khai: cư dân, nhân viên và ban quản trị chỉ có tài khoản khi ai đó tạo cho họ. Hiện tài khoản chỉ có từ seed (`R__seed_01_xac_thuc.sql`), nên chưa đưa được người thật vào và chưa chặn được người đã nghỉ. Tính năng 11 của scope là đường duy nhất để có tài khoản, và các tính năng 12 (khóa tạm khi đăng nhập sai) và 13 (quên mật khẩu qua email) dựa vào trạng thái và mật khẩu mà tính năng này quản lý.

Schema đã có sẵn gần đủ (spec 0002): `nguoi_dung_he_thong` với ba trạng thái và `phien_ban`, bảy bảng con vai trò theo kiểu bảng con cho từng loại (TPT, mỗi vai trò một bảng trỏ về bảng gốc), mỗi bảng có trường bắt buộc riêng, và `cu_dan.so_cccd` UNIQUE. Khoảng 20 khóa ngoại từ các phân hệ nghiệp vụ trỏ vào các bảng con, nên xóa một dòng vai trò sẽ bị DB chặn khi đã có dữ liệu. Phiên dùng JWT 15 phút cộng refresh token xoay vòng (spec 0004, 0005): access token không tra DB, nên mọi thay đổi quyền chỉ có hiệu lực đầy đủ ở lần làm mới kế tiếp. Nhật ký kiểm toán có sẵn (spec 0007) và bắt buộc cho mọi thay đổi quyền truy cập.

Các lực đẩy: (1) ai được cấp tài khoản cho ai, vì BQL xác minh cư trú nhưng không nên tự cấp quyền Admin; (2) mật khẩu đầu tiên phải tới tay người dùng khi chưa có kênh email; (3) gỡ vai trò va vào khóa ngoại; (4) không được để hệ thống mất Admin cuối cùng; (5) số CCCD là dữ liệu cá nhân theo Nghị định 13/2023; (6) nhóm hai người, đồ án có hạn, nên mỗi phần phải đơn giản và chạy được.

Nếu không quyết, tính năng 11 không build được, tính năng 12 và 13 không có chỗ cắm, và demo chỉ có tám tài khoản seed.

## Options considered

### Option 1: Người quản lý tạo, hệ thống sinh mật khẩu tạm và bắt đổi lần đầu, mỗi vai trò một endpoint

Admin (và BQL cho cư dân thuần) tạo tài khoản; backend sinh mật khẩu ngẫu nhiên, hiện một lần, đánh dấu `phai_doi_mat_khau`; backend chặn mọi API trừ hồ sơ và đổi mật khẩu cho tới khi đổi xong. Thông tin chung sửa bằng một `PUT`, vai trò thêm, sửa, gỡ bằng endpoint riêng, trạng thái đổi bằng một endpoint lệnh.

**Pros**:
- Người quản lý không chọn và không giữ mật khẩu người khác; mật khẩu tạm bị thay sớm.
- Mỗi lệnh làm một việc, lỗi 409 khi gỡ vai trò bị tham chiếu không làm hỏng việc lưu thông tin chung.
- Không cần hạ tầng mới (email, hàng đợi).

**Cons**:
- Thêm một cột, một claim và một interceptor chạy ở mọi request.
- Mật khẩu tạm đi qua tay người quản lý; không có hạn dùng.
- Màn hình chi tiết có nhiều nút và hộp thoại hơn một form duy nhất.

### Option 2: Một form lớn, Admin tự nhập mật khẩu đầu

`POST` và `PUT /accounts/{ma}` nhận cả thông tin chung lẫn danh sách vai trò đầy đủ; backend so danh sách cũ và mới để thêm, sửa, xóa dòng bảng con. Admin gõ mật khẩu đầu tiên, không bắt đổi.

**Pros**:
- Một nút Lưu, ít endpoint, dễ demo.
- Không phải đụng tới đăng nhập, làm mới, interceptor.

**Cons**:
- Admin hay dùng lại mật khẩu yếu và biết mật khẩu của mọi người.
- So danh sách vai trò ở backend dễ sai; một vai trò bị khóa ngoại chặn làm hỏng cả lần lưu.
- `phienBan` một lần cho cả form lớn dễ gây xung đột giả khi hai người sửa hai phần khác nhau.

### Option 3: Mời qua link (email hoặc mã dùng một lần)

Người quản lý tạo tài khoản không mật khẩu; hệ thống gửi link đặt mật khẩu dùng một lần, dùng bảng `yeu_cau_dat_lai_mat_khau` có sẵn.

**Pros**:
- Không ai ngoài người dùng biết mật khẩu, kể cả mật khẩu tạm.
- Dùng chung cơ chế với tính năng 13.

**Cons**:
- Cần chọn và vận hành dịch vụ gửi email, là quyết định riêng của tính năng 13 chưa có.
- Cư dân lớn tuổi hoặc không dùng email thì kẹt; vẫn phải có đường dự phòng.
- Làm tính năng 11 phụ thuộc vào tính năng 13, đảo thứ tự scope.

## Rationale

Option 1 được chọn vì nó giải được lực (2) mà không phụ thuộc vào tính năng 13: mật khẩu tạm cộng bắt đổi lần đầu giữ được ý "chỉ người dùng biết mật khẩu cuối cùng" ở mức đủ cho đồ án, và khi tính năng 13 có email thì nút đặt lại vẫn là đường dự phòng cho người không có email. Option 3 đúng hơn về bảo mật nhưng kéo theo một quyết định hạ tầng chưa chốt; Option 2 rẻ nhất nhưng để Admin biết mật khẩu của mọi người, và luồng so danh sách vai trò đụng thẳng vào lực (3).

Endpoint riêng cho từng vai trò bám đúng mô hình TPT: mỗi lệnh chạm đúng một bảng con, nên lỗi khóa ngoại khi gỡ (lực 3) thành một 409 rõ nghĩa ở một dòng, không làm hỏng cả form. Luật phạm vi BQL "chỉ tài khoản cư dân thuần" giải lực (1) gọn nhất: BQL không bao giờ đụng tới tài khoản có vai trò khác, nên không cần luật riêng cho từng cặp vai trò. Trả 404 thay vì 403 ngoài phạm vi để BQL không dò được danh sách nhân viên.

Chấp nhận khoảng hở 15 phút sau khi khóa (thay vì tra DB ở mọi request) vì spec 0005 đã chấp nhận đúng khoảng hở đó cho đổi mật khẩu, giao diện gọi `/profile` khi chuyển trang nên thực tế người bị khóa bị đẩy ra gần như ngay, và một truy vấn mỗi request là chi phí thường trực cho một sự kiện hiếm. Advisory lock cho luật Admin cuối (lực 4) là cách rẻ nhất để hai thao tác chạy song song không cùng thấy "vẫn còn một Admin khác".

### Các quyết định nhỏ đã chốt khi viết spec

- **Bảng chữ và độ dài mật khẩu tạm**: 12 ký tự, bỏ `0 O 1 l I`, luôn có chữ và số. Đủ mạnh (khoảng 70 bit), dễ đọc khi giao bằng miệng, và đạt quy tắc độ mạnh có sẵn nên người dùng đổi được ngay. Phương án thua: cụm từ dễ nhớ (passphrase), dài hơn và cần danh sách từ tiếng Việt.
- **Bắt đổi qua claim, không tra DB**: claim `phaiDoiMatKhau` đọc lúc cấp token; interceptor chỉ đọc claim. Phương án thua: tra cột ở mọi request, đúng hơn ngay tức thì nhưng tốn một truy vấn mỗi request.
- **Đổi mật khẩu cấp lại access token**: nếu không, token cũ còn claim `true` thêm tối đa 15 phút và người dùng kẹt ở trang đổi mật khẩu. Phương án thua: frontend tự gọi `/refresh-token` sau khi đổi, thêm một vòng request và một trạng thái chờ.
- **Chuyển trang ở khung app thay vì `proxy.ts`**: `proxy.ts` chỉ đọc cookie dấu `phien` và không giải mã token (spec 0004), nên khung app (đã tải hồ sơ) và bộ xử lý `ApiError` là nơi biết cờ. Phương án thua: đặt thêm một cookie dấu không bí mật cho cờ này để `proxy.ts` đọc, nhanh hơn một nhịp nhưng thêm một cookie phải giữ đồng bộ.
- **Nhật ký chỉ ghi tên trường đổi, không ghi giá trị**: BQT và BQL đọc được nhật ký, nên ghi số điện thoại hay email cũ và mới sẽ lộ thông tin nhân viên cho BQL, trái luật phạm vi. Phương án thua: ghi giá trị cũ và mới cho `hoTen`, hữu ích khi tra lỗi nhưng không đáng để tách luật.
- **`lyDo` khi mở lại không bắt buộc**: mở lại thường là sửa sai hoặc sau khi người dùng liên hệ; bắt lý do chỉ thêm ma sát. Phương án thua: bắt buộc cho cân xứng.
- **Đổi trạng thái không cần `phienBan`**: lệnh một trường, người thứ hai bấm khóa thì kết quả vẫn là khóa. Phương án thua: bắt `phienBan` cho mọi thao tác, an toàn hơn về lý thuyết nhưng gây 409 giả khi một người sửa tên còn người kia khóa.
- **Tìm kiếm bằng `unaccent` không index**: vài nghìn tài khoản, quét tuần tự dưới vài mili giây. Phương án thua: cột sinh sẵn không dấu cộng index trigram, chỉ đáng khi số dòng lên hàng trăm nghìn.
- **Thứ tự kiểm 404 trước 403 khi BQL gọi vai trò khác `CU_DAN`**: để BQL gọi vào tài khoản nhân viên luôn nhận 404 như mọi chỗ khác, không lộ tồn tại qua sự khác nhau giữa 403 và 404.
- **Chốt sau cross check (Sonnet, 2026-11-22)**: (1) cấm tự đặt lại mật khẩu, vì nó thu hồi chính phiên đang hiện mật khẩu tạm; Admin đổi mật khẩu của mình ở `/doi-mat-khau`. (2) BQL chỉ sửa `soCccd` qua endpoint vai trò, không thêm, không gỡ: các nhánh thêm và gỡ của BQL không bao giờ tới được (cư dân thuần đã có `CU_DAN`, gỡ vai trò duy nhất luôn bị chặn) và làm thứ tự kiểm 403/404 mâu thuẫn. (3) Chấp nhận việc 409 trùng thông tin cho BQL biết số điện thoại hay email đã có người dùng. (4) Khóa dòng tài khoản đích trước khi kiểm phạm vi, để Admin không thể đổi vai trò của tài khoản giữa lúc BQL kiểm và lúc BQL ghi (lỗ leo quyền qua đặt lại mật khẩu). (5) Các lệnh một trường ghi bằng `UPDATE` thuần và tăng `phien_ban`; lệnh vai trò dùng `OPTIMISTIC_FORCE_INCREMENT`, vì entity là `@DynamicUpdate` nên chỉ sửa `cap_nhat_luc` không chắc làm phiên bản tăng.
- **Tách `ThuHoiPhienService`**: ba nơi cần đúng một cách thu hồi (khóa theo họ, thứ tự cố định để không deadlock), nên giữ nó ở một chỗ thay vì chép lại từ `GhiMatKhauMoiService`.
