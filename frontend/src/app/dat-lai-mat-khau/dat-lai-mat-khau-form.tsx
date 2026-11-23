"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Clock, TriangleAlert } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import * as z from "zod";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { PasswordInput } from "@/components/form/password-input";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { ErrorState } from "@/components/feedback/error-state";
import { RetryButton } from "@/components/feedback/retry-button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { FieldGroup } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { api, ApiError, client } from "@/lib/api/api";
import { ERROR_MESSAGES } from "@/lib/api/error-messages";
import { BO_QUA_CHUYEN_TRANG } from "@/lib/api/query-client";
import { TRANG_DANG_NHAP } from "@/lib/auth/safe-next";
import { formatGioNgayDayDu } from "@/lib/format";
import { matKhauMoiSchema, QUY_TAC_MAT_KHAU_MOI } from "@/lib/mat-khau";
import { docVaXoaToken } from "./doc-token";

const schema = z
  .object({
    matKhauMoi: matKhauMoiSchema,
    xacNhan: z.string().min(1, "Vui lòng nhập lại mật khẩu mới"),
  })
  .refine((v) => v.xacNhan === v.matKhauMoi, { path: ["xacNhan"], message: "Mật khẩu nhập lại chưa khớp" });

type Values = z.output<typeof schema>;

/** Sau khi đặt lại xong thì về đây; trang đăng nhập hiện câu thành công. */
export const DICH_THANH_CONG = `${TRANG_DANG_NHAP}?ly-do=dat-lai-mat-khau`;

type TrangThai =
  | { loai: "dang-kiem" }
  | { loai: "khong-hop-le" }
  | { loai: "loi"; error: unknown }
  | { loai: "hop-le"; hetHanLuc: string };

const laTokenHong = (error: unknown) => error instanceof ApiError && error.errorCode === "AUTH_RESET_TOKEN_INVALID";

/**
 * Đặt mật khẩu mới từ link trong email (spec 0010, AC-13). Token đọc từ fragment đúng một lần vào `useRef`, để effect
 * chạy hai lần ở StrictMode không thấy hash đã bị xóa. Mỗi lần đổi trạng thái, focus tới tiêu đề của trạng thái mới.
 */
export function DatLaiMatKhauForm() {
  const tokenRef = useRef<string | null | undefined>(undefined);
  const tieuDeRef = useRef<HTMLHeadingElement>(null);
  const [trangThai, setTrangThai] = useState<TrangThai>({ loai: "dang-kiem" });
  const [lanKiem, setLanKiem] = useState(0);
  // Bản sao của token cho handler gửi form; ref chỉ để đọc fragment đúng một lần
  const [token, setToken] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const router = useRouter();
  const form = useApiForm({ schema, defaultValues: { matKhauMoi: "", xacNhan: "" } });

  const kiemTra = useMutation({
    mutationFn: (token: string) => api(client.POST("/api/v1/auth/password-reset/check", { body: { token } })),
    meta: { [BO_QUA_CHUYEN_TRANG]: true },
  });
  const { mutateAsync: kiem } = kiemTra;
  const xacNhan = useMutation({
    mutationFn: ({ token, matKhauMoi }: { token: string; matKhauMoi: string }) =>
      api(client.POST("/api/v1/auth/password-reset/confirm", { body: { token, matKhauMoi } })),
    meta: { [BO_QUA_CHUYEN_TRANG]: true },
  });

  useEffect(() => {
    if (tokenRef.current === undefined) tokenRef.current = docVaXoaToken();
    const token = tokenRef.current;
    let huy = false;
    if (token === null) {
      setTrangThai({ loai: "khong-hop-le" });
      return;
    }
    setToken(token);
    setTrangThai({ loai: "dang-kiem" });
    kiem(token).then(
      (data) => {
        if (!huy) setTrangThai({ loai: "hop-le", hetHanLuc: (data as { hetHanLuc: string }).hetHanLuc });
      },
      (error: unknown) => {
        if (!huy) setTrangThai(laTokenHong(error) ? { loai: "khong-hop-le" } : { loai: "loi", error });
      },
    );
    return () => {
      huy = true;
    };
  }, [kiem, lanKiem]);

  useEffect(() => {
    if (trangThai.loai !== "dang-kiem") tieuDeRef.current?.focus();
  }, [trangThai.loai]);

  const onSubmit = form.submitWith(async ({ matKhauMoi }: Values) => {
    if (!token) return;
    try {
      await xacNhan.mutateAsync({ token, matKhauMoi });
    } catch (error) {
      if (laTokenHong(error)) {
        setTrangThai({ loai: "khong-hop-le" });
        return;
      }
      throw error;
    }
    // Mọi phiên đã bị thu hồi, kể cả phiên của máy này (AC-7)
    queryClient.clear();
    router.replace(DICH_THANH_CONG);
  });

  const tieuDe = (text: string) => (
    <h1 ref={tieuDeRef} tabIndex={-1} className="type-page-title outline-none">
      {text}
    </h1>
  );

  if (trangThai.loai === "dang-kiem") {
    return (
      <div className="flex flex-col gap-5">
        <h1 className="type-page-title">Đặt lại mật khẩu</h1>
        <p role="status" className="flex items-center gap-2 type-ui text-muted-foreground">
          <Spinner aria-hidden="true" />
          Đang kiểm tra link…
        </p>
      </div>
    );
  }

  if (trangThai.loai === "khong-hop-le") {
    return (
      <div className="flex flex-col gap-5">
        {tieuDe("Không dùng được link này")}
        <div role="alert">
          <Alert tone="warning">
            <TriangleAlert aria-hidden="true" />
            <AlertDescription>{ERROR_MESSAGES.AUTH_RESET_TOKEN_INVALID}</AlertDescription>
          </Alert>
        </div>
        <p className="flex flex-col gap-2 text-center type-ui">
          <Link href="/quen-mat-khau">Yêu cầu link mới</Link>
          <Link href={TRANG_DANG_NHAP}>Quay lại đăng nhập</Link>
        </p>
      </div>
    );
  }

  if (trangThai.loai === "loi") {
    return (
      <ErrorState
        title="Chưa kiểm tra được link"
        error={trangThai.error instanceof ApiError ? trangThai.error : null}
        headingLevel="h1"
        actions={<RetryButton onRetry={() => setLanKiem((n) => n + 1)} />}
      />
    );
  }

  const hanId = "han-link-dat-lai";
  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        {tieuDe("Đặt lại mật khẩu")}
        <p id={hanId} className="flex items-center gap-2 type-ui text-muted-foreground">
          <Clock aria-hidden="true" className="size-4 shrink-0" />
          Link có hiệu lực tới {formatGioNgayDayDu(trangThai.hetHanLuc)}
        </p>
      </div>
      <form
        onSubmit={onSubmit}
        noValidate
        aria-label="Đặt lại mật khẩu"
        aria-describedby={hanId}
        className="flex flex-col gap-5"
      >
        <FormAlert error={form.formError} focusKey={form.formErrorKey} />
        <FieldGroup>
          <FormField
            control={form.control}
            name="matKhauMoi"
            label="Mật khẩu mới"
            description={QUY_TAC_MAT_KHAU_MOI}
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
        <SubmitButton form={form} size="lg" className="w-full">
          Đặt mật khẩu mới
        </SubmitButton>
      </form>
      <p className="text-center type-ui">
        <Link href={TRANG_DANG_NHAP}>Quay lại đăng nhập</Link>
      </p>
    </div>
  );
}
