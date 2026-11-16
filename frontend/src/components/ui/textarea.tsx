import * as React from "react";
import { cn } from "cn";

/** Ô nhập nhiều dòng, tự giãn theo nội dung. Chữ 16px dưới 640px (AC-18). */
function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "flex field-sizing-content min-h-24 w-full rounded-md border border-input bg-card px-3 py-2 text-base text-foreground transition-colors duration-150 ease-out placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive sm:text-sm",
        className,
      )}
      {...props}
    />
  );
}

export { Textarea };
