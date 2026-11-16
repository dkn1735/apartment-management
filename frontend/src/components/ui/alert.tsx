import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

/**
 * Khung thông báo trong trang: nền -soft của tone, viền trái 3px màu tone, luôn kèm icon.
 * Không tự đặt role; lỗi vừa xảy ra thì truyền role="alert".
 */
const alertVariants = cva(
  "grid w-full grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-1 rounded-md border-l-[3px] px-4 py-3 text-foreground type-ui [&>svg]:row-span-2 [&>svg]:mt-0.5 [&>svg]:size-4",
  {
    variants: {
      tone: {
        info: "border-info bg-info-soft [&>svg]:text-info",
        success: "border-success bg-success-soft [&>svg]:text-success",
        warning: "border-warning bg-warning-soft [&>svg]:text-warning",
        danger: "border-destructive bg-destructive-soft [&>svg]:text-destructive",
      },
    },
    defaultVariants: { tone: "info" },
  },
);

function Alert({ className, tone, ...props }: React.ComponentProps<"div"> & VariantProps<typeof alertVariants>) {
  return <div data-slot="alert" className={cn(alertVariants({ tone }), className)} {...props} />;
}

function AlertTitle({ className, ...props }: React.ComponentProps<"p">) {
  return <p data-slot="alert-title" className={cn("col-start-2 font-medium", className)} {...props} />;
}

function AlertDescription({ className, ...props }: React.ComponentProps<"div">) {
  return <div data-slot="alert-description" className={cn("col-start-2 flex flex-col gap-2", className)} {...props} />;
}

export { Alert, AlertTitle, AlertDescription };
