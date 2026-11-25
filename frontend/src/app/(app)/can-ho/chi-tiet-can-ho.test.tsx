import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { ChiTietCanHo } from "./chi-tiet-can-ho";

const get = vi.hoisted(() => vi.fn());
const put = vi.hoisted(() => vi.fn());
const notify = vi.hoisted(() => ({ success: vi.fn() }));
const hoSo = vi.hoisted(() => ({ vaiTro: ["BAN_QUAN_LY"] as string[] }));

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({ data: { vaiTro: hoSo.vaiTro } }),
}));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get, PUT: put }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/components/feedback/notify", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/components/feedback/notify")>()),
  notifySuccess: notify.success,
}));

const A0101 = {
  maCanHo: "A-0101",
  maTang: "A-01",
  soTang: 1,
  maToaNha: "A",
  tenToaNha: "Tòa A",
  dienTich: 75.5,
  trangThaiSuDung: "Đang sử dụng",
  biTamDungTienIch: false,
};

function hienThi(vaiTro: string[], choPhepSua: boolean, ketQua: () => Promise<unknown> = () => Promise.resolve(A0101)) {
  hoSo.vaiTro = vaiTro;
  get.mockImplementation(ketQua);
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <ChiTietCanHo maCanHo="a-0101" quayLai={{ href: "/can-ho", label: "Danh sách căn hộ" }} choPhepSua={choPhepSua} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  get.mockReset();
  put.mockReset();
  notify.success.mockReset();
});

describe("Chi tiết căn hộ (spec 0012, AC-19)", () => {
  it("hiện đủ thông tin, mã đã viết hoa; axe sạch", async () => {
    const { container } = hienThi(["BAN_QUAN_LY"], true);
    expect(await screen.findByRole("heading", { name: "Căn hộ A-0101" })).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/api/v1/apartments/{apartmentId}", {
      params: { path: { apartmentId: "A-0101" } },
    });
    expect(screen.getByText("Tòa A (A)")).toBeInTheDocument();
    expect(screen.getByText("75,50 m²")).toBeInTheDocument();
    expect(screen.getByText("Đang dùng bình thường")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("nút Sửa ẩn với Kế toán và trên trang cư dân", async () => {
    hienThi(["KE_TOAN"], true);
    await screen.findByRole("heading", { name: "Căn hộ A-0101" });
    expect(screen.queryByRole("button", { name: "Sửa" })).not.toBeInTheDocument();
  });

  it("404 hiện Không tìm thấy căn hộ cùng link quay lại", async () => {
    hienThi(["CU_DAN"], false, () =>
      Promise.reject(new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "x", errors: [] })),
    );
    expect(await screen.findByRole("heading", { name: "Không tìm thấy căn hộ" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Danh sách căn hộ" })).toHaveAttribute("href", "/can-ho");
  });

  it("BQL sửa: hai ô điền sẵn, gửi diện tích đã đổi dấu phẩy, báo toast", async () => {
    const user = userEvent.setup();
    put.mockResolvedValue({ ...A0101, dienTich: 80 });
    hienThi(["BAN_QUAN_LY"], true);
    await user.click(await screen.findByRole("button", { name: "Sửa" }));
    const dienTich = screen.getByRole("textbox", { name: /Diện tích/ });
    expect(dienTich).toHaveValue("75,50");
    expect(screen.getByRole("combobox", { name: /Trạng thái sử dụng/ })).toHaveTextContent("Đang sử dụng");
    await user.clear(dienTich);
    await user.type(dienTich, "80,25");
    await user.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã cập nhật căn hộ A-0101"));
    expect(put).toHaveBeenCalledWith("/api/v1/apartments/{apartmentId}", {
      params: { path: { apartmentId: "A-0101" } },
      body: { dienTich: 80.25, trangThaiSuDung: "Đang sử dụng" },
    });
  });

  it("lỗi 400 hiện dưới ô, 404 hiện trong hộp thoại", async () => {
    const user = userEvent.setup();
    put.mockRejectedValueOnce(
      new ApiError({
        status: 400,
        errorCode: "VALIDATION_FAILED",
        message: "Dữ liệu không hợp lệ",
        errors: [
          { field: "dienTich", message: "Diện tích phải lớn hơn 0, tối đa 1000 m² và tối đa 2 chữ số thập phân" },
        ],
      }),
    );
    hienThi(["BAN_QUAN_LY"], true);
    await user.click(await screen.findByRole("button", { name: "Sửa" }));
    await user.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    const dienTich = screen.getByRole("textbox", { name: /Diện tích/ });
    await waitFor(() => expect(dienTich).toHaveAccessibleDescription(/tối đa 1000 m²/));
    put.mockRejectedValueOnce(
      new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "Không tìm thấy dữ liệu yêu cầu", errors: [] }),
    );
    await user.click(screen.getByRole("button", { name: "Lưu thay đổi" }));
    expect(await screen.findByText(/Không tìm thấy dữ liệu yêu cầu/)).toBeInTheDocument();
  });
});
