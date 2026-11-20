"use client";

import { usePathname, useRouter } from "next/navigation";
import { useCallback, useRef } from "react";

/**
 * Ghi tham số lên URL của trang hiện tại; `undefined` thì xóa tham số đó. DataTable gọi hai lần liền nhau (đổi
 * sort rồi về trang 1), nên gộp vào bản mới nhất thay vì đọc lại `searchParams` cũ của lần render này.
 */
export function useCapNhatUrl(searchParams: URLSearchParams) {
  const router = useRouter();
  const pathname = usePathname();
  const goc = searchParams.toString();
  const moiNhat = useRef<{ goc: string; params: URLSearchParams } | null>(null);
  return useCallback(
    (doi: Record<string, string | undefined>) => {
      // Các lần gọi trong cùng một lượt render có cùng `goc`, nên cộng dồn; URL đã đổi thì bắt đầu lại
      if (moiNhat.current?.goc !== goc) {
        moiNhat.current = { goc, params: new URLSearchParams(goc) };
      }
      const params = moiNhat.current.params;
      for (const [ten, giaTri] of Object.entries(doi)) {
        if (giaTri === undefined) params.delete(ten);
        else params.set(ten, giaTri);
      }
      const qs = params.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [router, pathname, goc],
  );
}
