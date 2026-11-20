# 0006. Phân quyền theo vai trò và bố cục app: annotation `@ChoPhep` nhận enum, service phạm vi dùng chung, menu và chặn trang theo một bảng cấu hình

**Date**: 2026-11-20
**Status**: In Progress

## Summary

Spec này chốt một cách phân quyền dùng chung cho cả 81 API và cho giao diện. Ở backend, mỗi endpoint khai báo vai trò được phép bằng annotation `@ChoPhep({BAN_QUAN_LY, KE_TOAN})` nhận enum `VaiTro` (gõ sai thì lỗi lúc biên dịch). Một test bắt buộc endpoint nào cũng phải khai, và sai vai trò thì trả 403 `FORBIDDEN`. Giới hạn theo dữ liệu (cư dân chỉ thấy căn hộ của mình) đi qua một service phạm vi công khai mà mọi phân hệ gọi; dữ liệu ngoài phạm vi trả 404 để không lộ là nó có tồn tại. Ở giao diện, một bảng cấu hình duy nhất vừa sinh menu chia nhóm theo vai trò, vừa chặn trang không đủ quyền ngay trong khung app. Lát này làm thật `GET /api/v1/buildings` và trang Tòa nhà để chứng minh đường chặn chạy xuyên DB, API và giao diện, và thay trang chủ bằng trang Tổng quan có lối tắt theo vai trò.

## Requirements

**User stories**:
- Là nhân viên hoặc cư dân, tôi muốn menu chỉ có những mục của vai trò mình, để không lạc vào chức năng không dành cho mình.
- Là người có nhiều vai trò (ví dụ vừa là thành viên ban quản trị vừa là cư dân), tôi muốn dùng được mọi chức năng của mình mà không phải đổi vai trò.
- Là ban quản lý, tôi muốn mọi API từ chối người không đủ quyền ngay cả khi họ gọi thẳng, không chỉ dựa vào việc giấu menu.
- Là người làm các phân hệ UC01 đến UC12, tôi muốn khai quyền một dòng trên mỗi endpoint, lấy phạm vi căn hộ và tòa nhà của người đang gọi bằng một lời gọi, và thêm mục menu bằng một dòng cấu hình.

**Acceptance criteria**:
- **AC-1**: Mỗi method xử lý request (handler) của controller trong package `vn.edu.hust.chungcu` mang đúng một trong ba annotation ở mức method: `@ChoPhep(VaiTro...)` với mảng khác rỗng, `@DaDangNhap`, hoặc `@CongKhai`. Không handler nào dùng `@PreAuthorize`, `@PostAuthorize`, `@Secured` hay `@RolesAllowed`. Test `PhanQuyenCoverageTest` duyệt mọi handler trong `RequestMappingHandlerMapping` và báo đỏ khi một handler thiếu annotation, mang hai annotation, có `@ChoPhep` rỗng, hoặc dùng annotation bị cấm; thông báo lỗi nêu tên lớp và method. Test chỉ xét handler có lớp (`HandlerMethod.getBeanType()`) nằm trong `vn.edu.hust.chungcu`, nên controller của springdoc không bị tính. Cũng test đó kiểm hai chiều với danh sách `PublicPaths.CONG_KHAI` (xem *Backend: kiểm quyền theo annotation*): tập cặp (method, mẫu đường dẫn) của mọi handler `@CongKhai`, lấy từ `RequestMappingInfo`, bằng đúng tập cặp trong `CONG_KHAI` sau khi bỏ hai mẫu `/actuator/health` và `/actuator/health/**` (không có handler của dự án). Cặp có method rỗng nghĩa là mọi method (như `/error`). Các handler hiện có được gắn: `login`, `refresh-token`, `logout` và `JsonErrorController.error` (mapping `/error`, mọi method) là `@CongKhai`, `profile` là `@DaDangNhap`.
- **AC-2**: Request có access token hợp lệ tới một endpoint `@ChoPhep` mà claim `vaiTro` của token không chứa vai trò nào trong danh sách thì trả 403, `errorCode` `FORBIDDEN`, `message` "Bạn không có quyền thực hiện thao tác này", theo vỏ bọc lỗi chung có `requestId`. Việc kiểm chạy trước khi Spring MVC đọc tham số và body, nên người sai vai trò luôn nhận 403 kể cả khi tham số hay body sai (không bao giờ 400 trước 403). Thân method controller không chạy (không có truy vấn DB nào, kể cả truy vấn phạm vi). Token chứa ít nhất một vai trò trong danh sách thì endpoint chạy bình thường. Vai trò chỉ lấy từ authority `ROLE_<mã>` của token, không đọc DB. Thiếu token, token hỏng hay hết hạn vẫn trả 401 `UNAUTHENTICATED` như spec 0004 AC-7, không bao giờ 403.
- **AC-3**: Quyền của người nhiều vai trò là hợp của các vai trò. Tài khoản seed `0900000006` (`BAN_QUAN_TRI` và `CU_DAN`) gọi được `GET /api/v1/buildings` và gọi được một endpoint chỉ dành cho `CU_DAN` (controller chỉ có trong test). Tài khoản không có vai trò nào (`vaiTro` rỗng) gọi được mọi endpoint `@DaDangNhap` và nhận 403 `FORBIDDEN` ở mọi endpoint `@ChoPhep`.
- **AC-4**: Mỗi lần từ chối theo AC-2 ghi một dòng log mức WARN gồm `maNguoiDung` (claim `sub`), vai trò trong token, HTTP method, đường dẫn (không kèm query string), các vai trò được phép và `requestId` (qua MDC). Log không chứa token, cookie, body hay query string.
- **AC-5**: `docs/api/openapi.json` sinh lại có, cho mỗi operation `@ChoPhep`, dòng "Vai trò được phép: <mã, cách nhau bằng dấu phẩy, theo thứ tự enum>" ở cuối `description` và một response `403` mô tả "Không đủ quyền (FORBIDDEN)"; operation `@DaDangNhap` có dòng "Vai trò được phép: mọi người đã đăng nhập"; operation `@CongKhai` có dòng "Công khai, không cần đăng nhập". Các dòng này do một `OperationCustomizer` của springdoc sinh từ annotation, không viết tay.
- **AC-6**: Service công khai `PhamViTruyCapService` (package `auth`) có `cuaNguoiDangGoi()` trả record `PhamViTruyCap` gồm `maNguoiDung` (claim `sub`), `vaiTro` (tập vai trò từ token), `maToaNha` (mọi mã tòa nếu token có ít nhất một trong năm vai trò nhân viên `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`, `KY_THUAT`, `BAO_VE`, ngược lại rỗng; `QUAN_TRI_VIEN` và `CU_DAN` không cho phạm vi tòa) và `maCanHo` (căn hộ của các hồ sơ cư trú `Hợp lệ` còn hiệu lực hôm nay theo `app.timezone` nếu token có `CU_DAN` và DB còn dòng `cu_dan` của người đó, ngược lại rỗng). Quy tắc tính `maToaNha` và `maCanHo` là đúng một đoạn code dùng chung với `phamVi` của trang hồ sơ (spec 0004 AC-8), chỉ khác nguồn vai trò; vì vậy hồ sơ của người chỉ có `QUAN_TRI_VIEN` từ nay có `phamVi.maToaNha` rỗng (lệch có chủ ý so với spec 0004 AC-8). Gọi nhiều lần trong cùng một request thì chỉ truy vấn DB một lần. Record có `coVaiTro(VaiTro)`, `yeuCauToaNha(String ma)` và `yeuCauCanHo(String ma)`; hai hàm `yeuCau...` ném `ApiException` 404 `NOT_FOUND` với `message` "Không tìm thấy dữ liệu yêu cầu" khi mã không nằm trong phạm vi, giống hệt response khi mã không tồn tại. Gọi `cuaNguoiDangGoi()` ngoài một request đã xác thực thì ném `IllegalStateException`.
- **AC-7**: `GET /api/v1/buildings` (API-UC01-01) là `@ChoPhep({BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN})`, nhận `page` (mặc định 1), `size` (mặc định 20, tối đa 100), `sort` (mặc định `tenToaNha,asc`, chỉ nhận `maToaNha`, `tenToaNha`, `soTang` với `asc` hoặc `desc`), trả 200 `message` "Lấy danh mục cấu trúc tòa nhà thành công", `data` dạng `PageResponse` với mỗi phần tử `{ maToaNha, tenToaNha, soTang, diaChiKhuVuc, soCanHo }`. `soCanHo` là số dòng `can_ho` thuộc các `tang` của tòa đó, tòa chưa có căn hộ thì 0. Chỉ trả tòa có mã nằm trong `PhamViTruyCap.maToaNha`; phạm vi rỗng thì trả trang rỗng (`totalItems` 0, `totalPages` 0) mà không truy vấn bảng. `page` vượt trang cuối thì trả 200 với `items` rỗng và `totalItems`, `totalPages` đúng. Mọi kiểu sắp xếp có thêm `ma_toa_nha ASC` làm tiêu chí phụ để phân trang ổn định; `tenToaNha` so theo collation `vi-VN-x-icu` của cột.
- **AC-8**: `CU_DAN`, `KY_THUAT`, `BAO_VE`, `QUAN_TRI_VIEN` và tài khoản không vai trò gọi `GET /api/v1/buildings` đều nhận 403 `FORBIDDEN`. `QUAN_TRI_VIEN` chỉ được gọi endpoint mà file đặc tả ghi Admin; ở lát này đó là các endpoint `@DaDangNhap` và `@CongKhai`.
- **AC-9**: Helper phân trang dùng chung trong `common` nhận `page`, `size`, `sort` dạng chuỗi (controller khai `@RequestParam(required = false) String`) và tự đọc theo spec 0001. Thiếu tham số hoặc giá trị rỗng thì dùng mặc định. `page` không phải số nguyên hoặc < 1, `size` không phải số nguyên hoặc < 1 hoặc > 100, `sort` không khớp đúng `^[A-Za-z]+,(asc|desc)$` (phân biệt hoa thường, không bỏ khoảng trắng; tham số lặp lại như `sort=a&sort=b` được Spring nối thành `a,b` nên cũng sai dạng), hoặc trường sort ngoài danh sách trắng của endpoint đều trả 400 `VALIDATION_FAILED`. `errors` có một mục cho mỗi tham số sai, theo thứ tự `page`, `size`, `sort` (`field` là tên tham số). Không bao giờ trả `MALFORMED_REQUEST` hay 500 vì ba tham số này, không tự sửa giá trị.
- **AC-10**: Menu của khung app sinh từ một bảng cấu hình duy nhất `frontend/src/lib/phan-quyen/muc-chuc-nang.ts` (xem *Ma trận chức năng*) và `vaiTro` của query `["ho-so"]`. Nhóm đầu không có nhãn, gồm "Tổng quan" (`/`) và "Hồ sơ" (`/ho-so`) cho mọi người đã đăng nhập. Sau đó mỗi vai trò người dùng có một nhóm, theo thứ tự của mảng `THU_TU_VAI_TRO` (bằng thứ tự cố định của spec 0004; mã vai trò lạ thì bỏ qua), nhãn là `NHAN_VAI_TRO` của vai trò đó, chứa các mục `sanSang: true` mà vai trò ấy được phép và chưa xuất hiện ở nhóm đứng trước; nhóm không còn mục nào thì ẩn. Ở lát này: `0900000002` (`BAN_QUAN_LY`) thấy nhóm "Ban quản lý" có "Tòa nhà"; `0900000001` (`QUAN_TRI_VIEN`) chỉ thấy nhóm đầu; `0900000006` thấy nhóm "Ban quản trị" có "Tòa nhà" và không có nhóm "Cư dân"; `0900000007` (`CU_DAN`) chỉ thấy nhóm đầu. Không href nào xuất hiện hai lần trong menu. Mục chỉ hiện ở một nhóm nhưng quyền vào trang vẫn tính theo mọi vai trò của người dùng (AC-11).
- **AC-11**: Layout `(app)` tự chặn trang: khi đường dẫn hiện tại khớp một mục của ma trận (khớp theo đoạn trên `pathname` của `usePathname()`, vốn không có query string, chọn mục có `href` dài nhất như `activeHref`; `/toa-nha/abc` khớp `/toa-nha`, còn `/ho-so-cu-tru` không khớp `/ho-so`) mà `vaiTro` của hồ sơ không giao với vai trò của mục, vùng nội dung hiện component `KhongCoQuyen` thay cho trang: `ErrorState` mức `h1` có icon `ShieldX`, tiêu đề "Không có quyền truy cập", mô tả "Bạn không có quyền xem trang này. Nếu cần quyền, vui lòng liên hệ ban quản lý.", nút "Về trang chủ" tới `/`. Component trang không được render nên không gọi API nào của trang; URL, thanh trên và menu giữ nguyên. Đường dẫn không khớp mục nào (`/`, `/ho-so`) không bị chặn. Trang mới thêm vào ma trận được chặn mà không phải sửa code trang.
- **AC-12**: Một query trả 403 `FORBIDDEN` (ví dụ vai trò trong token chưa kịp cập nhật so với DB) thì trạng thái lỗi của `DataTable` và của helper mới `QueryErrorState` (chỗ trang hiện lỗi của một query) hiện `KhongCoQuyen` ở mức tiêu đề của chỗ đó, không có nút "Thử lại" và không có mã tra cứu; query không được thử lại; không chuyển trang. `ErrorState` giữ nguyên hành vi, nên chỗ nào truyền tiêu đề và mô tả riêng vẫn hiện đúng nội dung của mình. Mutation trả `FORBIDDEN` hiện câu của mã này qua cơ chế lỗi form hoặc toast hiện có. Xử lý 401 và `AUTH_ACCOUNT_LOCKED` giữ nguyên như spec 0004 và 0005.
- **AC-13**: Khi query `["ho-so"]` đang tải lần đầu, menu hiện khung xương (skeleton) thay cho các nhóm, và trang nằm trong ma trận hiện skeleton thay cho nội dung (chưa render trang). Khi query lỗi khác 401 và 403 khóa tài khoản, menu chỉ có nhóm đầu, và trang nằm trong ma trận hiện `ErrorState` "Không tải được quyền truy cập" có nút "Thử lại" gọi lại `["ho-so"]`. Không bao giờ render trang trong ma trận khi chưa biết vai trò. Trang `/` và `/ho-so` vẫn hiện khung của chúng trong cả hai trạng thái.
- **AC-14**: Trang `/` thay cho chuyển hướng tới `/ho-so`, và `TRANG_MAC_DINH` sau đăng nhập đổi thành `/`. Trang có `PageHeader` "Xin chào, <hoTen>" (skeleton khi đang tải), rồi mỗi nhóm vai trò của menu (đúng các nhóm và mục như AC-10, trừ nhóm đầu) là một `SectionHeader` có nhãn vai trò và một lưới thẻ; mỗi thẻ là một link duy nhất tới `href` của mục, có icon, tên mục và câu `moTa` của mục. Không có vai trò nào thì hiện `EmptyState` "Chưa được gán vai trò" với mô tả "Vui lòng liên hệ ban quản lý để được gán vai trò." Có vai trò nhưng không nhóm nào còn mục thì hiện `EmptyState` "Các chức năng cho vai trò của bạn đang được xây dựng" có link "Xem hồ sơ" tới `/ho-so`. Lỗi query hiện `ErrorState` có "Thử lại".
- **AC-15**: Trang `/toa-nha` (mục "Tòa nhà", icon `Building2`, vai trò `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`) có `PageHeader` "Tòa nhà" và một `DataTable` caption ẩn "Danh sách tòa nhà" theo mẫu *Bảng danh sách* của `frontend/design.md`: cột "Mã tòa nhà" (sort `maToaNha`), "Tên tòa nhà" (sort `tenToaNha`), "Số tầng" (sort `soTang`, canh phải), "Địa chỉ khu vực" (mô tả, cắt 2 dòng), "Số căn hộ" (canh phải, không sort). `page`, `size`, `sort` nằm trên URL; không có `sort` trên URL thì gửi không kèm `sort` (backend dùng mặc định). Rỗng hiện "Chưa có tòa nhà nào". Đang tải, lỗi có "Thử lại", và 403 theo AC-12 đều do `DataTable` lo.
- **AC-16**: Trang `/`, `/toa-nha` và trạng thái `KhongCoQuyen` dùng được hoàn toàn bằng bàn phím, focus nhìn thấy được, nhóm menu có nhãn đọc được bằng trình đọc màn hình, mỗi thẻ lối tắt có tên truy cập được (tên mà trình đọc màn hình đọc lên) là tên mục; test axe không báo lỗi ở cả ba.

## Decision

**Chosen option**: Option 1: annotation `@ChoPhep(VaiTro...)` trên method controller, kiểm bằng một `HandlerInterceptor` của Spring MVC trước khi đọc tham số; test bắt mọi handler phải khai; phạm vi dữ liệu qua `PhamViTruyCapService` công khai của `auth`; frontend dùng một bảng cấu hình chức năng cho cả menu, trang Tổng quan và chặn trang ở layout.

Backend thêm package `auth/phanquyen` (ba annotation, bộ kiểm quyền, cấu hình đăng ký interceptor, `OperationCustomizer`), `PhamViTruyCapService`, helper phân trang trong `common`, và API đầu tiên của `canho` (`GET /api/v1/buildings`). Không đổi schema. Frontend thêm `lib/phan-quyen/`, khung app phía client có menu theo vai trò, `KhongCoQuyen`, trang `/` và `/toa-nha`.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.agents/skills/java-springboot/`) · `next-dev-loop` (`vercel/next.js`, `.agents/skills/next-dev-loop/`) · `ui-styling` (`.claude/skills/ui-styling/`)

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).

## Feature design

### Ánh xạ cột RBAC của file đặc tả

Mỗi phân hệ đọc cột "Phân quyền (RBAC)" của `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx` theo bảng này khi khai annotation.

| Ghi trong file đặc tả | Khai trong code |
|---|---|
| BQL | `BAN_QUAN_LY` |
| BQT | `BAN_QUAN_TRI` |
| Kế toán | `KE_TOAN` |
| Kỹ thuật | `KY_THUAT` |
| Cư dân | `CU_DAN`, và service phải giới hạn theo `PhamViTruyCap.maCanHo` (dữ liệu ngoài phạm vi trả 404) |
| Admin | `QUAN_TRI_VIEN`; Admin không có quyền ngầm nào khác |
| Bảo vệ sảnh | `BAO_VE` |
| BQL duyệt (API-UC07-05) | `@ChoPhep({BAN_QUAN_LY, KY_THUAT})`; luật ai được tạm hoãn, ai phải duyệt nằm trong service của UC07 |
| Hệ thống (API-UC09-04) | Không phải vai trò HTTP. Endpoint khai theo vai trò người còn lại (`CU_DAN`); job nội bộ gọi thẳng service, không đi qua HTTP |
| Cổng thanh toán (API-UC05-03) | `@CongKhai` cộng kiểm chữ ký webhook, chốt ở spec của UC05; thêm cặp vào `PublicPaths.CONG_KHAI` |
| Public | `@CongKhai`, và cặp (method, đường dẫn) phải có trong `PublicPaths.CONG_KHAI` |
| Authenticated | `@DaDangNhap` |

Mọi nhân viên (năm vai trò nhân viên ở AC-6) hiện phụ trách mọi tòa, nên `maToaNha` là toàn bộ tòa nhà; khi có bảng gán nhân viên vào tòa (mục *Deferred* của scope), chỉ cần đổi đoạn code dùng chung tính `maToaNha`, không phải sửa API nào.

### Backend: kiểm quyền theo annotation

- **Package** `vn.edu.hust.chungcu.auth.phanquyen`: `@ChoPhep` (`@Target(METHOD)`, `@Retention(RUNTIME)`, `VaiTro[] value()`), `@DaDangNhap`, `@CongKhai` (cùng target và retention, không thuộc tính), `ChoPhepInterceptor`, `PhanQuyenConfig`, `PhanQuyenOperationCustomizer`. Annotation chỉ đặt ở mức method; annotation ở mức lớp không có tác dụng và test AC-1 không tính nó. Phân hệ khác chỉ import ba annotation, `VaiTro` và `PhamViTruyCapService`.
- **`ChoPhepInterceptor`** cài `HandlerInterceptor`. Trong `preHandle`: handler không phải `HandlerMethod` thì cho qua; `handlerMethod.getMethodAnnotation(ChoPhep.class)` là `null` thì cho qua (`@DaDangNhap`, `@CongKhai`; đăng nhập đã do `anyRequest().authenticated()` lo, và test AC-1 bảo đảm không có handler nào thiếu khai). Có `@ChoPhep` thì lấy `Authentication` từ `SecurityContextHolder` và cho qua khi authority có ít nhất một `ROLE_<mã>` trong danh sách. Ngược lại ghi log WARN rồi ném `AccessDeniedException("FORBIDDEN")`. Spring MVC đưa exception từ `preHandle` qua các `HandlerExceptionResolver` với đúng `HandlerMethod`, nên `GlobalExceptionHandler.handleAccessDenied` hiện có trả 403 `FORBIDDEN` đúng vỏ bọc và `requestId`. `preHandle` chạy trước khi đọc tham số và body, nên AC-2 đứng trước mọi lỗi 400. Không dùng method security (không `@EnableMethodSecurity`), không proxy controller, không sửa `SecurityConfig`.
- **Log WARN** (AC-4): một dòng `Từ chối quyền maNguoiDung={} vaiTro={} method={} path={} canCo={}`. `path` là `request.getRequestURI()` (không có query string). `vaiTro` và `canCo` in theo thứ tự enum, cách nhau bằng dấu phẩy. `requestId` có sẵn trong MDC do `RequestIdFilter`.
- **`PhanQuyenConfig`**: `@Configuration(proxyBeanMethods = false)` cài `WebMvcConfigurer`, `addInterceptors` đăng ký `ChoPhepInterceptor` cho `/api/**`.
- **`PublicPaths`**: thêm danh sách `CONG_KHAI` gồm các record `DuongCongKhai(HttpMethod methodHoacNull, String mau)`, và dựng `ALWAYS` từ danh sách này (thay cho việc liệt kê matcher bằng tay), để test AC-1 đọc được từng cặp. Hành vi của `ALWAYS`, `API_DOCS`, `NO_TOKEN` không đổi.
- **Thứ tự kiểm**: `CsrfHeaderFilter` (403 `CSRF_REJECTED`) rồi token (401) rồi vai trò (403 `FORBIDDEN`, trong `preHandle`) rồi đọc và kiểm tham số, body (400) rồi phạm vi dữ liệu (404) rồi nghiệp vụ.
- **Giới hạn**: interceptor chỉ chặn lời gọi HTTP. Service gọi từ job hay từ phân hệ khác không bị kiểm vai trò; luật nào phải đúng cả ngoài HTTP thì viết trong service.
- **`PhanQuyenOperationCustomizer`**: bean `OperationCustomizer` đọc annotation của `HandlerMethod` và thêm dòng theo AC-5: `description` rỗng thì dòng đó là cả mô tả, ngược lại nối sau mô tả bằng một dòng trống (`\n\n`). Chỉ handler `@ChoPhep` được thêm response `403` (nếu chưa có), dùng schema `ErrorResponse`; handler `@DaDangNhap` và `@CongKhai` không có 403 do customizer thêm. Sinh lại `openapi.json` trong `mvnw verify` như hiện nay.

### Backend: phạm vi dữ liệu

- **`PhamViTruyCap`** (record trong `auth/dto`): `maNguoiDung: String`, `vaiTro: Set<VaiTro>` (EnumSet), `maToaNha: List<String>` (tăng dần), `maCanHo: List<String>` (tăng dần); `coVaiTro(VaiTro)`, `yeuCauToaNha(String)`, `yeuCauCanHo(String)` theo AC-6.
- **`PhamViTinhToan`** (lớp mới trong `auth/service`, phần dùng chung): `tinh(Set<VaiTro> vaiTro, String maCuDanHoacNull)` trả cặp `maToaNha`, `maCanHo` theo quy tắc AC-6 bằng `ToaNhaService.danhSachMaToaNha()` và `CuTruService.maCanHoDangCuTru(maCuDan, LocalDate.now(clock theo app.timezone))`. `HoSoService.phamVi` chuyển sang gọi lớp này với vai trò đọc từ DB; `PhamViTruyCapService` gọi nó với vai trò từ token.
- **`PhamViTruyCapService.cuaNguoiDangGoi()`**: lấy `JwtAuthenticationToken` từ `SecurityContextHolder` (không có thì `IllegalStateException`), đọc vai trò từ authority, nếu có `CU_DAN` thì lấy `maCuDan` qua `VaiTroQuery` (không còn dòng `cu_dan` thì `maCanHo` rỗng), gọi `PhamViTinhToan`. Kết quả nhớ trong một attribute của request hiện tại (`RequestContextHolder.getRequestAttributes()`, scope request), nên lời gọi thứ hai trong cùng request không truy vấn DB. Không có request attributes (job, luồng chạy nền) hoặc `Authentication` không phải `JwtAuthenticationToken` thì ném `IllegalStateException`: phạm vi chỉ có nghĩa với một người đang gọi qua HTTP.

### Backend: `GET /api/v1/buildings`

- **Helper phân trang** trong `common/api`: record `TrangYeuCau(int page, int size, String sortField, Sort.Direction dir)` dựng bằng `TrangYeuCau.tu(String page, String size, String sort, Set<String> choPhepSort, String sortMacDinh)`, gom mọi tham số sai rồi ném một `ApiException` 400 `VALIDATION_FAILED` có danh sách lỗi theo trường (`field`, `message`) theo AC-9, cùng dạng `errors` mà `GlobalExceptionHandler` đang dùng cho lỗi validation. Câu lỗi: "Số trang phải từ 1 trở lên", "Số dòng mỗi trang phải từ 1 đến 100", "Không sắp xếp được theo trường này" (dùng cho cả sai dạng lẫn ngoài danh sách trắng). Có `offset()` = (`page` − 1) × `size`. `PageResponse` thêm `of(List<T> items, TrangYeuCau trang, long totalItems)` (tính `totalPages` = trần của `totalItems` / `size`) cho truy vấn không đi qua Spring Data.
- **`canho`**: `ToaNhaController` (`canho/controller`), `ToaNhaService.danhSach(List<String> maToaNha, TrangYeuCau trang)` trả `PageResponse<ToaNhaResponse>` (`@Transactional(readOnly = true)`), `ToaNhaQuery` (`canho/repository`, `JdbcClient`, theo kiểu `VaiTroQuery`). Controller gọi `PhamViTruyCapService` rồi truyền `maToaNha` xuống service, để service không phụ thuộc vào cách lấy người đang gọi.
- **SQL**: một câu đếm `SELECT count(*) FROM toa_nha WHERE ma_toa_nha IN (:ma)` và một câu dữ liệu `SELECT t.ma_toa_nha, t.ten_toa_nha, t.so_tang, t.dia_chi_khu_vuc, count(c.ma_can_ho) AS so_can_ho FROM toa_nha t LEFT JOIN tang g ON g.ma_toa_nha = t.ma_toa_nha LEFT JOIN can_ho c ON c.ma_tang = g.ma_tang WHERE t.ma_toa_nha IN (:ma) GROUP BY t.ma_toa_nha ORDER BY <cột> <chiều>, t.ma_toa_nha ASC LIMIT :size OFFSET :offset`. `<cột>` lấy từ map cố định `maToaNha → t.ma_toa_nha`, `tenToaNha → t.ten_toa_nha`, `soTang → t.so_tang`; chuỗi của người dùng không bao giờ được nối vào SQL. Danh sách `maToaNha` rỗng thì không chạy câu nào.

### Frontend: ma trận chức năng, menu, chặn trang

- **`frontend/src/lib/phan-quyen/muc-chuc-nang.ts`**: kiểu union `MucChucNang = MucSanSang | MucChuaSanSang`, với `MucChuaSanSang = { href: string; label: string; vaiTro: readonly VaiTro[]; sanSang: false }` và `MucSanSang = { href; label; vaiTro; sanSang: true; moTa: string; icon: LucideIcon }`. Như vậy chỉ mục đã bật mới phải có `moTa` và `icon`; phân hệ thêm hai trường này khi bật cờ. Hằng `MUC_CHUC_NANG` theo bảng *Ma trận chức năng*. Hai mục chung "Tổng quan" (icon `LayoutDashboard`) và "Hồ sơ" (icon `UserRound`) nằm riêng (`MUC_CHUNG`), không thuộc ma trận chặn.
- **`frontend/src/lib/vai-tro.ts`**: thêm `THU_TU_VAI_TRO: readonly VaiTro[]` theo thứ tự của spec 0004; đây là nguồn thứ tự duy nhất cho nhóm menu.
- **`frontend/src/lib/phan-quyen/menu.ts`**: hàm thuần `nhomMenu(vaiTro: VaiTro[]): NavGroup[]` (theo AC-10, duyệt `THU_TU_VAI_TRO`), `mucCuaDuongDan(pathname): MucChucNang | undefined` (dùng lại `activeHref` của `components/layout/nav.ts`), `duocVao(muc, vaiTro): boolean`. Trang Tổng quan dùng chung `nhomMenu`.
- **Khung app**: `src/app/(app)/layout.tsx` giữ là server component và render `KhungApp` (client component mới trong `(app)/`) thay cho `AppShell` trực tiếp. `KhungApp` đọc `useQuery(["ho-so"])` (cùng key và `queryFn` với trang hồ sơ, tách ra `useHoSo()` trong `src/lib/auth/`), dựng `nav`, truyền `AppShell` và bọc `children` bằng `ChanTrang`. `AppShell` thêm prop tùy chọn `navPending?: boolean` để hiện 4 dòng `Skeleton` thay cho `AppNav` (cả thanh bên và ngăn kéo di động).
- **`ChanTrang`** (client component trong `(app)/`): `usePathname()`, tìm mục bằng `mucCuaDuongDan`; không có mục thì render `children`; có mục thì theo AC-11 và AC-13.
- **`KhongCoQuyen`** (`src/components/feedback/khong-co-quyen.tsx`): render `ErrorState` với tiêu đề, mô tả và icon của AC-11, không truyền `error` (nên không có mã tra cứu), nhận `headingLevel` (mặc định `h1`) và `hideHomeAction`. `ErrorState` không đổi.
- **`QueryErrorState`** (`src/components/feedback/query-error-state.tsx`): nhận `error`, `title`, `onRetry`, `headingLevel`. Khi `error` là `ApiError` có `errorCode === "FORBIDDEN"` thì render `KhongCoQuyen` ở cùng `headingLevel` với `hideHomeAction` (khi không phải cả trang); ngược lại render `ErrorState` có nút "Thử lại". Nhánh lỗi của `DataTable` (khi chưa có dữ liệu) dùng `QueryErrorState`; khi đã có dữ liệu cũ mà lỗi `FORBIDDEN` thì `Alert` hiện câu của mã này, không có "Thử lại". `shouldRetryQuery` đã không thử lại lỗi 4xx nên không phải sửa.
- **Trang**: `src/app/(app)/page.tsx` (Tổng quan, thay `src/app/page.tsx`), `src/app/(app)/toa-nha/page.tsx` cộng `toa-nha-view.tsx`. `src/lib/auth/safe-next.ts` đổi `TRANG_MAC_DINH` thành `/`. Kiểu `ToaNhaResponse` lấy từ `pnpm gen:api`.

### Ma trận chức năng

Bảng khởi đầu, suy từ các API đọc danh sách trong cột RBAC. Mỗi phân hệ sở hữu các dòng của mình: khi làm phân hệ, spec của nó có thể đổi `href`, nhãn hay vai trò của dòng đó, rồi bật `sanSang`. Ở lát này chỉ "Tòa nhà" có `sanSang: true`.

| `href` | Nhãn | Vai trò | Nguồn | Sẵn sàng |
|---|---|---|---|---|
| `/toa-nha` | Tòa nhà | `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN` | API-UC01-01 | có |
| `/can-ho` | Căn hộ | `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN` | API-UC01-05 | không |
| `/khu-vuc-chung` | Khu vực chung | `BAN_QUAN_LY`, `KY_THUAT` | API-UC01-10 | không |
| `/ho-so-cu-tru` | Hồ sơ cư trú | `BAN_QUAN_LY` | API-UC02-02, 03 | không |
| `/can-ho-cua-toi` | Căn hộ của tôi | `CU_DAN` | API-UC01-06, UC02-01, UC02-05 | không |
| `/chi-so` | Chỉ số điện nước | `BAN_QUAN_LY`, `KE_TOAN`, `KY_THUAT` | API-UC03-01, 06 | không |
| `/bieu-phi` | Biểu phí | `BAN_QUAN_LY`, `KE_TOAN` | API-UC04-01 | không |
| `/thong-bao-phi` | Thông báo phí | `BAN_QUAN_TRI`, `KE_TOAN` | API-UC04-04 | không |
| `/doi-soat` | Đối soát | `KE_TOAN` | API-UC04-08, 09 | không |
| `/phi-cua-toi` | Phí của tôi | `CU_DAN` | API-UC05-01 | không |
| `/yeu-cau-ho-tro` | Yêu cầu hỗ trợ | `CU_DAN` | API-UC06-01, 03 | không |
| `/dieu-phoi` | Điều phối sự cố | `BAN_QUAN_LY` | API-UC07-01 | không |
| `/cong-viec` | Công việc của tôi | `KY_THUAT` | API-UC07-04, 06, UC08-04 | không |
| `/bao-tri` | Bảo trì | `BAN_QUAN_LY` | API-UC08-03, 07 | không |
| `/thiet-bi` | Thiết bị kỹ thuật | `BAN_QUAN_LY`, `KY_THUAT` | API-UC08-01 | không |
| `/tien-ich` | Tiện ích | `BAN_QUAN_LY`, `CU_DAN` | API-UC09-01 | không |
| `/check-in` | Check in tiện ích | `BAN_QUAN_LY`, `BAO_VE` | API-UC09-06 | không |
| `/thong-bao` | Thông báo | `BAN_QUAN_LY`, `CU_DAN` | API-UC10-02 | không |
| `/khao-sat` | Khảo sát | `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `CU_DAN` | API-UC11-02, 03, 05 | không |
| `/bao-cao` | Báo cáo | `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN` | API-UC12-01 | không |
| `/nhat-ky` | Nhật ký hệ thống | `QUAN_TRI_VIEN`, `BAN_QUAN_LY`, `BAN_QUAN_TRI` | API-AUTH-05 | không |
| `/tai-khoan` | Tài khoản | `QUAN_TRI_VIEN`, `BAN_QUAN_LY` | tính năng 11 | không |

"Tòa nhà" có `icon` `Building2` và `moTa` "Danh mục tòa nhà, số tầng và số căn hộ". Các dòng chưa sẵn sàng không có `icon` hay `moTa` (spec của phân hệ chọn khi bật cờ). Mục chưa sẵn sàng vẫn chặn được trang nếu ai đó tạo trang mà quên bật cờ, nhưng không hiện trong menu hay Tổng quan.

**Data model sketch**: Không đổi schema, không có migration. Lát này chỉ đọc:

| Bảng | Dùng cho | Quan hệ |
|---|---|---|
| `toa_nha` | danh sách tòa, `maToaNha` của phạm vi | 1:N với `tang` |
| `tang` | nối tòa với căn hộ khi đếm | N:1 `toa_nha`, 1:N `can_ho` |
| `can_ho` | `soCanHo` | N:1 `tang` |
| 7 bảng vai trò, `ho_so_cu_tru` | `maCuDan`, `maCanHo` của phạm vi (như spec 0004) | 1:0..1 với `nguoi_dung_he_thong` |

Không có bảng quyền trong DB: quyền là code (annotation và ma trận), đổi quyền là một commit có review.

**State transitions**: không có.

**API surface**:

| Endpoint | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `/api/v1/buildings` | GET | `page: int` (opt, mặc định 1), `size: int` (opt, mặc định 20, tối đa 100), `sort: string` (opt, `maToaNha` \| `tenToaNha` \| `soTang` cộng `,asc` \| `,desc`; mặc định `tenToaNha,asc`) | 200 `data { items: [{ maToaNha, tenToaNha, soTang, diaChiKhuVuc, soCanHo }], page, size, totalItems, totalPages }`, `message` "Lấy danh mục cấu trúc tòa nhà thành công" | `@ChoPhep({BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN})` | 400 `VALIDATION_FAILED`, 401 `UNAUTHENTICATED`, 403 `FORBIDDEN` |
| `/api/v1/auth/login`, `/refresh-token`, `/logout` | POST | như spec 0004, 0005 | như cũ | `@CongKhai` | như cũ |
| `/api/v1/auth/profile` | GET | như spec 0004 | như cũ | `@DaDangNhap` | như cũ |

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| Kiểm quyền | vai trò người gọi | authority `ROLE_<mã>` từ claim `vaiTro` của access token (spec 0004 AC-7) |
| Kiểm quyền | vai trò được phép | `@ChoPhep.value()` của method |
| Log từ chối | `maNguoiDung`, method, đường dẫn, `requestId` | claim `sub`; `HttpServletRequest` của `preHandle` (`getMethod()`, `getRequestURI()`); MDC `requestId` |
| OpenAPI | dòng vai trò | annotation trên `HandlerMethod` |
| Phạm vi | `maNguoiDung`, `vaiTro` | claim `sub`, authority của token |
| Phạm vi | `maToaNha` | toàn bộ `toa_nha.ma_toa_nha` khi có một trong năm vai trò nhân viên (giả định của spec 0004, thu hẹp ở AC-6) |
| Phạm vi | `maCuDan` | `cu_dan.ma_cu_dan` qua `VaiTroQuery` |
| Phạm vi | `maCanHo` | `CuTruService.maCanHoDangCuTru` với ngày hôm nay theo `app.timezone` từ `Clock` |
| Danh sách tòa | `maToaNha`, `tenToaNha`, `soTang`, `diaChiKhuVuc` | cột cùng tên của `toa_nha` |
| Danh sách tòa | `soCanHo` | `count(can_ho.ma_can_ho)` qua `tang`, tính lúc đọc |
| Danh sách tòa | `page`, `size`, `sort` | query param, mặc định `1`, `20`, `tenToaNha,asc` |
| Danh sách tòa | `totalItems`, `totalPages` | câu đếm; `totalPages` = trần(`totalItems` / `size`) |
| Menu, Tổng quan, chặn trang | vai trò người dùng | `vaiTro` của `GET /api/v1/auth/profile` (query `["ho-so"]`, đọc từ DB) |
| Menu, Tổng quan, chặn trang | mục, nhãn, icon, mô tả, vai trò của mục | `MUC_CHUC_NANG` |
| Menu, Tổng quan | nhãn nhóm, thứ tự nhóm | `NHAN_VAI_TRO`, `THU_TU_VAI_TRO` trong `frontend/src/lib/vai-tro.ts` |
| Tổng quan | `hoTen` | query `["ho-so"]` |
| Trang Tòa nhà | `page`, `size`, `sort` | URL qua `parseTableParams` |

**Key invariants**:
- Không có handler nào không khai quyền (AC-1); không có cách thứ hai để khai quyền trên handler.
- 401 luôn đứng trước 403: người chưa xác thực không bao giờ biết endpoint cần vai trò gì.
- Dữ liệu ngoài phạm vi và dữ liệu không tồn tại cho cùng một response 404.
- Quy tắc tính `maToaNha`, `maCanHo` chỉ có một chỗ (`PhamViTinhToan`).
- Menu không bao giờ có href trùng; trang trong ma trận không bao giờ render khi chưa biết vai trò.
- Giao diện chỉ là lớp tiện dụng; quyết định cuối cùng luôn là backend.

**Security model**: Xác thực như spec 0004 và 0005. Phân quyền có hai lớp ở backend: vai trò theo annotation trên controller (lớp ngoài, chặn trước khi chạy logic), và phạm vi dữ liệu trong service qua `PhamViTruyCap` (lớp trong, trả 404). Vai trò trong token có thể cũ tối đa 15 phút sau khi đổi trong DB (spec 0004, Consequences); menu đọc DB nên có thể hiện mục mà API vẫn chặn trong khoảng đó, và AC-12 xử lý đúng trường hợp này. Admin không có quyền ngầm. Dữ liệu ở lát này (tên, địa chỉ tòa nhà) không nhạy cảm; không có phạm vi tuân thủ nào mới.

**Critical test scenarios**:
- Happy path: `0900000002` đăng nhập, menu có nhóm "Ban quản lý" với "Tòa nhà", mở `/toa-nha` thấy bảng tòa seed có `soCanHo` đúng, đổi sort và trang, verifies **AC-7**, **AC-10**, **AC-15**.
- Ma trận vai trò: test tích hợp tham số hóa gọi `GET /api/v1/buildings` bằng token của từng vai trò trong 7 vai trò, tài khoản không vai trò và `0900000006`: đúng ba vai trò cộng tài khoản hai vai trò nhận 200, còn lại 403 `FORBIDDEN` và không gọi `ToaNhaQuery` hay `PhamViTruyCapService` (`@MockitoSpyBean`), verifies **AC-2**, **AC-3**, **AC-8**.
- Thứ tự 403 trước 400: cư dân gọi `GET /api/v1/buildings?page=abc&size=0` nhận 403 `FORBIDDEN`, không phải 400, verifies **AC-2**.
- Failure case: `sort=diaChiKhuVuc,asc`, `sort=tenToaNha`, `sort=tenToaNha,ASC`, `sort=a&sort=b`, `size=101`, `page=0`, `page=abc` đều 400 `VALIDATION_FAILED` đúng `field`; `page=0&size=0` có hai mục theo thứ tự `page`, `size`; `page=` rỗng dùng mặc định; trang vượt quá cho `items` rỗng; phạm vi rỗng cho trang rỗng, verifies **AC-7**, **AC-9**.
- Coverage: logic kiểm nằm trong một lớp thuần `PhanQuyenCoverage` nhận danh sách `HandlerMethod` kèm `RequestMappingInfo` và danh sách `CONG_KHAI`, trả danh sách lỗi. Unit test đưa vào các controller cố tình sai (thiếu annotation, hai annotation, `@ChoPhep({})`, `@PreAuthorize`, `@CongKhai` không có trong danh sách, một cặp công khai không có handler), đặt trong package test `...phanquyen.mau` không được component scan, và kiểm từng lỗi được báo. `PhanQuyenCoverageTest` chạy cùng lớp đó trên `RequestMappingHandlerMapping` của app thật (có controller test chỉ dành cho `CU_DAN`, vốn mang `@ChoPhep({CU_DAN})` và chỉ được `@Import` vào test cần nó) và phải xanh, verifies **AC-1**.
- Token khác vai trò DB: ký token chỉ có `CU_DAN` cho tài khoản BQL, mở `/toa-nha`: trang render (DB nói BQL), API trả 403, bảng hiện "Không có quyền truy cập" không có "Thử lại", verifies **AC-12**.
- Auth/permission: `0900000007` gõ `/toa-nha`: hiện `KhongCoQuyen`, không có request `GET /api/v1/buildings` trong Network, menu vẫn đủ, verifies **AC-11**.
- Phạm vi: unit test `PhamViTruyCapService` với fake: hai lần gọi trong một request chỉ một lần truy vấn; `yeuCauCanHo` với mã ngoài phạm vi ném 404 giống mã không tồn tại; gọi ngoài request ném `IllegalStateException`, verifies **AC-6**.

## Build plan

Theo Tracer Bullet: lát đầu chạy thật một đường từ annotation tới bảng trên màn hình cho một vai trò, các lát sau làm dày từng đoạn.

1. [x] **Đường mỏng: BQL thấy Tòa nhà, cư dân bị 403.** `@ChoPhep`, `ChoPhepInterceptor` (chưa log), `PhanQuyenConfig`; `ToaNhaController`, `ToaNhaService.danhSach`, `ToaNhaQuery` với trang mặc định và sort mặc định, chưa lọc phạm vi; sinh lại `openapi.json`, `pnpm gen:api`; `muc-chuc-nang.ts` chỉ có "Tòa nhà", `nhomMenu`, `KhungApp` dựng menu từ `["ho-so"]`; `/toa-nha` với `DataTable` tối thiểu; test tích hợp BQL 200, cư dân 403 `FORBIDDEN`, chưa đăng nhập 401. Satisfies **AC-2**, **AC-7**, **AC-10**, **AC-15**.
2. [x] **Làm dày backend: khai đủ, phạm vi, phân trang, tài liệu.** `@DaDangNhap`, `@CongKhai` gắn lên các handler hiện có; `PublicPaths.CONG_KHAI`; `PhanQuyenCoverage` và `PhanQuyenCoverageTest`; log WARN; `PhanQuyenOperationCustomizer`; helper `TrangYeuCau` và `PageResponse.of`; `/buildings` đủ sort, `soCanHo`, tiêu chí phụ; `PhamViTinhToan` (chuyển `HoSoService` sang dùng, sửa test hồ sơ của Admin cho `maToaNha` rỗng), `PhamViTruyCap`, `PhamViTruyCapService` có nhớ theo request, nối vào `/buildings`; test tham số hóa 7 vai trò; controller test chỉ dành cho `CU_DAN`. Satisfies **AC-1**, **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-7**, **AC-8**, **AC-9**.
3. [x] **Làm dày giao diện: ma trận đủ, chặn trang, 403 và trạng thái.** Ma trận đủ 22 dòng; `THU_TU_VAI_TRO`; luật nhóm và mục trùng; `mucCuaDuongDan`, `duocVao`; `ChanTrang`; `KhongCoQuyen`, `QueryErrorState` và nhánh lỗi của `DataTable`; `navPending` của `AppShell`; trạng thái đang tải và lỗi của quyền; `/toa-nha` đủ cột, sort, phân trang, rỗng; test Vitest cho `nhomMenu` (kể cả vai trò lạ, `/ho-so-cu-tru` không khớp `/ho-so`), `ChanTrang`, `QueryErrorState`. Satisfies **AC-10**, **AC-11**, **AC-12**, **AC-13**, **AC-15**.
4. [x] **Trang Tổng quan và khóa lại.** `/` thành trang Tổng quan, xóa `src/app/page.tsx`, `TRANG_MAC_DINH` là `/` (sửa test `safe-next` và `login-form`); axe và bàn phím cho `/`, `/toa-nha`, `KhongCoQuyen`; `verify.md`; README backend mục "Thêm một endpoint mới" (chọn annotation, gọi phạm vi, thêm dòng ma trận). Satisfies **AC-14**, **AC-16**.

## Consequences

**Positive**:
- Khai quyền là một dòng có kiểu, gõ sai tên vai trò là lỗi biên dịch, quên khai là build đỏ; 75 API nghiệp vụ còn lại không có chỗ nào lọt quyền vì sơ suất.
- Vai trò được kiểm trước khi đọc tham số, nên người không đủ quyền không bao giờ thấy lỗi validation, và không có proxy nào bọc controller.
- Quyền nằm ngay cạnh endpoint và tự hiện trong Swagger, nên người đọc code, người test và người làm giao diện cùng nhìn một nguồn.
- Phạm vi dữ liệu có một service và một quy tắc, nên khi thêm bảng gán nhân viên vào tòa chỉ phải sửa một chỗ.
- Thêm một trang mới chỉ cần một dòng trong ma trận để có menu, lối tắt và chặn trang.

**Negative / tradeoffs**:
- Vai trò có hai nguồn: token (API, cũ tối đa 15 phút) và DB (menu). Trong khoảng lệch, người dùng có thể thấy mục mà bấm vào bị "Không có quyền truy cập", hoặc ngược lại không thấy mục mà API vẫn cho. Chấp nhận vì đổi vai trò hiếm và spec 0004 đã chấp nhận độ trễ này.
- Ma trận ở frontend và annotation ở backend là hai bảng viết tay có thể lệch nhau; không có test tự so hai bên. Lệch thì backend vẫn đúng, giao diện chỉ hiện sai menu.
- Annotation riêng là một quy ước mà người mới phải học thay cho `@PreAuthorize` quen thuộc; luật phức tạp hơn "có một trong các vai trò" (ví dụ "BQL duyệt") phải viết trong service.
- Package `canho` gọi `auth` (lấy phạm vi), còn `auth` gọi `canho` (danh sách mã tòa): hai package phụ thuộc nhau qua service công khai. Không có vòng bean vì `ToaNhaService` không gọi `auth`, nhưng đừng để `ToaNhaService` gọi `PhamViTruyCapService`.
- Phạm vi của cư dân tốn thêm một truy vấn cho mỗi request cần nó.
- Vai trò chỉ được kiểm ở lời gọi HTTP; service gọi từ job hay phân hệ khác không có lớp kiểm này.
- Trang hồ sơ của người chỉ có vai trò Admin đổi từ "mọi tòa" thành "Không có" ở mục tòa nhà (lệch spec 0004 AC-8).

**Neutral**:
- `src/app/page.tsx` chuyển vào route group `(app)`; trang mặc định sau đăng nhập là `/`.
- Lần đầu có helper phân trang và API danh sách thật; mọi API danh sách sau dùng lại.
- Bảng *Ánh xạ cột RBAC* là chỗ tra khi đọc file đặc tả; nó lệch file xlsx ở hai chỗ có chủ ý ("Hệ thống", "Cổng thanh toán" không phải vai trò).

## Follow-up

- [ ] Bảng gán nhân viên vào tòa nhà (mục *Deferred* của scope): khi làm, chỉ đổi `PhamViTinhToan`.
- [ ] Tính năng 11 (Quản lý tài khoản): đổi vai trò hay khóa tài khoản nên thu hồi phiên, để khoảng lệch giữa token và DB không kéo dài tới 15 phút ở lần làm mới kế tiếp.
- [ ] Cân nhắc một test so ma trận frontend với annotation backend (ví dụ sinh danh sách vai trò của từng endpoint vào `openapi.json` dưới dạng extension `x-vai-tro` rồi kiểm ở frontend) nếu hai bên bắt đầu lệch.
- [ ] Cập nhật `docs/danh_sach_dac_ta_api_quan_ly_chung_cu.xlsx`: sort mặc định của API-UC01-01 là `tenToaNha,asc`, response có `soCanHo`.
- [ ] `/sync`: ghi vào `AGENTS.md` quy ước ba annotation phân quyền, `PhamViTruyCapService`, 404 cho dữ liệu ngoài phạm vi, helper `TrangYeuCau` và ma trận `muc-chuc-nang.ts`.
