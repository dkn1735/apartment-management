"use client";

import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";
import { TableHead } from "@/components/ui/table";
import type { DataTableSortOption } from "./data-table-columns";
import type { TableSort } from "./table-params";

type SortHeaderProps = {
  label: ReactNode;
  sortField?: string;
  sortOptions?: readonly DataTableSortOption[];
  sort?: TableSort;
  align?: "left" | "right";
  onSortChange(sort?: TableSort): void;
};

/** Thứ tự xoay vòng khi bấm tiêu đề: tăng, giảm, bỏ sắp xếp. */
function nextSort(field: string, current?: TableSort): TableSort | undefined {
  if (current?.field !== field) return { field, dir: "asc" };
  return current.dir === "asc" ? { field, dir: "desc" } : undefined;
}

/** Ô tiêu đề cột; cột có `sortField` hoặc `sortOptions` là nút bấm và có `aria-sort` khi đang sắp xếp (AC-9). */
export function DataTableSortHeader({
  label,
  sortField,
  sortOptions,
  sort,
  align = "left",
  onSortChange,
}: SortHeaderProps) {
  const alignClass = align === "right" ? "text-right" : "text-left";

  if (sortOptions && sortOptions.length > 0) {
    const activeOption = sortOptions.find((opt) => opt.field === sort?.field);
    const activeDir = activeOption ? sort?.dir : undefined;
    return (
      <TableHead
        scope="col"
        aria-sort={activeDir === "asc" ? "ascending" : activeDir === "desc" ? "descending" : undefined}
        className={cn("px-2", alignClass)}
      >
        <div className="flex flex-wrap items-center gap-1">
          <span className="px-1 font-medium">{label}</span>
          <span className="inline-flex items-center gap-1">
            {sortOptions.map((opt) => {
              const optDir = sort?.field === opt.field ? sort.dir : undefined;
              const OptIcon = optDir === "asc" ? ArrowUp : optDir === "desc" ? ArrowDown : ArrowUpDown;
              return (
                <button
                  key={opt.field}
                  type="button"
                  onClick={() => onSortChange(nextSort(opt.field, sort))}
                  className={cn(
                    "inline-flex h-7 cursor-pointer items-center gap-1 rounded-sm px-1.5 text-xs font-medium transition-colors duration-150 ease-out hover:bg-accent",
                    optDir ? "bg-muted text-foreground" : "text-muted-foreground",
                  )}
                >
                  {opt.label}
                  <OptIcon
                    aria-hidden="true"
                    className={cn("size-3", optDir ? "text-foreground" : "text-muted-foreground")}
                  />
                </button>
              );
            })}
          </span>
        </div>
      </TableHead>
    );
  }

  if (!sortField) {
    return (
      <TableHead scope="col" className={alignClass}>
        {label}
      </TableHead>
    );
  }

  const dir = sort?.field === sortField ? sort.dir : undefined;
  const Icon = dir === "asc" ? ArrowUp : dir === "desc" ? ArrowDown : ArrowUpDown;
  return (
    <TableHead
      scope="col"
      aria-sort={dir === "asc" ? "ascending" : dir === "desc" ? "descending" : undefined}
      className={cn("px-1", alignClass)}
    >
      <button
        type="button"
        onClick={() => onSortChange(nextSort(sortField, sort))}
        className={cn(
          "inline-flex h-8 cursor-pointer items-center gap-1.5 rounded-sm px-2 font-medium transition-colors duration-150 ease-out hover:bg-accent",
          align === "right" && "flex-row-reverse",
        )}
      >
        {label}
        <Icon aria-hidden="true" className={cn("size-3.5", dir ? "text-foreground" : "text-muted-foreground")} />
      </button>
    </TableHead>
  );
}
