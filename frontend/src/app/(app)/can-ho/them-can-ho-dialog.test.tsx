import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { ThemCanHoDialog } from "./them-can-ho-dialog";

const get = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn());
const notify = vi.hoisted(() => ({ success: vi.fn() }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get, POST: post }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/components/feedback/notify", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/components/feedback/notify")>()),
  notifySuccess: notify.success,
}));

const TOA = [{ maToaNha: "A", tenToaNha: "Tòa A", soTang: 2, diaChiKhuVuc: "HN", soCanHo: 3 }];
const TANG = {
  items: [
    { maTang: "A-01", soTang: 1, soCanHo: 4 },
    { maTang: "A-02", soTang: 2, soCanHo: 4 },
  ],
  page: 1,
  size: 100,
  totalItems: 2,
  totalPages: 1,
};

function hienThi() {
  get.mockResolvedValue(TANG);
  render(
    <QueryClientProvider client={createQueryClient()}>
      <ThemCanHoDialog toaNha={TOA} />
    </QueryClientProvider>,
  );
}

async function dien(user: ReturnType<typeof userEvent.setup>, soCan: string, dienTich: string) {
  await user.click(screen.getByRole("button", { name: "Thêm căn hộ" }));
  await user.click(screen.getByRole("combobox", { name: /Tòa nhà/ }));
  await user.click(await screen.findByRole("option", { name: "Tòa A" }));
  await user.click(screen.getByRole("combobox", { name: /Tầng/ }));
  await user.click(await screen.findByRole("option", { name: "Tầng 1 (A-01)" }));
  await user.type(screen.getByRole("textbox", { name: /Số căn/ }), soCan);
  await user.type(screen.getByRole("textbox", { name: /Diện tích/ }), dienTich);
}

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  notify.success.mockReset();
});

describe("Hộp thoại Thêm căn hộ (spec 0012, AC-18)", () => {
  it("tầng lấy theo tòa đã chọn, xem trước mã căn, axe sạch", async () => {
    const user = userEvent.setup();
    hienThi();
    await dien(user, "5", "70,5");
    expect(get).toHaveBeenCalledWith("/api/v1/buildings/{buildingId}/floors", {
      params: { path: { buildingId: "A" }, query: { size: 100, sort: "soTang,asc" } },
    });
    expect(screen.getByText(/Mã căn sẽ tạo:/)).toHaveTextContent("Mã căn sẽ tạo: A-0105");
    expect(screen.getByRole("textbox", { name: /Số căn/ })).toHaveAttribute("inputmode", "numeric");
    await expectNoA11yViolations(document.body);
  });

  it("gửi số và diện tích đã đổi dấu phẩy; thành công thì báo toast và đóng", async () => {
    const user = userEvent.setup();
    post.mockResolvedValue({ maCanHo: "A-0105" });
    hienThi();
    await dien(user, "5", "70,5");
    await user.click(screen.getByRole("dialog").querySelector("button[type=submit]")!);
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã thêm căn hộ A-0105"));
    expect(post).toHaveBeenCalledWith("/api/v1/apartments", {
      body: { maTang: "A-01", soCan: 5, dienTich: 70.5, trangThaiSuDung: "Trống" },
    });
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("lỗi 409 hiện dưới ô số căn", async () => {
    const user = userEvent.setup();
    post.mockRejectedValue(
      new ApiError({
        status: 409,
        errorCode: "CANHO_CAN_HO_DA_TON_TAI",
        message: "Căn hộ đã tồn tại",
        errors: [{ field: "soCan", message: "Tầng này đã có căn số 5 (A-0105)" }],
      }),
    );
    hienThi();
    await dien(user, "5", "70");
    await user.click(screen.getByRole("dialog").querySelector("button[type=submit]")!);
    const soCan = screen.getByRole("textbox", { name: /Số căn/ });
    await waitFor(() => expect(soCan).toHaveAccessibleDescription(/Tầng này đã có căn số 5 \(A-0105\)/));
    expect(soCan).toHaveFocus();
  });

  it("ẩn lựa chọn Đang sử dụng khi tạo căn hộ mới (spec 0014, AC-17)", async () => {
    const user = userEvent.setup();
    hienThi();
    await user.click(screen.getByRole("button", { name: "Thêm căn hộ" }));
    await user.click(screen.getByRole("combobox", { name: /Trạng thái sử dụng/ }));
    expect(await screen.findByRole("option", { name: "Trống" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Đang sửa chữa" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Đã bàn giao" })).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: "Đang sử dụng" })).not.toBeInTheDocument();
  });
});
