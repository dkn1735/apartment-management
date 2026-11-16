import { Inbox, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";

type EmptyStateProps = {
  title?: string;
  description?: ReactNode;
  icon?: LucideIcon;
  /** Nút hành động, ví dụ "Thêm căn hộ". */
  action?: ReactNode;
  className?: string;
};

/** Trạng thái rỗng: icon, tiêu đề, mô tả, nút hành động. */
export function EmptyState({
  title = "Chưa có dữ liệu",
  description,
  icon: Icon = Inbox,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div data-slot="empty-state" className={cn("flex flex-col items-center gap-2 px-4 py-10 text-center", className)}>
      <span className="mb-1 flex size-12 items-center justify-center rounded-full bg-muted">
        <Icon aria-hidden="true" className="size-6 text-muted-foreground" />
      </span>
      <p className="type-card-title">{title}</p>
      {description && <div className="max-w-md type-ui text-muted-foreground">{description}</div>}
      {action && <div className="mt-3 flex flex-wrap justify-center gap-3">{action}</div>}
    </div>
  );
}
