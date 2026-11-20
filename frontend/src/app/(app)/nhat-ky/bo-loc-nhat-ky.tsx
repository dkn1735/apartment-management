"use client";

import * as z from "zod";
import { SelectControl } from "@/components/form/field-controls";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { SelectItem } from "@/components/ui/select";

/** Bộ lọc nằm trên URL (spec 0007, AC-12); thiếu là không lọc. */
export type BoLoc = { maNguoiDung?: string; loaiThaoTac?: string; tuNgay?: string; denNgay?: string };

export const THAM_SO_LOC = ["maNguoiDung", "loaiThaoTac", "tuNgay", "denNgay"] as const;

export type LoaiThaoTac = { ma: string; nhan: string };

const TAT_CA = "TAT_CA";
const NGAY = /^\d{4}-\d{2}-\d{2}$/;
const SAI_NGAY = "Ngày phải có dạng yyyy-MM-dd";

/** Cùng quy tắc và câu báo lỗi với backend, để lỗi hiện ngay dưới ô thay vì sau một lần gọi API. */
const schema = z
  .object({
    maNguoiDung: z
      .string()
      .trim()
      .refine((v) => v === "" || /^ND\d{8}$/.test(v), "Mã người dùng không đúng định dạng"),
    loaiThaoTac: z.string(),
    tuNgay: z.string().refine((v) => v === "" || NGAY.test(v), SAI_NGAY),
    denNgay: z.string().refine((v) => v === "" || NGAY.test(v), SAI_NGAY),
  })
  .refine((v) => !v.tuNgay || !v.denNgay || v.tuNgay <= v.denNgay, {
    path: ["denNgay"],
    message: "Đến ngày phải bằng hoặc sau từ ngày",
  });

/** Đọc bộ lọc từ URL; chuỗi rỗng coi như không gửi. */
export function docBoLoc(searchParams: URLSearchParams): BoLoc {
  const boLoc: BoLoc = {};
  for (const ten of THAM_SO_LOC) {
    const giaTri = searchParams.get(ten);
    if (giaTri) boLoc[ten] = giaTri;
  }
  return boLoc;
}

type BoLocNhatKyProps = {
  boLoc: BoLoc;
  loaiThaoTac: readonly LoaiThaoTac[];
  /** Bộ lọc mới đã hợp lệ; trang gọi để ghi lên URL và về trang 1. */
  onApDung(boLoc: BoLoc): void;
};

/**
 * Form lọc nhật ký. Trang đặt `key` theo bộ lọc trên URL để form lấy lại giá trị khi URL đổi (Back, Xóa bộ lọc).
 */
export function BoLocNhatKy({ boLoc, loaiThaoTac, onApDung }: BoLocNhatKyProps) {
  const form = useApiForm({
    schema,
    defaultValues: {
      maNguoiDung: boLoc.maNguoiDung ?? "",
      loaiThaoTac: boLoc.loaiThaoTac ?? TAT_CA,
      tuNgay: boLoc.tuNgay ?? "",
      denNgay: boLoc.denNgay ?? "",
    },
  });
  const dangLoc = THAM_SO_LOC.some((ten) => boLoc[ten]);

  return (
    <form
      role="search"
      aria-label="Lọc nhật ký"
      noValidate
      className="grid gap-4 rounded-md border bg-card p-4 sm:grid-cols-2 lg:grid-cols-4"
      onSubmit={form.submitWith(async (values) => {
        onApDung({
          maNguoiDung: values.maNguoiDung || undefined,
          loaiThaoTac: values.loaiThaoTac === TAT_CA ? undefined : values.loaiThaoTac,
          tuNgay: values.tuNgay || undefined,
          denNgay: values.denNgay || undefined,
        });
      })}
    >
      <FormField
        control={form.control}
        name="maNguoiDung"
        label="Mã người dùng"
        description="Ví dụ ND00000001"
        render={(field) => <Input autoCapitalize="characters" spellCheck={false} {...field} />}
      />
      <FormField
        control={form.control}
        name="loaiThaoTac"
        label="Loại thao tác"
        render={(field) => (
          <SelectControl field={field}>
            <SelectItem value={TAT_CA}>Tất cả loại thao tác</SelectItem>
            {loaiThaoTac.map((loai) => (
              <SelectItem key={loai.ma} value={loai.ma}>
                {loai.nhan}
              </SelectItem>
            ))}
          </SelectControl>
        )}
      />
      <FormField
        control={form.control}
        name="tuNgay"
        label="Từ ngày"
        render={(field) => <Input type="date" {...field} />}
      />
      <FormField
        control={form.control}
        name="denNgay"
        label="Đến ngày"
        render={(field) => <Input type="date" {...field} />}
      />
      <div className="flex flex-wrap gap-2 sm:col-span-2 lg:col-span-4">
        <SubmitButton form={form}>Lọc</SubmitButton>
        {dangLoc && (
          <Button type="button" variant="outline" onClick={() => onApDung({})}>
            Xóa bộ lọc
          </Button>
        )}
      </div>
    </form>
  );
}
