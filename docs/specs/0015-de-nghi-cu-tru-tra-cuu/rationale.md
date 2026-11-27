# 0015. Lý do và các phương án: đề nghị cư trú và tra cứu hồ sơ

## Context

Sau lát cắt 13 (spec 0014), nhân viên Ban quản lý (BQL) đã thêm, sửa và kết thúc được hồ sơ cư trú trên từng căn hộ, nhưng toàn bộ việc ghi nhận vẫn đi một chiều từ phía BQL và phải mở từng trang căn hộ riêng lẻ. Trong vận hành thực tế của chung cư, cư dân đang sinh sống tại căn hộ mới là người biết trước khi có người thân chuyển vào ở cùng hoặc khi khách thuê cần đăng ký tạm trú mới, còn BQL ở quy mô hàng trăm căn hộ cần một màn hình danh sách tập trung để tra cứu nhân khẩu toàn tòa và xử lý hàng đợi đề nghị chờ xác minh.

Năm lực kỹ thuật và nghiệp vụ định hình trực tiếp thiết kế của lát cắt này:
- **Mâu thuẫn sẵn có trong schema gốc của `ho_so_cu_tru`.** Migration `V2` đã khai báo sẵn giá trị `'Chờ xác minh'` trong `chk_hsct_trangthai`, nhưng lại đặt `ma_nvbql_xac_minh VARCHAR(30) NOT NULL` và chưa có chỗ lưu ai là cư dân đứng đơn gửi đề nghị hay vì sao đề nghị bị từ chối. Khi cư dân vừa gửi yêu cầu qua `API-UC02-04`, chưa có nhân viên BQL nào tham gia xác minh.
- **Biên giới tài khoản và bảo mật danh tính cá nhân.** Theo spec 0008, cư dân không tự đăng ký tài khoản và mục nhân khẩu phụ thuộc chưa có tài khoản đang nằm ở phần hoãn lại (Deferred) của scope, nên mọi dòng cư trú đều phải trỏ tới một bản ghi `cu_dan` có sẵn. Tuy nhiên, hệ thống tuyệt đối không thể mở API tìm kiếm cư dân (`resident-options`) cho vai trò `CU_DAN`, vì như vậy bất kỳ cư dân nào cũng tra được họ tên, số điện thoại và số căn cước công dân (CCCD) 12 chữ số của toàn bộ hàng xóm trong khu.
- **Cách ly quyền riêng tư ngay trong cùng một căn hộ.** Ở căn hộ cho thuê nhiều người hoặc nhiều hộ ghép, người đang ở cùng căn không được tự ý xem lý do từ chối hay tự hủy đơn đề nghị do người khác trong căn gửi lên.
- **Giữ bất biến khóa dữ liệu và ranh giới phân hệ từ spec 0014.** Spec 0014 đã thiết lập thứ tự khóa cố định (`can_ho` trước, `ho_so_cu_tru` sau), hai ràng buộc `EXCLUDE USING gist` cho dòng `'Hợp lệ'`, hàm đồng bộ trạng thái sử dụng căn hộ và ranh giới gọi qua service công khai giữa `auth`, `canho` và `cutru`. Lát cắt 14 phải khớp vào đúng đường ray đó mà không tạo vòng phụ thuộc bean hay nguy cơ deadlock.
- **Tuân thủ phân quyền ở mức method.** Quy tắc của dự án bắt buộc mỗi handler controller mang đúng một annotation `@ChoPhep` ở mức hàm và hạn chế trộn vai trò không đồng nhất trong cùng một endpoint ghi dữ liệu.

## Options considered

### Option 1: Tách bảng mới `de_nghi_cu_tru` riêng biệt với `ho_so_cu_tru`

Giữ nguyên `ma_nvbql_xac_minh NOT NULL` và ba trạng thái cũ trên `ho_so_cu_tru`; tạo thêm bảng `de_nghi_cu_tru` (mã `DN00000001`) lưu các đơn chờ xác minh, bị từ chối hoặc đã hủy, và chỉ khi BQL bấm duyệt mới chèn một dòng mới vào `ho_so_cu_tru`.

**Pros**:
- Không cần đụng tới cột `ma_nvbql_xac_minh` hay các ràng buộc hiện có của bảng `ho_so_cu_tru`.
- Tách hẳn dữ liệu đơn đề nghị chưa duyệt khỏi bảng hồ sơ cư trú chính thức.

**Cons**:
- Màn hình tập trung `/ho-so-cu-tru` vừa phải lọc hồ sơ toàn tòa vừa phải xử lý hàng đợi chờ xác minh nên buộc phải tách làm hai bảng gọi hai API khác nhau hoặc viết câu `UNION ALL` phức tạp giữa hai khóa chính khác tiền tố (`DN...` và `HS...`).
- Khi đề nghị được duyệt, mã định danh bị đổi từ `DN...` sang `HS...`, làm đứt mạch đối tượng `HoSoCuTru:<maHoSo>` trong nhật ký kiểm toán và buộc giao diện cư dân phải nối hai nguồn dữ liệu để theo dõi một đợt đăng ký.
- Bỏ phí giá trị `'Chờ xác minh'` vốn đã được thiết kế sẵn trong `ho_so_cu_tru` từ schema gốc.

### Option 2: Lưu chung trên `ho_so_cu_tru` với `ma_nvbql_xac_minh` rỗng khi chờ duyệt, đối chiếu cặp CCCD kèm SĐT và tách ba endpoint hành động (chọn)

Bỏ `NOT NULL` trên `ma_nvbql_xac_minh` và khóa chặt bằng `CHECK` theo trạng thái (`'Chờ xác minh'` và `'Đã hủy'` bắt buộc `NULL`, ba trạng thái còn lại bắt buộc `NOT NULL`); thêm `ma_cu_dan_de_nghi`, `ghi_chu_de_nghi`, `ly_do_tu_choi` và hai trạng thái kết thúc `'Từ chối'`, `'Đã hủy'` ngay trên `ho_so_cu_tru`; cư dân định danh người được đề nghị bằng cờ `laChinhToi` hoặc cặp `(soCccd, soDienThoai)` khớp đồng thời; tách ba endpoint hành động `approve`, `reject`, `cancel` dưới cùng thứ tự khóa dòng `can_ho` của spec 0014.

**Pros**:
- Một bảng, một mã `HS...` xuyên suốt vòng đời và một DTO `HoSoCuTruResponse` dùng chung cho mọi màn hình; `GET /api/v1/residency-records` phân trang và sắp xếp trực tiếp trên một bảng duy nhất.
- Cặp khóa đối chiếu `(soCccd, soDienThoai)` kết hợp thông báo lỗi đồng nhất giúp cư dân đăng ký được người thân mà không mở API tra cứu danh bạ và ngăn dò quét CCCD đơn lẻ.
- Mỗi hành động `approve`, `reject` (`@ChoPhep(BAN_QUAN_LY)`) và `cancel` (`@ChoPhep(CU_DAN)`) có phân quyền mức hàm tách bạch, đầu vào rõ ràng và nhật ký kiểm toán riêng biệt.

**Cons**:
- Bảng `ho_so_cu_tru` mang thêm ba cột có thể `NULL` tùy theo nguồn tạo hồ sơ (cần viết các ràng buộc `CHECK` chặt ở `V19` để không sinh tổ hợp cột vô nghĩa).
- Cư dân phải nhập chính xác cả số CCCD lẫn số điện thoại đã đăng ký trên hệ thống của người sẽ vào ở thay vì tìm chọn theo tên.

### Option 3: Lưu chung trên `ho_so_cu_tru`, chỉ đối chiếu một trường định danh và xóa vật lý khi từ chối hoặc tự hủy

Cũng nới `ma_nvbql_xac_minh` trên `ho_so_cu_tru`, nhưng cho cư dân chỉ nhập số điện thoại (hoặc chỉ số CCCD) để tìm `ma_cu_dan`, gộp duyệt, từ chối và hủy vào một endpoint `PATCH /status` chung cho cả hai vai trò, và xóa dòng khỏi DB khi bị từ chối hoặc hủy để giữ nguyên ba giá trị trạng thái cũ.

**Pros**:
- Ít trạng thái hơn trên `chk_hsct_trangthai` và biểu mẫu gửi đề nghị ngắn hơn một ô nhập.
- Ít số lượng endpoint hơn trên controller.

**Cons**:
- Xóa dòng khi từ chối làm cư dân không còn thấy kết quả hay lý do từ chối trên web (trái yêu cầu theo dõi trạng thái của scope) và phá nguyên tắc không xóa vật lý của phân hệ cư trú.
- Đối chiếu chỉ bằng số điện thoại 10 chữ số dễ bị đoán trúng hơn nhiều so với yêu cầu biết đồng thời cả CCCD 12 chữ số lẫn số điện thoại.
- Gộp `BAN_QUAN_LY` và `CU_DAN` vào chung một endpoint đổi trạng thái buộc service phải tự rẽ nhánh phân quyền theo nội dung body, dễ sai sót hơn tách riêng handler.

## Rationale

Chọn Option 2 vì nó giải quyết trực tiếp mọi lực xung đột trong Context bằng ít thành phần chuyển động nhất. Bảng `ho_so_cu_tru` từ thiết kế gốc đã dự trù trạng thái `'Chờ xác minh'`; nguyên nhân duy nhất khiến nó chưa lưu được đề nghị của cư dân là cột `ma_nvbql_xac_minh` đang gắn cứng `NOT NULL`. Thay vì đẻ thêm bảng phụ như Option 1 làm vỡ màn hình tra cứu tập trung `/ho-so-cu-tru` và cắt đôi chuỗi nhật ký kiểm toán, việc chuyển quy tắc bắt buộc của `ma_nvbql_xac_minh` vào ràng buộc `CHECK` theo trạng thái vừa giữ nguyên độ chặt chẽ của dữ liệu (không bao giờ có hồ sơ `'Hợp lệ'`, `'Hết hiệu lực'` hay `'Từ chối'` thiếu nhân viên xác minh) vừa cho phép một câu truy vấn phân trang duy nhất phục vụ trọn vẹn màn hình của BQL. Hai ràng buộc `EXCLUDE USING gist` từ `V18` vốn đã lọc `WHERE (trang_thai_ho_so = 'Hợp lệ')` nên hoàn toàn không cản trở việc lưu lịch sử các đề nghị `'Từ chối'` và `'Đã hủy'`, trong khi một partial unique index nhỏ trên `WHERE (trang_thai_ho_so = 'Chờ xác minh')` chặn gọn việc gửi trùng đơn đang chờ.

Về định danh cư dân và phân quyền vai trò cư trú trong đề nghị: yêu cầu khớp đồng thời cặp `(soCccd, soDienThoai)` kèm tùy chọn `"Chính tôi"` hóa giải bài toán không có danh bạ tìm kiếm phía cư dân mà không để lộ thông tin cá nhân, đồng thời việc trả cùng một mã 422 `CUTRU_KHONG_KHOP_CU_DAN` cho cả trường hợp lệch thông tin lẫn tài khoản `'Ngừng hoạt động'` bịt kín đường dò xem một số CCCD có tồn tại trong hệ thống hay không. Giới hạn cư dân chỉ được gửi đề nghị cho vai trò `'Thành viên gia đình'` hoặc `'Khách thuê'` phản ánh đúng nghiệp vụ quản lý tòa nhà: quyền `'Chủ căn hộ'` gắn với hồ sơ pháp lý bàn giao hoặc chuyển nhượng căn hộ do BQL trực tiếp kiểm tra và ghi nhận.

Các quyết định chi tiết được chốt để quá trình triển khai không còn điểm mờ:
- **Duyệt nguyên trạng, không sửa trường ngay trong bước `approve`, và cho phép duyệt khi `ngay_bat_dau < homNay` miễn chưa qua `ngay_ket_thuc`**: giữ cho dòng được duyệt khớp đúng 100% nội dung cư dân đã khai báo và xử lý êm trường hợp cư dân nộp đơn cuối tuần sang đầu tuần BQL mới mở duyệt; nếu thông tin khai sai hoặc đã qua cả `ngay_ket_thuc`, BQL từ chối kèm lý do để cư dân nộp đơn mới. Phương án thua là chặn 422 ngay khi `ngay_bat_dau < homNay`, vì sẽ làm hỏng mọi đơn nộp đúng ngày vào buổi chiều tối hoặc dịp nghỉ lễ.
- **Không để `HoSoCuTruHetHanJob` tự đóng dòng `'Chờ xác minh'`**: đề nghị của cư dân cần phản hồi minh bạch của BQL thay vì âm thầm biến thái lúc nửa đêm mà không có người xác minh. Phương án thua là job tự từ chối ngầm, khiến BQL bỏ sót việc tồn đọng hàng đợi và sinh dòng `'Từ chối'` không có `ma_nvbql_xac_minh`.
- **Giữ nguyên ranh giới phân hệ bằng `CuTruDinhDanhService.maCuDanKhopTuKhoa` và `CanHoCuTruService.maCanHoThuocBoLoc`**: ở quy mô vài trăm căn hộ của tòa chung cư, truyền tập mã qua service công khai giữ cho `HoSoCuTruQuery` chỉ đọc duy nhất bảng `ho_so_cu_tru`, tuân thủ nghiêm quy tắc không đọc chéo bảng giữa các phân hệ như `NhatKyTraCuuService` đã làm ở spec 0007. Phương án thua là viết câu SQL `JOIN` xuyên cả bốn bảng của ba phân hệ ngay trong repository của `cutru`.
- **Đếm số lượng hàng đợi `'Chờ xác minh'` trên `/ho-so-cu-tru` bằng `GET /api/v1/residency-records?trangThaiHoSo=Chờ xác minh&size=1`**: tận dụng trực tiếp `totalItems` trên `PageResponse` chuẩn của spec 0001 mà không phải đẻ thêm trường lạ ngoài chuẩn phân trang hay thêm một endpoint chỉ đếm số. Phương án thua là mở thêm endpoint `/pending-count` riêng lẻ cho một truy vấn vốn đã chạy dưới vài mili giây nhờ index `idx_ho_so_cu_tru_trangthai_taoluc`.
