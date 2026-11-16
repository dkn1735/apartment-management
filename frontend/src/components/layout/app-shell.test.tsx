import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Building2, Gauge, Users } from "lucide-react";
import { afterEach, describe, expect, it } from "vitest";
import { mockPathname } from "@/test/next-navigation";
import { expectNoA11yViolations } from "@/test/axe";
import { AppShell } from "./app-shell";
import { activeHref, type NavGroup } from "./nav";

const NAV: NavGroup[] = [
  { items: [{ href: "/", label: "Trang chủ", icon: Gauge }] },
  {
    label: "Vận hành",
    items: [
      { href: "/can-ho", label: "Căn hộ", icon: Building2 },
      { href: "/can-ho-khac", label: "Căn hộ khác", icon: Building2 },
      { href: "/can-ho/123/cu-dan", label: "Cư dân căn hộ", icon: Users },
    ],
  },
];
const HREFS = NAV.flatMap((group) => group.items.map((item) => item.href));

afterEach(() => mockPathname.mockReturnValue("/"));

describe("activeHref (AC-12)", () => {
  it("so khớp theo từng đoạn đường dẫn, chọn href dài nhất", () => {
    expect(activeHref("/can-ho/123", HREFS)).toBe("/can-ho");
    expect(activeHref("/can-ho-khac", HREFS)).toBe("/can-ho-khac");
    expect(activeHref("/can-ho/123/cu-dan/5", HREFS)).toBe("/can-ho/123/cu-dan");
    expect(activeHref("/", HREFS)).toBe("/");
    expect(activeHref("/khac", HREFS)).toBeUndefined();
  });
});

describe("AppShell (AC-4, AC-12)", () => {
  function setup(pathname: string) {
    mockPathname.mockReturnValue(pathname);
    const user = userEvent.setup();
    const view = render(
      <AppShell nav={NAV}>
        <h1>Nội dung</h1>
      </AppShell>,
    );
    return { user, ...view };
  }

  it("ở /can-ho/123 chỉ mục /can-ho có aria-current; không vi phạm axe", async () => {
    const { container } = setup("/can-ho/123");
    const sidebar = screen.getByRole("navigation", { name: "Menu chính" });
    expect(within(sidebar).getByRole("link", { name: "Căn hộ" })).toHaveAttribute("aria-current", "page");
    expect(within(sidebar).getByRole("link", { name: "Trang chủ" })).not.toHaveAttribute("aria-current");
    expect(within(sidebar).getByRole("link", { name: "Căn hộ khác" })).not.toHaveAttribute("aria-current");
    await expectNoA11yViolations(container);
  });

  it("liên kết bỏ qua là điểm Tab đầu tiên và trỏ tới main#noi-dung", async () => {
    const { user } = setup("/");
    await user.tab();
    const skip = screen.getByRole("link", { name: "Bỏ qua, tới nội dung chính" });
    expect(skip).toHaveFocus();
    expect(skip).toHaveAttribute("href", "#noi-dung");
    const main = screen.getByRole("main");
    expect(main).toHaveAttribute("id", "noi-dung");
    expect(main).toHaveAttribute("tabindex", "-1");
  });

  it("ngăn kéo mở bằng nút Mở menu, đóng khi đường dẫn đổi và trả focus", async () => {
    const { user, rerender } = setup("/can-ho");
    await user.click(screen.getByRole("button", { name: "Mở menu" }));
    const drawer = await screen.findByRole("dialog");
    expect(within(drawer).getByRole("link", { name: "Căn hộ" })).toHaveAttribute("aria-current", "page");
    await expectNoA11yViolations(drawer);

    mockPathname.mockReturnValue("/can-ho-khac");
    rerender(
      <AppShell nav={NAV}>
        <h1>Nội dung</h1>
      </AppShell>,
    );
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("bấm một mục trong ngăn kéo thì ngăn kéo đóng", async () => {
    const { user } = setup("/");
    await user.click(screen.getByRole("button", { name: "Mở menu" }));
    const drawer = await screen.findByRole("dialog");
    await user.click(within(drawer).getByRole("link", { name: "Căn hộ" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("Esc đóng ngăn kéo và focus về nút Mở menu", async () => {
    const { user } = setup("/");
    await user.click(screen.getByRole("button", { name: "Mở menu" }));
    await screen.findByRole("dialog");
    await user.keyboard("{Escape}");
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Mở menu" })).toHaveFocus();
  });
});
