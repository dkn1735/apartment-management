import { FileQuestion, House } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { ErrorState } from "@/components/feedback/error-state";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = { title: "Không tìm thấy trang" };

/** Trang 404 theo design system (AC-13). */
export default function NotFound() {
  return (
    <main className="flex min-h-dvh items-center justify-center px-4 py-10">
      <ErrorState
        headingLevel="h1"
        icon={FileQuestion}
        title="Không tìm thấy trang"
        description="Trang bạn tìm không tồn tại hoặc đã được chuyển đi. Hãy kiểm tra lại đường dẫn."
        actions={
          <Button asChild>
            <Link href="/">
              <House aria-hidden="true" />
              Về trang chủ
            </Link>
          </Button>
        }
      />
    </main>
  );
}
