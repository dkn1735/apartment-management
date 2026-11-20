import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { QueryErrorState } from "./query-error-state";

const loi = (status: number, errorCode: string) =>
  new ApiError({ status, errorCode, message: "", requestId: "req-123" });

describe("QueryErrorState (spec 0006, AC-12)", () => {
  it("FORBIDDEN hiện KhongCoQuyen cùng mức tiêu đề, không Thử lại, không mã tra cứu", () => {
    render(
      <QueryErrorState title="Không tải được" error={loi(403, "FORBIDDEN")} onRetry={vi.fn()} headingLevel="h3" />,
    );
    expect(screen.getByRole("heading", { level: 3, name: "Không có quyền truy cập" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Thử lại" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Về trang chủ" })).not.toBeInTheDocument();
    expect(screen.queryByText(/req-123/)).not.toBeInTheDocument();
  });

  it("lỗi khác hiện tiêu đề riêng và Thử lại", () => {
    render(<QueryErrorState title="Không tải được" error={loi(500, "INTERNAL_ERROR")} onRetry={vi.fn()} />);
    expect(screen.getByRole("heading", { level: 2, name: "Không tải được" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
  });
});
