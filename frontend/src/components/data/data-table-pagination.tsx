"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useId } from "react";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { formatSo } from "@/lib/format";
import { PAGE_SIZES, type PageSize } from "./table-params";

type PaginationProps = {
  page: number;
  size: PageSize;
  totalItems: number;
  totalPages: number;
  onPageChange(page: number): void;
  onSizeChange(size: number): void;
};

/** Thanh phân trang: tổng kết quả, trang hiện tại, số dòng mỗi trang, trang trước, trang sau. */
export function DataTablePagination({
  page,
  size,
  totalItems,
  totalPages,
  onPageChange,
  onSizeChange,
}: PaginationProps) {
  const sizeLabelId = useId();
  const isFirst = page <= 1;
  const isLast = page >= totalPages;

  return (
    <nav aria-label="Phân trang" className="flex flex-wrap items-center justify-between gap-x-6 gap-y-3 type-ui">
      <p className="text-muted-foreground">{formatSo(totalItems)} kết quả</p>
      <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
        <div className="flex items-center gap-2">
          <span id={sizeLabelId} className="text-muted-foreground">
            Số dòng mỗi trang
          </span>
          <Select value={String(size)} onValueChange={(value) => onSizeChange(Number(value))}>
            <SelectTrigger aria-labelledby={sizeLabelId} className="h-8 w-20">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {PAGE_SIZES.map((option) => (
                <SelectItem key={option} value={String(option)}>
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <p aria-live="polite">
          Trang {page} / {Math.max(totalPages, 1)}
        </p>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            aria-disabled={isFirst || undefined}
            className="aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
            onClick={() => !isFirst && onPageChange(page - 1)}
          >
            <ChevronLeft aria-hidden="true" />
            Trang trước
          </Button>
          <Button
            variant="outline"
            size="sm"
            aria-disabled={isLast || undefined}
            className="aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
            onClick={() => !isLast && onPageChange(page + 1)}
          >
            Trang sau
            <ChevronRight aria-hidden="true" />
          </Button>
        </div>
      </div>
    </nav>
  );
}
