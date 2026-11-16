import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { mockPathname } from "@/test/next-navigation";
import AppLayout from "./layout";

const state = vi.hoisted(() => ({ current: {} as { data?: { hoTen: string } } }));

vi.mock("@/lib/auth/use-ho-so", () => ({ useHoSo: () => state.current }));

beforeEach(() => {
  state.current = {};
  mockPathname.mockReturnValue("/ho-so");
});

describe("Layout (app) (AC-14)", () => {
  it("mục Hồ sơ đang chọn ở /ho-so", () => {
    render(<AppLayout>nội dung</AppLayout>);
    for (const link of screen.getAllByRole("link", { name: "Hồ sơ" })) {
      expect(link).toHaveAttribute("aria-current", "page");
    }
  });

  it("thanh trên hiện họ tên khi đã có hồ sơ", () => {
    state.current = { data: { hoTen: "Vũ Cư Dân" } };
    render(<AppLayout>nội dung</AppLayout>);
    expect(screen.getByRole("banner")).toHaveTextContent("Vũ Cư Dân");
  });

  it("đang tải thì không hiện tên", () => {
    render(<AppLayout>nội dung</AppLayout>);
    expect(screen.getByRole("banner")).not.toHaveTextContent("Vũ Cư Dân");
  });
});
