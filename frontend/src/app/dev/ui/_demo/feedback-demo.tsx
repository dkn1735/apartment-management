"use client";

import { Plus, RotateCcw, SearchX } from "lucide-react";
import { EmptyState } from "@/components/feedback/empty-state";
import { ErrorState } from "@/components/feedback/error-state";
import { notifyError, notifySuccess } from "@/components/feedback/notify";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/api";
import { DemoGroup } from "./demo-section";

const internalError = new ApiError({
  status: 500,
  errorCode: "INTERNAL_ERROR",
  message: "raw",
  requestId: "7c9e6679-7425-40de-944b-e07fc1f90ae7",
});

/** Toast, EmptyState, ErrorState. */
export function FeedbackDemo() {
  return (
    <>
      <DemoGroup label="Toast">
        <div className="flex flex-wrap gap-3">
          <Button variant="secondary" onClick={() => notifySuccess("Đã lưu", "Thông tin cư dân đã được cập nhật.")}>
            Toast thành công
          </Button>
          <Button variant="secondary" onClick={() => notifyError(internalError)}>
            Toast lỗi có mã tra cứu
          </Button>
          <Button
            variant="secondary"
            onClick={() => notifyError(new ApiError({ status: 403, errorCode: "FORBIDDEN", message: "raw" }))}
          >
            Toast lỗi 403
          </Button>
        </div>
        <p className="mt-3 type-caption text-muted-foreground">
          Toast thành công tự tắt sau 5 giây. Toast lỗi không tự tắt, đóng bằng nút &quot;Đóng&quot;.
        </p>
      </DemoGroup>
      <DemoGroup label="EmptyState">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded-md border">
            <EmptyState
              description="Khi có căn hộ mới, chúng sẽ hiện ở đây."
              action={
                <Button size="sm">
                  <Plus aria-hidden="true" />
                  Thêm căn hộ
                </Button>
              }
            />
          </div>
          <div className="rounded-md border">
            <EmptyState icon={SearchX} title="Không có kết quả" description="Thử bỏ bớt bộ lọc hoặc đổi từ khóa." />
          </div>
        </div>
      </DemoGroup>
      <DemoGroup label="ErrorState">
        <div className="rounded-md border">
          <ErrorState
            headingLevel="h4"
            title="Không tải được dữ liệu"
            error={internalError}
            actions={
              <Button size="sm" variant="outline">
                <RotateCcw aria-hidden="true" />
                Thử lại
              </Button>
            }
          />
        </div>
      </DemoGroup>
    </>
  );
}
