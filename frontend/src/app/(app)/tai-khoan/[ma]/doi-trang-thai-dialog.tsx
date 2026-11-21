"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
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
import { Textarea } from "@/components/ui/textarea";
import { api, client } from "@/lib/api/api";
import { khoaChiTiet, type TaiKhoanChiTiet, type TrangThaiTaiKhoan } from "@/lib/tai-khoan";

type DoiTrangThaiDialogProps = {
  taiKhoan: TaiKhoanChiTiet;
  trangThaiMoi: TrangThaiTaiKhoan;
  trigger: ReactNode;
  title: string;
  description: string;
  confirmLabel: string;
  tone?: "default" | "destructive";
  /** Gọi sau khi đổi thành công; nút mở hộp thoại đã biến mất nên nơi gọi chọn chỗ đặt focus. */
  onFocusSauKhiDoi?: () => void;
};

/**
 * Hộp thoại đổi trạng thái (spec 0008, AC-10, AC-17) có ô lý do: bắt buộc khi khóa hay ngừng hoạt động, không bắt
 * buộc khi mở lại. Không gửi `phienBan`: lệnh một trường, ghi đè là đúng ý.
 */
export function DoiTrangThaiDialog({
  taiKhoan,
  trangThaiMoi,
  trigger,
  title,
  description,
  confirmLabel,
  tone = "default",
  onFocusSauKhiDoi,
}: DoiTrangThaiDialogProps) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const batBuoc = trangThaiMoi !== "Hoạt động";
  const schema = z.object({
    lyDo: z
      .string()
      .trim()
      .max(500, "Lý do tối đa 500 ký tự")
      .refine((v) => !batBuoc || v.length > 0, "Vui lòng nhập lý do"),
  });
  const form = useApiForm({ schema, defaultValues: { lyDo: "" } });
  const mutation = useMutation({
    mutationFn: (lyDo: string) =>
      api<TaiKhoanChiTiet>(
        client.PUT("/api/v1/accounts/{maNguoiDung}/status", {
          params: { path: { maNguoiDung: taiKhoan.maNguoiDung } },
          body: { trangThai: trangThaiMoi, lyDo: lyDo || undefined },
        }),
      ),
  });

  const onSubmit = form.submitWith(async ({ lyDo }) => {
    const data = await mutation.mutateAsync(lyDo);
    queryClient.setQueryData(khoaChiTiet(taiKhoan.maNguoiDung), data);
    void queryClient.invalidateQueries({ queryKey: ["tai-khoan", "danh-sach"] });
    notifySuccess(`Tài khoản đã chuyển sang ${trangThaiMoi}`);
    setOpen(false);
    // Trạng thái mới làm nút mở hộp thoại này biến mất (cả hộp thoại cũng gỡ khỏi cây), nên nơi gọi đặt focus
    onFocusSauKhiDoi?.();
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset({ lyDo: "" });
      }}
    >
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label={title} className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FormField
            control={form.control}
            name="lyDo"
            label="Lý do"
            description={batBuoc ? "Ghi vào nhật ký hệ thống, tối đa 500 ký tự" : "Không bắt buộc"}
            required={batBuoc}
            render={(field) => <Textarea rows={3} {...field} />}
          />
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form} variant={tone === "destructive" ? "destructive" : "primary"}>
              {confirmLabel}
            </SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
