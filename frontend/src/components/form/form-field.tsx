"use client";

import { useId, type ReactNode } from "react";
import {
  Controller,
  type Control,
  type ControllerRenderProps,
  type FieldPath,
  type FieldValues,
} from "react-hook-form";
import { Field, FieldContent, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { FIELD_NAME_ATTR } from "./use-api-form";

/** Những gì `render` nhận để gắn vào ô nhập: nhãn, gợi ý, lỗi đã nối sẵn. */
export type FieldRenderProps<T extends FieldValues = FieldValues, N extends FieldPath<T> = FieldPath<T>> = Pick<
  ControllerRenderProps<T, N>,
  "name" | "value" | "onChange" | "onBlur" | "ref"
> & {
  id: string;
  "aria-invalid": boolean;
  "aria-describedby": string | undefined;
  "aria-required": boolean | undefined;
};

type FormFieldProps<T extends FieldValues, N extends FieldPath<T>, TOut> = {
  control: Control<T, unknown, TOut>;
  name: N;
  label: string;
  description?: ReactNode;
  required?: boolean;
  /** `horizontal`: ô đứng trước nhãn, dùng cho Checkbox và Switch. */
  layout?: "vertical" | "horizontal";
  render: (field: FieldRenderProps<T, N>) => ReactNode;
  className?: string;
};

/** Một ô trong form: nhãn gắn với ô, gợi ý và lỗi nối qua aria-describedby (AC-5). */
export function FormField<T extends FieldValues, N extends FieldPath<T>, TOut = T>({
  control,
  name,
  label,
  description,
  required,
  layout = "vertical",
  render,
  className,
}: FormFieldProps<T, N, TOut>) {
  const id = useId();
  const descriptionId = `${id}-goi-y`;
  const errorId = `${id}-loi`;

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => {
        const message = fieldState.error?.message;
        const describedBy = [description ? descriptionId : null, message ? errorId : null].filter(Boolean).join(" ");
        const input = render({
          id,
          name: field.name,
          value: field.value,
          onChange: field.onChange,
          onBlur: field.onBlur,
          ref: field.ref,
          "aria-invalid": !!message,
          "aria-describedby": describedBy || undefined,
          "aria-required": required || undefined,
        });
        const labelNode = (
          <FieldLabel htmlFor={id}>
            {label}
            {required && (
              <>
                <span aria-hidden="true" className="text-destructive">
                  *
                </span>
                <span className="sr-only">(bắt buộc)</span>
              </>
            )}
          </FieldLabel>
        );
        const hint = description && <FieldDescription id={descriptionId}>{description}</FieldDescription>;
        const error = <FieldError id={errorId}>{message}</FieldError>;

        return (
          <Field
            className={className}
            orientation={layout}
            data-invalid={!!message || undefined}
            {...{ [FIELD_NAME_ATTR]: name }}
          >
            {layout === "horizontal" ? (
              <>
                {input}
                <FieldContent>
                  {labelNode}
                  {hint}
                  {error}
                </FieldContent>
              </>
            ) : (
              <>
                {labelNode}
                {input}
                {hint}
                {error}
              </>
            )}
          </Field>
        );
      }}
    />
  );
}
