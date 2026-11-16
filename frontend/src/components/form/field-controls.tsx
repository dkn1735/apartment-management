"use client";

import { useRef, type ReactNode } from "react";
import type { FieldPath, FieldValues } from "react-hook-form";
import { Checkbox } from "@/components/ui/checkbox";
import { Select, SelectContent, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import type { FieldRenderProps } from "./form-field";

type ControlProps<T extends FieldValues, N extends FieldPath<T>> = { field: FieldRenderProps<T, N> };

/**
 * Select trong FormField: onValueChange thành onChange, ref và aria-* gắn vào trigger.
 * Chỉ báo "rời ô" khi danh sách đã đóng, để lỗi không hiện trong lúc đang chọn.
 */
export function SelectControl<T extends FieldValues, N extends FieldPath<T>>({
  field,
  placeholder = "Chọn một mục",
  children,
}: ControlProps<T, N> & { placeholder?: string; children: ReactNode }) {
  const { value, onChange, onBlur, ref, name: _name, ...aria } = field;
  const open = useRef(false);
  return (
    <Select
      value={value ?? ""}
      onValueChange={onChange}
      onOpenChange={(next) => {
        open.current = next;
        if (!next) onBlur();
      }}
    >
      <SelectTrigger ref={ref} onBlur={() => !open.current && onBlur()} {...aria}>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>{children}</SelectContent>
    </Select>
  );
}

/** Checkbox trong FormField (đặt `layout="horizontal"`): giá trị là boolean. */
export function CheckboxControl<T extends FieldValues, N extends FieldPath<T>>({ field }: ControlProps<T, N>) {
  const { value, onChange, name: _name, ...rest } = field;
  return <Checkbox checked={value === true} onCheckedChange={(checked) => onChange(checked === true)} {...rest} />;
}

/** Switch trong FormField (đặt `layout="horizontal"`): giá trị là boolean. */
export function SwitchControl<T extends FieldValues, N extends FieldPath<T>>({ field }: ControlProps<T, N>) {
  const { value, onChange, name: _name, ...rest } = field;
  return <Switch checked={value === true} onCheckedChange={(checked) => onChange(checked)} {...rest} />;
}
