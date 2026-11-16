import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { expectNoA11yViolations } from "@/test/axe";
import { RequestIdNote } from "./request-id-note";

describe("RequestIdNote (AC-7)", () => {
  it("sao chép mã và báo Đã sao chép", async () => {
    const user = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();
    const { container } = render(<RequestIdNote requestId="abc-123" />);
    await expectNoA11yViolations(container);
    await user.click(screen.getByRole("button", { name: "Sao chép mã tra cứu" }));
    expect(writeText).toHaveBeenCalledWith("abc-123");
    expect(await screen.findByText("Đã sao chép")).toBeInTheDocument();
  });

  it("trình duyệt chặn sao chép thì bôi chọn mã và hướng dẫn Ctrl+C", async () => {
    const user = userEvent.setup();
    vi.spyOn(navigator.clipboard, "writeText").mockRejectedValue(new Error("blocked"));
    render(<RequestIdNote requestId="abc-123" />);
    await user.click(screen.getByRole("button", { name: "Sao chép mã tra cứu" }));
    expect(await screen.findByText("Hãy nhấn Ctrl+C để sao chép")).toBeInTheDocument();
    expect(window.getSelection()?.toString()).toBe("abc-123");
  });
});
