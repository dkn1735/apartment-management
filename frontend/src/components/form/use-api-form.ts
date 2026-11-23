"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRef, useState, type FormEvent } from "react";
import { useForm, type DefaultValues, type FieldValues, type Path, type Resolver } from "react-hook-form";
import type * as z from "zod";
import { ApiError } from "@/lib/api/api";

/** Thuộc tính đánh dấu khung của một ô, để biết thứ tự các ô trong form (FormField đặt). */
export const FIELD_NAME_ATTR = "data-field-name";

type UseApiFormOptions<TSchema extends z.ZodType<FieldValues, FieldValues>> = {
  schema: TSchema;
  defaultValues: DefaultValues<z.input<TSchema>>;
};

/**
 * Form có kiểm tra bằng Zod và nhận lỗi API (spec 0003, Hợp đồng form).
 * `submitWith(fn)`: `fn` phải trả về promise (dùng `mutateAsync`). Lỗi `ApiError` được gắn vào ô hoặc `formError`;
 * lỗi khác được ném lại trong lần render sau để error boundary (`error.tsx`) bắt.
 */
export function useApiForm<TSchema extends z.ZodType<FieldValues, FieldValues>>({
  schema,
  defaultValues,
}: UseApiFormOptions<TSchema>) {
  type Input = z.input<TSchema>;
  type Output = z.output<TSchema>;

  const form = useForm<Input, unknown, Output>({
    // Kiểu chung của TSchema làm TypeScript không tự suy ra được input/output, nên ép về đúng Resolver
    resolver: zodResolver(schema) as unknown as Resolver<Input, unknown, Output>,
    defaultValues,
    mode: "onTouched",
  });
  const [formError, setFormError] = useState<ApiError | null>(null);
  const [formErrorKey, setFormErrorKey] = useState(0);
  const [fatal, setFatal] = useState<unknown>(null);
  const submitting = useRef(false);

  if (fatal) {
    throw fatal;
  }

  function applyApiError(error: ApiError, formElement: HTMLFormElement | null) {
    // Tên các ô theo đúng thứ tự xuất hiện trong form
    const fieldOrder = formElement
      ? Array.from(formElement.querySelectorAll(`[${FIELD_NAME_ATTR}]`), (node) => node.getAttribute(FIELD_NAME_ATTR))
      : [];
    const matched = error.errors.filter((item) => item.field && fieldOrder.includes(item.field));
    const unmatched = error.errors.filter((item) => !matched.includes(item));

    for (const item of matched) {
      form.setError(item.field as Path<Input>, { type: "server", message: item.message });
    }

    const hasGeneralError = matched.length === 0 || unmatched.length > 0;
    if (hasGeneralError) {
      setFormError(
        new ApiError({
          status: error.status,
          errorCode: error.errorCode,
          message: error.message,
          errors: unmatched,
          requestId: error.requestId,
          retryAfter: error.retryAfter,
        }),
      );
    }

    const first = fieldOrder.find((name) => matched.some((item) => item.field === name));
    if (first) {
      form.setFocus(first as Path<Input>);
    } else {
      setFormErrorKey((key) => key + 1);
    }
  }

  function submitWith(fn: (values: Output) => Promise<unknown>) {
    return async (event?: FormEvent<HTMLFormElement>) => {
      const formElement = event?.currentTarget ?? null;
      if (submitting.current) {
        event?.preventDefault();
        return;
      }
      submitting.current = true;
      setFormError(null);
      try {
        await form.handleSubmit(async (values) => {
          try {
            await fn(values);
          } catch (error) {
            if (!(error instanceof ApiError)) {
              throw error;
            }
            applyApiError(error, formElement);
          }
        })(event);
      } catch (error) {
        setFatal(error);
      } finally {
        submitting.current = false;
      }
    };
  }

  return { ...form, formError, formErrorKey, submitWith };
}
