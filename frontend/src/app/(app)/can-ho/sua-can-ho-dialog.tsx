"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Pencil } from "lucide-react";
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
import { Input } from "@/components/ui/input";
import { SelectItem } from "@/components/ui/select";
import { api, client } from "@/lib/api/api";
import {
  CAN_HO_QUERY_KEY,
  dienTichGuiDi,
  dienTichNhap,
  TRANG_THAI_SU_DUNG,
  type CanHo,
  type TrangThaiSuDung,
} from "@/lib/can-ho";

// Diện tích để server kiểm, cùng câu lỗi (AC-12)
const schema = z.object({
  dienTich: z.string(),
  trangThaiSuDung: z.string().min(1, "Vui lòng chọn trạng thái sử dụng"),
});

type Values = z.output<typeof schema>;

/**
 * Nút và hộp thoại "Sửa" căn hộ (spec 0012, AC-19): hai ô đã điền sẵn giá trị hiện tại. Lỗi 400 hiện dưới ô; 404
 * hiện trong hộp thoại. Thành công thì làm mới chi tiết và danh sách căn.
 */
export function SuaCanHoDialog({ canHo }: { canHo: CanHo }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const giaTriDau: Values = { dienTich: dienTichNhap(canHo.dienTich), trangThaiSuDung: canHo.trangThaiSuDung };
  const form = useApiForm({ schema, defaultValues: giaTriDau });
  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<CanHo>(
        client.PUT("/api/v1/apartments/{apartmentId}", {
          params: { path: { apartmentId: canHo.maCanHo } },
          body: {
            dienTich: dienTichGuiDi(values.dienTich) as number,
            trangThaiSuDung: values.trangThaiSuDung as TrangThaiSuDung,
          },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    const moi = await mutation.mutateAsync(values);
    void queryClient.invalidateQueries({ queryKey: CAN_HO_QUERY_KEY });
    notifySuccess(`Đã cập nhật căn hộ ${moi.maCanHo}`);
    setOpen(false);
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset(giaTriDau);
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <Pencil aria-hidden="true" />
          Sửa
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Sửa căn hộ {canHo.maCanHo}</DialogTitle>
          <DialogDescription>
            Chỉ đổi được diện tích và trạng thái sử dụng. Mã căn và tầng giữ nguyên.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label={`Sửa căn hộ ${canHo.maCanHo}`} className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="dienTich"
              label="Diện tích (m²)"
              description="Lớn hơn 0, tối đa 1000, tối đa 2 chữ số thập phân"
              required
              render={(field) => <Input inputMode="decimal" autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="trangThaiSuDung"
              label="Trạng thái sử dụng"
              required
              render={(field) => (
                <SelectControl field={field}>
                  {TRANG_THAI_SU_DUNG.map((tt) => (
                    <SelectItem key={tt} value={tt}>
                      {tt}
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
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
