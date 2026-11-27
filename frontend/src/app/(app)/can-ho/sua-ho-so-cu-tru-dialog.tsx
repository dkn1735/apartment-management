"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useWatch } from "react-hook-form";
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
import { Input } from "@/components/ui/input";
import { SelectItem } from "@/components/ui/select";
import { api, client } from "@/lib/api/api";
import {
  lamMoiSauThayDoiCuTru,
  TINH_TRANG_CU_TRU,
  VAI_TRO_CU_TRU,
  type HoSoCuTru,
  type TinhTrangCuTru,
  type VaiTroCuTru,
} from "@/lib/cu-tru";

const schema = z
  .object({
    vaiTroCuTru: z.string().min(1, "Vui lòng chọn vai trò cư trú"),
    tinhTrangCuTru: z.string().min(1, "Vui lòng chọn tình trạng cư trú"),
    ngayBatDau: z.string().min(1, "Vui lòng chọn ngày bắt đầu"),
    ngayKetThuc: z.string(),
  })
  .superRefine((values, ctx) => {
    const ketThuc = values.ngayKetThuc.trim();
    if (values.tinhTrangCuTru === "Tạm trú" && !ketThuc) {
      ctx.addIssue({ code: "custom", path: ["ngayKetThuc"], message: "Tạm trú bắt buộc có ngày kết thúc" });
    } else if (values.ngayBatDau && ketThuc && ketThuc < values.ngayBatDau) {
      ctx.addIssue({
        code: "custom",
        path: ["ngayKetThuc"],
        message: "Ngày kết thúc phải từ ngày bắt đầu trở đi",
      });
    }
  });

type Values = z.output<typeof schema>;

function giaTriTuHoSo(hoSo: HoSoCuTru): Values {
  return {
    vaiTroCuTru: hoSo.vaiTroCuTru,
    tinhTrangCuTru: hoSo.tinhTrangCuTru,
    ngayBatDau: hoSo.ngayBatDau,
    ngayKetThuc: hoSo.ngayKetThuc ?? "",
  };
}

/**
 * Hộp thoại BQL chỉnh sửa 4 trường của hồ sơ đang hợp lệ (spec 0014, AC-7, AC-8, AC-17, AC-18).
 * Luôn gửi `trangThaiHoSo = 'Hợp lệ'`; căn hộ và cư dân giữ nguyên.
 */
export function SuaHoSoCuTruDialog({ hoSo }: { hoSo: HoSoCuTru }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const tenCuDan = hoSo.hoTen ?? hoSo.maCuDan;
  const form = useApiForm({ schema, defaultValues: giaTriTuHoSo(hoSo) });
  const tinhTrang = useWatch({ control: form.control, name: "tinhTrangCuTru" });

  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<HoSoCuTru>(
        client.PUT("/api/v1/residency-records/{recordId}", {
          params: { path: { recordId: hoSo.maHoSo } },
          body: {
            vaiTroCuTru: values.vaiTroCuTru as VaiTroCuTru,
            tinhTrangCuTru: values.tinhTrangCuTru as TinhTrangCuTru,
            ngayBatDau: values.ngayBatDau,
            ngayKetThuc: values.ngayKetThuc.trim() || null,
            trangThaiHoSo: "Hợp lệ",
          },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    lamMoiSauThayDoiCuTru(queryClient, hoSo.maCanHo);
    notifySuccess("Đã cập nhật hồ sơ cư trú");
    setOpen(false);
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset(giaTriTuHoSo(hoSo));
      }}
    >
      <DialogTrigger asChild>
        <Button type="button" variant="outline" size="sm" aria-label={`Sửa hồ sơ cư trú của ${tenCuDan}`}>
          Sửa
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Sửa hồ sơ cư trú</DialogTitle>
          <DialogDescription>
            {tenCuDan} ({hoSo.maCuDan}) tại căn hộ {hoSo.maCanHo}. Căn hộ và cư dân giữ nguyên.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={onSubmit}
          noValidate
          aria-label={`Sửa hồ sơ cư trú của ${tenCuDan}`}
          className="flex flex-col gap-5"
        >
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="vaiTroCuTru"
              label="Vai trò cư trú"
              required
              render={(field) => (
                <SelectControl field={field}>
                  {VAI_TRO_CU_TRU.map((vt) => (
                    <SelectItem key={vt} value={vt}>
                      {vt}
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
            />
            <FormField
              control={form.control}
              name="tinhTrangCuTru"
              label="Tình trạng cư trú"
              required
              render={(field) => (
                <SelectControl field={field}>
                  {TINH_TRANG_CU_TRU.map((tt) => (
                    <SelectItem key={tt} value={tt}>
                      {tt}
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
            />
            <FormField
              control={form.control}
              name="ngayBatDau"
              label="Ngày bắt đầu"
              required
              render={(field) => <Input type="date" {...field} />}
            />
            <FormField
              control={form.control}
              name="ngayKetThuc"
              label="Ngày kết thúc"
              required={tinhTrang === "Tạm trú"}
              description={
                tinhTrang === "Tạm trú" ? "Bắt buộc với hồ sơ tạm trú" : "Để trống nếu cư trú không thời hạn"
              }
              render={(field) => <Input type="date" {...field} />}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>Lưu thay đổi</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
