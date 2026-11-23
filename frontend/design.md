# Design system: Quản lý Chung cư

Nguồn sự thật duy nhất cho giao diện, từ [spec 0003](../docs/specs/0003-design-system-giao-dien/index.md). Giá trị token nằm trong `src/app/globals.css`; file này nói cách dùng. Mọi component trưng bày ở `/dev/ui` (chỉ có khi chạy `pnpm dev`).

source: spec-0003 (phong cách lấy từ ảnh mẫu của nhóm, ảnh mẫu không còn là căn cứ)
character: ấm, gọn, rõ ràng; nền ngà, màu chính đỏ gạch, mảng tối nâu than cho phần nhấn

## Nguyên tắc

- **Ấm**: nền ngà `--background`, thẻ màu kem `--card`, không dùng trắng tinh hay xám lạnh.
- **Gọn**: thẻ không bóng, chỉ viền mảnh; bảng đọc được nhiều dòng; khoảng cách theo thang 4px.
- **Rõ lỗi**: lỗi luôn có icon và chữ, nói rõ ở ô nào và vì sao, bằng tiếng Việt; không bao giờ chỉ đổi màu.
- **Bàn phím trước**: mọi thứ dùng được bằng Tab, Enter, Esc, phím mũi tên; vòng focus luôn nhìn thấy.
- **Một nguồn**: màn hình chỉ ghép component ở đây, không tự chọn màu, tự báo lỗi hay tự định dạng số.

## Build mandate

Mỗi màn hình mới ghép từ component trong danh mục dưới đây, đặt trong `AppShell`, có `PageHeader` với một `h1`, và có đủ trạng thái đang tải, rỗng, lỗi. Không màu thô (test `src/test/no-raw-colors.test.ts` chặn), không thông báo lỗi tự viết.

## Token

Biến CSS trên `:root` trong `src/app/globals.css`, ánh xạ sang Tailwind bằng `@theme inline`. Tên theo vai trò, không theo màu. Chỉ có giao diện sáng.

### Màu

| Biến                                     | Giá trị               | Dùng cho                                             | Tương phản           |
| ---------------------------------------- | --------------------- | ---------------------------------------------------- | -------------------- |
| `--background`                           | `#F5F2ED`             | Nền trang                                            | chữ chính 15.4:1     |
| `--foreground`                           | `#1F1A17`             | Chữ chính                                            |                      |
| `--card` / `--popover`                   | `#FFFDFA`             | Thẻ, hộp thoại, menu nổi                             | chữ chính 17.0:1     |
| `--muted`                                | `#ECE7E0`             | Nền phụ: hàng nhãn giá trị, đầu bảng, ô số liệu sáng | chữ phụ 5.3:1        |
| `--muted-foreground`                     | `#655C55`             | Chữ phụ, mô tả, gợi ý                                | trên nền trang 5.8:1 |
| `--border`                               | `#E3DDD5`             | Viền thẻ, đường kẻ bảng (trang trí)                  |                      |
| `--input`                                | `#8C8279`             | Viền ô nhập, checkbox, switch tắt                    | trên thẻ 3.7:1       |
| `--primary`                              | `#B23A1A`             | Nút chính, link, nhãn nhỏ                            | trên nền trang 5.4:1 |
| `--primary-hover`                        | `#93300F`             | Nút chính khi rê, nhấn                               |                      |
| `--primary-foreground`                   | `#FFFFFF`             | Chữ trên nút chính                                   | 6.0:1                |
| `--primary-soft`                         | `#F6E3DC`             | Nền nhạt của màu chính                               |                      |
| `--secondary`                            | `#ECE7E0`             | Nút phụ                                              |                      |
| `--accent`                               | `#E3DCD3`             | Nền khi rê: mục menu, dòng bảng, nút phụ             |                      |
| `--destructive`                          | `#B0123B`             | Lỗi, nút nguy hiểm                                   | trên khung lỗi 5.9:1 |
| `--destructive-hover`                    | `#8E0E30`             | Nút nguy hiểm khi rê                                 |                      |
| `--destructive-soft`                     | `#FCE7EC`             | Nền khung lỗi                                        |                      |
| `--success` / `--success-soft`           | `#1B6B37` / `#DDF3E2` | Thành công, switch bật                               |                      |
| `--warning` / `--warning-soft`           | `#8A4A00` / `#FCEAD3` | Chờ, đang xử lý                                      |                      |
| `--info` / `--info-soft`                 | `#1D5592` / `#E3ECF7` | Thông tin                                            |                      |
| `--highlight` / `--highlight-foreground` | `#BFF2CC` / `#14532D` | Ô số liệu nổi bật                                    |                      |
| `--surface-dark`                         | `#1F1A17`             | `HeroBand`, mục menu đang chọn                       |                      |
| `--on-dark` / `--on-dark-muted`          | `#FAF7F2` / `#BDB3A9` | Chữ trên mảng tối                                    |                      |
| `--ring`                                 | `#B23A1A`             | Vòng focus                                           | trên nền trang 5.4:1 |
| `--overlay`                              | `rgb(31 26 23 / 0.5)` | Lớp phủ sau hộp thoại, ngăn kéo                      |                      |

Mọi cặp chữ và nền được `src/test/contrast.test.ts` kiểm tra (chữ từ 4.5:1, viền và vòng focus từ 3:1). Đổi một màu thì chạy `pnpm test`.

Lớp `.on-dark` đặt lại `--ring` thành `--on-dark` cho mọi thứ bên trong mảng tối.

### Chữ

Be Vietnam Pro qua `next/font/google` (subset `vietnamese`, `latin`; nét 200, 400, 500, 600). Chữ nhỏ nhất 12px. Thang chữ là các lớp `type-*` trong `globals.css` (tên `type-*` để không đụng lớp màu `text-*` khi gộp class):

| Lớp                  | Cỡ / dòng                     | Nét | Dùng cho                                   |
| -------------------- | ----------------------------- | --- | ------------------------------------------ |
| `type-stat`          | 40px / 1.1 (32px dưới 640px)  | 200 | Số lớn trong `StatTile`                    |
| `type-page-title`    | 28px / 1.25 (24px dưới 640px) | 500 | `h1`, một cái mỗi trang                    |
| `type-section-title` | 22px / 1.3                    | 500 | `h2`                                       |
| `type-card-title`    | 17px / 1.4                    | 500 | `h3`                                       |
| `type-body`          | 15px / 1.6                    | 400 | Thân bài (mặc định của `body`)             |
| `type-ui`            | 14px / 1.5                    | 400 | Bảng, ô nhập, nút                          |
| `type-eyebrow`       | 13px / 1.4                    | 500 | Nhãn nhỏ trên tiêu đề mục, màu `--primary` |
| `type-caption`       | 13px / 1.5                    | 400 | Chú thích, gợi ý, màu `--muted-foreground` |

**Chữ số**: Be Vietnam Pro bản Google Fonts không có tính năng `tnum` (đã đo trên trình duyệt: `1111` và `8888` khác độ rộng dù bật `tabular-nums`). Vì vậy không dựa vào `tabular-nums`; **cột số trong bảng và số tiền luôn căn phải** để hàng đơn vị thẳng cột.

### Khoảng cách, kích thước

Thang 4px của Tailwind. Lề trang 24px từ 640px, 16px dưới 640px. Khoảng giữa các mục 40px. Đệm thẻ 20px (16px dưới 640px). Ô nhập, nút `md`, dòng bảng cao 40px; nút `sm` 32px; nút `lg` 48px. Vùng bấm tối thiểu 24 × 24px. Nội dung rộng tối đa 1440px. Ô nhập dùng chữ 16px dưới 640px để iOS không tự phóng to.

### Bo góc, bóng, chuyển động

- Bo góc `--radius: 6px` cho thẻ, ô nhập, bảng, hộp thoại, khung thông báo; `rounded-full` cho nút, chip trạng thái, mục menu đang chọn, avatar.
- Thẻ không bóng, chỉ viền `--border`. Hộp thoại, ngăn kéo, menu nổi, toast dùng một bóng duy nhất `shadow-overlay`.
- Chuyển động 150ms `ease-out` cho màu và độ mờ. Người dùng bật giảm chuyển động thì mọi transition và animation tắt hẳn (luật toàn cục trong `globals.css`); trạng thái đang tải vẫn có chữ "Đang tải" và `aria-busy`.

### Focus, link, trạng thái

- Vòng focus: viền 2px `--ring`, cách 2px, đặt chung bằng `:focus-visible` trong `globals.css`. Component không được thêm `outline-none`.
- Link luôn gạch chân, màu `--primary`, rê chuột đổi sang `--primary-hover` (màu chính so với chữ thường chỉ 2.88:1, không đủ phân biệt bằng màu).
- Vô hiệu: `opacity: 0.5`, `cursor: not-allowed`, chỉ cho thứ thật sự không dùng được. Đang tải không phải vô hiệu: dùng `loading`.

## Quy tắc dùng màu

- Màu chính `--primary` chỉ cho hành động (nút chính, link) và nhãn nhỏ trên tiêu đề mục. **Không bao giờ** dùng màu chính cho lỗi hay cảnh báo.
- Lỗi dùng `--destructive` và `--destructive-soft`, luôn kèm icon `CircleAlert` và chữ. Cảnh báo dùng `--warning`, thông tin dùng `--info`, thành công dùng `--success`.
- Trạng thái nghiệp vụ hiện bằng `StatusBadge`: chữ nói rõ trạng thái, màu chỉ hỗ trợ.
- Mảng tối (`--surface-dark`) chỉ cho `HeroBand` và mục menu đang chọn; luôn đặt lớp `.on-dark` để vòng focus sáng lên.
- Lớp phủ sau hộp thoại dùng `bg-overlay`, không dùng `bg-black/50`.
- Không lớp màu bảng có sẵn của Tailwind (`text-gray-600`), không `bg-black`, `text-white`, không mã hex hay `rgb(` trong `.tsx`. Cần màu mới thì thêm token vào `globals.css`, bảng trên và test tương phản.

## Danh mục component

Component sinh từ shadcn ở `src/components/ui/` (đã đổi sang token, chữ ẩn bằng tiếng Việt). Component ghép của dự án ở `src/components/<nhóm>/`. Mọi component xem ở `/dev/ui`; khung app xem ở `/dev/ui/shell`.

### Nút và ô nhập (`ui`, `form`)

| Component                                           | Dùng khi                             | Không dùng khi                                                   | Props chính, trạng thái                                                                                                                                                                                                                                  |
| --------------------------------------------------- | ------------------------------------ | ---------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Button`                                            | Mọi hành động                        | Điều hướng thuần (dùng `Link`, hoặc `Button asChild` bọc `Link`) | `variant`: `primary`, `secondary`, `outline`, `ghost`, `destructive`, `link`; `size`: `sm`, `md`, `lg`, `icon`, `icon-sm`; `loading` hiện vòng quay, `aria-busy`, `aria-disabled`, chặn bấm, không đặt `disabled`. Nút chỉ có icon phải có `aria-label`. |
| `Input`, `Textarea`                                 | Nhập chữ                             |                                                                  | Viền `--input`; `aria-invalid` thì viền đỏ. Luôn đặt trong `FormField`.                                                                                                                                                                                  |
| `Select`                                            | Chọn 1 trong danh sách ngắn, cố định | Danh sách dài cần tìm (combobox, chưa có)                        | Trong form dùng `SelectControl`.                                                                                                                                                                                                                         |
| `Checkbox`, `Switch`                                | Đồng ý, bật tắt                      |                                                                  | Trong form dùng `CheckboxControl`, `SwitchControl` với `layout="horizontal"`. Switch bật màu `--success`.                                                                                                                                                |
| `FormField`                                         | Mỗi ô trong form                     |                                                                  | `control`, `name`, `label`, `description`, `required`, `layout`, `render`. Nối nhãn, gợi ý, lỗi qua `aria-describedby`, đặt `aria-invalid`, `aria-required`; ô bắt buộc có `*` và chữ ẩn "(bắt buộc)".                                                   |
| `SelectControl`, `CheckboxControl`, `SwitchControl` | Ghép Radix vào `FormField`           |                                                                  | Chuyển `onValueChange`, `onCheckedChange` thành `onChange`; gắn `ref`, `onBlur` vào phần tử nhận focus.                                                                                                                                                  |
| `FormAlert`                                         | Lỗi chung của form hoặc hộp thoại    | Lỗi của một ô (đã hiện dưới ô)                                   | `error`, `focusKey`. Không tự biến mất; nhận focus khi `focusKey` đổi; lỗi hệ thống có mã tra cứu.                                                                                                                                                       |
| `SubmitButton`                                      | Nút gửi form                         |                                                                  | `form`; tự `loading` khi đang gửi.                                                                                                                                                                                                                       |
| `useApiForm`                                        | Mọi form gọi API                     |                                                                  | Xem _Form có lỗi API_ dưới đây.                                                                                                                                                                                                                          |

### Phản hồi (`feedback`, `dialogs`)

| Component       | Dùng khi                                            | Không dùng khi                    | Props chính                                                                                                                                                                                             |
| --------------- | --------------------------------------------------- | --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `notifySuccess` | Báo thành công sau một thao tác                     | Báo lỗi                           | Toast tự tắt sau 5 giây.                                                                                                                                                                                |
| `notifyInfo`    | Báo việc trang đã tự xử lý (đã tải lại dữ liệu mới) | Báo lỗi, báo thành công           | Toast tự tắt sau 5 giây.                                                                                                                                                                                |
| `notifyError`   | Lỗi của thao tác không nằm trong form hay hộp thoại | Lỗi trong form (dùng `FormAlert`) | Toast không tự tắt, nút "Đóng", mã tra cứu với lỗi hệ thống.                                                                                                                                            |
| `RequestIdNote` | Hiện mã tra cứu                                     | Lỗi 4xx                           | Nút sao chép; không sao chép được thì bôi chọn mã và nhắc Ctrl+C.                                                                                                                                       |
| `EmptyState`    | Danh sách rỗng                                      | Đang tải, lỗi                     | `title` (mặc định "Chưa có dữ liệu"), `description`, `icon`, `action`.                                                                                                                                  |
| `ErrorState`    | 404, lỗi bất ngờ, lỗi bảng, sau này 403             | Lỗi trong form                    | `title`, `error` (lấy câu và mã tra cứu), `headingLevel`, `actions`.                                                                                                                                    |
| `useApiError`   | Giữ lỗi API nơi không có form                       |                                   | `{ error, focusKey, capture, clear }`.                                                                                                                                                                  |
| `ConfirmDialog` | Xác nhận thao tác khó hoàn tác                      | Thông tin thuần (dùng `Dialog`)   | `trigger`, `title`, `description`, `confirmLabel` (động từ cụ thể), `tone`, `onConfirm` trả promise. Mở ra focus ở "Hủy"; lỗi thì giữ mở và hiện `FormAlert`; thành công thì đóng, trả focus về nút mở. |
| `Dialog`        | Nội dung phụ cần tập trung                          | Xác nhận (dùng `ConfirmDialog`)   | Giữ focus bên trong, Esc đóng, nút đóng có nhãn "Đóng".                                                                                                                                                 |
| `Alert`         | Thông báo trong trang                               | Lỗi vừa xảy ra trong form         | `tone`: `info`, `success`, `warning`, `danger`; luôn kèm icon; truyền `role="alert"` khi là lỗi vừa xảy ra.                                                                                             |

### Dữ liệu (`data`)

| Component                                | Dùng khi                                       | Props chính                                                                                                |
| ---------------------------------------- | ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `DataTable`                              | Mọi danh sách từ API                           | Xem _Bảng danh sách_ dưới đây.                                                                             |
| `StatusBadge`                            | Trạng thái nghiệp vụ                           | `tone`: `neutral`, `info`, `success`, `warning`, `danger`. Mỗi tính năng tự ánh xạ trạng thái sang `tone`. |
| `KeyValueList`                           | Chi tiết một đối tượng                         | `items`, `variant`: `striped` hoặc `divided`. Dưới 640px nhãn và giá trị xếp chồng.                        |
| `StatTile`                               | Một con số quan trọng                          | `label`, `value`, `icon`, `hint`, `variant`: `default`, `highlight`, `on-dark`.                            |
| `Card`, `Skeleton`, `Separator`, `Badge` | Khối nội dung, giữ chỗ khi tải, đường kẻ, chip | Thẻ không bóng; `Skeleton` ẩn với trình đọc màn hình, nơi dùng tự báo "Đang tải".                          |

### Bố cục (`layout`)

| Component       | Dùng khi                                              | Props chính                                                                                                                                                                                                                                     |
| --------------- | ----------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AppShell`      | Bọc mọi trang sau đăng nhập (tính năng 7 chọn layout) | `nav: NavGroup[]`, `userSlot`, `children`. Liên kết "Bỏ qua, tới nội dung chính" là điểm Tab đầu tiên; thanh bên từ 1024px, ngăn kéo dưới 1024px; mục đang chọn khớp theo đoạn đường dẫn, `href` dài nhất thắng. Dùng được trong layout server. |
| `PageHeader`    | Đầu mỗi trang, một `h1`                               | `title`, `description`, `actions`.                                                                                                                                                                                                              |
| `SectionHeader` | Đầu một mục                                           | `title`, `eyebrow`, `description`, `as` (`h2` hoặc `h3`), `actions`.                                                                                                                                                                            |
| `HeroBand`      | Đầu trang tổng quan                                   | `greeting`, `title`, `titleAs`, `subtitle`, con là các `StatTile` `on-dark`.                                                                                                                                                                    |
| `Brand`         | Thương hiệu                                           | Icon `Building2` và `APP_NAME` từ `src/lib/brand.ts`.                                                                                                                                                                                           |

Chưa có, tính năng đầu tiên cần tới tự thêm theo mục cuối: chọn ngày, combobox, tải tệp, biểu đồ, menu người dùng.

## Mẫu ghép

### Form có lỗi API

```tsx
const form = useApiForm({ schema, defaultValues });
const mutation = useMutation({ mutationFn: (values: Values) => api(client.PUT("/api/v1/...", { body: values })) });

<form onSubmit={form.submitWith((values) => mutation.mutateAsync(values))} noValidate>
  <FormAlert error={form.formError} focusKey={form.formErrorKey} />
  <FormField
    control={form.control}
    name="soDienThoai"
    label="Số điện thoại"
    required
    render={(field) => <Input type="tel" {...field} />}
  />
  <SubmitButton form={form}>Lưu</SubmitButton>
</form>;
```

- `submitWith` cần promise: dùng `mutateAsync`, không dùng `mutate`.
- Lỗi `errors[]` có `field` trùng tên ô thì hiện dưới ô đó, ô đầu tiên nhận focus; phần còn lại vào `FormAlert`, khung này nhận focus.
- Câu lỗi chung lấy từ `src/lib/api/error-messages.ts`. Lỗi 409, 422 có ý nghĩa riêng thì backend trả mã có tiền tố phân hệ (`AUTH_PHONE_TAKEN`) và tính năng thêm câu vào từ điển. Không bao giờ hiện `message` của response.
- Kiểm tra khi rời ô lần đầu, sau đó theo từng phím. Thông báo lỗi của schema Zod viết tiếng Việt có dấu.
- Thành công: `notifySuccess("Đã lưu")`.

### Bảng danh sách

```tsx
const { page, size, sort } = parseTableParams(searchParams);   // đọc từ URL
const query = useQuery({
  queryKey: ["can-ho", page, size, sort],
  queryFn: () => api(client.GET("/api/v1/can-ho", { params: { query: { page, size, sort: toSortParam(sort) } } })),
  placeholderData: keepPreviousData,
});

const col = dataTableColumns<CanHo>();                         // ngoài component, để cột ổn định
const columns = col.columns([
  col.accessor("maCanHo", { header: "Mã căn hộ", meta: { sortField: "maCanHo" } }),
  col.accessor("phiThang", { header: "Phí tháng", meta: { align: "right" }, cell: (c) => formatTien(c.getValue()) }),
  col.accessor("ghiChu", { header: "Ghi chú", meta: { description: true } }),
]);

<DataTable caption="Danh sách căn hộ" hideCaption columns={columns} query={query}
  page={page} size={size} sort={sort} onPageChange={...} onSizeChange={...} onSortChange={...} />
```

- Chỉ cột có `meta.sortField` sắp xếp được; tên đó phải nằm trong danh sách trắng của endpoint. Bấm tiêu đề xoay vòng tăng, giảm, bỏ.
- Cột số, tiền đặt `meta.align: "right"`. Cột mô tả dài đặt `meta.description: true` để cắt 2 dòng.
- DataTable tự lo: skeleton lần đầu, giữ dữ liệu cũ làm mờ khi chuyển trang, rỗng, lỗi có "Thử lại", về trang cuối khi trang vượt quá, về trang 1 khi đổi sắp xếp hay số dòng.
- `DataTable` dùng TanStack Table v9 bên trong; trang chỉ dùng `dataTableColumns` và `DataTableColumn`, không gọi thẳng `useTable`.

### Trang tổng quan

`PageHeader` (hoặc `HeroBand` với `titleAs="h1"` khi nó là đầu trang), rồi `HeroBand` chứa 2 đến 4 `StatTile` `on-dark`, rồi các mục `SectionHeader` với `Card`, `KeyValueList`, `DataTable`. Xem `/dev/ui/shell`.

### Hộp thoại xác nhận

```tsx
<ConfirmDialog
  trigger={<Button variant="destructive">Khóa tài khoản</Button>}
  title="Khóa tài khoản này?"
  description="Người dùng sẽ không đăng nhập được cho tới khi được mở khóa."
  confirmLabel="Khóa tài khoản"
  tone="destructive"
  onConfirm={async () => {
    await khoa.mutateAsync(id);
    notifySuccess("Đã khóa tài khoản");
  }}
/>
```

## Định dạng số, tiền, ngày

Mọi hàm ở `src/lib/format.ts`; giá trị `null`, `undefined` hoặc không hợp lệ cho ra `—`. Không tự gọi `toLocaleString` hay `new Date()` trong component.

| Hàm             | Vào                        | Ra                                              |
| --------------- | -------------------------- | ----------------------------------------------- |
| `formatTien`    | `4850000`                  | `4.850.000 ₫` (NBSP trước `₫`)                  |
| `formatSo`      | `135000`                   | `135.000`                                       |
| `formatNgay`    | `"2026-11-16"` (LocalDate) | `16/11/2026`, tách chuỗi, không qua `Date`      |
| `formatNgayGio` | `"2026-11-16T07:05:00Z"`   | `16/11/2026 14:05`, luôn giờ `Asia/Ho_Chi_Minh` |

Tham số bảng trên URL đọc bằng `parseTableParams` (trang sai thì 1, số dòng ngoài 10, 20, 50 thì 20).

## Cách thêm một component mới

1. Sinh bằng shadcn: `pnpm dlx shadcn@latest add <ten>`. Nếu CLI hỏi ghi đè `button.tsx` hay file đã chỉnh, trả lời không (các file trong `src/components/ui/` đã được sửa tay).
2. Đổi sang token: bỏ `outline-none`, `focus-visible:ring-*` (vòng focus đã có chung), bỏ lớp `dark:`, đổi `rounded-lg`, `rounded-xl` thành `rounded-md`, đổi `bg-black/50` thành `bg-overlay`, ô nhập cao 40px với `text-base sm:text-sm`.
3. Dịch chữ ẩn cho trình đọc màn hình sang tiếng Việt ("Close" thành "Đóng").
4. Thêm vào `/dev/ui` ở mọi trạng thái áp dụng được (mặc định, vô hiệu, đang tải, lỗi, rỗng).
5. Viết test Vitest có `expectNoA11yViolations` ở trạng thái chính và trạng thái lỗi.
6. Cập nhật danh mục ở file này.
