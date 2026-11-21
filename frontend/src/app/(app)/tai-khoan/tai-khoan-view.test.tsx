import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createQueryClient } from "@/lib/api/query-client";
import type { TaiKhoanTomTat } from "@/lib/tai-khoan";
import { expectNoA11yViolations } from "@/test/axe";
import { mockPathname, mockPush, mockReplace, mockSearchParams } from "@/test/next-navigation";
import { TaiKhoanView } from "./tai-khoan-view";

const get = vi.hoisted(() => vi.fn());
const hoSo = vi.hoisted(() => ({ vaiTro: ["QUAN_TRI_VIEN"] as string[] }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({ data: { vaiTro: hoSo.vaiTro } }),
}));

const DONG: TaiKhoanTomTat[] = [
  {
    maNguoiDung: "ND00000020",
    hoTen: "Nguyễn Văn An",
    soDienThoai: "0912345678",
    email: "an@chungcu.test",
    trangThaiTaiKhoan: "Tạm khóa",
    phaiDoiMatKhau: true,
    vaiTro: ["BAN_QUAN_LY", "CU_DAN"],
    taoLuc: "2026-11-16T01:00:00Z",
  },
];

function hienThi(url = "") {
  mockSearchParams.mockReturnValue(new URLSearchParams(url));
  get.mockResolvedValue({ items: DONG, page: 1, size: 20, totalItems: 1, totalPages: 1 });
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <TaiKhoanView />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  get.mockReset();
  mockPush.mockClear();
  mockReplace.mockClear();
  mockPathname.mockReturnValue("/tai-khoan");
  hoSo.vaiTro = ["QUAN_TRI_VIEN"];
});

describe("Danh sách tài khoản (spec 0008, AC-5, AC-17)", () => {
  it("hiện dòng có link chi tiết, nhãn vai trò, trạng thái và chờ đổi mật khẩu; axe sạch", async () => {
    const { container } = hienThi();
    expect(await screen.findByRole("link", { name: "Nguyễn Văn An" })).toHaveAttribute("href", "/tai-khoan/ND00000020");
    expect(screen.getByText("Ban quản lý, Cư dân")).toBeInTheDocument();
    expect(screen.getByText("Tạm khóa")).toBeInTheDocument();
    expect(screen.getByText("Chờ đổi mật khẩu")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Tạo tài khoản" })).toHaveAttribute("href", "/tai-khoan/moi");
    await expectNoA11yViolations(container);
  });

  it("gửi bộ lọc trên URL cho API", async () => {
    hienThi("q=nguyen&trangThai=T%E1%BA%A1m+kh%C3%B3a&page=2");
    await screen.findByRole("link", { name: "Nguyễn Văn An" });
    expect(get).toHaveBeenCalledWith("/api/v1/accounts", {
      params: { query: { page: 2, size: 20, q: "nguyen", trangThai: "Tạm khóa" } },
    });
  });

  it("ô tìm nhấn Enter thì ghi q lên URL và về trang 1", async () => {
    const user = userEvent.setup();
    hienThi("page=3");
    await user.type(screen.getByRole("searchbox", { name: "Tìm tài khoản" }), "an{Enter}");
    await waitFor(() => expect(mockPush).toHaveBeenCalled());
    const url = String(mockPush.mock.calls.at(-1)?.[0]);
    expect(url).toContain("q=an");
    expect(url).not.toContain("page=");
  });

  it("BQL không có ô lọc vai trò", async () => {
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    hienThi();
    await screen.findByRole("link", { name: "Nguyễn Văn An" });
    expect(screen.queryByLabelText("Vai trò")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Trạng thái")).toBeInTheDocument();
  });
});
