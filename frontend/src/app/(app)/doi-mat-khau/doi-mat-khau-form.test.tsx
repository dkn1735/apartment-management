import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { DoiMatKhauForm } from "./doi-mat-khau-form";

const apiMock = vi.hoisted(() => vi.fn());
const putMock = vi.hoisted(() => vi.fn((..._args: unknown[]) => Promise.resolve({})));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, api: apiMock, client: { PUT: putMock } };
});

function setup() {
  const user = userEvent.setup();
  const view = render(
    <QueryClientProvider client={createQueryClient()}>
      <DoiMatKhauForm />
    </QueryClientProvider>,
  );
  return { user, ...view };
}

const matKhauCu = () => screen.getByLabelText(/^Mật khẩu hiện tại/);
const matKhauMoi = () => screen.getByLabelText(/^Mật khẩu mới/);
const xacNhan = () => screen.getByLabelText(/^Nhập lại mật khẩu mới/);
const nutGui = () => screen.getByRole("button", { name: "Đổi mật khẩu" });

async function dienVaGui(user: ReturnType<typeof userEvent.setup>, cu: string, moi: string, lai = moi) {
  await user.type(matKhauCu(), cu);
  await user.type(matKhauMoi(), moi);
  await user.type(xacNhan(), lai);
  await user.click(nutGui());
}

beforeEach(() => {
  apiMock.mockReset();
  putMock.mockClear();
});

describe("Form đổi mật khẩu (API-AUTH-04)", () => {
  it("có nhãn, autocomplete, gợi ý quy tắc và axe sạch", async () => {
    const { container } = setup();
    expect(matKhauCu()).toHaveAttribute("autocomplete", "current-password");
    expect(matKhauMoi()).toHaveAttribute("autocomplete", "new-password");
    expect(xacNhan()).toHaveAttribute("autocomplete", "new-password");
    expect(matKhauMoi()).toHaveAccessibleDescription("Ít nhất 8 ký tự, có cả chữ và số, khác mật khẩu hiện tại.");
    await expectNoA11yViolations(container);
  });

  it("bỏ trống thì báo lỗi dưới từng ô, không gọi API", async () => {
    const { user } = setup();
    await user.click(nutGui());
    expect(await screen.findByText("Vui lòng nhập mật khẩu hiện tại")).toBeInTheDocument();
    expect(screen.getByText("Vui lòng nhập mật khẩu mới")).toBeInTheDocument();
    expect(screen.getByText("Vui lòng nhập lại mật khẩu mới")).toBeInTheDocument();
    expect(apiMock).not.toHaveBeenCalled();
  });

  it.each([
    ["Ab1", "Mật khẩu mới phải có từ 8 đến 128 ký tự"],
    ["chikhongcoso", "Mật khẩu mới phải có cả chữ và số"],
    ["12345678", "Mật khẩu mới phải có cả chữ và số"],
    ["ChungCu@2026", "Mật khẩu mới phải khác mật khẩu hiện tại"],
  ])("mật khẩu mới %s bị chặn ở client", async (moi, loi) => {
    const { user } = setup();
    await dienVaGui(user, "ChungCu@2026", moi);
    expect(await screen.findByText(loi)).toBeInTheDocument();
    expect(matKhauMoi()).toHaveAttribute("aria-invalid", "true");
    expect(apiMock).not.toHaveBeenCalled();
  });

  it("nhập lại không khớp thì báo ở ô nhập lại", async () => {
    const { user } = setup();
    await dienVaGui(user, "ChungCu@2026", "MatKhauMoi2026", "MatKhauKhac2026");
    expect(await screen.findByText("Mật khẩu nhập lại chưa khớp")).toBeInTheDocument();
    expect(apiMock).not.toHaveBeenCalled();
  });

  it("thành công thì chỉ gửi hai trường, xóa trắng form và báo thành công", async () => {
    apiMock.mockResolvedValue({ capNhatLuc: "2026-11-21T03:00:00Z" });
    const { user } = setup();
    await dienVaGui(user, "ChungCu@2026", "MatKhauMoi2026");
    expect(await screen.findByText("Đã đổi mật khẩu. Các thiết bị khác đã được đăng xuất.")).toBeInTheDocument();
    expect(putMock).toHaveBeenCalledWith("/api/v1/auth/change-password", {
      body: { matKhauCu: "ChungCu@2026", matKhauMoi: "MatKhauMoi2026" },
    });
    expect(matKhauCu()).toHaveValue("");
    expect(matKhauMoi()).toHaveValue("");
    expect(xacNhan()).toHaveValue("");
  });

  it("sai mật khẩu hiện tại thì lỗi gắn vào ô đó, ô được xóa và có focus", async () => {
    apiMock.mockRejectedValue(
      new ApiError({
        status: 422,
        errorCode: "AUTH_OLD_PASSWORD_INCORRECT",
        message: "Mật khẩu hiện tại không đúng.",
        errors: [{ field: "matKhauCu", message: "Mật khẩu hiện tại không đúng." }],
        requestId: "r1",
      }),
    );
    const { user } = setup();
    await dienVaGui(user, "SaiMatKhau1", "MatKhauMoi2026");
    expect(await screen.findByText("Mật khẩu hiện tại không đúng.")).toBeInTheDocument();
    await waitFor(() => expect(matKhauCu()).toHaveFocus());
    expect(matKhauCu()).toHaveValue("");
    expect(matKhauCu()).toHaveAttribute("aria-invalid", "true");
    expect(matKhauMoi()).toHaveValue("MatKhauMoi2026");
    expect(screen.queryByText(/Đã đổi mật khẩu/)).not.toBeInTheDocument();
  });

  it("lỗi không theo ô (409) hiện ở FormAlert đầu form", async () => {
    apiMock.mockRejectedValue(
      new ApiError({
        status: 409,
        errorCode: "CONCURRENT_MODIFICATION",
        message: "x",
        errors: [],
        requestId: "r2",
      }),
    );
    const { user } = setup();
    await dienVaGui(user, "ChungCu@2026", "MatKhauMoi2026");
    expect(
      await screen.findByText("Dữ liệu vừa được người khác thay đổi, vui lòng tải lại và thử lại."),
    ).toBeInTheDocument();
  });
});
