"use client";

import { ApiError } from "@/lib/api/api";
import { ErrorState } from "./error-state";
import { KhongCoQuyen } from "./khong-co-quyen";
import { RetryButton } from "./retry-button";

type QueryErrorStateProps = {
  error: unknown;
  /** Tiêu đề khi lỗi không phải 403 `FORBIDDEN`. */
  title: string;
  onRetry: () => unknown;
  retrying?: boolean;
  headingLevel?: "h1" | "h2" | "h3" | "h4";
  className?: string;
};

/**
 * Chỗ trang hiện lỗi của một query (spec 0006, AC-12): 403 `FORBIDDEN` thì là `KhongCoQuyen` ở cùng mức tiêu đề,
 * không "Thử lại"; lỗi khác thì `ErrorState` có "Thử lại".
 */
export function QueryErrorState({
  error,
  title,
  onRetry,
  retrying,
  headingLevel = "h2",
  className,
}: QueryErrorStateProps) {
  const apiError = error instanceof ApiError ? error : null;
  if (apiError?.errorCode === "FORBIDDEN") {
    return <KhongCoQuyen headingLevel={headingLevel} hideHomeAction={headingLevel !== "h1"} className={className} />;
  }
  return (
    <ErrorState
      title={title}
      error={apiError}
      headingLevel={headingLevel}
      className={className}
      actions={<RetryButton onRetry={onRetry} loading={retrying} />}
    />
  );
}
