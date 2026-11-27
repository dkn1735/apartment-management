"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { notifySuccess } from "@/components/feedback/notify";
import { Button } from "@/components/ui/button";
import { api, client } from "@/lib/api/api";
import { lamMoiPhuongTienCanHo, type PhuongTien } from "@/lib/phuong-tien";

/**
 * Hộp thoại xác nhận cư dân rút đơn đăng ký phương tiện còn chờ duyệt (spec 0016, AC-11, AC-16).
 */
export function RutDonXeDialog({ phuongTien }: { phuongTien: PhuongTien }) {
  const queryClient = useQueryClient();
  const nhanXe = phuongTien.bienSo ?? phuongTien.maPhuongTien;
  const moTaBienSo = phuongTien.bienSo ? `biển số ${phuongTien.bienSo}` : "không có biển";

  return (
    <ConfirmDialog
      trigger={
        <Button type="button" variant="outline" size="sm" aria-label={`Rút đơn đăng ký xe ${nhanXe}`}>
          Rút đơn
        </Button>
      }
      title="Rút đơn đăng ký xe này?"
      description={`Đơn đăng ký ${phuongTien.loaiPhuongTien.toLowerCase()} (${moTaBienSo}) tại căn hộ ${phuongTien.maCanHo} sẽ chuyển sang trạng thái Đã hủy và không thể mở lại.`}
      confirmLabel="Rút đơn"
      tone="destructive"
      onConfirm={async () => {
        await api<PhuongTien>(
          client.POST("/api/v1/vehicles/{vehicleId}/cancel", {
            params: { path: { vehicleId: phuongTien.maPhuongTien } },
          }),
        );
        lamMoiPhuongTienCanHo(queryClient, phuongTien.maCanHo);
        notifySuccess("Đã rút đơn đăng ký phương tiện");
      }}
    />
  );
}
