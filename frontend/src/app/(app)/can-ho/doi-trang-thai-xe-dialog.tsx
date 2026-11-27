"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import * as z from "zod";
import { notifySuccess } from "@/components/feedback/notify";
import { SelectControl } from "@/components/form/field-controls";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { FieldGroup } from "@/components/ui/field";
import { SelectItem } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { api, client } from "@/lib/api/api";
import { doDaiLyDo, LY_DO_TOI_DA } from "@/lib/can-ho";
import {
  canNhapLyDoKhiChuyen,
  lamMoiPhuongTienCanHo,
  trangThaiDichHopLe,
  type PhuongTien,
  type TrangThaiDichPhuongTien,
} from "@/lib/phuong-tien";

const schema = z
  .object({
    trangThaiDangKy: z.string().min(1, "Vui lòng chọn trạng thái"),
    lyDo: z.string().refine((v) => doDaiLyDo(v) <= LY_DO_TOI_DA, `Lý do tối đa ${LY_DO_TOI_DA} ký tự`),
  })
  .superRefine((values, ctx) => {
    if (canNhapLyDoKhiChuyen(values.trangThaiDangKy as TrangThaiDichPhuongTien | "") && doDaiLyDo(values.lyDo) === 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["lyDo"],
        message: "Vui lòng nhập lý do",
      });
    }
  });

type Values = z.output<typeof schema>;

/**
 * Hộp thoại BQL đổi trạng thái thẻ xe theo ma trận AC-10 (spec 0016, AC-9, AC-10, AC-16).
 */
export function DoiTrangThaiXeDialog({ phuongTien }: { phuongTien: PhuongTien }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const cacTrangThaiDich = trangThaiDichHopLe(phuongTien.trangThaiDangKy);
  const nhanXe = phuongTien.bienSo ?? phuongTien.maPhuongTien;

  const form = useApiForm({
    schema,
    defaultValues: { trangThaiDangKy: "", lyDo: "" },
  });

  const trangThaiChon = form.watch("trangThaiDangKy") as TrangThaiDichPhuongTien | "";
  const batBuocLyDo = canNhapLyDoKhiChuyen(trangThaiChon);

  const mutation = useMutation({
    mutationFn: (values: Values) => {
      const lyDoSach = values.lyDo.normalize("NFC").trim();
      return api<PhuongTien>(
        client.PUT("/api/v1/vehicles/{vehicleId}/status", {
          params: { path: { vehicleId: phuongTien.maPhuongTien } },
          body: {
            trangThaiDangKy: values.trangThaiDangKy as TrangThaiDichPhuongTien,
            lyDo: lyDoSach.length > 0 ? lyDoSach : null,
          },
        }),
      );
    },
  });

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    lamMoiPhuongTienCanHo(queryClient, phuongTien.maCanHo);
    notifySuccess("Đã cập nhật trạng thái thẻ xe");
    setOpen(false);
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset({ trangThaiDangKy: "", lyDo: "" });
      }}
    >
      <DialogTrigger asChild>
        <Button type="button" variant="outline" size="sm" aria-label={`Đổi trạng thái thẻ xe ${nhanXe}`}>
          Đổi trạng thái
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Đổi trạng thái thẻ xe</DialogTitle>
          <DialogDescription>
            Cập nhật trạng thái cho {phuongTien.loaiPhuongTien.toLowerCase()} ({nhanXe}) của {phuongTien.hoTenChuXe} tại
            căn hộ {phuongTien.maCanHo}.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={onSubmit}
          noValidate
          aria-label={`Đổi trạng thái thẻ xe ${nhanXe}`}
          className="flex flex-col gap-5"
        >
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="trangThaiDangKy"
              label="Trạng thái mới"
              required
              render={(field) => (
                <SelectControl field={field} placeholder="Chọn trạng thái mới">
                  {cacTrangThaiDich.map((trangThai) => (
                    <SelectItem key={trangThai} value={trangThai}>
                      {trangThai}
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
            />
            <FormField
              control={form.control}
              name="lyDo"
              label="Lý do"
              description={
                batBuocLyDo
                  ? "Bắt buộc khi tạm dừng, từ chối hoặc hủy, tối đa 500 ký tự"
                  : "Không bắt buộc, tối đa 500 ký tự"
              }
              required={batBuocLyDo}
              render={(field) => <Textarea rows={3} {...field} />}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>Lưu trạng thái</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
