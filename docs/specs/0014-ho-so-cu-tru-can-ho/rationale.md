# 0014. Lý do và các phương án: hồ sơ cư trú căn hộ

## Context

Hệ thống đã có bảng `ho_so_cu_tru` từ migration `V2` và một lớp đọc tối thiểu `CuTruService.maCanHoDangCuTru` để cấp danh sách mã căn hộ trong `PhamViTruyCap` cho cư dân đăng nhập (spec 0004 và spec 0006), nhưng chưa có bất kỳ API hay màn hình nào để ghi nhận, điều chỉnh hay xem nhân khẩu của từng căn hộ. Trong khi đó ở spec 0012, BQL đang tự chọn `trang_thai_su_dung` của căn hộ bằng tay mà không có kiểm tra chéo với hồ sơ cư trú, dẫn tới khả năng dữ liệu mâu thuẫn (căn báo `Trống` nhưng cư dân vẫn có quyền truy cập, hoặc căn báo `Đang sử dụng` nhưng không có nhân khẩu nào).

Các lực chính định hình thiết kế của lát cắt này:
- **Ranh giới chia lát của UC02.** Scope tách UC02 thành ba lát: lát 13 làm luồng BQL quản lý trực tiếp trên căn hộ (`API-UC02-01` đến `03`), lát 14 mới mở luồng cư dân tự gửi đề nghị `Chờ xác minh` và màn hình tra cứu toàn tòa `/ho-so-cu-tru` (`API-UC02-04`), lát 15 làm phương tiện gửi xe. Thiết kế lát 13 phải đặt nền đồng bộ căn hộ và khóa dữ liệu để lát 14 tái sử dụng mà không phải sửa lại cấu trúc.
- **Ranh giới phân hệ và vòng phụ thuộc bean.** `PhamViTinhToan` trong phân hệ `auth` đã phụ thuộc vào `CuTruService` của `cutru` và `ToaNhaService` của `canho`. Ngược lại, `cutru` cần đọc thông tin cư dân, nhân viên BQL từ `auth` và khóa dòng căn hộ của `canho`, còn `SuaCanHoService` của `canho` lại cần hỏi `cutru` xem căn hộ có người đang ở không. Nếu đặt chung vào một service lớn sẽ tạo vòng phụ thuộc lúc khởi động Spring.
- **Ẩn cư dân kiêm nhiệm trên API tài khoản cũ.** Ở spec 0008, `GET /api/v1/accounts` giới hạn cho BQL chỉ thấy tài khoản cư dân thuần (chỉ có duy nhất vai trò `CU_DAN`) và ẩn số CCCD trong danh sách. Thực tế trong chung cư, thành viên Ban quản trị (`BAN_QUAN_TRI`) cũng là cư dân sống trong tòa nhà (như tài khoản mẫu `0900000006`), nên BQL cần một đường tra cứu riêng để chọn được mọi người có dòng `cu_dan` kèm đối chiếu CCCD.
- **Bảo vệ dữ liệu cá nhân (PII) trên căn hộ dùng chung.** Bảng `cu_dan` và `nguoi_dung_he_thong` giữ số CCCD 12 chữ số, số điện thoại và email. Một căn hộ cho thuê có nhiều khách thuê ở ghép hoặc nhiều đời khách thuê nối tiếp nhau; cư dân đang ở cần biết ai đang sống cùng căn với mình hôm nay nhưng không được thấy số CCCD, thông tin liên lạc cá nhân của người cùng căn, tên nhân viên BQL nội bộ hay lịch sử của các đời chủ và khách thuê trước.
- **Tính đúng đắn theo ngày thực tế.** Hồ sơ tạm trú hoặc hợp đồng thuê có `ngay_ket_thuc` xác định trước, hoặc có `ngay_bat_dau` ký trước cho tuần tới. Quyền vào căn hộ và trạng thái sử dụng của căn hộ phải phản ánh đúng ngày hôm nay theo giờ Việt Nam, kể cả khi hai nhân viên BQL cùng thao tác một lúc.

## Options considered

### Option 1: Bỏ `@Immutable` trên JPA entity, chỉ kiểm tra trùng ngày và đồng bộ ở tầng ứng dụng

Mở rộng entity `HoSoCuTru` thành entity JPA đọc ghi đầy đủ, kiểm tra giao khoảng ngày bằng câu truy vấn `SELECT` trong Java và không thêm ràng buộc loại trừ hay cột thời điểm ở cơ sở dữ liệu; trạng thái căn hộ để BQL tự sửa tay hoàn toàn.

**Pros**:
- Không cần viết migration `V18`, ít thay đổi nhất ở tầng cơ sở dữ liệu.
- Viết nhanh cho luồng đơn lẻ không có tranh chấp đồng thời.

**Cons**:
- Hai request chạy song song ở mức cô lập `READ COMMITTED` mặc định của PostgreSQL cùng đọc thấy trống rồi cùng chèn được hai `Chủ căn hộ` hoặc hai hồ sơ chồng ngày nếu quên khóa dòng căn hộ.
- Trạng thái `trang_thai_su_dung` của căn hộ tiếp tục lệch với thực tế nhân khẩu đang ở, để lại lỗi nghiệp vụ cho các phân hệ tính phí và tiện ích phía sau.
- Khóa chính `HS00000001` sinh bằng biểu thức chuỗi từ sequence ở PostgreSQL không khớp tự nhiên với `@GeneratedValue` chuẩn của JPA khi thêm mới.

### Option 2: `JdbcClient` dưới khóa dòng `can_ho`, ràng buộc `EXCLUDE USING gist` ở DB và đồng bộ hai chiều trạng thái căn (chọn)

Giữ nguyên `HoSoCuTru` `@Immutable` cho đường đọc phạm vi nhanh; dùng `JdbcClient` cho toàn bộ truy vấn và ghi mới dưới khóa `SELECT ... FOR UPDATE` trên dòng `can_ho`; bổ sung `V18` với `tao_luc`, `cap_nhat_luc`, `CHECK` ngày kết thúc và hai ràng buộc `EXCLUDE USING gist` (`btree_gist`); đồng bộ tự động hai chiều giữa nhân khẩu đang cư trú hôm nay và `can_ho.trang_thai_su_dung` kèm job quét hết hạn đầu ngày.

**Pros**:
- Khóa dòng `can_ho` tuần tự hóa mọi thao tác cư trú lẫn sửa căn hộ trên cùng một căn, giữ bất biến đồng bộ trạng thái 100% và loại bỏ nguy cơ deadlock nhờ thứ tự khóa cố định (`can_ho` trước, `ho_so_cu_tru` sau).
- Cơ sở dữ liệu tự bảo vệ mình khỏi dữ liệu chồng ngày kể cả khi chèn SQL trực tiếp hay khi có lối rẽ code mới ở lát 14.
- Đồng nhất hoàn toàn với cách viết `JdbcClient`, đọc `JsonNode` và kiểm lỗi gom trường của các spec 0011, 0012 và 0013.

**Cons**:
- Cần bật extension `btree_gist` và dịch thêm mã lỗi SQLSTATE `23P01` trong `LoiCoSoDuLieu`.
- Phải tách nhỏ các service đầu nối giữa `auth`, `canho` và `cutru` để tránh vòng phụ thuộc bean.

### Option 3: Tính động `trang_thai_su_dung` lúc đọc thay vì lưu và đồng bộ trên `can_ho`

Bỏ ý nghĩa lưu trữ độc lập của `'Trống'` và `'Đang sử dụng'` trên cột `can_ho.trang_thai_su_dung`: mỗi lần đọc danh sách hoặc chi tiết căn hộ đều `EXISTS` sang `ho_so_cu_tru` để suy ra căn đang sử dụng hay không, chỉ lưu riêng cờ sửa chữa hoặc bàn giao.

**Pros**:
- Không bao giờ lệch trạng thái căn hộ ngay cả khi job đầu ngày chưa kịp chạy trong vài phút sau nửa đêm.
- Không cần cập nhật bảng `can_ho` khi thêm hoặc kết thúc hồ sơ cư trú.

**Cons**:
- Phá vỡ hợp đồng lọc và sắp xếp theo cột `trang_thai_su_dung` đã xây và kiểm thử xong ở spec 0012 (`GET /api/v1/apartments?trangThaiSuDung=...&sort=trangThaiSuDung,asc`), làm phức tạp mọi câu truy vấn danh sách căn hộ.
- Mất khả năng phân biệt giữa căn `'Trống'` và căn `'Đã bàn giao'` nhưng chủ chưa dọn vào ở.

## Rationale

Chọn Option 2 vì đây là điểm cân bằng tốt nhất giữa tính toàn vẹn dữ liệu cứng và việc kế thừa trọn vẹn những gì spec 0008, 0012 và 0013 đã xây. Trong bài toán cư trú, hai lỗi gây hậu quả nặng nhất là cấp trùng chủ hộ hoặc chồng ngày ở cùng một căn và để trạng thái căn hộ mâu thuẫn với quyền truy cập thực tế của cư dân. Khóa dòng `can_ho` giải quyết trọn vẹn bài toán đồng bộ trạng thái giữa hai phân hệ `cutru` và `canho`, còn `EXCLUDE USING gist` trên `daterange(ngay_bat_dau, ngay_ket_thuc, '[]')` khóa chặt bất biến khoảng ngày ngay tại tầng lưu trữ bằng công cụ có sẵn của PostgreSQL 18 mà không cần hạ tầng mới. Option 1 quá mỏng ở khâu đồng thời và bỏ ngỏ mâu thuẫn trạng thái căn; Option 3 đòi viết lại bộ lọc và sắp xếp của spec 0012 đồng thời làm mất sắc thái nghiệp vụ giữa căn `'Trống'` và căn `'Đã bàn giao'`.

Về chiều đồng bộ trạng thái căn hộ: khi căn hộ bắt đầu có người đang cư trú hôm nay, chuyển `'Trống'` hoặc `'Đã bàn giao'` sang `'Đang sử dụng'`; khi người cuối cùng kết thúc cư trú hôm nay, đưa `'Đang sử dụng'` về `'Trống'` để phản ánh đúng việc căn hiện không có người ở (nếu căn thực chất chỉ bàn giao chờ khách mới, BQL có thể chuyển tay sang `'Đã bàn giao'` qua `PUT /apartments/{id}`). Riêng `'Đang sửa chữa'` luôn được giữ nguyên ở cả hai chiều vì hộ gia đình có thể sửa nội thất trong lúc vẫn giữ quyền cư trú tại căn. Ở chiều ngược lại, chặn sửa tay căn có người ở sang `'Trống'` / `'Đã bàn giao'` và chặn đặt `'Đang sử dụng'` cho căn không có người ở giúp BQL không vô tình tạo lại dữ liệu mâu thuẫn.

Về phân quyền và bảo mật PII: tách `GET /api/v1/residency-records/resident-options` (gọi qua `CuTruDinhDanhService` của `auth`) giải quyết triệt để việc `GET /api/v1/accounts` ẩn cư dân kiêm vai trò khác như BQT, mà không phải mở rộng quyền quản lý tài khoản của BQL. Trên `GET /api/v1/apartments/{id}/residents`, tách ba mảng `{ dangCuTru, sapBatDau, lichSuCuTru }` giúp giao diện hiển thị mạch lạc cả hợp đồng thuê ký trước ngày dọn vào mà không làm phình `CanHoResponse` của danh sách căn hộ; việc gán `null` toàn bộ CCCD, SĐT, email, người xác minh và cắt lịch sử người khác ngay ở backend khi người gọi chỉ mang quyền `CU_DAN` bảo đảm an toàn dữ liệu cá nhân tuyệt đối ngay trên đường truyền mạng.

Các quyết định chi tiết được chốt trong spec để không phải đoán khi code:
- **Tách `CuTruDinhDanhService` (trong `auth`) và `CanHoCuTruService` (trong `canho`) chỉ phụ thuộc repository/query**, đồng thời giữ `CuTruService` chỉ phụ thuộc `HoSoCuTruRepository`: phá hoàn toàn nguy cơ vòng phụ thuộc Spring bean với `PhamViTinhToan` và `SuaCanHoService`. Phương án thua là dùng `@Lazy` trên constructor, vì `@Lazy` chỉ che giấu thiết kế phụ thuộc vòng.
- **Lịch chạy `HoSoCuTruHetHanJob` đặt mặc định `0 5 0 * * *` (`app.cutru.residency-expiry-cron`) theo `app.timezone` kèm một lần chạy bù khi nhận `ApplicationReadyEvent`**: chạy lúc 00:05 đầu ngày mới để trạng thái `'Hết hiệu lực'` và trạng thái căn hộ được chốt sớm ngay đầu ngày làm việc, đồng thời chạy bù lúc khởi động để tự hồi phục nếu server bảo trì qua nửa đêm (trong 5 phút đầu ngày trước khi job chạy, quyền truy cập ở `CuTruService` và phân nhóm ở `GET .../residents` vẫn tính tức thì theo `homNay` nên không có khe hở quyền). Phương án thua là chỉ chạy cron 03:00 không có chạy bù, làm trạng thái căn trên bảng lọc `/can-ho` trễ hàng giờ hoặc cả ngày nếu tắt máy qua mốc cron.
- **Thứ tự sắp xếp mặc định của ba mảng nhân khẩu**: `dangCuTru` ưu tiên `Chủ căn hộ`, `Thành viên gia đình`, `Khách thuê` rồi tới `ngay_bat_dau ASC` để chủ hộ luôn đứng đầu danh sách căn; `sapBatDau` tăng dần theo `ngay_bat_dau`; `lichSuCuTru` ưu tiên các đợt đã thực sự bắt đầu (`ngay_bat_dau <= homNay`) đứng trước các hồ sơ tương lai bị hủy sớm, rồi giảm dần theo `COALESCE(ngay_ket_thuc, ngay_bat_dau)` để đợt ở vừa kết thúc hiện lên đầu. Phương án thua là sắp thuần theo tên chữ cái hoặc để ngày hủy tương lai nổi lên trên lịch sử thật.
- **Sửa không đổi gì trên `PUT /residency-records/{recordId}` trả 200 không ghi DB và không ghi nhật ký**: nhất quán với `SuaCanHoService` của spec 0012 và `CapNhatTaiKhoanService` của spec 0008, khác với thao tác bật tắt tiện ích có kèm lý do mới của spec 0013.
- **Dùng trực tiếp `daterange(ngay_bat_dau, ngay_ket_thuc, '[]')` thay vì `COALESCE(ngay_ket_thuc, 'infinity'::date)`**: trong PostgreSQL, kiểu rời rạc `daterange` chuẩn hóa cận đóng `'[]'` bằng cách cộng 1 ngày vào cận trên; truyền `'infinity'::date` với `'[]'` sẽ gây lỗi tràn ngày (`date out of range`), còn truyền `NULL` được PostgreSQL hiểu chuẩn xác là cận trên mở vô hạn `[ngay_bat_dau,)`.
