# 0003. Design system và nền tảng giao diện: lý do và các phương án

Phần build spec ở [index.md](index.md). File này là hồ sơ quyết định, `/develop` không cần đọc.

## Context

Bộ khung frontend (spec 0001) đã có Next.js 16, Tailwind CSS v4 và quyết định dùng shadcn/ui, nhưng chưa có gì về hình ảnh: `globals.css` chỉ có `@import "tailwindcss"`, chưa khởi tạo shadcn, chưa có component nào, chưa có `design.md`. Chín slice của đợt này (đăng nhập, hồ sơ, bố cục theo vai trò, nhật ký kiểm toán, quản lý tài khoản...) đều cần form, bảng, thông báo lỗi và khung trang. Nếu mỗi slice tự dựng, mười hai phân hệ sau sẽ có mười hai kiểu nút và ba cách báo lỗi.

Các lực định hình quyết định:

- **Trợ năng là yêu cầu cứng** (`AGENTS.md`): WCAG AA, dùng được hoàn toàn bằng bàn phím, focus nhìn thấy được, mọi ô nhập có nhãn, lỗi gắn với ô nhập, đủ tương phản. Những thứ này rẻ khi nằm trong component nền và rất đắt khi sửa từng màn hình.
- **Tiếng Việt có dấu**: chữ xếp chồng dấu (ẳ, ỡ, Ữ) cần font vẽ đủ glyph và khoảng dòng đủ cao, nếu không dấu bị cắt hoặc rơi về font hệ thống.
- **Người dùng khác nhau**: nhân viên BQL, kế toán xem nhiều dòng trên máy tính; cư dân dùng điện thoại. Giao diện phải gọn cho bảng mà vẫn dùng được ở 360px.
- **Hợp đồng lỗi đã chốt** (spec 0001): mọi lỗi có `errorCode`, `errors[]` theo trường và `requestId`; frontend phải xử lý theo `errorCode`, không theo `message`. Cần một chỗ dùng chung biến điều này thành giao diện.
- **Nhóm hai người, đồ án có buổi bảo vệ**: không đủ sức duy trì công cụ phụ; cần một hướng hình ảnh có cá tính để trình diễn. Nhóm đã có một ảnh mẫu tham khảo (dashboard căn hộ cao cấp, nền ngà ấm, nhấn đỏ gạch).

Không quyết định thì tính năng 5 sẽ tự chọn màu, font và cách báo lỗi, rồi tính năng 7 và 11 phải theo hoặc sửa lại.

## Options considered

### Option 1: shadcn/ui phủ token ngữ nghĩa lấy từ ảnh mẫu, cộng lớp component ghép và `design.md`

Khởi tạo shadcn/ui (Radix bên dưới), thay toàn bộ biến màu bằng bảng token rút từ ảnh mẫu, thêm một lớp component ghép riêng của dự án (trường form gắn lỗi API, bảng phân trang phía server, khung app, ô số liệu), mô tả tất cả trong `frontend/design.md`, trưng bày ở `/dev/ui`.

**Pros**:
- Khớp spec 0001, không thêm hệ component thứ hai; Radix lo focus, bàn phím, ARIA cho dialog, select, switch.
- Code component nằm trong repo nên chỉnh được theo ảnh mẫu tới từng chi tiết.
- Lớp ghép gom các quy tắc của dự án (lỗi theo `errorCode`, phân trang `page` từ 1) vào một chỗ.

**Cons**:
- Tự chịu trách nhiệm bảo trì component đã sinh; cập nhật shadcn là việc tay.
- Thiếu sẵn bảng dữ liệu và chọn ngày, phải ghép TanStack Table và thêm sau.

### Option 2: shadcn/ui với theme mặc định (stone), không theo ảnh mẫu

Chạy `shadcn init`, giữ màu và kiểu mặc định, chỉ thêm component ghép.

**Pros**:
- Nhanh nhất, ít quyết định nhất.

**Cons**:
- Trông như mọi app shadcn khác; mất hướng hình ảnh nhóm đã chọn cho buổi bảo vệ.
- Màu `destructive` mặc định gần màu nhấn nếu sau này đổi sang đỏ gạch; vẫn phải làm bảng tương phản.

### Option 3: Thư viện component trọn gói (Mantine hoặc MUI)

Dùng một thư viện có sẵn bảng, chọn ngày, form, theme.

**Pros**:
- Nhiều component sẵn, kể cả chọn ngày và bảng nâng cao; trợ năng tốt.

**Cons**:
- Mâu thuẫn spec 0001 (Tailwind + shadcn) và chồng hai hệ style.
- Theme theo ảnh mẫu khó hơn vì kiểu dáng do thư viện quyết định; bundle nặng hơn.

### Option 4: Như Option 1 nhưng trưng bày bằng Storybook

**Pros**:
- Chuẩn ngành, tách biệt, có addon trợ năng.

**Cons**:
- Thêm một bộ công cụ phải cấu hình với Next.js 16, Turbopack và Tailwind v4, và phải giữ nó chạy trong CI; nặng với nhóm hai người.

## Rationale

Option 1 là cách duy nhất đáp ứng cùng lúc ba lực: giữ stack spec 0001, đạt WCAG AA bằng Radix thay vì tự viết, và cho nhóm một giao diện có cá tính theo ảnh mẫu. Option 2 rẻ hơn một chút nhưng bỏ đúng thứ nhóm muốn trình diễn. Option 3 cho nhiều component hơn nhưng phá quyết định đã chốt và trả giá bằng hai hệ style. Storybook (Option 4) giải một vấn đề nhóm chưa có: trang `/dev/ui` ngay trong app đủ để nhìn và kiểm tra, còn axe trong Vitest lo phần tự động.

Các lựa chọn nhỏ đi theo cùng logic "ít thứ phải vận hành": lucide và Sonner là mặc định của shadcn; TanStack Table cùng họ với TanStack Query đã có; Be Vietnam Pro được vẽ cho tiếng Việt và có nét 200 để giữ chất số lớn mảnh của ảnh mẫu.

Hai điều chỉnh so với ảnh mẫu, có chủ đích:
- **Màu lỗi tách khỏi màu chính.** Màu chính là đỏ gạch, nên lỗi dùng đỏ thẫm ngả hồng và luôn đi kèm icon cùng chữ. Thiếu điều này người dùng không phân biệt được nút "Lưu" với dòng báo lỗi.
- **Thanh bên thay menu ngang.** Ảnh mẫu là app cư dân với 7 mục; BQL ở đây sẽ có hơn chục phân hệ. Kiểu viên thuốc tối của ảnh được giữ cho mục đang chọn.

### Các lựa chọn nhỏ mình tự quyết (người làm có thể đổi)

| Việc | Chọn | Lý do | Phương án thứ hai |
|---|---|---|---|
| Kiểu màu trong CSS | Hex trong biến CSS | Bảng tương phản trong `design.md` kiểm được bằng tay, khớp số liệu đã tính | OKLCH như mặc định shadcn mới |
| Chế độ dark của Tailwind | Giữ `@custom-variant dark (&:is(.dark *))` | Lớp `dark:` trong component shadcn chỉ bật khi có class `.dark`, không tự bật theo hệ điều hành làm vỡ giao diện | Xóa hết lớp `dark:` (tốn công, mất sẵn sàng cho dark) |
| Lỗi của thao tác không có form | Toast lỗi không tự tắt, có nút đóng | Không biến mất trước khi đọc, vẫn đúng ý "lỗi không tự biến mất" | Bắt buộc mọi thao tác đi qua form hoặc hộp thoại |
| Khung lỗi đầu form | `tabIndex={-1}` và nhận focus, không đặt `role="alert"` | Tránh trình đọc màn hình đọc hai lần | `role="alert"` không chuyển focus |
| Định dạng ngày giờ | Ghép từ `formatToParts` | `vi-VN` của `Intl` đặt giờ trước ngày (`14:05 16/11/2026`), khác dạng đã chọn | Thư viện `date-fns` (thêm phụ thuộc) |
| Chống lẫn màu thô | Test Vitest quét class màu Tailwind và mã hex trong component | Không cần plugin ESLint (plugin Tailwind cho v4 chưa ổn định) | Quy tắc ESLint tự viết |
| Môi trường test | jsdom | axe-core chạy ổn định trên jsdom | happy-dom (nhanh hơn, axe kém ổn định hơn) |
| Gọi axe | Helper nhỏ gọi thẳng `axe-core` | `vitest-axe` là bản tách ít được bảo trì của `jest-axe` | `vitest-axe` |
| Nút đang tải | `aria-disabled` cộng chặn trong handler | `disabled` làm focus rơi về `body` giữa lúc gửi, người dùng bàn phím mất chỗ | `disabled` (đơn giản hơn) |
| Giảm chuyển động | Một luật CSS toàn cục | Phủ cả animation của `tw-animate-css` mà lớp `motion-reduce:` riêng lẻ bỏ sót | Lớp `motion-reduce:` trên từng component |
| Thử lại truy vấn | Không thử với 4xx, 1 lần với 5xx | Mặc định 3 lần làm trạng thái lỗi hiện sau khoảng 7 giây | Mặc định của TanStack Query |
| Chặn `/dev/*` | Ở `dev/layout.tsx` | Một chỗ phủ mọi trang mẫu, gồm `/dev/ui/shell` | Ở từng trang |

### Kiểm tra chéo

Bản nháp đầu được một model khác đọc độc lập. Nó tìm ra các chỗ người build sẽ phải tự đoán (hợp đồng form và hộp thoại, quy tắc sắp xếp và phân trang của bảng, câu từ điển lỗi, định dạng giá trị rỗng, phạm vi test màu thô, số đo các component ghép, cách so khớp menu, hạ tầng test) và vài điểm kỹ thuật (`disabled` làm mất focus, iOS phóng to ô 14px, khung cuộn ngang phải focus được, `global-error.tsx`, thử lại truy vấn). Nhóm chọn áp dụng toàn bộ cách sửa đề xuất; spec hiện tại đã gồm các sửa đó.

### Bảng tương phản đã tính

Tính theo công thức độ sáng tương đối của WCAG 2.x. Chữ thường cần từ 4.5:1, viền thành phần giao diện và vòng focus cần từ 3:1.

| Cặp | Tiền cảnh | Nền | Tỉ lệ |
|---|---|---|---|
| Chữ chính trên nền trang | `#1F1A17` | `#F5F2ED` | 15.44 |
| Chữ chính trên nền phụ | `#1F1A17` | `#ECE7E0` | 14.01 |
| Chữ phụ trên nền trang | `#655C55` | `#F5F2ED` | 5.85 |
| Chữ phụ trên nền phụ | `#655C55` | `#ECE7E0` | 5.31 |
| Chữ trắng trên nút chính | `#FFFFFF` | `#B23A1A` | 5.98 |
| Chữ trắng trên nút chính khi rê chuột | `#FFFFFF` | `#93300F` | 7.86 |
| Màu chính trên nền trang | `#B23A1A` | `#F5F2ED` | 5.35 |
| Màu chính trên nền phụ | `#B23A1A` | `#ECE7E0` | 4.86 |
| Lỗi trên nền thẻ | `#B0123B` | `#FFFDFA` | 6.90 |
| Chữ trắng trên nút nguy hiểm | `#FFFFFF` | `#B0123B` | 7.01 |
| Lỗi trên nền lỗi nhạt | `#B0123B` | `#FCE7EC` | 5.94 |
| Thành công trên nền nhạt | `#1B6B37` | `#DDF3E2` | 5.61 |
| Cảnh báo trên nền nhạt | `#8A4A00` | `#FCEAD3` | 5.83 |
| Thông tin trên nền nhạt | `#1D5592` | `#E3ECF7` | 6.36 |
| Viền ô nhập trên nền thẻ | `#8C8279` | `#FFFDFA` | 3.70 |
| Viền ô nhập trên nền trang | `#8C8279` | `#F5F2ED` | 3.37 |
| Vòng focus trên nền trang | `#B23A1A` | `#F5F2ED` | 5.35 |
| Chữ trên mảng tối | `#FAF7F2` | `#1F1A17` | 16.13 |
| Chữ phụ trên mảng tối | `#BDB3A9` | `#1F1A17` | 8.36 |
| Chữ trên ô nhấn xanh bạc hà | `#14532D` | `#BFF2CC` | 7.28 |
| Chữ trên mục menu đang chọn (`--on-dark`) | `#FAF7F2` | `#1F1A17` | 16.13 |
| Chữ chính trên nền rê chuột (`--accent`) | `#1F1A17` | `#E3DCD3` | 12.68 |
| Chữ phụ trên nền rê chuột | `#655C55` | `#E3DCD3` | 4.80 |
| Chữ trắng trên nút nguy hiểm khi rê chuột | `#FFFFFF` | `#8E0E30` | 9.31 |
| Màu chính trên nền chính nhạt | `#B23A1A` | `#F6E3DC` | 4.82 |
| Chữ phụ trên nền chính nhạt | `#655C55` | `#F6E3DC` | 5.27 |
| Chữ phụ trên nền lỗi nhạt | `#655C55` | `#FCE7EC` | 5.53 |
| Viền ô nhập trên nền phụ | `#8C8279` | `#ECE7E0` | 3.06 |
| Vòng focus trên nền thẻ | `#B23A1A` | `#FFFDFA` | 5.89 |
| Vòng focus trên nền phụ | `#B23A1A` | `#ECE7E0` | 4.86 |
| Link khi rê chuột trên nền trang | `#93300F` | `#F5F2ED` | 7.04 |

Link màu chính so với chữ thường chỉ đạt 2.88:1, dưới mức 3:1 để phân biệt chỉ bằng màu (WCAG 1.4.1), nên link luôn gạch chân. `--accent` so với `--muted` chỉ 1.11:1; chấp nhận được vì rê chuột chỉ là gợi ý, không mang thông tin.

### Ảnh mẫu

Nguồn hướng hình ảnh là một ảnh chụp dashboard căn hộ do nhóm cung cấp (`mauthamkhao.webp`, không commit vào repo). Chỉ lấy phong cách: nền ngà ấm, màu nhấn đỏ gạch, nhãn nhỏ màu nhấn trên tiêu đề mục, số lớn nét mảnh, thẻ bo 6px viền mảnh, nút viên thuốc, hàng nhãn và giá trị, mảng tối ở đầu trang tổng quan. Không lấy tên, logo, ảnh hay nội dung của sản phẩm trong ảnh.
