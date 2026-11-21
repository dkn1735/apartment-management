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

describe("Thẻ Trạng thái và bảo mật (spec 0008, AC-10, AC-12, AC-17)", () => {
  it("tạm khóa cần lý do rồi gửi trạng thái và lý do", async () => {
    put.mockResolvedValue({ ...TK, trangThaiTaiKhoan: "Tạm khóa", phienBan: 4 });
    const { user, container } = hienThi();
    await user.click(await screen.findByRole("button", { name: "Tạm khóa" }));
    const dialog = await screen.findByRole("dialog", { name: "Tạm khóa tài khoản này?" });
    await user.click(within(dialog).getByRole("button", { name: "Tạm khóa" }));
    expect(await within(dialog).findByText("Vui lòng nhập lý do")).toBeInTheDocument();
    expect(put).not.toHaveBeenCalled();
    await expectNoA11yViolations(container);
    await user.type(within(dialog).getByLabelText(/^Lý do/), "Nợ phí");
    await user.click(within(dialog).getByRole("button", { name: "Tạm khóa" }));
    await waitFor(() =>
      expect(put).toHaveBeenCalledWith("/api/v1/accounts/{maNguoiDung}/status", {
        params: { path: { maNguoiDung: "ND00000020" } },
        body: { trangThai: "Tạm khóa", lyDo: "Nợ phí" },
      }),
    );
    expect(notify.success).toHaveBeenCalledWith("Tài khoản đã chuyển sang Tạm khóa");
  });

  it("đặt lại mật khẩu thì hiện mật khẩu tạm một lần", async () => {
    post.mockResolvedValue({ matKhauTam: "Xy3dEf7hJk9m" });
    const { user } = hienThi();
    await user.click(await screen.findByRole("button", { name: "Đặt lại mật khẩu" }));
    const xacNhan = await screen.findByRole("alertdialog", { name: "Đặt lại mật khẩu?" });
    await user.click(within(xacNhan).getByRole("button", { name: "Đặt lại mật khẩu" }));
    const dialog = await screen.findByRole("dialog", { name: "Mật khẩu tạm của Nguyễn Văn An" });
    expect(within(dialog).getByText("Xy3dEf7hJk9m")).toBeInTheDocument();
    await user.click(within(dialog).getByRole("button", { name: "Tôi đã lưu mật khẩu" }));
    await waitFor(() => expect(screen.queryByText("Xy3dEf7hJk9m")).not.toBeInTheDocument());
  });

  it("tài khoản của chính mình không có nút khóa hay đặt lại", async () => {
    hoSo.maNguoiDung = "ND00000020";
    hienThi();
    await screen.findByRole("heading", { level: 1, name: "Nguyễn Văn An" });
    expect(screen.queryByRole("button", { name: "Tạm khóa" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Đặt lại mật khẩu" })).not.toBeInTheDocument();
    expect(screen.getByText(/Đây là tài khoản của bạn/)).toBeInTheDocument();
  });

  it("BQL với tài khoản Ngừng hoạt động: không đổi trạng thái được, vẫn đặt lại mật khẩu", async () => {
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    hienThi({ ...TK, trangThaiTaiKhoan: "Ngừng hoạt động", vaiTro: [TK.vaiTro[1]] });
    await screen.findByRole("heading", { level: 1, name: "Nguyễn Văn An" });
    expect(screen.queryByRole("button", { name: "Mở lại" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Tạm khóa" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Đặt lại mật khẩu" })).toBeInTheDocument();
  });
});
