"use client";

import { useQuery } from "@tanstack/react-query";
import { useEffect, useId } from "react";
import { Controller, useWatch } from "react-hook-form";
import * as z from "zod";
import { SelectControl } from "@/components/form/field-controls";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { FIELD_NAME_ATTR, useApiForm } from "@/components/form/use-api-form";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { FieldError, FieldLegend, FieldSet } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SelectItem } from "@/components/ui/select";
import { api, client, type ApiError } from "@/lib/api/api";
import { TINH_TRANG_CU_TRU, TRANG_THAI_HO_SO, VAI_TRO_CU_TRU, type TrangThaiHoSo } from "@/lib/cu-tru";
import { khoaChiTietToaNha, type ToaNha } from "@/lib/toa-nha";

/** Bộ lọc nằm trên URL với đúng tên tham số của `GET /api/v1/residency-records` (spec 0015, AC-9, AC-10, AC-16). */
export type BoLocHoSoCuTruValues = {
  maToaNha?: string;
  soTang?: string;
  trangThaiHoSo: string[];
  vaiTroCuTru?: string;
  tinhTrangCuTru?: string;
  q?: string;
};

export const THAM_SO_LOC_CU_TRU = [
  "maToaNha",
  "soTang",
  "trangThaiHoSo",
  "vaiTroCuTru",
  "tinhTrangCuTru",
  "q",
] as const;

const TAT_CA = "TAT_CA";

const schema = z.object({
  maToaNha: z.string(),
  soTang: z.string(),
  trangThaiHoSo: z.array(z.string()),
  vaiTroCuTru: z.string(),
  tinhTrangCuTru: z.string(),
  q: z.string().refine((v) => [...v.normalize("NFC").trim()].length <= 100, "Từ khóa tối đa 100 ký tự"),
});

type Values = z.output<typeof schema>;

/** Đọc bộ lọc từ URL; chuỗi rỗng sau cắt khoảng trắng tính như vắng (AC-10, AC-16). */
export function docBoLocHoSoCuTru(searchParams: URLSearchParams): BoLocHoSoCuTruValues {
  const lay = (ten: string) => searchParams.get(ten)?.normalize("NFC").trim() || undefined;
  return {
    maToaNha: lay("maToaNha"),
    soTang: lay("soTang"),
    trangThaiHoSo: searchParams
      .getAll("trangThaiHoSo")
      .map((v) => v.normalize("NFC").trim())
      .filter((v) => v !== ""),
    vaiTroCuTru: lay("vaiTroCuTru"),
    tinhTrangCuTru: lay("tinhTrangCuTru"),
    q: lay("q"),
  };
}

export function dangLocHoSoCuTru(boLoc: BoLocHoSoCuTruValues): boolean {
  return THAM_SO_LOC_CU_TRU.some((ten) =>
    ten === "trangThaiHoSo" ? boLoc.trangThaiHoSo.length > 0 : Boolean(boLoc[ten]),
  );
}

type BoLocHoSoCuTruProps = {
  boLoc: BoLocHoSoCuTruValues;
  toaNha: readonly ToaNha[];
  loiServer?: ApiError | null;
  onApDung(boLoc: BoLocHoSoCuTruValues): void;
};

/**
 * Thanh lọc hồ sơ cư trú toàn tòa (spec 0015, AC-16, AC-18).
 * Đổi hoặc bỏ tòa thì tự xóa tầng; ô tầng chỉ bật khi đã chọn tòa nhà.
 */
export function BoLocHoSoCuTru({ boLoc, toaNha, loiServer, onApDung }: BoLocHoSoCuTruProps) {
  const form = useApiForm({
    schema,
    defaultValues: {
      maToaNha: boLoc.maToaNha ?? TAT_CA,
      soTang: boLoc.soTang ?? TAT_CA,
      trangThaiHoSo: boLoc.trangThaiHoSo,
      vaiTroCuTru: boLoc.vaiTroCuTru ?? TAT_CA,
      tinhTrangCuTru: boLoc.tinhTrangCuTru ?? TAT_CA,
      q: boLoc.q ?? "",
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
      if (loi.field && (THAM_SO_LOC_CU_TRU as readonly string[]).includes(loi.field)) {
        setError(loi.field as keyof Values, { type: "server", message: loi.message });
      }
    }
  }, [loiServer, setError]);

  const luaChonToa = toaNha.map((t) => ({ value: t.maToaNha, label: t.tenToaNha }));
  if (boLoc.maToaNha && !luaChonToa.some((lc) => lc.value === boLoc.maToaNha)) {
    luaChonToa.push({ value: boLoc.maToaNha, label: boLoc.maToaNha });
  }

  return (
    <form
      role="search"
      aria-label="Lọc hồ sơ cư trú"
      noValidate
      className="grid gap-4 rounded-md border bg-card p-4 sm:grid-cols-2 lg:grid-cols-4"
      onSubmit={form.submitWith(async (values) => {
        const toa = values.maToaNha === TAT_CA ? undefined : values.maToaNha;
        onApDung({
          maToaNha: toa,
          soTang: toa && values.soTang !== TAT_CA ? values.soTang : undefined,
          trangThaiHoSo: TRANG_THAI_HO_SO.filter((tt) => values.trangThaiHoSo.includes(tt)),
          vaiTroCuTru: values.vaiTroCuTru === TAT_CA ? undefined : values.vaiTroCuTru,
          tinhTrangCuTru: values.tinhTrangCuTru === TAT_CA ? undefined : values.tinhTrangCuTru,
          q: values.q.normalize("NFC").trim() || undefined,
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
        name="vaiTroCuTru"
        label="Vai trò cư trú"
        render={(field) => (
          <SelectControl field={field}>
            <SelectItem value={TAT_CA}>Tất cả vai trò</SelectItem>
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
        render={(field) => (
          <SelectControl field={field}>
            <SelectItem value={TAT_CA}>Tất cả tình trạng</SelectItem>
            {TINH_TRANG_CU_TRU.map((tt) => (
              <SelectItem key={tt} value={tt}>
                {tt}
              </SelectItem>
            ))}
          </SelectControl>
        )}
      />
      <div className="sm:col-span-2 lg:col-span-2">
        <FormField
          control={form.control}
          name="q"
          label="Từ khóa"
          description="Họ tên (có hoặc không dấu), SĐT, CCCD, email, mã cư dân, mã căn hoặc mã hồ sơ"
          render={(field) => <Input type="search" autoComplete="off" maxLength={100} {...field} />}
        />
      </div>
      <div className="sm:col-span-2 lg:col-span-2">
        <Controller
          control={form.control}
          name="trangThaiHoSo"
          render={({ field, fieldState }) => (
            <NhomTrangThaiHoSo
              giaTri={field.value}
              loi={fieldState.error?.message}
              onChange={field.onChange}
              onBlur={field.onBlur}
            />
          )}
        />
      </div>
      <div className="flex flex-wrap gap-2 sm:col-span-2 lg:col-span-4">
        <SubmitButton form={form}>Lọc</SubmitButton>
        {dangLocHoSoCuTru(boLoc) && (
          <Button type="button" variant="outline" onClick={() => onApDung({ trangThaiHoSo: [] })}>
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

function NhomTrangThaiHoSo({
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
      {...{ [FIELD_NAME_ATTR]: "trangThaiHoSo" }}
    >
      <FieldLegend className="type-ui font-medium">Trạng thái hồ sơ</FieldLegend>
      <div className="flex flex-wrap gap-x-4 gap-y-2">
        {TRANG_THAI_HO_SO.map((tt: TrangThaiHoSo) => {
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
