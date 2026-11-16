import { cn } from "cn";

/** Khối giữ chỗ khi đang tải lần đầu. Trang trí, nên ẩn với trình đọc màn hình; nơi dùng tự báo "Đang tải". */
function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      aria-hidden="true"
      className={cn("animate-pulse rounded-md bg-muted", className)}
      {...props}
    />
  );
}

export { Skeleton };
