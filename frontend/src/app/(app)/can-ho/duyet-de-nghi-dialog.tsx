"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { notifySuccess } from "@/components/feedback/notify";
import { Button } from "@/components/ui/button";
import { api, client } from "@/lib/api/api";
import { lamMoiSauThayDoiCuTru, type HoSoCuTru } from "@/lib/cu-tru";
import { formatNgay } from "@/lib/format";

/** Hộp thoại BQL xác nhận duyệt đề nghị cư trú đang chờ xác minh (spec 0015, AC-4, AC-15, AC-17, AC-18). */
export function DuyetDeNghiDialog({ hoSo }: { hoSo: HoSoCuTru }) {
  const queryClient = useQueryClient();
  const tenCuDan = hoSo.hoTen ?? hoSo.maCuDan;
  const khoangNgay = `${formatNgay(hoSo.ngayBatDau)} đến ${hoSo.ngayKetThuc ? formatNgay(hoSo.ngayKetThuc) : "không thời hạn"}`;

  return (
    <ConfirmDialog
      trigger={
        <Button type="button" size="sm" aria-label={`Duyệt đề nghị cư trú của ${tenCuDan}`}>
          Duyệt
        </Button>
      }
      title="Duyệt đề nghị cư trú?"
      description={
        <span>
          Duyệt cho <strong>{tenCuDan}</strong> ({hoSo.maCuDan}) đăng ký {hoSo.vaiTroCuTru.toLowerCase()},{" "}
          {hoSo.tinhTrangCuTru.toLowerCase()} tại căn hộ <strong>{hoSo.maCanHo}</strong> ({khoangNgay}).
          {hoSo.ghiChuDeNghi ? ` Ghi chú của cư dân: "${hoSo.ghiChuDeNghi}".` : ""} Hồ sơ sẽ chuyển sang Hợp lệ và trạng
          thái căn hộ được cập nhật tương ứng.
        </span>
      }
      confirmLabel="Duyệt đề nghị"
      onConfirm={async () => {
        await api<HoSoCuTru>(
          client.POST("/api/v1/residency-records/{recordId}/approve", {
            params: { path: { recordId: hoSo.maHoSo } },
          }),
        );
        lamMoiSauThayDoiCuTru(queryClient, hoSo.maCanHo);
        notifySuccess("Đã duyệt đề nghị cư trú");
      }}
    />
  );
}
