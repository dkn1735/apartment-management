export const PAGE_SIZES = [10, 20, 50] as const;
export type PageSize = (typeof PAGE_SIZES)[number];
export type SortDir = "asc" | "desc";
export type TableSort = { field: string; dir: SortDir };

export type TableParams = { page: number; size: PageSize; sort?: TableSort };

const DEFAULT_SIZE: PageSize = 20;

type ParamsSource = { get(name: string): string | null } | Record<string, string | string[] | undefined>;

function read(source: ParamsSource, name: string): string | undefined {
  if (typeof source.get === "function") {
    return (source as { get(name: string): string | null }).get(name) ?? undefined;
  }
  const value = (source as Record<string, string | string[] | undefined>)[name];
  return Array.isArray(value) ? value[0] : value;
}

/**
 * Đọc `page`, `size`, `sort` từ URL cho mọi trang có bảng (spec 0003, Giao diện DataTable).
 * `page` không phải số nguyên dương thì 1; `size` ngoài 10, 20, 50 thì 20; `sort` dạng `truong,asc|desc`.
 * Nhận cả `URLSearchParams` (client) lẫn `searchParams` đã await của trang (server).
 */
export function parseTableParams(source: ParamsSource): TableParams {
  const pageText = read(source, "page") ?? "";
  const page = /^\d+$/.test(pageText) && Number(pageText) >= 1 ? Number(pageText) : 1;
  const sizeNumber = Number(read(source, "size"));
  const size = (PAGE_SIZES as readonly number[]).includes(sizeNumber) ? (sizeNumber as PageSize) : DEFAULT_SIZE;
  const sortMatch = /^([A-Za-z][\w.]*),(asc|desc)$/.exec(read(source, "sort") ?? "");
  const sort = sortMatch ? { field: sortMatch[1], dir: sortMatch[2] as SortDir } : undefined;
  return { page, size, sort };
}

/** Giá trị tham số `sort=` gửi lên API. */
export function toSortParam(sort?: TableSort): string | undefined {
  return sort ? `${sort.field},${sort.dir}` : undefined;
}
