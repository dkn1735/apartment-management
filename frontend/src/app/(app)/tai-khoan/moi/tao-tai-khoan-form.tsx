"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Controller } from "react-hook-form";
import * as z from "zod";
import { notifySuccess } from "@/components/feedback/notify";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { FIELD_NAME_ATTR, useApiForm } from "@/components/form/use-api-form";
import { PageHeader } from "@/components/layout/page-header";
import { SectionHeader } from "@/components/layout/section-header";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { FieldError, FieldGroup, FieldLegend, FieldSet } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, client } from "@/lib/api/api";
import type { components } from "@/lib/api/schema";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { doiTenTruongLoi, loiTruong, TRUONG_CUA_VAI_TRO, type TruongVaiTro } from "@/lib/tai-khoan";
import { NHAN_VAI_TRO, THU_TU_VAI_TRO, type VaiTro } from "@/lib/vai-tro";
import { MatKhauTamDialog } from "../mat-khau-tam-dialog";

type TaoTaiKhoanResponse = components["schemas"]["TaoTaiKhoanResponse"];
type VaiTroRequest = components["schemas"]["VaiTroRequest"];

const TRUONG_RONG: Record<TruongVaiTro, string> = {
  chucVu: "",
  boPhan: "",
  nhiemKy: "",
  vaiTroBqt: "",
  phamViPhuTrach: "",
  chuyenMonKyThuat: "",
  viTriTruc: "",
  soCccd: "",
};

/** Cùng quy tắc và câu với backend (spec 0008, AC-4), để lỗi hiện ngay dưới ô; backend vẫn kiểm lại. */
const schema = z
  .object({
    hoTen: z.string().trim().min(1, "Vui lòng nhập họ tên").max(100, "Họ tên tối đa 100 ký tự"),
    soDienThoai: z.string().trim().min(1, "Vui lòng nhập số điện thoại"),
    email: z.string().trim().min(1, "Vui lòng nhập email").max(150, "Email không hợp lệ"),
    vaiTro: z.array(z.enum(THU_TU_VAI_TRO as [VaiTro, ...VaiTro[]])).min(1, "Vui lòng chọn ít nhất một vai trò"),
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
    for (const vaiTro of v.vaiTro) {
      for (const moTa of TRUONG_CUA_VAI_TRO[vaiTro]) {
        const loi = loiTruong(moTa, v[moTa.ten]);
        if (loi) ctx.addIssue({ code: "custom", path: [moTa.ten], message: loi });
      }
    }
  });

type Values = z.output<typeof schema>;

/** Body gửi backend: chỉ trường của vai trò đã chọn (trường của vai trò khác bị backend từ chối). */
function sangBody(v: Values) {
  return {
    hoTen: v.hoTen,
    soDienThoai: v.soDienThoai,
    email: v.email,
    vaiTro: THU_TU_VAI_TRO.filter((ma) => v.vaiTro.includes(ma)).map((ma) => {
      const phanTu: VaiTroRequest = { vaiTro: ma };
      for (const moTa of TRUONG_CUA_VAI_TRO[ma]) phanTu[moTa.ten] = v[moTa.ten];
      return phanTu;
    }),
  };
}

/**
 * Tạo tài khoản (spec 0008, AC-1, AC-17). Admin chọn được bảy vai trò; BQL chỉ tạo tài khoản cư dân, ô Cư dân được
 * chọn sẵn và không bỏ được. Tạo xong mở hộp thoại mật khẩu tạm, đóng hộp thoại thì sang trang chi tiết.
 */
export function TaoTaiKhoanForm() {
  const hoSo = useHoSo();
  const laAdmin = hoSo.data?.vaiTro.includes("QUAN_TRI_VIEN") ?? false;
  const vaiTroChoPhep: readonly VaiTro[] = laAdmin ? THU_TU_VAI_TRO : ["CU_DAN"];

  const form = useApiForm({
    schema,
    defaultValues: { hoTen: "", soDienThoai: "", email: "", vaiTro: laAdmin ? [] : ["CU_DAN"], ...TRUONG_RONG },
  });
  const router = useRouter();
  const queryClient = useQueryClient();
  // Tick "Quản trị viên" thì hỏi xác nhận trước; xác nhận xong mới thêm vào danh sách vai trò
  const [hoiAdmin, setHoiAdmin] = useState(false);
  const [ketQua, setKetQua] = useState<{ ma: string; hoTen: string; matKhau: string } | null>(null);

  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<TaoTaiKhoanResponse>(client.POST("/api/v1/accounts", { body: sangBody(values) })).catch((error) => {
        throw doiTenTruongLoi(error);
      }),
    // Không giữ kết quả (có mật khẩu tạm) trong cache của mutation (AC-17)
    gcTime: 0,
  });

  // Trang chỉ hiện sau khi ChanTrang đã có hồ sơ, nên laAdmin đúng ngay từ lần render đầu
  const daChon = form.watch("vaiTro");

  const onSubmit = form.submitWith(async (values) => {
    const data = await mutation.mutateAsync(values);
    void queryClient.invalidateQueries({ queryKey: ["tai-khoan"] });
    setKetQua({ ma: data.taiKhoan.maNguoiDung, hoTen: data.taiKhoan.hoTen, matKhau: data.matKhauTam });
  });

  return (
    <>
      <PageHeader
        title="Tạo tài khoản"
        description={
          laAdmin
            ? "Hệ thống sẽ sinh một mật khẩu tạm; người dùng phải đổi nó ở lần đăng nhập đầu."
            : "Ban quản lý tạo tài khoản cho cư dân. Hệ thống sinh mật khẩu tạm; cư dân phải đổi nó ở lần đăng nhập đầu."
        }
      />
      <form onSubmit={onSubmit} noValidate aria-label="Tạo tài khoản" className="flex flex-col gap-6">
        <FormAlert error={form.formError} focusKey={form.formErrorKey} />
        <Card className="max-w-3xl">
          <SectionHeader as="h2" title="Thông tin chung" />
          <FieldGroup className="mt-4 grid gap-5 sm:grid-cols-2">
            <FormField
              className="sm:col-span-2"
              control={form.control}
              name="hoTen"
              label="Họ tên"
              required
              render={(field) => <Input autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="soDienThoai"
              label="Số điện thoại"
              description="10 chữ số, bắt đầu bằng 0"
              required
              render={(field) => <Input type="tel" inputMode="tel" autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="email"
              label="Email"
              required
              render={(field) => <Input type="email" autoComplete="off" spellCheck={false} {...field} />}
            />
          </FieldGroup>
        </Card>

        <Card className="max-w-3xl">
          <SectionHeader
            as="h2"
            title="Vai trò"
            description={
              laAdmin
                ? "Chọn một hoặc nhiều vai trò; mỗi vai trò có thông tin riêng."
                : "Ban quản lý chỉ tạo được tài khoản cư dân."
            }
          />
          <Controller
            control={form.control}
            name="vaiTro"
            render={({ field, fieldState }) => (
              <FieldSet
                className="mt-4"
                {...{ [FIELD_NAME_ATTR]: "vaiTro" }}
                aria-invalid={!!fieldState.error || undefined}
                aria-describedby={fieldState.error ? "vai-tro-loi" : undefined}
              >
                <FieldLegend className="sr-only">Vai trò của tài khoản</FieldLegend>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {vaiTroChoPhep.map((ma, i) => {
                    const id = `vai-tro-${ma}`;
                    const chon = field.value.includes(ma);
                    return (
                      <div key={ma} className="flex items-center gap-3 rounded-md border p-3">
                        <Checkbox
                          id={id}
                          ref={i === 0 ? field.ref : undefined}
                          checked={chon}
                          disabled={!laAdmin}
                          onBlur={field.onBlur}
                          onCheckedChange={(checked) => {
                            if (checked === true && ma === "QUAN_TRI_VIEN") setHoiAdmin(true);
                            else
                              field.onChange(
                                checked === true ? [...field.value, ma] : field.value.filter((v: VaiTro) => v !== ma),
                              );
                          }}
                        />
                        <Label htmlFor={id}>{NHAN_VAI_TRO[ma]}</Label>
                      </div>
                    );
                  })}
                </div>
                <FieldError id="vai-tro-loi">{fieldState.error?.message}</FieldError>
              </FieldSet>
            )}
          />
          {THU_TU_VAI_TRO.filter((ma) => daChon.includes(ma) && TRUONG_CUA_VAI_TRO[ma].length > 0).map((ma) => (
            <section key={ma} aria-labelledby={`nhom-${ma}`} className="mt-6 flex flex-col gap-4 border-t pt-5">
              <h3 id={`nhom-${ma}`} className="type-card-title">
                {NHAN_VAI_TRO[ma]}
              </h3>
              <FieldGroup className="grid gap-5 sm:grid-cols-2">
                {TRUONG_CUA_VAI_TRO[ma].map((moTa) => (
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
            </section>
          ))}
        </Card>

        <div className="flex flex-wrap gap-2">
          <SubmitButton form={form}>Tạo tài khoản</SubmitButton>
          <Button asChild variant="outline">
            <Link href="/tai-khoan">Hủy</Link>
          </Button>
        </div>
      </form>

      <AlertDialog open={hoiAdmin} onOpenChange={setHoiAdmin}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cấp quyền quản trị viên?</AlertDialogTitle>
            <AlertDialogDescription>
              Quản trị viên tạo, khóa và đặt lại mật khẩu được mọi tài khoản, kể cả tài khoản quản trị khác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <Button
              type="button"
              onClick={() => {
                const hienTai = form.getValues("vaiTro");
                if (!hienTai.includes("QUAN_TRI_VIEN")) {
                  form.setValue("vaiTro", [...hienTai, "QUAN_TRI_VIEN"], { shouldValidate: true });
                }
                setHoiAdmin(false);
              }}
            >
              Cấp quyền quản trị
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <MatKhauTamDialog
        matKhau={ketQua?.matKhau ?? null}
        hoTen={ketQua?.hoTen ?? ""}
        onClose={() => {
          const ma = ketQua?.ma;
          setKetQua(null);
          notifySuccess("Đã tạo tài khoản");
          if (ma) router.push("/tai-khoan");
        }}
      />
    </>
  );
}
