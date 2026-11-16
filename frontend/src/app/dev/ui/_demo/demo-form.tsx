"use client";

import { useState } from "react";
import * as z from "zod";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { fakeSubmit, SCENARIOS, type Scenario } from "./demo-api";

const schema = z.object({
  hoTen: z.string().trim().min(1, "Vui lòng nhập họ và tên."),
  soDienThoai: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập số điện thoại.")
    .regex(/^0\d{9}$/, "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0."),
  email: z.union([z.literal(""), z.email("Email chưa đúng định dạng.")]),
});

type DemoFormProps = {
  /** Cho test rút ngắn độ trễ của hàm gửi giả. */
  delayMs?: number;
  onSuccess?: () => void;
};

/** Form mẫu: kiểm tra phía trình duyệt và mọi tình huống lỗi API (AC-5, AC-6, AC-7). */
export function DemoForm({ delayMs, onSuccess }: DemoFormProps) {
  const [scenario, setScenario] = useState<Scenario>("ok");
  const [status, setStatus] = useState("");
  const form = useApiForm({ schema, defaultValues: { hoTen: "", soDienThoai: "", email: "" } });

  const onSubmit = form.submitWith(async () => {
    setStatus("");
    await fakeSubmit(scenario, delayMs);
    setStatus("Đã lưu");
    onSuccess?.();
  });

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
      <form onSubmit={onSubmit} noValidate aria-label="Form mẫu" className="flex flex-col gap-5">
        <FormAlert error={form.formError} focusKey={form.formErrorKey} />
        <FieldGroup>
          <FormField
            control={form.control}
            name="hoTen"
            label="Họ và tên"
            required
            render={(field) => <Input autoComplete="name" {...field} />}
          />
          <FormField
            control={form.control}
            name="soDienThoai"
            label="Số điện thoại"
            required
            description="10 chữ số, ví dụ 0912345678."
            render={(field) => <Input type="tel" inputMode="numeric" autoComplete="tel" {...field} />}
          />
          <FormField
            control={form.control}
            name="email"
            label="Email"
            render={(field) => <Input type="email" autoComplete="email" {...field} />}
          />
        </FieldGroup>
        <div className="flex flex-wrap items-center gap-3">
          <SubmitButton form={form}>Lưu</SubmitButton>
          <span aria-live="polite" className="type-ui text-success">
            {status}
          </span>
        </div>
      </form>
      <fieldset className="flex flex-col gap-2 self-start rounded-md bg-muted p-4">
        <legend className="float-left mb-2 type-eyebrow text-primary">Hàm gửi giả trả về</legend>
        {SCENARIOS.map((item) => (
          <Button
            key={item.value}
            variant={scenario === item.value ? "primary" : "outline"}
            size="sm"
            aria-pressed={scenario === item.value}
            className="justify-start"
            onClick={() => setScenario(item.value)}
          >
            {item.label}
          </Button>
        ))}
      </fieldset>
    </div>
  );
}
