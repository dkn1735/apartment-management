import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { StrictMode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { DatLaiMatKhauForm, DICH_THANH_CONG } from "./dat-lai-mat-khau-form";

const TOKEN = "A".repeat(43);
const replace = vi.hoisted(() => vi.fn());
const apiMock = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn((path: string, init: unknown) => Promise.resolve({ path, init })));

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace, push: vi.fn() }) }));
vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, api: apiMock, client: { POST: post } };
});

const tokenHong = () =>
  new ApiError({
    status: 400,
    errorCode: "AUTH_RESET_TOKEN_INVALID",
    message: "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu link mới.",
    errors: [],
  });

/** `api` giả: trả theo đường dẫn mà `client.POST` giả chuyển qua. */
function traLoi(check: () => Promise<unknown>, confirm: () => Promise<unknown> = () => Promise.resolve(null)) {
  apiMock.mockImplementation(async (request: Promise<{ path: string }>) => {
    const { path } = await request;
    return path.endsWith("/check") ? check() : confirm();
  });
}

function setup(hash: string, strict = false) {
  window.history.replaceState(null, "", `/dat-lai-mat-khau${hash}`);
  const queryClient = createQueryClient();
  const clear = vi.spyOn(queryClient, "clear");
  const user = userEvent.setup();
  const tree = (
    <QueryClientProvider client={queryClient}>
      <DatLaiMatKhauForm />
    </QueryClientProvider>
  );
  const view = render(strict ? <StrictMode>{tree}</StrictMode> : tree);
  return { user, clear, ...view };
}

const matKhauMoi = () => screen.getByLabelText(/^Mật khẩu mới/);
const nhapLai = () => screen.getByLabelText(/^Nhập lại mật khẩu mới/);

beforeEach(() => {
  apiMock.mockReset();
  post.mockClear();
  replace.mockReset();
});

afterEach(() => {
  window.history.replaceState(null, "", "/");
});

describe("Trang đặt lại mật khẩu (spec 0010, AC-13)", () => {
  it("đọc token từ fragment, xóa fragment, gọi check với đúng token kể cả ở StrictMode", async () => {
    traLoi(() => Promise.resolve({ hetHanLuc: "2026-11-16T01:30:00Z" }));
    setup(`#token=${TOKEN}`, true);
    expect(window.location.hash).toBe("");
    expect(await screen.findByText("Link có hiệu lực tới 08:30 ngày 16/11/2026")).toBeInTheDocument();
    expect(post).toHaveBeenCalledWith("/api/v1/auth/password-reset/check", { body: { token: TOKEN } });
    expect(post.mock.calls.every(([, init]) => (init as { body: { token: string } }).body.token === TOKEN)).toBe(true);
    expect(screen.getByRole("heading", { name: "Đặt lại mật khẩu" })).toHaveFocus();
  });

  it("không có token thì hiện câu lỗi và link yêu cầu link mới, không gọi API", async () => {
    const { container } = setup("");
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu link mới.",
    );
    expect(screen.getByRole("link", { name: "Yêu cầu link mới" })).toHaveAttribute("href", "/quen-mat-khau");
    expect(apiMock).not.toHaveBeenCalled();
    await expectNoA11yViolations(container);
  });

  it("check trả AUTH_RESET_TOKEN_INVALID thì hiện câu lỗi", async () => {
    traLoi(() => Promise.reject(tokenHong()));
    setup(`#token=${TOKEN}`);
    expect(await screen.findByRole("alert")).toHaveTextContent("Link đặt lại mật khẩu không hợp lệ");
    expect(screen.getByRole("heading", { name: "Không dùng được link này" })).toHaveFocus();
  });

  it("nhập lại không khớp thì báo lỗi ở ô thứ hai, không gọi confirm", async () => {
    traLoi(() => Promise.resolve({ hetHanLuc: "2026-11-16T01:30:00Z" }));
    const { user } = setup(`#token=${TOKEN}`);
    await screen.findByText(/Link có hiệu lực tới/);
    await user.type(matKhauMoi(), "MatKhauMoi2026");
    await user.type(nhapLai(), "MatKhauKhac2026");
    await user.click(screen.getByRole("button", { name: "Đặt mật khẩu mới" }));
    const loi = await screen.findByText("Mật khẩu nhập lại chưa khớp");
    expect(nhapLai().getAttribute("aria-describedby")).toContain(loi.id);
    expect(post).not.toHaveBeenCalledWith("/api/v1/auth/password-reset/confirm", expect.anything());
  });

  it("thành công thì xóa cache rồi chuyển về đăng nhập", async () => {
    traLoi(() => Promise.resolve({ hetHanLuc: "2026-11-16T01:30:00Z" }));
    const { user, clear, container } = setup(`#token=${TOKEN}`);
    await screen.findByText(/Link có hiệu lực tới/);
    await expectNoA11yViolations(container);
    expect(screen.getByText("Ít nhất 8 ký tự, có cả chữ và số.")).toBeInTheDocument();
    await user.type(matKhauMoi(), "MatKhauMoi2026");
    await user.type(nhapLai(), "MatKhauMoi2026");
    await user.click(screen.getByRole("button", { name: "Đặt mật khẩu mới" }));
    await waitFor(() => expect(replace).toHaveBeenCalledWith(DICH_THANH_CONG));
    expect(post).toHaveBeenCalledWith("/api/v1/auth/password-reset/confirm", {
      body: { token: TOKEN, matKhauMoi: "MatKhauMoi2026" },
    });
    expect(clear).toHaveBeenCalled();
    expect(DICH_THANH_CONG).toBe("/dang-nhap?ly-do=dat-lai-mat-khau");
  });

  it("confirm trả AUTH_RESET_TOKEN_INVALID thì chuyển sang trạng thái link hỏng", async () => {
    traLoi(
      () => Promise.resolve({ hetHanLuc: "2026-11-16T01:30:00Z" }),
      () => Promise.reject(tokenHong()),
    );
    const { user } = setup(`#token=${TOKEN}`);
    await screen.findByText(/Link có hiệu lực tới/);
    await user.type(matKhauMoi(), "MatKhauMoi2026");
    await user.type(nhapLai(), "MatKhauMoi2026");
    await user.click(screen.getByRole("button", { name: "Đặt mật khẩu mới" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Link đặt lại mật khẩu không hợp lệ");
    expect(replace).not.toHaveBeenCalled();
  });
});
