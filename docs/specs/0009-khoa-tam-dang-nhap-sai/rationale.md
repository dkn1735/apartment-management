# 0009. Lý do và các phương án: khóa tạm khi đăng nhập sai nhiều lần

Bản ghi quyết định cho [index.md](index.md). `/develop` không cần đọc file này.

## Context

Đăng nhập (spec 0004) hiện chấp nhận số lần thử không giới hạn: mỗi lần sai chỉ tốn khoảng 250 ms bcrypt và trả 401. Với tài khoản cư dân, định danh là số điện thoại (dễ đoán, thường công khai trong nhóm cư dân) và mật khẩu thường yếu, nên dò mật khẩu bằng cách thử liên tục là rủi ro thật. Spec 0002 đã chuẩn bị sẵn hai cột `so_lan_dang_nhap_sai` và `khoa_tam_den` trên `nguoi_dung_he_thong`, ghi rõ khóa tạm độc lập với trạng thái `Tạm khóa` do người quản lý đặt; spec 0008 đã đặt lại hai cột này khi mở lại tài khoản.

Điều kiện Done của scope có bốn ý: vượt ngưỡng thì bị chặn kể cả khi nhập đúng mật khẩu; tới hạn thì tự mở; đăng nhập đúng thì bộ đếm về 0; Admin mở sớm được; và thông báo lỗi không tiết lộ tài khoản có tồn tại hay không. Ý cuối là ràng buộc khó nhất: nếu chỉ tài khoản có thật bị khóa, sau 5 lần sai kẻ dò thấy 429 với số có tài khoản và 401 mãi với số không có, tức là có một công cụ liệt kê số điện thoại cư dân.

Hai lực khác: backend chạy song song nhiều request (virtual thread, nhóm kết nối), nên một kiểu "đọc bộ đếm, so mật khẩu, rồi ghi" để lọt nhiều lượt đoán khi gửi dồn; và hệ thống là đồ án của nhóm 2 người, chạy một instance, nên mọi hạ tầng mới (Redis, bộ giới hạn ngoài) là chi phí thật. Không quyết định thì tính năng 12 không thể bắt đầu, và mỗi tài khoản cư dân vẫn mở cho dò mật khẩu.

## Options considered

### Option 1: Sửa tại chỗ, giữ lượt nguyên tử trước bcrypt, đếm cả định danh ảo (chọn)

Dùng hai cột có sẵn cho tài khoản thật; thêm một bảng nhỏ `dang_nhap_sai_dinh_danh` cho định danh đúng định dạng nhưng không có tài khoản. Mỗi lần thử chạy một câu SQL nguyên tử tăng bộ đếm và tự đặt khóa ở lượt thứ 5, trước khi so mật khẩu; đang khóa thì trả 429 kèm `Retry-After` mà không so mật khẩu.

**Pros**:
- Đáp đủ bốn ý Done, kể cả không lộ tồn tại.
- Chặn được việc gửi song song để lấy thêm lượt.
- Không thêm hạ tầng; chỉ một migration nhỏ, dùng lại `JdbcClient`, `Clock`, `ApiException` có header, job dọn sẵn mẫu.
- Người dùng biết chính xác phải chờ bao lâu.

**Cons**:
- Hai nơi lưu bộ đếm (cột trên tài khoản, bảng định danh) với hai câu SQL gần giống nhau.
- Thêm bảng ghi theo dữ liệu kẻ dò gửi lên, cần job dọn.
- Mỗi lần đăng nhập thành công thêm hai câu ghi.

### Option 2: Luôn trả 401 với một câu gộp

Chỉ khóa tài khoản thật bằng hai cột có sẵn, nhưng trong lúc khóa vẫn trả 401 `AUTH_INVALID_CREDENTIALS` với câu "Thông tin không đúng hoặc tài khoản đang tạm khóa, thử lại sau 15 phút". Không có mã mới, không có bảng mới.

**Pros**:
- Đơn giản nhất, không có bảng, không đổi hợp đồng API.
- Không lộ tồn tại qua mã lỗi.

**Cons**:
- Người nhập đúng mật khẩu trong lúc khóa thấy câu nói có thể là sai, dễ nghĩ mình quên mật khẩu và gọi BQL đặt lại.
- Thời gian phản hồi vẫn có thể lộ: tài khoản đang khóa không chạy bcrypt, tài khoản không tồn tại thì chạy (hoặc phải cố tình chạy bcrypt giả cả lúc khóa).
- Không có số phút còn lại.

### Option 3: 429 chỉ cho tài khoản có thật

Khóa bằng hai cột có sẵn, trả 429 khi khóa; định danh không tồn tại vẫn 401 mãi.

**Pros**:
- Rất ít code, phản hồi rõ ràng.

**Cons**:
- Lộ tồn tại sau đúng 5 lần thử, trái thẳng với điều kiện Done.

### Option 4: Một bảng chung cho mọi định danh, hoặc bộ nhớ trong ứng dụng

Hoặc bỏ không dùng hai cột trên `nguoi_dung_he_thong`, đếm mọi định danh (có thật hay không) trong một bảng; hoặc giữ bộ đếm định danh ảo trong bộ nhớ (Caffeine) thay vì DB.

**Pros**:
- Bảng chung: một cơ chế duy nhất, không phân nhánh.
- Bộ nhớ: không có migration, không có job dọn.

**Cons**:
- Bảng chung: số điện thoại và email của cùng tài khoản thành hai bộ đếm, kẻ dò có 10 lượt thay vì 5; hai cột đã chuẩn bị ở spec 0002 thành thừa, và nút Admin mở khóa phải tìm theo hai hash.
- Bộ nhớ: khởi động lại thì định danh ảo mở còn tài khoản thật vẫn khóa, lộ tồn tại; sai khi chạy nhiều instance.

## Rationale

Ràng buộc "không lộ tồn tại" loại Option 3 ngay. Option 2 giữ được bí mật nhưng đánh đổi trải nghiệm đúng ở nhóm người dùng dễ bị ảnh hưởng nhất (cư dân lớn tuổi nhập đúng mật khẩu mà vẫn bị báo sai) và vẫn phải xử lý cẩn thận chênh lệch thời gian. Option 1 trả phản hồi rõ và đúng cho người dùng thật, còn chi phí giữ bí mật được đẩy sang một bảng nhỏ không ai đọc ngoài luồng đăng nhập. Bảng chung (Option 4) nghe gọn hơn nhưng làm yếu chính mục tiêu chống dò (gấp đôi lượt) và bỏ phí thiết kế đã chốt ở spec 0002; bộ nhớ trong ứng dụng thì mở lại kẽ hở tồn tại mỗi lần khởi động lại.

Giữ lượt trước bcrypt là điểm quyết định về độ đúng. Cách "so mật khẩu xong mới tăng bộ đếm" cho phép gửi 50 request cùng lúc và cả 50 đều được so trước khi bộ đếm kịp chạm 5. Một câu SQL khóa dòng (`FOR NO KEY UPDATE`, cùng kiểu khóa `TaiKhoanQuery` đã dùng để tránh deadlock với khóa ngoại) rồi tăng bộ đếm và tự đặt khóa ở lượt thứ 5 khiến PostgreSQL tuần tự hóa các lượt trên cùng một dòng, nên giới hạn đúng bằng 5 mà không cần khóa bi quan hay transaction dài giữ kết nối trong lúc chạy bcrypt (spec 0004 đã cố ý để bcrypt ngoài transaction). Đặt bộ đếm về 0 ngay khi đặt khóa giúp trạng thái tự phục hồi khi khóa hết hạn mà không cần job nào, và khiến một lần ghi lỡ dở (tiến trình chết sau giữ lượt) không để lại bộ đếm sai.

Bản đầu của spec chốt "bộ đếm không giảm theo thời gian, chỉ về 0 khi đúng" theo lựa chọn ban đầu của bạn. Lượt kiểm chéo bằng model khác chỉ ra rằng như vậy chính bộ đếm thành công cụ dò: tài khoản thật có vài lần gõ nhầm từ tháng trước bị khóa sau ít hơn 5 lần thử, còn định danh ảo luôn cần đủ 5, nên kẻ dò phân biệt được; đồng thời 5 lần gõ nhầm rải trong một năm cũng khóa người dùng thật. Bạn đã đổi sang cửa sổ trượt 15 phút (`lan_sai_cuoi_luc`). Hệ quả kéo theo: dòng định danh ảo không có lần sai nào trong 15 phút tương đương với không có dòng, nên job dọn xóa ngay ở lần chạy kế tiếp thay cho mức giữ 30 ngày đã chọn trước đó (mức 30 ngày chỉ để làm chậm kẻ dò kiên nhẫn, rủi ro này không còn khi cả hai phía cùng tự về 0). Cột `cap_nhat_luc` của bảng mới cũng được thay bằng `lan_sai_cuoi_luc`, cùng vai trò. Cũng sau lượt kiểm chéo, các lần thử bị chặn trong lúc khóa chỉ còn ghi log ứng dụng, không ghi nhật ký kiểm toán, vì lúc khóa không có bcrypt làm chậm nên kẻ dò có thể ghi hàng nghìn dòng mỗi phút vào bảng chỉ được thêm, không được xóa.

Các lựa chọn khác giữ đúng như bạn chọn (5 lần, 15 phút cố định, 429 cho cả định danh ảo, endpoint mở khóa riêng, `SHA-256` thay vì HMAC).

### Quyết định nhỏ đã chốt khi viết spec

- **`N`, `T` là hằng trong code, không phải cấu hình.** Câu gợi ý trên trang đăng nhập cần biết hai số này; đưa vào `application.yml` mà frontend vẫn hằng cứng thì dễ lệch hơn là để cả hai là hằng ghi rõ cùng chỗ trong spec. Phương án kế: thuộc tính `app.security.login-lockout.*` cộng một endpoint công khai trả hai số, chỉ đáng khi có nhu cầu đổi theo môi trường.
- **Lượt thứ 5 tự trả 429, không phải 401.** Người dùng biết ngay mình bị khóa thay vì phải thử thêm lần 6 mới thấy. Phương án kế: 401 ở lần 5, 429 từ lần 6; thêm một lượt đoán cho kẻ dò mà không lợi gì cho người dùng.
- **Lượt cuối mà mật khẩu đúng thì vẫn đăng nhập được.** Giữ lượt đặt khóa trước khi biết kết quả, nhưng mật khẩu đúng đặt lại ngay; chặn nó sẽ phạt người dùng thật ở lần thử thứ 5.
- **Không kéo dài khóa khi có lần thử trong lúc khóa.** Kéo dài khiến kẻ dò giữ tài khoản bị khóa vô hạn chỉ với một request mỗi vài phút, thay vì 5 request mỗi 15 phút.
- **Không chạy bcrypt khi đang khóa, ở cả hai nhánh.** Cả tài khoản thật lẫn định danh ảo chỉ chạy vài câu SQL rồi trả 429, nên đợt dò dồn dập không đốt CPU. Thời gian phản hồi gần như bằng nhau: định danh ảo thêm một câu `INSERT ... DO NOTHING`, chênh lệch cỡ mili giây, nhỏ hơn nhiều so với dao động mạng, nhưng không phải bằng tuyệt đối.
- **Cùng một mẫu SQL cho hai nhánh.** Định danh ảo đảm bảo có dòng trước rồi chạy đúng câu giữ lượt của tài khoản thật, thay vì một câu `INSERT ... ON CONFLICT DO UPDATE` riêng; một mẫu để test song song, một ngữ nghĩa khi khóa.
- **Xóa dòng định danh ảo khi định danh có chủ.** Không xóa thì một số đang bị khóa ảo, ngay khi Admin tạo tài khoản cho nó, chuyển từ 429 sang 401 và kẻ dò biết vừa có tài khoản mới.
- **Chuỗi sai định dạng không bị đếm.** Không tài khoản nào có thể mang chuỗi đó, nên 401 mãi không lộ gì; đếm nó chỉ làm bảng phình theo rác.
- **Đặt lại bộ đếm khi mật khẩu đúng ở cả nhánh 403.** Một quy tắc "mật khẩu khớp thì về 0" dễ hiểu và dễ test hơn ngoại lệ theo trạng thái.
- **Bộ đếm không đụng `phien_ban`, `cap_nhat_luc`.** Nếu không, mỗi lần cư dân gõ nhầm sẽ làm form sửa tài khoản của BQL đang mở bị 409 `CONCURRENT_MODIFICATION`.
- **Mở khóa sớm không có hộp thoại xác nhận.** Thao tác nhẹ, đảo ngược được (người dùng chỉ được thử lại), khác với khóa tài khoản ở spec 0008 cần lý do.
- **Endpoint là `DELETE .../login-lock`.** Xóa một tài nguyên con "khóa đăng nhập" đọc tự nhiên và lặp lại an toàn (gọi hai lần cho cùng kết quả). Phương án kế: `POST .../unlock-login`, kém rõ về tính lặp lại.
- **Hai repository, không interface chung.** Chỉ một service gọi, hai câu SQL khác nhau; interface chỉ thêm một tầng mà không có cách cài thứ ba (quy tắc interface của `AGENTS.md`).
- **Job dọn riêng nhưng dùng chung lịch.** Một lớp một trách nhiệm, không thêm thuộc tính cấu hình mới.
