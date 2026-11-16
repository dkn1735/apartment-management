import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { expectNoA11yViolations } from "@/test/axe";
import ErrorPage from "./error";
import NotFound from "./not-found";

describe("trang lỗi (AC-13)", () => {
  it("not-found có h1 tiếng Việt và nút Về trang chủ", async () => {
    const { container } = render(<NotFound />);
    expect(screen.getByRole("heading", { level: 1, name: "Không tìm thấy trang" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    await expectNoA11yViolations(container);
  });

  it("error có nút Thử lại gọi retry và không lộ message của lỗi", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    const retry = vi.fn();
    const { container } = render(<ErrorPage error={new Error("SQL: select * from tai_khoan")} retry={retry} />);
    expect(screen.getByRole("heading", { level: 1, name: "Đã có lỗi xảy ra" })).toBeInTheDocument();
    expect(container).not.toHaveTextContent("SQL");
    await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(retry).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    await expectNoA11yViolations(container);
  });

  it("error ghi lỗi ra console để còn dấu vết khi gỡ lỗi", () => {
    const log = vi.spyOn(console, "error").mockImplementation(() => {});
    const error = new Error("boom");
    render(<ErrorPage error={error} retry={vi.fn()} />);
    expect(log).toHaveBeenCalledWith(error);
  });

  it("error với ApiError 5xx hiện câu theo errorCode và mã tra cứu, không lộ message (AC-7)", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    const error = new ApiError({ status: 500, errorCode: "INTERNAL_ERROR", message: "raw stack", requestId: "r-500" });
    const { container } = render(<ErrorPage error={error} retry={vi.fn()} />);
    expect(screen.getByText("Hệ thống đang gặp sự cố, vui lòng thử lại sau.")).toBeInTheDocument();
    expect(screen.getByText("r-500")).toBeInTheDocument();
    expect(container).not.toHaveTextContent("raw stack");
    await expectNoA11yViolations(container);
  });

  it("error với ApiError 4xx không hiện mã tra cứu (AC-7)", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    const error = new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "raw", requestId: "r-404" });
    render(<ErrorPage error={error} retry={vi.fn()} />);
    expect(screen.getByText("Không tìm thấy dữ liệu yêu cầu, có thể nó đã bị xóa.")).toBeInTheDocument();
    expect(screen.queryByText("r-404")).not.toBeInTheDocument();
  });
});
