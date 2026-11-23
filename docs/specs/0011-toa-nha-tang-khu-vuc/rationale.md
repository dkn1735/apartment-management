# 0011. Lý do: tòa nhà, tầng và khu vực dùng chung

Phần lý do của [spec 0011](index.md). `/develop` không cần đọc file này.

## Context

Đây là phần đầu của UC01 (tính năng 14 trong scope, Slice 10). Bảng `toa_nha`, `tang`, `khu_vuc_dung_chung` đã có từ `V2` (spec 0002), cùng seed một tòa `A` có ba tầng `A-01` đến `A-03`. API-UC01-01 (danh mục tòa) và trang `/toa-nha` đã chạy từ spec 0006. Mã tòa và mã tầng do người dùng đặt, không có DEFAULT; mã khu vực tự sinh. File đặc tả xlsx chỉ có body giữ chỗ (`maThamChieu`, `ghiChu`) cho hai API thêm, nên trường thật và luật kiểm phải chốt ở đây.

Bảng `toa_nha` có cột `so_tang`, bảng `tang` có các dòng tầng thật. Hai nguồn này nói về cùng một sự thật, và schema hiện tại không có gì buộc chúng khớp nhau: một tòa khai 30 tầng có thể chỉ có 3 dòng tầng, hoặc ngược lại. Luật 6b của đặc tả ("quan hệ phân cấp Tòa nhà, Tầng, Căn hộ hợp lệ") không nói rõ khớp theo nghĩa nào. Mọi phân hệ sau (căn hộ, cư trú, chỉ số điện nước, sự cố) đều dựa vào khung không gian này, nên một khung lệch sẽ lan ra khắp nơi.

Lực khác: phân quyền trong đặc tả không liền mạch (Kỹ thuật được xem tầng nhưng không xem được danh mục tòa); hai BQL có thể thao tác cùng lúc; trang chi tiết tòa cần dữ liệu mà 81 API không trả; nhóm 2 người cần một thiết kế nhỏ, dễ test.

## Options considered

### Option 1: Tầng tự do, `so_tang` chỉ là con số khai báo

Thêm tòa không tạo tầng. Thêm tầng nhận số tầng bất kỳ chưa có (có thể giới hạn trong 1 đến `so_tang`). `so_tang` của tòa là con số BQL khai, không gắn với số dòng tầng.

**Pros**:
- Linh hoạt nhất: tòa có thể thiếu tầng, có tầng đặc biệt, nhập dần.
- Ít code nhất ở bước thêm tòa.

**Cons**:
- `so_tang` và số tầng thật lệch nhau; danh mục tòa hiện "30 tầng" trong khi chỉ có 3 tầng để gắn căn hộ.
- Tòa mới không dùng được cho slice căn hộ cho tới khi BQL thêm từng tầng một.
- "Tầng sai phân cấp" gần như không có trường hợp nào để từ chối ngoài trùng số.

### Option 2: Tòa và tầng luôn khớp (chọn)

Thêm tòa N tầng thì tạo luôn N dòng tầng. Thêm tầng chỉ nhận đúng tầng kế tiếp (`so_tang + 1`), tăng `so_tang` trong cùng transaction, đọc dưới khóa dòng tòa. Mã tầng do server ghép. CHECK ở DB giữ định dạng mã và giới hạn.

**Pros**:
- Một sự thật duy nhất: `so_tang` luôn đúng, không cần đồng bộ.
- Tòa dùng được ngay; slice căn hộ đơn giản hơn.
- Luật 6b có nghĩa rõ, kiểm được, có một mã lỗi duy nhất.
- Khóa dòng biến chạy đua thành cùng một lỗi 422 như mọi trường hợp sai khác.

**Cons**:
- Không mô tả được tầng hầm, tầng lửng, đánh số đặc biệt.
- Thêm tòa 99 tầng ghi 100 dòng trong một request (vẫn nhỏ).

### Option 3: Bỏ cột `so_tang`, tính từ số dòng `tang`

Không lưu `so_tang` nữa; danh mục đếm tầng lúc đọc như đếm căn hộ.

**Pros**:
- Không có dữ liệu suy ra được lưu trữ, nên không thể lệch.

**Cons**:
- Đổi schema đã chạy (bỏ cột, sửa entity và `ToaNhaQuery`), lệch khỏi `sql/database_schema_quan_ly_chung_cu.sql` mà báo cáo đồ án dựa vào.
- Vẫn cần một luật cho tầng kế tiếp và khóa cho chạy đua, nên không bớt được độ phức tạp ở bước thêm tầng.

## Rationale

Option 2 thắng vì lực mạnh nhất ở Context là mọi phân hệ sau dựa vào khung không gian này: một khung tự nhất quán ngay từ đầu rẻ hơn nhiều so với dọn dữ liệu lệch về sau. Option 3 đạt cùng mục tiêu theo cách "đúng sách" hơn (không lưu giá trị suy ra), nhưng phải sửa schema gốc của đồ án và vẫn cần đúng luật cùng khóa như Option 2; Option 2 giữ cột, dùng transaction và khóa dòng để cột không thể lệch. Option 1 chỉ đáng chọn nếu có tòa với cấu trúc tầng đặc biệt, điều mà phạm vi đồ án không đòi hỏi.

Các quyết định nhỏ đi theo cùng hướng "người dùng không thể nhập sai": mã tầng do server ghép, mã tòa không có gạch nối để tách mã tầng không nhập nhằng, giới hạn 99 tầng giữ mã tầng luôn hai chữ số và chặn nhập nhầm tạo hàng nghìn dòng. Thêm tầng gửi `soTang` thay vì body rỗng để có một giá trị kiểm được: nút bấm hai lần hay hai BQL cùng thao tác cho ra lỗi rõ ràng chứ không lặng lẽ thêm hai tầng (một dạng idempotency nhẹ, tức gửi lại cùng một yêu cầu không gây tác dụng gấp đôi).

Tên tòa so không phân biệt hoa thường bằng `lower()` của PostgreSQL, cùng biểu thức với unique index, để luật ở service và lưới chặn ở DB là một; tự hạ chữ ở Java có thể khác ICU ở vài ký tự tiếng Việt. Chạy đua khi thêm tòa dịch tên ràng buộc thành cùng response 409, theo mẫu `AUTH_IDENTITY_TAKEN` của spec 0008.

Về phân quyền, mở UC01-01 cho Kỹ thuật vì nếu không, Kỹ thuật có quyền xem tầng mà không có đường nào để tới trang tầng. Endpoint chi tiết tòa (`GET /buildings/{id}`) được thêm thay vì nhét thông tin tòa vào response danh sách tầng, vì làm vậy phá dạng `PageResponse` chuẩn của spec 0001; endpoint này mở cho cùng vai trò với danh mục vì nó trả đúng dữ liệu của danh mục. Khu vực dùng chung chỉ có API xem theo đặc tả, nên dữ liệu đến từ seed. Ghi nhật ký cho thêm tòa và tầng vì đây là thay đổi cấu trúc mà mọi phân hệ khác dựa vào, và spec 0007 làm việc này gần như miễn phí.

Phương án dự phòng cho từng quyết định nhỏ: tạo tầng thủ công (Option 1); body thêm tầng rỗng (đơn giản hơn nhưng mất kiểm 6b và chặn bấm đúp); để unique của DB tự báo 409 `CONFLICT` chung thay cho khóa dòng (ít code hơn nhưng hai mã lỗi cho một tình huống); so tên chính xác (đúng unique hiện có nhưng "Tòa A" và "TÒA A" cùng tồn tại được).
