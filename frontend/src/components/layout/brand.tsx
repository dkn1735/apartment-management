import { Building2 } from "lucide-react";
import { cn } from "cn";
import { APP_NAME } from "@/lib/brand";

/** Thương hiệu: icon Building2 màu chính cạnh tên app. */
export function Brand({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2 font-medium text-foreground", className)}>
      <span className="flex size-8 items-center justify-center rounded-md bg-primary-soft">
        <Building2 aria-hidden="true" className="size-[18px] text-primary" />
      </span>
      <span className="type-card-title">{APP_NAME}</span>
    </span>
  );
}
