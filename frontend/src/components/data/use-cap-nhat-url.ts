"use client";

import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef } from "react";

/**
 * Ghi tham số lên URL của trang hiện tại; `undefined` thì xóa tham số đó. DataTable gọi hai lần liền nhau (đổi
 * sort rồi về trang 1), nên gộp vào bản mới nhất thay vì đọc lại `searchParams` cũ của lần render này.
 *
 * Giá trị là mảng thì ghi tham số lặp lại (`?a=1&a=2`); mảng rỗng xóa tham số đó.
 *
 * Mặc định thay URL tại chỗ (sắp xếp, phân trang). Truyền `{ themLichSu: true }` cho thay đổi người dùng chủ động
 * áp dụng như bộ lọc, để nút Back của trình duyệt trả lại trạng thái trước.
 */
export function useCapNhatUrl(searchParams: URLSearchParams) {
  const router = useRouter();
  const pathname = usePathname();
  const goc = searchParams.toString();
  const moiNhat = useRef<{ goc: string; params: URLSearchParams } | null>(null);
  // URL đã đổi thật (kể cả nút Back về đúng chuỗi cũ) thì bỏ bản cộng dồn, để không mang lại tham số của lần ghi trước
  useEffect(() => {
    moiNhat.current = null;
  }, [goc]);
  return useCallback(
    (
      doi: Record<string, string | readonly string[] | undefined>,
      { themLichSu = false }: { themLichSu?: boolean } = {},
    ) => {
      // Các lần gọi trong cùng một lượt render có cùng `goc`, nên cộng dồn; URL đã đổi thì bắt đầu lại
      if (moiNhat.current?.goc !== goc) {
        moiNhat.current = { goc, params: new URLSearchParams(goc) };
      }
      const params = moiNhat.current.params;
      for (const [ten, giaTri] of Object.entries(doi)) {
        params.delete(ten);
        if (typeof giaTri === "string") params.set(ten, giaTri);
        else giaTri?.forEach((motGiaTri) => params.append(ten, motGiaTri));
      }
      const qs = params.toString();
      const dich = qs ? `${pathname}?${qs}` : pathname;
      if (themLichSu) router.push(dich, { scroll: false });
      else router.replace(dich, { scroll: false });
    },
    [router, pathname, goc],
  );
}
