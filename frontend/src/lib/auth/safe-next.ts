/** Trang mặc định sau khi đăng nhập. */
export const TRANG_MAC_DINH = "/ho-so";

/** Trang đăng nhập; không bao giờ là đích của `next`. */
export const TRANG_DANG_NHAP = "/dang-nhap";

// Ký tự điều khiển C0 và DEL: trình duyệt bỏ qua chúng trong URL, nên "/\t/evil.com" thành "//evil.com"
const CONTROL_CHARS = /[\u0000-\u001f\u007f]/;

/**
 * Đích chuyển tới sau khi đăng nhập (spec 0004, Quy tắc next), dùng chung cho `proxy.ts` và trang đăng nhập.
 * Chỉ nhận đường dẫn nội bộ: bắt đầu bằng `/`, không phải `//` hay `/\` (trình duyệt hiểu là tên miền khác),
 * không có ký tự điều khiển, không trỏ về trang đăng nhập. Còn lại trả `/ho-so`.
 */
export function safeNext(value: string | null | undefined): string {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.startsWith("/\\")) {
    return TRANG_MAC_DINH;
  }
  if (CONTROL_CHARS.test(value)) {
    return TRANG_MAC_DINH;
  }
  const path = value.split(/[?#]/, 1)[0];
  if (path === TRANG_DANG_NHAP || path.startsWith(`${TRANG_DANG_NHAP}/`)) {
    return TRANG_MAC_DINH;
  }
  return value;
}
