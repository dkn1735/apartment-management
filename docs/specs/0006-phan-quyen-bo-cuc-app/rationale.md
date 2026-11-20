# 0006. Lý do: phân quyền theo vai trò và bố cục app

Bản ghi quyết định cho [index.md](index.md). `/develop` không cần đọc file này.

## Context

Spec 0004 đã chốt bảy mã vai trò (`QUAN_TRI_VIEN`, `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`, `KY_THUAT`, `BAO_VE`, `CU_DAN`), suy ra từ việc người dùng có dòng trong bảng vai trò tương ứng. Các mã này nằm trong claim `vaiTro` của access token và được đổi thành authority `ROLE_<mã>`. Nhưng hiện mọi API chỉ yêu cầu đăng nhập: một cư dân gọi thẳng API của kế toán thì không có gì chặn. Hệ thống sắp có thêm 75 API nghiệp vụ, do hai người viết trong nhiều lát, và mỗi API có cột "Phân quyền (RBAC)" riêng trong file đặc tả. Chỉ cần quên khai quyền ở một endpoint là có lỗ hổng, và lỗi kiểu này không lộ ra khi demo.

Có ba loại giới hạn khác nhau trong file đặc tả. Loại thứ nhất là theo vai trò ("BQL, Kế toán"). Loại thứ hai là theo dữ liệu: "Cư dân" ở `GET /apartments/{id}` ngầm hiểu là chỉ căn hộ của mình, còn nhân viên chỉ thấy tòa mình phụ trách (hiện giả định là mọi tòa). Loại thứ ba là các ô không phải vai trò: "Hệ thống", "Cổng thanh toán", "BQL duyệt". Admin chỉ xuất hiện ở đúng một API. Một người có thể có nhiều vai trò cùng lúc (tài khoản seed `0900000006` vừa ở ban quản trị vừa là cư dân).

Ở giao diện, khung app (`AppShell`, spec 0003) đã có chỗ nhận menu nhưng menu hiện chỉ có "Hồ sơ", và `/` chuyển thẳng tới `/ho-so`. Proxy của Next.js chỉ biết có cookie phiên, không biết vai trò, và JavaScript không đọc được token vì cookie là `HttpOnly`. Menu, trang chủ và việc chặn trang đều cần biết vai trò, nhưng chỉ có một nguồn mà trình duyệt đọc được là API hồ sơ (đọc DB, nên có thể lệch với token tối đa 15 phút).

Chưa có API nghiệp vụ nào, nên muốn chứng minh việc chặn chạy thật xuyên DB, API và giao diện thì lát này phải tự mang theo một API thật. Đây cũng sẽ là API danh sách đầu tiên của dự án, nên nó kéo theo helper phân trang mà spec 0001 đã quy định nhưng chưa ai viết.

## Options considered

### Option 1: Annotation riêng nhận enum, kiểm bằng `HandlerInterceptor` của Spring MVC, test bắt buộc khai

Mỗi handler mang `@ChoPhep({BAN_QUAN_LY, KE_TOAN})`, `@DaDangNhap` hoặc `@CongKhai`. Một `HandlerInterceptor` đọc `@ChoPhep` từ `HandlerMethod` trong `preHandle`, trước khi Spring MVC đọc tham số và body, và ném `AccessDeniedException` khi thiếu vai trò. Một test duyệt mọi handler và báo đỏ khi thiếu khai. Phạm vi dữ liệu đi qua một service công khai trả 404.

**Pros**:
- Gõ sai tên vai trò là lỗi biên dịch, vì giá trị là enum `VaiTro` chứ không phải chuỗi.
- Quên khai là build đỏ, nên mặc định an toàn mà không phải chặn lúc chạy.
- Kiểm trước khi đọc tham số, nên người sai vai trò luôn nhận 403, không bao giờ nhận 400 tiết lộ endpoint có thật.
- Không proxy controller, nên không lo method `final`, method package private hay việc tìm annotation trên method của proxy (một lỗi kiểu này sẽ âm thầm cho qua).
- Annotation đọc được bằng reflection, nên OpenAPI tự ghi quyền và test tự kiểm được; lỗi từ chối đi qua `GlobalExceptionHandler` có sẵn.

**Cons**:
- Là quy ước riêng của dự án; người mới phải học thay cho `@PreAuthorize` quen thuộc.
- Chỉ diễn đạt được "có một trong các vai trò"; luật phức tạp hơn phải viết trong service.
- Chỉ bảo vệ lời gọi HTTP; service gọi từ job hay phân hệ khác không qua lớp kiểm này.
- Thêm khoảng 6 lớp nhỏ và một test duyệt handler.

### Option 2: `@PreAuthorize` với chuỗi SpEL

`@PreAuthorize("hasAnyRole('BAN_QUAN_LY','KE_TOAN')")` trên mỗi handler, có sẵn trong Spring Security.

**Pros**:
- Không phải viết gì ở hạ tầng, ai biết Spring Security cũng biết.
- Diễn đạt được luật phức tạp ngay tại chỗ (gọi bean, so tham số).

**Cons**:
- Gõ sai tên vai trò (`BAN_QUAN_LI`) thì biên dịch vẫn qua và endpoint im lặng chặn mọi người; chỉ test của đúng endpoint đó mới bắt được.
- Test bắt buộc khai và OpenAPI phải phân tích chuỗi SpEL để biết vai trò.
- Mở cửa cho mỗi người viết quyền một kiểu, khó rà khi có 80 endpoint.

### Option 3: Bảng URL tập trung trong `SecurityConfig`

`requestMatchers(GET, "/api/v1/buildings").hasAnyRole(...)` cho mọi endpoint trong một file của `common`.

**Pros**:
- Nhìn thấy toàn bộ ma trận quyền ở một chỗ, giống hệt file đặc tả.
- Chặn ở tầng filter, trước cả khi vào Spring MVC.

**Cons**:
- Mỗi phân hệ phải sửa một file chung của `common`, trái quy tắc ranh giới phân hệ và dễ xung đột khi hai người cùng làm.
- Mẫu đường dẫn tách khỏi controller nên dễ lệch (đổi path ở controller mà quên sửa bảng thì endpoint chỉ còn yêu cầu đăng nhập).
- Không tự kiểm được endpoint nào chưa có dòng trong bảng.

### Option 4: Quyền lưu trong DB (bảng quyền, bảng gán quyền cho vai trò)

Thêm bảng `quyen` và `vai_tro_quyen`, endpoint khai mã quyền, Admin chỉnh quyền trên màn hình.

**Pros**:
- Đổi quyền không cần deploy.
- Hợp với hệ thống nhiều khách hàng có ma trận quyền khác nhau.

**Cons**:
- Đồ án có một ma trận cố định do file đặc tả quy định; không ai cần chỉnh quyền lúc chạy.
- Thêm migration, seed, màn hình quản lý và một truy vấn (hoặc cache) ở mỗi request.
- Quyền đổi mà không qua review code là rủi ro hơn là tiện.

## Rationale

Lực quyết định là số lượng: 75 endpoint sắp tới, hai người viết, rải qua nhiều lát. Ở quy mô đó, lỗi hay gặp không phải là luật quyền sai mà là quên khai hoặc gõ sai tên vai trò, và cả hai đều không lộ ra khi demo. Option 1 biến cả hai thành lỗi build (enum và test duyệt handler), điều mà Option 2 chỉ làm được nếu thêm một trình phân tích SpEL, còn Option 3 không làm được vì bảng URL tách khỏi controller. Option 1 cũng giữ quyền ngay cạnh endpoint trong package của phân hệ, đúng quy tắc "phân hệ không sửa code của nhau" trong `AGENTS.md`, điều mà Option 3 vi phạm. Option 4 giải một bài toán dự án không có (ma trận cố định theo file đặc tả) với cái giá là schema và một truy vấn mỗi request.

Bản nháp đầu của spec gắn annotation vào method security của Spring Security (`AuthorizationManagerBeforeMethodInterceptor` bọc controller bằng proxy). Lượt đọc chéo chỉ ra rằng Spring MVC đọc và kiểm tham số, body trước khi gọi vào proxy, nên người sai vai trò gửi body sai sẽ nhận 400 trước 403. Vì vậy spec chuyển sang `HandlerInterceptor`, vốn chạy trước bước đọc tham số, đơn giản hơn và bỏ được proxy. Cái mất là không còn bảo vệ được lời gọi không qua HTTP, nhưng ở dự án này mọi lời gọi từ người dùng đều qua HTTP.

Điểm yếu của Option 1 là chỉ diễn đạt được "có một trong các vai trò". Nhưng chính file đặc tả cho thấy các luật phức tạp hơn ("BQL duyệt", "chỉ căn hộ của mình") đều là luật nghiệp vụ hoặc luật dữ liệu, cần đọc DB, nên chúng thuộc service dù chọn cách nào. Vì vậy spec tách hai lớp: vai trò ở annotation (rẻ, đọc từ token, chặn trước khi chạy logic), phạm vi dữ liệu ở `PhamViTruyCapService` (đọc DB khi cần). Dữ liệu ngoài phạm vi trả 404 thay vì 403 để không ai dò được mã căn hộ nào có thật (chống IDOR, tức truy cập dữ liệu của người khác bằng cách đoán mã). Phạm vi đọc DB thay vì đưa vào token vì hồ sơ cư trú đổi theo ngày và sai phạm vi nặng hơn tốn một truy vấn.

Ở giao diện, menu, trang Tổng quan và việc chặn trang đều đọc cùng một bảng cấu hình, nên chúng không thể lệch nhau, và một trang mới không thể quên bị chặn vì việc chặn nằm ở layout. Vai trò lấy từ query hồ sơ đang có thay vì một API menu mới, vì giao diện chỉ là lớp tiện dụng, backend mới là nơi quyết định. Khoảng lệch tối đa 15 phút giữa DB và token được xử lý bằng cách hiện đúng trạng thái "Không có quyền truy cập" khi API trả 403, thay vì cố đồng bộ hai nguồn. Chọn `GET /api/v1/buildings` làm API chứng minh vì entity `ToaNha` đã có, đây là API đầu tiên của UC01 nên không phải công làm bỏ, và nó cho cơ hội dùng thật service phạm vi lẫn helper phân trang ngay từ lát đầu.

Các điểm nhỏ mình tự quyết khi viết spec (bạn có thể đổi):
- Ba annotation và bộ kiểm quyền nằm trong `auth/phanquyen` thay vì `common/security`, vì chúng phụ thuộc enum `VaiTro` của `auth`, và `common` không nên phụ thuộc vào một phân hệ. Phương án thay thế: chuyển `VaiTro` vào `common`.
- `@DaDangNhap` và `@CongKhai` không làm gì lúc chạy, chỉ là khai báo để test và OpenAPI đọc; việc yêu cầu đăng nhập vẫn do `anyRequest().authenticated()` và `PublicPaths`. Phương án thay thế: interceptor cũng kiểm hai annotation này, đổi lại là hai nơi cùng quyết định một việc.
- Không bật `@EnableMethodSecurity`, và test cấm `@PreAuthorize` trên handler: không có method security thì `@PreAuthorize` sẽ âm thầm không chạy, nên cấm bằng test để lỗi lộ ra ngay.
- `PublicPaths` công khai danh sách cặp (method, mẫu) để test so được hai chiều, thay vì test thử bằng request giả trên một matcher không đọc được bên trong.
- Phạm vi tòa chỉ cho năm vai trò nhân viên, không cho Admin, để đúng với "Admin không có quyền ngầm"; trang hồ sơ của Admin vì vậy hiện "Không có" ở mục tòa nhà.
- Lỗi `FORBIDDEN` của query đi qua một helper `QueryErrorState` mới thay vì sửa `ErrorState`, để các chỗ đang truyền tiêu đề riêng cho `ErrorState` không bị đổi nội dung.
- Ba tham số phân trang nhận dạng chuỗi và tự đọc, để `page=abc` trả `VALIDATION_FAILED` như spec 0001 chứ không phải `MALFORMED_REQUEST`.
- Phạm vi nhớ theo request bằng attribute của request, thay vì bean `@RequestScope`, cho dễ thay bằng fake trong unit test.
- `GET /buildings` viết bằng `JdbcClient` (như `VaiTroQuery`) thay vì Spring Data, vì cần đếm căn hộ qua hai bảng chưa có entity và cần map sort từ danh sách trắng sang cột.
- Mục trùng giữa các vai trò hiện ở nhóm vai trò đứng trước theo thứ tự của spec 0004, để không có hai link giống nhau và trình đọc màn hình không đọc lặp.
