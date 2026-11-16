import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/api";
import { expectNoA11yViolations } from "@/test/axe";
import { ConfirmDialog } from "./confirm-dialog";

function setup(onConfirm: () => Promise<void>) {
  const user = userEvent.setup();
  render(
    <ConfirmDialog
      trigger={<Button>Khóa</Button>}
      title="Khóa tài khoản này?"
      description="Người dùng sẽ không đăng nhập được."
      confirmLabel="Khóa tài khoản"
      tone="destructive"
      onConfirm={onConfirm}
    />,
  );
  return user;
}

describe("ConfirmDialog (AC-4, AC-6)", () => {
  it("mở ra với focus ở nút Hủy, Esc đóng và focus về nút mở", async () => {
    const user = setup(vi.fn());
    await user.click(screen.getByRole("button", { name: "Khóa" }));
    const dialog = await screen.findByRole("alertdialog", { name: "Khóa tài khoản này?" });
    expect(screen.getByRole("button", { name: "Hủy" })).toHaveFocus();
    await expectNoA11yViolations(dialog);
    await user.keyboard("{Escape}");
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Khóa" })).toHaveFocus();
  });

  it("onConfirm ném lỗi thì hộp thoại vẫn mở, hiện lỗi và lỗi nhận focus", async () => {
    const onConfirm = vi.fn(() =>
      Promise.reject(new ApiError({ status: 500, errorCode: "INTERNAL_ERROR", message: "raw", requestId: "r-1" })),
    );
    const user = setup(onConfirm);
    await user.click(screen.getByRole("button", { name: "Khóa" }));
    await user.click(await screen.findByRole("button", { name: "Khóa tài khoản" }));
    const alert = await screen.findByRole("alert");
    await waitFor(() => expect(alert).toHaveFocus());
    expect(alert).toHaveTextContent("Hệ thống đang gặp sự cố");
    expect(alert).toHaveTextContent("Mã tra cứu: r-1");
    expect(screen.getByRole("alertdialog")).toBeInTheDocument();
    await expectNoA11yViolations(screen.getByRole("alertdialog"));
  });

  it("thành công thì đóng và trả focus về nút mở", async () => {
    const onConfirm = vi.fn(() => Promise.resolve());
    const user = setup(onConfirm);
    await user.click(screen.getByRole("button", { name: "Khóa" }));
    await user.click(await screen.findByRole("button", { name: "Khóa tài khoản" }));
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Khóa" })).toHaveFocus();
  });
});
