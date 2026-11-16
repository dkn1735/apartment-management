"use client";

import { useCallback, useState } from "react";
import { ApiError } from "@/lib/api/api";

/**
 * Giữ lỗi API cho nơi không có form (ví dụ ConfirmDialog), dùng cùng FormAlert.
 * `capture(fn)` chạy `fn`; bắt ApiError thì đặt `error` và tăng `focusKey`, trả `false`. Lỗi khác ném tiếp.
 */
export function useApiError() {
  const [error, setError] = useState<ApiError | null>(null);
  const [focusKey, setFocusKey] = useState(0);

  const capture = useCallback(async (fn: () => Promise<unknown>): Promise<boolean> => {
    setError(null);
    try {
      await fn();
      return true;
    } catch (caught) {
      if (!(caught instanceof ApiError)) {
        throw caught;
      }
      setError(caught);
      setFocusKey((key) => key + 1);
      return false;
    }
  }, []);

  const clear = useCallback(() => setError(null), []);

  return { error, focusKey, capture, clear };
}
