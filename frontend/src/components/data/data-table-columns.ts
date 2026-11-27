import {
  createColumnHelper,
  metaHelper,
  rowPaginationFeature,
  rowSortingFeature,
  tableFeatures,
  type ColumnDef,
  type RowData,
} from "@tanstack/react-table";

export type DataTableSortOption = {
  field: string;
  label: string;
};

/** Thông tin thêm của một cột DataTable. */
export type DataTableColumnMeta = {
  /** Tên trường gửi lên `sort=`; phải nằm trong danh sách trắng của endpoint. Không có thì cột không sắp xếp được. */
  sortField?: string;
  /** Nhiều trường sắp xếp trong cùng một cột gộp (ví dụ Thời gian cư trú gồm Ngày bắt đầu và Ngày kết thúc). */
  sortOptions?: readonly DataTableSortOption[];
  /** Cột mô tả dài: cắt ở 2 dòng. */
  description?: boolean;
  /** Cột số, tiền: căn phải (Be Vietnam Pro không có chữ số đều cột). */
  align?: "left" | "right";
};

/** Bộ tính năng TanStack Table v9 mà DataTable dùng: sắp xếp và phân trang đều chạy phía server. */
export const dataTableFeatures = tableFeatures({
  rowSortingFeature,
  rowPaginationFeature,
  columnMeta: metaHelper<DataTableColumnMeta>(),
});

export type DataTableFeatures = typeof dataTableFeatures;

/** Định nghĩa một cột của DataTable. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type DataTableColumn<T extends RowData> = ColumnDef<DataTableFeatures, T, any>;

/**
 * Tạo bộ định nghĩa cột có kiểu cho DataTable. Gọi ở ngoài component (hoặc trong useMemo) để cột ổn định.
 * Ví dụ: `const col = dataTableColumns<CanHo>(); const columns = col.columns([col.accessor("maCanHo", {...})])`.
 */
export function dataTableColumns<T extends RowData>() {
  return createColumnHelper<DataTableFeatures, T>();
}
