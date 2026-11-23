import { safeNext } from "./safe-next";

/** Trang đổi mật khẩu; là trang duy nhất mở được khi đang dùng mật khẩu tạm (spec 0008, AC-14). */
export const TRANG_DOI_MAT_KHAU = "/doi-mat-khau";

/** Đường tới trang đổi mật khẩu, giữ `next` đã qua `safeNext` để đổi xong quay lại đúng chỗ. */
export function duongDoiMatKhau(next: string | null | undefined): string {
  const dich = safeNext(next);
  const path = dich.split(/[?#]/, 1)[0];
  if (path === TRANG_DOI_MAT_KHAU) return TRANG_DOI_MAT_KHAU;
  return `${TRANG_DOI_MAT_KHAU}?next=${encodeURIComponent(dich)}`;
}
