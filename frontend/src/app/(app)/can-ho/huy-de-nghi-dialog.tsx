"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { notifySuccess } from "@/components/feedback/notify";
import { Button } from "@/components/ui/button";
import { api, client } from "@/lib/api/api";
import { lamMoiSauThayDoiCuTru, type HoSoCuTru } from "@/lib/cu-tru";
import { formatNgay } from "@/lib/format";

/** Hộp thoại cư dân tự hủy đề nghị cư trú do chính mình gửi khi còn chờ xác minh (spec 0015, AC-6, AC-15, AC-18). */
export function HuyDeNghiDialog({ hoSo }: { hoSo: HoSoCuTru }) {
  const queryClient = useQueryClient();
  const tenCuDan = hoSo.hoTen ?? hoSo.maCuDan;
  const khoangNgay = `${formatNgay(hoSo.ngayBatDau)} đến ${hoSo.ngayKetThuc ? formatNgay(hoSo.ngayKetThuc) : "không thời hạn"}`;

  return (
    <ConfirmDialog
      trigger={
        <Button type="button" variant="outline" size="sm" aria-label={`Hủy đề nghị cư trú của ${tenCuDan}`}>
          Hủy đề nghị
        </Button>
      }
      title="Hủy đề nghị cư trú này?"
      description={`Đề nghị cư trú cho ${tenCuDan} tại căn hộ ${hoSo.maCanHo} (${khoangNgay}) sẽ chuyển sang trạng thái Đã hủy và không thể mở lại.`}
      confirmLabel="Hủy đề nghị"
      tone="destructive"
      onConfirm={async () => {
        await api<HoSoCuTru>(
          client.POST("/api/v1/residency-records/{recordId}/cancel", {
            params: { path: { recordId: hoSo.maHoSo } },
          }),
        );
        lamMoiSauThayDoiCuTru(queryClient, hoSo.maCanHo);
        notifySuccess("Đã hủy đề nghị cư trú");
      }}
    />
  );
}
