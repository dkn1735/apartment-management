"use client";

import { House, RotateCcw } from "lucide-react";
import Link from "next/link";
import { ApiError } from "@/lib/api/api";
import { Button } from "@/components/ui/button";
import { ErrorState } from "./error-state";

type UnexpectedErrorProps = { error: unknown; retry: () => void };

/** Nội dung trang lỗi bất ngờ, dùng chung cho error.tsx và global-error.tsx (AC-13). */
export function UnexpectedError({ error, retry }: UnexpectedErrorProps) {
  return (
    <main className="flex min-h-dvh items-center justify-center px-4 py-10">
      <ErrorState
        headingLevel="h1"
        title="Đã có lỗi xảy ra"
        description={
          error instanceof ApiError ? undefined : "Trang gặp sự cố ngoài dự kiến. Bạn có thể thử lại hoặc về trang chủ."
        }
        error={error instanceof ApiError ? error : undefined}
        actions={
          <>
            <Button onClick={() => retry()}>
              <RotateCcw aria-hidden="true" />
              Thử lại
            </Button>
            <Button variant="outline" asChild>
              <Link href="/">
                <House aria-hidden="true" />
                Về trang chủ
              </Link>
            </Button>
          </>
        }
      />
    </main>
  );
}
