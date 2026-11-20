import { vi } from "vitest";

/** Đường dẫn giả cho usePathname; test đổi bằng `mockPathname.mockReturnValue("/can-ho/123")`. */
export const mockPathname = vi.fn(() => "/");

/** notFound giả: ném lỗi như bản thật để dừng render. */
export const mockNotFound = vi.fn((): never => {
  throw new Error("NEXT_NOT_FOUND");
});

/** Tham số URL giả cho useSearchParams; test đổi bằng `mockSearchParams.mockReturnValue(new URLSearchParams("a=1"))`. */
export const mockSearchParams = vi.fn(() => new URLSearchParams());

/** `router.replace` giả, để test kiểm URL mà trang ghi. */
export const mockReplace = vi.fn();
