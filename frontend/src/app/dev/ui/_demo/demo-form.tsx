"use client";

import { useState } from "react";
import * as z from "zod";
import { FormAlert } from "@/components/form/form-alert";
import { CheckboxControl, SelectControl, SwitchControl } from "@/components/form/field-controls";
import { FormField } from "@/components/form/form-field";
import { notifySuccess } from "@/components/feedback/notify";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { SelectItem } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { fakeSubmit, SCENARIOS, type Scenario } from "./demo-api";

const schema = z.object({
  hoTen: z.string().trim().min(1, "Vui lòng nhập họ và tên."),
  soDienThoai: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập số điện thoại.")
    .regex(/^0\d{9}$/, "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0."),
  email: z.union([z.literal(""), z.email("Email chưa đúng định dạng.")]),
  loaiCanHo: z.string().min(1, "Vui lòng chọn loại căn hộ."),
  ghiChu: z.string().max(200, "Ghi chú tối đa 200 ký tự."),
  nhanThongBao: z.boolean(),
  dongY: z.boolean().refine((value) => value, "Vui lòng xác nhận thông tin là đúng."),
});

type DemoFormProps = {
  /** Cho test rút ngắn độ trễ của hàm gửi giả. */
  delayMs?: number;
  onSuccess?: () => void;
};

/** Form mẫu: kiểm tra phía trình duyệt và mọi tình huống lỗi API (AC-5, AC-6, AC-7). */
export function DemoForm({ delayMs, onSuccess }: DemoFormProps) {
  const [scenario, setScenario] = useState<Scenario>("ok");
  const form = useApiForm({
    schema,
    defaultValues: {
      hoTen: "",
      soDienThoai: "",
      email: "",
      loaiCanHo: "",
      ghiChu: "",
      nhanThongBao: true,
      dongY: false,
    },
  });

  const onSubmit = form.submitWith(async () => {
    await fakeSubmit(scenario, delayMs);
    notifySuccess("Đã lưu", "Thông tin cư dân đã được cập nhật.");
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
          <FormField
            control={form.control}
            name="loaiCanHo"
            label="Loại căn hộ"
            required
            render={(field) => (
              <SelectControl field={field} placeholder="Chọn loại căn hộ">
                <SelectItem value="studio">Studio</SelectItem>
                <SelectItem value="1pn">1 phòng ngủ</SelectItem>
                <SelectItem value="2pn">2 phòng ngủ</SelectItem>
                <SelectItem value="3pn">3 phòng ngủ</SelectItem>
              </SelectControl>
            )}
          />
          <FormField
            control={form.control}
            name="ghiChu"
            label="Ghi chú"
            description="Tối đa 200 ký tự."
            render={(field) => <Textarea {...field} />}
          />
          <FormField
            control={form.control}
            name="nhanThongBao"
            label="Nhận thông báo qua email"
            description="Thông báo phí, lịch bảo trì và khảo sát."
            layout="horizontal"
            render={(field) => <SwitchControl field={field} />}
          />
          <FormField
            control={form.control}
            name="dongY"
            label="Tôi xác nhận thông tin trên là đúng"
            required
            layout="horizontal"
            render={(field) => <CheckboxControl field={field} />}
          />
        </FieldGroup>
        <div className="flex flex-wrap items-center gap-3">
          <SubmitButton form={form}>Lưu</SubmitButton>
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
