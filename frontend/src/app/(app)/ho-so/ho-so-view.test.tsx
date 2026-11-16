import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import type { HoSo } from "@/lib/auth/use-ho-so";
import { expectNoA11yViolations } from "@/test/axe";
import { HoSoView } from "./ho-so-view";

const state = vi.hoisted(() => ({
  current: {} as { data?: unknown; error?: unknown; isError?: boolean; refetch?: () => void; isRefetching?: boolean },
}));

vi.mock("@/lib/auth/use-ho-so", () => ({ useHoSo: () => state.current }));

const HAI_VAI_TRO: HoSo = {
  maNguoiDung: "ND00000006",
  hoTen: "Đặng Ban Quản Trị",
  soDienThoai: "0900000006",
  email: "bqt@chungcu.test",
  trangThaiTaiKhoan: "Hoạt động",
  vaiTro: ["BAN_QUAN_TRI", "CU_DAN"],
  phamVi: { maToaNha: ["A"], maCanHo: ["A-0101"] },
  chiTietVaiTro: {
    banQuanTri: { maThanhVien: "BQT00000001", nhiemKy: "2026-2028", vaiTro: "Trưởng ban" },
    cuDan: { maCuDan: "CD00000001" },
  },
};

beforeEach(() => {
  state.current = {};
});

describe("Trang hồ sơ (AC-14)", () => {
  it("hiện đủ ba phần với tài khoản hai vai trò", async () => {
    state.current = { data: HAI_VAI_TRO };
    const { container } = render(<HoSoView />);

    expect(screen.getByRole("heading", { level: 1, name: "Hồ sơ của tôi" })).toBeInTheDocument();
    const taiKhoan = screen.getByRole("region", { name: "Thông tin tài khoản" });
    expect(within(taiKhoan).getByText("Đặng Ban Quản Trị")).toBeInTheDocument();
    expect(within(taiKhoan).getByText("ND00000006")).toBeInTheDocument();
    expect(within(taiKhoan).getByText("Hoạt động")).toBeInTheDocument();

    const vaiTro = screen.getByRole("region", { name: "Vai trò" });
    expect(within(vaiTro).getByRole("heading", { name: "Ban quản trị" })).toBeInTheDocument();
    expect(within(vaiTro).getByRole("heading", { name: "Cư dân" })).toBeInTheDocument();
    expect(within(vaiTro).getByText("Nhiệm kỳ")).toBeInTheDocument();
    expect(within(vaiTro).getByText("2026-2028")).toBeInTheDocument();
    expect(within(vaiTro).getByText("Vai trò trong ban quản trị")).toBeInTheDocument();
    expect(within(vaiTro).getByText("CD00000001")).toBeInTheDocument();

    const phamVi = screen.getByRole("region", { name: "Phạm vi" });
    expect(within(phamVi).getByText("A")).toBeInTheDocument();
    expect(within(phamVi).getByText("A-0101")).toBeInTheDocument();

    expect(container).not.toHaveTextContent(/CCCD/i);
    await expectNoA11yViolations(container);
  });

  it("danh sách rỗng hiện Không có, không vai trò hiện Chưa được gán vai trò", async () => {
    state.current = {
      data: { ...HAI_VAI_TRO, vaiTro: [], phamVi: { maToaNha: [], maCanHo: [] }, chiTietVaiTro: {} },
    };
    const { container } = render(<HoSoView />);
    expect(within(screen.getByRole("region", { name: "Phạm vi" })).getAllByText("Không có")).toHaveLength(2);
    expect(screen.getByText("Chưa được gán vai trò.")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("đang tải hiện skeleton và báo Đang tải hồ sơ", async () => {
    const { container } = render(<HoSoView />);
    const status = screen.getByRole("status");
    expect(status).toHaveAttribute("aria-busy", "true");
    expect(status).toHaveTextContent("Đang tải hồ sơ");
    await expectNoA11yViolations(container);
  });

  it("lỗi hệ thống hiện trạng thái lỗi có nút Thử lại", async () => {
    const refetch = vi.fn();
    state.current = {
      isError: true,
      error: new ApiError({ status: 500, errorCode: "INTERNAL_ERROR", message: "x", requestId: "abc123" }),
      refetch,
    };
    const { container } = render(<HoSoView />);
    expect(screen.getByRole("heading", { name: "Chưa tải được hồ sơ" })).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(refetch).toHaveBeenCalledOnce();
    await expectNoA11yViolations(container);
  });

  it.each(["UNAUTHENTICATED", "AUTH_ACCOUNT_LOCKED"])("lỗi %s giữ skeleton trong lúc chuyển trang", (errorCode) => {
    state.current = { isError: true, error: new ApiError({ status: 401, errorCode, message: "x" }) };
    render(<HoSoView />);
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải hồ sơ");
    expect(screen.queryByRole("button", { name: "Thử lại" })).not.toBeInTheDocument();
  });
});
