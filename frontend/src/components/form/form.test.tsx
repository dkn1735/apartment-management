import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DemoForm } from "@/app/dev/ui/_demo/demo-form";
import { RAW_MESSAGE } from "@/app/dev/ui/_demo/demo-api";
import { expectNoA11yViolations } from "@/test/axe";

function setup(onSuccess = vi.fn()) {
  const user = userEvent.setup();
  const view = render(<DemoForm delayMs={30} onSuccess={onSuccess} />);
  return { user, onSuccess, ...view };
}

async function fillValid(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/Họ và tên/), "Nguyễn Văn An");
  await user.type(screen.getByLabelText(/Số điện thoại/), "0912345678");
}

const submit = () => screen.getByRole("button", { name: /Lưu/ });

describe("form với lỗi API", () => {
  it("không có vi phạm axe ở trạng thái đầu và trạng thái lỗi", async () => {
    const { user, container } = setup();
    await expectNoA11yViolations(container);
    await user.click(submit());
    await screen.findAllByText(/Vui lòng nhập/);
    await expectNoA11yViolations(container);
  });

  it("ô bắt buộc có chữ ẩn (bắt buộc) và gợi ý nối qua aria-describedby", () => {
    setup();
    const phone = screen.getByLabelText(/Số điện thoại/);
    expect(screen.getByLabelText("Số điện thoại*(bắt buộc)")).toBe(phone);
    expect(phone).toHaveAccessibleDescription("10 chữ số, ví dụ 0912345678.");
  });

  it("đang gửi thì nút có aria-busy, aria-disabled và lần bấm thứ hai không gửi", async () => {
    const { user, onSuccess } = setup();
    await fillValid(user);
    await user.click(submit());
    expect(submit()).toHaveAttribute("aria-busy", "true");
    expect(submit()).toHaveAttribute("aria-disabled", "true");
    expect(submit()).not.toBeDisabled();
    await user.click(submit());
    await screen.findByText("Đã lưu");
    expect(onSuccess).toHaveBeenCalledTimes(1);
  });

  it("kiểm tra khi rời ô, rồi theo từng phím", async () => {
    const { user } = setup();
    const phone = screen.getByLabelText(/Số điện thoại/);
    await user.click(phone);
    await user.tab();
    expect(await screen.findByText("Vui lòng nhập số điện thoại.")).toBeInTheDocument();
    expect(phone).toHaveAttribute("aria-invalid", "true");
    await user.type(phone, "0912345678");
    await waitFor(() => expect(phone).toHaveAttribute("aria-invalid", "false"));
    expect(screen.queryByText(/Số điện thoại gồm 10 chữ số/)).not.toBeInTheDocument();
  });

  it("gửi còn lỗi phía trình duyệt thì focus về ô lỗi đầu tiên", async () => {
    const { user } = setup();
    await user.type(screen.getByLabelText(/Họ và tên/), "An");
    await user.click(submit());
    await waitFor(() => expect(screen.getByLabelText(/Số điện thoại/)).toHaveFocus());
  });

  it("lỗi theo trường vào đúng ô và ô nhận focus; lỗi không khớp ô vào FormAlert", async () => {
    const { user } = setup();
    await fillValid(user);
    await user.click(screen.getByRole("button", { name: "VALIDATION_FAILED theo trường" }));
    await user.click(submit());
    const phone = screen.getByLabelText(/Số điện thoại/);
    await waitFor(() => expect(phone).toHaveFocus());
    expect(phone).toHaveAttribute("aria-invalid", "true");
    expect(phone).toHaveAccessibleDescription(/Số điện thoại đã được dùng cho tài khoản khác\./);
    const alert = screen.getByRole("alert");
    expect(alert).toHaveTextContent("Mã lạ không khớp ô nào");
    expect(alert).toHaveTextContent("Dữ liệu chưa hợp lệ, vui lòng kiểm tra các ô được đánh dấu.");
  });

  it("lỗi hệ thống hiện FormAlert nhận focus kèm mã tra cứu; lặp lại vẫn nhận focus", async () => {
    const { user } = setup();
    await fillValid(user);
    await user.click(screen.getByRole("button", { name: "INTERNAL_ERROR có mã tra cứu" }));
    await user.click(submit());
    const alert = await screen.findByRole("alert");
    await waitFor(() => expect(alert).toHaveFocus());
    expect(alert).toHaveTextContent("Hệ thống đang gặp sự cố, vui lòng thử lại sau.");
    expect(alert).toHaveTextContent("Mã tra cứu: a1b2c3d4");

    submit().focus();
    await user.keyboard("{Enter}");
    await waitFor(() => expect(screen.getByRole("alert")).toHaveFocus());
    expect(document.body).not.toHaveTextContent(RAW_MESSAGE);
  });

  it("CONFLICT không có mã tra cứu và không hiện message của response", async () => {
    const { user } = setup();
    await fillValid(user);
    await user.click(screen.getByRole("button", { name: "CONFLICT" }));
    await user.click(submit());
    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Dữ liệu bị trùng");
    expect(alert).not.toHaveTextContent("Mã tra cứu");
    expect(document.body).not.toHaveTextContent(RAW_MESSAGE);
  });
});
