"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Ban, CircleAlert, CircleCheck } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import * as z from "zod";
import { notifySuccess } from "@/components/feedback/notify";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Alert, AlertTitle } from "@/components/ui/alert";
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
import { api, ApiError, client } from "@/lib/api/api";
import { CAN_HO_QUERY_KEY, doDaiLyDo, LY_DO_TOI_DA, type CanHo } from "@/lib/can-ho";

const schema = z.object({
  lyDo: z
    .string()
    .refine((v) => doDaiLyDo(v) > 0, "Vui lòng nhập lý do")
    .refine((v) => doDaiLyDo(v) <= LY_DO_TOI_DA, `Lý do tối đa ${LY_DO_TOI_DA} ký tự`),
});

type Values = z.output<typeof schema>;

/**
 * Nút và hộp thoại tạm dừng hoặc mở khóa tiện ích căn hộ (spec 0013, AC-15, AC-17). Chiều đổi chốt lúc mở hộp
 * thoại, để tiêu đề không đổi khi 409 làm mới trạng thái căn. Lỗi 400 hiện dưới ô lý do; 409 và 404 hiện trong
 * hộp thoại. Câu 409 chọn theo chiều đã yêu cầu (một mã lỗi, hai câu), không đọc `message` của response.
 */
export function DoiTienIchDialog({ canHo }: { canHo: CanHo }) {
  const [open, setOpen] = useState(false);
  const [tamDung, setTamDung] = useState(!canHo.biTamDungTienIch);
  const [khongDoi, setKhongDoi] = useState(0);
  const khungKhongDoi = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();
  const form = useApiForm({ schema, defaultValues: { lyDo: "" } });
  const conLai = LY_DO_TOI_DA - doDaiLyDo(form.watch("lyDo") ?? "");
  const hanhDong = tamDung ? "Tạm dừng" : "Mở khóa";
  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<CanHo>(
        client.PATCH("/api/v1/apartments/{apartmentId}/facility-suspension", {
          params: { path: { apartmentId: canHo.maCanHo } },
          body: { biTamDungTienIch: tamDung, lyDo: values.lyDo },
        }),
      ),
  });

  useEffect(() => {
    if (khongDoi > 0) khungKhongDoi.current?.focus();
  }, [khongDoi]);

  const onSubmit = form.submitWith(async (values) => {
    setKhongDoi(0);
    try {
      await mutation.mutateAsync(values);
    } catch (error) {
      if (error instanceof ApiError && error.errorCode === "CANHO_TIEN_ICH_KHONG_DOI") {
        void queryClient.invalidateQueries({ queryKey: CAN_HO_QUERY_KEY });
        setKhongDoi((n) => n + 1);
        return;
      }
      throw error;
    }
    void queryClient.invalidateQueries({ queryKey: CAN_HO_QUERY_KEY });
    notifySuccess(`Đã ${hanhDong.toLowerCase()} tiện ích căn hộ ${canHo.maCanHo}`);
    setOpen(false);
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) {
          setTamDung(!canHo.biTamDungTienIch);
          setKhongDoi(0);
          form.reset({ lyDo: "" });
        }
      }}
    >
      <DialogTrigger asChild>
        <Button variant={canHo.biTamDungTienIch ? "outline" : "destructive"}>
          {canHo.biTamDungTienIch ? <CircleCheck aria-hidden="true" /> : <Ban aria-hidden="true" />}
          {canHo.biTamDungTienIch ? "Mở khóa tiện ích" : "Tạm dừng tiện ích"}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {hanhDong} tiện ích căn hộ {canHo.maCanHo}
          </DialogTitle>
          <DialogDescription>
            {tamDung
              ? "Căn hộ sẽ không đặt được tiện ích cho tới khi được mở khóa."
              : "Căn hộ sẽ dùng lại tiện ích bình thường."}{" "}
            Cư dân của căn sẽ thấy lý do này.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={onSubmit}
          noValidate
          aria-label={`${hanhDong} tiện ích căn hộ ${canHo.maCanHo}`}
          className="flex flex-col gap-5"
        >
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          {khongDoi > 0 && (
            <Alert ref={khungKhongDoi} tone="danger" role="alert" tabIndex={-1}>
              <CircleAlert aria-hidden="true" />
              <AlertTitle>
                {tamDung ? "Căn hộ đang bị tạm dừng tiện ích." : "Căn hộ đang dùng tiện ích bình thường."} Đóng hộp
                thoại để xem trạng thái mới.
              </AlertTitle>
            </Alert>
          )}
          <FieldGroup>
            <FormField
              control={form.control}
              name="lyDo"
              label="Lý do"
              description={
                conLai >= 0 ? `Còn ${conLai}/${LY_DO_TOI_DA} ký tự` : `Vượt ${-conLai} ký tự so với ${LY_DO_TOI_DA}`
              }
              required
              render={(field) => <Textarea rows={4} {...field} />}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>{hanhDong}</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
