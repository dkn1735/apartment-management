import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { expectNoA11yViolations } from "@/test/axe";
import { TongQuanView } from "./tong-quan-view";

const state = vi.hoisted(() => ({ current: {} as Record<string, unknown> }));

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => state.current,
}));

const coVaiTro = (vaiTro: string[]) => ({ data: { hoTen: "Trần Ban Quản Lý", vaiTro } });

beforeEach(() => {
  state.current = {};
});

describe("Trang Tổng quan (spec 0006, AC-14, AC-16)", () => {
  it("chào theo tên và có thẻ lối tắt theo nhóm vai trò", async () => {
    state.current = coVaiTro(["BAN_QUAN_LY"]);
    const { container } = render(<TongQuanView />);
    expect(screen.getByRole("heading", { level: 1, name: "Xin chào, Trần Ban Quản Lý" })).toBeInTheDocument();
    const nhom = screen.getByRole("region", { name: "Ban quản lý" });
    const the = within(nhom).getByRole("link", { name: "Tòa nhà" });
    expect(the).toHaveAttribute("href", "/toa-nha");
    expect(the).toHaveAccessibleDescription("Danh mục tòa nhà, số tầng và số căn hộ");
    await expectNoA11yViolations(container);
  });

  it("không có vai trò thì báo chưa được gán", () => {
    state.current = coVaiTro([]);
    render(<TongQuanView />);
    expect(screen.getByText("Chưa được gán vai trò")).toBeInTheDocument();
  });

  it("có vai trò nhưng chưa có chức năng thì có link Xem hồ sơ", () => {
    state.current = coVaiTro(["BAO_VE"]);
    render(<TongQuanView />);
    expect(screen.getByText("Các chức năng cho vai trò của bạn đang được xây dựng")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Xem hồ sơ" })).toHaveAttribute("href", "/ho-so");
  });

  it("đang tải thì hiện skeleton", () => {
    render(<TongQuanView />);
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
  });
});
