"use client";

import { useMutation } from "@tanstack/react-query";
import { MailCheck } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import * as z from "zod";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { api, client } from "@/lib/api/api";
import { BO_QUA_CHUYEN_TRANG } from "@/lib/api/query-client";
import { TRANG_DANG_NHAP } from "@/lib/auth/safe-next";
import { THOI_HAN_LINK_PHUT } from "@/lib/mat-khau";

/** Câu backend trả cho mọi yêu cầu hợp lệ, dù email có tài khoản hay không (spec 0010, AC-2). */
export const CAU_PHAN_HOI_CHUNG =
  "Nếu email này thuộc một tài khoản đang hoạt động, link đặt lại mật khẩu sẽ được gửi tới trong vài phút.";

const schema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập email")
    .max(150, "Email không hợp lệ")
    .regex(/^[^\s@]+@[^\s@]+\.[^\s@]+$/, "Email không hợp lệ"),
});

type Values = z.output<typeof schema>;

/**
 * Form yêu cầu link đặt lại mật khẩu (spec 0010, AC-13). Gửi xong thì thay form bằng câu phản hồi chung trong vùng
 * `role="status"` và đưa focus tới đó; "Gửi lại" đưa form trở lại với email đã nhập.
 */
export function QuenMatKhauForm() {
  const form = useApiForm({ schema, defaultValues: { email: "" } });
  const [daGui, setDaGui] = useState(false);
  const ketQuaRef = useRef<HTMLDivElement>(null);
  const mutation = useMutation({
    mutationFn: (values: Values) => api(client.POST("/api/v1/auth/password-reset/request", { body: values })),
    meta: { [BO_QUA_CHUYEN_TRANG]: true },
  });

  useEffect(() => {
    if (daGui) ketQuaRef.current?.focus();
  }, [daGui]);

  const onSubmit = form.submitWith(async (values) => {
    await mutation.mutateAsync(values);
    setDaGui(true);
  });

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        <h1 className="type-page-title">Quên mật khẩu</h1>
        <p className="type-ui text-muted-foreground">
          Nhập email đã đăng ký, chúng tôi sẽ gửi link để bạn đặt mật khẩu mới.
        </p>
      </div>
      <div role="status" className="empty:hidden">
        {daGui && (
          <div
            ref={ketQuaRef}
            tabIndex={-1}
            className="flex flex-col gap-3 rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <Alert tone="success">
              <MailCheck aria-hidden="true" />
              <AlertDescription>
                <p>{CAU_PHAN_HOI_CHUNG}</p>
                <p>Link có hiệu lực {THOI_HAN_LINK_PHUT} phút.</p>
              </AlertDescription>
            </Alert>
            <p className="type-caption text-muted-foreground">
              Không thấy email? Hãy xem cả thư mục thư rác, hoặc gửi lại sau một phút.
            </p>
          </div>
        )}
      </div>
      {daGui ? (
        <Button type="button" variant="outline" className="w-full" onClick={() => setDaGui(false)}>
          Gửi lại
        </Button>
      ) : (
        <form onSubmit={onSubmit} noValidate aria-label="Quên mật khẩu" className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="email"
              label="Email"
              required
              render={(field) => (
                <Input type="email" autoComplete="email" autoCapitalize="none" spellCheck={false} {...field} />
              )}
            />
          </FieldGroup>
          <SubmitButton form={form} size="lg" className="w-full">
            Gửi link đặt lại
          </SubmitButton>
        </form>
      )}
      <p className="text-center type-ui">
        <Link href={TRANG_DANG_NHAP}>Quay lại đăng nhập</Link>
      </p>
    </div>
  );
}
