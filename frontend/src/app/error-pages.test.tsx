import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
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
});
