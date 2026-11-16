import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Component, type ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import * as z from "zod";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api/api";
import { FormAlert } from "./form-alert";
import { FormField } from "./form-field";
import { useApiForm } from "./use-api-form";

const schema = z.object({ hoTen: z.string(), soDienThoai: z.string() });

/** Form nhỏ hai ô, theo thứ tự Họ và tên rồi Số điện thoại. */
function HarnessForm({ onSubmit }: { onSubmit: () => Promise<unknown> }) {
  const form = useApiForm({ schema, defaultValues: { hoTen: "", soDienThoai: "" } });
  return (
    <form onSubmit={form.submitWith(onSubmit)} noValidate>
      <FormAlert error={form.formError} focusKey={form.formErrorKey} />
      <FormField control={form.control} name="hoTen" label="Họ và tên" render={(field) => <Input {...field} />} />
      <FormField
        control={form.control}
        name="soDienThoai"
        label="Số điện thoại"
        render={(field) => <Input {...field} />}
      />
      <button type="submit">Lưu</button>
    </form>
  );
}

class Boundary extends Component<{ children: ReactNode }, { caught: unknown }> {
  state = { caught: null as unknown };
  static getDerivedStateFromError(caught: unknown) {
    return { caught };
  }
  render() {
    return this.state.caught ? <p>Boundary bắt: {String(this.state.caught)}</p> : this.props.children;
  }
}

function validationError(errors: { field: string; message: string }[]) {
  return new ApiError({ status: 400, errorCode: "VALIDATION_FAILED", message: "raw", errors });
}

afterEach(() => vi.restoreAllMocks());

describe("useApiForm (AC-5, AC-6)", () => {
  it("ô nhận focus là ô đứng đầu trong form, không theo thứ tự errors[] của response", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(
      validationError([
        { field: "soDienThoai", message: "Số điện thoại đã được dùng." },
        { field: "hoTen", message: "Họ tên quá dài." },
      ]),
    );
    render(<HarnessForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "Lưu" }));

    await waitFor(() => expect(screen.getByLabelText("Họ và tên")).toHaveFocus());
    expect(screen.getByText("Số điện thoại đã được dùng.")).toBeInTheDocument();
    expect(screen.getByText("Họ tên quá dài.")).toBeInTheDocument();
  });

  it("mọi lỗi đều khớp ô thì không hiện khung lỗi chung", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(validationError([{ field: "hoTen", message: "Họ tên quá dài." }]));
    render(<HarnessForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByText("Họ tên quá dài.")).toBeInTheDocument();
    expect(screen.queryByText("Dữ liệu chưa hợp lệ, vui lòng kiểm tra các ô được đánh dấu.")).not.toBeInTheDocument();
  });

  it("gửi lại thành công thì khung lỗi chung biến mất", async () => {
    const user = userEvent.setup();
    const onSubmit = vi
      .fn()
      .mockRejectedValueOnce(new ApiError({ status: 409, errorCode: "CONFLICT", message: "raw" }))
      .mockResolvedValueOnce(undefined);
    render(<HarnessForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "Lưu" }));
    const alert = await screen.findByRole("alert");
    await waitFor(() => expect(alert).toHaveFocus());

    await user.click(screen.getByRole("button", { name: "Lưu" }));

    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
    expect(onSubmit).toHaveBeenCalledTimes(2);
  });

  it("lỗi không phải ApiError được ném cho error boundary, không nuốt im lặng", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(new TypeError("mạng hỏng"));
    render(
      <Boundary>
        <HarnessForm onSubmit={onSubmit} />
      </Boundary>,
    );

    await user.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByText(/Boundary bắt: TypeError: mạng hỏng/)).toBeInTheDocument();
  });
});
