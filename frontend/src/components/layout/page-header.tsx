import type { ReactNode } from "react";
import { cn } from "cn";

type PageHeaderProps = {
  title: string;
  description?: ReactNode;
  /** Nút hành động, căn phải; dưới 640px xuống dưới tiêu đề. */
  actions?: ReactNode;
  className?: string;
};

/** Đầu trang: một `h1` duy nhất, mô tả, vùng nút hành động. Cách nội dung 32px. */
export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
  return (
    <header className={cn("mb-8 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between", className)}>
      <div className="flex min-w-0 flex-col gap-1">
        <h1 className="type-page-title">{title}</h1>
        {description && <div className="max-w-3xl text-muted-foreground">{description}</div>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap gap-3">{actions}</div>}
    </header>
  );
}
