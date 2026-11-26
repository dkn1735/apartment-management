# 0012. Lý do: tìm kiếm và quản lý căn hộ

## Context

Căn hộ là đơn vị mà gần như mọi phân hệ sau dựa vào: cư trú gắn cư dân vào căn, điện nước đo theo căn, phí tính theo căn, tiện ích và sự cố đều mang `ma_can_ho`. Bảng `can_ho` đã có từ V2 (mã do người dùng đặt, diện tích, bốn trạng thái, cờ tạm dừng tiện ích), seed có ba căn `A-0101`, `A-0201`, `A-0301`, nhưng chưa có API nào đọc hay ghi nó. Spec 0011 đã dựng khung tòa và tầng, với tầng luôn liên tục từ 1 tới `so_tang` và mã tầng do server ghép.

Đặc tả gốc (API-UC01-05 đến 08) yêu cầu "mã căn hộ duy nhất và hợp lệ theo tầng", lọc theo tòa, tầng, diện tích, trạng thái, và sửa diện tích hoặc trạng thái. "Hợp lệ theo tầng" chưa được định nghĩa: nếu mã là chuỗi tự do thì một căn có thể mang mã chỉ tầng 3 trong khi nằm ở tầng 5, và mọi phân hệ đọc mã sẽ hiểu sai. Mã căn là khóa chính được nhiều bảng tham chiếu, nên sai từ đầu thì gần như không sửa được.

Các lực khác: nhóm 2 người, dữ liệu vài trăm căn, tần suất sửa rất thấp; scope yêu cầu sửa diện tích và trạng thái phải vào nhật ký kiểm toán; phạm vi dữ liệu (cư dân chỉ xem căn mình) phải đi qua `PhamViTruyCapService` (spec 0006); entity `ToaNha` là `@Immutable` và spec 0011 đã chọn ghi bằng `JdbcClient` vì khóa do người dùng đặt làm `save()` chạy `merge`. Phân hệ cư trú (UC02) chưa có, nên luật giữa trạng thái căn và cư dân chưa có chỗ dựa.

## Options considered

### Option 1: Mã nhập tự do, ghi bằng JPA

BQL gõ mã căn bất kỳ (chỉ kiểm duy nhất và tiền tố tòa); thêm entity `CanHo` có thể ghi, dùng `save()` và Spring Data `Specification` cho bộ lọc.

**Pros**:
- Linh hoạt nhất với cách đánh số thực tế (`A-0105B`, căn ghép).
- Bộ lọc động gọn bằng `Specification`, ít SQL viết tay.

**Cons**:
- "Hợp lệ theo tầng" gần như không kiểm được; mã có thể nói sai tầng.
- `save()` với khóa do người dùng đặt chạy `merge`, ghi đè lặng lẽ thay vì báo trùng (đúng lỗi spec 0011 đã tránh); lệch cách ghi của phân hệ.

### Option 2: Mã do server ghép từ tầng và số căn, `JdbcClient`, CHECK ở DB (chọn)

BQL chọn tầng và nhập số căn 1 đến 99; server ghép `<maTang><số căn hai chữ số>`; `V16` thêm CHECK ràng mã căn với `ma_tang` của chính dòng đó. Ghi bằng `JdbcClient`, sửa dưới `SELECT ... FOR UPDATE`.

**Pros**:
- Mã luôn khớp tầng, khớp seed hiện có, và DB chặn cả dữ liệu chèn tay.
- Cùng mẫu với mã tầng và cách ghi của spec 0011, nên lớp và test đi theo khuôn có sẵn.
- Khóa dòng làm giá trị cũ trong nhật ký luôn đúng mà không thêm cột.

**Cons**:
- Không có cách đánh số đặc biệt; tối đa 99 căn một tầng.
- Bộ lọc động phải viết SQL tay (dựng `WHERE` có điều kiện).

### Option 3: Thêm cột `so_can` và UNIQUE (`ma_tang`, `so_can`)

Như Option 2 nhưng lưu số căn thành cột riêng, mã căn vẫn ghép.

**Pros**:
- Số căn đọc thẳng từ cột, không phải tách từ mã; dễ sắp theo số căn.

**Cons**:
- Lưu một giá trị suy ra được từ mã, phải giữ hai nơi khớp nhau (thêm CHECK hoặc rủi ro lệch).
- Đổi schema nhiều hơn mà không đổi hành vi nào người dùng thấy.

## Rationale

Lực quyết định là mã căn làm khóa chính cho cả hệ thống: một mã sai tầng lan sang cư trú, điện nước, phí và không sửa được sau đó. Option 2 làm "hợp lệ theo tầng" thành một bất biến DB tự giữ, đúng như mã tầng ở spec 0011, nên hai lát cùng một khuôn và nhóm 2 người không phải học thêm cách nào. Option 1 linh hoạt hơn nhưng đổi sự linh hoạt lấy dữ liệu sai lặng lẽ, và đưa lại lỗi `merge` mà spec 0011 đã tránh. Option 3 không thêm hành vi gì mà thêm một giá trị suy ra phải giữ khớp.

Ghi sau thắng có khóa dòng được chọn thay vì khóa lạc quan (thêm cột phiên bản): tần suất sửa rất thấp, nhật ký đã thấy cả hai lần sửa, và khóa lạc quan thêm cột, thêm trường request và một luồng lỗi trên giao diện cho một tình huống gần như không xảy ra.

Bộ lọc tầng dùng `soTang` dựng từ số tầng của tòa, thay vì mở danh sách tầng cho Kế toán và BQT, vì bất biến tầng liên tục của spec 0011 đã bảo đảm "Tầng 1 đến N" đúng, và không phải đổi quyền của một API đã xong. Luật giữa trạng thái và cư dân để UC02 quyết, vì đọc bảng cư trú từ `canho` trước khi phân hệ đó có service công khai sẽ phá quy tắc "phân hệ khác chỉ gọi qua service công khai".

Các chi tiết mình tự quyết khi viết spec: `q` khớp một phần mã và thoát ký tự `%`, `_` (không thêm index, dữ liệu nhỏ); sắp trạng thái theo thứ tự nghiệp vụ bằng `CASE` thay vì bảng chữ cái (phương án khác: theo collation, nhưng `Đã bàn giao` sẽ đứng trước `Đang sử dụng`); tham số lọc số nhận `String` rồi parse ở service để lỗi ra đúng trường (phương án khác: `@RequestParam BigDecimal`, nhưng giá trị sai ra lỗi chuyển kiểu chung); `maTang` không tồn tại là 400 ở ô đó thay vì 404, vì nó là một ô trong form và thông báo không phân biệt "không có" với "ngoài phạm vi".
