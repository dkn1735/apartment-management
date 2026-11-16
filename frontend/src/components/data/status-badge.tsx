import type { ReactNode } from "react";
import { Badge } from "@/components/ui/badge";

export type StatusTone = "neutral" | "info" | "success" | "warning" | "danger";

type StatusBadgeProps = { tone: StatusTone; children: ReactNode; className?: string };

/** Chip trạng thái. Mỗi tính năng tự ánh xạ trạng thái nghiệp vụ sang `tone`; chữ luôn nói rõ trạng thái. */
export function StatusBadge({ tone, children, className }: StatusBadgeProps) {
  return (
    <Badge tone={tone} className={className}>
      {children}
    </Badge>
  );
}
