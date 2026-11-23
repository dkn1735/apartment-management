"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CircleCheck, Info } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
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
import { safeNext } from "@/lib/auth/safe-next";
import { HO_SO_QUERY_KEY, useHoSo } from "@/lib/auth/use-ho-so";
import { matKhauMoiSchema } from "@/lib/mat-khau";

/** Quy tắc độ mạnh của API-AUTH-04; backend kiểm lại, đây chỉ để báo sớm. */
export const QUY_TAC_MAT_KHAU = "Ít nhất 8 ký tự, có cả chữ và số, khác mật khẩu hiện tại.";

const schema = z
  .object({
    matKhauCu: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
    matKhauMoi: matKhauMoiSchema,
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
 * form được xóa trắng và một thông báo thành công hiện ở đầu form. Đang dùng mật khẩu tạm (spec 0008, AC-14) thì
 * form có thông báo ở đầu, và đổi xong thì làm mới hồ sơ rồi chuyển tới `next`.
 */
export function DoiMatKhauForm() {
  const form = useApiForm({ schema, defaultValues: RONG });
  const phaiDoi = useHoSo().data?.phaiDoiMatKhau === true;
  const queryClient = useQueryClient();
  const router = useRouter();
  const searchParams = useSearchParams();
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
    if (phaiDoi) {
      await queryClient.invalidateQueries({ queryKey: HO_SO_QUERY_KEY });
      router.replace(safeNext(searchParams.get("next")));
      return;
    }
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
          {phaiDoi && (
            <Alert tone="info" id="thong-bao-mat-khau-tam">
              <Info aria-hidden="true" />
              <AlertDescription>Bạn đang dùng mật khẩu tạm. Hãy đặt mật khẩu mới để tiếp tục.</AlertDescription>
            </Alert>
          )}
          <form
            onSubmit={onSubmit}
            noValidate
            aria-label="Đổi mật khẩu"
            aria-describedby={phaiDoi ? "thong-bao-mat-khau-tam" : undefined}
            className="flex flex-col gap-5"
          >
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
