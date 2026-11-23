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

  it.each([
    ["next=%2Fho-so", "/doi-mat-khau?next=%2Fho-so"],
    ["next=%2F%2Fevil.com", "/doi-mat-khau?next=%2F"],
  ])("mật khẩu tạm với %s thì chuyển tới %s (spec 0008, AC-14)", async (query, expected) => {
    apiMock.mockResolvedValue({ maNguoiDung: "ND00000020", phaiDoiMatKhau: true });
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

  it("ly-do=dat-lai-mat-khau hiện câu thành công (spec 0010, AC-13)", async () => {
    const { container } = setup("ly-do=dat-lai-mat-khau");
    expect(screen.getByRole("status")).toHaveTextContent("Đã đặt lại mật khẩu. Vui lòng đăng nhập bằng mật khẩu mới.");
    await expectNoA11yViolations(container);
  });

  it("có link Quên mật khẩu? tới /quen-mat-khau (spec 0010, AC-13)", () => {
    setup();
    expect(screen.getByRole("link", { name: "Quên mật khẩu?" })).toHaveAttribute("href", "/quen-mat-khau");
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

describe("Khóa tạm đăng nhập (spec 0009, AC-13)", () => {
  const GOI_Y = "Nhập sai 5 lần liên tiếp sẽ bị tạm khóa đăng nhập 15 phút.";

  it("429 hiện số phút trong role=alert; câu gợi ý hiện từ lần 401 đầu và còn sau 429", async () => {
    const { user, container } = setup();
    expect(screen.queryByText(GOI_Y)).not.toBeInTheDocument();

    apiMock.mockRejectedValueOnce(new ApiError({ status: 401, errorCode: "AUTH_INVALID_CREDENTIALS", message: "x" }));
    await dienVaGui(user);
    const goiY = await screen.findByText(GOI_Y);
    const form = screen.getByRole("form", { name: "Đăng nhập" });
    expect(form).toHaveAttribute("aria-describedby", goiY.id);

    apiMock.mockRejectedValueOnce(
      new ApiError({ status: 429, errorCode: "AUTH_TOO_MANY_ATTEMPTS", message: "x", retryAfter: 720 }),
    );
    await user.type(matKhau(), "SaiMatKhau2");
    await user.click(screen.getByRole("button", { name: "Đăng nhập" }));
    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 12 phút hoặc liên hệ ban quản lý.",
      ),
    );
    expect(screen.getByText(GOI_Y)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Đăng nhập" })).toBeEnabled();
    await expectNoA11yViolations(container);
  });

  it("429 thiếu Retry-After thì hiện câu mặc định, không hiện câu gợi ý", async () => {
    apiMock.mockRejectedValueOnce(new ApiError({ status: 429, errorCode: "AUTH_TOO_MANY_ATTEMPTS", message: "x" }));
    const { user } = setup();
    await dienVaGui(user);
    expect(await screen.findByRole("alert")).toHaveTextContent("Vui lòng thử lại sau ít phút");
    expect(screen.queryByText(GOI_Y)).not.toBeInTheDocument();
  });
});
