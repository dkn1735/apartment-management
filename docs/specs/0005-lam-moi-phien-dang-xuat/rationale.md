# 0005. Lý do: làm mới phiên và đăng xuất

Bản ghi quyết định cho [index.md](index.md). `/develop` không cần đọc file này để build.

## Context

Spec 0004 đã cấp access token sống 15 phút và refresh token sống 7 ngày, nhưng chưa có cách dùng refresh token. Hệ quả là cứ 15 phút người dùng bị đưa về trang đăng nhập dù phiên 7 ngày còn hạn, và không có nút đăng xuất: trên máy dùng chung ở phòng ban quản lý, người sau có thể dùng tiếp phiên của người trước. Scope tính năng 6 đặt điều kiện xong: làm mới âm thầm khi người dùng đang thao tác, đăng xuất thì refresh token cũ không dùng được nữa, token hết hạn hoặc bị thu hồi thì phải đăng nhập lại.

Nền đã có sẵn và không nên làm lại: bảng `phien_dang_nhap` một dòng cho mỗi refresh token, cột `ma_ho_phien` (UUID chung cho các token từ một lần đăng nhập), cột `thu_hoi_luc` và `ly_do_thu_hoi` với các giá trị `Đã xoay vòng`, `Dùng lại token cũ`, `Đăng xuất`. Cookie `refresh_token` có `Path=/api/v1/auth`, cookie `phien` cho `proxy.ts`, và client đã có xử lý 401 toàn cục trong `createQueryClient`. Chống CSRF bằng `SameSite=Strict` cộng header `X-Requested-With`.

Các lực cần cân: refresh token là thứ quý nhất trong trình duyệt (7 ngày truy cập), nên lộ nó phải được phát hiện; người dùng thường mở nhiều tab (danh sách hóa đơn, chi tiết căn hộ), và các tab dùng chung cookie nên có thể cùng làm mới một lúc; nhóm có 2 người và một instance backend, không có Redis hay hạ tầng phụ; mọi lời gọi API phía client đi qua một hàm `api` duy nhất. Không quyết thì tính năng 7 trở đi (phân quyền, đổi mật khẩu, khóa tài khoản) không có cách thu hồi phiên để dựa vào.

## Options considered

### Option 1: Xoay vòng mỗi lần làm mới, thu hồi cả họ khi dùng lại, khoan dung 10 giây (sau đổi thành 30 giây và trả lại token, xem *Bổ sung 2026-11-20*)

(Mô tả bản đầu; phần khoan dung đã được thay, xem *Bổ sung 2026-11-20*.) Mỗi lần làm mới cấp refresh token mới cùng `ma_ho_phien`, đánh dấu dòng cũ `Đã xoay vòng`. Token đã xoay mà bị dùng lại quá 10 giây thì thu hồi mọi dòng của họ. Trong 10 giây thì coi là các tab chạy đua: trả 200 không cookie, tab đó gửi lại request với cookie mà tab kia vừa nhận. Phiên trượt 7 ngày, trần 30 ngày tính từ `bat_dau_luc`.

**Pros**:
- Phát hiện được token bị đánh cắp; đúng thứ bảng `phien_dang_nhap` được thiết kế để làm.
- Nhiều tab không đá nhau ra ngoài, mà không cần khóa giữa các tab ở trình duyệt.
- Người dùng tích cực gần như không bao giờ thấy trang đăng nhập, nhưng phiên vẫn có điểm kết thúc.

**Cons**:
- Thêm một dòng mỗi lần làm mới, cần job dọn dẹp và một cột mới.
- Logic phía server có bốn nhánh (thành công, khoan dung, dùng lại, hết hạn hoặc thu hồi), phải test kỹ với đồng hồ giả.
- Khoảng 10 giây là một kẽ hở nhỏ, dù không cấp được token mới trong khoảng đó.

### Option 2: Refresh token không xoay vòng

Một refresh token dùng suốt 7 ngày; làm mới chỉ cấp access token mới. Đăng xuất thu hồi đúng dòng đó.

**Pros**:
- Đơn giản nhất: một dòng mỗi lần đăng nhập, không có chạy đua giữa các tab.
- Không cần dọn dẹp thường xuyên.

**Cons**:
- Token bị lộ dùng được trọn 7 ngày mà không ai biết; cột `Đã xoay vòng`, `Dùng lại token cũ` trong schema thành thừa.
- Không có cách kéo dài phiên cho người dùng tích cực mà không kéo dài luôn cửa sổ bị lợi dụng.

### Option 3: Xoay vòng nghiêm ngặt, khóa giữa các tab ở trình duyệt

Như Option 1 nhưng không có khoan dung: mọi lần dùng lại đều thu hồi cả họ. Client dùng Web Locks API (khóa có tên dùng chung giữa các tab cùng nguồn) để chỉ một tab làm mới tại một thời điểm.

**Pros**:
- Không có kẽ hở thời gian nào ở server.
- Server đơn giản hơn Option 1 một nhánh.

**Cons**:
- Khóa ở trình duyệt không che được mọi trường hợp: request đang bay trước khi lấy khóa, tab bị treo giữa chừng, trình duyệt cũ. Mỗi lần trượt là người dùng bị đăng xuất, và rất khó tái hiện để sửa.
- Logic phân tán ở cả hai đầu, test phía client phức tạp hơn.

### Option 4: Phiên phía server, bỏ refresh token

Bỏ JWT ngắn hạn, mỗi request tra DB theo một mã phiên trong cookie và trượt hạn trực tiếp.

**Pros**:
- Thu hồi có hiệu lực ngay lập tức, không có 15 phút chờ.
- Không có làm mới, không có chạy đua.

**Cons**:
- Đi ngược spec 0004 đã build và test (JWT, `sid`, resolver cookie); phải làm lại một lát đã xong.
- Mỗi request thêm một lần đọc DB; mọi phân hệ sau phụ thuộc vào bảng phiên.

## Rationale

Option 1 được chọn vì nó dùng đúng nền đã xây: schema đã có `ma_ho_phien` và đủ lý do thu hồi, spec 0004 đã chốt cookie và JWT có `sid`. Option 2 bỏ phí phần phát hiện đánh cắp mà schema đã trả giá để có, trong khi refresh token 7 ngày là thứ đáng bảo vệ nhất. Option 4 làm lại một lát đã xong để đổi lấy thu hồi tức thì, điều mà bài toán chung cư ở quy mô này không đòi (15 phút trễ khi khóa tài khoản là chấp nhận được, và lần làm mới kế tiếp đã kiểm lại trạng thái).

Giữa Option 1 và Option 3, khác biệt nằm ở chỗ đặt phần xử lý chạy đua. Các tab dùng chung cookie, nên khi tab A đã xoay xong thì tab B chỉ cần gửi lại request là có cookie mới; server biết chắc dòng đã xoay lúc nào và họ còn sống không, còn trình duyệt thì không. Đặt khoan dung ở server cho một quy tắc kiểm được bằng test tích hợp với `MutableClock`, thay vì một khóa phía client khó tái hiện. Khoảng 10 giây đủ cho một request chậm và không cấp token mới cho ai, nên kẻ cầm token cũ không lợi gì. Khóa phải theo cả họ phiên chứ không theo một dòng: bản nháp đầu dùng `SELECT ... FOR UPDATE` trên dòng được gửi lên, và cross check chỉ ra rằng đăng xuất bằng T1 chạy cùng lúc làm mới bằng T2 thì dòng T3 mới sinh nằm ngoài câu UPDATE của đăng xuất (READ COMMITTED chỉ kiểm lại dòng đã thấy), nên phiên sống lại. Khóa tư vấn `pg_advisory_xact_lock` theo `ma_ho_phien` làm mọi thao tác ghi lên một họ chạy tuần tự mà không cần bảng khóa riêng, và index duy nhất một phần trên dòng chưa thu hồi là chốt chặn ở DB nếu code sau này quên lấy khóa. Phương án dự phòng là `SELECT ... FOR UPDATE` trên mọi dòng của họ, đúng nhưng khóa nhiều dòng hơn và vẫn hở với dòng được chèn sau đó.

Trần 30 ngày cần biết họ phiên bắt đầu lúc nào. Suy ra bằng `MIN(tao_luc)` của họ thì gãy khi job dọn dẹp xóa dòng đầu, nên thêm cột `bat_dau_luc` chép qua mỗi lần xoay. Giữ dòng đã thu hồi 30 ngày (bằng trần) bảo đảm token cũ của một họ còn sống luôn còn dòng để phát hiện dùng lại. Phía client, làm mới khi gặp 401 trong middleware của `openapi-fetch` thay vì hẹn giờ trước khi hết hạn: hẹn giờ trôi trong tab chạy nền và khi máy ngủ, còn nhánh 401 thì vẫn phải có, nên chỉ giữ một đường. Đặt trong middleware giữ nguyên mọi chỗ gọi `api(...)`, đúng quy tắc trong `AGENTS.md` là màn hình chỉ gọi API qua hàm `api`.

Các quyết định nhỏ đã chốt khi viết: một mã `AUTH_REFRESH_INVALID` cho mọi nguyên nhân thất bại (không lộ là dùng lại hay hết hạn; mã riêng để client không phải đoán URL nào lỗi, phương án dự phòng là dùng lại `UNAUTHENTICATED`); đăng xuất công khai và nhận diện bằng cookie refresh để vẫn đăng xuất được khi access token đã hết (phương án dự phòng là đòi access token, buộc làm mới trước khi đăng xuất); `noRollbackFor` để việc thu hồi được commit dù request trả lỗi (phương án dự phòng là một bean `REQUIRES_NEW`, đúng nhưng thêm một lớp); `@Scheduled` trong tiến trình cho job dọn dẹp (một instance nên không cần ShedLock, phương án dự phòng là dọn khi đăng nhập, nhưng bỏ sót người không bao giờ đăng nhập lại).

## Bổ sung 2026-11-20: response làm mới bị mất

**Vấn đề.** Lần kiểm ở bước 5 thấy người dùng bị đưa về `het-phien` khi tải lại trang đúng lúc request làm mới đang chạy. Server đã xoay T1 thành T2 và commit, nhưng trang tải lại hủy response trước khi `Set-Cookie` tới trình duyệt, nên trình duyệt vẫn giữ T1. Thiết kế ban đầu có hai cách hỏng ở đây: trong 10 giây, nhánh khoan dung trả 200 không cookie, request gửi lại vẫn 401 và người dùng bị đá ra; quá 10 giây, T1 bị coi là token bị đánh cắp và cả họ bị thu hồi kèm WARN báo sai. Gốc của lỗi là giả định "cookie mới sẽ tới từ một response khác": đúng với hai tab chạy đua, sai khi response duy nhất mang cookie bị mất. Vì DB chỉ lưu hash, server không thể trả lại T2.

**Các phương án đã cân.**
- *Token kế tiếp tất định* (chọn): T2 = HMAC(`K`, T1). Trong khoảng khoan dung, server tính lại đúng token đang sống và đặt lại cookie. Ưu: làm mới thành idempotent (gọi lại ra cùng kết quả), không thêm cột, hai tab chạy đua nhận cùng cookie nên thứ tự response không còn quan trọng, client bỏ được đoạn chờ 400 ms. Nhược: kẻ cầm token vừa xoay trong khoảng khoan dung nhận được phiên; token phụ thuộc một khóa bí mật.
- *Xoay thêm một lần trong grace*: dùng lại T1 trong grace thì thu hồi T2, cấp T3. Ưu: không cần khóa. Nhược: hai tab chạy đua nhận hai cookie khác nhau, trình duyệt có thể giữ cookie tới sau cùng là T2 đã chết, 15 phút sau làm mới bằng T2 bị coi là dùng lại và cả họ mất. Lỗi này hiếm nhưng không tái hiện được, đúng loại lỗi tệ nhất.
- *Lưu T2 đã mã hóa trong DB*: cùng hiệu quả với HMAC nhưng thêm migration, thêm khóa mã hóa, và đặt dữ liệu đủ để dùng ngay vào DB.
- *Chỉ dùng `keepalive` ở client*: rẻ, nhưng hành vi khác nhau giữa các trình duyệt và không cứu được trường hợp mất mạng giữa chừng. Giữ làm lớp phụ.

**Vì sao chọn HMAC.** Nó sửa đúng gốc lỗi: mọi 200 đều mang cookie dùng được, nên client không còn phải đoán cookie có tới không. Đây cũng là cách các nhà cung cấp lớn làm với "reuse interval" (khoảng dùng lại): trong khoảng đó token cũ vẫn đổi được lấy token đang sống. Kẽ hở mới (kẻ trộm trong 30 giây nhận được phiên) được chặn ngược lại bằng AC-4: hai bên cùng giữ T2 thì bên làm mới sau quá 30 giây sẽ thu hồi cả họ. Khóa `K` suy ra từ `JWT_SECRET` bằng một nhãn cố định, vì lộ `JWT_SECRET` đã là mất trắng (ký được access token tùy ý), nên một biến môi trường riêng chỉ thêm việc cấu hình mà không thêm an toàn thật. Phương án dự phòng là biến `REFRESH_TOKEN_SECRET` riêng.

**Các số đã chốt.** Khoảng khoan dung lên 30 giây (từ 10) vì một lần tải lại trên mạng chậm hoặc khi `next dev` đang biên dịch có thể quá 10 giây; phương án dự phòng là 60 giây, mức trần của validate. Đi chuỗi tối đa 3 bước, để tab mang T1 vẫn nhận được T3 khi tab khác đã xoay tiếp trong 30 giây; quá 3 bước trong 30 giây là bất thường nên trả 401 (phương án dự phòng là chỉ so token kế tiếp trực tiếp). Trường `lamMoi` giữ lại với nghĩa mới để không đổi hình dạng API và để test, log phân biệt được hai nhánh.
