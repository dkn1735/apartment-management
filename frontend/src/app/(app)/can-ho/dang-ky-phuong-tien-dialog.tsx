"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
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
import { api, client, type ApiError } from "@/lib/api/api";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { doDaiLyDo } from "@/lib/can-ho";
import { khoaHoSoCuTru, type HoSoCuTruCanHo } from "@/lib/cu-tru";
import {
  BIEN_SO_HOP_LE_REGEX,
  chuanHoaBienSo,
  lamMoiPhuongTienCanHo,
  LOAI_PHUONG_TIEN,
  type LoaiPhuongTien,
  type PhuongTien,
} from "@/lib/phuong-tien";

const NHAN_HIEU_TOI_DA = 100;

const schema = z
  .object({
    maCuDanSoHuu: z.string().min(1, "Vui lòng chọn chủ xe"),
    loaiPhuongTien: z.string().min(1, "Vui lòng chọn loại phương tiện"),
    bienSo: z.string(),
    nhanHieuMauSac: z.string().refine((v) => doDaiLyDo(v) <= NHAN_HIEU_TOI_DA, "Nhãn hiệu, màu sắc tối đa 100 ký tự"),
  })
  .superRefine((values, ctx) => {
    const bienChuan = chuanHoaBienSo(values.bienSo);
    if (values.loaiPhuongTien !== "Xe đạp" && bienChuan.length === 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["bienSo"],
        message: "Vui lòng nhập biển số",
      });
      return;
    }
    if (bienChuan.length > 0 && !BIEN_SO_HOP_LE_REGEX.test(bienChuan)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["bienSo"],
        message: "Biển số chỉ gồm chữ và số, dài từ 5 đến 12 ký tự",
      });
    }
  });

type Values = z.output<typeof schema>;

type DangKyPhuongTienDialogProps = {
  maCanHo: string;
  laBql: boolean;
  onThanhCong?: () => void;
};

/**
 * Hộp thoại đăng ký phương tiện cho BQL (`Đang hoạt động` ngay) và cư dân (`Chờ duyệt`) (spec 0016, AC-5, AC-6, AC-7, AC-15).
 */
export function DangKyPhuongTienDialog({ maCanHo, laBql, onThanhCong }: DangKyPhuongTienDialogProps) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const maCuDanHienTai = useHoSo().data?.chiTietVaiTro?.cuDan?.maCuDan ?? "";

  const cuTruQuery = useQuery<HoSoCuTruCanHo, ApiError>({
    queryKey: khoaHoSoCuTru(maCanHo),
    queryFn: () =>
      api<HoSoCuTruCanHo>(
        client.GET("/api/v1/apartments/{apartmentId}/residents", { params: { path: { apartmentId: maCanHo } } }),
      ),
    enabled: open,
  });

  const danhSachChuXe = useMemo(() => {
    const seen = new Set<string>();
    const list: Array<{ maCuDan: string; hoTen: string }> = [];
    for (const item of cuTruQuery.data?.dangCuTru ?? []) {
      if (!seen.has(item.maCuDan)) {
        seen.add(item.maCuDan);
        list.push({ maCuDan: item.maCuDan, hoTen: item.hoTen ?? item.maCuDan });
      }
    }
    return list;
  }, [cuTruQuery.data?.dangCuTru]);

  const chuXeMacDinh = useMemo(() => {
    if (laBql || !maCuDanHienTai) return "";
    return danhSachChuXe.some((c) => c.maCuDan === maCuDanHienTai) ? maCuDanHienTai : "";
  }, [laBql, maCuDanHienTai, danhSachChuXe]);

  const giaTriMacDinh = (): Values => ({
    maCuDanSoHuu: chuXeMacDinh,
    loaiPhuongTien: "",
    bienSo: "",
    nhanHieuMauSac: "",
  });

  const form = useApiForm({ schema, defaultValues: giaTriMacDinh() });
  const loaiDaChon = form.watch("loaiPhuongTien");
  const laXeDap = loaiDaChon === "Xe đạp";

  useEffect(() => {
    if (open && chuXeMacDinh && !form.getValues("maCuDanSoHuu")) {
      form.setValue("maCuDanSoHuu", chuXeMacDinh);
    }
  }, [open, chuXeMacDinh, form]);

  const mutation = useMutation({
    mutationFn: (values: Values) => {
      const bienChuan = chuanHoaBienSo(values.bienSo);
      const nhanHieu = values.nhanHieuMauSac.normalize("NFC").trim();
      return api<PhuongTien>(
        client.POST("/api/v1/vehicles", {
          body: {
            maCanHo,
            maCuDanSoHuu: values.maCuDanSoHuu,
            loaiPhuongTien: values.loaiPhuongTien as LoaiPhuongTien,
            bienSo: bienChuan.length > 0 ? bienChuan : null,
            nhanHieuMauSac: nhanHieu.length > 0 ? nhanHieu : null,
          },
        }),
      );
    },
  });

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    lamMoiPhuongTienCanHo(queryClient, maCanHo);
    notifySuccess(laBql ? "Đã đăng ký phương tiện" : "Đã gửi đơn đăng ký xe");
    setOpen(false);
    onThanhCong?.();
  });

  const nhanNut = laBql ? "Đăng ký phương tiện" : "Gửi đơn đăng ký xe";

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) {
          form.reset(giaTriMacDinh());
        }
      }}
    >
      <DialogTrigger asChild>
        <Button type="button">
          <Plus aria-hidden="true" />
          {nhanNut}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{nhanNut}</DialogTitle>
          <DialogDescription>
            {laBql
              ? `Đăng ký thẻ xe cho cư dân đang cư trú hợp lệ tại căn hộ ${maCanHo}. Thẻ xe có hiệu lực ngay.`
              : `Gửi đơn đăng ký xe cho căn hộ ${maCanHo}. Ban quản lý sẽ xem xét và duyệt đơn.`}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label={nhanNut} className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="maCuDanSoHuu"
              label="Chủ xe"
              description={
                cuTruQuery.isPending
                  ? "Đang tải danh sách cư dân đang cư trú..."
                  : danhSachChuXe.length === 0 && !cuTruQuery.isError
                    ? "Căn hộ hiện chưa có cư dân đang cư trú hợp lệ."
                    : "Chỉ chọn được người đang cư trú hợp lệ tại căn hộ."
              }
              required
              render={(field) => (
                <SelectControl field={field} placeholder={cuTruQuery.isPending ? "Đang tải..." : "Chọn chủ xe"}>
                  {danhSachChuXe.map((cuDan) => (
                    <SelectItem key={cuDan.maCuDan} value={cuDan.maCuDan}>
                      {cuDan.hoTen} ({cuDan.maCuDan})
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
            />
            <FormField
              control={form.control}
              name="loaiPhuongTien"
              label="Loại phương tiện"
              required
              render={(field) => (
                <SelectControl field={field} placeholder="Chọn loại phương tiện">
                  {LOAI_PHUONG_TIEN.map((loai) => (
                    <SelectItem key={loai} value={loai}>
                      {loai}
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
            />
            <FormField
              control={form.control}
              name="bienSo"
              label={laXeDap ? "Biển số (không bắt buộc với xe đạp)" : "Biển số"}
              description="Chữ và số, dài từ 5 đến 12 ký tự (ví dụ 30A-123.45)"
              required={!laXeDap}
              render={(field) => <Input placeholder="30A-123.45" {...field} />}
            />
            <FormField
              control={form.control}
              name="nhanHieuMauSac"
              label="Nhãn hiệu, màu sắc"
              description="Không bắt buộc, tối đa 100 ký tự"
              render={(field) => <Input placeholder="Ví dụ: Honda Vision đen" {...field} />}
            />
          </FieldGroup>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>{laBql ? "Đăng ký" : "Gửi đơn"}</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
