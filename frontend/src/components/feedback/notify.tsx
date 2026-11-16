"use client";

import { toast } from "sonner";
import { ApiError } from "@/lib/api/api";
import { messageFor, requestIdToShow } from "@/lib/api/error-messages";
import { RequestIdNote } from "./request-id-note";

/** Báo thành công: toast tự tắt sau 5 giây, trình đọc màn hình đọc lên (AC-8). */
export function notifySuccess(message: string, description?: string) {
  return toast.success(message, { description });
}

/**
 * Báo lỗi của thao tác không nằm trong form hay hộp thoại: toast không tự tắt, có nút "Đóng" (AC-8),
 * kèm mã tra cứu với lỗi hệ thống (AC-7). Lỗi không phải ApiError dùng câu chung của lỗi hệ thống.
 */
export function notifyError(error: unknown) {
  const apiError = error instanceof ApiError ? error : new ApiError({ status: 0, errorCode: "", message: "" });
  const requestId = requestIdToShow(apiError);
  return toast.error(messageFor(apiError), {
    duration: Infinity,
    description: requestId ? <RequestIdNote requestId={requestId} /> : undefined,
    action: { label: "Đóng", onClick: () => {} },
  });
}
