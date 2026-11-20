"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { api, client } from "@/lib/api/api";
import { authNavigation, BO_QUA_CHUYEN_TRANG } from "@/lib/api/query-client";
import { batDauDangXuat, dangLamMoi, ketThucDangXuat } from "@/lib/api/refresh";

export const TRANG_SAU_DANG_XUAT = "/dang-nhap?ly-do=dang-xuat";

export const LOI_DANG_XUAT = "Không đăng xuất được, vui lòng thử lại.";

/**
 * Đăng xuất (spec 0005, AC-14). Từ lúc bấm, không bắt đầu lần làm mới nào và không chuyển
 * trang `het-phien`; một lần làm mới đang chạy được chờ xong trước khi gọi `logout`, để nó
 * không đặt lại cookie sau khi đã đăng xuất.
 */
export function useDangXuat() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      batDauDangXuat();
      await queryClient.cancelQueries();
      await dangLamMoi()?.catch(() => undefined);
      await api(client.POST("/api/v1/auth/logout"));
    },
    meta: { [BO_QUA_CHUYEN_TRANG]: true },
    onSuccess: () => {
      queryClient.clear();
      // Tải lại hẳn trang; cờ đăng xuất mất theo trang cũ
      authNavigation.assign(TRANG_SAU_DANG_XUAT);
    },
    onError: () => {
      ketThucDangXuat();
      toast.error(LOI_DANG_XUAT, { duration: Infinity, action: { label: "Đóng", onClick: () => {} } });
    },
  });
}
