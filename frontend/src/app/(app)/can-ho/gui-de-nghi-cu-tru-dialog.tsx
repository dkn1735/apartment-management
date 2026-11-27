"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useId, useMemo, useState } from "react";
import { Controller, useWatch } from "react-hook-form";
import * as z from "zod";
import { notifySuccess } from "@/components/feedback/notify";
import { SelectControl } from "@/components/form/field-controls";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { FIELD_NAME_ATTR, useApiForm } from "@/components/form/use-api-form";
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
import { FieldError, FieldGroup, FieldLegend, FieldSet } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { SelectItem } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "cn";
import { api, client } from "@/lib/api/api";
import { doDaiLyDo, LY_DO_TOI_DA } from "@/lib/can-ho";
import {
  lamMoiSauThayDoiCuTru,
  TINH_TRANG_CU_TRU,
  VAI_TRO_DE_NGHI_CU_TRU,
  type HoSoCuTru,
  type TinhTrangCuTru,
  type VaiTroDeNghiCuTru,
} from "@/lib/cu-tru";
import { ngayHomNay } from "@/lib/format";

function chuanHoaCccd(raw: string): string {
  return raw.replace(/[\s-]+/g, "");
}

function chuanHoaSdt(raw: string): string {
  const gon = raw.replace(/[\s.-]+/g, "");
  if (/^\+84\d+$/.test(gon)) return `0${gon.slice(3)}`;
  if (/^84\d+$/.test(gon)) return `0${gon.slice(2)}`;
  return gon;
}

function taoSchema() {
  return z
    .object({
      laChinhToi: z.boolean(),
      soCccd: z.string(),
      soDienThoai: z.string(),
      vaiTroCuTru: z.string().min(1, "Vui lòng chọn vai trò cư trú"),
      tinhTrangCuTru: z.string().min(1, "Vui lòng chọn tình trạng cư trú"),
      ngayBatDau: z.string().min(1, "Vui lòng chọn ngày bắt đầu"),
      ngayKetThuc: z.string(),
      ghiChuDeNghi: z
        .string()
        .refine((v) => doDaiLyDo(v) <= LY_DO_TOI_DA, `Ghi chú đề nghị tối đa ${LY_DO_TOI_DA} ký tự`),
    })
    .superRefine((values, ctx) => {
      if (!values.laChinhToi) {
        const cccd = chuanHoaCccd(values.soCccd);
        if (!cccd) {
          ctx.addIssue({ code: "custom", path: ["soCccd"], message: "Vui lòng nhập số CCCD" });
        } else if (!/^[0-9]{12}$/.test(cccd)) {
          ctx.addIssue({ code: "custom", path: ["soCccd"], message: "Số CCCD phải gồm đúng 12 chữ số" });
        }

        const sdt = chuanHoaSdt(values.soDienThoai);
        if (!sdt) {
          ctx.addIssue({ code: "custom", path: ["soDienThoai"], message: "Vui lòng nhập số điện thoại" });
        } else if (!/^0[0-9]{9}$/.test(sdt)) {
          ctx.addIssue({ code: "custom", path: ["soDienThoai"], message: "Số điện thoại không hợp lệ" });
        }
      }

      const homNay = ngayHomNay();
      if (values.ngayBatDau && values.ngayBatDau < homNay) {
        ctx.addIssue({
          code: "custom",
          path: ["ngayBatDau"],
          message: "Ngày bắt đầu của đề nghị phải từ hôm nay trở đi",
        });
      }

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
}

type Values = z.output<ReturnType<typeof taoSchema>>;

function giaTriMacDinh(): Values {
  return {
    laChinhToi: false,
    soCccd: "",
    soDienThoai: "",
    vaiTroCuTru: "",
    tinhTrangCuTru: "",
    ngayBatDau: ngayHomNay(),
    ngayKetThuc: "",
    ghiChuDeNghi: "",
  };
}

/**
 * Hộp thoại cư dân gửi đề nghị cư trú cho người thân, khách thuê hoặc chính mình (spec 0015, AC-1, AC-2, AC-3, AC-15, AC-18).
 */
export function GuiDeNghiCuTruDialog({ maCanHo, onThanhCong }: { maCanHo: string; onThanhCong?: () => void }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const schema = useMemo(() => taoSchema(), []);
  const form = useApiForm({ schema, defaultValues: giaTriMacDinh() });
  const laChinhToi = useWatch({ control: form.control, name: "laChinhToi" });
  const tinhTrang = useWatch({ control: form.control, name: "tinhTrangCuTru" });

  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<HoSoCuTru>(
        client.POST("/api/v1/residency-records/resident-request", {
          body: {
            maCanHo,
            // OpenAPI xuất JsonNode thành string nhưng backend yêu cầu boolean JSON (AC-2)
            laChinhToi: values.laChinhToi as unknown as string,
            ...(values.laChinhToi
              ? {}
              : {
                  soCccd: values.soCccd.trim(),
                  soDienThoai: values.soDienThoai.trim(),
                }),
            vaiTroCuTru: values.vaiTroCuTru as VaiTroDeNghiCuTru,
            tinhTrangCuTru: values.tinhTrangCuTru as TinhTrangCuTru,
            ngayBatDau: values.ngayBatDau,
            ngayKetThuc: values.ngayKetThuc.trim() || null,
            ghiChuDeNghi: values.ghiChuDeNghi.normalize("NFC").trim() || null,
          },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    lamMoiSauThayDoiCuTru(queryClient, maCanHo);
    notifySuccess("Đã gửi đề nghị cư trú");
    setOpen(false);
    onThanhCong?.();
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset(giaTriMacDinh());
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <Plus aria-hidden="true" />
          Gửi đề nghị cư trú
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Gửi đề nghị cư trú căn hộ {maCanHo}</DialogTitle>
          <DialogDescription>
            Đề nghị thêm thành viên gia đình, khách thuê hoặc đăng ký đợt cư trú mới tại căn hộ {maCanHo}. Hồ sơ ở trạng
            thái Chờ xác minh cho tới khi Ban quản lý duyệt.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={onSubmit}
          noValidate
          aria-label={`Gửi đề nghị cư trú căn hộ ${maCanHo}`}
          className="flex flex-col gap-5"
        >
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <Controller
              control={form.control}
              name="laChinhToi"
              render={({ field, fieldState }) => (
                <ChonDoiTuong
                  value={field.value}
                  error={fieldState.error?.message}
                  onBlur={field.onBlur}
                  onChange={(next) => {
                    field.onChange(next);
                    if (next) form.clearErrors(["soCccd", "soDienThoai"]);
                  }}
                />
              )}
            />
            {!laChinhToi && (
              <>
                <FormField
                  control={form.control}
                  name="soCccd"
                  label="Số CCCD"
                  description="12 chữ số căn cước công dân gắn với tài khoản cư dân"
                  required
                  render={(field) => <Input inputMode="numeric" autoComplete="off" {...field} />}
                />
                <FormField
                  control={form.control}
                  name="soDienThoai"
                  label="Số điện thoại"
                  description="Số điện thoại gắn đồng thời với tài khoản cư dân đó"
                  required
                  render={(field) => <Input type="tel" autoComplete="off" {...field} />}
                />
              </>
            )}
            <FormField
              control={form.control}
              name="vaiTroCuTru"
              label="Vai trò cư trú"
              required
              render={(field) => (
                <SelectControl field={field} placeholder="Chọn vai trò cư trú">
                  {VAI_TRO_DE_NGHI_CU_TRU.map((vt) => (
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
                <SelectControl field={field} placeholder="Chọn tình trạng cư trú">
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
            <FormField
              control={form.control}
              name="ghiChuDeNghi"
              label="Ghi chú đề nghị"
              description="Tùy chọn, tối đa 500 ký tự"
              render={(field) => <Textarea rows={3} {...field} />}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>Gửi đề nghị</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ChonDoiTuong({
  value,
  error,
  onChange,
  onBlur,
}: {
  value: boolean;
  error?: string;
  onChange(next: boolean): void;
  onBlur(): void;
}) {
  const id = useId();
  const loiId = `${id}-loi`;
  const luaChon: readonly { label: string; moTa: string; giaTri: boolean }[] = [
    { label: "Người khác", moTa: "Thành viên gia đình hoặc khách thuê đã có tài khoản", giaTri: false },
    { label: "Chính tôi", moTa: "Đăng ký đợt cư trú mới cho tài khoản của bạn", giaTri: true },
  ];

  return (
    <FieldSet
      className="flex flex-col gap-2"
      aria-describedby={error ? loiId : undefined}
      aria-invalid={error ? true : undefined}
      {...{ [FIELD_NAME_ATTR]: "laChinhToi" }}
    >
      <FieldLegend className="type-ui font-medium">
        Đối tượng cư trú <span aria-hidden="true">*</span>
        <span className="sr-only"> (bắt buộc)</span>
      </FieldLegend>
      <div className="grid gap-2 sm:grid-cols-2">
        {luaChon.map((muc) => {
          const checked = value === muc.giaTri;
          const radioId = `${id}-${String(muc.giaTri)}`;
          return (
            <label
              key={String(muc.giaTri)}
              htmlFor={radioId}
              className={cn(
                "flex cursor-pointer items-start gap-2.5 rounded-md border p-3 transition-colors duration-150 ease-out",
                checked ? "border-primary bg-primary-soft" : "border-input bg-card hover:bg-accent",
              )}
            >
              <input
                id={radioId}
                type="radio"
                name={`${id}-laChinhToi`}
                checked={checked}
                onBlur={onBlur}
                onChange={() => onChange(muc.giaTri)}
                className="mt-1 size-4 shrink-0 accent-primary"
              />
              <span className="flex flex-col gap-0.5 type-ui font-medium text-foreground">
                {muc.label}
                <span className="type-caption font-normal text-muted-foreground">{muc.moTa}</span>
              </span>
            </label>
          );
        })}
      </div>
      <FieldError id={loiId}>{error}</FieldError>
    </FieldSet>
  );
}
