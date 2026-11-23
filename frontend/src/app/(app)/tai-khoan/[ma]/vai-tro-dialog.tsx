"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { TriangleAlert } from "lucide-react";
import { useState, type ReactNode } from "react";
import * as z from "zod";
import { notifyInfo, notifySuccess } from "@/components/feedback/notify";
import { SelectControl } from "@/components/form/field-controls";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Alert, AlertDescription } from "@/components/ui/alert";
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
  CAU_LECH_PHIEN_BAN,
  khoaChiTiet,
  laLechPhienBan,
  loiTruong,
  TRUONG_CUA_VAI_TRO,
  type TaiKhoanChiTiet,
  type TruongVaiTro,
  type VaiTroTaiKhoan,
} from "@/lib/tai-khoan";
import { NHAN_VAI_TRO, type VaiTro } from "@/lib/vai-tro";

const CHUA_CHON = "";

const schema = z
  .object({
    vaiTro: z.string().min(1, "Vui lòng chọn vai trò"),
    chucVu: z.string(),
    boPhan: z.string(),
    nhiemKy: z.string(),
    vaiTroBqt: z.string(),
    phamViPhuTrach: z.string(),
    chuyenMonKyThuat: z.string(),
    viTriTruc: z.string(),
    soCccd: z.string(),
  })
  .superRefine((v, ctx) => {
    if (!v.vaiTro) return;
    for (const moTa of TRUONG_CUA_VAI_TRO[v.vaiTro as VaiTro]) {
      const loi = loiTruong(moTa, v[moTa.ten]);
      if (loi) ctx.addIssue({ code: "custom", path: [moTa.ten], message: loi });
    }
  });

type Values = z.output<typeof schema>;

function giaTriDau(dang?: VaiTroTaiKhoan): Values {
  const doc = (ten: TruongVaiTro) => dang?.[ten] ?? "";
  return {
    vaiTro: dang?.vaiTro ?? CHUA_CHON,
    chucVu: doc("chucVu"),
    boPhan: doc("boPhan"),
    nhiemKy: doc("nhiemKy"),
    vaiTroBqt: doc("vaiTroBqt"),
    phamViPhuTrach: doc("phamViPhuTrach"),
    chuyenMonKyThuat: doc("chuyenMonKyThuat"),
    viTriTruc: doc("viTriTruc"),
    soCccd: doc("soCccd"),
  };
}

type VaiTroDialogProps = {
  taiKhoan: TaiKhoanChiTiet;
  trigger: ReactNode;
  /** Sửa vai trò đã có; bỏ trống là thêm vai trò mới, chọn trong `coTheThem`. */
  dangSua?: VaiTroTaiKhoan;
  coTheThem?: readonly VaiTro[];
};

/**
 * Hộp thoại thêm hoặc sửa một vai trò (spec 0008, AC-8, AC-17). Chọn Quản trị viên thì hộp thoại cảnh báo và nút gửi
 * đổi thành "Cấp quyền quản trị", đó là bước xác nhận trước khi gửi.
 */
export function VaiTroDialog({ taiKhoan, trigger, dangSua, coTheThem = [] }: VaiTroDialogProps) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const form = useApiForm({ schema, defaultValues: giaTriDau(dangSua) });
  const chon = form.watch("vaiTro") as VaiTro | typeof CHUA_CHON;
  const mutation = useMutation({
    mutationFn: (values: Values) => {
      const vaiTro = values.vaiTro as VaiTro;
      const body: Record<string, unknown> = { phienBan: taiKhoan.phienBan };
      for (const moTa of TRUONG_CUA_VAI_TRO[vaiTro]) body[moTa.ten] = values[moTa.ten];
      return api<TaiKhoanChiTiet>(
        client.PUT("/api/v1/accounts/{maNguoiDung}/roles/{vaiTro}", {
          params: { path: { maNguoiDung: taiKhoan.maNguoiDung, vaiTro } },
          body: body as { phienBan: number },
        }),
      );
    },
  });

  const onSubmit = form.submitWith(async (values) => {
    try {
      const data = await mutation.mutateAsync(values);
      queryClient.setQueryData(khoaChiTiet(taiKhoan.maNguoiDung), data);
      void queryClient.invalidateQueries({ queryKey: ["tai-khoan", "danh-sach"] });
      notifySuccess(dangSua ? "Đã lưu vai trò" : `Đã thêm vai trò ${NHAN_VAI_TRO[values.vaiTro as VaiTro]}`);
      setOpen(false);
    } catch (error) {
      if (!laLechPhienBan(error)) throw error;
      notifyInfo(CAU_LECH_PHIEN_BAN);
      await queryClient.invalidateQueries({ queryKey: khoaChiTiet(taiKhoan.maNguoiDung) });
      setOpen(false);
    }
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset(giaTriDau(dangSua));
      }}
    >
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{dangSua ? `Sửa vai trò ${NHAN_VAI_TRO[dangSua.vaiTro]}` : "Thêm vai trò"}</DialogTitle>
          <DialogDescription>
            Vai trò mới có hiệu lực trong vòng 15 phút, khi phiên của người dùng được làm mới.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label="Vai trò" className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            {!dangSua && (
              <FormField
                control={form.control}
                name="vaiTro"
                label="Vai trò"
                required
                render={(field) => (
                  <SelectControl field={field} placeholder="Chọn vai trò">
                    {coTheThem.map((v) => (
                      <SelectItem key={v} value={v}>
                        {NHAN_VAI_TRO[v]}
                      </SelectItem>
                    ))}
                  </SelectControl>
                )}
              />
            )}
            {chon &&
              TRUONG_CUA_VAI_TRO[chon].map((moTa) => (
                <FormField
                  key={moTa.ten}
                  control={form.control}
                  name={moTa.ten}
                  label={moTa.nhan}
                  description={moTa.goiY}
                  required
                  render={(field) => (
                    <Input inputMode={moTa.ten === "soCccd" ? "numeric" : undefined} autoComplete="off" {...field} />
                  )}
                />
              ))}
          </FieldGroup>
          {chon === "QUAN_TRI_VIEN" && (
            <Alert tone="warning">
              <TriangleAlert aria-hidden="true" />
              <AlertDescription>
                Quản trị viên tạo, khóa và đặt lại mật khẩu được mọi tài khoản, kể cả tài khoản quản trị khác.
              </AlertDescription>
            </Alert>
          )}
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>
              {chon === "QUAN_TRI_VIEN" ? "Cấp quyền quản trị" : dangSua ? "Lưu" : "Thêm vai trò"}
            </SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
