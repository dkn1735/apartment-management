"use client";

import { CircleAlert } from "lucide-react";
import { useEffect, useRef } from "react";
import { cn } from "cn";
import type { ApiError } from "@/lib/api/api";
import { messageFor, requestIdToShow } from "@/lib/api/error-messages";
import { RequestIdNote } from "@/components/feedback/request-id-note";

type FormAlertProps = {
  /** Lỗi chung của form; `errors[]` còn lại là lỗi theo trường không khớp ô nào. */
  error: ApiError | null;
  /** Đổi giá trị thì khung nhận focus, kể cả khi cùng một lỗi lặp lại. */
  focusKey?: number;
  className?: string;
};

/** Khung lỗi ở đầu form. Không tự biến mất; chỉ xóa khi gửi lại (AC-6). */
export function FormAlert({ error, focusKey = 0, className }: FormAlertProps) {
  const ref = useRef<HTMLDivElement>(null);
  const lastFocusKey = useRef(focusKey);

  // Chỉ chuyển focus khi focusKey đổi, không phải mỗi lần lỗi đổi (lỗi đã gắn vào ô thì ô nhận focus)
  useEffect(() => {
    if (focusKey !== lastFocusKey.current) {
      lastFocusKey.current = focusKey;
      ref.current?.focus();
    }
  }, [error, focusKey]);

  if (!error) {
    return null;
  }

  const details = error.errors.map((item) => item.message).filter((message): message is string => !!message);
  const requestId = requestIdToShow(error);

  return (
    <div
      ref={ref}
      role="alert"
      tabIndex={-1}
      data-slot="form-alert"
      className={cn(
        "flex gap-3 rounded-md border-l-[3px] border-destructive bg-destructive-soft px-4 py-3 text-foreground",
        className,
      )}
    >
      <CircleAlert aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-destructive" />
      <div className="flex min-w-0 flex-col gap-1 type-ui">
        <p className="font-medium">{messageFor(error)}</p>
        {details.length > 0 && (
          <ul className="list-disc pl-5">
            {details.map((message, index) => (
              <li key={index}>{message}</li>
            ))}
          </ul>
        )}
        {requestId && <RequestIdNote requestId={requestId} />}
      </div>
    </div>
  );
}
