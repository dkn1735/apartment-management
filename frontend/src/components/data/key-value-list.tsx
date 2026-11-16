import type { ReactNode } from "react";
import { cn } from "cn";

export type KeyValueItem = { label: ReactNode; value: ReactNode };

type KeyValueListProps = {
  items: KeyValueItem[];
  /** `striped`: mỗi hàng nền --muted, cách nhau 4px; `divided`: nền trong, kẻ --border giữa các hàng. */
  variant?: "striped" | "divided";
  className?: string;
};

/** Danh sách nhãn và giá trị (`<dl>`). Dưới 640px nhãn và giá trị xếp chồng. */
export function KeyValueList({ items, variant = "striped", className }: KeyValueListProps) {
  return (
    <dl className={cn("flex flex-col", variant === "striped" ? "gap-1" : "divide-y", className)}>
      {items.map((item, index) => (
        <div
          key={index}
          className={cn(
            "flex min-h-10 flex-col gap-0.5 px-3 py-3 type-ui sm:flex-row sm:items-start sm:justify-between sm:gap-4",
            variant === "striped" && "rounded-md bg-muted",
          )}
        >
          <dt className="shrink-0 text-foreground">{item.label}</dt>
          <dd className="min-w-0 font-medium [overflow-wrap:anywhere] break-words sm:text-right">{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}
