import { MutationCache, QueryCache, QueryClient } from "@tanstack/react-query";
import { safeNext, TRANG_DANG_NHAP } from "@/lib/auth/safe-next";
import { ApiError } from "./api";

/** Đặt `meta: { [BO_QUA_CHUYEN_TRANG]: true }` cho mutation tự xử lý lỗi xác thực (form đăng nhập). */
export const BO_QUA_CHUYEN_TRANG = "boQuaChuyenTrang";

/**
 * Chính sách thử lại chung (spec 0003, AC-19): lỗi 4xx là lỗi của yêu cầu nên không thử lại;
 * 5xx và mất kết nối thử lại đúng 1 lần để trạng thái lỗi hiện sau vài giây.
 */
export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < 1;
}

/** Chỗ chuyển trang; test thay bằng `vi.spyOn(authNavigation, "assign")` vì jsdom khóa `window.location`. */
export const authNavigation = {
  assign(url: string) {
    window.location.assign(url);
  },
};

// Nhiều query lỗi cùng lúc chỉ chuyển trang một lần
let dangChuyenTrang = false;

/** Cho test đặt lại cờ chuyển trang. */
export function resetAuthRedirect() {
  dangChuyenTrang = false;
}

function laTrangKhongChuyen(pathname: string): boolean {
  return (
    pathname === TRANG_DANG_NHAP ||
    pathname.startsWith(`${TRANG_DANG_NHAP}/`) ||
    pathname === "/dev" ||
    pathname.startsWith("/dev/")
  );
}

/**
 * Lỗi xác thực ở bất kỳ lời gọi API nào thì về trang đăng nhập (spec 0004, AC-15):
 * 401 `UNAUTHENTICATED` kèm `next` và `ly-do=het-phien`; 403 `AUTH_ACCOUNT_LOCKED` kèm `ly-do=khoa`.
 */
export function handleAuthError(error: unknown, boQua = false): void {
  if (boQua || dangChuyenTrang || typeof window === "undefined" || !(error instanceof ApiError)) {
    return;
  }
  const { pathname, search } = window.location;
  if (laTrangKhongChuyen(pathname)) {
    return;
  }
  let dich: string | null = null;
  if (error.errorCode === "UNAUTHENTICATED") {
    const next = encodeURIComponent(safeNext(pathname + search));
    dich = `${TRANG_DANG_NHAP}?next=${next}&ly-do=het-phien`;
  } else if (error.errorCode === "AUTH_ACCOUNT_LOCKED") {
    dich = `${TRANG_DANG_NHAP}?ly-do=khoa`;
  }
  if (dich) {
    dangChuyenTrang = true;
    // Tải lại hẳn trang: bỏ mọi dữ liệu cache của phiên cũ
    authNavigation.assign(dich);
  }
}

/** QueryClient của app. Cấu hình chung, tính năng không tự đặt lại `retry`. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({ onError: (error) => handleAuthError(error) }),
    mutationCache: new MutationCache({
      onError: (error, _variables, _context, mutation) =>
        handleAuthError(error, mutation.meta?.[BO_QUA_CHUYEN_TRANG] === true),
    }),
    defaultOptions: {
      queries: { staleTime: 30_000, retry: shouldRetryQuery },
      mutations: { retry: false },
    },
  });
}
