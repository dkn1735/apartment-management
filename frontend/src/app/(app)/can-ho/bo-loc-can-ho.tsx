"use client";

import { useQuery } from "@tanstack/react-query";
import { useEffect, useId } from "react";
import { Controller, useWatch } from "react-hook-form";
import * as z from "zod";
import { SelectControl } from "@/components/form/field-controls";
import { FormField } from "@/components/form/form-field";
import { FIELD_NAME_ATTR, useApiForm } from "@/components/form/use-api-form";
import { SubmitButton } from "@/components/form/submit-button";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { FieldError, FieldLegend, FieldSet } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SelectItem } from "@/components/ui/select";
import { api, client, type ApiError } from "@/lib/api/api";
import { chuanHoaDienTich, DIEN_TICH_LOC, TRANG_THAI_SU_DUNG, type TrangThaiSuDung } from "@/lib/can-ho";
import { khoaChiTietToaNha, type ToaNha } from "@/lib/toa-nha";

/** Bộ lọc nằm trên URL với đúng tên tham số của API (spec 0012, AC-17); thiếu là không lọc. */
export type BoLoc = {
  maToaNha?: string;
  soTang?: string;
  dienTichTu?: string;
  dienTichDen?: string;
  trangThaiSuDung: string[];
  q?: string;
  /** Lọc cờ tạm dừng tiện ích (spec 0013, AC-16). */
  biTamDungTienIch?: "true" | "false";
};

export const THAM_SO_LOC = [
  "maToaNha",
  "soTang",
  "dienTichTu",
  "dienTichDen",
  "trangThaiSuDung",
  "q",
  "biTamDungTienIch",
] as const;

const TAT_CA = "TAT_CA";
const SAI_DIEN_TICH = "Diện tích lọc phải từ 0 đến 1000, tối đa 2 chữ số thập phân";

const dienTichLoc = z.string().refine((v) => {
  const gon = chuanHoaDienTich(v);
  return gon === "" || (DIEN_TICH_LOC.test(gon) && Number(gon) <= 1000);
}, SAI_DIEN_TICH);

/** Cùng quy tắc và câu báo lỗi với backend (AC-3), để lỗi hiện dưới ô trước khi gọi API. */
const schema = z
  .object({
    maToaNha: z.string(),
    soTang: z.string(),
    dienTichTu: dienTichLoc,
    dienTichDen: dienTichLoc,
    trangThaiSuDung: z.array(z.string()),
    q: z.string().trim().max(20, "Từ khóa tìm kiếm tối đa 20 ký tự"),
    biTamDungTienIch: z.string(),
  })
  .refine(
    (v) => {
      const tu = chuanHoaDienTich(v.dienTichTu);
      const den = chuanHoaDienTich(v.dienTichDen);
      return !tu || !den || !DIEN_TICH_LOC.test(tu) || !DIEN_TICH_LOC.test(den) || Number(tu) <= Number(den);
    },
    { path: ["dienTichDen"], message: "Diện tích đến phải lớn hơn hoặc bằng diện tích từ" },
  );

type Values = z.output<typeof schema>;

/**
 * Đọc bộ lọc từ URL; chuỗi rỗng coi như không gửi. `soTang` không có `maToaNha` thì bỏ (AC-17). Giá trị lạ của
 * `biTamDungTienIch` thì bỏ (spec 0013, AC-16).
 */
export function docBoLoc(searchParams: URLSearchParams): BoLoc {
  const lay = (ten: string) => searchParams.get(ten)?.trim() || undefined;
  const maToaNha = lay("maToaNha");
  return {
    maToaNha,
    soTang: maToaNha ? lay("soTang") : undefined,
    dienTichTu: lay("dienTichTu"),
    dienTichDen: lay("dienTichDen"),
    trangThaiSuDung: searchParams.getAll("trangThaiSuDung").filter((v) => v.trim() !== ""),
    q: lay("q"),
    biTamDungTienIch: docTienIch(lay("biTamDungTienIch")),
  };
}

function docTienIch(giaTri: string | undefined): BoLoc["biTamDungTienIch"] {
  return giaTri === "true" || giaTri === "false" ? giaTri : undefined;
}

export function dangLoc(boLoc: BoLoc): boolean {
  return THAM_SO_LOC.some((ten) => (ten === "trangThaiSuDung" ? boLoc[ten].length > 0 : Boolean(boLoc[ten])));
}

type BoLocCanHoProps = {
  boLoc: BoLoc;
  toaNha: readonly ToaNha[];
  /** Lỗi 400 của API khi bộ lọc trên URL bị sửa tay: gắn vào đúng ô. */
  loiServer?: ApiError | null;
  /** Bộ lọc mới đã hợp lệ; trang gọi để ghi lên URL và về trang 1. */
  onApDung(boLoc: BoLoc): void;
};

/**
 * Thanh lọc căn hộ (spec 0012, AC-17, AC-21). Trang đặt `key` theo bộ lọc trên URL để form lấy lại giá trị khi URL
 * đổi. Đổi tòa thì bỏ tầng; ô tầng chỉ bật khi đã chọn tòa và có "Tầng 1" đến "Tầng N" của tòa đó.
 */
export function BoLocCanHo({ boLoc, toaNha, loiServer, onApDung }: BoLocCanHoProps) {
  const form = useApiForm({
    schema,
    defaultValues: {
      maToaNha: boLoc.maToaNha ?? TAT_CA,
      soTang: boLoc.soTang ?? TAT_CA,
      dienTichTu: boLoc.dienTichTu ?? "",
      dienTichDen: boLoc.dienTichDen ?? "",
      trangThaiSuDung: boLoc.trangThaiSuDung,
      q: boLoc.q ?? "",
      biTamDungTienIch: boLoc.biTamDungTienIch ?? TAT_CA,
    },
  });
  const { setError } = form;
  const maToaNha = useWatch({ control: form.control, name: "maToaNha" });
  const toaDaChon = maToaNha && maToaNha !== TAT_CA ? maToaNha : undefined;

  const chiTietToa = useQuery<ToaNha, ApiError>({
    queryKey: khoaChiTietToaNha(toaDaChon ?? ""),
    queryFn: () =>
      api<ToaNha>(client.GET("/api/v1/buildings/{buildingId}", { params: { path: { buildingId: toaDaChon! } } })),
    enabled: Boolean(toaDaChon),
  });
  const soTangToiDa = toaDaChon ? (chiTietToa.data?.soTang ?? 0) : 0;

  useEffect(() => {
    for (const loi of loiServer?.errors ?? []) {
      if (loi.field && (THAM_SO_LOC as readonly string[]).includes(loi.field)) {
        setError(loi.field as keyof Values, { type: "server", message: loi.message });
      }
    }
  }, [loiServer, setError]);

  // Tòa trên URL không có trong danh sách thì vẫn hiện mã, để người dùng thấy đang lọc theo gì
  const luaChonToa = toaNha.map((t) => ({ value: t.maToaNha, label: t.tenToaNha }));
  if (boLoc.maToaNha && !luaChonToa.some((lc) => lc.value === boLoc.maToaNha)) {
    luaChonToa.push({ value: boLoc.maToaNha, label: boLoc.maToaNha });
  }

  return (
    <form
      role="search"
      aria-label="Lọc căn hộ"
      noValidate
      className="grid gap-4 rounded-md border bg-card p-4 sm:grid-cols-2 lg:grid-cols-3"
      onSubmit={form.submitWith(async (values) => {
        const toa = values.maToaNha === TAT_CA ? undefined : values.maToaNha;
        onApDung({
          maToaNha: toa,
          soTang: toa && values.soTang !== TAT_CA ? values.soTang : undefined,
          dienTichTu: chuanHoaDienTich(values.dienTichTu) || undefined,
          dienTichDen: chuanHoaDienTich(values.dienTichDen) || undefined,
          trangThaiSuDung: TRANG_THAI_SU_DUNG.filter((tt) => values.trangThaiSuDung.includes(tt)),
          q: values.q.trim() || undefined,
          biTamDungTienIch: docTienIch(values.biTamDungTienIch),
        });
      })}
    >
      <FormField
        control={form.control}
        name="maToaNha"
        label="Tòa nhà"
        render={(field) => (
          <SelectControl
            field={{
              ...field,
              onChange: (giaTri: string) => {
                field.onChange(giaTri);
                form.setValue("soTang", TAT_CA);
              },
            }}
          >
            <SelectItem value={TAT_CA}>Tất cả tòa nhà</SelectItem>
            {luaChonToa.map((lc) => (
              <SelectItem key={lc.value} value={lc.value}>
                {lc.label}
              </SelectItem>
            ))}
          </SelectControl>
        )}
      />
      <FormField
        control={form.control}
        name="soTang"
        label="Tầng"
        description={toaDaChon ? undefined : "Chọn tòa nhà trước khi lọc theo tầng"}
        render={(field) => <ChonTang field={field} disabled={!toaDaChon} soTangToiDa={soTangToiDa} />}
      />
      <FormField
        control={form.control}
        name="q"
        label="Mã căn hộ"
        description="Một phần mã, ví dụ A-01 hoặc 03"
        render={(field) => <Input type="search" autoComplete="off" spellCheck={false} maxLength={40} {...field} />}
      />
      <FormField
        control={form.control}
        name="dienTichTu"
        label="Diện tích từ (m²)"
        render={(field) => <Input inputMode="decimal" autoComplete="off" {...field} />}
      />
      <FormField
        control={form.control}
        name="dienTichDen"
        label="Diện tích đến (m²)"
        render={(field) => <Input inputMode="decimal" autoComplete="off" {...field} />}
      />
      <FormField
        control={form.control}
        name="biTamDungTienIch"
        label="Tiện ích"
        render={(field) => (
          <SelectControl field={field}>
            <SelectItem value={TAT_CA}>Tất cả</SelectItem>
            <SelectItem value="true">Đang tạm dừng</SelectItem>
            <SelectItem value="false">Bình thường</SelectItem>
          </SelectControl>
        )}
      />
      <Controller
        control={form.control}
        name="trangThaiSuDung"
        render={({ field, fieldState }) => (
          <NhomTrangThai
            giaTri={field.value}
            loi={fieldState.error?.message}
            onChange={field.onChange}
            onBlur={field.onBlur}
          />
        )}
      />
      <div className="flex flex-wrap gap-2 sm:col-span-2 lg:col-span-3">
        <SubmitButton form={form}>Lọc</SubmitButton>
        {dangLoc(boLoc) && (
          <Button type="button" variant="outline" onClick={() => onApDung({ trangThaiSuDung: [] })}>
            Xóa bộ lọc
          </Button>
        )}
      </div>
    </form>
  );
}

function ChonTang({
  field,
  disabled,
  soTangToiDa,
}: {
  field: Parameters<typeof SelectControl>[0]["field"];
  disabled: boolean;
  soTangToiDa: number;
}) {
  const daChon = typeof field.value === "string" && field.value !== TAT_CA ? Number(field.value) : 0;
  // Tầng trên URL lớn hơn số tầng của tòa vẫn hiện, để người dùng thấy đang lọc theo gì
  const soLuaChon = Math.max(soTangToiDa, Number.isInteger(daChon) ? daChon : 0);
  return (
    <fieldset disabled={disabled} className="contents">
      <SelectControl field={field}>
        <SelectItem value={TAT_CA}>Tất cả tầng</SelectItem>
        {Array.from({ length: soLuaChon }, (_, i) => (
          <SelectItem key={i + 1} value={String(i + 1)}>
            Tầng {i + 1}
          </SelectItem>
        ))}
      </SelectControl>
    </fieldset>
  );
}

function NhomTrangThai({
  giaTri,
  loi,
  onChange,
  onBlur,
}: {
  giaTri: string[];
  loi?: string;
  onChange(giaTri: string[]): void;
  onBlur(): void;
}) {
  const id = useId();
  const loiId = `${id}-loi`;
  return (
    <FieldSet
      className="flex flex-col gap-2"
      aria-describedby={loi ? loiId : undefined}
      aria-invalid={loi ? true : undefined}
      {...{ [FIELD_NAME_ATTR]: "trangThaiSuDung" }}
    >
      <FieldLegend className="type-ui font-medium">Trạng thái sử dụng</FieldLegend>
      <div className="flex flex-wrap gap-x-4 gap-y-2">
        {TRANG_THAI_SU_DUNG.map((tt: TrangThaiSuDung) => {
          const oId = `${id}-${tt}`;
          return (
            <div key={tt} className="flex items-center gap-2">
              <Checkbox
                id={oId}
                checked={giaTri.includes(tt)}
                onBlur={onBlur}
                onCheckedChange={(chon) => onChange(chon === true ? [...giaTri, tt] : giaTri.filter((v) => v !== tt))}
              />
              <Label htmlFor={oId} className="font-normal">
                {tt}
              </Label>
            </div>
          );
        })}
      </div>
      <FieldError id={loiId}>{loi}</FieldError>
    </FieldSet>
  );
}
