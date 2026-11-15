# 0001. Stack và kiến trúc nền: lý do và các phương án

Phần này là hồ sơ quyết định. `/develop` không cần đọc; spec để build nằm ở [index.md](index.md).

## Context

Đây là đồ án IT3930 của nhóm 2 người: hệ thống quản lý vận hành chung cư cho ban quản lý, kỹ thuật, kế toán, ban quản trị và cư dân. Đã có sẵn hai tài liệu thiết kế: schema PostgreSQL 38 bảng (`sql/database_schema_quan_ly_chung_cu.sql`) và đặc tả 80 REST API (`docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx`). Scope đã chọn Spring Boot, Next.js và PostgreSQL, nhưng chưa chốt phiên bản, cách tổ chức repo, cách truy cập dữ liệu, migration, quy chuẩn API và cách chạy trên máy (scope, tính năng 1).

Các lực tác động: nhóm nhỏ, cả hai đều dùng Windows, làm theo kiểu Tracer Bullet (mỗi lát cắt chạy thật qua DB, API và giao diện). Quy mô người dùng nhỏ (một tổ hợp chung cư), không có yêu cầu tải lớn. Có dữ liệu cá nhân (số điện thoại, email, CCCD) và tiền (phí, thanh toán) nên bảo mật và nhật ký kiểm toán là quan trọng, dù không thuộc một chuẩn tuân thủ cụ thể như PCI DSS (thanh toán trực tuyến qua cổng bên ngoài, chốt ở UC05).

Schema có vài đặc điểm ảnh hưởng tới stack: khóa chính là `VARCHAR(30)`, tên bảng và cột camelCase không có ngoặc kép (PostgreSQL sẽ đổi thành chữ thường), vai trò người dùng theo kiểu kế thừa TPT (mỗi vai trò một bảng con), và giá trị trạng thái là nhãn tiếng Việt có dấu trong CHECK. Đặc tả API đã định tiền tố `/api/v1`, bảng mã HTTP và một vỏ bọc response `{ success, code, message, data, timestamp }`.

Nếu không chốt sớm, mỗi phân hệ sẽ tự chọn dạng response, cách phân trang, cách xử lý giờ, và 80 API sẽ lệch nhau; sửa sau khi đã có nhiều màn hình tốn kém hơn nhiều. Phiên bản cũng gấp: Spring Boot 3.5 đã hết hỗ trợ miễn phí, 4.0 hết vào cuối năm 2026.

## Options considered

### Option 1: Modular monolith Spring Boot 4.1 + JPA + Flyway, Next.js gọi API phía client qua cùng tên miền

Một ứng dụng Spring Boot chia package theo phân hệ, JPA cho CRUD và SQL thuần cho báo cáo, Flyway chạy migration SQL. Next.js App Router với Client Component, TanStack Query và client sinh từ OpenAPI; trình duyệt gọi `/api` trên tên miền Next.js, Next.js chuyển tiếp sang Spring Boot. Một repo, PostgreSQL trong Docker (basis: monolith first cho nhóm nhỏ).

**Pros**:
- Ít thành phần phải chạy nhất: một backend, một frontend, một database.
- Cùng tên miền nên không có CORS, cookie `HttpOnly` dùng được cho token.
- Kiểu dữ liệu đi từ backend sang frontend tự động qua OpenAPI.
- Dòng Spring Boot đang được hỗ trợ suốt thời gian làm đồ án.

**Cons**:
- Spring Boot 4 còn mới, ít ví dụ hơn 3.x.
- Thêm bước sinh kiểu và commit `openapi.json` mỗi khi API đổi.
- `rewrites` khóa địa chỉ backend lúc build image frontend.

### Option 2: Next.js làm BFF, ưu tiên Server Components

Next.js giữ phiên và gọi Spring Boot từ server của Next (BFF, tức backend phục vụ riêng cho frontend). Trình duyệt không bao giờ thấy token. Trang chủ yếu là Server Component, form dùng Server Actions.

**Pros**:
- An toàn nhất cho token, trình duyệt không chạm vào JWT.
- Tải trang đầu nhanh hơn, dùng đúng hướng Next.js đang đẩy.

**Cons**:
- Phải viết thêm một lớp trung gian cho cả 80 API, mỗi API sửa ở hai nơi.
- Hai mô hình dữ liệu (server và client) trộn nhau, khó cho nhóm nhỏ và khó debug.
- Lợi ích SEO và tải trang đầu gần như không có giá trị với app nội bộ sau đăng nhập.

### Option 3: Stack quen thuộc trong hướng dẫn: Spring Boot 3.5, gọi thẳng backend qua CORS, token trong localStorage

Bộ phổ biến nhất trong bài hướng dẫn: Boot 3.5, frontend gọi `localhost:8080` trực tiếp, access token lưu trong localStorage, kiểu TypeScript viết tay.

**Pros**:
- Nhiều tài liệu và ví dụ nhất, dễ tìm lời giải khi bí.
- Không có bước chuyển tiếp hay sinh code.

**Cons**:
- Boot 3.5 đã hết hỗ trợ miễn phí từ 30/06/2026, không còn bản vá bảo mật miễn phí.
- Token trong localStorage bị lộ nếu có lỗi XSS (chèn script).
- Kiểu viết tay cho 80 API dễ lệch giữa hai đầu.

### Option 4: Microservices theo phân hệ + Keycloak

Mỗi phân hệ một service, API gateway phía trước, Keycloak làm máy chủ xác thực.

**Pros**:
- Ranh giới phân hệ bị ép chặt, mỗi phần triển khai độc lập.
- Keycloak có sẵn quên mật khẩu, khóa tài khoản, quản lý phiên.

**Cons**:
- Gấp nhiều lần công sức xây và vận hành cho nhóm 2 người (gateway, nhiều DB, gọi qua mạng, giao dịch phân tán).
- Người dùng nằm trong Keycloak chứ không ở `NguoiDungHeThong`, lệch với schema và đặc tả đã nộp.
- Báo cáo tổng hợp (UC12) phải ghép dữ liệu từ nhiều service.

## Rationale

Lực quyết định là quy mô nhóm và cách làm Tracer Bullet. Với 2 người và một tổ hợp chung cư, chi phí vận hành mỗi thành phần thêm vào lớn hơn mọi lợi ích về mở rộng, nên Option 4 bị loại ngay (basis: monolith first; tránh microservices sớm). Modular monolith giữ được ranh giới phân hệ để chia việc mà không trả giá của hệ phân tán, và có thể thêm Spring Modulith sau để kiểm tra ranh giới bằng test.

Giữa Option 1 và Option 3, phiên bản là điểm quyết định: Boot 3.5 đã hết hỗ trợ miễn phí, và lựa chọn localStorage mà Option 3 thường kéo theo là rủi ro bảo mật thật với hệ thống có dữ liệu cá nhân và tiền (basis: OWASP, không lưu token nhạy cảm ở nơi script đọc được). Đường gọi cùng tên miền của Option 1 mở đường cho cookie `HttpOnly` với chi phí rất thấp (một dòng `rewrites`), còn Option 2 đạt cùng mức an toàn nhưng bắt viết lại một lớp trung gian cho 80 API. Với app nội bộ không cần SEO, Server Components không đáng sự phức tạp đó.

Các quy ước đi kèm bám sát tài liệu đã có: giữ vỏ bọc response và nhãn trạng thái tiếng Việt của đặc tả để code khớp tài liệu nộp cho GVHD, nhưng đổi tên bảng sang snake_case vì camelCase không ngoặc kép trong PostgreSQL sẽ thành chữ thường khó đọc (basis: quy ước đặt tên PostgreSQL). Flyway với SQL thuần được chọn vì file schema có sẵn đã là SQL, và `ddl-auto=validate` giữ cho entity không lệch schema. JPA cho CRUD, SQL thuần cho báo cáo (basis: ORM cho CRUD, SQL cho truy vấn phức tạp). Lưu UTC và tính quy tắc theo giờ Việt Nam tránh lỗi lệch giờ khi container hoặc máy chủ đặt múi giờ khác.

## Ghi chú tra cứu phiên bản (16/11/2026)

- Spring Boot: dòng mới nhất 4.1 (bản vá 4.1.2, 22/10/2026). 4.0 hết hỗ trợ miễn phí 31/12/2026. 3.5 hết hỗ trợ miễn phí 30/06/2026. Spring Boot 4.2 chưa xác nhận.
- Java: 21 và 25 là LTS (theo hiểu biết, chưa xác nhận trên trang chính thức trong lần tra này).
- Next.js: dòng 16, cần Node 20.9 trở lên. Bản vá chính xác chưa xác nhận; kiểm tra bằng `pnpm view next version` lúc scaffold.
- Node.js: 26 là Active LTS; 24 (bản được chọn) và 22 đang ở giai đoạn Maintenance LTS. Node 24 hết giai đoạn Active ngày 20/10/2026 và còn nhận bản vá bảo mật tới 04/2028, đủ cho thời gian làm đồ án. Node 25 trở lên không còn kèm Corepack.

## Ghi chú kiểm tra chéo (16/11/2026)

Một model khác (Sonnet) đọc bản nháp và chỉ ra các chỗ hở. Bạn đã chọn áp dụng mọi đề xuất. Các thay đổi chính: controller tự bọc response (không bọc ngầm) cùng một `OpenApiCustomizer`; hàm `api` ở frontend ném `ApiError`; bảng mã lỗi đầy đủ hơn; danh sách trắng cho sort; sinh mã bằng `DEFAULT` trong DB với 8 chữ số thay cho generator Hibernate tự viết (API generator của Hibernate 7 đã đổi, và `lpad` cắt số dài); chốt `Instant` và bean `Clock`; cổng DB 5433; `127.0.0.1` cho `BACKEND_URL` ở dev; Testcontainers để xuất `openapi.json`; liệt kê đủ starter của Boot 4 và lưu ý Jackson 3; chỗ mount volume mới của PostgreSQL 18; cài đặt UTF-8 trên Windows; cài pnpm không qua Corepack; quy tắc bảo mật tạm thời trước spec Đăng nhập.
- PostgreSQL: 18 là bản ổn định mới nhất (18.7, hỗ trợ tới 14/11/2030).
- Flyway Community miễn phí và hỗ trợ PostgreSQL. Phiên bản Flyway do Boot 4.1 quản lý chưa xác nhận từ nguồn chính thức; dùng phiên bản Boot quản lý, không ghi đè.
- springdoc-openapi: dòng 3.x dành cho Boot 4; bản 3.1.0 nhắm Boot 4.1 (chỉ thấy ở bản sao, chưa xác nhận trên GitHub chính thức).
- openapi-typescript 7.13.0. Orval 8.48.0 cần Node 22.18 trở lên (không chọn).

## References

**Project sources**:
- `docs/scope/scope.md`, tính năng 1 (Stack & kiến trúc) và header build approach Tracer Bullet
- `sql/database_schema_quan_ly_chung_cu.sql`: khóa `VARCHAR(30)`, kế thừa TPT, CHECK tiếng Việt
- `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx`: tiền tố `/api/v1`, bảng mã HTTP, vỏ bọc response, sheet DTO và Enum
- Skill đã cài: `java-springboot` (`.agents/skills/java-springboot/`), `next-dev-loop` (`.agents/skills/next-dev-loop/`), `ui-styling` (`.claude/skills/ui-styling/`)

**Practices & standards**:
- Monolith first; tránh microservices sớm với nhóm nhỏ
- ORM cho CRUD, SQL thuần cho báo cáo và tổng hợp
- OWASP: không lưu token nhạy cảm ở nơi script trên trang đọc được
- Quy ước đặt tên PostgreSQL (snake_case, chữ thường)
- Migration chỉ thêm, không sửa file đã chạy

**Links** (đã kiểm tra trong lần tra cứu):
- Spring Boot, vòng đời phiên bản: https://endoflife.date/spring-boot
- Next.js, cài đặt và yêu cầu Node: https://nextjs.org/docs/app/getting-started/installation
- Node.js, vòng đời phiên bản: https://endoflife.date/nodejs
- PostgreSQL, vòng đời phiên bản: https://endoflife.date/postgresql
- Flyway Community: https://www.red-gate.com/products/flyway/community/
- openapi-typescript: https://www.npmjs.com/package/openapi-typescript
