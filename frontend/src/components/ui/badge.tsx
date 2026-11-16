import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

/** Chip nhỏ: cao 24px, đệm ngang 10px, chữ 13px nét 500, viên thuốc. Màu theo tone. */
const badgeVariants = cva(
  "inline-flex h-6 w-fit shrink-0 items-center gap-1 rounded-full px-2.5 type-eyebrow whitespace-nowrap [&>svg]:size-3.5 [&>svg]:shrink-0",
  {
    variants: {
      tone: {
        neutral: "bg-muted text-foreground",
        info: "bg-info-soft text-info",
        success: "bg-success-soft text-success",
        warning: "bg-warning-soft text-warning",
        danger: "bg-destructive-soft text-destructive",
        primary: "bg-primary-soft text-primary",
      },
    },
    defaultVariants: { tone: "neutral" },
  },
);

function Badge({
  className,
  tone = "neutral",
  ...props
}: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return <span data-slot="badge" data-tone={tone} className={cn(badgeVariants({ tone }), className)} {...props} />;
}

export { Badge, badgeVariants };
