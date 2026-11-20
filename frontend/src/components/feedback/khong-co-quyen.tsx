import { House, ShieldX } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ErrorState } from "./error-state";

type KhongCoQuyenProps = {
  /** `h1` khi thay cả trang; `h2` tới `h4` khi nằm trong một vùng của trang. */
  headingLevel?: "h1" | "h2" | "h3" | "h4";
  /** Ẩn nút "Về trang chủ", khi chỉ một vùng của trang bị chặn. */
  hideHomeAction?: boolean;
  className?: string;
};

/** Trạng thái không có quyền (spec 0006, AC-11, AC-12). Không có mã tra cứu và không có "Thử lại". */
export function KhongCoQuyen({ headingLevel = "h1", hideHomeAction, className }: KhongCoQuyenProps) {
  return (
    <ErrorState
      headingLevel={headingLevel}
      icon={ShieldX}
      title="Không có quyền truy cập"
      description="Bạn không có quyền xem trang này. Nếu cần quyền, vui lòng liên hệ ban quản lý."
      className={className}
      actions={
        hideHomeAction ? undefined : (
          <Button asChild>
            <Link href="/">
              <House aria-hidden="true" />
              Về trang chủ
            </Link>
          </Button>
        )
      }
    />
  );
}
