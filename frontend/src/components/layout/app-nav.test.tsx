import { render, screen, within } from "@testing-library/react";
import { Building2, Gauge, Receipt } from "lucide-react";
import { describe, expect, it } from "vitest";
import { AppNav } from "./app-nav";
import type { NavGroup } from "./nav";

const NAV: NavGroup[] = [
  { items: [{ href: "/", label: "Tổng quan", icon: Gauge }] },
  { label: "Ban quản lý", items: [{ href: "/toa-nha", label: "Tòa nhà", icon: Building2 }] },
  { label: "Kế toán", items: [{ href: "/doi-soat", label: "Đối soát", icon: Receipt }] },
];

describe("AppNav (spec 0006, AC-16)", () => {
  it("mỗi nhóm có nhãn cho danh sách của nó đúng tên nhóm đó", () => {
    render(<AppNav nav={NAV} />);
    const nav = screen.getByRole("navigation", { name: "Menu chính" });
    expect(within(within(nav).getByRole("list", { name: "Ban quản lý" })).getByRole("link")).toHaveAccessibleName(
      "Tòa nhà",
    );
    expect(within(within(nav).getByRole("list", { name: "Kế toán" })).getByRole("link")).toHaveAccessibleName(
      "Đối soát",
    );
  });

  it("nhóm đầu không nhãn: danh sách không có aria-labelledby, không trỏ tới id không tồn tại", () => {
    render(<AppNav nav={NAV} />);
    const lists = screen.getAllByRole("list");
    expect(lists).toHaveLength(3);
    expect(lists[0]).not.toHaveAttribute("aria-labelledby");
    expect(lists[0]).toHaveAccessibleName("");
  });

  it("mọi aria-labelledby trỏ tới một phần tử có thật", () => {
    render(<AppNav nav={NAV} />);
    for (const el of document.querySelectorAll("[aria-labelledby]")) {
      expect(document.getElementById(el.getAttribute("aria-labelledby")!)).not.toBeNull();
    }
  });

  it("hai menu cùng lúc (thanh bên và ngăn kéo) không trùng id nhãn nhóm", () => {
    render(
      <>
        <AppNav nav={NAV} />
        <AppNav nav={NAV} label="Menu di động" />
      </>,
    );
    const ids = [...document.querySelectorAll("[id]")].map((el) => el.id);
    expect(ids).toHaveLength(4);
    expect(new Set(ids).size).toBe(ids.length);
    const diDong = screen.getByRole("navigation", { name: "Menu di động" });
    expect(within(diDong).getByRole("list", { name: "Kế toán" })).toBeInTheDocument();
  });
});
