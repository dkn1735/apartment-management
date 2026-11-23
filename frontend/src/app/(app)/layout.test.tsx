import { QueryClientProvider } from "@tanstack/react-query";
import { render as rtlRender, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createQueryClient } from "@/lib/api/query-client";
import { mockPathname, mockReplace } from "@/test/next-navigation";
import AppLayout from "./layout";

const state = vi.hoisted(() => ({
  current: {} as { data?: { hoTen: string; vaiTro: string[]; phaiDoiMatKhau?: boolean }; isPending?: boolean },
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
  mockReplace.mockClear();
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

describe("Mật khẩu tạm (spec 0008, AC-14)", () => {
  it("trang khác thì chuyển sang /doi-mat-khau kèm next, ẩn menu điều hướng và nội dung", () => {
    state.current = { data: { hoTen: "Người Mới", vaiTro: ["QUAN_TRI_VIEN"], phaiDoiMatKhau: true } };
    mockPathname.mockReturnValue("/tai-khoan");
    render(<AppLayout>nội dung</AppLayout>);
    expect(mockReplace).toHaveBeenCalledWith("/doi-mat-khau?next=%2Ftai-khoan");
    expect(screen.queryByText("nội dung")).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Tài khoản" })).not.toBeInTheDocument();
    // Menu người dùng (đăng xuất) vẫn còn
    expect(screen.getByRole("banner")).toHaveTextContent("Người Mới");
  });

  it("ở trang đổi mật khẩu thì hiện nội dung, không chuyển", () => {
    state.current = { data: { hoTen: "Người Mới", vaiTro: ["QUAN_TRI_VIEN"], phaiDoiMatKhau: true } };
    mockPathname.mockReturnValue("/doi-mat-khau");
    render(<AppLayout>nội dung</AppLayout>);
    expect(screen.getByText("nội dung")).toBeInTheDocument();
    expect(mockReplace).not.toHaveBeenCalled();
  });
});
