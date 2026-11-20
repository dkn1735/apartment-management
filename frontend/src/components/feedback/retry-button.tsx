"use client";

import { RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";

type RetryButtonProps = { onRetry: () => unknown; loading?: boolean; className?: string };

/** Nút "Thử lại" của các trạng thái lỗi khi tải dữ liệu. */
export function RetryButton({ onRetry, loading, className }: RetryButtonProps) {
  return (
    <Button size="sm" variant="outline" loading={loading} className={className} onClick={() => void onRetry()}>
      <RotateCcw aria-hidden="true" />
      Thử lại
    </Button>
  );
}
