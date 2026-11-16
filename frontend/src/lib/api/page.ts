/** Dạng một trang dữ liệu của mọi API danh sách (spec 0001): `page` bắt đầu từ 1. */
export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
