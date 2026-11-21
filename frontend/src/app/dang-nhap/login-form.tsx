"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CircleCheck, Clock, Lock } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import * as z from "zod";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { PasswordInput } from "@/components/form/password-input";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { api, client } from "@/lib/api/api";
import { ERROR_MESSAGES } from "@/lib/api/error-messages";
import { BO_QUA_CHUYEN_TRANG } from "@/lib/api/query-client";
import { duongDoiMatKhau } from "@/lib/auth/bat-doi-mat-khau";
import { safeNext } from "@/lib/auth/safe-next";

const schema = z.object({
  taiKhoan: z.string().trim().min(1, "Vui lòng nhập số điện thoại hoặc email"),
  matKhau: z.string().min(1, "Vui lòng nhập mật khẩu"),
});

type Values = z.output<typeof schema>;

/** Lý do bị đưa về trang đăng nhập (`?ly-do=`); giá trị khác thì bỏ qua. */
const LY_DO = {
  "het-phien": { tone: "info", icon: Clock, text: "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại." },
  khoa: { tone: "warning", icon: Lock, text: ERROR_MESSAGES.AUTH_ACCOUNT_LOCKED },
  // Spec 0005, AC-14
  "dang-xuat": { tone: "success", icon: CircleCheck, text: "Bạn đã đăng xuất." },
} as const;

function laLyDo(value: string | null): value is keyof typeof LY_DO {
  return value !== null && Object.hasOwn(LY_DO, value);
}

/** Form đăng nhập (spec 0004, AC-12). Đọc `next` và `ly-do` từ URL nên phải nằm trong Suspense. */
export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const form = useApiForm({ schema, defaultValues: { taiKhoan: "", matKhau: "" } });
  const mutation = useMutation({
    mutationFn: (values: Values) => api(client.POST("/api/v1/auth/login", { body: values })),
    // Lỗi của chính form đăng nhập không kích hoạt chuyển trang toàn cục (AC-15)
    meta: { [BO_QUA_CHUYEN_TRANG]: true },
  });

  const lyDoParam = searchParams.get("ly-do");
  const lyDo = laLyDo(lyDoParam) ? LY_DO[lyDoParam] : null;

  const onSubmit = form.submitWith(async (values) => {
    let phaiDoiMatKhau = false;
    try {
      const data = (await mutation.mutateAsync(values)) as { phaiDoiMatKhau?: boolean } | undefined;
      phaiDoiMatKhau = data?.phaiDoiMatKhau === true;
    } catch (error) {
      // Giữ ô tài khoản, xóa ô mật khẩu để gõ lại
      form.setValue("matKhau", "");
      throw error;
    }
    // Bỏ dữ liệu của tài khoản trước (nếu có) trước khi vào trang của tài khoản mới
    queryClient.clear();
    // Mật khẩu tạm: đổi mật khẩu trước, rồi mới tới trang định vào (spec 0008, AC-14)
    router.replace(phaiDoiMatKhau ? duongDoiMatKhau(searchParams.get("next")) : safeNext(searchParams.get("next")));
  });

  return (
    <div className="flex flex-col gap-5">
      <div role="status" className="empty:hidden">
        {lyDo && (
          <Alert tone={lyDo.tone}>
            <lyDo.icon aria-hidden="true" />
            <AlertDescription>{lyDo.text}</AlertDescription>
          </Alert>
        )}
      </div>
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
            render={(field) => <PasswordInput autoComplete="current-password" {...field} />}
          />
        </FieldGroup>
        <SubmitButton form={form} size="lg" className="w-full">
          Đăng nhập
        </SubmitButton>
      </form>
    </div>
  );
}
