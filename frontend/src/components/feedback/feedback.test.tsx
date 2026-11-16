import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { Toaster } from "@/components/ui/sonner";
import { ApiError } from "@/lib/api/api";
import { expectNoA11yViolations } from "@/test/axe";
import { EmptyState } from "./empty-state";
import { ErrorState } from "./error-state";
import { notifyError, notifySuccess } from "./notify";

afterEach(() => {
  act(() => toast.dismiss());
  vi.useRealTimers();
});

describe("EmptyState và ErrorState", () => {
  it("EmptyState có tiêu đề mặc định và không vi phạm axe", async () => {
    const { container } = render(<EmptyState description="Chưa có căn hộ nào." />);
    expect(screen.getByText("Chưa có dữ liệu")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("ErrorState lấy câu theo errorCode và hiện mã tra cứu cho lỗi 5xx", async () => {
    const error = new ApiError({ status: 500, errorCode: "INTERNAL_ERROR", message: "raw", requestId: "r-9" });
    const { container } = render(<ErrorState title="Không tải được" error={error} />);
    expect(screen.getByRole("heading", { level: 2, name: "Không tải được" })).toBeInTheDocument();
    expect(screen.getByText("Hệ thống đang gặp sự cố, vui lòng thử lại sau.")).toBeInTheDocument();
    expect(screen.getByText("r-9")).toBeInTheDocument();
    expect(container).not.toHaveTextContent("raw");
    await expectNoA11yViolations(container);
  });
});

describe("toast (AC-8)", () => {
  it("toast thành công tự tắt sau 5 giây", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    render(<Toaster />);
    act(() => {
      notifySuccess("Đã lưu");
    });
    expect(await screen.findByText("Đã lưu")).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(6000);
    });
    await waitFor(() => expect(screen.queryByText("Đã lưu")).not.toBeInTheDocument());
  });

  it("toast lỗi không tự tắt, có nút Đóng và mã tra cứu", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    render(<Toaster />);
    act(() => {
      notifyError(new ApiError({ status: 503, errorCode: "XYZ", message: "raw", requestId: "r-7" }));
    });
    expect(await screen.findByText("Hệ thống đang gặp sự cố, vui lòng thử lại sau.")).toBeInTheDocument();
    expect(screen.getByText("r-7")).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(30_000);
    });
    expect(screen.getByText("Hệ thống đang gặp sự cố, vui lòng thử lại sau.")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Đóng" }));
    await waitFor(() =>
      expect(screen.queryByText("Hệ thống đang gặp sự cố, vui lòng thử lại sau.")).not.toBeInTheDocument(),
    );
  });

  it("vùng toast có nhãn tiếng Việt", () => {
    render(<Toaster />);
    expect(screen.getByRole("region", { name: /Thông báo/ })).toBeInTheDocument();
  });
});
