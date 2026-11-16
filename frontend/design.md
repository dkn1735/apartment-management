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

<!-- Các mục quy tắc dùng màu, danh mục component, mẫu ghép, định dạng và cách thêm component được hoàn thiện ở mốc cuối của tính năng 4. -->
