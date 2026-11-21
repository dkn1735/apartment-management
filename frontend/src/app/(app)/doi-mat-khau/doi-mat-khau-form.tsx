"use client";

import { useMutation } from "@tanstack/react-query";
import { CircleCheck } from "lucide-react";
import { useState } from "react";
import * as z from "zod";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { PasswordInput } from "@/components/form/password-input";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { PageHeader } from "@/components/layout/page-header";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Card } from "@/components/ui/card";
import { FieldGroup } from "@/components/ui/field";
import { api, ApiError, client } from "@/lib/api/api";

/** Quy tắc độ mạnh của API-AUTH-04; backend kiểm lại, đây chỉ để báo sớm. */
export const QUY_TAC_MAT_KHAU = "Ít nhất 8 ký tự, có cả chữ và số, khác mật khẩu hiện tại.";

const schema = z
  .object({
    matKhauCu: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
    matKhauMoi: z
      .string()
      .min(1, "Vui lòng nhập mật khẩu mới")
      .min(8, "Mật khẩu mới phải có từ 8 đến 128 ký tự")
      .max(128, "Mật khẩu mới phải có từ 8 đến 128 ký tự")
      .regex(/\p{L}/u, "Mật khẩu mới phải có cả chữ và số")
      .regex(/\p{Nd}/u, "Mật khẩu mới phải có cả chữ và số"),
    xacNhan: z.string().min(1, "Vui lòng nhập lại mật khẩu mới"),
  })
  .refine((v) => v.matKhauMoi !== v.matKhauCu, {
    path: ["matKhauMoi"],
    message: "Mật khẩu mới phải khác mật khẩu hiện tại",
  })
  .refine((v) => v.xacNhan === v.matKhauMoi, { path: ["xacNhan"], message: "Mật khẩu nhập lại chưa khớp" });

type Values = z.output<typeof schema>;

const RONG: Values = { matKhauCu: "", matKhauMoi: "", xacNhan: "" };

/**
 * Trang đổi mật khẩu (API-AUTH-04). Thành công thì giữ phiên trên máy này, các máy khác phải đăng nhập lại;
 * form được xóa trắng và một thông báo thành công hiện ở đầu form.
 */
export function DoiMatKhauForm() {
  const form = useApiForm({ schema, defaultValues: RONG });
  const [thanhCong, setThanhCong] = useState(false);
  const mutation = useMutation({
    mutationFn: ({ matKhauCu, matKhauMoi }: Values) =>
      api(client.PUT("/api/v1/auth/change-password", { body: { matKhauCu, matKhauMoi } })),
  });

  const onSubmit = form.submitWith(async (values) => {
    setThanhCong(false);
    try {
      await mutation.mutateAsync(values);
    } catch (error) {
      // Sai mật khẩu hiện tại: xóa ô đó để gõ lại, giữ mật khẩu mới
      if (error instanceof ApiError && error.errorCode === "AUTH_OLD_PASSWORD_INCORRECT") {
        form.setValue("matKhauCu", "");
      }
      throw error;
    }
    form.reset(RONG);
    setThanhCong(true);
  });

  return (
    <>
      <PageHeader
        title="Đổi mật khẩu"
        description="Sau khi đổi, bạn vẫn đăng nhập trên máy này; các thiết bị khác sẽ phải đăng nhập lại."
      />
      <Card className="max-w-xl">
        <div className="flex flex-col gap-5">
          <div role="status" className="empty:hidden">
            {thanhCong && (
              <Alert tone="success">
                <CircleCheck aria-hidden="true" />
                <AlertDescription>Đã đổi mật khẩu. Các thiết bị khác đã được đăng xuất.</AlertDescription>
              </Alert>
            )}
          </div>
          <form onSubmit={onSubmit} noValidate aria-label="Đổi mật khẩu" className="flex flex-col gap-5">
            <FormAlert error={form.formError} focusKey={form.formErrorKey} />
            <FieldGroup>
              <FormField
                control={form.control}
                name="matKhauCu"
                label="Mật khẩu hiện tại"
                required
                render={(field) => <PasswordInput autoComplete="current-password" {...field} />}
              />
              <FormField
                control={form.control}
                name="matKhauMoi"
                label="Mật khẩu mới"
                description={QUY_TAC_MAT_KHAU}
                required
                render={(field) => <PasswordInput autoComplete="new-password" {...field} />}
              />
              <FormField
                control={form.control}
                name="xacNhan"
                label="Nhập lại mật khẩu mới"
                required
                render={(field) => <PasswordInput autoComplete="new-password" {...field} />}
              />
            </FieldGroup>
            <div>
              <SubmitButton form={form}>Đổi mật khẩu</SubmitButton>
            </div>
          </form>
        </div>
      </Card>
    </>
  );
}
