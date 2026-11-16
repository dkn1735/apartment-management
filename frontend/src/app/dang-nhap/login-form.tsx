"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import * as z from "zod";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { api, client } from "@/lib/api/api";
import { safeNext } from "@/lib/auth/safe-next";

const schema = z.object({
  taiKhoan: z.string().trim().min(1, "Vui lòng nhập số điện thoại hoặc email"),
  matKhau: z.string().min(1, "Vui lòng nhập mật khẩu"),
});

type Values = z.output<typeof schema>;

/** Form đăng nhập (spec 0004, AC-12). Đọc `next` từ URL nên phải nằm trong Suspense. */
export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const form = useApiForm({ schema, defaultValues: { taiKhoan: "", matKhau: "" } });
  const mutation = useMutation({
    mutationFn: (values: Values) => api(client.POST("/api/v1/auth/login", { body: values })),
    // Lỗi của chính form đăng nhập không kích hoạt chuyển trang toàn cục (AC-15)
    meta: { boQuaChuyenTrang: true },
  });

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    // Bỏ dữ liệu của tài khoản trước (nếu có) trước khi vào trang của tài khoản mới
    queryClient.clear();
    router.replace(safeNext(searchParams.get("next")));
  });

  return (
    <form onSubmit={onSubmit} noValidate aria-label="Đăng nhập" className="flex flex-col gap-5">
      <FormAlert error={form.formError} focusKey={form.formErrorKey} />
      <FieldGroup>
        <FormField
          control={form.control}
          name="taiKhoan"
          label="Số điện thoại hoặc email"
          required
          render={(field) => <Input autoComplete="username" autoCapitalize="none" spellCheck={false} {...field} />}
        />
        <FormField
          control={form.control}
          name="matKhau"
          label="Mật khẩu"
          required
          render={(field) => <Input type="password" autoComplete="current-password" {...field} />}
        />
      </FieldGroup>
      <SubmitButton form={form} size="lg" className="w-full">
        Đăng nhập
      </SubmitButton>
    </form>
  );
}
