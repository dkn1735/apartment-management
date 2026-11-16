import { CircleAlert, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";
import type { ApiError } from "@/lib/api/api";
import { messageFor, requestIdToShow } from "@/lib/api/error-messages";
import { RequestIdNote } from "./request-id-note";

type ErrorStateProps = {
  title: string;
  /** Mô tả tự viết; bỏ trống mà có `error` thì lấy câu theo errorCode. */
  description?: ReactNode;
  /** Lỗi API gây ra trạng thái này: cho câu mô tả và mã tra cứu (AC-7). */
  error?: ApiError | null;
  icon?: LucideIcon;
  /** Mức tiêu đề: `h1` khi là cả trang (404, lỗi, 403), `h2` tới `h4` theo vị trí trong trang. */
  headingLevel?: "h1" | "h2" | "h3" | "h4";
  /** Các nút, ví dụ "Thử lại" và "Về trang chủ". */
  actions?: ReactNode;
  className?: string;
};

/** Trạng thái lỗi dùng chung cho 404, lỗi bất ngờ, lỗi bảng và sau này 403. */
export function ErrorState({
  title,
  description,
  error,
  icon: Icon = CircleAlert,
  headingLevel: Heading = "h2",
  actions,
  className,
}: ErrorStateProps) {
  const text = description ?? (error ? messageFor(error) : undefined);
  const requestId = error ? requestIdToShow(error) : undefined;

  return (
    <div data-slot="error-state" className={cn("flex flex-col items-center gap-2 px-4 py-10 text-center", className)}>
      <span className="mb-1 flex size-12 items-center justify-center rounded-full bg-destructive-soft">
        <Icon aria-hidden="true" className="size-6 text-destructive" />
      </span>
      <Heading className={Heading === "h1" ? "type-page-title" : "type-card-title"}>{title}</Heading>
      {text && <div className="max-w-md type-ui text-muted-foreground">{text}</div>}
      {requestId && <RequestIdNote requestId={requestId} className="justify-center text-muted-foreground" />}
      {actions && <div className="mt-3 flex flex-wrap justify-center gap-3">{actions}</div>}
    </div>
  );
}
