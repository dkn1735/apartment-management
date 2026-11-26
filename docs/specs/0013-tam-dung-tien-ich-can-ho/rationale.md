# 0013. Lý do và các phương án

## Context

Bảng `can_ho` có cờ `bi_tam_dung_tien_ich` từ V2, spec 0012 đã đọc và hiện cờ này, nhưng chưa có đường nào ghi nó. API-UC01-09 giao việc bật tắt cờ cho BQL và Kế toán khi căn nợ phí quá hạn; phân hệ tiện ích (UC09) sau này sẽ dựa vào cờ để chặn đặt chỗ.

Các lực định hình quyết định:
- **Chưa có công nợ.** UC04 nằm ở Deferred, nên hệ thống chưa biết căn nào nợ quá hạn. Bất cứ cách nào dựa vào nợ đều chưa chạy được ở lát này.
- **Khóa quyền của cư dân là thao tác nhạy cảm.** Scope yêu cầu bật và tắt đều kèm lý do và đều ghi nhật ký kiểm toán; cư dân bị chặn cần biết vì sao.
- **Schema chỉ có một boolean.** Không có chỗ cho lý do, người đổi hay thời điểm; đặc tả Excel của API này lại chép nhầm body và response từ API khác, nên không dựa vào được.
- **Hai người cùng thao tác.** BQL và Kế toán cùng có quyền, nên có thể cùng đổi một căn gần như cùng lúc từ màn hình đã cũ.
- **Nhóm 2 người, đồ án.** Mỗi lát nên mỏng, dùng lại mẫu đã có (khóa dòng, `JdbcClient`, nhật ký sau commit của spec 0012).

## Options considered

### Option 1: Chỉ đổi cờ, lý do chỉ nằm trong nhật ký

PATCH đổi boolean, lý do chỉ ghi vào chi tiết dòng nhật ký. Không đổi schema.

**Pros**:
- Không migration, lát mỏng nhất.
- Một nguồn lịch sử duy nhất.

**Cons**:
- Trang chi tiết và UC09 muốn hiện lý do phải tra nhật ký theo đối tượng `CanHo:<ma>`, ghép hai phân hệ cho một nhu cầu đọc thường xuyên.
- DB không bảo đảm được "căn bị tạm dừng thì có lý do".

### Option 2: Lần đổi gần nhất trên `can_ho`, lịch sử ở nhật ký (chọn)

Thêm ba cột lý do, thời điểm, người đổi trên `can_ho`, luôn giữ lần đổi gần nhất (cả tạm dừng lẫn mở khóa); CHECK bảo đảm ba cột đi cùng nhau. Nhật ký giữ toàn bộ lịch sử.

**Pros**:
- Trang chi tiết, danh sách và UC09 đọc thẳng từ căn, không gọi `nhatky`.
- DB tự chặn căn bị tạm dừng mà không có lý do.

**Cons**:
- Ba cột thêm trên `can_ho` chỉ cho một tính năng; lý do của lần trước bị đè.
- Một khóa ngoại từ `can_ho` sang `nguoi_dung_he_thong`.

### Option 3: Bảng lịch sử tạm dừng riêng

Bảng `tam_dung_tien_ich` mỗi lần bật tắt một dòng; cờ trên căn đọc từ dòng mới nhất hoặc giữ song song.

**Pros**:
- Lịch sử đầy đủ có cấu trúc, truy vấn được bằng SQL thường.

**Cons**:
- Trùng vai với nhật ký kiểm toán dùng chung (spec 0007).
- Thêm một bảng, một repository và câu hỏi đồng bộ giữa cờ và dòng mới nhất.

### Option 4: Tự động theo công nợ

Không có API bật tay; UC04 tính nợ và tự đặt cờ.

**Pros**:
- Không lệch giữa cờ và nợ thật.

**Cons**:
- UC04 chưa có, nên lát này không giao được gì và scope bị chặn.
- Vẫn cần đường tay cho ngoại lệ (gia hạn, khiếu nại).

## Rationale

Option 2 vì nhu cầu đọc lý do (trang chi tiết, cư dân, UC09) thường xuyên hơn nhiều so với nhu cầu xem lịch sử, nên lý do nên nằm cạnh cờ; lịch sử đã có chỗ ở nhật ký dùng chung, nên không cần bảng riêng như Option 3. Option 1 rẻ hơn một migration nhưng bắt `canho` đọc `nhatky` cho mỗi lần xem chi tiết và để DB không biết luật "tạm dừng thì có lý do". Option 4 chờ một phân hệ chưa có; thay vào đó service đổi cờ là điểm vào duy nhất để UC04 gọi lại sau.

Giữ lần đổi gần nhất (cả mở khóa) thay vì chỉ lần tạm dừng đang hiệu lực, để sau khi mở khóa vẫn thấy ai mở và vì sao mà không tra nhật ký. Từ chối yêu cầu không đổi bằng 409 (thay vì 200 không làm gì như PUT sửa căn) vì ở đây yêu cầu mang một lý do mới: bỏ lặng lẽ lý do đó, hay đè lên lý do gốc, đều làm mất thông tin; 409 cũng báo cho người dùng rằng màn hình đã cũ. Nhờ đó chiều của lần đổi gần nhất luôn suy ra được từ cờ, không cần cột chiều riêng.

Cư dân thấy lý do nhưng không thấy tên nhân viên: lý do giúp cư dân tự xử lý (đi trả phí), còn tên nhân viên không giúp gì và dễ thành khiếu nại cá nhân. BQT chỉ đọc vì vai trò giám sát, đúng với đặc tả gốc chỉ nêu BQL và Kế toán.

Các điểm nhỏ mình tự chốt khi viết:
- Hai loại nhật ký riêng thay vì một loại "Đổi quyền tiện ích": lọc được riêng các lần khóa trên màn hình tra cứu (engineer chọn).
- Lý do tối đa 500 ký tự tính theo code point để khớp `char_length` của PostgreSQL; phương án thua là đếm UTF-16 của Java, sẽ lệch với emoji.
- Một hàm `NguoiDoiTienIch` gom mã rồi gọi `hoTenTheoMa` một lần mỗi response, như `NhatKyTraCuuService`; phương án thua là join bảng `nguoi_dung_he_thong` trong `CanHoQuery`, nhanh hơn một truy vấn nhưng phá luật phân hệ chỉ gọi nhau qua service.
- Không có hộp xác nhận thứ hai trong dialog: ô lý do bắt buộc đã là bước cân nhắc, và thao tác đảo ngược được ngay.
- Seed dùng `UPDATE ... WHERE tien_ich_doi_luc IS NULL` thay vì sửa dòng `INSERT`, vì `ON CONFLICT DO NOTHING` sẽ không đổi căn đã có trên DB dev.
