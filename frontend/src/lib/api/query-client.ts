import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "./api";

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

/** QueryClient của app. Cấu hình chung, tính năng không tự đặt lại `retry`. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { staleTime: 30_000, retry: shouldRetryQuery },
      mutations: { retry: false },
    },
  });
}
