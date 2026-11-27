"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useWatch } from "react-hook-form";
import * as z from "zod";
import { StatusBadge } from "@/components/data/status-badge";
import { notifySuccess } from "@/components/feedback/notify";
import { SelectControl } from "@/components/form/field-controls";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Button } from "@/components/ui/button";
import { Combobox } from "@/components/ui/combobox";
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
import { api, client, type ApiError } from "@/lib/api/api";
import { messageFor } from "@/lib/api/error-messages";
import type { PageResponse } from "@/lib/api/page";
import { CAN_HO_QUERY_KEY, toneTrangThai, type CanHo } from "@/lib/can-ho";
import {
  CU_DAN_LUA_CHON_QUERY_KEY,
  lamMoiSauThayDoiCuTru,
  TINH_TRANG_CU_TRU,
  VAI_TRO_CU_TRU,
  type CuDanLuaChon,
  type HoSoCuTru,
  type TinhTrangCuTru,
  type VaiTroCuTru,
} from "@/lib/cu-tru";
import { ngayHomNay } from "@/lib/format";
import { TOA_NHA_QUERY_KEY, type ToaNha } from "@/lib/toa-nha";

function taoSchema(chonCanHo: boolean) {
  return z
    .object({
      maToaNha: z.string(),
      maCanHo: z.string(),
      maCuDan: z.string().min(1, "Vui lòng chọn cư dân"),
      vaiTroCuTru: z.string().min(1, "Vui lòng chọn vai trò cư trú"),
      tinhTrangCuTru: z.string().min(1, "Vui lòng chọn tình trạng cư trú"),
      ngayBatDau: z.string().min(1, "Vui lòng chọn ngày bắt đầu"),
      ngayKetThuc: z.string(),
    })
    .superRefine((values, ctx) => {
      if (chonCanHo) {
        if (!values.maToaNha.trim()) {
          ctx.addIssue({ code: "custom", path: ["maToaNha"], message: "Vui lòng chọn tòa nhà" });
        }
        if (!values.maCanHo.trim()) {
          ctx.addIssue({ code: "custom", path: ["maCanHo"], message: "Vui lòng chọn căn hộ" });
        }
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

function giaTriMacDinh(maCanHo?: string): Values {
  return {
    maToaNha: "",
    maCanHo: maCanHo ?? "",
    maCuDan: "",
    vaiTroCuTru: "",
    tinhTrangCuTru: "",
    ngayBatDau: ngayHomNay(),
    ngayKetThuc: "",
  };
}

type ThemHoSoCuTruDialogProps = {
  /** Có mặt khi mở từ trang chi tiết căn hộ; bỏ trống khi mở từ `/ho-so-cu-tru` để chọn tòa và căn hộ (AC-17). */
  maCanHo?: string;
  /** Danh sách tòa nhà sẵn có từ trang cha (nếu đã tải). */
  toaNha?: readonly ToaNha[];
};

/**
 * Hộp thoại BQL thêm hồ sơ cư trú vào căn hộ (spec 0014, 0015, AC-3, AC-4, AC-5, AC-6, AC-17, AC-18).
 * Hỗ trợ cả chế độ cố định căn hộ lẫn chế độ chọn tòa nhà rồi tìm căn hộ qua combobox trên `/ho-so-cu-tru`.
 */
export function ThemHoSoCuTruDialog({ maCanHo, toaNha }: ThemHoSoCuTruDialogProps) {
  const chonCanHo = !maCanHo;
  const [open, setOpen] = useState(false);
  const [tuKhoa, setTuKhoa] = useState("");
  const [tuKhoaTre, setTuKhoaTre] = useState("");
  const [daChon, setDaChon] = useState<CuDanLuaChon | null>(null);
  const [tuKhoaCanHo, setTuKhoaCanHo] = useState("");
  const [tuKhoaCanHoTre, setTuKhoaCanHoTre] = useState("");
  const [canHoDaChon, setCanHoDaChon] = useState<CanHo | null>(null);
  const queryClient = useQueryClient();
  const schema = useMemo(() => taoSchema(chonCanHo), [chonCanHo]);
  const form = useApiForm({ schema, defaultValues: giaTriMacDinh(maCanHo) });
  const tinhTrang = useWatch({ control: form.control, name: "tinhTrangCuTru" });
  const maToaNhaChon = useWatch({ control: form.control, name: "maToaNha" });

  useEffect(() => {
    const gon = tuKhoa.trim();
    const henGio = setTimeout(() => setTuKhoaTre(gon), gon ? 200 : 0);
    return () => clearTimeout(henGio);
  }, [tuKhoa]);

  useEffect(() => {
    const gon = tuKhoaCanHo.trim().slice(0, 20);
    const henGio = setTimeout(() => setTuKhoaCanHoTre(gon), gon ? 200 : 0);
    return () => clearTimeout(henGio);
  }, [tuKhoaCanHo]);

  const toaNhaQuery = useQuery<PageResponse<ToaNha>, ApiError>({
    queryKey: [...TOA_NHA_QUERY_KEY, "lua-chon"],
    queryFn: () =>
      api<PageResponse<ToaNha>>(
        client.GET("/api/v1/buildings", { params: { query: { size: 100, sort: "tenToaNha,asc" } } }),
      ),
    enabled: open && chonCanHo && !toaNha,
  });

  const canHoQuery = useQuery<PageResponse<CanHo>, ApiError>({
    queryKey: [...CAN_HO_QUERY_KEY, "lua-chon-cu-tru", maToaNhaChon, tuKhoaCanHoTre],
    queryFn: () =>
      api<PageResponse<CanHo>>(
        client.GET("/api/v1/apartments", {
          params: {
            query: {
              maToaNha: maToaNhaChon,
              q: tuKhoaCanHoTre || undefined,
              size: 50,
              sort: "maCanHo,asc",
            },
          },
        }),
      ),
    enabled: open && chonCanHo && Boolean(maToaNhaChon),
  });

  const cuDanQuery = useQuery<CuDanLuaChon[], ApiError>({
    queryKey: [...CU_DAN_LUA_CHON_QUERY_KEY, tuKhoaTre],
    queryFn: () =>
      api<CuDanLuaChon[]>(
        client.GET("/api/v1/residency-records/resident-options", {
          params: { query: tuKhoaTre ? { q: tuKhoaTre } : undefined },
        }),
      ),
    enabled: open,
  });

  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<HoSoCuTru>(
        client.POST("/api/v1/residency-records", {
          body: {
            maCanHo: maCanHo ?? values.maCanHo,
            maCuDan: values.maCuDan,
            vaiTroCuTru: values.vaiTroCuTru as VaiTroCuTru,
            tinhTrangCuTru: values.tinhTrangCuTru as TinhTrangCuTru,
            ngayBatDau: values.ngayBatDau,
            ngayKetThuc: values.ngayKetThuc.trim() || null,
          },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    const canHoDich = maCanHo ?? values.maCanHo;
    await mutation.mutateAsync(values);
    lamMoiSauThayDoiCuTru(queryClient, canHoDich);
    notifySuccess("Đã thêm hồ sơ cư trú");
    setOpen(false);
  });

  const danhSachToaNha = toaNha ?? toaNhaQuery.data?.items ?? [];
  const danhSachCanHo = canHoQuery.data?.items ?? [];
  const luaChonCanHo = danhSachCanHo.map((c) => ({
    value: c.maCanHo,
    label: `${c.maCanHo} (Tầng ${c.soTang})`,
    content: (
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-col">
          <span className="font-medium text-foreground">{c.maCanHo}</span>
          <span className="type-caption text-muted-foreground">
            {c.tenToaNha} · Tầng {c.soTang}
          </span>
        </div>
        <StatusBadge tone={toneTrangThai(c.trangThaiSuDung)}>{c.trangThaiSuDung}</StatusBadge>
      </div>
    ),
  }));

  const danhSachCuDan = cuDanQuery.data ?? [];
  const luaChonCuDan = danhSachCuDan.map((c) => ({
    value: c.maCuDan,
    label: `${c.hoTen} (${c.soDienThoai})`,
    content: (
      <div className="flex flex-col gap-0.5">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-medium text-foreground">{c.hoTen}</span>
          {c.trangThaiTaiKhoan === "Tạm khóa" && <StatusBadge tone="warning">Tạm khóa</StatusBadge>}
        </div>
        <span className="type-caption text-muted-foreground">
          SĐT: {c.soDienThoai} · CCCD: {c.soCccd} · {c.maCuDan}
        </span>
      </div>
    ),
  }));

  const tieuDe = maCanHo ? `Thêm hồ sơ cư trú căn hộ ${maCanHo}` : "Thêm hồ sơ cư trú";
  const moTa = maCanHo
    ? `Chọn cư dân đã có tài khoản và ghi nhận thời gian cư trú tại căn hộ ${maCanHo}.`
    : "Chọn tòa nhà, căn hộ và cư dân đã có tài khoản để ghi nhận thời gian cư trú.";

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) {
          setTuKhoa("");
          setTuKhoaTre("");
          setDaChon(null);
          setTuKhoaCanHo("");
          setTuKhoaCanHoTre("");
          setCanHoDaChon(null);
          form.reset(giaTriMacDinh(maCanHo));
        }
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <Plus aria-hidden="true" />
          Thêm hồ sơ cư trú
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{tieuDe}</DialogTitle>
          <DialogDescription>{moTa}</DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label={tieuDe} className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            {chonCanHo && (
              <>
                <FormField
                  control={form.control}
                  name="maToaNha"
                  label="Tòa nhà"
                  required
                  render={(field) => (
                    <SelectControl
                      field={{
                        ...field,
                        onChange: (maMoi: string) => {
                          field.onChange(maMoi);
                          form.setValue("maCanHo", "", { shouldValidate: false });
                          form.clearErrors("maCanHo");
                          setCanHoDaChon(null);
                          setTuKhoaCanHo("");
                          setTuKhoaCanHoTre("");
                        },
                      }}
                      placeholder="Chọn tòa nhà"
                    >
                      {danhSachToaNha.map((t) => (
                        <SelectItem key={t.maToaNha} value={t.maToaNha}>
                          {t.tenToaNha}
                        </SelectItem>
                      ))}
                    </SelectControl>
                  )}
                />
                <FormField
                  control={form.control}
                  name="maCanHo"
                  label="Căn hộ"
                  description={
                    maToaNhaChon ? "Tìm theo một phần mã căn hộ trong tòa đã chọn" : "Chọn tòa nhà trước khi tìm căn hộ"
                  }
                  required
                  render={({ name: _name, onChange, value, ...fieldProps }) => (
                    <Combobox
                      {...fieldProps}
                      value={value}
                      disabled={!maToaNhaChon}
                      onValueChange={(maMoi) => {
                        const muc = danhSachCanHo.find((c) => c.maCanHo === maMoi) ?? null;
                        setCanHoDaChon(muc);
                        onChange(maMoi);
                      }}
                      options={luaChonCanHo}
                      selectedLabel={
                        canHoDaChon?.maCanHo === value
                          ? `${canHoDaChon.maCanHo} (Tầng ${canHoDaChon.soTang})`
                          : value || undefined
                      }
                      placeholder={maToaNhaChon ? "Chọn căn hộ" : "Chọn tòa nhà trước"}
                      searchValue={tuKhoaCanHo}
                      onSearchChange={setTuKhoaCanHo}
                      searchPlaceholder="Nhập mã căn hộ, ví dụ A-01..."
                      searchLabel="Tìm căn hộ"
                      listLabel="Danh sách căn hộ"
                      emptyText="Không tìm thấy căn hộ phù hợp."
                      loading={Boolean(maToaNhaChon) && canHoQuery.isPending}
                      errorText={canHoQuery.isError ? messageFor(canHoQuery.error) : undefined}
                    />
                  )}
                />
              </>
            )}
            <FormField
              control={form.control}
              name="maCuDan"
              label="Cư dân"
              description="Tìm theo họ tên, số điện thoại, số CCCD, email hoặc mã cư dân"
              required
              render={({ name: _name, onChange, value, ...fieldProps }) => (
                <Combobox
                  {...fieldProps}
                  value={value}
                  onValueChange={(maMoi) => {
                    const muc = danhSachCuDan.find((c) => c.maCuDan === maMoi) ?? null;
                    setDaChon(muc);
                    onChange(maMoi);
                  }}
                  options={luaChonCuDan}
                  selectedLabel={daChon?.maCuDan === value ? `${daChon.hoTen} (${daChon.soDienThoai})` : undefined}
                  placeholder="Chọn cư dân"
                  searchValue={tuKhoa}
                  onSearchChange={setTuKhoa}
                  searchPlaceholder="Nhập tên, số điện thoại hoặc CCCD..."
                  searchLabel="Tìm cư dân"
                  listLabel="Danh sách cư dân"
                  emptyText="Không tìm thấy cư dân phù hợp."
                  loading={cuDanQuery.isPending}
                  errorText={cuDanQuery.isError ? messageFor(cuDanQuery.error) : undefined}
                />
              )}
            />
            <FormField
              control={form.control}
              name="vaiTroCuTru"
              label="Vai trò cư trú"
              required
              render={(field) => (
                <SelectControl field={field} placeholder="Chọn vai trò cư trú">
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
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>Thêm hồ sơ</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
