# 0007. Lý do và các phương án

## Context

Hệ thống có dữ liệu cá nhân (số điện thoại, email, CCCD) và về sau có tiền (phí, thanh toán), nên cần biết ai đã làm gì và lúc nào. File đặc tả API yêu cầu ghi nhật ký cho đăng nhập (cả thành công lẫn thất bại), đăng xuất và đổi mật khẩu, và có API-AUTH-05 để tra cứu. Spec 0004 và 0005 đã hoãn việc này sang tính năng 8; hiện các sự kiện chỉ nằm trong log ứng dụng, vốn mất khi xoay file log và không tra cứu được trên giao diện.

Bảng `nhat_ky_he_thong` có từ V9 nhưng chưa dùng được cho đăng nhập sai: `ma_nguoi_dung` đang `NOT NULL` trong khi tài khoản không tồn tại thì không có người để gắn. `chi_tiet_thay_doi` là `TEXT` tự do, chưa có IP hay request id.

Các lực định hình thiết kế:
- Ghi nhật ký lỗi không được làm hỏng thao tác chính (scope tính năng 8).
- Đăng nhập sai luôn kết thúc bằng một exception, nên dòng nhật ký phải sống dù thao tác chính "thất bại". Ngược lại, thao tác bị rollback không được để lại dòng "thành công".
- Các luồng hiện có trộn cả hai kiểu: `DangNhapService` không có transaction (bcrypt chạy ngoài transaction), `DangXuatService` có transaction, `LamMoiPhienService` có transaction commit dù ném `ApiException` (`noRollbackFor`).
- Mười hai phân hệ sau sẽ cắm vào cùng cơ chế, nên cách gọi phải ngắn và khó dùng sai, nhất là chuyện lỡ ghi mật khẩu hay token.
- Quy mô nhỏ (một tổ hợp chung cư), nhóm hai người, không có hạ tầng hàng đợi.

## Options considered

### Option 1: Ghi đồng bộ trong transaction riêng, hoãn tới sau commit khi đang có transaction

`ghi` mở một transaction mới (`REQUIRES_NEW`) để lưu dòng; nếu nơi gọi đang trong transaction thì đăng ký ghi ở `afterCommit`. Mọi lỗi bị bắt và thành log ERROR.

**Pros**:
- Đăng nhập sai (ngoài transaction) được ghi ngay; thao tác rollback không để lại dòng.
- Một hàm, đồng bộ, dễ test bằng Testcontainers, giữ được MDC và request hiện tại.

**Cons**:
- Có cửa sổ nhỏ mất dòng nếu tiến trình chết giữa commit và lúc ghi.
- Tốn thêm một kết nối DB ngắn cho mỗi sự kiện hoãn.
- Sự kiện thất bại xảy ra bên trong một transaction sẽ bị rollback thì không được ghi (hiện không có trường hợp nào như vậy).

### Option 2: Chung transaction với thao tác

Lưu dòng nhật ký bằng repository trong transaction của nơi gọi.

**Pros**:
- Nguyên tử tuyệt đối: dữ liệu và nhật ký cùng còn hoặc cùng mất.
- Ít code nhất.

**Cons**:
- Lỗi ghi nhật ký làm hỏng thao tác chính, trái yêu cầu scope.
- Đăng nhập sai không có transaction; sự kiện thất bại trong transaction rollback thì mất theo.

### Option 3: Sự kiện Spring và `@TransactionalEventListener(AFTER_COMMIT)`

Nơi gọi phát một `ApplicationEvent`, một listener ghi sau commit.

**Pros**:
- Nơi gọi không phụ thuộc trực tiếp vào service nhật ký.
- Không bao giờ ghi cho thao tác đã rollback.

**Cons**:
- Mặc định listener không chạy khi không có transaction, nên đăng nhập sai cần `fallbackExecution` hoặc nhánh riêng; luồng khó đọc hơn một lời gọi thẳng.
- Lỗi trong listener và việc listener tự cần transaction mới vẫn phải xử lý như Option 1, nên không bớt được phần khó.

### Option 4: Bất đồng bộ (`@Async` hoặc hàng đợi)

Đưa sự kiện vào thread pool hoặc hàng đợi, ghi sau.

**Pros**:
- Không thêm độ trễ nào vào request.

**Cons**:
- Mất sự kiện khi tắt máy hoặc đầy hàng đợi; mất MDC và request hiện tại nếu không chép tay.
- Thêm hạ tầng và trạng thái cho một khối lượng vài nghìn dòng mỗi tháng; test phải chờ.

## Rationale

Option 1 là phương án duy nhất thỏa cả hai yêu cầu ngược chiều: ghi được sự kiện của thao tác thất bại (đăng nhập sai) và không ghi sự kiện của thao tác chưa xảy ra (rollback). Option 2 phạm thẳng yêu cầu "không làm hỏng thao tác chính". Option 3 cuối cùng phải làm lại đúng phần khó của Option 1 (transaction mới trong `afterCommit`, bắt lỗi) mà thêm một tầng gián tiếp. Option 4 giải một bài toán độ trễ mà hệ thống không có, đổi lại khả năng mất dữ liệu cao hơn.

Cửa sổ mất dòng của Option 1 (chết tiến trình đúng giữa commit và ghi) là rủi ro chấp nhận được ở quy mô này, và vẫn còn log ERROR hoặc log INFO hiện có làm dấu vết. Muốn đóng hẳn cửa sổ đó thì cần outbox (bảng đệm ghi chung transaction rồi chuyển sau), quá tay cho đồ án.

Các lựa chọn nhỏ đi kèm, đã chốt với bạn trong buổi thiết kế:

| Chỗ quyết | Chọn | Phương án đứng sau | Vì sao |
|---|---|---|---|
| Sự kiện gắn ngay | đăng nhập thành công và thất bại, đăng xuất, dùng lại token | thêm làm mới thành công | làm mới mỗi 15 phút mỗi tab làm loãng nhật ký mà không thêm giá trị kiểm toán |
| Phía đọc | chốt hợp đồng ở đây, tính năng 9 làm | chỉ phía ghi | tính năng 9 không có tag needs a decision |
| Tài khoản lạ | `ma_nguoi_dung` `NULL`, không lưu chuỗi đã nhập | lưu chuỗi che bớt | người dùng hay gõ nhầm mật khẩu vào ô tài khoản; giữ spec 0004 AC-16 |
| Cột thêm | IP, request id | thêm user agent, cột kết quả | IP đủ để điều tra dò mật khẩu; request id nối với log ứng dụng; kết quả đã nằm trong mã loại |
| `loai_thao_tac` | mã cố định viết hoa, nhãn ở enum | chữ tiếng Việt như xlsx | lọc ổn định, không lệch NFC, đổi câu chữ không vỡ dữ liệu cũ, giống cách `errorCode` làm |
| `chi_tiet_thay_doi` | JSONB có kiểu | giữ TEXT | có cấu trúc để hiển thị và để kiểm tên khóa nhạy cảm |
| Đối tượng | một cột `Loai:ma` | hai cột | khớp ví dụ xlsx; API-AUTH-05 không lọc theo đối tượng; tách sau vẫn rẻ |
| Bất biến | ứng dụng + trigger | chỉ ứng dụng; tách quyền DB | trigger rẻ và chặn cả SQL tay; tách user DB nặng cho đồ án |
| Lưu giữ | vô thời hạn | dọn sau 1 hoặc 5 năm | khối lượng nhỏ; khớp trigger chặn xóa |
| Giữ bí mật | record có kiểu + kiểm khóa lúc ghi + test quét | chỉ bộ lọc khóa; chỉ review | chặn lúc biên dịch, lúc chạy và lúc build |
| Vị trí code | phân hệ `nhatky`, một enum chung | mỗi phân hệ một enum | API tra nhãn và kiểm mã lọc từ một chỗ; interface chỉ đáng khi có cơ chế gom |
| IP và request id | service tự lấy qua `NguCanhYeuCau` | nơi gọi truyền vào | nơi gọi không phải kéo thông tin web xuống; thay được trong test |
| Họ tên khi đọc | tra theo lô qua service của `auth` | chụp lúc ghi; JOIN thẳng | đúng quy ước phân hệ, không thêm dữ liệu cá nhân lặp lại |
| Danh mục cho bộ lọc | endpoint `action-types` | frontend giữ bản sao | một nguồn duy nhất |
| Phạm vi đọc | cả ba vai trò xem toàn bộ, kể cả IP | BQL theo tòa; chỉ Admin thấy IP | khớp RBAC của xlsx; sự kiện xác thực không gắn tòa |
| Ghi lỗi | log ERROR, không thử lại | thử lại một lần; ghi ra file | thử lại làm chậm request đúng lúc DB có vấn đề |
| Đăng nhập thất bại | một loại, lý do trong chi tiết | mỗi lý do một loại | lọc một lần thấy mọi lần thất bại |
| Mã cho tính năng 10, 11 | chưa thêm vào enum, spec ghi sẵn tên | thêm luôn | bộ lọc không hiện loại chưa bao giờ xảy ra |
| Ghi lượt xem | không | có | làm loãng nhật ký |
| Ngập dòng do dò tài khoản | chấp nhận, ghi Follow-up | gộp dòng trùng | gộp cần UPDATE, trái trigger |

Những chỗ mình tự quyết khi viết spec (bạn đổi được):
- **Đối tượng khi không biết tài khoản** là `NguoiDungHeThong` không có mã, thay vì bỏ `NOT NULL` của cột: giữ cột luôn có giá trị để lọc và hiển thị đơn giản. Phương án đứng sau: cho cột rỗng.
- **Đối tượng của sự kiện dùng lại token** là người dùng, còn `maHoPhien` nằm trong chi tiết: lọc theo người thấy được cả chuỗi đăng nhập, đăng xuất và bị đánh cắp. Phương án đứng sau: `PhienDangNhap:<uuid>`.
- **`TransactionTemplate` thay cho `@Transactional`** trên `ghi`: tránh lỗi gọi nội bộ không qua proxy, và `afterCommit` bắt buộc cần transaction mới. Phương án đứng sau: tách một bean `NhatKyGhiDb` mang `@Transactional(propagation = REQUIRES_NEW)`.
- **Ghi bằng `JdbcClient`, không có entity JPA** (đổi sau lượt kiểm tra chéo): service tự chuyển JSON để kiểm tên khóa, rồi `CAST(:chiTiet AS jsonb)` trong câu `INSERT`. Cách này bỏ được các điểm chưa chắc chắn của Hibernate với JSONB và Jackson 3, chuyện sinh `ma_log` kiểu `VARCHAR` có default, và giống phía đọc. Phương án đứng sau: entity có field `String` gắn `@JdbcTypeCode(SqlTypes.JSON)`.
- **Bỏ `soPhienThuHoi` khỏi sự kiện dùng lại token**: index duy nhất một phần chỉ cho một dòng còn hiệu lực mỗi họ phiên, nên giá trị luôn là 1. Đổi mật khẩu (tính năng 10) thu hồi nhiều họ phiên nên vẫn giữ trường này.
- **Quy tắc tên khóa nhạy cảm theo từ** (tách camelCase và `_`) thay vì tìm chuỗi con: tránh báo nhầm như `footprint` chứa `otp`. Phương án đứng sau: regex chuỗi con.
- **Pool kết nối 20**: sự kiện hoãn sau commit giữ hai kết nối cùng lúc. Phương án đứng sau: gọi `ghi` của đăng xuất từ controller (ngoài transaction), nhưng như vậy nơi gọi phải biết chuyện transaction, trái mục tiêu một lời gọi.
- **Đường dẫn danh mục** là `/action-types`, theo kiểu tiếng Anh của các đường dẫn trong xlsx (`audit-logs`). Phương án đứng sau: `/loai-thao-tac`.
- **`maNguoiDung` sai định dạng trả 400** thay vì trả trang rỗng: báo sớm lỗi gõ, cùng cách xử lý với `page`, `size`, `sort`. Phương án đứng sau: chấp nhận mọi chuỗi.
- **Phía đọc dùng `JdbcClient`** như `ToaNhaQuery`: bộ lọc động và sort theo danh sách trắng viết SQL rõ hơn JPA Criteria. Phương án đứng sau: Spring Data `Specification`.
