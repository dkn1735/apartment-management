import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { ThemToaNhaDialog } from "./them-toa-nha-dialog";

const post = vi.hoisted(() => vi.fn());
const notify = vi.hoisted(() => ({ success: vi.fn() }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { POST: post }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/components/feedback/notify", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/components/feedback/notify")>()),
  notifySuccess: notify.success,
}));

function hienThi() {
  render(
    <QueryClientProvider client={createQueryClient()}>
      <ThemToaNhaDialog />
    </QueryClientProvider>,
  );
}

async function dien(user: ReturnType<typeof userEvent.setup>, soTang: string) {
  await user.click(screen.getByRole("button", { name: "Thêm tòa nhà" }));
  await user.type(screen.getByRole("textbox", { name: /Mã tòa nhà/ }), "b2");
  await user.type(screen.getByRole("textbox", { name: /Tên tòa nhà/ }), "Tòa A");
  await user.type(screen.getByRole("textbox", { name: /Số tầng/ }), soTang);
  await user.type(screen.getByRole("textbox", { name: /Địa chỉ khu vực/ }), "Hà Nội");
}

beforeEach(() => {
  post.mockReset();
  notify.success.mockReset();
});

describe("Hộp thoại Thêm tòa nhà (spec 0011, AC-15)", () => {
  it("bốn ô có nhãn, mã tự viết hoa, axe sạch", async () => {
    const user = userEvent.setup();
    hienThi();
    await dien(user, "5");
    expect(screen.getByRole("textbox", { name: /Mã tòa nhà/ })).toHaveValue("B2");
    expect(screen.getByRole("textbox", { name: /Số tầng/ })).toHaveAttribute("inputmode", "numeric");
    await expectNoA11yViolations(document.body);
  });

  it("gửi số nguyên khi là chữ số, thành công thì đóng, báo toast và trả focus về nút", async () => {
    const user = userEvent.setup();
    post.mockResolvedValue({ maToaNha: "B2", tenToaNha: "Tòa B2", soTang: 5, diaChiKhuVuc: "Hà Nội", soCanHo: 0 });
    hienThi();
    await dien(user, "5");
    await user.click(screen.getByRole("dialog").querySelector("button[type=submit]")!);
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã thêm tòa nhà Tòa B2"));
    expect(post).toHaveBeenCalledWith("/api/v1/buildings", {
      body: { maToaNha: "B2", tenToaNha: "Tòa A", soTang: 5, diaChiKhuVuc: "Hà Nội" },
    });
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Thêm tòa nhà" })).toHaveFocus();
  });

  it("số tầng không phải chữ số thì gửi nguyên chuỗi; lỗi 409 gắn vào ô Tên và ô đó nhận focus", async () => {
    const user = userEvent.setup();
    post.mockRejectedValue(
      new ApiError({
        status: 409,
        errorCode: "CANHO_TOA_NHA_DA_TON_TAI",
        message: "Tòa nhà đã tồn tại",
        errors: [{ field: "tenToaNha", message: "Tên tòa nhà đã được dùng" }],
      }),
    );
    hienThi();
    await dien(user, "ba");
    const dialog = screen.getByRole("dialog");
    await user.click(dialog.querySelector("button[type=submit]")!);
    const ten = await screen.findByRole("textbox", { name: /Tên tòa nhà/ });
    await waitFor(() => expect(ten).toHaveAttribute("aria-invalid", "true"));
    expect(ten).toHaveAccessibleDescription(/Tên tòa nhà đã được dùng/);
    expect(ten).toHaveFocus();
    expect(post.mock.calls[0][1].body.soTang).toBe("ba");
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });
});
