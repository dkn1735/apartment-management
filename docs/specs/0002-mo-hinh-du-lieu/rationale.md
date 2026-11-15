# 0002. Mô hình dữ liệu: lý do và các phương án

Phần ghi lại quyết định cho [index.md](index.md). `/develop` không cần đọc file này.

## Context

Dự án đã có một schema thiết kế sẵn (`sql/database_schema_quan_ly_chung_cu.sql`, 38 bảng, tên PascalCase và camelCase) nằm trong tài liệu nộp, nhưng backend chưa có migration nào. Spec 0001 đã chốt Flyway, tên snake_case, `TIMESTAMPTZ`, mã khóa chính dạng tiền tố cộng số tuần tự, `ddl-auto=validate`, và để lại cho spec này: danh sách tiền tố, cách seed, collation tiếng Việt.

Schema gốc chưa đủ cho phân hệ xác thực, phân hệ được làm trước tiên: không có nơi lưu refresh token, mã đặt lại mật khẩu, bộ đếm đăng nhập sai; file đặc tả API nhắc tới Admin (API-AUTH-05) và Bảo vệ sảnh (API-UC09-06) nhưng không có bảng cho hai vai trò này. Vai trò hiện được xác định bằng các bảng con (mô hình TPT, tức mỗi vai trò một bảng con trỏ về bảng người dùng gốc).

Đọc kỹ schema còn thấy vài lỗi nhỏ: cột thời điểm không có múi giờ, cột JSON lưu TEXT, hai cột trạng thái cho phép rỗng, cột kỳ là chuỗi tự do (hai cách viết cùng một kỳ sẽ lọt qua UNIQUE theo kỳ), PostgreSQL không tự tạo index cho khóa ngoại, ba index trùng với UNIQUE, và CASCADE từ người dùng xuống bảng vai trò. Ngoài ra, naming strategy mặc định của Spring không tách chữ viết tắt (`maNVBQL` thành `manvbql`), và cách chặn tràn mã ở spec 0001 (CHECK độ dài) không có tác dụng vì `lpad` cắt số về đúng độ dài.

Nhóm 2 người, đồ án học phần, bảo vệ bằng bản demo chạy `docker-compose.prod.yml` trên máy cá nhân. Mô hình dữ liệu là thứ tốn kém nhất nếu phải làm lại, và 12 phân hệ sau đều dựa vào nó.

## Options considered

### Option 1: Giữ cấu trúc gốc, sửa lỗi rõ ràng, chia migration theo nhóm, seed repeatable bật bằng profile

Đưa cả 38 bảng vào 9 file `V` theo nhóm phân hệ, sửa đúng danh sách lỗi đã liệt kê, thêm bảng xác thực theo mẫu bảng con, seed trong `db/seed` chỉ nạp khi bật profile `seed`.

**Pros**:
- Khớp "Done when" của scope; schema đã thiết kế xong nên làm một lần là rẻ nhất.
- Lệch tài liệu nộp ít và có danh sách cụ thể để cập nhật.
- Seed có ở bản demo mà không lọt vào test.

**Cons**:
- 38 bảng nằm sẵn chưa có code dùng; lỗi thiết kế ở các phân hệ sau chỉ lộ ra muộn.
- Profile `seed` là một profile nữa cần nhớ ghép vào `SPRING_PROFILES_ACTIVE`.

### Option 2: Chép y nguyên, một file V1

Chỉ đổi tên sang snake_case và `TIMESTAMPTZ`, tất cả trong một file.

**Pros**:
- Khớp tuyệt đối tài liệu nộp, ít quyết định nhất.

**Cons**:
- Giữ lỗi kỳ tự do, thiếu index khóa ngoại, CASCADE xóa dây chuyền; mỗi phân hệ phải tự vá sau.
- Một file lớn khó review, khó tìm.

### Option 3: Chỉ tạo bảng cho xác thực, phân hệ khác tự thêm khi tới lượt

Đúng Tracer Bullet thuần: mỗi phân hệ mang migration của mình.

**Pros**:
- Không có bảng chưa dùng; mỗi bảng được thiết kế lại cùng lúc với code dùng nó.

**Cons**:
- Lệch "Done when" của scope.
- Thiết kế đã có sẵn và đã được duyệt trong tài liệu; làm lại từng phần không thêm thông tin mới mà rải công việc ra 12 lần.

## Rationale

Option 1 vì schema đã là một thiết kế hoàn chỉnh được duyệt, nên giá trị của Tracer Bullet (học từ code chạy thật trước khi thiết kế tiếp) ở đây thấp: không có gì để học thêm về tên bảng hay quan hệ. Tracer Bullet vẫn áp vào thứ tự làm: nhóm định danh chạy xuyên migration, seed và test trước, các nhóm sau làm dày. Sửa lỗi ngay bây giờ rẻ vì database còn trống; sau khi có dữ liệu, mỗi lỗi là một migration ALTER kèm dọn dữ liệu.

Các lựa chọn nhỏ hơn, mỗi cái kèm phương án đứng thứ hai:
- **Admin và Bảo vệ là bảng con** thay vì bảng vai trò cộng bảng nối: giữ đúng một nguồn vai trò, tính năng 7 không phải hòa giải hai nguồn. Phương án thứ hai (bảng nối) linh hoạt hơn nhưng vai trò nằm hai nơi.
- **Bộ đếm sai là cột trên người dùng** thay vì bảng ghi từng lần sai: một UPDATE, không cần dọn dòng. Đổi lại không chặn được theo IP; nếu cần, tính năng 12 thêm sau.
- **Phiên có `ma_ho_phien`** thay vì bảng đơn giản: thêm một cột UUID bây giờ để tính năng 6 phát hiện được token bị đánh cắp rồi dùng lại (dùng lại một token đã xoay vòng thì thu hồi cả họ). Hai bảng kỹ thuật dùng số tự tăng thay vì mã tiền tố vì sinh nhiều dòng và không ai đọc mã của chúng.
- **Lưu `{id}hash`** thay vì seed bằng code Java: tính năng 5 vẫn tự do chọn bcrypt hay Argon2, vì tiền tố cho biết mỗi hash dùng thuật toán nào và DelegatingPasswordEncoder kiểm tra được cả hai; seed nằm trọn trong SQL.
- **Bật seed bằng profile `seed`** thay vì cờ boolean cộng bean `FlywayConfigurationCustomizer`: chỉ cần một file yml liệt kê thư mục, không cần code. Customizer còn có một bẫy: `locations(...)` thay hẳn danh sách chứ không cộng thêm, viết sai là mất hết bảng. Ý này đến từ lượt kiểm tra chéo.
- **Seed repeatable** thay vì versioned số lớn: một file `V9000` đã chạy làm Flyway từ chối khi sau đó thêm `V10`, trừ khi bật `outOfOrder`. Seed không ghi mã tự sinh mà tra lại bằng số điện thoại, nên không cần `setval` sau khi seed.
- **Chuẩn hóa email ở app cộng CHECK viết thường** thay vì `citext`: không thêm extension hay kiểu cột lạ cho Hibernate.
- **Collation theo cột** thay vì cho cả database: Testcontainers và compose không phải cấu hình locale giống nhau, quên một chỗ là thứ tự lệch âm thầm. Đã thử trên `postgres:18.7`: `vi-VN-x-icu` xếp `Anh, Ánh, Bình, Dương, Đào`, collation `C` xếp `Ánh` và `Đào` xuống cuối.
- **Kỳ dạng `YYYY-MM`** thay vì `DATE` ngày đầu tháng: sắp xếp chuỗi vẫn đúng thứ tự thời gian, ít lệch tài liệu nộp hơn.
- **Chặn tràn mã bằng `MAXVALUE`**: `lpad('100000000', 8, '0')` trả `'10000000'`, trùng với mã đã có; CHECK độ dài vẫn qua. Sequence có `MAXVALUE 99999999 NO CYCLE` thì `nextval` báo lỗi rõ ràng.
- **Không viết entity ở tính năng này**: `ddl-auto=validate` chỉ kiểm tra bảng có entity, nên bảng chưa có entity không gây lỗi; entity viết trước khi biết cách dùng thường phải viết lại.
- **Để tính năng 8 chỉnh nhật ký, tính năng 6 dọn phiên, từng phân hệ quyết cột thời điểm và khóa lạc quan**: các quyết định đó cần ngữ cảnh của tính năng tương ứng; ALTER một bảng còn trống là rẻ.
