import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { expectNoA11yViolations } from "@/test/axe";
import { Button } from "./button";

describe("Button", () => {
  it("mặc định là type=button và gọi onClick", async () => {
    const onClick = vi.fn();
    const { container } = render(<Button onClick={onClick}>Lưu</Button>);
    await expectNoA11yViolations(container);
    expect(screen.getByRole("button")).toHaveAttribute("type", "button");
    await userEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("đang tải thì có chữ Đang tải, aria-busy, aria-disabled, không disabled và chặn bấm", async () => {
    const onClick = vi.fn();
    const { container } = render(
      <Button loading onClick={onClick}>
        Lưu
      </Button>,
    );
    await expectNoA11yViolations(container);
    const button = screen.getByRole("button", { name: /^Đang tải:\s*Lưu$/ });
    expect(button).toHaveAttribute("aria-busy", "true");
    expect(button).toHaveAttribute("aria-disabled", "true");
    expect(button).not.toBeDisabled();
    await userEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });
});
