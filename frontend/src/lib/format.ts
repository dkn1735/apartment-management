/**
 * Định dạng số, tiền, ngày theo vi-VN và múi giờ Asia/Ho_Chi_Minh, bất kể múi giờ máy người dùng (spec 0003, AC-11).
 * Giá trị null, undefined hoặc không hợp lệ cho ra "—".
 */

export const EMPTY_VALUE = "—";
const TIME_ZONE = "Asia/Ho_Chi_Minh";
const LOCAL_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

const tienFormat = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });
const soFormat = new Intl.NumberFormat("vi-VN");
const ngayGioFormat = new Intl.DateTimeFormat("vi-VN", {
  timeZone: TIME_ZONE,
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
});

function isValidNumber(n: unknown): n is number {
  return typeof n === "number" && Number.isFinite(n);
}

/** `4850000` thành `4.850.000 ₫` (khoảng trắng trước ₫ là NBSP do Intl sinh). */
export function formatTien(n: number | null | undefined): string {
  return isValidNumber(n) ? tienFormat.format(n) : EMPTY_VALUE;
}

/** `135000` thành `135.000`. */
export function formatSo(n: number | null | undefined): string {
  return isValidNumber(n) ? soFormat.format(n) : EMPTY_VALUE;
}

function parts(value: string): Record<string, string> | null {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return Object.fromEntries(ngayGioFormat.formatToParts(date).map((part) => [part.type, part.value]));
}

/** Thời điểm ISO (có `Z` hoặc độ lệch như `+07:00`) thành `dd/MM/yyyy HH:mm` giờ Việt Nam. */
export function formatNgayGio(value: string | null | undefined): string {
  if (!value || LOCAL_DATE.test(value)) return EMPTY_VALUE;
  const p = parts(value);
  return p ? `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}` : EMPTY_VALUE;
}

/** Thời điểm ISO thành `HH:mm dd/MM/yyyy` giờ Việt Nam, cho câu kiểu "đến 08:15 16/11/2026" (spec 0009, AC-14). */
export function formatGioNgay(value: string | null | undefined): string {
  if (!value || LOCAL_DATE.test(value)) return EMPTY_VALUE;
  const p = parts(value);
  return p ? `${p.hour}:${p.minute} ${p.day}/${p.month}/${p.year}` : EMPTY_VALUE;
}

/** Thời điểm ISO thành `HH:mm ngày dd/MM/yyyy` giờ Việt Nam (spec 0010, hạn của link đặt lại mật khẩu). */
export function formatGioNgayDayDu(value: string | null | undefined): string {
  if (!value || LOCAL_DATE.test(value)) return EMPTY_VALUE;
  const p = parts(value);
  return p ? `${p.hour}:${p.minute} ngày ${p.day}/${p.month}/${p.year}` : EMPTY_VALUE;
}

/**
 * `2026-11-16` (LocalDate) thành `16/11/2026` bằng cách tách chuỗi, không qua `Date` nên không lệch ngày.
 * Chuỗi ISO khác là thời điểm: lấy phần ngày theo giờ Việt Nam.
 */
export function formatNgay(value: string | null | undefined): string {
  if (!value) return EMPTY_VALUE;
  const local = LOCAL_DATE.exec(value);
  if (local) {
    const [, year, month, day] = local;
    const check = new Date(Date.UTC(+year, +month - 1, +day));
    const valid = check.getUTCFullYear() === +year && check.getUTCMonth() === +month - 1 && check.getUTCDate() === +day;
    return valid ? `${day}/${month}/${year}` : EMPTY_VALUE;
  }
  const p = parts(value);
  return p ? `${p.day}/${p.month}/${p.year}` : EMPTY_VALUE;
}
