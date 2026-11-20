# Scope: Hệ thống Quản lý Chung cư

Hệ thống quản lý vận hành chung cư cho ban quản lý (BQL), kỹ thuật, kế toán, ban quản trị (BQT) và cư dân: căn hộ, cư trú, điện nước, phí, thanh toán, sự cố, bảo trì, tiện ích, thông báo, khảo sát và báo cáo. Đồ án IT3930 của nhóm 2 người, backend Java Spring Boot, frontend NextJS, cơ sở dữ liệu PostgreSQL. Đợt này làm nền tảng và phân hệ Xác thực & Bảo mật trước; 12 phân hệ nghiệp vụ nằm ở mục Deferred.

**Build approach:** Tracer Bullet (mỗi lát cắt chạy thật xuyên suốt DB, API và giao diện; lát sau làm dày thêm một đoạn của đường đã chạy).
**Workflow:** Beta (sau `/develop` chạy `/check verify`, rồi `/test`). Đây là mức kỹ lưỡng mặc định của dự án. `/architect` là điểm dừng đầu tiên nên ghé khi một tính năng còn quyết định chưa chốt, nhưng bạn có thể bỏ qua nếu đã biết cách làm. Tính năng nào cũng có thể mang tag riêng (ví dụ `· GA`) để làm kỹ hơn hoặc nhẹ hơn.

_Đây là gợi ý để việc xây dựng có trật tự, không phải yêu cầu bắt buộc. Bỏ qua phần nào không hợp: nếu bạn đã biết cách làm một tính năng, cứ dùng `/develop` và bỏ qua `/architect`. Bạn là người quyết định khi nào một tính năng là `done`._

Tài liệu nguồn: `sql/database_schema_quan_ly_chung_cu.sql` (38 bảng) và `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx` (80 API, phân hệ Xác thực gồm API-AUTH-01 đến API-AUTH-05).

## At a glance

| # | Feature | Phase | Status |
|---|---------|-------|--------|
| 1 | Stack & kiến trúc | Foundation | done |
| 2 | Quy chuẩn code & công cụ | Foundation | done |
| 3 | Mô hình dữ liệu | Foundation | done |
| 4 | Design system & nền tảng giao diện | Foundation | done |
| 5 | Đăng nhập & trang hồ sơ | Slice 1 | in-progress |
| 6 | Làm mới phiên & đăng xuất | Slice 2 | in-progress |
| 7 | Phân quyền theo vai trò & bố cục app | Slice 3 | planned |
| 8 | Ghi nhật ký kiểm toán dùng chung | Slice 4 | planned |
| 9 | Màn hình tra cứu nhật ký kiểm toán | Slice 5 | planned |
| 10 | Đổi mật khẩu | Slice 6 | planned |
| 11 | Quản lý tài khoản (Admin) | Slice 7 | planned |
| 12 | Khóa tạm khi đăng nhập sai nhiều lần | Slice 8 | planned |
| 13 | Quên mật khẩu qua email | Slice 9 | planned |

## Foundations

### 1. Stack & kiến trúc · done
Bạn đã chọn Spring Boot, NextJS và PostgreSQL. Phần còn phải chốt: phiên bản, cách tổ chức repo, cách backend nói chuyện với database và chạy migration, quy chuẩn API (dạng response chung, mã lỗi, phân trang) và cách chạy cả ba phần trên máy local.
**Done when:** quyết định được ghi trong một spec, và bộ khung rỗng (backend, frontend, database) khởi động được trên máy local và build thành công.
spec [0001](../specs/0001-stack-kien-truc/index.md) · code in `backend/`, `frontend/`, `docker-compose.yml`, `docker-compose.prod.yml`
- [x] Decide the stack (spec): `/architect stack & kiến trúc`
- [x] Scaffold from the decision: `/develop stack & kiến trúc`

### 2. Quy chuẩn code & công cụ · done
Ghi lại quy ước code và chọn công cụ lint, format, kiểm tra trước khi commit, dựa trên bộ khung thật sau khi scaffold.
**Done when:** `AGENTS.md` ở thư mục gốc mô tả đúng stack thật, và lint, format, pre-commit chạy sạch cho cả backend lẫn frontend.
code in `AGENTS.md`, `lefthook.yml`, `.github/workflows/ci.yml`, `frontend/eslint.config.mjs`, `backend/pom.xml`
- [x] Capture conventions + tooling choices: `/audit`

### 3. Mô hình dữ liệu · done
Đưa schema có sẵn (38 bảng) vào migration, và bổ sung những gì phân hệ xác thực cần mà schema chưa có: nơi lưu refresh token và mã đặt lại mật khẩu, bộ đếm đăng nhập sai, cách biểu diễn vai trò Admin và Bảo vệ (API có nhắc nhưng chưa có bảng). Mô hình dữ liệu là thứ tốn kém nhất nếu phải làm lại, nên chốt sớm.
**Done when:** migration tạo đủ schema trên database trống, các bổ sung cho xác thực có mặt, và có dữ liệu mẫu (seed) cho ít nhất một tài khoản mỗi vai trò.
spec [0002](../specs/0002-mo-hinh-du-lieu/index.md) · code in `backend/src/main/resources/db/`, `backend/src/test/java/vn/edu/hust/chungcu/SchemaMigrationTest.java`
- [x] Design it (spec): `/architect mô hình dữ liệu`
- [x] Build it: `/develop mô hình dữ liệu`
  - [x] Đường mỏng định danh: V1, profile `seed`, seed tài khoản, khung test (AC-3, AC-4, AC-5, AC-6, AC-7)
  - [x] Không gian và cư trú: V2, seed tòa nhà và hồ sơ cư trú, test chạy lại seed (AC-2, AC-5, AC-6, AC-8)
  - [x] Các nhóm còn lại: V3 đến V9 (AC-1, AC-2, AC-5)
  - [x] Khóa lại: test toàn schema, collation, README (AC-1, AC-2, AC-9)
- [x] Verify it: `/check verify mô hình dữ liệu`
- [x] Test it: `/test mô hình dữ liệu`

### 4. Design system & nền tảng giao diện · done
Ngôn ngữ hình ảnh, bố cục và các component nền (form, nút, bảng, thông báo lỗi) bằng tiếng Việt, để mọi màn hình sau trông thống nhất và dùng được bằng bàn phím.
**Done when:** `design.md` mô tả chữ, màu, khoảng cách, component; các component nền xử lý focus, bàn phím, trạng thái lỗi và trạng thái đang tải.
spec [0003](../specs/0003-design-system-giao-dien/index.md) · code in `frontend/design.md`, `frontend/src/app/globals.css`, `frontend/src/components/`, `frontend/src/lib/`, `frontend/src/app/dev/`
- [x] Design it (spec): `/architect design system & nền tảng giao diện`
- [x] Build it: `/develop design system & nền tảng giao diện`
  - [x] Đường mỏng: token, font, hạ tầng test, form có lỗi API, `/dev/ui` (AC-1, AC-2, AC-5, AC-6, AC-7, AC-14, AC-15, AC-16, AC-17, AC-18, AC-19)
  - [x] Form và phản hồi đủ bộ: các loại ô, toast, hộp thoại xác nhận, trang lỗi (AC-3, AC-4, AC-5, AC-6, AC-8, AC-13)
  - [x] Hiển thị dữ liệu: DataTable, định dạng, ô số liệu, mảng tối đầu trang (AC-3, AC-9, AC-10, AC-11)
  - [x] Khung app và khóa lại: AppShell, `/dev/ui/shell`, hoàn thiện `design.md` (AC-1, AC-3, AC-4, AC-12, AC-14)
- [x] Verify it: `/check verify design system & nền tảng giao diện`
- [x] Test it: `/test design system & nền tảng giao diện`

## Slice 1: Đăng nhập & trang hồ sơ

### 5. Đăng nhập & trang hồ sơ · in-progress · GA
Đường chạy mỏng nhất xuyên mọi tầng (cũng chính là walking skeleton): người dùng đăng nhập bằng số điện thoại hoặc email và mật khẩu, nhận access token và refresh token, vào trang hồ sơ được bảo vệ (API-AUTH-01, API-AUTH-03). Spec này là spec gốc của phân hệ: chốt cách cấp và kiểm tra token, cách lưu token phía trình duyệt, cách băm mật khẩu. Các lát sau dựa vào nó.
**Done when:** tài khoản seed đăng nhập được trên màn hình NextJS và thấy hồ sơ của mình (họ tên, vai trò, phạm vi tòa nhà hoặc căn hộ); sai mật khẩu hoặc tài khoản `Tạm khóa` bị từ chối với thông báo rõ ràng; vào trang được bảo vệ khi chưa đăng nhập thì bị chuyển về trang đăng nhập.
spec [0004](../specs/0004-dang-nhap-ho-so/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/auth/`, `frontend/src/app/dang-nhap/`, `frontend/src/app/(app)/`, `frontend/src/proxy.ts`
- [x] Design it (spec): `/architect đăng nhập & trang hồ sơ`
- [x] Build it: `/develop đăng nhập & trang hồ sơ`
  - [x] Đường mỏng: đăng nhập bằng số điện thoại tới trang hồ sơ, JWT trong cookie, chống CSRF, `proxy.ts` (AC-1, AC-7, AC-10, AC-11, AC-13)
  - [x] Làm dày đăng nhập: chuẩn hóa tài khoản, sai thông tin, tài khoản khóa, ghi phiên, nâng hash, log (AC-2, AC-3, AC-4, AC-5, AC-6, AC-16)
  - [x] Làm dày hồ sơ: phạm vi, chi tiết vai trò, khóa giữa chừng, trang `/ho-so` đủ phần (AC-8, AC-9, AC-14)
  - [x] Giao diện đăng nhập đủ và khóa lại: xử lý 401 toàn cục, thông báo lý do, a11y, README (AC-12, AC-13, AC-15)
- [x] Verify it: `/check verify đăng nhập & trang hồ sơ`
- [x] Test it: `/test đăng nhập & trang hồ sơ`
- [x] Review it (fresh model): `/check review đăng nhập & trang hồ sơ`
- [x] Document it: `/document đăng nhập & trang hồ sơ`

## Slice 2: Làm mới phiên & đăng xuất

### 6. Làm mới phiên & đăng xuất · in-progress
Gia hạn phiên bằng refresh token mà không cần nhập lại mật khẩu (API-AUTH-02), và đăng xuất thì thu hồi refresh token. Dựa trên spec của tính năng 5.
**Done when:** access token hết hạn được làm mới âm thầm khi người dùng đang thao tác; sau khi đăng xuất, refresh token cũ không dùng được nữa; refresh token hết hạn hoặc bị thu hồi thì người dùng phải đăng nhập lại.
spec [0005](../specs/0005-lam-moi-phien-dang-xuat/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/auth/`, `backend/src/main/resources/db/migration/V10__phien_bat_dau_luc.sql`, `frontend/src/lib/api/refresh.ts`, `frontend/src/app/(app)/menu-nguoi-dung.tsx`
- [x] Design it (spec): `/architect làm mới phiên & đăng xuất`
- [x] Build it: `/develop làm mới phiên & đăng xuất`
  - [x] Đường mỏng: làm mới âm thầm khi access token hết hạn, V10, middleware gửi lại request (AC-1, AC-7, AC-9, AC-10, AC-12)
  - [x] Làm dày làm mới: hết hạn, trần 30 ngày, dùng lại token cũ, khoan dung 10 giây, khóa tài khoản, khóa theo họ phiên (AC-2, AC-3, AC-4, AC-5, AC-6, AC-11, AC-16)
  - [x] Đăng xuất xuyên suốt: endpoint, menu người dùng, thông báo ở trang đăng nhập (AC-8, AC-13, AC-14)
  - [x] Dọn dẹp phiên cũ hằng ngày (AC-15)
  - [x] Làm mới chịu được response bị mất: token kế tiếp HMAC, khoan dung 30 giây trả lại token đang sống, keepalive, bỏ chờ 400 ms (AC-1, AC-3, AC-4, AC-5, AC-6, AC-10, AC-16, AC-17)
- [x] Verify it: `/check verify làm mới phiên & đăng xuất`
- [x] Test it: `/test làm mới phiên & đăng xuất`
- [x] Review it (fresh model): `/check review làm mới phiên & đăng xuất`
- [x] Document it: `/document làm mới phiên & đăng xuất`

## Slice 3: Phân quyền theo vai trò & bố cục app

### 7. Phân quyền theo vai trò & bố cục app · needs a decision
Một cách phân quyền dùng chung cho cả 80 API: xác định vai trò từ các bảng con (NhanVienBQL, NhanVienKyThuat, KeToan, ThanhVienBanQuanTri, CuDan) cùng Admin, chặn mỗi endpoint theo cột RBAC trong file đặc tả, và bố cục app NextJS có menu theo vai trò. Phân hệ nào sau này cũng dựa vào nó.
**Done when:** mỗi vai trò chỉ thấy menu và gọi được API của mình; gọi API không đủ quyền trả về 403 và giao diện hiện trang báo không có quyền; một người có nhiều vai trò vẫn được xử lý đúng.
- [ ] Design it (spec): `/architect phân quyền theo vai trò & bố cục app`

## Slice 4: Ghi nhật ký kiểm toán dùng chung

### 8. Ghi nhật ký kiểm toán dùng chung · needs a decision
Cơ chế ghi vào NhatKyHeThong mà mọi phân hệ cắm vào được, đặt trước các tính năng tạo ra sự kiện cần ghi. Đợt này ghi các sự kiện xác thực: đăng nhập thành công và thất bại, đăng xuất, đổi mật khẩu, khóa và mở khóa tài khoản.
**Done when:** mỗi sự kiện xác thực tạo đúng một dòng log có người thực hiện, loại thao tác, đối tượng và chi tiết thay đổi; không ghi mật khẩu hay token vào log; ghi log lỗi không làm hỏng thao tác chính.
- [ ] Design it (spec): `/architect ghi nhật ký kiểm toán dùng chung`

## Slice 5: Màn hình tra cứu nhật ký kiểm toán

### 9. Màn hình tra cứu nhật ký kiểm toán
Admin, BQT và BQL xem lịch sử thao tác có phân trang (API-AUTH-05). Dựa trên spec của tính năng 8.
**Done when:** người có quyền xem được danh sách log mới nhất trước, phân trang và lọc theo người dùng, loại thao tác, khoảng thời gian; vai trò khác bị chặn; trạng thái rỗng và lỗi hiển thị đúng.
- [ ] Build it: `/develop màn hình tra cứu nhật ký kiểm toán`

## Slice 6: Đổi mật khẩu

### 10. Đổi mật khẩu
Người dùng đã đăng nhập đổi mật khẩu sau khi nhập đúng mật khẩu cũ (API-AUTH-04). Dựa trên spec của tính năng 5 và 8.
**Done when:** sai mật khẩu cũ bị từ chối; mật khẩu mới phải đạt quy tắc độ mạnh; đổi xong thì các phiên khác bị thu hồi và có dòng nhật ký kiểm toán.
- [ ] Build it: `/develop đổi mật khẩu`

## Slice 7: Quản lý tài khoản (Admin)

### 11. Quản lý tài khoản (Admin) · needs a decision
Admin hoặc BQL tạo tài khoản cho từng vai trò, gán vai trò, khóa và mở khóa (trangThaiTaiKhoan). Đây là cách duy nhất để có tài khoản, vì cư dân không tự đăng ký.
**Done when:** Admin tìm, tạo, sửa, khóa và mở khóa tài khoản trên màn hình; trùng số điện thoại, email hoặc CCCD bị báo lỗi rõ; tài khoản bị khóa không đăng nhập được và mất phiên đang có; mọi thay đổi có nhật ký kiểm toán.
- [ ] Design it (spec): `/architect quản lý tài khoản (admin)`

## Slice 8: Khóa tạm khi đăng nhập sai nhiều lần

### 12. Khóa tạm khi đăng nhập sai nhiều lần · needs a decision
Chặn dò mật khẩu: sai liên tiếp N lần thì tạm khóa đăng nhập trong một khoảng thời gian. Cần chốt ngưỡng, thời gian khóa, và cách phân biệt với trạng thái `Tạm khóa` do Admin đặt.
**Done when:** vượt ngưỡng thì đăng nhập bị chặn kể cả khi nhập đúng mật khẩu, tới hạn thì tự mở; đăng nhập đúng thì bộ đếm về 0; Admin mở khóa sớm được; thông báo lỗi không tiết lộ tài khoản có tồn tại hay không.
- [ ] Design it (spec): `/architect khóa tạm khi đăng nhập sai nhiều lần`

## Slice 9: Quên mật khẩu qua email

### 13. Quên mật khẩu qua email · needs a decision · GA
Người dùng nhận link hoặc mã đặt lại mật khẩu qua email. Kéo theo việc chọn cách gửi email, nên cần một quyết định riêng.
**Done when:** yêu cầu đặt lại gửi email có link hoặc mã dùng một lần và có hạn; dùng xong hoặc hết hạn thì không dùng lại được; phản hồi giống nhau dù email có tồn tại hay không; đặt lại xong thì mọi phiên cũ bị thu hồi.
- [ ] Design it (spec): `/architect quên mật khẩu qua email`

## Deferred
Ngoài phạm vi đợt này, ghi lại để kế hoạch trung thực. Mỗi phân hệ sẽ được chia thành các tính năng nhỏ khi bạn chạy lại `/scope` tới lượt nó.
- **UC01 Căn hộ & Tòa nhà**: tòa nhà, tầng, căn hộ, khu vực dùng chung (10 API) · needs a decision
- **UC02 Cư trú & Nhân khẩu**: hồ sơ cư trú, phương tiện (7 API) · needs a decision
- **UC03 Chỉ số tiêu thụ**: đồng hồ điện nước, ghi chỉ số, ngoại lệ (6 API) · needs a decision
- **UC04 Tài chính & Công nợ**: biểu phí, tính phí, phát hành, đối soát (9 API) · needs a decision · GA
- **UC05 Thanh toán trực tuyến**: cổng thanh toán, webhook, biên nhận (5 API) · needs a decision · GA
- **UC06 Service Desk**: ticket cư dân, tệp đính kèm (4 API) · needs a decision
- **UC07 Điều phối & nghiệm thu SLA**: giao việc, tạm hoãn, đánh giá (7 API) · needs a decision
- **UC08 Bảo trì & thiết bị kỹ thuật**: kế hoạch, checklist, phiếu công việc (7 API) · needs a decision
- **UC09 Tiện ích & đặt chỗ**: slot, giữ chỗ 10 phút, QR check in (7 API) · needs a decision
- **UC10 Thông báo & truyền thông**: soạn, phát hành, lượt đọc (5 API) · needs a decision
- **UC11 Khảo sát & biểu quyết**: đợt khảo sát, nộp phiếu, kết quả (6 API) · needs a decision
- **UC12 Báo cáo & thống kê**: KPI, báo cáo, xuất file nền (5 API) · needs a decision
- **Gán nhân viên vào tòa nhà**: bảng gán để `phamVi.maToaNha` của nhân viên đúng khi khu có nhiều nhóm phụ trách riêng; hiện giả định nhân viên phụ trách mọi tòa · needs a decision · from spec 0004

## Legend

**The decision box.** Mỗi tính năng có đúng một ô quyết định, là ô có nhãn kết thúc bằng `(spec)`. Chữ có thể khác nhau (`Design it (spec)` thông thường, `Decide the stack (spec)` ở Stack & kiến trúc), nên các skill tìm nó theo hậu tố `(spec)`, không theo nhãn chính xác. Mọi ô khác là ô thực thi, và `/architect` không bao giờ tick chúng.

**Feature lifecycle**: scope cập nhật khi một tính năng tiến lên; mỗi dòng là trạng thái nó hiển thị và ai đặt nó:

| State | Set by | The feature shows |
|---|---|---|
| `planned` · needs a decision | `/scope` | một ô: `Design it (spec): /architect <feature>` |
| `in-progress` (designed) | **`/architect` khi chốt spec** | `Design it` đã tick; có link spec; `Build it: /develop <feature>` kèm **2 đến 5 mốc**; các ô kết thúc theo tier (`Verify it` từ Alpha, `Test it` từ Beta, `Review it` + `Document it` ở GA) |
| `in-progress` (building) | `/develop` | các ô mốc được tick dần; có đường dẫn code |
| `in-progress` (verified) | `/check verify` | `Build it` và các mốc đã tick; `Verify it` đã tick |
| `done` | **bạn, khi bạn thấy đủ** (skill nào cũng đặt được khi bạn bảo); `/sync` đối chiếu | các ô bạn đã chạy được tick, ô bỏ qua ghi là skipped; bước cuối của tier (`Prototype` sau `/develop`; `Alpha` sau `/check verify`; `Beta`/`GA` sau `/test`) là thời điểm gợi ý để gọi là xong |

- **Bước tiếp theo** = ô đầu tiên chưa tick (luôn là một lệnh hoặc một mốc được theo dõi).
- **needs a decision** = chạy `/architect` trước; nếu không có tag này thì đi thẳng `/develop` (hoặc `/audit` cho quy chuẩn & công cụ). Tag mất đi khi spec được chốt.
- **Việc build chi tiết nằm trong `## Build plan` của spec, không nằm ở đây**: scope chỉ giữ phần tóm tắt theo mốc.
- **Status** `planned` → `in-progress` → `done`, cộng thêm `existing` (có trước workflow) và `dropped` (bỏ khỏi phạm vi, giữ lại làm lịch sử).
- **Approach tag** cạnh tiêu đề (ví dụ `· Facade`) ghi đè cách làm mặc định cho riêng tính năng đó; không có tag là dùng mặc định.
- **Workflow tier tag** cạnh tiêu đề (ví dụ `· GA`) đặt mức kỹ lưỡng riêng cho tính năng đó; không có tag là dùng mặc định. Nó quyết định các ô kiểm tra và gợi ý tiếp theo của mỗi skill.
- **Workflow** (dòng đầu file) là mặc định của dự án, những gì chạy sau `/develop`: **Prototype** = không gì cả; **Alpha** = `/check verify`; **Beta** = `/check verify` rồi `/test`; **GA** = thêm `/check review` bằng model khác, rồi `/document`. Tính năng xây trên một quyết định chưa phê duyệt (spec `Assumed`) vẫn bị đánh dấu, nhưng không bao giờ chặn `done`.
- **Pointer line** (`spec <n> · code in <path>`): link spec do `/architect` thêm, đường dẫn code do `/develop` thêm.
