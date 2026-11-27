# Scope: Hệ thống Quản lý Chung cư

Hệ thống quản lý vận hành chung cư cho ban quản lý (BQL), kỹ thuật, kế toán, ban quản trị (BQT) và cư dân: căn hộ, cư trú, điện nước, phí, thanh toán, sự cố, bảo trì, tiện ích, thông báo, khảo sát và báo cáo. Đồ án IT3930 của nhóm 2 người, backend Java Spring Boot, frontend NextJS, cơ sở dữ liệu PostgreSQL. Đợt này làm nền tảng và phân hệ Xác thực & Bảo mật trước, rồi tới UC01 Căn hộ & Tòa nhà và UC02 Cư trú & Nhân khẩu; 10 phân hệ nghiệp vụ còn lại nằm ở mục Deferred.

**Build approach:** Tracer Bullet (mỗi lát cắt chạy thật xuyên suốt DB, API và giao diện; lát sau làm dày thêm một đoạn của đường đã chạy).
**Workflow:** Beta (sau `/develop` chạy `/check verify`, rồi `/test`). Đây là mức kỹ lưỡng mặc định của dự án. Từ tính năng 17 trở đi, mỗi tính năng đi theo năm bước: `/architect` một spec cho cả backend lẫn frontend, `/develop <feature> backend`, `/check verify <feature> backend` (chỉ API và database), `/develop <feature> frontend`, rồi `/check verify <feature>` cho cả hai phần; sau đó `/test` như cũ. Nhánh git theo phân hệ (`feat/<phân hệ>`, ví dụ `feat/cutru`), mỗi tính năng nhỏ trong phân hệ là một commit khi xong cả backend lẫn frontend. `/architect` là điểm dừng đầu tiên nên ghé khi một tính năng còn quyết định chưa chốt, nhưng bạn có thể bỏ qua nếu đã biết cách làm. Tính năng nào cũng có thể mang tag riêng (ví dụ `· GA`) để làm kỹ hơn hoặc nhẹ hơn.

_Đây là gợi ý để việc xây dựng có trật tự, không phải yêu cầu bắt buộc. Bỏ qua phần nào không hợp: nếu bạn đã biết cách làm một tính năng, cứ dùng `/develop` và bỏ qua `/architect`. Bạn là người quyết định khi nào một tính năng là `done`._

Tài liệu nguồn: `sql/database_schema_quan_ly_chung_cu.sql` (38 bảng) và `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx` (80 API, phân hệ Xác thực gồm API-AUTH-01 đến API-AUTH-05).

## At a glance

| #   | Feature                              | Phase      | Status      |
| --- | ------------------------------------ | ---------- | ----------- |
| 1   | Stack & kiến trúc                    | Foundation | done        |
| 2   | Quy chuẩn code & công cụ             | Foundation | done        |
| 3   | Mô hình dữ liệu                      | Foundation | done        |
| 4   | Design system & nền tảng giao diện   | Foundation | done        |
| 5   | Đăng nhập & trang hồ sơ              | Slice 1    | done        |
| 6   | Làm mới phiên & đăng xuất            | Slice 2    | done        |
| 7   | Phân quyền theo vai trò & bố cục app | Slice 3    | done        |
| 8   | Ghi nhật ký kiểm toán dùng chung     | Slice 4    | done        |
| 9   | Màn hình tra cứu nhật ký kiểm toán   | Slice 5    | done        |
| 10  | Đổi mật khẩu                         | Slice 6    | done        |
| 11  | Quản lý tài khoản (Admin)            | Slice 7    | done        |
| 12  | Khóa tạm khi đăng nhập sai nhiều lần | Slice 8    | done        |
| 13  | Quên mật khẩu qua email              | Slice 9    | done        |
| 14  | Tòa nhà, tầng & khu vực dùng chung   | Slice 10   | done        |
| 15  | Tìm kiếm & quản lý căn hộ            | Slice 11   | done        |
| 16  | Tạm dừng quyền tiện ích căn hộ       | Slice 12   | done        |
| 17  | Hồ sơ cư trú căn hộ                  | Slice 13   | done        |
| 18  | Đề nghị cư trú & tra cứu hồ sơ       | Slice 14   | done        |
| 19  | Phương tiện gửi xe căn hộ            | Slice 15   | done        |

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

### 5. Đăng nhập & trang hồ sơ · done · GA

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

### 6. Làm mới phiên & đăng xuất · done

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

### 7. Phân quyền theo vai trò & bố cục app · done

Một cách phân quyền dùng chung cho cả 80 API: xác định vai trò từ các bảng con (NhanVienBQL, NhanVienKyThuat, KeToan, ThanhVienBanQuanTri, CuDan) cùng Admin, chặn mỗi endpoint theo cột RBAC trong file đặc tả, và bố cục app NextJS có menu theo vai trò. Phân hệ nào sau này cũng dựa vào nó.
**Done when:** mỗi vai trò chỉ thấy menu và gọi được API của mình; gọi API không đủ quyền trả về 403 và giao diện hiện trang báo không có quyền; một người có nhiều vai trò vẫn được xử lý đúng.
spec [0006](../specs/0006-phan-quyen-bo-cuc-app/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/auth/phanquyen/`, `backend/src/main/java/vn/edu/hust/chungcu/canho/`, `frontend/src/lib/phan-quyen/`, `frontend/src/app/(app)/`

- [x] Design it (spec): `/architect phân quyền theo vai trò & bố cục app`
- [x] Build it: `/develop phân quyền theo vai trò & bố cục app`
  - [x] Đường mỏng: `@ChoPhep`, interceptor, `GET /buildings`, menu từ hồ sơ, trang `/toa-nha` tối thiểu (AC-2, AC-7, AC-10, AC-15)
  - [x] Làm dày backend: test bắt buộc khai, `PublicPaths.CONG_KHAI`, log, OpenAPI, helper phân trang, service phạm vi (AC-1, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9)
  - [x] Làm dày giao diện: ma trận đủ, chặn trang, `KhongCoQuyen`, trạng thái đang tải và lỗi (AC-10, AC-11, AC-12, AC-13, AC-15)
  - [x] Trang Tổng quan và khóa lại: trang `/`, a11y, `verify.md`, README (AC-14, AC-16)
- [x] Verify it: `/check verify phân quyền theo vai trò & bố cục app`
- [x] Test it: `/test phân quyền theo vai trò & bố cục app`
- [x] Review it (fresh model): `/check review phân quyền theo vai trò & bố cục app`

## Slice 4: Ghi nhật ký kiểm toán dùng chung

### 8. Ghi nhật ký kiểm toán dùng chung · done

Cơ chế ghi vào NhatKyHeThong mà mọi phân hệ cắm vào được, đặt trước các tính năng tạo ra sự kiện cần ghi. Đợt này ghi các sự kiện xác thực: đăng nhập thành công và thất bại, đăng xuất, đổi mật khẩu, khóa và mở khóa tài khoản.
**Done when:** mỗi sự kiện xác thực tạo đúng một dòng log có người thực hiện, loại thao tác, đối tượng và chi tiết thay đổi; không ghi mật khẩu hay token vào log; ghi log lỗi không làm hỏng thao tác chính.
spec [0007](../specs/0007-nhat-ky-kiem-toan-dung-chung/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/nhatky/`, `backend/src/main/resources/db/migration/V11__nhat_ky_he_thong.sql`, `backend/src/main/java/vn/edu/hust/chungcu/auth/service/`

- [x] Design it (spec): `/architect ghi nhật ký kiểm toán dùng chung`
- [x] Build it: `/develop ghi nhật ký kiểm toán dùng chung`
  - [x] Đường mỏng: V11, `NhatKyService.ghi` trong transaction riêng, gắn đăng nhập thành công và thất bại (AC-1, AC-2, AC-3, AC-10)
  - [x] Ghi sau commit: đăng xuất và phát hiện dùng lại token (AC-4, AC-5, AC-6)
  - [x] Chịu lỗi và giữ bí mật: log ERROR, chặn tên khóa nhạy cảm, test quét, trigger và CHECK (AC-7, AC-8, AC-9)
- [x] Verify it: `/check verify ghi nhật ký kiểm toán dùng chung`
- [x] Test it: `/test ghi nhật ký kiểm toán dùng chung`

## Slice 5: Màn hình tra cứu nhật ký kiểm toán

### 9. Màn hình tra cứu nhật ký kiểm toán · done

Admin, BQT và BQL xem lịch sử thao tác có phân trang (API-AUTH-05). Dựa trên spec của tính năng 8.
**Done when:** người có quyền xem được danh sách log mới nhất trước, phân trang và lọc theo người dùng, loại thao tác, khoảng thời gian; vai trò khác bị chặn; trạng thái rỗng và lỗi hiển thị đúng.
spec [0007](../specs/0007-nhat-ky-kiem-toan-dung-chung/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/nhatky/`, `backend/src/main/java/vn/edu/hust/chungcu/auth/service/NguoiDungTraCuuService.java`, `frontend/src/app/(app)/nhat-ky/`

- [x] Build it: `/develop màn hình tra cứu nhật ký kiểm toán`
  - [x] API đọc: danh sách có bộ lọc, danh mục loại thao tác, họ tên theo lô (AC-11, AC-12, AC-13, AC-14)
  - [x] Màn hình `/nhat-ky`: bảng, bộ lọc trên URL, trạng thái rỗng và lỗi, mục menu (AC-11, AC-12, AC-14)

## Slice 6: Đổi mật khẩu

### 10. Đổi mật khẩu · done

Người dùng đã đăng nhập đổi mật khẩu sau khi nhập đúng mật khẩu cũ (API-AUTH-04). Dựa trên spec của tính năng 5 và 8.
**Done when:** sai mật khẩu cũ bị từ chối; mật khẩu mới phải đạt quy tắc độ mạnh; đổi xong thì các phiên khác bị thu hồi và có dòng nhật ký kiểm toán.
không có spec riêng (build theo API-AUTH-04 trong `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx`, spec 0004, 0005, 0007) · code in `backend/src/main/java/vn/edu/hust/chungcu/auth/service/DoiMatKhauService.java`, `backend/src/main/java/vn/edu/hust/chungcu/auth/service/GhiMatKhauMoiService.java`, `frontend/src/app/(app)/doi-mat-khau/`

- [x] Build it: `/develop đổi mật khẩu`
  - [x] API `PUT /api/v1/auth/change-password`: kiểm mật khẩu cũ, quy tắc độ mạnh, thu hồi phiên khác, nhật ký `AUTH_DOI_MAT_KHAU`
  - [x] Trang `/doi-mat-khau`, lối vào từ menu người dùng và trang hồ sơ

## Slice 7: Quản lý tài khoản (Admin)

### 11. Quản lý tài khoản (Admin) · done

Admin hoặc BQL tạo tài khoản cho từng vai trò, gán vai trò, khóa và mở khóa (trangThaiTaiKhoan). Đây là cách duy nhất để có tài khoản, vì cư dân không tự đăng ký.
**Done when:** Admin tìm, tạo, sửa, khóa và mở khóa tài khoản trên màn hình; trùng số điện thoại, email hoặc CCCD bị báo lỗi rõ; tài khoản bị khóa không đăng nhập được và mất phiên đang có; mọi thay đổi có nhật ký kiểm toán.
spec [0008](../specs/0008-quan-ly-tai-khoan/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/auth/`, `backend/src/main/resources/db/migration/V12__quan_ly_tai_khoan.sql`, `frontend/src/app/(app)/tai-khoan/`, `frontend/src/lib/tai-khoan.ts`

- [x] Design it (spec): `/architect quản lý tài khoản (admin)`
- [x] Build it: `/develop quản lý tài khoản (admin)`
  - [x] Đường mỏng: V12, tạo tài khoản đủ bảy vai trò, mật khẩu tạm, danh sách tối thiểu, `/tai-khoan` và `/tai-khoan/moi` (AC-1, AC-2, AC-4, AC-16, AC-17, AC-18)
  - [x] Bắt đổi mật khẩu xuyên suốt: claim, interceptor, cấp lại access token, chuyển trang (AC-13, AC-14)
  - [x] Chi tiết, sửa thông tin và vai trò: trùng thông tin, `phienBan`, gỡ vai trò bị chặn, trang chi tiết (AC-3, AC-6, AC-7, AC-8, AC-9, AC-16)
  - [x] Trạng thái và đặt lại mật khẩu: thu hồi phiên, tự bảo vệ, Admin cuối, khóa dòng (AC-9, AC-10, AC-11, AC-12, AC-16)
  - [x] Phạm vi BQL, tìm kiếm đủ, khóa lại: cư dân thuần, `unaccent`, a11y, `verify.md` (AC-5, AC-15, AC-16, AC-17)
- [x] Verify it: `/check verify quản lý tài khoản (admin)`
- [x] Test it: `/test quản lý tài khoản (admin)`

## Slice 8: Khóa tạm khi đăng nhập sai nhiều lần

### 12. Khóa tạm khi đăng nhập sai nhiều lần · done

Chặn dò mật khẩu: sai liên tiếp N lần thì tạm khóa đăng nhập trong một khoảng thời gian. Cần chốt ngưỡng, thời gian khóa, và cách phân biệt với trạng thái `Tạm khóa` do Admin đặt.
**Done when:** vượt ngưỡng thì đăng nhập bị chặn kể cả khi nhập đúng mật khẩu, tới hạn thì tự mở; đăng nhập đúng thì bộ đếm về 0; Admin mở khóa sớm được; thông báo lỗi không tiết lộ tài khoản có tồn tại hay không.
spec [0009](../specs/0009-khoa-tam-dang-nhap-sai/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/auth/` (KhoaTam\*, MoKhoaDangNhapService) và `frontend/src/app/dang-nhap/`, `frontend/src/app/(app)/tai-khoan/[ma]/`

- [x] Design it (spec): `/architect khóa tạm khi đăng nhập sai nhiều lần`
- [x] Build it: `/develop khóa tạm khi đăng nhập sai nhiều lần`
  - [x] Đường mỏng cho tài khoản thật: giữ lượt, 429 kèm `Retry-After`, cửa sổ 15 phút, câu báo số phút (AC-1, AC-2, AC-3, AC-4, AC-7, AC-13, AC-15, AC-18)
  - [x] Định danh ảo và chạy song song: bảng `dang_nhap_sai_dinh_danh`, phản hồi giống hệt, xóa khi định danh có chủ, test 20 luồng (AC-5, AC-6, AC-7, AC-19)
  - [x] Phía người quản lý: `khoaTamDen`, `DELETE .../login-lock`, đặt lại mật khẩu xóa khóa, thẻ Trạng thái và bảo mật (AC-8, AC-9, AC-10, AC-14, AC-16)
  - [x] Nhật ký, log, job dọn và khóa lại: hai loại nhật ký mới, trang `/nhat-ky`, câu gợi ý, a11y, `verify.md` (AC-11, AC-12, AC-13, AC-14, AC-17)
- [x] Verify it: `/check verify khóa tạm khi đăng nhập sai nhiều lần`
- [x] Test it: `/test khóa tạm khi đăng nhập sai nhiều lần`

## Slice 9: Quên mật khẩu qua email

### 13. Quên mật khẩu qua email · done · GA

Người dùng nhận link hoặc mã đặt lại mật khẩu qua email. Kéo theo việc chọn cách gửi email, nên cần một quyết định riêng.
**Done when:** yêu cầu đặt lại gửi email có link hoặc mã dùng một lần và có hạn; dùng xong hoặc hết hạn thì không dùng lại được; phản hồi giống nhau dù email có tồn tại hay không; đặt lại xong thì mọi phiên cũ bị thu hồi.
spec [0010](../specs/0010-quen-mat-khau-email/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/auth/` (`QuenMatKhau*`), `common/email/`, `frontend/src/app/quen-mat-khau/`, `frontend/src/app/dat-lai-mat-khau/`

- [x] Design it (spec): `/architect quên mật khẩu qua email`
- [x] Build it: `/develop quên mật khẩu qua email`
  - [x] Đường mảnh từ trang tới hộp thư: `V14`, cấu hình mail và `AppProperties`, Mailpit, `EmailSender`, gửi sau commit, `request` và `confirm` cơ bản, hai trang tối thiểu, GreenMail (AC-1, AC-7, AC-14)
  - [x] Không lộ tài khoản và giới hạn: phản hồi giống nhau, chuẩn hóa email, SMTP hỏng, 60 giây, 5 lần/giờ, trần chung, một link mở (AC-2, AC-3, AC-4, AC-5)
  - [x] An toàn của token và các luồng khác: `check`, một lỗi cho mọi token hỏng, một lần dùng, thứ tự khóa, hủy link khi đổi mật khẩu, khóa hay sửa email (AC-6, AC-8, AC-9, AC-10)
  - [x] Nhật ký, job dọn và khóa lại giao diện: hai loại nhật ký, dọn 7 ngày, trạng thái và a11y của hai trang, `Referrer-Policy`, `verify.md` (AC-11, AC-12, AC-13, AC-15)
- [x] Verify it: `/check verify quên mật khẩu qua email`
- [x] Test it: `/test quên mật khẩu qua email`
- [x] Review it (fresh model): `/check review quên mật khẩu qua email`
- [x] Document it: `/document quên mật khẩu qua email`

## Slice 10: Tòa nhà, tầng & khu vực dùng chung

### 14. Tòa nhà, tầng & khu vực dùng chung · done

Phần đầu của UC01. BQL xem danh mục tòa nhà (số tầng, số căn hộ), thêm tòa nhà và thêm tầng; BQL và kỹ thuật xem danh mục khu vực dùng chung (API-UC01-01 đến 04, API-UC01-10). Đây là khung không gian mà mọi phân hệ sau dựa vào.
**Done when:** BQL thêm được tòa nhà và tầng, mã trùng hoặc tầng sai quan hệ phân cấp bị từ chối với mã lỗi rõ ràng; danh mục tòa nhà, tầng và khu vực dùng chung hiện đúng cho đúng vai trò, trên cả API lẫn giao diện.
spec [0011](../specs/0011-toa-nha-tang-khu-vuc/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/canho/`, `backend/src/main/resources/db/migration/V15__toa_nha_tang.sql`, `frontend/src/app/(app)/toa-nha/`, `frontend/src/app/(app)/khu-vuc-chung/`

- [x] Design it (spec): `/architect tòa nhà, tầng & khu vực dùng chung`
- [x] Build it: `/develop tòa nhà, tầng & khu vực dùng chung`
  - [x] Đường mỏng thêm tòa: V15, `ToaNhaGhi`, `POST /buildings`, nhật ký, dialog trên `/toa-nha` (AC-1, AC-2, AC-3, AC-6, AC-7, AC-13, AC-14, AC-15, AC-19)
  - [x] Chi tiết tòa và danh sách tầng: `GET /buildings/{id}`, `GET .../floors`, mở UC01-01 cho Kỹ thuật, trang `/toa-nha/[maToaNha]` (AC-8, AC-9, AC-10, AC-16)
  - [x] Thêm tầng dưới khóa dòng, dialog xác nhận và xử lý 422, test chạy song song (AC-4, AC-5, AC-7, AC-14, AC-17)
  - [x] Khu vực dùng chung: seed `R__seed_02_khong_gian.sql`, `GET /common-areas`, trang `/khu-vuc-chung` (AC-11, AC-12, AC-13, AC-18)
- [x] Verify it: `/check verify tòa nhà, tầng & khu vực dùng chung`
- [x] Test it: `/test tòa nhà, tầng & khu vực dùng chung`

## Slice 11: Tìm kiếm & quản lý căn hộ

### 15. Tìm kiếm & quản lý căn hộ · done

BQL, kế toán và BQT tìm căn hộ có phân trang theo tòa, tầng, diện tích, trạng thái sử dụng; BQL tạo và sửa căn hộ; BQL và cư dân xem chi tiết căn hộ (API-UC01-05 đến 08). Phần nhân khẩu, phương tiện, đồng hồ trong trang chi tiết chỉ hiện khi UC02 và UC03 đã có.
**Done when:** tìm và lọc trả đúng kết quả có phân trang; mã căn hộ trùng hoặc không hợp lệ theo tầng bị từ chối; cư dân chỉ xem được căn hộ của mình; sửa diện tích và trạng thái sử dụng được ghi nhật ký kiểm toán.

spec [0012](../specs/0012-tim-kiem-quan-ly-can-ho/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/canho/`, `backend/src/main/resources/db/migration/V16__can_ho.sql`, `backend/src/main/resources/db/seed/R__seed_03_can_ho.sql`, `frontend/src/app/(app)/can-ho/`, `frontend/src/app/(app)/can-ho-cua-toi/`

- [x] Design it (spec): `/architect tìm kiếm & quản lý căn hộ`
- [x] Build it: `/develop tìm kiếm & quản lý căn hộ`
  - [x] Đường mỏng danh sách căn: `GET /apartments` có phân trang, sort, trang `/can-ho` (AC-1, AC-4, AC-15, AC-21)
  - [x] Tạo căn: V16, mã do server ghép, `POST /apartments`, kiểm trùng và chạy song song, nhật ký, dialog "Thêm căn hộ" (AC-7, AC-8, AC-9, AC-10, AC-14, AC-18)
  - [x] Bộ lọc và seed: `R__seed_03_can_ho.sql`, sửa 4 assert seed, lọc tòa, tầng, diện tích, trạng thái, `q` trên URL (AC-1, AC-2, AC-3, AC-16, AC-17)
  - [x] Chi tiết và sửa: `GET` và `PUT /apartments/{id}` dưới khóa dòng, nhật ký cũ mới, trang `/can-ho/[maCanHo]` (AC-5, AC-6, AC-11, AC-12, AC-13, AC-14, AC-19)
  - [x] Căn hộ của tôi: `/can-ho-cua-toi` và trang chi tiết của cư dân (AC-6, AC-19, AC-20, AC-21)
- [x] Verify it: `/check verify tìm kiếm & quản lý căn hộ`
- [x] Test it: `/test tìm kiếm & quản lý căn hộ`

## Slice 12: Tạm dừng quyền tiện ích căn hộ

### 16. Tạm dừng quyền tiện ích căn hộ · done

BQL hoặc kế toán bật, tắt cờ tạm dừng tiện ích của một căn hộ khi nợ phí quá hạn (API-UC01-09). Cần chốt cờ này bật tay hay tự động theo công nợ (UC04 chưa có), và ai được mở khóa.
**Done when:** người có quyền bật và tắt được cờ kèm lý do, thay đổi được ghi nhật ký kiểm toán, và trạng thái cờ hiện trên trang chi tiết căn hộ để phân hệ tiện ích (UC09) đọc được sau này.

spec [0013](../specs/0013-tam-dung-tien-ich-can-ho/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/canho/`, `frontend/src/app/(app)/can-ho/`

- [x] Design it (spec): `/architect tạm dừng quyền tiện ích căn hộ`
- [x] Build it: `/develop tạm dừng quyền tiện ích căn hộ`
  - [x] Đường mỏng tạm dừng: V17, `TamDungTienIchService` dưới khóa dòng, `PATCH /facility-suspension`, 409 khi không đổi, nhật ký hai loại, nút và dialog trên `/can-ho/[maCanHo]` (AC-1, AC-2, AC-3, AC-4, AC-5, AC-7, AC-10, AC-13, AC-15, AC-17)
  - [x] Hiện lần đổi: `lanDoiTienIch` có họ tên trên mọi response căn, ẩn người đổi với cư dân, hai dòng mới trên trang chi tiết (AC-8, AC-9, AC-14, AC-17)
  - [x] Bộ lọc và seed: tham số `biTamDungTienIch`, ô lọc "Tiện ích", căn `A-0204` bị tạm dừng trong seed (AC-11, AC-12, AC-16, AC-17)
  - [x] Song song và hoàn tất: test hai yêu cầu cùng lúc, sửa constructor trong test, kiểm kiểu OpenAPI, `mvnw verify` (AC-6, AC-13)
- [x] Verify it: `/check verify tạm dừng quyền tiện ích căn hộ`
- [x] Test it: `/test tạm dừng quyền tiện ích căn hộ`

## Slice 13: Hồ sơ cư trú căn hộ

### 17. Hồ sơ cư trú căn hộ · done

Phần đầu của UC02, nối tiếp `CuTruService` đang cấp phạm vi căn hộ cho cư dân. BQL ghi nhận và cập nhật hồ sơ cư trú cho cư dân đã có tài khoản (`cu_dan`), BQL và cư dân xem nhân khẩu đang ở cùng lịch sử cư trú ngay trên trang chi tiết căn hộ (`API-UC02-01` đến `03`), đồng thời chốt luôn luật đồng bộ với `trang_thai_su_dung` của căn hộ.
**Done when:** BQL thêm, sửa và kết thúc được hồ sơ cư trú kèm nhật ký kiểm toán; nhân khẩu và lịch sử hiện đúng trên `/can-ho/[maCanHo]` và `/can-ho-cua-toi` mà không lộ dữ liệu cá nhân ngoài phạm vi; trạng thái sử dụng của căn hộ và quyền xem căn của cư dân phản ánh đúng hồ sơ hợp lệ.

spec [0014](../specs/0014-ho-so-cu-tru-can-ho/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/cutru/`, `frontend/src/lib/cu-tru.ts`, `frontend/src/app/(app)/can-ho/`, `frontend/src/components/ui/`

- [x] Design it (spec): `/architect hồ sơ cư trú căn hộ`
- [x] Build backend: `/develop hồ sơ cư trú căn hộ backend`
  - [x] Đường mỏng đọc và thêm hồ sơ: `V18`, `CuTruDinhDanhService`, `CanHoCuTruService`, `GET .../residents`, `resident-options`, `POST /residency-records`, nhật ký và đồng bộ căn (AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-9, AC-10, AC-12, AC-13)
  - [x] Làm dày sửa, kết thúc và chặn sửa tay căn hộ: `PUT /residency-records/{id}`, hủy sớm `sapBatDau`, kiểm 422 ở `SuaCanHoService` và `ThemCanHoService` (AC-7, AC-8, AC-10, AC-12, AC-13)
  - [x] Job hết hạn đầu ngày, seed và OpenAPI: `HoSoCuTruHetHanJob` kèm chạy bù lúc khởi động, `R__seed_04_cu_tru.sql`, `mvnw verify` (AC-11, AC-12, AC-13, AC-14)
- [x] Verify backend: `/check verify hồ sơ cư trú căn hộ backend`
- [x] Build frontend: `/develop hồ sơ cư trú căn hộ frontend`
  - [x] Khối Hồ sơ cư trú trên `ChiTietCanHo`: hai tab Hiện tại và Lịch sử, phân quyền hiển thị, ẩn PII và trạng thái tải, rỗng, lỗi độc lập (AC-15, AC-16, AC-18)
  - [x] Hộp thoại thao tác BQL và hoàn tất: Thêm, Sửa, Kết thúc hoặc Hủy cư trú, sửa `ThemCanHoDialog`, làm mới cache và kiểm tra WCAG AA (AC-17, AC-18)
- [x] Verify it: `/check verify hồ sơ cư trú căn hộ`
- [x] Test it: `/test hồ sơ cư trú căn hộ`

## Slice 14: Đề nghị cư trú & tra cứu hồ sơ

### 18. Đề nghị cư trú & tra cứu hồ sơ · done

Cư dân gửi đề nghị thêm nhân khẩu hoặc đăng ký tạm trú từ `/can-ho-cua-toi` ở trạng thái chờ xác minh (`API-UC02-04`), và BQL dùng màn hình tập trung `/ho-so-cu-tru` để lọc hồ sơ toàn tòa cũng như duyệt hoặc từ chối đề nghị mà không phải mở từng căn. Cần chốt cách lưu hồ sơ khi chưa có nhân viên xác minh và luồng từ chối đề nghị.
**Done when:** cư dân gửi được đề nghị cho căn hộ mình đang ở và theo dõi được trạng thái trên web; BQL lọc được danh sách hồ sơ cư trú có phân trang trên `/ho-so-cu-tru`, xử lý được hàng đợi chờ xác minh và mọi quyết định đều có nhật ký kiểm toán.

spec [0015](../specs/0015-de-nghi-cu-tru-tra-cuu/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/cutru/`, `frontend/src/lib/cu-tru.ts`, `frontend/src/app/(app)/can-ho/`, `frontend/src/app/(app)/ho-so-cu-tru/`, `frontend/src/lib/phan-quyen/muc-chuc-nang.ts`

- [x] Design it (spec): `/architect đề nghị cư trú & tra cứu hồ sơ`
- [x] Build backend: `/develop đề nghị cư trú & tra cứu hồ sơ backend`
  - [x] Đường mỏng đề nghị cư trú: `V19`, gửi đề nghị `resident-request`, mảng `deNghiCuTru` trên căn hộ (AC-1, AC-2, AC-3, AC-8, AC-11, AC-12, AC-13)
  - [x] Vòng đời đề nghị: `approve`, `reject`, `cancel` dưới khóa dòng, đồng bộ căn và khóa trạng thái kết thúc (AC-4, AC-5, AC-6, AC-7, AC-11, AC-12, AC-13)
  - [x] Tra cứu tập trung toàn tòa: `GET /residency-records` có phân trang và bộ lọc, seed đề nghị mẫu, OpenAPI (AC-9, AC-10, AC-12, AC-13, AC-14)
- [x] Verify backend: `/check verify đề nghị cư trú & tra cứu hồ sơ backend`
- [x] Build frontend: `/develop đề nghị cư trú & tra cứu hồ sơ frontend`
  - [x] Luồng đề nghị trên trang căn hộ và `/can-ho-cua-toi`: tab Đề nghị cư trú, hộp thoại gửi, hủy, duyệt, từ chối (AC-15, AC-18)
  - [x] Màn hình tập trung `/ho-so-cu-tru` và hoàn tất: thanh lọc nhanh kèm số đếm, bảng sắp xếp, thêm hồ sơ chọn căn, WCAG AA (AC-16, AC-17, AC-18)
- [x] Verify it: `/check verify đề nghị cư trú & tra cứu hồ sơ`
- [x] Test it: `/test đề nghị cư trú & tra cứu hồ sơ`

## Slice 15: Phương tiện gửi xe căn hộ

### 19. Phương tiện gửi xe căn hộ · done

BQL, kế toán và cư dân xem danh sách phương tiện của căn hộ; BQL và cư dân đăng ký xe mới gắn với chủ sở hữu; BQL đổi trạng thái thẻ xe để phục vụ tính phí gửi xe ở UC04 (`API-UC02-05` đến `07`). Cần chốt quy ước biển số (kể cả xe đạp và đăng ký lại biển số đã hủy), điều kiện chủ xe cư trú tại căn và trạng thái ban đầu khi cư dân tự gửi đăng ký.
**Done when:** BQL và cư dân đăng ký được phương tiện trên trang căn hộ, biển số trùng đang hoạt động bị từ chối với mã lỗi rõ ràng; BQL chuyển được trạng thái đăng ký xe kèm nhật ký kiểm toán; BQL, kế toán và cư dân thấy đúng danh sách phương tiện trong phạm vi của mình.

spec [0016](../specs/0016-phuong-tien-gui-xe-can-ho/index.md) · code in `backend/src/main/java/vn/edu/hust/chungcu/phuongtien/`, `backend/src/main/resources/db/migration/V20__phuong_tien_gui_xe.sql`, `backend/src/main/resources/db/seed/R__seed_05_phuong_tien.sql`, `frontend/src/lib/phuong-tien.ts`, `frontend/src/app/(app)/can-ho/`

- [x] Design it (spec): `/architect phương tiện gửi xe căn hộ`
- [x] Build backend: `/develop phương tiện gửi xe căn hộ backend`
  - [x] Đường mỏng: `V20`, package `phuongtien`, xem danh sách theo góc nhìn và BQL đăng ký, chống trùng biển kể cả khi chạy song song (AC-1 đến AC-8, AC-12)
  - [x] Làm dày: đơn cư dân giới hạn 10, ma trận đổi trạng thái kèm kiểm lại chủ xe và biển khi duyệt, rút đơn (AC-5, AC-7, AC-9, AC-10, AC-11, AC-12)
  - [x] Dữ liệu mẫu `R__seed_05` và khóa OpenAPI (AC-13)
- [x] Verify backend: `/check verify phương tiện gửi xe căn hộ backend`
- [x] Build frontend: `/develop phương tiện gửi xe căn hộ frontend`
  - [x] Khối Phương tiện hai tab trên trang căn hộ, hiện theo vai trò (AC-14)
  - [x] Hộp thoại đăng ký, đổi trạng thái, rút đơn, câu lỗi và WCAG AA (AC-15, AC-16)
- [x] Verify it: `/check verify phương tiện gửi xe căn hộ`
- [x] Test it: `/test phương tiện gửi xe căn hộ`

## Deferred

Ngoài phạm vi đợt này, ghi lại để kế hoạch trung thực. Mỗi phân hệ sẽ được chia thành các tính năng nhỏ khi bạn chạy lại `/scope` tới lượt nó.

- **Giới hạn đăng nhập theo IP**: chống dò rải trên nhiều tài khoản và giảm việc cố tình khóa tài khoản người khác; cần chốt có tin `X-Forwarded-For` sau proxy hay không · needs a decision · from spec 0009
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
- **Gán nhân viên vào tòa nhà**: bảng gán để `phamVi.maToaNha` của nhân viên đúng khi khu có nhiều nhóm phụ trách riêng; hiện giả định nhân viên phụ trách mọi tòa; khi làm, tòa vừa thêm cần gán cho người tạo · needs a decision · from spec 0004, spec 0011
- **Sửa và xóa tòa nhà, tầng**: sửa tên, địa chỉ tòa; xóa tòa hoặc tầng chưa có căn hộ (ngoài 81 API) · needs a decision · from spec 0011
- **Xóa căn hộ và đổi tầng của căn nhập nhầm**: xóa căn chưa có dữ liệu liên quan, chuyển căn sang tầng khác (ngoài 81 API) · needs a decision · from spec 0012
- **Tầng hầm và đánh số tầng đặc biệt**: tầng hầm, tầng lửng, bỏ số 13; mô hình hiện chỉ có tầng 1 đến 99 liên tục · needs a decision · from spec 0011
- **Nhân khẩu phụ thuộc không có tài khoản**: ghi nhận trẻ nhỏ hoặc người phụ thuộc chưa có số điện thoại và email riêng trong `cu_dan` · needs a decision
- **Tra cứu phương tiện toàn tòa**: màn hình danh sách để BQL và kế toán tìm xe theo biển số, loại xe và trạng thái thẻ xe trên toàn khu (ngoài 81 API) · needs a decision
- **Sửa thông tin phương tiện**: BQL sửa nhãn hiệu màu sắc hoặc biển số của xe đã đăng ký thay vì hủy rồi đăng ký lại (ngoài 81 API) · needs a decision · from spec 0016
- **Tự tạm dừng thẻ xe khi chủ xe thôi cư trú**: nối sự kiện sau commit từ `cutru` sang `phuongtien` nếu nhãn cảnh báo không đủ · needs a decision · from spec 0016

## Legend

**The decision box.** Mỗi tính năng có đúng một ô quyết định, là ô có nhãn kết thúc bằng `(spec)`. Chữ có thể khác nhau (`Design it (spec)` thông thường, `Decide the stack (spec)` ở Stack & kiến trúc), nên các skill tìm nó theo hậu tố `(spec)`, không theo nhãn chính xác. Mọi ô khác là ô thực thi, và `/architect` không bao giờ tick chúng.

**Feature lifecycle**: scope cập nhật khi một tính năng tiến lên; mỗi dòng là trạng thái nó hiển thị và ai đặt nó:

| State                            | Set by                                                                            | The feature shows                                                                                                                                                                                                                                                                                                                                                       |
| -------------------------------- | --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `planned` · needs a decision     | `/scope`                                                                          | một ô: `Design it (spec): /architect <feature>`                                                                                                                                                                                                                                                                                                                         |
| `in-progress` (designed)         | **`/architect` khi chốt spec**                                                    | `Design it` đã tick (một spec cho cả backend và frontend); có link spec; `Build backend: /develop <feature> backend` và `Build frontend: /develop <feature> frontend`, mỗi ô kèm các mốc của phần đó (tổng **2 đến 5 mốc**); `Verify backend` sau `Build backend`; các ô kết thúc theo tier (`Verify it` từ Alpha, `Test it` từ Beta, `Review it` + `Document it` ở GA) |
| `in-progress` (building)         | `/develop`                                                                        | các ô mốc được tick dần; có đường dẫn code                                                                                                                                                                                                                                                                                                                              |
| `in-progress` (backend verified) | `/check verify <feature> backend`                                                 | `Build backend` và các mốc của nó đã tick; `Verify backend` đã tick                                                                                                                                                                                                                                                                                                     |
| `in-progress` (verified)         | `/check verify <feature>`                                                         | `Build frontend` và các mốc đã tick; `Verify it` (cả backend lẫn frontend) đã tick                                                                                                                                                                                                                                                                                      |
| `done`                           | **bạn, khi bạn thấy đủ** (skill nào cũng đặt được khi bạn bảo); `/sync` đối chiếu | các ô bạn đã chạy được tick, ô bỏ qua ghi là skipped; bước cuối của tier (`Prototype` sau `/develop`; `Alpha` sau `/check verify`; `Beta`/`GA` sau `/test`) là thời điểm gợi ý để gọi là xong                                                                                                                                                                           |

- **Bước tiếp theo** = ô đầu tiên chưa tick (luôn là một lệnh hoặc một mốc được theo dõi).
- **Hình dạng ô từ tính năng 17** (tính năng 1 đến 16 giữ nguyên dạng cũ làm lịch sử):

  ```markdown
  - [ ] Design it (spec): `/architect <feature>`
  - [ ] Build backend: `/develop <feature> backend`
    - [ ] <mốc backend> (AC-...)
  - [ ] Verify backend: `/check verify <feature> backend`
  - [ ] Build frontend: `/develop <feature> frontend`
    - [ ] <mốc frontend> (AC-...)
  - [ ] Verify it: `/check verify <feature>`
  - [ ] Test it: `/test <feature>`
  ```

  `Verify backend` chỉ gọi API và xem database (không mở giao diện); `Verify it` chạy lại toàn bộ đường đi qua giao diện. Tính năng chỉ có backend thì bỏ `Build frontend` và gộp hai lần verify làm một. Commit một lần sau `Verify it`.

- **Git**: một nhánh cho mỗi phân hệ, `feat/<phân hệ>` theo tên package (`feat/canho`, `feat/cutru`); mỗi tính năng nhỏ trong phân hệ là đúng một commit `feat(<phân hệ>): ...`, tạo khi đã xong cả backend lẫn frontend.
- **needs a decision** = chạy `/architect` trước; nếu không có tag này thì đi thẳng `/develop` (hoặc `/audit` cho quy chuẩn & công cụ). Tag mất đi khi spec được chốt.
- **Việc build chi tiết nằm trong `## Build plan` của spec, không nằm ở đây**: scope chỉ giữ phần tóm tắt theo mốc.
- **Status** `planned` → `in-progress` → `done`, cộng thêm `existing` (có trước workflow) và `dropped` (bỏ khỏi phạm vi, giữ lại làm lịch sử).
- **Approach tag** cạnh tiêu đề (ví dụ `· Facade`) ghi đè cách làm mặc định cho riêng tính năng đó; không có tag là dùng mặc định.
- **Workflow tier tag** cạnh tiêu đề (ví dụ `· GA`) đặt mức kỹ lưỡng riêng cho tính năng đó; không có tag là dùng mặc định. Nó quyết định các ô kiểm tra và gợi ý tiếp theo của mỗi skill.
- **Workflow** (dòng đầu file) là mặc định của dự án, những gì chạy sau `/develop`: **Prototype** = không gì cả; **Alpha** = `/check verify`; **Beta** = `/check verify` rồi `/test`; **GA** = thêm `/check review` bằng model khác, rồi `/document`. Tính năng xây trên một quyết định chưa phê duyệt (spec `Assumed`) vẫn bị đánh dấu, nhưng không bao giờ chặn `done`.
- **Pointer line** (`spec <n> · code in <path>`): link spec do `/architect` thêm, đường dẫn code do `/develop` thêm.
