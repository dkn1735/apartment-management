# 0016. Lý do và các phương án: phương tiện gửi xe căn hộ

## Context

Bảng `phuong_tien` có từ migration `V2` (chép từ schema gốc 38 bảng) nhưng chưa có API hay màn hình nào dùng tới. Scope tách UC02 thành ba lát: hồ sơ cư trú (spec 0014), đề nghị cư trú và tra cứu (spec 0015), và lát này là phương tiện gửi xe (`API-UC02-05` đến `07`). Lát này phải chốt dữ liệu xe đủ chắc để UC04 sau này tính phí gửi xe theo loại xe và đối soát vé tháng.

Schema hiện có mâu thuẫn với yêu cầu ở ba chỗ. Cột `bien_so` là `NOT NULL UNIQUE` trên toàn bảng: biển của một xe đã hủy không bao giờ đăng ký lại được (kể cả khi bán xe cho hộ khác trong tòa), trong khi scope chỉ yêu cầu chặn trùng với xe *đang hoạt động*. Cũng vì `NOT NULL`, xe đạp (thường không có biển) không lưu được. CHECK trạng thái chỉ có `Đang hoạt động`, `Tạm dừng`, `Đã hủy`, không có chỗ cho một đơn cư dân gửi đang chờ BQL xem. Bảng cũng không có cột thời gian, người gửi hay lý do.

Các lực khác định hình thiết kế:
- **Tiền thật phía sau.** Một thẻ xe đang hoạt động sẽ thành một khoản phí hằng tháng ở UC04. Nếu cư dân tự kích hoạt được thẻ, hoặc thẻ gắn với người không ở tại căn, dữ liệu tính phí sai từ gốc.
- **Biển số nhập tay nhiều kiểu.** Cùng một biển có thể được gõ `30A-123.45`, `30a12345` hay `30A 123 45`. So trùng trên chuỗi thô sẽ để lọt trùng.
- **Ranh giới phân hệ.** Phân hệ `cutru` đã hơn 40 lớp. Điều kiện chủ xe cư trú tại căn phải đọc từ `cutru`, khóa dòng căn hộ phải đi qua `canho`, tên cư dân đọc từ `auth`, mà không tạo vòng phụ thuộc bean (bài học từ spec 0014).
- **Dữ liệu cá nhân trên căn dùng chung.** Nhiều hộ thuê ghép một căn; cư dân cần thấy xe đang gửi của căn nhưng không nên thấy đơn bị từ chối của người khác hay tên nhân viên nội bộ (cùng nguyên tắc spec 0014 và 0015).
- **Danh sách API gốc chỉ có ba endpoint**, không có endpoint duyệt hay sửa xe.

Nếu không chốt, `/develop` sẽ phải tự đoán cách xử lý biển số, xe đạp và trạng thái ban đầu của đơn cư dân, và đó đều là những chỗ đoán sai sẽ làm hỏng dữ liệu tính phí.

## Options considered

### Option 1: Giữ schema gốc, làm trong `cutru`, cư dân đăng ký vào thẳng `Đang hoạt động`

Không sửa CHECK hay UNIQUE; thêm vài lớp xe vào `cutru` với mã lỗi `CUTRU_XE_...`; cư dân tự đăng ký là thẻ có hiệu lực ngay, BQL chỉ tạm dừng hoặc hủy sau. Xe đạp phải nhập một chuỗi giả vào ô biển số.

**Pros**:
- Ít thay đổi nhất: không cần migration đổi ràng buộc, đúng ba API gốc, không có luồng duyệt.
- Cư dân không phải chờ.

**Cons**:
- Cư dân tự tạo được khoản phí gửi xe (hoặc tự cấp thẻ để vào hầm) mà BQL không kiểm.
- Biển của xe đã hủy bị khóa vĩnh viễn, trái với scope.
- Chuỗi giả cho xe đạp làm bẩn cột biển số và vẫn phải chống trùng.
- `cutru` phình thêm, mã lỗi lẫn giữa cư trú và xe.

### Option 2: Phân hệ `phuongtien` riêng, sửa bảng bằng `V20`, đơn cư dân đi qua chung endpoint đổi trạng thái (chọn)

Thêm `Chờ duyệt` và `Từ chối`; biển số dạng chuẩn, cho phép `NULL` với xe đạp, chống trùng bằng unique index một phần trên hai trạng thái giữ biển (`Đang hoạt động`, `Tạm dừng`); đơn `Chờ duyệt` không giữ chỗ biển, để không ai chặn được biển thật bằng một đơn giả, và BQL kiểm trùng lại lúc duyệt. BQL duyệt, từ chối, tạm dừng, kích hoạt lại, hủy qua một ma trận trong `PUT /vehicles/{id}/status`; cư dân chỉ thêm một endpoint `cancel` để rút đơn.

**Pros**:
- Mọi thẻ hoạt động đều qua BQL và gắn với người đang ở thật, dữ liệu tính phí tin được.
- Index một phần cho phép dùng lại biển đã hủy mà DB vẫn là chốt chặn cuối khi chạy đua.
- Chỉ thêm một endpoint ngoài danh sách gốc; luồng duyệt dùng lại `API-UC02-07`.
- Package riêng có tiền tố mã lỗi riêng, điểm vào rõ cho UC04.

**Cons**:
- Một migration đổi ràng buộc trên bảng có sẵn.
- Một endpoint `PUT` gánh nhiều nghĩa (duyệt và quản lý thẻ), nên ma trận chuyển trạng thái phải được test kỹ.
- Cư dân phải chờ BQL duyệt.

### Option 3: Như Option 2 nhưng tách `approve` và `reject` riêng như spec 0015

Giữ `PUT .../status` cho vòng đời thẻ đã có hiệu lực, thêm `POST /vehicles/{id}/approve` và `/reject`.

**Pros**:
- Nhất quán với luồng đề nghị cư trú của spec 0015; mỗi endpoint một nghĩa.
- Quyền và kiểm lỗi từng bước dễ đọc hơn.

**Cons**:
- Thêm hai API ngoài danh sách 81 API của đồ án, thêm hai hàm trên frontend.
- Hai đường cùng chạm trạng thái xe, phải giữ hai bộ kiểm đồng bộ.

## Rationale

Lực quyết định là tiền: thẻ xe đang hoạt động sẽ thành phí ở UC04, nên trạng thái khởi đầu của đơn cư dân không thể là `Đang hoạt động` (loại Option 1). Cũng vì tính phí, mỗi thẻ hoạt động phải gắn với một người đang ở hợp lệ hôm nay, kiểm cả lúc đăng ký lẫn lúc duyệt và kích hoạt lại, chứ không chỉ một lần.

Giữa Option 2 và Option 3, bạn chọn dùng chung `PUT /vehicles/{id}/status`. Cái giá là một endpoint mang nhiều nghĩa, nhưng ma trận chuyển trạng thái nhỏ (năm trạng thái, sáu cạnh), dễ gói vào một lớp `MaTranTrangThai` để unit test hết, và đổi lại đồ án giữ sát danh sách API gốc. Spec 0015 tách endpoint vì đề nghị cư trú có body và luồng kiểm khác hẳn sửa hồ sơ; còn với xe, duyệt chỉ là một bước chuyển trạng thái như mọi bước khác, chỉ cộng thêm phần kiểm chủ xe cũng có ở bước kích hoạt lại.

Biển số lưu dạng chuẩn thay vì lưu chuỗi người nhập rồi index trên biểu thức: tên ràng buộc đơn giản, `LoiCoSoDuLieu` đổi lỗi theo tên index như các phân hệ trước, và giao diện hiển thị một dạng thống nhất. Kiểm định dạng lỏng (`^[A-Z0-9]{5,12}$`) thay vì theo mẫu biển Việt Nam vì chung cư có xe biển nước ngoài, biển ngoại giao, biển tạm; kiểm chặt sẽ chặn nhầm người thật. Việc không tự dừng xe khi hồ sơ cư trú kết thúc là chủ ý: luồng cư trú và job hết hạn đã xong, nối thêm sự kiện sang `phuongtien` sẽ làm `cutru` phải biết về xe; cờ `chuXeConCuTru` đủ cho BQL ở lát này, và việc tự động có thể thêm sau mà không đổi schema.

Phương án dự phòng là Option 3, nếu sau này luồng duyệt xe cần body riêng (ví dụ cấp số thẻ vật lý lúc duyệt).
