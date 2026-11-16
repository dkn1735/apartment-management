import { cn } from "cn";
import { Loader2Icon } from "lucide-react";

/** Vòng quay trang trí; trạng thái đang tải được báo bằng chữ "Đang tải" và aria-busy ở nơi dùng. */
function Spinner({ className, ...props }: React.ComponentProps<"svg">) {
  return (
    <Loader2Icon data-slot="spinner" aria-hidden="true" className={cn("size-4 animate-spin", className)} {...props} />
  );
}

export { Spinner };
