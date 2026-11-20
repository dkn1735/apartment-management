import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { expectNoA11yViolations } from "@/test/axe";
import AppNotFound from "./not-found";

describe("404 trong khung app (spec 0006, AC-11)", () => {
  it("hiện h1 Không tìm thấy trang và nút Về trang chủ", async () => {
    const { container } = render(<AppNotFound />);
    expect(screen.getByRole("heading", { level: 1, name: "Không tìm thấy trang" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    await expectNoA11yViolations(container);
  });

  it("không tự bọc <main>, vì AppShell đã có một main", () => {
    render(<AppNotFound />);
    expect(screen.queryByRole("main")).not.toBeInTheDocument();
  });
});
