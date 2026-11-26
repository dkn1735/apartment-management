"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useId } from "react";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { notifyInfo, notifySuccess } from "@/components/feedback/notify";
import { Button } from "@/components/ui/button";
import { api, ApiError, client } from "@/lib/api/api";
import {
  CAU_TOI_DA_TANG,
  khoaChiTietToaNha,
  SO_TANG_TOI_DA,
  TOA_NHA_QUERY_KEY,
  type Tang,
  type ToaNha,
} from "@/lib/toa-nha";

/**
 * Nút "Thêm tầng" kèm hộp thoại xác nhận (spec 0011, AC-17). Tầng đề xuất là số tầng đang hiển thị cộng một. Server
 * trả 422 (người khác vừa thêm, hoặc tòa đã đủ 99 tầng) thì tải lại chi tiết tòa và báo theo số tầng mới.
 */
export function ThemTangButton({ toaNha }: { toaNha: ToaNha }) {
  const goiYId = useId();
  const queryClient = useQueryClient();
  const tangMoi = toaNha.soTang + 1;
  const mutation = useMutation({
    mutationFn: () =>
      api<Tang>(
        client.POST("/api/v1/buildings/{buildingId}/floors", {
          params: { path: { buildingId: toaNha.maToaNha } },
          body: { soTang: tangMoi },
        }),
      ),
  });

  if (toaNha.soTang >= SO_TANG_TOI_DA) {
    return (
      <div className="flex flex-col items-start gap-1 sm:items-end">
        <Button disabled aria-describedby={goiYId}>
          <Plus aria-hidden="true" />
          Thêm tầng
        </Button>
        <p id={goiYId} className="type-caption text-muted-foreground">
          {CAU_TOI_DA_TANG}
        </p>
      </div>
    );
  }

  async function xacNhan() {
    try {
      const tang = await mutation.mutateAsync();
      void queryClient.invalidateQueries({ queryKey: TOA_NHA_QUERY_KEY });
      notifySuccess(`Đã thêm tầng ${tang.soTang}`);
    } catch (error) {
      if (!(error instanceof ApiError) || error.errorCode !== "CANHO_TANG_SAI_THU_TU") throw error;
      // Tải lại chi tiết tòa ngay để số tầng mới hiện ra, rồi báo theo số vừa tải về
      void queryClient.invalidateQueries({ queryKey: TOA_NHA_QUERY_KEY });
      await queryClient.refetchQueries({ queryKey: khoaChiTietToaNha(toaNha.maToaNha) });
      const moi = queryClient.getQueryData<ToaNha>(khoaChiTietToaNha(toaNha.maToaNha));
      notifyInfo((moi?.soTang ?? 0) >= SO_TANG_TOI_DA ? CAU_TOI_DA_TANG : "Tòa vừa được cập nhật, vui lòng thử lại");
    }
  }

  return (
    <ConfirmDialog
      trigger={
        <Button>
          <Plus aria-hidden="true" />
          Thêm tầng
        </Button>
      }
      title={`Thêm tầng ${tangMoi} vào ${toaNha.tenToaNha}?`}
      description={`Tầng mới có mã ${toaNha.maToaNha}-${String(tangMoi).padStart(2, "0")}. Tầng đã thêm chưa xóa được.`}
      confirmLabel={`Thêm tầng ${tangMoi}`}
      onConfirm={xacNhan}
    />
  );
}
