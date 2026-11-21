import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import type { TaiKhoanChiTiet } from "@/lib/tai-khoan";
import { expectNoA11yViolations } from "@/test/axe";
import { ChiTietTaiKhoanView } from "./chi-tiet-tai-khoan-view";

const get = vi.hoisted(() => vi.fn());
const put = vi.hoisted(() => vi.fn());
const del = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn());
const hoSo = vi.hoisted(() => ({ vaiTro: ["QUAN_TRI_VIEN"] as string[], maNguoiDung: "ND00000001" }));
const notify = vi.hoisted(() => ({ info: vi.fn(), success: vi.fn() }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return {
    ...actual,
    client: { GET: get, PUT: put, DELETE: del, POST: post },
    api: (p: Promise<unknown>) => p,
  };
});

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({ data: hoSo }),
}));

vi.mock("next/navigation", async (importOriginal) => ({
  ...(await importOriginal<typeof import("next/navigation")>()),
  useParams: () => ({ ma: "ND00000020" }),
}));

vi.mock("@/components/feedback/notify", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/components/feedback/notify")>()),
  notifyInfo: notify.info,
  notifySuccess: notify.success,
}));

const TK: TaiKhoanChiTiet = {
  maNguoiDung: "ND00000020",
  hoTen: "Nguyễn Văn An",
  soDienThoai: "0912345678",
  email: "an@chungcu.test",
  trangThaiTaiKhoan: "Hoạt động",
  phaiDoiMatKhau: false,
  taoLuc: "2026-11-16T01:00:00Z",
  capNhatLuc: "2026-11-16T01:00:00Z",
  phienBan: 3,
  vaiTro: [
    { vaiTro: "BAN_QUAN_LY", maVaiTro: "BQL00000002", chucVu: "Trưởng ca", boPhan: "Vận hành" },
    { vaiTro: "CU_DAN", maVaiTro: "CD00000009", soCccd: "001203004567" },
  ],
};

function hienThi(tk: TaiKhoanChiTiet = TK) {
  get.mockResolvedValue(tk);
  const user = userEvent.setup();
  const view = render(
    <QueryClientProvider client={createQueryClient()}>
      <ChiTietTaiKhoanView />
    </QueryClientProvider>,
  );
  return { user, ...view };
}

beforeEach(() => {
  for (const m of [get, put, del, post, notify.info, notify.success]) m.mockReset();
  hoSo.vaiTro = ["QUAN_TRI_VIEN"];
  hoSo.maNguoiDung = "ND00000001";
});

describe("Chi tiết tài khoản (spec 0008, AC-6 đến AC-9, AC-17)", () => {
  it("hiện thông tin chung, vai trò kèm CCCD; axe sạch", async () => {
    const { container } = hienThi();
    expect(await screen.findByRole("heading", { level: 1, name: "Nguyễn Văn An" })).toBeInTheDocument();
    expect(screen.getByText("001203004567")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Gỡ vai trò Cư dân" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Thêm vai trò" })).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("sửa thông tin chung gửi phienBan", async () => {
    put.mockResolvedValue({ ...TK, hoTen: "Tên Mới", phienBan: 4 });
    const { user } = hienThi();
    await user.click(await screen.findByRole("button", { name: "Sửa" }));
    const hoTen = screen.getByLabelText(/^Họ tên/);
    await user.clear(hoTen);
    await user.type(hoTen, "Tên Mới");
    await user.click(screen.getByRole("button", { name: "Lưu" }));
    await waitFor(() =>
      expect(put).toHaveBeenCalledWith("/api/v1/accounts/{maNguoiDung}", {
        params: { path: { maNguoiDung: "ND00000020" } },
        body: { hoTen: "Tên Mới", soDienThoai: "0912345678", email: "an@chungcu.test", phienBan: 3 },
      }),
    );
    expect(await screen.findByRole("heading", { level: 1, name: "Tên Mới" })).toBeInTheDocument();
  });

  it("phiên bản lệch thì báo đã tải lại và gọi lại chi tiết", async () => {
    put.mockRejectedValue(new ApiError({ status: 409, errorCode: "CONCURRENT_MODIFICATION", message: "x" }));
    const { user } = hienThi();
    await user.click(await screen.findByRole("button", { name: "Sửa" }));
    await user.click(screen.getByRole("button", { name: "Lưu" }));
    await waitFor(() =>
      expect(notify.info).toHaveBeenCalledWith("Tài khoản vừa được thay đổi ở nơi khác, đã tải lại dữ liệu mới nhất."),
    );
    expect(get).toHaveBeenCalledTimes(2);
  });

  it("thêm vai trò Quản trị viên thì có cảnh báo và nút Cấp quyền quản trị", async () => {
    put.mockResolvedValue({ ...TK, phienBan: 4 });
    const { user } = hienThi();
    await user.click(await screen.findByRole("button", { name: "Thêm vai trò" }));
    const dialog = await screen.findByRole("dialog", { name: "Thêm vai trò" });
    await user.click(within(dialog).getByRole("combobox", { name: /Vai trò/ }));
    await user.click(await screen.findByRole("option", { name: "Quản trị viên" }));
    expect(within(dialog).getByText(/kể cả tài khoản quản trị khác/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole("button", { name: "Cấp quyền quản trị" }));
    await waitFor(() =>
      expect(put).toHaveBeenCalledWith("/api/v1/accounts/{maNguoiDung}/roles/{vaiTro}", {
        params: { path: { maNguoiDung: "ND00000020", vaiTro: "QUAN_TRI_VIEN" } },
        body: { phienBan: 3 },
      }),
    );
  });

  it("gỡ vai trò qua hộp thoại xác nhận, gửi phienBan trên query", async () => {
    del.mockResolvedValue({ ...TK, vaiTro: [TK.vaiTro[0]], phienBan: 4 });
    const { user } = hienThi();
    await user.click(await screen.findByRole("button", { name: "Gỡ vai trò Cư dân" }));
    await user.click(await screen.findByRole("button", { name: "Gỡ vai trò" }));
    await waitFor(() =>
      expect(del).toHaveBeenCalledWith("/api/v1/accounts/{maNguoiDung}/roles/{vaiTro}", {
        params: { path: { maNguoiDung: "ND00000020", vaiTro: "CU_DAN" }, query: { phienBan: 3 } },
      }),
    );
    expect(notify.success).toHaveBeenCalledWith("Đã gỡ vai trò Cư dân");
  });

  it("vai trò duy nhất không có nút Gỡ; BQL chỉ sửa được dòng Cư dân", async () => {
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    hienThi({ ...TK, vaiTro: [TK.vaiTro[1]] });
    await screen.findByRole("heading", { level: 1, name: "Nguyễn Văn An" });
    expect(screen.queryByRole("button", { name: /^Gỡ vai trò/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Thêm vai trò" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sửa vai trò Cư dân" })).toBeInTheDocument();
  });

  it("404 hiện Không tìm thấy tài khoản", async () => {
    get.mockRejectedValue(new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "x" }));
    render(
      <QueryClientProvider client={createQueryClient()}>
        <ChiTietTaiKhoanView />
      </QueryClientProvider>,
    );
    expect(await screen.findByRole("heading", { level: 1, name: "Không tìm thấy tài khoản" })).toBeInTheDocument();
  });
});
