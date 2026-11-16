"use client";

import { useEffect } from "react";
import { UnexpectedError } from "@/components/feedback/unexpected-error";

/** Bắt lỗi bất ngờ của các trang dưới root layout (props theo tài liệu Next.js 16: error, retry). */
export default function Error({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return <UnexpectedError error={error} retry={retry} />;
}
