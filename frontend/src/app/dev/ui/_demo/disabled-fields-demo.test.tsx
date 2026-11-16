import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { expectNoA11yViolations } from "@/test/axe";
import { DisabledFieldsDemo } from "./disabled-fields-demo";

describe("trạng thái vô hiệu của ô nhập (AC-3)", () => {
  it("mọi loại ô đều vô hiệu, có nhãn, không vi phạm axe", async () => {
    const { container } = render(<DisabledFieldsDemo />);
    expect(screen.getByRole("textbox", { name: "Mã căn hộ" })).toBeDisabled();
    expect(screen.getByRole("textbox", { name: "Ghi chú" })).toBeDisabled();
    expect(screen.getByRole("combobox", { name: "Loại căn hộ" })).toBeDisabled();
    expect(screen.getByRole("switch", { name: "Nhận thông báo qua email" })).toBeDisabled();
    expect(screen.getByRole("checkbox", { name: "Đã xác nhận thông tin" })).toBeDisabled();
    await expectNoA11yViolations(container);
  });

  it("phím Tab bỏ qua các ô vô hiệu", async () => {
    render(<DisabledFieldsDemo />);
    await userEvent.tab();
    expect(document.body).toHaveFocus();
  });
});
