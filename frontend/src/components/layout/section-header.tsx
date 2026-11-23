import type { ReactNode, Ref } from "react";
import { cn } from "cn";

type SectionHeaderProps = {
  title: string;
  /** Nhãn nhỏ màu chính phía trên tiêu đề. */
  eyebrow?: string;
  description?: ReactNode;
  /** `h3` khi mục nằm trong một mục khác. */
  as?: "h2" | "h3";
  id?: string;
  actions?: ReactNode;
  className?: string;
  /** Cho trang chuyển focus tới tiêu đề sau một thao tác (tiêu đề nhận `tabIndex={-1}`). */
  headingRef?: Ref<HTMLHeadingElement>;
};

/** Đầu một mục: nhãn nhỏ, tiêu đề, mô tả. Cách nội dung mục 20px. */
export function SectionHeader({
  title,
  eyebrow,
  description,
  as: Heading = "h2",
  id,
  actions,
  className,
  headingRef,
}: SectionHeaderProps) {
  return (
    <div className={cn("mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between", className)}>
      <div className="flex min-w-0 flex-col gap-1">
        {eyebrow && <p className="type-eyebrow text-primary">{eyebrow}</p>}
        <Heading
          ref={headingRef}
          id={id}
          tabIndex={headingRef ? -1 : undefined}
          className={Heading === "h2" ? "type-section-title" : "type-card-title"}
        >
          {title}
        </Heading>
        {description && <div className="type-ui text-muted-foreground">{description}</div>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap gap-3">{actions}</div>}
    </div>
  );
}
