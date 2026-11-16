# 0003. Design system và nền tảng giao diện

**Date**: 2026-11-16
**Status**: Accepted

## Summary

Spec này chốt cách mọi màn hình trông và hoạt động: nền ngà ấm, màu chính đỏ gạch, font Be Vietnam Pro, thẻ bo 6px, nút viên thuốc, lấy hướng từ ảnh mẫu nhóm đã chọn. Tính năng này dựng bộ component nền trên shadcn/ui (form, nút, bảng phân trang, thông báo, hộp thoại, khung app có thanh bên) đã xử lý sẵn bàn phím, focus, trạng thái lỗi và đang tải, kèm cách biến lỗi API thành thông báo tiếng Việt. Mọi quy tắc nằm trong `frontend/design.md`, mọi component trưng bày ở trang `/dev/ui` (chỉ có khi chạy dev), và mỗi component có test tự động bằng axe (công cụ dò lỗi trợ năng). Từ tính năng 5 trở đi, màn hình chỉ ghép component này, không tự chọn màu hay tự báo lỗi.

## Requirements

**User stories**:
- Là người làm tính năng 5 đến 13, tôi muốn form, bảng, thông báo và khung trang đã có sẵn và đúng WCAG AA, để chỉ còn ghép nghiệp vụ.
- Là nhân viên BQL hay kế toán, tôi muốn bảng gọn, đọc được nhiều dòng và dùng hết bằng bàn phím.
- Là cư dân, tôi muốn dùng được app trên điện thoại từ 360px mà không phải cuộn ngang trang.
- Là người dùng gặp lỗi, tôi muốn biết rõ lỗi ở ô nào và vì sao, bằng tiếng Việt, và có mã tra cứu khi hệ thống gặp sự cố.

**Acceptance criteria**:
- **AC-1**: `frontend/design.md` có đủ các mục ở *Nội dung design.md*; mọi biến màu, bo góc, bóng trong `src/app/globals.css` khớp một một với bảng token của spec này (không còn biến `--chart-*`, `--sidebar-*` do shadcn sinh); một test đọc `globals.css` và khẳng định mọi cặp trong bảng tương phản đạt ngưỡng (chữ từ 4.5:1, viền và vòng focus từ 3:1).
- **AC-2**: Toàn app dùng Be Vietnam Pro nạp qua `next/font` với subset `vietnamese`; chuỗi mẫu `Ẳ Ẵ Ặ Ỷ Ữ ặ ỡ ừ` trên `/dev/ui` hiển thị bằng font này, dấu không bị cắt ở mọi cỡ chữ trong thang chữ; số trong bảng và ô số liệu dùng chữ số đều cột (`tabular-nums`).
- **AC-3**: Có đủ component ở mục *Danh mục component*; trang `/dev/ui` hiện từng component ở mọi trạng thái tĩnh áp dụng được (mặc định, vô hiệu, đang tải, lỗi, rỗng); trạng thái rê chuột và focus xem được bằng chuột và phím Tab trên chính trang đó.
- **AC-4**: Mọi thành phần tương tác dùng được chỉ bằng bàn phím, có vòng focus nhìn thấy được (viền 2px màu `--ring`, cách 2px; trong vùng `.on-dark` thì `--ring` là màu `--on-dark`); hộp thoại giữ focus bên trong, Esc đóng và trả focus về nút đã mở; `ConfirmDialog` mở ra với focus ở nút "Hủy"; `AppShell` có liên kết "Bỏ qua, tới nội dung chính" là điểm Tab đầu tiên, trỏ tới `<main id="noi-dung" tabIndex={-1}>`.
- **AC-5**: Mỗi ô nhập trong form, kể cả Select, Checkbox, Switch, có nhãn gắn với ô, gợi ý (nếu có) và lỗi nối qua `aria-describedby`, đặt `aria-invalid` khi lỗi; ô bắt buộc có dấu `*` kèm chữ ẩn "(bắt buộc)"; lỗi hiện kèm icon cảnh báo và chữ, không chỉ đổi màu. Form kiểm tra khi rời ô lần đầu, sau đó theo từng phím (`mode: "onTouched"`); bấm gửi còn lỗi kiểm tra phía trình duyệt thì focus về ô lỗi đầu tiên; đang gửi thì nút gửi hiện vòng quay, đặt `aria-disabled="true"` (không dùng `disabled`, để focus không rơi mất) và lần bấm thứ hai không gửi.
- **AC-6**: Khi API ném `ApiError`: lỗi trong `errors[]` có `field` trùng tên ô nhập thì hiện câu `errors[].message` dưới ô đó; lỗi còn lại hiện trong `FormAlert` ở đầu form, khung này không tự biến mất và chỉ xóa khi gửi lại. Focus: có lỗi gắn được vào ô thì về ô lỗi đầu tiên, không có thì về `FormAlert`; cùng lỗi lặp lại vẫn chuyển focus lại. Câu thông báo chung lấy từ từ điển `errorCode` ở mục *Xử lý lỗi API*; mã chưa có dùng câu chung theo mã HTTP; `message` chung của response lỗi không bao giờ được hiện.
- **AC-7**: Với lỗi 5xx và `BACKEND_UNAVAILABLE`, cả `FormAlert`, `ErrorState` lẫn toast lỗi có thêm dòng "Mã tra cứu: <requestId>" và nút sao chép (khi có `requestId`); sao chép xong báo "Đã sao chép" qua vùng `aria-live`; trình duyệt chặn sao chép thì bôi chọn sẵn mã và báo "Hãy nhấn Ctrl+C để sao chép"; lỗi 4xx không hiện dòng này.
- **AC-8**: Toast (Sonner) chỉ dùng báo thành công, tự tắt sau 5 giây, được trình đọc màn hình đọc lên, nhãn của Sonner bằng tiếng Việt. Lỗi của thao tác không nằm trong form hay hộp thoại hiện bằng toast lỗi không tự tắt, có nút "Đóng".
- **AC-9**: `DataTable` phân trang và sắp xếp phía server đúng hợp đồng spec 0001 (`page` từ 1, `size` 10, 20 hoặc 50, `sort=truong,asc|desc`, đọc `items`, `totalItems`, `totalPages`) theo các quy tắc ở *Giao diện DataTable*; chỉ cột khai báo `meta.sortField` sắp xếp được; lần tải đầu hiện skeleton; lần tải sau giữ dữ liệu cũ làm mờ, có thanh tải mỏng và `aria-busy`; không có dòng nào thì hiện trạng thái rỗng và ẩn thanh phân trang; lỗi thì hiện thông báo theo AC-6 và nút "Thử lại"; `th` của cột đang sắp xếp có `aria-sort`, bên trong là nút bấm; bảng có `caption`.
- **AC-10**: Chữ dài trong bảng và thẻ xuống dòng, không bị cắt mất; cột có `meta.description` cắt ở 2 dòng; bảng rộng hơn màn hình cuộn ngang trong khung của nó (khung có `tabIndex={0}`, `role="region"` và nhãn để dùng phím mũi tên cuộn được), trang không cuộn ngang.
- **AC-11**: Hàm định dạng cho ra `4.850.000 ₫` (tiền, khoảng trắng trước `₫` là NBSP do `Intl` sinh), `16/11/2026` (ngày), `16/11/2026 14:05` (ngày giờ 24h), luôn theo múi giờ `Asia/Ho_Chi_Minh` bất kể múi giờ máy người dùng; ngày dạng `2026-11-16` từ API không bị lệch ngày; giá trị `null`, `undefined` hoặc không hợp lệ cho ra `—`.
- **AC-12**: `AppShell` nhận danh sách mục menu qua props; từ 1024px có thanh bên trái cố định, dưới 1024px thanh bên thành ngăn kéo mở bằng nút menu; mục đang chọn là viên thuốc tối và có `aria-current="page"`; thương hiệu là icon `Building2` màu chính cạnh chữ "Quản lý Chung cư"; ở 360px và khi phóng to 200% không có cuộn ngang trang.
- **AC-13**: Có `not-found.tsx`, `error.tsx` và `global-error.tsx` (bắt lỗi ở chính root layout) ở gốc app theo design system, bằng tiếng Việt, có nút "Về trang chủ" (và "Thử lại" ở trang lỗi); dựng trên component `ErrorState` để tính năng 7 dùng lại cho trang 403. Props của error boundary theo đúng tài liệu Next.js 16 trong `node_modules/next/dist/docs/`.
- **AC-14**: Mọi đường dưới `/dev/` (gồm `/dev/ui` và `/dev/ui/shell`) trả 404 khi `NODE_ENV` là `production`, kiểm bằng cả test và `curl` vào bản `next build` rồi `next start`; chỉ xem được khi chạy `pnpm dev`.
- **AC-15**: `pnpm test` chạy Vitest với Testing Library và axe; mỗi component có ít nhất một test ở trạng thái chính và trạng thái lỗi (nếu có) với 0 vi phạm axe; CI chạy `pnpm test`.
- **AC-16**: Không component hay trang nào dùng màu thô; test quét ở mục *Test chặn màu thô* thất bại khi gặp.
- **AC-17**: Khi người dùng bật giảm chuyển động (`prefers-reduced-motion: reduce`), mọi transition và animation (kể cả mở đóng Dialog, Sheet) tắt hẳn nhờ một luật CSS toàn cục; trạng thái đang tải vẫn nhận ra được bằng chữ "Đang tải" (ẩn hoặc hiện) và `aria-busy`.
- **AC-18**: Link trong nội dung luôn gạch chân (màu chính so với chữ thường chỉ 2.88:1, không đủ phân biệt bằng màu); ô nhập dùng chữ 16px dưới 640px để iOS không tự phóng to.
- **AC-19**: `QueryClient` không tự thử lại với `ApiError` có status 4xx, thử lại đúng 1 lần với 5xx và `BACKEND_UNAVAILABLE`, nên trạng thái lỗi hiện trong vài giây.

## Decision

**Chosen option**: Option 1: shadcn/ui phủ token ngữ nghĩa lấy từ ảnh mẫu, cộng lớp component ghép và `design.md`.

Dựng component nền bằng shadcn/ui (Radix) theo bảng token dưới đây, thêm lớp component ghép của dự án, ghi quy tắc vào `frontend/design.md` và trưng bày ở `/dev/ui`.

**Implementation skills**: `ui-styling` (`.claude/skills/ui-styling/`, Tailwind CSS + shadcn/ui) · `design-system` (`.claude/skills/design-system/`, cấu trúc token và spec component) · `tanstack-table` (`tanstack-skills/tanstack-skills`, `.agents/skills/tanstack-table/`, cho `DataTable`) · `next-dev-loop` (`vercel/next.js`, `.agents/skills/next-dev-loop/`, kiểm tra `/dev/ui` khi `pnpm dev` chạy)

## Feature design

### Nguồn thiết kế

Ảnh mẫu nhóm cung cấp (mô tả ở [rationale.md](rationale.md#ảnh-mẫu)), chỉ lấy phong cách. Sau tính năng này, `frontend/design.md` là nguồn sự thật duy nhất; ảnh mẫu không còn được dùng làm căn cứ.

### Token

Đặt trong `src/app/globals.css`: biến CSS trên `:root`, ánh xạ sang Tailwind bằng `@theme inline` theo cách của shadcn. Tên theo vai trò, không theo màu, để thêm chế độ tối sau này chỉ cần thêm một khối biến. Chỉ có giao diện sáng; giữ `@custom-variant dark (&:is(.dark *));` để lớp `dark:` trong code shadcn không tự bật theo hệ điều hành.

**Màu** (mọi cặp chữ và nền đã tính tương phản, xem [rationale.md](rationale.md#bảng-tương-phản-đã-tính)):

| Biến | Giá trị | Dùng cho |
|---|---|---|
| `--background` | `#F5F2ED` | Nền trang |
| `--foreground` | `#1F1A17` | Chữ chính |
| `--card` / `--popover` | `#FFFDFA` | Thẻ, hộp thoại, menu nổi |
| `--card-foreground` / `--popover-foreground` | `#1F1A17` | Chữ trên thẻ |
| `--muted` | `#ECE7E0` | Nền phụ: hàng nhãn giá trị, đầu bảng, ô số liệu sáng |
| `--muted-foreground` | `#655C55` | Chữ phụ, mô tả, gợi ý |
| `--border` | `#E3DDD5` | Viền thẻ, đường kẻ bảng (trang trí) |
| `--input` | `#8C8279` | Viền ô nhập, checkbox, switch tắt (cần 3:1) |
| `--primary` | `#B23A1A` | Nút chính, link, nhãn nhỏ trên tiêu đề mục |
| `--primary-hover` | `#93300F` | Nút chính khi rê chuột hoặc nhấn |
| `--primary-foreground` | `#FFFFFF` | Chữ trên nút chính |
| `--primary-soft` | `#F6E3DC` | Nền nhạt của màu chính (mục chọn trong select, chip nhấn) |
| `--secondary` | `#ECE7E0` | Nút phụ (giống `--muted`) |
| `--secondary-foreground` | `#1F1A17` | Chữ nút phụ |
| `--accent` | `#E3DCD3` | Nền khi rê chuột: mục menu, dòng bảng, nút `secondary`, `outline`, `ghost` (khác `--muted` để thấy được trên đầu bảng) |
| `--accent-foreground` | `#1F1A17` | |
| `--destructive` | `#B0123B` | Lỗi, nút nguy hiểm |
| `--destructive-hover` | `#8E0E30` | Nút nguy hiểm khi rê chuột hoặc nhấn |
| `--destructive-foreground` | `#FFFFFF` | Chữ trên nút nguy hiểm |
| `--destructive-soft` | `#FCE7EC` | Nền khung lỗi |
| `--success` / `--success-soft` | `#1B6B37` / `#DDF3E2` | Trạng thái thành công, switch bật |
| `--warning` / `--warning-soft` | `#8A4A00` / `#FCEAD3` | Trạng thái chờ, đang xử lý |
| `--info` / `--info-soft` | `#1D5592` / `#E3ECF7` | Thông tin |
| `--highlight` / `--highlight-foreground` | `#BFF2CC` / `#14532D` | Ô số liệu nổi bật (xanh bạc hà của ảnh mẫu) |
| `--surface-dark` | `#1F1A17` | Mảng tối của `HeroBand`, mục menu đang chọn |
| `--on-dark` | `#FAF7F2` | Chữ và vòng focus trên mảng tối |
| `--on-dark-muted` | `#BDB3A9` | Chữ phụ trên mảng tối |
| `--ring` | `#B23A1A` | Vòng focus |
| `--overlay` | `rgb(31 26 23 / 0.5)` | Lớp phủ sau Dialog, AlertDialog, Sheet (thay `bg-black/50` của shadcn) |

Lớp `.on-dark` (đặt trên `HeroBand` và mục menu đang chọn) đặt lại `--ring: var(--on-dark)` cho mọi thứ bên trong.

**Trạng thái dùng chung**: rê chuột của nút chính dùng `--primary-hover`, nút nguy hiểm dùng `--destructive-hover`, các nút còn lại dùng `--accent`. Vô hiệu: `opacity: 0.5` và `cursor: not-allowed`, chỉ cho thứ thật sự không dùng được (không dùng cho đang tải). Placeholder màu `--muted-foreground` và không bao giờ thay nhãn. `StatusBadge` tone `neutral` là chữ `--foreground` trên `--muted`; các tone khác là chữ màu tone trên nền `-soft` của tone đó (`danger` dùng `--destructive` và `--destructive-soft`). Toast: nền `--card`, viền `--border`, chữ `--foreground`, bóng `--shadow-overlay`, icon màu tone (thành công `--success`, lỗi `--destructive`).

**Chữ**: Be Vietnam Pro qua `next/font/google`, `subsets: ["vietnamese", "latin"]`, `weight: ["200", "400", "500", "600"]`, `display: "swap"`, gắn vào biến `--font-sans`. Chữ nhỏ nhất 12px. Dòng chữ tiếng Việt cần khoảng dòng từ 1.4 trở lên ở cỡ thân bài.

| Vai trò | Cỡ / dòng | Nét | Ghi chú |
|---|---|---|---|
| Số lớn (`StatTile`) | 40px / 1.1 (32px dưới 640px) | 200 | `tabular-nums`, chỉ dùng cho số |
| Tiêu đề trang | 28px / 1.25 (24px dưới 640px) | 500 | Một `h1` mỗi trang |
| Tiêu đề mục | 22px / 1.3 | 500 | `h2` |
| Tiêu đề thẻ | 17px / 1.4 | 500 | `h3` |
| Thân bài | 15px / 1.6 | 400 | |
| Bảng, ô nhập, nút | 14px / 1.5 | 400 (nút 500) | ô nhập 16px dưới 640px (AC-18) |
| Nhãn nhỏ trên tiêu đề mục | 13px / 1.4 | 500 | màu `--primary`, không viết hoa toàn bộ |
| Chú thích, gợi ý | 13px / 1.5 | 400 | màu `--muted-foreground` |

Chữ số đều cột: bước 1 của build kiểm tra Be Vietnam Pro có tính năng `tnum` (hiển thị `1111` và `8888` cùng độ rộng khi bật `tabular-nums`). Không có thì bỏ yêu cầu `tabular-nums` ở AC-2, căn phải cột số và ghi lại trong `design.md`.

**Khoảng cách**: thang 4px mặc định của Tailwind. Lề trang 24px từ 640px, 16px dưới 640px. Khoảng giữa các mục 40px. Đệm thẻ 20px (16px dưới 640px). Ô nhập, nút `md`, dòng bảng cao 40px; nút `sm` 32px (chữ 13px); nút `lg` 48px (chữ 15px); vùng bấm tối thiểu 24 × 24px. Nội dung rộng tối đa 1440px.

**Bo góc**: `--radius: 6px`; ánh xạ shadcn `--radius-sm: 4px`, `--radius-md`, `--radius-lg`, `--radius-xl` đều `6px`. Dùng cho thẻ, ô nhập, select, bảng, hộp thoại, khung thông báo, `HeroBand`. `rounded-full` cho nút, chip trạng thái, mục menu đang chọn, avatar.

**Bóng**: thẻ không có bóng, chỉ viền `--border`. Hộp thoại, ngăn kéo, menu nổi, toast có một bóng mềm duy nhất (`--shadow-overlay: 0 8px 24px rgb(31 26 23 / 0.12)`).

**Chuyển động**: 150ms `ease-out` cho màu và độ mờ. Một luật toàn cục trong `globals.css` dưới `@media (prefers-reduced-motion: reduce)` đặt `animation-duration` và `transition-duration` của mọi phần tử về `0.01ms` và `animation-iteration-count: 1`; vòng quay khi đó đứng yên, chữ "Đang tải" vẫn có.

**Link**: màu `--primary`, luôn gạch chân, rê chuột đổi sang `--primary-hover`.

**Điểm ngắt**: mặc định Tailwind; thanh bên cố định từ `lg` (1024px).

### Danh mục component

Component sinh từ shadcn nằm ở `src/components/ui/` (chỉnh theo token, dịch chữ ẩn cho trình đọc màn hình như "Close" thành "Đóng"). Component ghép của dự án nằm ở `src/components/<nhóm>/`. Mỗi file dưới khoảng 200 dòng.

| Nhóm | Component | Nguồn | Điểm chính |
|---|---|---|---|
| ui | `Button` | shadcn | biến thể `primary`, `secondary`, `outline`, `ghost`, `destructive`, `link`; cỡ `sm`, `md`, `lg`; prop `loading` hiện `Spinner` trước nhãn, đặt `aria-busy` và `aria-disabled="true"`, chặn `onClick` và submit trong lúc tải, không đặt `disabled` |
| ui | `Input`, `Textarea`, `Label`, `Select`, `Checkbox`, `Switch` | shadcn | viền `--input`, trạng thái `aria-invalid` viền `--destructive` |
| ui | `Field` (và các phần con) | shadcn | khung nhãn, ô, gợi ý, lỗi |
| ui | `Card`, `Badge`, `Alert`, `Separator`, `Skeleton`, `Spinner` | shadcn | |
| ui | `Dialog`, `AlertDialog`, `Sheet` | shadcn | `Sheet` làm ngăn kéo menu trên điện thoại |
| ui | `Table`, `Pagination` | shadcn | phần trình bày cho `DataTable` |
| ui | `Toaster` | shadcn (Sonner) | đặt một lần trong `providers.tsx`, góc dưới phải |
| form | `FormField` | ghép | xem *Hợp đồng form*; lỗi có icon `CircleAlert` |
| form | `FormAlert` | ghép | khung lỗi đầu form, nền `--destructive-soft`, viền trái 3px `--destructive`, `tabIndex={-1}`; nhận `error` và `focusKey`, chuyển focus mỗi khi `focusKey` đổi; hiện `RequestIdNote` theo AC-7 |
| form | `SubmitButton` | ghép | `Button` lấy `loading` từ `formState.isSubmitting` |
| form | `useApiForm` | ghép | xem *Hợp đồng form* |
| feedback | `useApiError` | ghép | hook giữ `{ error, focusKey }` cho nơi không có form (`ConfirmDialog`); `capture(fn)` chạy `fn`, bắt `ApiError`, đặt `error` và tăng `focusKey`; `clear()` |
| feedback | `RequestIdNote` | ghép | "Mã tra cứu: …" và nút sao chép (lucide `Copy`), thông báo qua vùng `aria-live="polite"`; không sao chép được thì bôi chọn mã và báo nhấn Ctrl+C |
| feedback | `EmptyState`, `ErrorState` | ghép | icon, tiêu đề, mô tả, nút hành động; `ErrorState` dùng cho 404, lỗi bất ngờ, lỗi bảng và sau này 403 |
| feedback | `notifySuccess`, `notifyError` | ghép | bọc Sonner theo AC-8 |
| dialogs | `ConfirmDialog` | ghép trên `AlertDialog` | xem *Hợp đồng ConfirmDialog* |
| data | `DataTable` | ghép trên TanStack Table + `Table` | xem *Giao diện DataTable* |
| data | `StatusBadge` | ghép trên `Badge` | prop `tone`: `neutral`, `info`, `success`, `warning`, `danger`; cao 24px, đệm ngang 10px, chữ 13px nét 500, `rounded-full`; mỗi tính năng tự ánh xạ trạng thái nghiệp vụ sang `tone` |
| data | `KeyValueList` | ghép | `<dl>`; mỗi cặp là một hàng `flex` cao tối thiểu 40px, đệm 12px dọc 12px ngang, `<dt>` trái màu `--foreground` nét 400, `<dd>` phải nét 500 căn phải; prop `variant`: `striped` (mỗi hàng nền `--muted`, cách nhau 4px, bo 6px) hoặc `divided` (nền trong suốt, kẻ `--border` giữa các hàng); dưới 640px nhãn và giá trị xếp chồng, giá trị căn trái |
| data | `StatTile` | ghép | thẻ đệm 20px, bo 6px; trên cùng ô icon 32px (nền `--card`, viền `--border`, bo 6px, icon 18px màu `--primary`), dưới đó nhãn 14px màu chữ phụ, rồi số lớn (thang *Số lớn*), dòng gợi ý 13px tùy chọn; biến thể `default` (nền `--muted`), `highlight` (nền `--highlight`, chữ `--highlight-foreground`), `on-dark` (nền `--on-dark` độ mờ 8%, viền `--on-dark` độ mờ 16%, chữ `--on-dark`) |
| layout | `AppShell` | ghép | xem *Giao diện AppShell* |
| layout | `PageHeader` | ghép | `h1`, mô tả, vùng nút hành động căn phải (xuống dưới tiêu đề khi dưới 640px); cách nội dung 32px |
| layout | `SectionHeader` | ghép | nhãn nhỏ (thang *Nhãn nhỏ*) cách `h2` 4px, mô tả 14px màu chữ phụ cách `h2` 4px; prop `as` đổi `h2` thành `h3`; cách nội dung mục 20px |
| layout | `HeroBand` | ghép | lớp `.on-dark`, nền `--surface-dark`, bo 6px, đệm 32px (20px dưới 640px); lưới hai vùng từ 1024px: trái là dòng chào 18px nét 400, tên hoặc tiêu đề 36px nét 500, dòng phụ 14px `--on-dark-muted`; phải là vùng con chứa `StatTile` `on-dark` xếp lưới tự co (tối thiểu 160px mỗi ô); dưới 1024px xếp chồng; không ảnh nền |
| layout | `Brand` | ghép | icon `Building2` màu chính và chữ "Quản lý Chung cư" (một hằng số `APP_NAME` trong `src/lib/brand.ts`) |

Ngoài phạm vi, để tính năng đầu tiên cần tới tự thêm theo `design.md`: chọn ngày, combobox, tải tệp, biểu đồ, menu người dùng.

### Hợp đồng form

```ts
const form = useApiForm({ schema, defaultValues });   // useForm + zodResolver + mode "onTouched"
<form onSubmit={form.submitWith((values) => mutation.mutateAsync(values))} noValidate>
  <FormAlert error={form.formError} focusKey={form.formErrorKey} />
  <FormField control={form.control} name="soDienThoai" label="Số điện thoại" required
    render={(field) => <Input {...field} />} />
  <SubmitButton form={form}>Lưu</SubmitButton>
</form>
```

- `submitWith(fn)` trả về handler cho `onSubmit`. Nó xóa `formError` khi bắt đầu, gọi `form.handleSubmit` với `fn`, và `fn` **phải trả về promise** (dùng `mutateAsync`, không dùng `mutate`), vì `isSubmitting` và việc chặn gửi lần hai dựa vào promise đó. Nó bắt `ApiError` ném ra; lỗi khác ném tiếp để `error.tsx` bắt.
- Khi bắt được `ApiError`: mỗi `errors[i]` có `field` là tên một ô đã đăng ký thì `setError(field, { message })`; phần còn lại thành `formError` (câu chung theo *Xử lý lỗi API*, kèm danh sách câu của các lỗi theo trường không khớp ô nào). Có ít nhất một lỗi gắn vào ô thì focus ô đầu tiên theo thứ tự trong form (`setFocus`); không có thì tăng `formErrorKey` để `FormAlert` nhận focus.
- `FormField` dùng `Controller` cho **mọi** loại ô. Hàm `render` nhận `{ id, name, value, onChange, onBlur, ref, "aria-invalid", "aria-describedby", "aria-required" }`. Với Radix `Select`, `Checkbox`, `Switch`, chỗ ghép phải chuyển `onValueChange`/`onCheckedChange` thành `onChange`, gắn `ref` và `onBlur` vào phần tử nhận focus (trigger của Select), để `onTouched` và focus ô lỗi chạy đúng. Ví dụ cho từng loại nằm trên `/dev/ui`.
- Nhãn bắt buộc: prop `required` hiện `*` màu `--destructive` có `aria-hidden` cộng chữ ẩn "(bắt buộc)".

### Hợp đồng ConfirmDialog

```ts
type ConfirmDialogProps = {
  trigger: ReactNode; title: string; description: ReactNode;
  confirmLabel: string;                       // động từ cụ thể: "Khóa tài khoản"
  tone?: "default" | "destructive";
  onConfirm: () => Promise<void>;             // ném ApiError khi lỗi
};
```

Bấm nút xác nhận: gọi `event.preventDefault()` trên `AlertDialogAction` để hộp thoại không tự đóng, chạy `onConfirm` qua `useApiError().capture`, nút ở trạng thái `loading`. Thành công thì đóng hộp thoại (trả focus về `trigger`); người gọi tự gọi `notifySuccess`. Lỗi thì giữ hộp thoại mở và hiện `FormAlert` trong hộp thoại. Đóng hộp thoại thì `clear()` lỗi.

### Giao diện DataTable

```ts
type DataTableProps<T> = {
  caption: string;                         // bắt buộc, có thể ẩn bằng sr-only
  columns: ColumnDef<T>[];                 // meta.description = true để cắt 2 dòng
  query: UseQueryResult<PageResponse<T>, ApiError>;
  page: number; size: 10 | 20 | 50;        // trạng thái do trang giữ (thường trong URL)
  sort?: { field: string; dir: "asc" | "desc" };
  onPageChange(page: number): void;
  onSizeChange(size: number): void;
  onSortChange(sort?: { field: string; dir: "asc" | "desc" }): void;
  emptyState?: ReactNode;
};
```

TanStack Table chạy `manualPagination` và `manualSorting`. Trang gọi API với `placeholderData: keepPreviousData` để có hành vi "giữ dữ liệu cũ làm mờ". `PageResponse<T>` là kiểu chung trong `src/lib/api/` khớp dạng `items`, `page`, `size`, `totalItems`, `totalPages` của spec 0001. Thanh phân trang ghi "135 kết quả", "Trang 2 / 7", nút "Trang trước", "Trang sau", chọn "Số dòng mỗi trang".

Quy tắc:
- **Cột sắp xếp được**: chỉ cột có `meta.sortField` (tên trường gửi lên `sort=`, phải nằm trong danh sách trắng của endpoint); cột khác đặt `enableSorting: false`. Bấm tiêu đề xoay vòng: tăng, giảm, bỏ sắp xếp. Sắp xếp chỉ một cột một lúc.
- **Trang**: đổi sắp xếp hoặc số dòng thì gọi `onPageChange(1)`. Dữ liệu về có `page > totalPages` và `totalPages > 0` (ví dụ sau khi xóa) thì gọi `onPageChange(totalPages)`. Nút "Trang trước" ở trang 1 và "Trang sau" ở trang cuối có `aria-disabled`.
- **Giá trị từ URL**: `page` không phải số nguyên dương thì dùng 1; `size` ngoài 10, 20, 50 thì dùng 20. Hàm `parseTableParams(searchParams)` trong `src/components/data/` làm việc này cho mọi trang.
- **Lỗi**: chưa có dữ liệu thì thay bảng bằng `ErrorState` có nút "Thử lại"; đã có dữ liệu cũ thì giữ bảng và hiện `Alert` lỗi kèm "Thử lại" phía trên bảng.
- **Rỗng**: `totalItems = 0` thì hiện `emptyState` (mặc định `EmptyState` "Chưa có dữ liệu") và ẩn thanh phân trang.
- Đang tải lần sau: thân bảng `opacity: 0.6`, `aria-busy="true"` trên `<table>`, thanh tải mỏng 2px màu `--primary` dưới đầu bảng.

### Giao diện AppShell

```ts
type NavItem = { href: string; label: string; icon: LucideIcon };
type NavGroup = { label?: string; items: NavItem[] };
type AppShellProps = { nav: NavGroup[]; userSlot?: ReactNode; children: ReactNode };
```

- **Mục đang chọn**: so `usePathname()` với `href` theo từng đoạn đường dẫn: khớp khi bằng nhau hoặc đường dẫn bắt đầu bằng `href + "/"` (nên `/can-ho` không khớp `/can-ho-khac`); `href` là `/` chỉ khớp đúng `/`; nhiều mục khớp thì chọn `href` dài nhất.
- **Bố cục**: thanh bên rộng 256px, nền `--background`, viền phải `--border`, nhóm có nhãn 12px nét 500 màu chữ phụ; mục menu cao 40px, icon 18px, mục đang chọn là viên thuốc nền `--surface-dark` chữ `--on-dark` (có lớp `.on-dark`). Thanh trên cao 64px chứa nút mở menu (dưới 1024px, nhãn "Mở menu"), `Brand` và `userSlot`. Vùng nội dung `<main id="noi-dung" tabIndex={-1}>` có `outline: none` khi nhận focus từ liên kết bỏ qua.
- **Ngăn kéo dưới 1024px**: `Sheet` từ bên trái chứa cùng menu, tự đóng khi `usePathname()` đổi hoặc khi bấm một mục.
- **Chỗ gắn**: tính năng 7 quyết định layout nào bọc `AppShell` (ví dụ một route group cho trang sau đăng nhập) và truyền menu theo vai trò. Tính năng này chỉ trưng bày ở `/dev/ui/shell`, một trang riêng có `AppShell` bọc toàn trang, để không lồng vào `/dev/ui` (giữ một liên kết bỏ qua và một `h1` mỗi trang).

### Xử lý lỗi API trên giao diện

`src/lib/api/error-messages.ts` giữ từ điển `errorCode` sang câu tiếng Việt và hàm `messageFor(error: ApiError): string`. Từ điển ban đầu, đúng nguyên văn:

| `errorCode` | Câu |
|---|---|
| `VALIDATION_FAILED` | Dữ liệu chưa hợp lệ, vui lòng kiểm tra các ô được đánh dấu. |
| `MALFORMED_REQUEST` | Yêu cầu không hợp lệ, vui lòng tải lại trang và thử lại. |
| `METHOD_NOT_ALLOWED` | Thao tác này không được hỗ trợ. |
| `UNSUPPORTED_MEDIA_TYPE` | Định dạng dữ liệu gửi lên không được hỗ trợ. |
| `UNAUTHENTICATED` | Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại. |
| `FORBIDDEN` | Bạn không có quyền thực hiện thao tác này. |
| `NOT_FOUND` | Không tìm thấy dữ liệu yêu cầu, có thể nó đã bị xóa. |
| `CONFLICT` | Dữ liệu bị trùng hoặc đang ở trạng thái không cho phép thao tác này. |
| `CONCURRENT_MODIFICATION` | Dữ liệu vừa được người khác thay đổi, vui lòng tải lại và thử lại. |
| `PAYLOAD_TOO_LARGE` | Tệp quá lớn, giới hạn là 20MB. |
| `BUSINESS_RULE_VIOLATION` | Thao tác vi phạm quy định của hệ thống. |
| `INTERNAL_ERROR` | Hệ thống đang gặp sự cố, vui lòng thử lại sau. |
| `BACKEND_UNAVAILABLE` | Không kết nối được tới máy chủ, vui lòng thử lại sau ít phút. |

Quy tắc cho tính năng sau: khi một lỗi 409 hoặc 422 có ý nghĩa riêng mà người dùng cần biết (ví dụ trùng số điện thoại, tài khoản đang bị khóa), backend phải trả mã riêng có tiền tố phân hệ (`AUTH_PHONE_TAKEN`) và tính năng đó thêm câu vào từ điển này; không dựa vào `CONFLICT` hay `BUSINESS_RULE_VIOLATION` chung. Câu chung theo mã HTTP khi mã chưa có trong từ điển:

| HTTP | Câu |
|---|---|
| 400, 422 | Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại. |
| 401 | Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại. |
| 403 | Bạn không có quyền thực hiện thao tác này. |
| 404 | Không tìm thấy dữ liệu yêu cầu. |
| 409 | Dữ liệu đã thay đổi hoặc bị trùng, vui lòng tải lại và thử lại. |
| 413 | Tệp quá lớn. |
| 5xx, 0 | Hệ thống đang gặp sự cố, vui lòng thử lại sau. |

Lỗi trong `errors[]` dùng chính `message` của từng trường (backend viết tiếng Việt cho lỗi kiểm tra dữ liệu theo trường); quy tắc "không dùng `message`" áp cho thông báo chung của response. Cách chuyển hướng khi gặp 401 thuộc tính năng 5 và 6.

**QueryClient** (`src/app/providers.tsx`): `retry` là hàm trả `false` khi lỗi là `ApiError` có `400 <= status < 500`, ngược lại cho thử lại khi `failureCount < 1`; mutation không thử lại. Đây là cấu hình chung, tính năng không tự đặt lại.

### Định dạng

`src/lib/format.ts`, mọi hàm nhận `null` hoặc `undefined` hoặc giá trị không hợp lệ thì trả `"—"`:
- `formatTien(n: number)`: `Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" })`, cho ra `4.850.000 ₫` (NBSP trước `₫`; test so với chuỗi có ` `).
- `formatNgayGio(value: string)`: `new Date(value)` (chấp nhận ISO có `Z` hoặc độ lệch như `+07:00`), rồi `Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).formatToParts` và ghép thành `dd/MM/yyyy HH:mm` (vì thứ tự mặc định của `vi-VN` là giờ trước ngày).
- `formatNgay(value: string)`: chuỗi khớp `^\d{4}-\d{2}-\d{2}$` là `LocalDate`, tách chuỗi thành `dd/MM/yyyy`, không qua `Date`; chuỗi ISO khác là thời điểm, định dạng phần ngày như trên.

### Trang /dev/ui

`src/app/dev/layout.tsx` gọi `notFound()` khi `process.env.NODE_ENV === "production"`, nên chặn mọi trang dưới `/dev/`. `src/app/dev/ui/page.tsx` chia mục theo nhóm component, có mục lục link tới từng mục. Có một form mẫu dùng đủ Input, Textarea, Select, Checkbox, Switch, gửi tới hàm giả theo lựa chọn: thành công, `VALIDATION_FAILED` có lỗi theo trường (một trường khớp ô, một không), `CONFLICT`, `INTERNAL_ERROR` có `requestId`, `BACKEND_UNAVAILABLE`; một `ConfirmDialog` mẫu thành công và lỗi; một `DataTable` mẫu trên dữ liệu giả có độ trễ 600ms, công tắc chuyển sang rỗng và lỗi; `HeroBand` với `StatTile`; chuỗi thử dấu tiếng Việt ở mọi cỡ trong thang chữ; bảng màu với tên biến; kiểm tra chữ số đều cột. `src/app/dev/ui/shell/page.tsx` trưng bày `AppShell` với menu mẫu hai nhóm. Tính năng 5 khi bảo vệ route bằng `proxy.ts` phải chừa `/dev/` ra (ghi ở Follow-up).

### Nội dung design.md

`frontend/design.md` có các mục: nguyên tắc (4 đến 6 dòng: ấm, gọn, rõ lỗi, bàn phím trước); token (bảng màu kèm tương phản, chữ, khoảng cách, bo góc, bóng, chuyển động, như spec này); quy tắc dùng màu (màu chính chỉ cho hành động và nhãn nhỏ, không bao giờ cho lỗi hay cảnh báo; lỗi luôn có icon; link luôn gạch chân); danh mục component (mỗi component: dùng khi nào, không dùng khi nào, props chính, trạng thái); mẫu ghép (form có lỗi API, bảng danh sách, trang tổng quan với `HeroBand`, hộp thoại xác nhận); định dạng số, tiền, ngày; cách thêm một component mới (sinh bằng shadcn, đổi sang token, dịch chữ ẩn, thêm vào `/dev/ui`, viết test axe, cập nhật file này).

### Hạ tầng test

- `vitest.config.ts`: `@vitejs/plugin-react`, `vite-tsconfig-paths`, `environment: "jsdom"`, `setupFiles: ["./vitest.setup.ts"]`; đầu file đặt `process.env.TZ = "America/New_York"` để mọi test chạy ở múi giờ khác Việt Nam (đặt bằng biến trong lệnh không ổn định trên Windows).
- `vitest.setup.ts`: `@testing-library/jest-dom/vitest`; polyfill cho Radix trên jsdom (`ResizeObserver`, `Element.prototype.hasPointerCapture`, `releasePointerCapture`, `scrollIntoView`, `window.matchMedia`); mock `next/navigation` (`usePathname`, `useRouter`, `useSearchParams`, `notFound`) và `next/font/google` (trả `{ className, variable, style }`).
- Trợ năng: helper `expectNoA11yViolations(container)` trong `src/test/axe.ts` gọi thẳng `axe-core` (`axe.run`), báo lỗi kèm danh sách vi phạm; không dùng `vitest-axe`. Tắt quy tắc `color-contrast` (jsdom không tính được), tương phản do test bảng tương phản lo.
- Test môi trường production: `vi.stubEnv("NODE_ENV", "production")` rồi render `dev/layout.tsx`, mong `notFound` được gọi.

### Test chặn màu thô

`src/test/no-raw-colors.test.ts` quét mọi `src/**/*.tsx` trừ `*.test.tsx`. Thất bại khi gặp:
- class màu bảng có sẵn: `\b(bg|text|border|ring|outline|fill|stroke|from|via|to|divide|placeholder|decoration|shadow|accent|caret)-(slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-\d{2,3}\b`, kể cả có hậu tố độ mờ `/50`;
- `\b(bg|text|border|ring|fill|stroke)-(black|white)\b`, kể cả có hậu tố độ mờ;
- mã hex `#[0-9a-fA-F]{3,8}\b` và `rgb(`, `hsl(`, `oklch(` trong file `.tsx`.

Code shadcn sinh ra (`bg-black/50` ở lớp phủ) phải đổi sang `bg-overlay`.

### Data model sketch

Không có. Tính năng này không đổi schema.

### API surface

Không thêm endpoint. Tính năng dùng hợp đồng có sẵn của spec 0001: vỏ bọc lỗi (`errorCode`, `errors[].field`, `errors[].message`, `requestId`), dạng trang (`items`, `page`, `size`, `totalItems`, `totalPages`) và tham số `page`, `size`, `sort`.

### Value sourcing

| Hành động | Giá trị hiển thị | Nguồn |
|---|---|---|
| Báo lỗi chung | Câu tiếng Việt | Từ điển `errorCode` trong `error-messages.ts`; thiếu thì câu theo `ApiError.status` (bảng trên) |
| Báo lỗi theo trường | Ô nhận lỗi và câu lỗi | `ApiError.errors[].field` khớp tên ô trong form (tên field DTO camelCase của backend); `errors[].message` |
| Lỗi theo trường không khớp ô nào | Câu lỗi | Đưa vào `FormAlert` dạng danh sách |
| Mã tra cứu | `requestId` | `ApiError.requestId` (body lỗi, hoặc header `X-Request-Id` theo `api.ts`); không có thì không hiện dòng |
| Thanh phân trang | Tổng kết quả, trang hiện tại, tổng số trang | `PageResponse.totalItems`, `page`, `totalPages` |
| Tiêu đề cột sắp xếp | `aria-sort` | Prop `sort` của `DataTable` |
| Ngày giờ | `dd/MM/yyyy HH:mm` | Chuỗi ISO từ API, múi giờ cố định `Asia/Ho_Chi_Minh` (spec 0001) |
| Tiền | `4.850.000 ₫` | Số từ API (spec 0001), `Intl.NumberFormat` `vi-VN` |
| Mục menu đang chọn | `aria-current` | `usePathname()` so với `NavItem.href` |
| Màu chip trạng thái | `tone` | Ánh xạ do tính năng sở hữu trạng thái tự khai báo |
| Tên app | "Quản lý Chung cư" | Hằng `APP_NAME` trong `src/lib/brand.ts`, cũng dùng cho `metadata.title` |
| Ẩn `/dev/*` | 404 hay trang | `process.env.NODE_ENV`, kiểm ở `src/app/dev/layout.tsx` |
| Tham số `sort=` | Tên trường | `columns[i].meta.sortField`, trùng danh sách trắng của endpoint |
| `page`, `size` từ URL | Số trang, số dòng | `parseTableParams(searchParams)`, mặc định 1 và 20 |
| Có thử lại truy vấn không | Số lần thử | `ApiError.status` qua hàm `retry` của `QueryClient` |
| Ô nhận focus sau lỗi | Ô đầu tiên hay `FormAlert` | Thứ tự đăng ký ô trong form; `formErrorKey` |

### Key invariants

- Component chỉ dùng màu qua token; không class màu bảng có sẵn của Tailwind, không mã hex trong `.tsx` (AC-16, test chặn).
- Không có thông báo lỗi nào chỉ dựa vào màu: luôn có icon và chữ.
- Lỗi không bao giờ đi bằng toast tự tắt.
- Giao diện không bao giờ hiện `message` chung của response lỗi, stack trace hay SQL.
- Mọi ô nhập có nhãn nhìn thấy được; placeholder không thay nhãn.
- Mọi trang có đúng một `h1`; `<html lang="vi">` giữ nguyên.
- `LocalDate` không đi qua `new Date()`.

### Security model

Không có dữ liệu nhạy cảm mới. `/dev/ui` trả 404 ở production nên không lộ trang nội bộ khi demo. Dữ liệu trên `/dev/ui` là dữ liệu giả, không gọi API thật. `requestId` chỉ là mã ngẫu nhiên, hiện ra không lộ gì. Câu lỗi do frontend tự chọn nên backend lỡ trả chi tiết kỹ thuật cũng không tới mắt người dùng.

### Configuration required

Không có biến môi trường mới. Gói mới: `class-variance-authority`, `clsx`, `tailwind-merge`, `tw-animate-css`, các gói `@radix-ui/*` do shadcn kéo theo, `lucide-react`, `sonner`, `@tanstack/react-table`, `react-hook-form`, `zod` (4), `@hookform/resolvers`; dev: `vitest`, `@vitejs/plugin-react`, `vite-tsconfig-paths`, `jsdom`, `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`, `axe-core`. Lúc build cài bản ổn định mới nhất của mỗi gói rồi ghi phiên bản đúng số vào `package.json` (không `^`, không `~`), như các gói hiện có; riêng `zod` lấy dòng 4 và `@hookform/resolvers` lấy bản hỗ trợ Zod 4. `shadcn init` chọn Radix, base color `stone`, biến CSS bật, rồi thay biến theo bảng token.

### Critical test scenarios

- Happy path: form mẫu nhập đúng, gửi, nút có `aria-busy` và `aria-disabled`, bấm lần hai không gọi lại hàm gửi, rồi toast "Đã lưu" được đọc lên; verifies **AC-5**, **AC-8**.
- Kiểm tra phía trình duyệt: rời ô số điện thoại trống thì hiện lỗi, gõ đúng thì lỗi mất ngay; Select để trống rồi gửi thì focus vào trigger của Select; verifies **AC-5**.
- Lỗi theo trường từ API: hàm giả ném `VALIDATION_FAILED` với một lỗi `soDienThoai` và một lỗi `maLa`; lỗi đầu hiện dưới đúng ô có `aria-invalid` và ô nhận focus; lỗi sau vào `FormAlert`; verifies **AC-6**.
- Lỗi hệ thống: `INTERNAL_ERROR` có `requestId` hiện `FormAlert` nhận focus với mã tra cứu; gửi lại gặp cùng lỗi vẫn nhận focus lại; `CONFLICT` thì không có mã; mã lạ `XYZ` với status 422 hiện câu chung của 422; câu `message` của response không xuất hiện ở đâu; verifies **AC-6**, **AC-7**.
- Hộp thoại: mở `ConfirmDialog`, focus ở "Hủy"; xác nhận khi `onConfirm` ném lỗi thì hộp thoại vẫn mở và hiện lỗi; Esc đóng và focus về nút mở; verifies **AC-4**, **AC-6**.
- Bảng: tải đầu thấy skeleton; chuyển trang vẫn thấy dòng cũ với `aria-busy="true"`; đổi số dòng thì về trang 1; trả rỗng thấy `EmptyState` và không có thanh phân trang; lỗi khi đã có dữ liệu thì giữ bảng và hiện "Thử lại", bấm thì gọi `refetch`; cột không có `sortField` không bấm được; bấm tiêu đề cột ba lần đi qua tăng, giảm, bỏ, `aria-sort` trên `th` đổi theo; verifies **AC-9**, **AC-10**.
- Định dạng: trong múi giờ `America/New_York` của bộ test, `formatNgayGio("2026-11-16T07:05:00Z")` ra `16/11/2026 14:05`, `formatNgayGio("2026-11-16T14:05:00+07:00")` cũng vậy, `formatNgay("2026-11-16")` ra `16/11/2026`, `formatTien(4850000)` ra `4.850.000 ₫`, `formatNgay(null)` ra `—`; verifies **AC-11**.
- AppShell: ở `/can-ho/123` mục `/can-ho` có `aria-current`, mục `/` và `/can-ho-khac` không; ngăn kéo đóng khi đường dẫn đổi; verifies **AC-12**.
- QueryClient: truy vấn lỗi 404 gọi hàm đúng 1 lần, lỗi 500 gọi đúng 2 lần; verifies **AC-19**.
- axe: mọi test component chạy `expectNoA11yViolations`, 0 vi phạm; verifies **AC-15**.
- Tương phản: test đọc `globals.css`, mọi cặp trong bảng đạt ngưỡng; verifies **AC-1**.
- Màu thô: test quét thấy `text-gray-600`, `bg-black/50` hoặc `#fff` thì thất bại; verifies **AC-16**.
- Production: `vi.stubEnv("NODE_ENV", "production")`, render `dev/layout.tsx` gọi `notFound()`; sau `next build` và `next start`, `curl` vào `/dev/ui` và `/dev/ui/shell` trả 404; verifies **AC-14**.

**Kiểm tay khi `/check verify`** (không tự động được trong jsdom): dấu tiếng Việt không bị cắt ở mọi cỡ chữ (AC-2); hover và focus nhìn thấy rõ trên từng component (AC-3, AC-4); `/dev/ui` và `/dev/ui/shell` ở 360px và phóng to 200% không cuộn ngang (AC-12); bật giảm chuyển động của hệ điều hành thì Dialog, Sheet mở không có hiệu ứng (AC-17); đi hết `/dev/ui` chỉ bằng bàn phím; chạy axe trên trình duyệt thật (ví dụ tiện ích axe DevTools) cho `/dev/ui`, gồm cả tương phản.

## Build plan

Tracer Bullet áp vào một tính năng chỉ có tầng giao diện: dựng trước một đường mỏng chạy thật từ token qua một component, một form gặp lỗi API, trang `/dev/ui` tới test axe trong CI, rồi làm dày từng nhóm component trên đường đó.

1. **Đường mỏng: token tới form có lỗi API.** Hạ tầng test theo *Hạ tầng test* (Vitest, setup, polyfill, mock, helper axe, script `test`, bước `pnpm test` trong CI); `shadcn init`; token màu, chữ, bo góc, bóng, `--overlay`, lớp `.on-dark`, luật giảm chuyển động và gạch chân link trong `globals.css`, xóa biến `--chart-*` và `--sidebar-*`; Be Vietnam Pro trong `layout.tsx` và kiểm tra `tnum`; `QueryClient` với `retry` mới; `Button`, `Input`, `Label`, `Field`, `Spinner`; `FormField`, `FormAlert`, `RequestIdNote`, `SubmitButton`, `useApiForm`; `error-messages.ts` với từ điển đầy đủ; `dev/layout.tsx` chặn production; `/dev/ui` với chuỗi thử dấu, bảng màu, form mẫu (mới có Input); khung `frontend/design.md` (token); test form và lỗi API có axe, test tương phản, test màu thô, test QueryClient, test production; sửa `page.tsx` bỏ `text-gray-600`. Satisfies **AC-1**, **AC-2**, **AC-5**, **AC-6**, **AC-7**, **AC-14**, **AC-15**, **AC-16**, **AC-17**, **AC-18**, **AC-19**.
2. **Form và phản hồi đủ bộ.** `Textarea`, `Select`, `Checkbox`, `Switch` (ghép vào `FormField` theo *Hợp đồng form*), `Card`, `Badge`, `Alert`, `Skeleton`, `Separator`, `Dialog`, `AlertDialog`, `Toaster` Việt hóa và `notifySuccess`, `notifyError`; `useApiError`, `ConfirmDialog`, `StatusBadge`, `EmptyState`, `ErrorState`; `not-found.tsx`, `error.tsx`, `global-error.tsx`; dịch chữ ẩn của Radix; các mục tương ứng trên `/dev/ui` (form mẫu đủ loại ô) và test. Satisfies **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-8**, **AC-13**.
3. **Hiển thị dữ liệu.** `PageResponse<T>`, `parseTableParams`, `Table`, `Pagination`, `DataTable` theo các quy tắc ở *Giao diện DataTable*; `format.ts`; `KeyValueList`, `StatTile`, `SectionHeader`, `PageHeader`, `HeroBand`; bảng mẫu có độ trễ trên `/dev/ui`; test bảng và định dạng. Satisfies **AC-3**, **AC-9**, **AC-10**, **AC-11**.
4. **Khung app và khóa lại.** `Sheet`, `Brand`, `AppShell` với liên kết bỏ qua, so khớp đường dẫn theo đoạn và ngăn kéo tự đóng; trang `/dev/ui/shell`; hoàn thiện `frontend/design.md` theo *Nội dung design.md*; chạy `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build`, rồi `next start` và `curl` kiểm `/dev/*` trả 404; đi qua danh sách *Kiểm tay* bằng `next-dev-loop`. Satisfies **AC-1**, **AC-3**, **AC-4**, **AC-12**, **AC-14**.

## Consequences

**Positive**:
- Tính năng 5 trở đi chỉ ghép component; báo lỗi, đang tải, bàn phím và tương phản đã đúng sẵn.
- Một từ điển lỗi cho cả 80 API; thêm mã mới là thêm một dòng.
- Bộ test frontend đầu tiên ra đời cùng axe, nên lỗi trợ năng bị bắt từ component đầu.
- Token theo vai trò nên thêm chế độ tối sau này không phải sửa component.

**Negative / tradeoffs**:
- Code shadcn nằm trong repo: nhóm tự bảo trì, cập nhật shadcn là việc tay và có thể đè chỗ đã chỉnh.
- Chọn ngày, combobox, tải tệp chưa có; tính năng đầu tiên cần tới sẽ chậm hơn một chút.
- axe trong jsdom không đo được tương phản màu thật; tương phản dựa vào test bảng tương phản và một lượt axe trên trình duyệt khi `/check verify`.
- Nút đang tải dùng `aria-disabled` thay cho `disabled`, nên mọi nút phải tự chặn hành động khi đang tải; quên chặn là gửi hai lần.
- `submitWith` bắt buộc hàm trả về promise; người dùng `mutate` thay cho `mutateAsync` sẽ mất chặn gửi hai lần mà không có lỗi nào báo.
- `next/font/google` tải font lúc build, nên build (kể cả image Docker) cần mạng.
- Đỏ gạch và đỏ lỗi gần nhau về sắc; nếu ai đó dùng `--primary` cho cảnh báo thì quy tắc tách màu bị phá. `design.md` phải nói rõ.
- `/dev/ui` phải được cập nhật khi thêm component, nếu không nó lệch dần với thực tế.
- Cuộn ngang bảng trên điện thoại vẫn kém tiện; màn hình cho cư dân nên ưu tiên thẻ thay vì bảng rộng.

**Neutral**:
- `frontend/AGENTS.md` (hoặc root) nên trỏ tới `frontend/design.md` để agent làm giao diện đọc nó trước.
- Ảnh mẫu không vào repo; từ đây `design.md` là căn cứ.

## Follow-up

- [ ] `AGENTS.md` nên có một dòng trỏ tới `frontend/design.md` và quy tắc "màn hình chỉ dùng component và token của design system" (`/sync` sau khi build).
- [ ] `AGENTS.md` mục `## Agent skills` nên thêm `tanstack-table` (`.agents/skills/tanstack-table/`) và ghi `Declined` cho các skill và MCP đã xem nhưng không chọn: `antfu/skills@vitest`, `claude-dev-suite` (`axe-core`, `wcag`), `addyosmani/web-quality-skills@accessibility`, `emilkowalski/skills@ask-sonner`, TanStack MCP, Vitest MCP.
- [ ] Spec 0001 ghi `Tailwind CSS + shadcn/ui`; sau tính năng này nên thêm một dòng trỏ sang spec 0003.
- [ ] Khi tính năng đầu tiên cần chọn ngày, combobox hoặc tải tệp: thêm theo token và quy tắc trong `design.md`, có test axe, cập nhật `/dev/ui`.
- [ ] Nếu sau này cần chế độ tối: thêm khối biến `.dark` theo bảng token, tính lại bảng tương phản.
- [ ] Tính năng 5: `proxy.ts` bảo vệ route phải chừa `/dev/` ra, nếu không `/dev/ui` đòi đăng nhập khi dev.
- [ ] Tính năng 5 trở đi: lỗi 409 hoặc 422 có ý nghĩa riêng cần mã riêng có tiền tố phân hệ và một câu trong `error-messages.ts` (quy tắc ở *Xử lý lỗi API*).

## Rationale

Lý do và các phương án: xem [rationale.md](rationale.md).
