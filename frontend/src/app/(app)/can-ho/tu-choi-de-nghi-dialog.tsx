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
import { Textarea } from "@/components/ui/textarea";
import { api, client } from "@/lib/api/api";
import { doDaiLyDo, LY_DO_TOI_DA } from "@/lib/can-ho";
import { lamMoiSauThayDoiCuTru, type HoSoCuTru } from "@/lib/cu-tru";

const schema = z.object({
  lyDoTuChoi: z
    .string()
    .refine((v) => doDaiLyDo(v) > 0, "Vui lòng nhập lý do từ chối")
    .refine((v) => doDaiLyDo(v) <= LY_DO_TOI_DA, `Lý do từ chối tối đa ${LY_DO_TOI_DA} ký tự`),
});

type Values = z.output<typeof schema>;

/** Hộp thoại BQL nhập lý do từ chối đề nghị cư trú đang chờ xác minh (spec 0015, AC-5, AC-15, AC-17, AC-18). */
export function TuChoiDeNghiDialog({ hoSo }: { hoSo: HoSoCuTru }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const tenCuDan = hoSo.hoTen ?? hoSo.maCuDan;
  const form = useApiForm({ schema, defaultValues: { lyDoTuChoi: "" } });

  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<HoSoCuTru>(
        client.POST("/api/v1/residency-records/{recordId}/reject", {
          params: { path: { recordId: hoSo.maHoSo } },
          body: { lyDoTuChoi: values.lyDoTuChoi.normalize("NFC").trim() },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    lamMoiSauThayDoiCuTru(queryClient, hoSo.maCanHo);
    notifySuccess("Đã từ chối đề nghị cư trú");
    setOpen(false);
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset({ lyDoTuChoi: "" });
      }}
    >
      <DialogTrigger asChild>
        <Button type="button" variant="outline" size="sm" aria-label={`Từ chối đề nghị cư trú của ${tenCuDan}`}>
          Từ chối
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Từ chối đề nghị cư trú</DialogTitle>
          <DialogDescription>
            Từ chối đề nghị cư trú của {tenCuDan} tại căn hộ {hoSo.maCanHo}. Người gửi và người được đề nghị sẽ thấy lý
            do này trên trang căn hộ.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={onSubmit}
          noValidate
          aria-label={`Từ chối đề nghị cư trú của ${tenCuDan}`}
          className="flex flex-col gap-5"
        >
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="lyDoTuChoi"
              label="Lý do từ chối"
              description="Tối đa 500 ký tự"
              required
              render={(field) => <Textarea rows={3} {...field} />}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form} variant="destructive">
              Từ chối đề nghị
            </SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
