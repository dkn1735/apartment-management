import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { CAU_PHAN_HOI_CHUNG, QuenMatKhauForm } from "./quen-mat-khau-form";

const apiMock = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn(() => Promise.resolve({})));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, api: apiMock, client: { POST: post } };
});

function setup() {
  const user = userEvent.setup();
  const view = render(
    <QueryClientProvider client={createQueryClient()}>
      <QuenMatKhauForm />
    </QueryClientProvider>,
  );
  return { user, ...view };
}

const email = () => screen.getByLabelText(/^Email/);

beforeEach(() => {
  apiMock.mockReset();
  post.mockClear();
});

describe("Trang quên mật khẩu (spec 0010, AC-13)", () => {
  it("ô email có nhãn, có link về đăng nhập, axe sạch", async () => {
    const { container } = setup();
    expect(email()).toHaveAttribute("autocomplete", "email");
    expect(screen.getByRole("link", { name: "Quay lại đăng nhập" })).toHaveAttribute("href", "/dang-nhap");
    await expectNoA11yViolations(container);
  });

  it("email sai định dạng thì báo lỗi gắn với ô, không gọi API", async () => {
    const { user } = setup();
    await user.type(email(), "khong-phai-email");
    await user.click(screen.getByRole("button", { name: "Gửi link đặt lại" }));
    const loi = await screen.findByText("Email không hợp lệ");
    expect(email()).toHaveAttribute("aria-invalid", "true");
    expect(email().getAttribute("aria-describedby")).toContain(loi.id);
    expect(apiMock).not.toHaveBeenCalled();
  });

  it("gửi xong thì hiện câu chung trong role=status, có thời hạn 30 phút và focus tới đó", async () => {
    apiMock.mockResolvedValue(null);
    const { user, container } = setup();
    await user.type(email(), "  CuDan@ChungCu.Test ");
    await user.click(screen.getByRole("button", { name: "Gửi link đặt lại" }));
    const status = await screen.findByRole("status");
    expect(status).toHaveTextContent(CAU_PHAN_HOI_CHUNG);
    expect(status).toHaveTextContent("Link có hiệu lực 30 phút.");
    expect(status.firstElementChild).toHaveFocus();
    expect(post).toHaveBeenCalledWith("/api/v1/auth/password-reset/request", {
      body: { email: "CuDan@ChungCu.Test" },
    });
    expect(screen.queryByRole("form")).not.toBeInTheDocument();
    await expectNoA11yViolations(container);

    await user.click(screen.getByRole("button", { name: "Gửi lại" }));
    expect(email()).toHaveValue("CuDan@ChungCu.Test");
  });

  it("lỗi ô email từ backend gắn vào ô", async () => {
    apiMock.mockRejectedValue(
      new ApiError({
        status: 400,
        errorCode: "VALIDATION_FAILED",
        message: "Dữ liệu không hợp lệ",
        errors: [{ field: "email", message: "Email không hợp lệ" }],
      }),
    );
    const { user } = setup();
    await user.type(email(), "a@b.vn");
    await user.click(screen.getByRole("button", { name: "Gửi link đặt lại" }));
    expect(await screen.findByText("Email không hợp lệ")).toBeInTheDocument();
    expect(email()).toHaveFocus();
  });
});
