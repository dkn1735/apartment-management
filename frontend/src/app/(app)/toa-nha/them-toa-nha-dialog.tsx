"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
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
import { soTangGuiDi, TOA_NHA_QUERY_KEY, type ToaNha } from "@/lib/toa-nha";

// Chỉ kiểm ô rỗng ở trình duyệt, cùng câu với backend; định dạng, độ dài và số tầng để server kiểm (AC-2, AC-15)
const schema = z.object({
  maToaNha: z.string().trim().min(1, "Vui lòng nhập mã tòa nhà"),
  tenToaNha: z.string().trim().min(1, "Vui lòng nhập tên tòa nhà"),
  soTang: z.string(),
  diaChiKhuVuc: z.string().trim().min(1, "Vui lòng nhập địa chỉ khu vực"),
});

type Values = z.output<typeof schema>;

const GIA_TRI_DAU: Values = { maToaNha: "", tenToaNha: "", soTang: "", diaChiKhuVuc: "" };

/**
 * Nút và hộp thoại "Thêm tòa nhà" (spec 0011, AC-15). Lỗi 400 và 409 gắn vào đúng ô, focus về ô sai đầu tiên; thành
 * công thì đóng, báo toast và làm mới danh mục. Radix trả focus về nút mở khi hộp thoại đóng.
 */
export function ThemToaNhaDialog() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const form = useApiForm({ schema, defaultValues: GIA_TRI_DAU });
  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<ToaNha>(
        client.POST("/api/v1/buildings", {
          body: {
            maToaNha: values.maToaNha,
            tenToaNha: values.tenToaNha,
            // Chuỗi không phải chữ số vẫn gửi đi để server trả lỗi trường soTang
            soTang: soTangGuiDi(values.soTang) as number,
            diaChiKhuVuc: values.diaChiKhuVuc,
          },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    const toa = await mutation.mutateAsync(values);
    void queryClient.invalidateQueries({ queryKey: TOA_NHA_QUERY_KEY });
    notifySuccess(`Đã thêm tòa nhà ${toa.tenToaNha}`);
    setOpen(false);
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset(GIA_TRI_DAU);
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <Plus aria-hidden="true" />
          Thêm tòa nhà
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Thêm tòa nhà</DialogTitle>
          <DialogDescription>
            Hệ thống tạo sẵn đủ các tầng từ 1 tới số tầng bạn nhập. Mã và tên tòa không được trùng với tòa đã có.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label="Thêm tòa nhà" className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="maToaNha"
              label="Mã tòa nhà"
              description="1 đến 10 chữ cái không dấu hoặc chữ số, ví dụ B hoặc T2"
              required
              render={(field) => (
                <Input
                  autoComplete="off"
                  maxLength={20}
                  {...field}
                  onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                />
              )}
            />
            <FormField
              control={form.control}
              name="tenToaNha"
              label="Tên tòa nhà"
              required
              render={(field) => <Input autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="soTang"
              label="Số tầng"
              description="Từ 1 đến 99"
              required
              render={(field) => <Input inputMode="numeric" autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="diaChiKhuVuc"
              label="Địa chỉ khu vực"
              required
              render={(field) => <Input autoComplete="off" {...field} />}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>Thêm tòa nhà</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
