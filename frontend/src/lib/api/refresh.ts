import type { Middleware } from "openapi-fetch";
import { CSRF_HEADER, CSRF_HEADER_VALUE } from "./csrf";

/**
 * Làm mới phiên âm thầm khi gặp 401 (spec 0005, AC-10, AC-14, AC-17). Lời gọi làm mới không
 * đi qua `client`, nên không bao giờ tự kích hoạt chính middleware này. Mọi response 200 của
 * làm mới đều đặt cookie (kể cả `lamMoi` false), nên gửi lại ngay, không chờ.
 */

export const DUONG_LAM_MOI = "/api/v1/auth/refresh-token";

/** Ba đường auth không bao giờ kích hoạt làm mới khi trả 401. */
const DUONG_KHONG_LAM_MOI = new Set(["/api/v1/auth/login", DUONG_LAM_MOI, "/api/v1/auth/logout"]);

export type KetQuaLamMoi = { ok: true } | { ok: false; response: Response };

type FetchFn = (input: Request) => Promise<Response>;

// Mọi request cùng gặp 401 trong một tab chờ chung một lời gọi làm mới
let dangChay: Promise<KetQuaLamMoi> | null = null;
let dangXuat = false;

/**
 * Gọi làm mới, hoặc nhập vào lời gọi đang chạy. `ok: false` mang response lỗi (chưa đọc body);
 * mất mạng thì promise bị từ chối.
 */
export function lamMoiPhien(fetchFn: FetchFn, base: string): Promise<KetQuaLamMoi> {
  if (dangChay) {
    return dangChay;
  }
  const request = new Request(new URL(DUONG_LAM_MOI, base), {
    method: "POST",
    headers: { [CSRF_HEADER]: CSRF_HEADER_VALUE },
    credentials: "same-origin",
    // Trang tải lại hay điều hướng không hủy lời gọi, để `Set-Cookie` vẫn tới. Chỉ là lớp phụ:
    // trình duyệt bỏ qua cờ này thì nhánh khoan dung ở server vẫn giữ người dùng đăng nhập
    keepalive: true,
  });
  const lan = (async (): Promise<KetQuaLamMoi> => {
    const response = await fetchFn(request);
    return response.ok ? { ok: true } : { ok: false, response };
  })();
  const chung = lan.finally(() => {
    dangChay = null;
  });
  dangChay = chung;
  return chung;
}

/** Lời gọi làm mới đang chạy, nếu có; đăng xuất chờ nó xong trước khi gọi `logout`. */
export function dangLamMoi(): Promise<KetQuaLamMoi> | null {
  return dangChay;
}

/** Từ lúc bấm đăng xuất: không bắt đầu lần làm mới mới nào, không chuyển trang `het-phien`. */
export function batDauDangXuat(): void {
  dangXuat = true;
}

export function ketThucDangXuat(): void {
  dangXuat = false;
}

export function dangDangXuat(): boolean {
  return dangXuat;
}

/** Cho test đặt lại trạng thái cấp module. */
export function resetRefreshState(): void {
  dangChay = null;
  dangXuat = false;
  banSao.clear();
}

async function laUnauthenticated(response: Response): Promise<boolean> {
  if (response.status !== 401) {
    return false;
  }
  try {
    const body = (await response.clone().json()) as { errorCode?: unknown };
    return body.errorCode === "UNAUTHENTICATED";
  } catch {
    return false;
  }
}

// Body của request ghi bị đọc khi gửi đi, nên chép trước theo id của middleware
const banSao = new Map<string, Request>();

/** Bản mới để gửi lại; bản gốc giữ nguyên để còn chép tiếp. */
function guiLai(goc: Request, fetchFn: FetchFn): Promise<Response> {
  return fetchFn(goc.method === "GET" || goc.method === "HEAD" ? new Request(goc) : goc.clone());
}

/** Gặp 401 `UNAUTHENTICATED` thì làm mới một lần rồi gửi lại đúng request ban đầu một lần. */
export const refreshMiddleware: Middleware = {
  onRequest({ request, id }) {
    if (request.method !== "GET" && request.method !== "HEAD") {
      banSao.set(id, request.clone());
    }
    return undefined;
  },
  async onResponse({ request, response, options, id, schemaPath }) {
    const goc = banSao.get(id) ?? request;
    banSao.delete(id);
    if (response.status !== 401 || DUONG_KHONG_LAM_MOI.has(schemaPath) || dangXuat) {
      return undefined;
    }
    if (!(await laUnauthenticated(response))) {
      return undefined;
    }
    const fetchFn = options.fetch;
    const ketQua = await lamMoiPhien(fetchFn, request.url);
    if (!ketQua.ok) {
      // Mỗi nơi chờ nhận một bản riêng, để `api` đọc được body lỗi
      return ketQua.response.clone();
    }
    // Lần gửi lại đi thẳng qua `fetchFn`, không qua middleware: vẫn 401 thì `handleAuthError`
    // chuyển `het-phien`, không làm mới lần hai
    return guiLai(goc, fetchFn);
  },
  onError({ id }) {
    banSao.delete(id);
    return undefined;
  },
};
