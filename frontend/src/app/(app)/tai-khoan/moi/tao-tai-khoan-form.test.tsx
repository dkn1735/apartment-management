import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { mockPush } from "@/test/next-navigation";
import { TaoTaiKhoanForm } from "./tao-tai-khoan-form";

const apiMock = vi.hoisted(() => vi.fn());
const postMock = vi.hoisted(() => vi.fn((..._args: unknown[]) => Promise.resolve({})));
const hoSo = vi.hoisted(() => ({ vaiTro: ["QUAN_TRI_VIEN"] as string[] }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, api: apiMock, client: { POST: postMock } };
});

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({ data: { vaiTro: hoSo.vaiTro } }),
}));

function setup() {
  const user = userEvent.setup();
  const view = render(
    <QueryClientProvider client={createQueryClient()}>
      <TaoTaiKhoanForm />
    </QueryClientProvider>,
  );
  return { user, ...view };
}

async function dienChung(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/^Họ tên/), "Nguyễn Văn An");
  await user.type(screen.getByLabelText(/^Số điện thoại/), "0912345678");
  await user.type(screen.getByLabelText(/^Email/), "an@chungcu.test");
}

const nutTao = () => screen.getByRole("button", { name: "Tạo tài khoản" });

beforeEach(() => {
  apiMock.mockReset();
  postMock.mockClear();
  mockPush.mockClear();
  hoSo.vaiTro = ["QUAN_TRI_VIEN"];
});

describe("Form tạo tài khoản (spec 0008, AC-17)", () => {
  it("Admin thấy bảy vai trò, tick vai trò thì hiện ô của vai trò đó, axe sạch", async () => {
    const { user, container } = setup();
    expect(screen.getAllByRole("checkbox")).toHaveLength(7);
    expect(screen.queryByLabelText(/^Phạm vi phụ trách/)).not.toBeInTheDocument();
    await user.click(screen.getByLabelText("Kế toán"));
    expect(screen.getByLabelText(/^Phạm vi phụ trách/)).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("bỏ trống thì báo lỗi dưới từng ô và nhóm vai trò, không gọi API", async () => {
    const { user, container } = setup();
    await user.click(nutTao());
    expect(await screen.findByText("Vui lòng nhập họ tên")).toBeInTheDocument();
    expect(screen.getByText("Vui lòng chọn ít nhất một vai trò")).toBeInTheDocument();
    await user.click(screen.getByLabelText("Ban quản lý"));
    await user.click(nutTao());
    expect(await screen.findByText("Vui lòng nhập chức vụ")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Chức vụ/)).toHaveAttribute("aria-invalid", "true");
    expect(apiMock).not.toHaveBeenCalled();
    await expectNoA11yViolations(container);
  });

  it("tạo xong thì gửi đúng trường của vai trò đã chọn, hiện mật khẩu tạm một lần", async () => {
    apiMock.mockResolvedValue({
      taiKhoan: { maNguoiDung: "ND00000020", hoTen: "Nguyễn Văn An" },
      matKhauTam: "Ab3dEf7hJk9m",
    });
    const { user } = setup();
    await dienChung(user);
    await user.click(screen.getByLabelText("Cư dân"));
    await user.type(screen.getByLabelText(/^Số CCCD/), "001203004567");
    await user.click(nutTao());

    const dialog = await screen.findByRole("dialog", { name: "Mật khẩu tạm của Nguyễn Văn An" });
    expect(within(dialog).getByText("Ab3dEf7hJk9m")).toBeInTheDocument();
    expect(within(dialog).getByText(/Mật khẩu chỉ hiện một lần/)).toBeInTheDocument();
    expect(postMock).toHaveBeenCalledWith("/api/v1/accounts", {
      body: {
        hoTen: "Nguyễn Văn An",
        soDienThoai: "0912345678",
        email: "an@chungcu.test",
        vaiTro: [{ vaiTro: "CU_DAN", soCccd: "001203004567" }],
      },
    });

    await user.click(within(dialog).getByRole("button", { name: "Tôi đã lưu mật khẩu" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(mockPush).toHaveBeenCalled();
    expect(screen.queryByText("Ab3dEf7hJk9m")).not.toBeInTheDocument();
  });

  it("lỗi trùng của backend gắn vào đúng ô, kể cả trường vai trò vaiTro[i]", async () => {
    apiMock.mockRejectedValue(
      new ApiError({
        status: 409,
        errorCode: "AUTH_IDENTITY_TAKEN",
        message: "x",
        errors: [
          { field: "soDienThoai", message: "Số điện thoại đã được dùng cho tài khoản khác" },
          { field: "soCccd", message: "Số CCCD đã được dùng cho cư dân khác" },
        ],
      }),
    );
    const { user } = setup();
    await dienChung(user);
    await user.click(screen.getByLabelText("Cư dân"));
    await user.type(screen.getByLabelText(/^Số CCCD/), "001203004567");
    await user.click(nutTao());
    expect(await screen.findByText("Số điện thoại đã được dùng cho tài khoản khác")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Số CCCD/)).toHaveAccessibleDescription(/Số CCCD đã được dùng cho cư dân khác/);
    expect(screen.getByLabelText(/^Số điện thoại/)).toHaveFocus();
  });

  it("tick Quản trị viên thì hỏi xác nhận trước, hủy thì không chọn", async () => {
    const { user } = setup();
    const qtv = screen.getByLabelText("Quản trị viên");
    await user.click(qtv);
    const hoi = await screen.findByRole("alertdialog", { name: "Cấp quyền quản trị viên?" });
    await user.click(within(hoi).getByRole("button", { name: "Hủy" }));
    expect(qtv).not.toBeChecked();
    await user.click(qtv);
    await user.click(await screen.findByRole("button", { name: "Cấp quyền quản trị" }));
    expect(qtv).toBeChecked();
  });

  it("BQL chỉ thấy Cư dân, đã chọn sẵn", () => {
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    setup();
    const hop = screen.getAllByRole("checkbox");
    expect(hop).toHaveLength(1);
    expect(screen.getByRole("checkbox", { name: "Cư dân" })).toBeChecked();
    expect(screen.getByLabelText(/^Số CCCD/)).toBeInTheDocument();
  });
});
