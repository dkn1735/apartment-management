# 0001. Stack và kiến trúc nền cho Hệ thống Quản lý Chung cư

**Date**: 2026-11-16
**Status**: Accepted

## Summary

Spec này chốt bộ công nghệ và các quy ước nền cho cả dự án: backend Spring Boot 4.1 trên Java 25, frontend Next.js 16, database PostgreSQL 18, tất cả trong một repo. Backend là một ứng dụng duy nhất chia package theo phân hệ (modular monolith, tức một khối triển khai nhưng ranh giới phân hệ rõ ràng), trình duyệt chỉ gọi API qua chính tên miền của Next.js nên không cần CORS. Spec cũng chốt dạng response chung, mã lỗi, phân trang, múi giờ, cách đặt tên bảng và cách sinh mã, để 80 API sau này đều theo một khuôn. Việc dựng bộ khung rỗng (backend, frontend, database chạy được trên máy) là bước tiếp theo, do `/develop` làm dựa trên bảng stack bên dưới.

## Decision

**Chosen option**: Option 1: Modular monolith Spring Boot 4.1 + JPA + Flyway, Next.js 16 gọi API phía client qua cùng tên miền

Một repo gồm `backend/` (Spring Boot 4.1, Java 25, Maven, Spring Data JPA, Flyway, Spring Security tự cấp JWT) và `frontend/` (Next.js 16 App Router, TanStack Query, openapi-fetch, Tailwind CSS + shadcn/ui), PostgreSQL 18 chạy trong Docker, trình duyệt gọi `/api/...` trên tên miền của Next.js và Next.js chuyển tiếp sang Spring Boot.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`) · `next-dev-loop` (`vercel/next.js`, `.agents/skills/next-dev-loop/`) · `ui-styling` (`.claude/skills/ui-styling/`, Tailwind CSS + shadcn/ui) · `design-system` (`.claude/skills/design-system/`, dùng ở tính năng 4)

> Lưu ý khi dùng `java-springboot`: skill này mặc định trả lỗi dạng `ProblemDetail` (RFC 9457). Dự án này dùng vỏ bọc lỗi riêng ở mục *Hợp đồng API chung* bên dưới, và spec này thắng ở điểm đó. Các quy ước khác của skill (package theo phân hệ, inject qua constructor, controller trả DTO hoặc record chứ không trả entity, service giữ transaction) khớp với spec. Với Boot 4, starter web là `spring-boot-starter-webmvc`.

**MCP server**: `next-devtools-mcp` (Vercel). Bạn tự thêm vào cấu hình MCP, ví dụ `claude mcp add next-devtools -- npx -y next-devtools-mcp@latest`; nó chỉ hoạt động khi `pnpm dev` đang chạy.

## Proposed stack

| Layer | Choice | Reason |
|---|---|---|
| Mẫu kiến trúc | Modular monolith, một package cho mỗi phân hệ | Nhóm 2 người, 13 phân hệ: một khối triển khai dễ chạy và debug nhất, package theo phân hệ giúp chia việc và tìm code nhanh. |
| Ngôn ngữ backend | Java 25 (LTS) | LTS mới nhất, được hỗ trợ lâu nhất, Spring Boot 4.1 hỗ trợ đầy đủ. |
| Framework backend | Spring Boot 4.1.x (Spring Framework 7) | Dòng đang được hỗ trợ; 3.5 đã hết hỗ trợ miễn phí 30/06/2026, 4.0 hết vào 31/12/2026. |
| Build backend | Maven, có Maven Wrapper (`mvnw`, `mvnw.cmd`) | Quen thuộc, ít bất ngờ, chạy trên Windows không cần cài Maven. |
| Database | PostgreSQL 18, image khóa bản nhỏ (ví dụ `postgres:18.7`) | Bản ổn định mới nhất, hỗ trợ tới 2030, khớp schema 38 bảng có sẵn. |
| Truy cập dữ liệu | Spring Data JPA (Hibernate 7); truy vấn báo cáo dùng SQL thuần qua `@Query(nativeQuery = true)` hoặc `JdbcClient` | ORM cho CRUD, SQL thuần cho tổng hợp và báo cáo. |
| Migration | Flyway (Community) qua `spring-boot-starter-flyway` + `flyway-database-postgresql`, file SQL trong `backend/src/main/resources/db/migration` | Mỗi thay đổi là một file SQL thuần, tự chạy khi backend khởi động, hợp với file schema có sẵn. Chỉ thêm `flyway-core` thì Boot 4 lặng lẽ không chạy migration. |
| JSON | Jackson 3 (mặc định của Boot 4, package `tools.jackson`; annotation vẫn là `com.fasterxml.jackson.annotation`) | Ví dụ trên mạng viết cho Jackson 2 (`com.fasterxml.jackson.databind`, `@JsonComponent`) sẽ không biên dịch; dùng `JsonMapper` và `@JacksonComponent`. |
| Test backend | JUnit 5 + Testcontainers với `postgres:18.7` (không dùng H2) | CHECK, sequence và `TIMESTAMPTZ` chạy đúng như thật; đổi lại máy chạy test cần Docker. Chiến lược test chi tiết do `/audit` và `/test` quyết định. |
| Bảo mật và xác thực | Spring Security, backend tự ký JWT (OAuth2 Resource Server + Nimbus JOSE có sẵn trong Spring), refresh token lưu trong DB | Tài khoản nằm trong `nguoi_dung_he_thong`, đúng API-AUTH-01 đến 05, không thêm dịch vụ phải chạy. Chi tiết token và cookie chốt ở spec Đăng nhập. |
| Kiểm tra dữ liệu backend | Jakarta Bean Validation (`spring-boot-starter-validation`), thông báo lỗi tiếng Việt | Chuẩn của Spring, lỗi đổ vào mảng `errors` của response. |
| Tài liệu API | springdoc-openapi dòng 3.x (dòng dành cho Boot 4), Swagger UI chỉ bật ở profile `dev` | Sinh đặc tả OpenAPI từ code, là nguồn để frontend sinh kiểu. |
| Ngôn ngữ frontend | TypeScript ở chế độ `strict` | Bắt lỗi lệch kiểu với API ngay lúc biên dịch. |
| Framework frontend | Next.js 16 (App Router), React 19 | Lựa chọn trong scope; app nội bộ sau đăng nhập nên trang chủ yếu là Client Component. |
| Runtime frontend | Node.js 24 (Maintenance LTS, hỗ trợ tới 04/2028), ghi vào `.nvmrc` và `engines` | Đã qua giai đoạn Active nên thư viện chạy ổn, đủ cho Next.js 16 (cần từ 20.9). |
| Trình quản lý package | pnpm, cài bằng `npm i -g pnpm@<bản khóa>`, phiên bản ghi trong `packageManager` của `package.json` | Cài nhanh, chặt về dependency. Không dùng Corepack: nó cần quyền admin trên Windows và đã bị bỏ khỏi Node 25 trở lên. |
| Gọi API từ frontend | `openapi-typescript` sinh kiểu từ `docs/api/openapi.json`, `openapi-fetch` gọi API, TanStack Query quản lý cache và trạng thái | Đổi API thì frontend báo lỗi biên dịch; TanStack Query lo đang tải, lỗi, tải lại. |
| Form | React Hook Form + Zod | Ít render lại, thông báo lỗi tiếng Việt trong schema; backend vẫn kiểm tra lại. |
| CSS và component | Tailwind CSS v4 + shadcn/ui (dựng trên Radix) | Component nằm trong repo, xử lý focus và bàn phím tốt; shadcn hiện mặc định Tailwind v4; giao diện cụ thể chốt ở tính năng 4. |
| Đường gọi API | Trình duyệt gọi `/api/v1/...` trên tên miền Next.js, `rewrites` trong `next.config.ts` chuyển sang Spring Boot | Cùng tên miền nên không cần CORS, dùng được cookie `HttpOnly`. |
| Chạy local | `docker compose up` chỉ bật PostgreSQL; backend chạy bằng `mvnw.cmd spring-boot:run`, frontend bằng `pnpm dev` | Reload nhanh, debug Java thẳng trong IDE. |
| Triển khai và demo | `docker-compose.prod.yml` build cả ba phần thành image, chạy demo bằng một lệnh | Không tốn tiền, không phụ thuộc mạng khi bảo vệ đồ án. |
| Cấu hình và bí mật | `.env` không commit, có `.env.example` được commit; Spring profiles `dev` và `prod`; `@ConfigurationProperties` + `@Validated` | Bí mật không lên git, thiếu biến thì backend dừng ngay lúc khởi động với lỗi rõ ràng. |
| Theo dõi | Spring Boot Actuator (chỉ mở `health`), mã `X-Request-Id` cho mỗi request, log dễ đọc có `requestId` ở `dev`, log JSON ở `prod` (`logging.structured.format.console=ecs`) | Nối được một lỗi trên giao diện với dòng log tương ứng. |
| Việc chạy nền, email, lưu tệp | Chưa cần ở đợt này | Email chốt ở tính năng 13; lưu tệp và lịch chạy định kỳ chốt khi tới UC06, UC08, UC10. |

**Starter backend** (Boot 4 chia starter nhỏ hơn 3.x, cần khai báo đủ): `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-security`, `spring-boot-starter-security-oauth2-resource-server`, `spring-boot-starter-flyway` + `flyway-database-postgresql`, driver `org.postgresql:postgresql`, `springdoc-openapi-starter-webmvc-ui` (dòng 3.x), `spring-boot-testcontainers` + `org.testcontainers:postgresql` cho test. Phiên bản do Boot quản lý thì không ghi đè. Riêng springdoc thì khóa đúng bản lúc scaffold và kiểm tra app khởi động được trên Boot 4.1 (bản dành cho 4.1 chưa được xác nhận từ nguồn chính thức).

## Cấu trúc repo

```
project2/
  backend/                     Spring Boot, Maven
    src/main/java/vn/edu/hust/chungcu/
      ChungCuApplication.java
      common/                  vỏ response, xử lý lỗi, phân trang, sinh mã, cấu hình chung
      auth/                    phân hệ Xác thực & Bảo mật (controller, service, repository, dto, entity)
      ...                      mỗi phân hệ sau là một package ngang hàng (canho, cutru, taichinh, ...)
    src/main/resources/
      application.yml, application-dev.yml, application-prod.yml
      db/migration/            V1__..., V2__... (Flyway)
  frontend/                    Next.js, pnpm
    src/app/                   App Router
    src/lib/api/               schema.d.ts sinh ra, client openapi-fetch
  docs/
    api/openapi.json           đặc tả OpenAPI xuất từ backend, được commit
    scope/, specs/
  sql/                         file schema gốc, giữ làm tài liệu thiết kế
  docker-compose.yml           PostgreSQL cho dev
  docker-compose.prod.yml      cả ba phần cho demo
  .gitattributes, .gitignore, .env.example
```

Package gốc là `vn.edu.hust.chungcu`. Bên trong mỗi phân hệ chia theo lớp (controller, service, repository). Một phân hệ chỉ gọi phân hệ khác qua service công khai của nó, không đọc thẳng repository của phân hệ khác. Quy ước code chi tiết (kiến trúc bên trong, lint, format) do `/audit` hỏi và ghi vào `AGENTS.md`.

## Hợp đồng API chung (áp dụng cho cả 80 API)

**Tiền tố và đặt tên**: mọi endpoint nằm dưới `/api/v1`, đúng đường dẫn trong file đặc tả. Tên trường JSON là camelCase tiếng Việt không dấu, khớp các DTO trong sheet `Data Models & Enums` (ví dụ `maCanHo`, `trangThaiSuDung`).

**Response thành công** (giữ vỏ bọc của đặc tả, `code` luôn trùng mã HTTP thật):

```json
{
  "success": true,
  "code": 200,
  "message": "Đăng nhập thành công",
  "data": { },
  "timestamp": "2026-11-16T03:00:00Z"
}
```

Tạo mới trả `201`, đăng nhập và các thao tác không tạo tài nguyên trả `200`. Response `204` không có body (không có vỏ bọc). `message` khi thành công do controller đặt, là một câu tiếng Việt ngắn cố định cho thao tác đó. Response thành công không có `errorCode`, `errors`, `requestId`.

**Cách bọc trong code**: controller tự bọc rõ ràng bằng `ApiResponse.ok(data, message)` / `ApiResponse.created(data, message)` (một record dùng chung trong `common`). Không dùng `ResponseBodyAdvice` bọc ngầm, vì nó xung đột với springdoc (tài liệu sẽ ghi kiểu chưa bọc), với `ResponseEntity`, với body kiểu `String` và với `204`. Một `OpenApiCustomizer` trong `common` mô tả đúng kiểu đã bọc cho từng endpoint (`ApiResponse<KieuDuLieu>`) và gắn schema `ErrorResponse` dùng chung vào mọi endpoint, để kiểu sinh ra ở frontend khớp thật.

**Tiền**: Java dùng `BigDecimal`, cột giữ kiểu `NUMERIC` của schema, JSON trả dạng số. VND không có phần lẻ và giá trị nằm xa giới hạn số an toàn của JavaScript, nên trả dạng số là an toàn.

**Response lỗi** (cùng vỏ bọc, thêm mã lỗi máy đọc được):

```json
{
  "success": false,
  "code": 400,
  "message": "Dữ liệu không hợp lệ",
  "errorCode": "VALIDATION_FAILED",
  "errors": [ { "field": "soDienThoai", "message": "Số điện thoại không đúng định dạng" } ],
  "requestId": "3f2c9a1e7b4d",
  "timestamp": "2026-11-16T03:00:00Z"
}
```

`errorCode` là chuỗi cố định viết hoa có gạch dưới, frontend dùng nó để chọn thông báo, không dựa vào `message`. Response lỗi luôn có `errors` (mảng rỗng khi lỗi không theo từng trường) và `requestId`, không có `data`. Bộ mã chung, mỗi phân hệ thêm mã riêng có tiền tố phân hệ (ví dụ `AUTH_INVALID_CREDENTIALS`):

| HTTP | `errorCode` chung | Khi nào |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Thiếu trường, Bean Validation thất bại, tham số phân trang hoặc sort không hợp lệ |
| 400 | `MALFORMED_REQUEST` | JSON hỏng, sai kiểu tham số đường dẫn hoặc query |
| 405 | `METHOD_NOT_ALLOWED` | Sai phương thức HTTP |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Sai `Content-Type` |
| 401 | `UNAUTHENTICATED` | Thiếu token, token sai hoặc hết hạn |
| 403 | `FORBIDDEN` | Đã đăng nhập nhưng không đủ quyền |
| 404 | `NOT_FOUND` | Không có tài nguyên với mã đó, hoặc route không tồn tại (`NoResourceFoundException`) |
| 409 | `CONFLICT` | Trùng mã định danh, xung đột trạng thái, hoặc DB từ chối vì unique, FK, CHECK (`DataIntegrityViolationException`) |
| 409 | `CONCURRENT_MODIFICATION` | Hai người sửa cùng bản ghi (optimistic lock) |
| 413 | `PAYLOAD_TOO_LARGE` | Tệp vượt giới hạn (20MB theo đặc tả) |
| 422 | `BUSINESS_RULE_VIOLATION` | Đúng định dạng nhưng vi phạm quy tắc nghiệp vụ |
| 500 | `INTERNAL_ERROR` | Lỗi không lường trước; `message` chung chung, không lộ stack trace hay SQL |

Mọi lỗi đi qua một `@RestControllerAdvice` duy nhất trong `common`, kể cả lỗi 401 và 403 từ Spring Security (qua `AuthenticationEntryPoint` và `AccessDeniedHandler`), để không có response lỗi nào lệch dạng.

**Phân trang**: `?page=1&size=20&sort=tenTruong,desc`. `page` bắt đầu từ 1 (backend trừ 1 trước khi đưa cho Spring Data), `size` mặc định 20, tối đa 100. `page<1`, `size<1` hoặc `size>100` trả 400 `VALIDATION_FAILED`, không tự sửa. Mỗi endpoint khai báo danh sách trắng các trường được sort; trường ngoài danh sách trả 400 (không để lọt xuống Spring Data thành lỗi 500). Mọi API danh sách đều phân trang, trừ danh mục nhỏ cố định mà spec của tính năng ghi rõ. `data` của danh sách có dạng (dựng bằng helper `PageResponse.from(Page)` trong `common`):

```json
{ "items": [ ], "page": 1, "size": 20, "totalItems": 135, "totalPages": 7 }
```

**Ngày giờ**: thời điểm lưu bằng `TIMESTAMPTZ`, ánh xạ sang `Instant` trong Java, API trả ISO 8601 có `Z` (UTC). Ngày không giờ dùng `DATE` và `LocalDate`, trả dạng `2026-11-16`. Mọi quy tắc theo ngày (kỳ tính phí, hạn mức theo tuần, hạn SLA) tính theo múi giờ `Asia/Ho_Chi_Minh` ở backend, lấy từ cấu hình `app.timezone`. Code lấy giờ hiện tại qua một bean `Clock` (không gọi `Instant.now()` trực tiếp) để test đặt được giờ. Frontend hiển thị theo `Asia/Ho_Chi_Minh`. Kết nối JDBC chạy UTC (`spring.jpa.properties.hibernate.jdbc.time_zone=UTC`).

**Theo dõi request**: backend đọc `X-Request-Id` nếu có và hợp lệ (tối đa 64 ký tự, chỉ chữ, số và `-`), ngược lại tự sinh mã mới, để không ai chèn được nội dung lạ vào log. Mã được đưa vào MDC (ngữ cảnh log), trả lại trong header và trong `requestId` của response lỗi. Log request không ghi body, mật khẩu, token hay CCCD.

**Frontend gọi API**: một hàm `api` duy nhất trong `src/lib/api/` bọc openapi-fetch: thành công thì trả thẳng `data` bên trong vỏ bọc (kiểu `T`), lỗi thì ném `ApiError` có `status`, `errorCode`, `errors`, `requestId`. TanStack Query dùng hàm này làm `queryFn` và `mutationFn` (openapi-fetch mặc định không ném lỗi, nên thiếu lớp này thì TanStack Query không biết là có lỗi). Body lỗi không phải JSON (backend sập, proxy trả 502 hoặc 504) được đổi thành `ApiError` với `errorCode` là `BACKEND_UNAVAILABLE`.

## Quy ước database

- **Tên bảng và cột**: snake_case không dấu (`nguoi_dung_he_thong`, `ma_nguoi_dung`). Hibernate tự ánh xạ `maNguoiDung` sang `ma_nguoi_dung` bằng naming strategy mặc định của Spring, không cần `@Column(name=...)`. File `sql/database_schema_quan_ly_chung_cu.sql` giữ nguyên làm tài liệu thiết kế; việc chuyển 38 bảng sang migration snake_case thuộc spec Mô hình dữ liệu (tính năng 3).
- **Sinh mã khóa chính**: tiền tố cố định + số tuần tự 8 chữ số, sinh ngay trong DB bằng `DEFAULT` của cột, mỗi bảng một sequence riêng. Ví dụ trong migration: `ma_nguoi_dung VARCHAR(30) PRIMARY KEY DEFAULT 'ND' || lpad(nextval('seq_nguoi_dung_he_thong')::text, 8, '0')`, cho ra `ND00000001`. Entity đánh dấu cột là `@Generated(event = EventType.INSERT)` để Hibernate đọc lại mã DB vừa sinh (PostgreSQL trả về qua `RETURNING`), không viết generator riêng trong Java. Cách này chạy được với cả seed và SQL viết tay. `lpad` **cắt bớt** số dài hơn độ dài cho trước (số thứ 100 triệu sẽ bị cắt còn 8 chữ số và trùng khóa), nên thứ chặn tràn là sequence khai báo `MAXVALUE 99999999 NO CYCLE`: vượt mức thì `nextval` báo lỗi thay vì sinh mã trùng. `CHECK` định dạng trên cột chỉ là lớp phụ (xem spec 0002). Seed chèn mã tay thì phải `setval` lại sequence. Bảng do người dùng đặt mã (tòa nhà, tầng, căn hộ) thì nhận mã từ request, không có `DEFAULT`. Danh sách tiền tố cho từng bảng chốt ở spec Mô hình dữ liệu.
- **Giá trị trạng thái**: DB và API giữ nhãn tiếng Việt có dấu như trong CHECK (`'Hoạt động'`, `'Tạm khóa'`). Java dùng enum có nhãn (`HOAT_DONG("Hoạt động")`) cài một interface dùng chung `CoNhan` (có `getNhan()`), cộng một converter gốc trừu tượng `NhanEnumConverter<E>` trong `common`; mỗi enum chỉ cần một lớp converter một dòng kế thừa nó. JSON dùng `@JsonValue` trên `getNhan()` và một `@JsonCreator` tra theo nhãn, nên OpenAPI hiển thị đúng các nhãn. Mọi file (SQL, Java, TS) lưu UTF-8 dạng NFC (dạng dựng sẵn của chữ có dấu), để `'Tạm khóa'` trong Java và trong CHECK là cùng một chuỗi byte. CHECK trong DB vẫn giữ làm lớp bảo vệ cuối.
- **Schema do Flyway quản lý**: `spring.jpa.hibernate.ddl-auto=validate` (Hibernate chỉ kiểm tra entity khớp schema, không tự tạo bảng). `spring.jpa.open-in-view=false` để không có truy vấn lười chạy ngầm khi render response.
- **Không sửa migration đã chạy**: mỗi thay đổi là một file `V<n>__<mo_ta>.sql` mới. Dữ liệu mẫu (seed) chỉ chạy khi bật profile `seed` (dev và bản demo), không chạy trong test; cơ chế ở [spec 0002](../0002-mo-hinh-du-lieu/index.md).

## Đường gọi API và cổng chạy

| Thành phần | Dev | Demo (`docker-compose.prod.yml`) |
|---|---|---|
| Frontend Next.js | `http://localhost:3000` | container `frontend`, mở cổng 3000 ra ngoài |
| Backend Spring Boot | `http://localhost:8080` | container `backend`, chỉ trong mạng Docker |
| PostgreSQL | `localhost:5433` trên máy (cổng 5432 trong container) | container `db`, chỉ trong mạng Docker |

Cổng 5433 trên máy để không đụng PostgreSQL có thể đã cài sẵn trên Windows; đổi được qua `POSTGRES_PORT`.

Trình duyệt chỉ nói chuyện với Next.js. `next.config.ts` có `rewrites` chuyển `/api/:path*` sang `${BACKEND_URL}/api/:path*`. Vì vậy frontend **không** có Route Handler riêng dưới `/api` (rewrite sẽ nuốt mất). Spring Boot không bật CORS, nên gọi thẳng `localhost:8080` từ trang `localhost:3000` sẽ bị trình duyệt chặn; muốn thử API trực tiếp thì dùng Swagger UI. Swagger UI ở `http://localhost:8080/swagger-ui.html` chỉ có ở profile `dev`. Backend bật `server.forward-headers-strategy=framework` để đọc `X-Forwarded-For` mà proxy gửi kèm, nhờ đó nhật ký kiểm toán sau này lấy được IP thật của người dùng.

`rewrites` được đóng cứng vào bản build (`routes-manifest.json`) lúc `next build`, đổi biến môi trường lúc chạy không có tác dụng. Dev dùng `BACKEND_URL=http://127.0.0.1:8080` (không dùng `localhost`, vì trên Windows Node có thể phân giải sang IPv6 `::1` và kết nối lúc được lúc không). Image demo build với giá trị cố định `http://backend:8080` (tên service trong compose), truyền qua `ARG BACKEND_URL` trong Dockerfile.

**Đồng bộ kiểu API**: một test Spring (`OpenApiExportTest`, chạy profile `dev` với PostgreSQL từ Testcontainers) gọi `/v3/api-docs` và ghi ra `docs/api/openapi.json`; chạy bằng `mvnw.cmd verify`, commit file kèm thay đổi API. Sau đó `pnpm gen:api` trong frontend sinh `src/lib/api/schema.d.ts` từ file đó. Frontend build được mà không cần backend đang chạy. Khi có CI, thêm bước sinh lại và `git diff --exit-code` để bắt file bị quên cập nhật.

## Cấu hình

**Cách biến tới được ứng dụng**: compose đọc `.env` ở gốc repo. Backend đọc `backend/.env` qua `spring.config.import=optional:file:.env[.properties]` trong `application.yml`, nên `mvnw.cmd spring-boot:run` và IDE đều nhận biến mà không cần cấu hình thêm; biến môi trường thật (trong Docker) vẫn được ưu tiên. Frontend dùng `frontend/.env.local` theo cơ chế có sẵn của Next.js. Cả ba file đều nằm trong `.gitignore`, mỗi file có một bản `.example` được commit.

**Backend** (biến môi trường, `backend/.env.example`):
- `SPRING_PROFILES_ACTIVE`: `dev` hoặc `prod`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: kết nối PostgreSQL
- `JWT_SECRET` (hoặc cặp khóa, tùy spec Đăng nhập): khóa ký access token; không có giá trị mặc định
- `APP_TIMEZONE`: mặc định `Asia/Ho_Chi_Minh`, dùng cho quy tắc theo ngày

**Frontend** (`frontend/.env.example`):
- `BACKEND_URL`: địa chỉ Spring Boot cho `rewrites` (dev `http://127.0.0.1:8080`, demo `http://backend:8080` lúc build image)

**Compose** (`.env.example` ở gốc):
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `POSTGRES_PORT`: cổng trên máy, mặc định `5433`

Backend kiểm tra các biến bắt buộc lúc khởi động và dừng với thông báo rõ ràng nếu thiếu.

## Nền tảng khác

- **Windows**: mọi lệnh chạy được trên Windows (`mvnw.cmd`, script pnpm, Docker Desktop). `.gitattributes` đặt `* text=auto eol=lf`, riêng `*.cmd` và `*.bat` dùng `eol=crlf`, để hai máy không lệch xuống dòng. Đánh dấu `mvnw` chạy được bằng `git update-index --chmod=+x mvnw` (cho Dockerfile và máy Linux).
- **UTF-8 cho tiếng Việt**: `pom.xml` đặt `project.build.sourceEncoding` là UTF-8; `backend/.mvn/jvm.config` có `-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8` (không thì log tiếng Việt bị vỡ chữ trên console Windows). File migration lưu UTF-8 **không BOM** (BOM làm hỏng câu SQL đầu tiên). Thông báo lỗi validation viết thẳng trong annotation (`message = "..."`), không để trong file `.properties`.
- **Bảo mật tạm thời** (tới khi spec Đăng nhập chốt cách cấp và kiểm tra token): Spring Security cho qua `/actuator/health`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`, chỉ ở `dev`) và `/api/v1/auth/**`; mọi đường còn lại trả 401 `UNAUTHENTICATED` theo đúng vỏ bọc lỗi. Actuator chỉ mở `health`.
- **Docker Compose**: service `db` mount named volume vào `/var/lib/postgresql` (image PostgreSQL 18 đã đổi chỗ này, không còn `/var/lib/postgresql/data`), có healthcheck `pg_isready`. Ở bản demo, `backend` có `depends_on: db: condition: service_healthy`, và `frontend` phụ thuộc `backend`. Dockerfile dùng build nhiều tầng: backend build bằng image JDK 25 rồi chạy trên image JRE 25 (ví dụ `eclipse-temurin:25-jre`), frontend build bằng `node:24` với `output: 'standalone'` rồi chạy `node server.js`; cả hai chạy bằng user không phải root.
- **Cấu trúc frontend**: mã nguồn trong `src/`, alias `@/` trỏ tới `src/`. Provider (TanStack `QueryClientProvider`) nằm ở `src/app/providers.tsx`, client API và hàm `api` ở `src/lib/api/`. Zod 4 đi với bản `@hookform/resolvers` tương thích, thông báo lỗi tiếng Việt viết trong schema. Next.js 16 đã bỏ `next lint`, nên script `lint` gọi thẳng ESLint (công cụ cụ thể do `/audit` chọn). React Compiler tạm tắt cho đơn giản. Next.js 16 dùng Turbopack mặc định, và file `middleware.ts` cũ nay tên là `proxy.ts` (liên quan khi spec Đăng nhập bảo vệ route).
- **Virtual threads**: bật `spring.threads.virtual.enabled=true` (Java 25 ổn định, không tốn gì thêm). Giữ kích thước pool kết nối Hikari mặc định; nhiều virtual thread cùng chờ một pool nhỏ sẽ gặp lỗi hết thời gian chờ kết nối, nên nếu thấy lỗi này thì tăng pool chứ không tắt virtual threads.
- **Phiên bản được khóa**: Java trong `pom.xml` (`java.version` 25), Maven Wrapper trong `.mvn/wrapper/maven-wrapper.properties`, Node trong `.nvmrc`, pnpm trong `packageManager`, PostgreSQL bằng tag bản nhỏ (`postgres:18.7`).

## Consequences

**Positive**:
- Một lệnh clone, một lịch sử commit; sửa API và giao diện trong cùng một commit, đúng tinh thần Tracer Bullet.
- Kiểu TypeScript sinh từ OpenAPI nên đổi API là frontend báo lỗi biên dịch ngay, không lệch âm thầm giữa hai đầu.
- Một dạng response và một bộ mã lỗi cho cả 80 API: frontend viết một lớp xử lý lỗi dùng chung.
- Cùng tên miền nên không có CORS, và spec Đăng nhập có thể dùng cookie `HttpOnly` thay vì localStorage.

**Negative / tradeoffs**:
- Spring Boot 4.x mới hơn 3.x nên ít bài hướng dẫn hơn; một số ví dụ trên mạng (đặc biệt Spring Security và springdoc) viết cho 3.x và cần chỉnh.
- Vỏ bọc `{ success, code, ... }` lặp lại mã HTTP trong body; openapi-fetch phải bóc `data` ở một hàm dùng chung, và mọi controller phải trả qua lớp bọc này.
- `rewrites` của Next.js được đóng cứng lúc build, nên image frontend chỉ chạy với backend tên `backend` trong compose; muốn chạy ở nơi khác thì phải build lại image. Proxy của Next còn có giới hạn dung lượng body và thời gian chờ, cần chỉnh khi tới upload tệp 20MB (UC06).
- Test cần Docker đang chạy (Testcontainers), kể cả lệnh xuất `openapi.json`.
- Đổi tên sang snake_case làm migration lệch tên với file SQL gốc và tài liệu nộp; người đọc phải nhớ quy tắc đổi tên.
- Mã tuần tự (`ND00000001`) đoán được, nên mọi endpoint đọc theo mã phải kiểm tra quyền sở hữu, không dựa vào việc mã khó đoán.
- Nhãn tiếng Việt có dấu trong DB và JSON: phải giữ mã hóa UTF-8 ở mọi nơi (file nguồn, kết nối JDBC, console Windows).

**Neutral**:
- Backend kiểm tra lại mọi dữ liệu frontend đã kiểm tra bằng Zod; quy tắc nằm ở hai nơi.
- Thêm bước `pnpm gen:api` mỗi khi API đổi; quên chạy thì frontend vẫn build với kiểu cũ cho tới khi `openapi.json` được cập nhật.

## Follow-up

- [ ] Dựng bộ khung rỗng theo bảng stack (backend, frontend, compose, `.env.example`, `.gitattributes`) sao cho khởi động và build thành công trên Windows: `/develop stack & kiến trúc`
- [ ] Ghi quy ước code và công cụ (lint, format, kiểm tra trước commit, CI, git) sau khi có bộ khung: `/audit` (tính năng 2)
- [ ] Chuyển schema 38 bảng sang migration snake_case, chốt tiền tố mã cho từng bảng và cách seed: `/architect mô hình dữ liệu` (tính năng 3)
- [ ] Chốt thời hạn token, cách lưu token phía trình duyệt (cookie `HttpOnly`), chống CSRF và cách bảo vệ route ở Next.js: `/architect đăng nhập & trang hồ sơ` (tính năng 5)
- [ ] File đặc tả API có vài chỗ lệch do sinh từ mẫu (đăng nhập trả `code: 201`, body của refresh token là tài khoản và mật khẩu, mọi `data` ví dụ giống nhau). Spec này lấy dạng vỏ bọc làm chuẩn; nội dung `data` thật của từng API chốt ở spec của tính năng đó. Bạn có thể cập nhật file xlsx cho khớp trước khi nộp.
- [ ] Cân nhắc thêm Spring Modulith (kiểm tra ranh giới phân hệ bằng một test) khi có từ 3 phân hệ trở lên
- [ ] Chiến lược test chi tiết (mức độ phủ, test nào chạy trước commit): `/audit` và `/test` quyết định, dùng Testcontainers đã chốt ở đây
- [ ] Khi tới UC06 (tệp đính kèm): chỉnh giới hạn body của proxy Next.js (`proxyClientMaxBodySize`), `spring.servlet.multipart.max-file-size` 20MB và ánh xạ lỗi 413
- [ ] Spec Mô hình dữ liệu: chọn collation để sắp xếp tiếng Việt đúng (ví dụ collation ICU) và extension `unaccent` nếu cần tìm kiếm không dấu
- [ ] Nếu muốn bỏ việc đóng cứng địa chỉ backend vào image frontend: cân nhắc thêm một reverse proxy nhỏ (Caddy) trước cả hai ở bản demo

## Rationale

Lý do và các phương án đã cân nhắc: xem [rationale.md](rationale.md).
