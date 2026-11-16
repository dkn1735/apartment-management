"use client";

import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";
import { CircleAlert } from "lucide-react";

import { Label } from "@/components/ui/label";

function FieldSet({ className, ...props }: React.ComponentProps<"fieldset">) {
  return <fieldset data-slot="field-set" className={cn("flex min-w-0 flex-col gap-4", className)} {...props} />;
}

function FieldLegend({ className, ...props }: React.ComponentProps<"legend">) {
  return (
    <legend data-slot="field-legend" className={cn("mb-2 text-sm font-medium text-foreground", className)} {...props} />
  );
}

function FieldGroup({ className, ...props }: React.ComponentProps<"div">) {
  return <div data-slot="field-group" className={cn("flex w-full flex-col gap-5", className)} {...props} />;
}

const fieldVariants = cva("flex w-full gap-1.5", {
  variants: {
    orientation: {
      vertical: "flex-col",
      horizontal: "flex-row items-start gap-3",
    },
  },
  defaultVariants: { orientation: "vertical" },
});

/** Khung một ô nhập: nhãn, ô, gợi ý, lỗi. */
function Field({
  className,
  orientation = "vertical",
  ...props
}: React.ComponentProps<"div"> & VariantProps<typeof fieldVariants>) {
  return (
    <div
      data-slot="field"
      data-orientation={orientation}
      className={cn(fieldVariants({ orientation }), className)}
      {...props}
    />
  );
}

function FieldContent({ className, ...props }: React.ComponentProps<"div">) {
  return <div data-slot="field-content" className={cn("flex flex-1 flex-col gap-1", className)} {...props} />;
}

function FieldLabel({ className, ...props }: React.ComponentProps<typeof Label>) {
  return <Label data-slot="field-label" className={cn("w-fit", className)} {...props} />;
}

function FieldDescription({ className, ...props }: React.ComponentProps<"p">) {
  return <p data-slot="field-description" className={cn("type-caption text-muted-foreground", className)} {...props} />;
}

/** Lỗi của một ô: luôn có icon cảnh báo kèm chữ, không chỉ đổi màu. */
function FieldError({ className, children, ...props }: React.ComponentProps<"p">) {
  if (!children) {
    return null;
  }
  return (
    <p
      data-slot="field-error"
      className={cn("flex items-start gap-1.5 type-caption text-destructive", className)}
      {...props}
    >
      <CircleAlert aria-hidden="true" className="mt-0.5 size-3.5 shrink-0" />
      <span>{children}</span>
    </p>
  );
}

export { Field, FieldLabel, FieldDescription, FieldError, FieldGroup, FieldLegend, FieldSet, FieldContent };
