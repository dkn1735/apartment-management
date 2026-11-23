import createClient, { type Middleware } from "openapi-fetch";
import { CSRF_HEADER, CSRF_HEADER_VALUE } from "./csrf";
import { refreshMiddleware } from "./refresh";
import type { components, paths } from "./schema";

/** Vỏ bọc lỗi chung của backend (spec 0001). */
export type ErrorResponse = components["schemas"]["ErrorResponse"];
export type FieldError = components["schemas"]["FieldError"];

/** Lỗi ném ra từ mọi lời gọi API; TanStack Query nhận nó qua queryFn và mutationFn. */
export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string;
  readonly errors: FieldError[];
  readonly requestId: string | undefined;
  /** Số giây từ header `Retry-After` (spec 0009, AC-15); chỉ có khi header là số nguyên dương. */
  readonly retryAfter: number | undefined;

  constructor(init: {
    status: number;
    errorCode: string;
    message: string;
    errors?: FieldError[];
    requestId?: string;
    retryAfter?: number;
  }) {
    super(init.message);
    this.name = "ApiError";
    this.status = init.status;
    this.errorCode = init.errorCode;
    this.errors = init.errors ?? [];
    this.requestId = init.requestId;
    this.retryAfter = init.retryAfter;
  }
}

/** `Retry-After` dạng số giây nguyên dương thành số; dạng ngày HTTP, thiếu hay không phải số thì `undefined`. */
export function docRetryAfter(value: string | null): number | undefined {
  if (value === null || !/^\d+$/.test(value.trim())) return undefined;
  const giay = Number(value.trim());
  return Number.isSafeInteger(giay) && giay > 0 ? giay : undefined;
}

/** Client openapi-fetch có kiểu sinh từ docs/api/openapi.json. Gọi cùng tên miền, nên baseUrl rỗng. */
export const client = createClient<paths>({ baseUrl: "" });

export { CSRF_HEADER, CSRF_HEADER_VALUE };

/** Gắn header chống CSRF cho mọi request của client, để không màn hình nào phải tự thêm. */
export const csrfHeaderMiddleware: Middleware = {
  onRequest({ request }) {
    request.headers.set(CSRF_HEADER, CSRF_HEADER_VALUE);
    return request;
  },
};

// Làm mới phiên âm thầm khi gặp 401 (spec 0005, AC-10); mọi chỗ gọi api(...) giữ nguyên
client.use(csrfHeaderMiddleware, refreshMiddleware);

type Envelope<T> = { success?: boolean; data?: T };

type FetchResult<T> = {
  data?: T;
  error?: unknown;
  response: Response;
};

function isErrorEnvelope(value: unknown): value is ErrorResponse {
  return (
    typeof value === "object" &&
    value !== null &&
    "errorCode" in value &&
    typeof (value as { errorCode: unknown }).errorCode === "string"
  );
}

/**
 * Hàm gọi API duy nhất: thành công thì trả thẳng `data` bên trong vỏ bọc,
 * lỗi thì ném {@link ApiError}. Body lỗi không phải vỏ bọc JSON (backend sập,
 * proxy trả 502/504) thành `BACKEND_UNAVAILABLE`.
 *
 * Ví dụ: `useQuery({ queryKey: ["ho-so"], queryFn: () => api(client.GET("/api/v1/auth/me")) })`
 */
export async function api<T>(request: Promise<FetchResult<Envelope<T>>>): Promise<T> {
  let result: FetchResult<Envelope<T>>;
  try {
    result = await request;
  } catch {
    throw backendUnavailable(0);
  }

  const { response } = result;
  if (response.ok) {
    // 204 không có body
    return result.data?.data as T;
  }

  if (isErrorEnvelope(result.error)) {
    const body = result.error;
    throw new ApiError({
      status: response.status,
      errorCode: body.errorCode ?? "INTERNAL_ERROR",
      message: body.message ?? "Hệ thống gặp lỗi, vui lòng thử lại sau",
      errors: body.errors,
      requestId: body.requestId ?? response.headers.get("X-Request-Id") ?? undefined,
      retryAfter: docRetryAfter(response.headers.get("Retry-After")),
    });
  }
  throw backendUnavailable(response.status, response.headers.get("X-Request-Id") ?? undefined);
}

function backendUnavailable(status: number, requestId?: string): ApiError {
  return new ApiError({
    status,
    errorCode: "BACKEND_UNAVAILABLE",
    message: "Không kết nối được tới máy chủ, vui lòng thử lại sau",
    requestId,
  });
}
