# 0010. Lý do và các phương án: quên mật khẩu qua email

Spec build: [index.md](index.md).

## Context

Hôm nay người quên mật khẩu chỉ có một đường: nhờ Admin hoặc BQL cấp mật khẩu tạm (spec 0008), rồi bị bắt đổi ở lần đăng nhập đầu. Với vài trăm cư dân, việc này tốn công ban quản lý và để người dùng chờ. Scope (tính năng 13, mức GA) yêu cầu tự đặt lại qua email: link hoặc mã dùng một lần và có hạn, phản hồi giống nhau dù email có tồn tại hay không, đặt lại xong thì thu hồi mọi phiên cũ.

Các lực tác động. Trang này công khai, ai cũng gọi được, nên nó là một cửa để dò email nào có tài khoản (qua nội dung phản hồi, qua thời gian phản hồi) và để spam hộp thư người khác. Spec 0004 và 0009 đã tốn công để đăng nhập không lộ tài khoản; trang quên mật khẩu không được mở lại lỗ đó. Người cầm link đặt lại có quyền như chủ tài khoản, nên link phải khó đoán, sống ngắn, dùng một lần, và không rò qua log hay header. Dự án chưa có hạ tầng gửi email nào (spec 0001 để email cho tính năng này chốt), không có tên miền riêng, và nhóm chỉ hai người trên Windows, nên mọi thứ thêm vào phải chạy được bằng `docker compose` và biến môi trường.

Dữ liệu sẵn có: spec 0002 đã tạo bảng `yeu_cau_dat_lai_mat_khau` (token lưu dạng SHA-256, có hạn, có lúc dùng). Các luồng đổi mật khẩu, Admin đặt lại, khóa tài khoản và thu hồi phiên đã có (spec 0005, 0008, 0009), và spec 0009 để lại câu hỏi đặt lại qua email có xóa khóa tạm hay không.

Nếu không quyết định, tính năng 13 không build được, và mỗi phân hệ sau cần gửi email (thông báo phí, sự cố) sẽ tự chọn một cách gửi riêng.

## Options considered

### Option 1: Link một lần, SMTP qua `spring-boot-starter-mail`, gửi sau commit trên luồng nền (chọn)

Email chứa link với token 256 bit trong fragment (`#token=`); DB lưu SHA-256. Ghi dòng yêu cầu trong transaction, sau commit thì gửi email trên virtual thread, phản hồi trả ngay. Giới hạn theo tài khoản đếm bằng chính bảng yêu cầu. Dev dùng Mailpit, demo dùng Gmail SMTP, test dùng GreenMail.

**Pros**:
- Thời gian SMTP không nằm trong phản hồi, nên không dò được email qua thời gian.
- Không bảng mới, không hạ tầng mới ngoài một container dev; SMTP là chuẩn chung, đổi nhà cung cấp chỉ cần đổi biến môi trường.
- Token đủ dài nên không cần đếm số lần đoán sai; fragment không lọt vào log máy chủ hay `Referer`.

**Cons**:
- Email mất nếu backend dừng ngay sau commit hoặc SMTP lỗi; không tự gửi lại.
- Thêm `@Async` và một lớp lắng nghe sự kiện, khó theo dõi hơn một lời gọi thẳng.

### Option 2: Bảng outbox và job gửi lại

Như Option 1, nhưng email chờ trong một bảng `email_cho_gui`; một job quét mỗi vài giây, gửi và thử lại khi lỗi.

**Pros**:
- Bền: email sống qua cả lúc backend khởi động lại và SMTP tạm lỗi.
- Có lịch sử gửi để điều tra "tôi không nhận được email".

**Cons**:
- Thêm một bảng, một job, logic thử lại và chống gửi trùng; token thô phải nằm trong bảng chờ (hoặc mã hóa), trái với quy tắc không lưu token thô của spec 0002.
- Nhiều phần chuyển động cho một email mà người dùng có thể tự yêu cầu lại sau 60 giây.

### Option 3: Gửi đồng bộ, đệm thời gian phản hồi

Gửi email ngay trong request, rồi kéo mọi phản hồi tới cùng một mức tối thiểu (ví dụ 1,5 giây).

**Pros**:
- Đơn giản nhất về luồng: không sự kiện, không luồng nền; lỗi SMTP hiện ngay trong log của request.

**Cons**:
- SMTP chậm hơn mức đệm (Gmail có lúc vài giây) thì vẫn lộ; mức đệm cao làm mọi người chờ.
- Giữ một luồng xử lý trong suốt thời gian gửi.

### Option 4: Mã OTP 6 chữ số

Email chứa mã 6 chữ số; người dùng gõ mã cùng mật khẩu mới.

**Pros**:
- Dùng được khi mở email trên máy khác với máy đang đặt lại; không có link để lọt vào đâu.

**Cons**:
- Chỉ một triệu giá trị, nên phải giới hạn số lần nhập sai cho mỗi yêu cầu (thêm cột đếm, thêm trạng thái khóa), và vẫn là mục tiêu dò song song.
- Người dùng phải gõ thêm email ở bước xác nhận để biết mã thuộc tài khoản nào, lại mở ra một chỗ có thể lộ tài khoản.

## Rationale

Lực quyết định là "không lộ tài khoản". Gửi đồng bộ (Option 3) không thể đảm bảo điều đó khi SMTP dao động, nên chỉ còn gửi ngoài request. Giữa luồng nền (Option 1) và outbox (Option 2), cái giá của Option 1 là mất một email hiếm hoi, và người dùng tự sửa được bằng cách bấm gửi lại sau 60 giây; cái giá của Option 2 là một bảng, một job, logic thử lại, và phải giữ token thô trong DB. Ở quy mô một chung cư với hai người phát triển, Option 1 vừa đủ. Khi các phân hệ sau cần email bền (thông báo phí có giá trị pháp lý hơn), outbox có thể thêm vào sau `EmailSender` mà không đổi luồng đặt lại.

Link thay vì OTP (Option 4) vì token 256 bit loại hẳn bài toán đoán mã, và bảng `token_hash` của spec 0002 đã được thiết kế cho đúng cách này. Fragment thay vì query string vì phần sau `#` không bao giờ rời trình duyệt, nên không phải đi chặn token ở log Next.js, log proxy và header `Referer` từng chỗ một.

SMTP qua `spring-boot-starter-mail` thay vì SDK HTTP của một hãng vì nó thuộc hệ Spring Boot đã dùng, cấu hình bằng `spring.mail.*`, và Mailpit, Gmail, GreenMail đều nói SMTP: một đường code chạy ở cả dev, demo và test. Gmail cho demo vì dự án không có tên miền riêng (Resend, SendGrid cần DNS đã xác minh để gửi tới người ngoài); Brevo là phương án thay thế nếu Gmail chặn.

### Quyết định nhỏ đã chốt khi viết spec

- **HTTP 400 cho `AUTH_RESET_TOKEN_INVALID`**, không dùng 410 hay 404: frontend xử lý theo `errorCode`, và một status cho mọi nhánh không cho biết token từng tồn tại hay chưa. Phương án thay thế: 422 như `AUTH_OLD_PASSWORD_INCORRECT`.
- **Token sai định dạng trả `AUTH_RESET_TOKEN_INVALID`, không phải `VALIDATION_FAILED`**: người dùng không gõ token, nên lỗi định dạng chỉ có nghĩa là link hỏng; một mã duy nhất cho giao diện. Phương án thay thế: bean validation báo ở ô `token`.
- **`check` trả `hetHanLuc`**: hữu ích cho trang (có thể hiện giờ hết hạn) mà không lộ gì người cầm link chưa biết. Phương án thay thế: `data` null.
- **Tra token trước bcrypt ở `confirm`**: token sai không tốn CPU; token đúng mới băm. Câu `UPDATE` tiêu token vẫn kiểm lại mọi điều kiện, nên khoảng giữa hai bước không mở ra chạy đua. Phương án thay thế: băm trước rồi làm hết trong transaction (bcrypt khoảng 250 ms giữ kết nối DB).
- **Xóa cookie phiên ở response của `confirm`**: mọi họ phiên đã bị thu hồi, kể cả của trình duyệt này; xóa luôn cookie thì trình duyệt không mang một access token còn sống 15 phút vào trang đăng nhập. Phương án thay thế: để access token hết hạn tự nhiên.
- **Khóa dòng `nguoi_dung_he_thong` khi tạo yêu cầu**: các yêu cầu song song cho cùng tài khoản xếp hàng, nên phép đếm 60 giây và 5 lần/giờ luôn đúng; unique index một phần là lớp bảo vệ cuối. Phương án thay thế: dựa vào lỗi 23505 của index rồi bỏ qua (đếm 5 lần/giờ vẫn có thể lệch).
- **Người thực hiện NULL cho `AUTH_YEU_CAU_DAT_LAI_MAT_KHAU`**: người gửi yêu cầu chưa chứng minh là ai, giống đăng nhập thất bại ở spec 0007. Phương án thay thế: ghi chính tài khoản (sai sự thật nếu kẻ khác gửi).
- **Hai controller**: `QuenMatKhauController` riêng thay vì thêm vào `AuthController`, để giữ mỗi lớp dưới khoảng 200 dòng theo `AGENTS.md`. Cùng tiền tố `/api/v1/auth`. Tiền tố `QuenMatKhau` cho các lớp mới để không nhầm với `DatLaiMatKhauService` có sẵn (Admin đặt lại).
- **Thứ tự khóa tài khoản rồi tới yêu cầu** (thêm sau kiểm tra chéo): bản nháp đầu để `confirm` tiêu token trước rồi mới khóa tài khoản, ngược với luồng yêu cầu, nên hai luồng chạy cùng lúc có thể deadlock. Giờ `confirm` lấy `ma_nguoi_dung` từ câu tra không khóa, khóa tài khoản, rồi mới tiêu token bằng câu `UPDATE` có điều kiện.
- **Executor email có giới hạn và trần chung 100/giờ** (thêm sau kiểm tra chéo): giới hạn theo tài khoản không chặn được kẻ xoay vòng nhiều email có thật; executor mặc định không giới hạn sẽ để kẻ đó đốt hết hạn mức Gmail. Phương án thay thế: chờ giới hạn theo IP (đang Deferred), nhưng tới lúc đó tính năng này đã mở ra Internet.
- **Đọc token vào `useRef`**: React StrictMode ở dev chạy effect hai lần; lần hai sẽ thấy hash đã bị `replaceState` xóa.
- **Hằng giới hạn trong code, không đưa vào cấu hình**: `60 giây`, `5 lần`, `60 phút` như `N` và `T` của spec 0009; chỉ thời hạn link (`password-reset-ttl`) là cấu hình vì nó cũng xuất hiện trong email.
- **`common.email` thay vì một phân hệ `email` ngang hàng**: gửi email là khả năng kỹ thuật không có nghiệp vụ riêng, giống `common.error`, `common.security`.
- **Timeout SMTP 10 giây**: luồng nền không treo vô hạn khi máy chủ SMTP không trả lời.
