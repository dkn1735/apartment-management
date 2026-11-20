import { FileQuestion, House } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ErrorState } from "./error-state";

/** Trạng thái không tìm thấy trang (AC-13 của design system), dùng cho trang 404 gốc và trong khung app. */
export function KhongTimThay() {
  return (
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
  );
}
