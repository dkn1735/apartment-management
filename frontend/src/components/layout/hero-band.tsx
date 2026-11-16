import type { ReactNode } from "react";
import { cn } from "cn";

type HeroBandProps = {
  /** Dòng chào nhỏ, ví dụ "Chào buổi sáng". */
  greeting?: string;
  title: string;
  /** Mức tiêu đề: `h1` khi HeroBand là đầu trang tổng quan. */
  titleAs?: "h1" | "h2";
  subtitle?: ReactNode;
  /** Vùng bên phải: các StatTile `on-dark`. */
  children?: ReactNode;
  className?: string;
};

/** Mảng tối đầu trang tổng quan. Vòng focus bên trong đổi sang màu sáng nhờ lớp `.on-dark`. */
export function HeroBand({ greeting, title, titleAs: Title = "h2", subtitle, children, className }: HeroBandProps) {
  return (
    <section
      className={cn(
        "on-dark grid gap-6 rounded-md bg-surface-dark p-5 text-on-dark sm:p-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)] lg:items-center",
        className,
      )}
    >
      <div className="flex min-w-0 flex-col gap-1">
        {greeting && <p className="text-[18px] leading-snug">{greeting}</p>}
        <Title className="text-[28px] leading-tight font-medium break-words sm:text-[36px]">{title}</Title>
        {subtitle && <div className="type-ui text-on-dark-muted">{subtitle}</div>}
      </div>
      {children && <div className="grid grid-cols-[repeat(auto-fit,minmax(160px,1fr))] gap-3">{children}</div>}
    </section>
  );
}
