import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { LoginForm } from "./login-form";

const nav = vi.hoisted(() => ({ params: new URLSearchParams(), replace: vi.fn() }));
const apiMock = vi.hoisted(() => vi.fn());

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: nav.replace, push: vi.fn() }),
  useSearchParams: () => nav.params,
}));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, api: apiMock, client: { POST: vi.fn(() => Promise.resolve({})) } };
});

function setup(query = "") {
  nav.params = new URLSearchParams(query);
  const user = userEvent.setup();
  const view = render(
    <QueryClientProvider client={createQueryClient()}>
      <LoginForm />
    </QueryClientProvider>,
  );
  return { user, ...view };
}

const taiKhoan = () => screen.getByLabelText(/Số điện thoại hoặc email/);
const matKhau = () => screen.getByLabelText(/^Mật khẩu/);

async function dienVaGui(user: ReturnType<typeof userEvent.setup>) {
  await user.type(taiKhoan(), "0900000007");
  await user.type(matKhau(), "SaiMatKhau1");
  await user.click(screen.getByRole("button", { name: "Đăng nhập" }));
}

beforeEach(() => {
  nav.replace.mockReset();
  apiMock.mockReset();
});

describe("Form đăng nhập (AC-12)", () => {
  it("có nhãn, autocomplete và axe sạch", async () => {
    const { container } = setup();
    expect(taiKhoan()).toHaveAttribute("autocomplete", "username");
    expect(matKhau()).toHaveAttribute("autocomplete", "current-password");
    expect(matKhau()).toHaveAttribute("type", "password");
    await expectNoA11yViolations(container);
  });

  it("nút hiện/ẩn mật khẩu có aria-pressed và nhãn, dùng được bằng bàn phím", async () => {
    const { user } = setup();
    const nut = screen.getByRole("button", { name: "Hiện mật khẩu" });
    expect(nut).toHaveAttribute("aria-pressed", "false");
    nut.focus();
    await user.keyboard("{Enter}");
    expect(nut).toHaveAttribute("aria-pressed", "true");
    expect(matKhau()).toHaveAttribute("type", "text");
    await user.keyboard(" ");
    expect(matKhau()).toHaveAttribute("type", "password");
  });

  it("bỏ trống thì báo lỗi dưới ô", async () => {
    const { user } = setup();
    await user.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByText("Vui lòng nhập số điện thoại hoặc email")).toBeInTheDocument();
    expect(screen.getByText("Vui lòng nhập mật khẩu")).toBeInTheDocument();
    expect(apiMock).not.toHaveBeenCalled();
  });

  it.each([
    ["AUTH_INVALID_CREDENTIALS", 401, "Số điện thoại, email hoặc mật khẩu không đúng."],
    ["AUTH_ACCOUNT_LOCKED", 403, "Tài khoản đang bị khóa hoặc đã ngừng hoạt động. Vui lòng liên hệ ban quản lý."],
  ])("lỗi %s hiện ở FormAlert có focus, xóa mật khẩu, giữ tài khoản", async (errorCode, status, text) => {
    apiMock.mockRejectedValue(new ApiError({ status, errorCode, message: "x" }));
    const { user, container } = setup("next=/ho-so");
    await dienVaGui(user);

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(text);
    await waitFor(() => expect(alert).toHaveFocus());
    expect(matKhau()).toHaveValue("");
    expect(taiKhoan()).toHaveValue("0900000007");
    expect(nav.replace).not.toHaveBeenCalled();
    await expectNoA11yViolations(container);
  });

  it.each([
    ["next=%2Fho-so%3Ftab%3D1", "/ho-so?tab=1"],
    ["next=%2F%2Fevil.com", "/"],
    ["next=https%3A%2F%2Fevil.com", "/"],
    ["", "/"],
  ])("thành công với %s thì chuyển tới %s", async (query, expected) => {
    apiMock.mockResolvedValue({ maNguoiDung: "ND00000007" });
    const { user } = setup(query);
    await dienVaGui(user);
    await waitFor(() => expect(nav.replace).toHaveBeenCalledWith(expected));
  });
});

describe("Thông báo lý do (AC-15)", () => {
  it("ly-do=het-phien hiện thông báo hết phiên", async () => {
    const { container } = setup("ly-do=het-phien");
    expect(screen.getByRole("status")).toHaveTextContent("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.");
    await expectNoA11yViolations(container);
  });

  it("ly-do=khoa hiện thông báo tài khoản bị khóa", () => {
    setup("ly-do=khoa");
    expect(screen.getByRole("status")).toHaveTextContent("Tài khoản đang bị khóa");
  });

  it("ly-do=dang-xuat hiện Bạn đã đăng xuất. (spec 0005, AC-14)", async () => {
    const { container } = setup("ly-do=dang-xuat");
    expect(screen.getByRole("status")).toHaveTextContent("Bạn đã đăng xuất.");
    await expectNoA11yViolations(container);
  });

  it("ly-do lạ thì không hiện gì", () => {
    setup("ly-do=toString");
    expect(screen.getByRole("status", { hidden: true })).toBeEmptyDOMElement();
  });

  it("ly-do không có trong danh sách thì không hiện gì", () => {
    setup("ly-do=abc");
    expect(screen.getByRole("status", { hidden: true })).toBeEmptyDOMElement();
  });
});
