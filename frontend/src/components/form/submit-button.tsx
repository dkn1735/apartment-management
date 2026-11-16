"use client";

import { useFormState, type FieldValues, type UseFormReturn } from "react-hook-form";
import { Button, type ButtonProps } from "@/components/ui/button";

type SubmitButtonProps<T extends FieldValues> = Omit<ButtonProps, "type" | "loading" | "form"> & {
  form: Pick<UseFormReturn<T>, "control">;
};

/** Nút gửi form: tự ở trạng thái đang tải trong lúc gửi (AC-5). */
export function SubmitButton<T extends FieldValues>({ form, children, ...props }: SubmitButtonProps<T>) {
  const { isSubmitting } = useFormState({ control: form.control });
  return (
    <Button type="submit" loading={isSubmitting} {...props}>
      {children}
    </Button>
  );
}
