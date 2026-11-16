import { vi } from "vitest";

/** Đường dẫn giả cho usePathname; test đổi bằng `mockPathname.mockReturnValue("/can-ho/123")`. */
export const mockPathname = vi.fn(() => "/");

/** notFound giả: ném lỗi như bản thật để dừng render. */
export const mockNotFound = vi.fn((): never => {
  throw new Error("NEXT_NOT_FOUND");
});
