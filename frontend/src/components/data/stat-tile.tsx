import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";

type StatTileProps = {
  label: string;
  value: ReactNode;
  icon: LucideIcon;
  hint?: ReactNode;
  /** `default` nền --muted; `highlight` xanh bạc hà; `on-dark` đặt trong HeroBand. */
  variant?: "default" | "highlight" | "on-dark";
  className?: string;
};

const variants = {
  default: { tile: "bg-muted text-foreground", label: "text-muted-foreground", icon: "bg-card text-primary" },
  highlight: {
    tile: "bg-highlight text-highlight-foreground",
    label: "text-highlight-foreground",
    icon: "bg-card text-primary",
  },
  "on-dark": {
    tile: "border border-on-dark/16 bg-on-dark/8 text-on-dark",
    label: "text-on-dark-muted",
    icon: "border-on-dark/16 bg-on-dark/8 text-on-dark",
  },
} as const;

/** Ô số liệu: icon, nhãn, số lớn, dòng gợi ý. Số căn trái theo thang Số lớn. */
export function StatTile({ label, value, icon: Icon, hint, variant = "default", className }: StatTileProps) {
  const style = variants[variant];
  return (
    <div data-slot="stat-tile" className={cn("flex flex-col gap-3 rounded-md p-5", style.tile, className)}>
      <span className={cn("flex size-8 items-center justify-center rounded-md border", style.icon)}>
        <Icon aria-hidden="true" className="size-[18px]" />
      </span>
      <div className="flex flex-col gap-1">
        <p className={cn("type-ui", style.label)}>{label}</p>
        <p className="type-stat break-words">{value}</p>
        {hint && <p className={cn("type-caption", style.label)}>{hint}</p>}
      </div>
    </div>
  );
}
