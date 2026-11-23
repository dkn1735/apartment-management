import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { expectNoA11yViolations } from "@/test/axe";
import { MatKhauTamDialog } from "./mat-khau-tam-dialog";

const MAT_KHAU = "Ab3dEf7hJk9m";

function Khung({ onClose }: { onClose: () => void }) {
  const [matKhau, setMatKhau] = useState<string | null>(MAT_KHAU);
  return (
    <>
      <p>Bên ngoài hộp thoại</p>
      <MatKhauTamDialog
        matKhau={matKhau}
        hoTen="Nguyễn Văn An"
        onClose={() => {
          setMatKhau(null);
          onClose();
        }}
      />
    </>
  );
}

function setup() {
  const user = userEvent.setup();
  const onClose = vi.fn();
  render(<Khung onClose={onClose} />);
  return { user, onClose, dialog: screen.getByRole("dialog", { name: "Mật khẩu tạm của Nguyễn Văn An" }) };
}

describe("Hộp thoại mật khẩu tạm (spec 0008, AC-2, AC-17)", () => {
  it("hiện mật khẩu kèm tên đọc từng ký tự cho trình đọc màn hình; axe sạch", async () => {
    const { dialog } = setup();
    expect(screen.getByText(MAT_KHAU)).toHaveAccessibleName("Mật khẩu tạm: A b 3 d E f 7 h J k 9 m");
    expect(screen.getByText(/Mật khẩu chỉ hiện một lần/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /close|đóng/i })).not.toBeInTheDocument();
    await expectNoA11yViolations(dialog);
  });

  it("Sao chép ghi mật khẩu vào clipboard và báo Đã sao chép qua vùng status", async () => {
    const { user } = setup();
    const writeText = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();
    await user.click(screen.getByRole("button", { name: "Sao chép" }));
    expect(writeText).toHaveBeenCalledWith(MAT_KHAU);
    expect(await screen.findByRole("status")).toHaveTextContent("Đã sao chép");
  });

  it("clipboard bị chặn thì hướng dẫn tự sao chép", async () => {
    const { user } = setup();
    vi.spyOn(navigator.clipboard, "writeText").mockRejectedValue(new Error("denied"));
    await user.click(screen.getByRole("button", { name: "Sao chép" }));
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Không sao chép được, hãy bôi chọn mật khẩu và nhấn Ctrl+C",
    );
  });

  it("bấm ra ngoài không đóng, mật khẩu vẫn còn", async () => {
    const { user, onClose } = setup();
    const overlay = document.querySelector<HTMLElement>('[data-slot="dialog-overlay"]');
    expect(overlay).not.toBeNull();
    await user.click(overlay!);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText(MAT_KHAU)).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it("Tôi đã lưu mật khẩu thì đóng và mật khẩu biến mất khỏi trang", async () => {
    const { user, onClose } = setup();
    await user.click(screen.getByRole("button", { name: "Tôi đã lưu mật khẩu" }));
    expect(onClose).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.queryByText(MAT_KHAU)).not.toBeInTheDocument();
  });

  it("focus nằm trong hộp thoại khi mở, Tab không thoát ra ngoài", async () => {
    const { user, dialog } = setup();
    await waitFor(() => expect(dialog).toContainElement(document.activeElement as HTMLElement));
    for (let i = 0; i < 4; i++) {
      await user.tab();
      expect(dialog).toContainElement(document.activeElement as HTMLElement);
    }
  });
});
