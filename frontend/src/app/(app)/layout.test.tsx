import { QueryClientProvider } from "@tanstack/react-query";
import { render as rtlRender, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createQueryClient } from "@/lib/api/query-client";
import { mockPathname } from "@/test/next-navigation";
import AppLayout from "./layout";

const state = vi.hoisted(() => ({
  current: {} as { data?: { hoTen: string; vaiTro: string[] }; isPending?: boolean },
}));

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => state.current,
}));

// Menu người dùng có mutation đăng xuất, nên cần QueryClient
const render = (ui: ReactElement) =>
  rtlRender(<QueryClientProvider client={createQueryClient()}>{ui}</QueryClientProvider>);

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
    state.current = { data: { hoTen: "Vũ Cư Dân", vaiTro: ["CU_DAN"] } };
    render(<AppLayout>nội dung</AppLayout>);
    expect(screen.getByRole("banner")).toHaveTextContent("Vũ Cư Dân");
  });

  it("đang tải thì nút menu hiện Tài khoản, không hiện tên (spec 0005, AC-13)", () => {
    render(<AppLayout>nội dung</AppLayout>);
    expect(screen.getByRole("banner")).not.toHaveTextContent("Vũ Cư Dân");
    expect(screen.getByRole("button", { name: /^Tài khoản/ })).toBeInTheDocument();
  });

  it("menu theo vai trò: BQL có nhóm Ban quản lý với Tòa nhà (spec 0006, AC-10)", () => {
    state.current = { data: { hoTen: "Trần Ban Quản Lý", vaiTro: ["BAN_QUAN_LY"] } };
    render(<AppLayout>nội dung</AppLayout>);
    expect(screen.getAllByText("Ban quản lý").length).toBeGreaterThan(0);
    expect(screen.getAllByRole("link", { name: "Tòa nhà" })[0]).toHaveAttribute("href", "/toa-nha");
  });

  it("chưa biết vai trò thì menu là skeleton (spec 0006, AC-13)", () => {
    state.current = { isPending: true };
    render(<AppLayout>nội dung</AppLayout>);
    expect(screen.queryByRole("navigation", { name: "Menu chính" })).not.toBeInTheDocument();
    expect(screen.getAllByText("Đang tải menu").length).toBeGreaterThan(0);
  });
});
