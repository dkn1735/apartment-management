"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import * as z from "zod";
import { notifySuccess } from "@/components/feedback/notify";
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
import { Input } from "@/components/ui/input";
import { api, client } from "@/lib/api/api";
import { lamMoiSauThayDoiCuTru, type HoSoCuTru } from "@/lib/cu-tru";
import { formatNgay, ngayHomNay } from "@/lib/format";

function taoSchema(ngayBatDau: string) {
  return z
    .object({
      ngayKetThuc: z.string().min(1, "Kết thúc hồ sơ bắt buộc có ngày kết thúc"),
    })
    .refine((v) => !v.ngayKetThuc || v.ngayKetThuc >= ngayBatDau, {
      path: ["ngayKetThuc"],
      message: "Ngày kết thúc phải từ ngày bắt đầu trở đi",
    });
}

type Values = { ngayKetThuc: string };

function ngayKetThucMacDinh(hoSo: HoSoCuTru, laHuySom: boolean): string {
  const homNay = ngayHomNay();
  return laHuySom || hoSo.ngayBatDau > homNay ? hoSo.ngayBatDau : homNay;
}

/**
 * Hộp thoại BQL kết thúc cư trú hoặc hủy sớm hồ sơ chưa tới ngày bắt đầu (spec 0014, AC-7, AC-8, AC-17, AC-18).
 * Hồ sơ `sapBatDau` khóa chỉ đọc `ngayKetThuc = ngayBatDau`; hồ sơ đã bắt đầu điền sẵn hôm nay theo giờ Việt Nam.
 */
export function KetThucCuTruDialog({ hoSo, laHuySom = false }: { hoSo: HoSoCuTru; laHuySom?: boolean }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const tenCuDan = hoSo.hoTen ?? hoSo.maCuDan;
  const khoaNgay = laHuySom || hoSo.ngayBatDau > ngayHomNay();
  const nhanNut = laHuySom ? "Hủy hồ sơ" : "Kết thúc cư trú";
  const tieuDe = laHuySom ? "Hủy hồ sơ cư trú" : "Kết thúc hồ sơ cư trú";
  const form = useApiForm({
    schema: taoSchema(hoSo.ngayBatDau),
    defaultValues: { ngayKetThuc: ngayKetThucMacDinh(hoSo, laHuySom) },
  });

  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<HoSoCuTru>(
        client.PUT("/api/v1/residency-records/{recordId}", {
          params: { path: { recordId: hoSo.maHoSo } },
          body: {
            vaiTroCuTru: hoSo.vaiTroCuTru,
            tinhTrangCuTru: hoSo.tinhTrangCuTru,
            ngayBatDau: hoSo.ngayBatDau,
            ngayKetThuc: values.ngayKetThuc,
            trangThaiHoSo: "Hết hiệu lực",
          },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    lamMoiSauThayDoiCuTru(queryClient, hoSo.maCanHo);
    notifySuccess(laHuySom ? "Đã hủy hồ sơ cư trú" : "Đã kết thúc hồ sơ cư trú");
    setOpen(false);
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset({ ngayKetThuc: ngayKetThucMacDinh(hoSo, laHuySom) });
      }}
    >
      <DialogTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          aria-label={laHuySom ? `Hủy hồ sơ cư trú của ${tenCuDan}` : `Kết thúc cư trú của ${tenCuDan}`}
        >
          {nhanNut}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{tieuDe}</DialogTitle>
          <DialogDescription>
            {tenCuDan} ({hoSo.maCuDan}) tại căn hộ {hoSo.maCanHo}, bắt đầu từ {formatNgay(hoSo.ngayBatDau)}. Hồ sơ đã
            chuyển sang hết hiệu lực sẽ không mở lại được.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label={`${tieuDe} của ${tenCuDan}`} className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="ngayKetThuc"
              label="Ngày kết thúc"
              required
              description={
                khoaNgay
                  ? "Hồ sơ chưa tới ngày bắt đầu nên ngày kết thúc đặt bằng ngày bắt đầu."
                  : `Từ ${formatNgay(hoSo.ngayBatDau)} đến hôm nay.`
              }
              render={(field) => (
                <Input type="date" readOnly={khoaNgay} className={khoaNgay ? "bg-muted" : undefined} {...field} />
              )}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Đóng
              </Button>
            </DialogClose>
            <SubmitButton form={form} variant="destructive">
              {nhanNut}
            </SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
