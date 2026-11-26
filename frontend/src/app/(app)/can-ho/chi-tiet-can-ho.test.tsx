import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { ChiTietCanHo } from "./chi-tiet-can-ho";

const get = vi.hoisted(() => vi.fn());
const put = vi.hoisted(() => vi.fn());
const patch = vi.hoisted(() => vi.fn());
const notify = vi.hoisted(() => ({ success: vi.fn() }));
const hoSo = vi.hoisted(() => ({ vaiTro: ["BAN_QUAN_LY"] as string[] }));

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({ data: { vaiTro: hoSo.vaiTro } }),
}));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get, PUT: put, PATCH: patch }, api: (p: Promise<unknown>) => p };
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
  lanDoiTienIch: null,
};

const TAM_DUNG = {
  ...A0101,
  biTamDungTienIch: true,
  lanDoiTienIch: {
    lyDo: "Nợ phí quý 3\n<b>hai kỳ</b>",
    doiLuc: "2026-11-26T03:00:00Z",
    maNguoiDoi: "ND00000004",
    hoTenNguoiDoi: "Phạm Kế Toán",
  },
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
  patch.mockReset();
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

describe("Tạm dừng tiện ích trên chi tiết căn hộ (spec 0013, AC-14, AC-15, AC-17)", () => {
  it("hiện lý do tạm dừng nguyên văn và người đổi", async () => {
    hienThi(["BAN_QUAN_LY"], true, () => Promise.resolve(TAM_DUNG));
    await screen.findByRole("heading", { name: "Căn hộ A-0101" });
    expect(screen.getByText("Lý do tạm dừng")).toBeInTheDocument();
    expect(screen.getByText(/<b>hai kỳ<\/b>/)).toBeInTheDocument();
    expect(screen.getByText("26/11/2026 10:00 bởi Phạm Kế Toán (ND00000004)")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mở khóa tiện ích" })).toBeInTheDocument();
  });

  it("cư dân: lý do mở khóa, không có người đổi, không có nút", async () => {
    const moKhoa = {
      ...A0101,
      lanDoiTienIch: { lyDo: "Đã thanh toán", doiLuc: "2026-11-26T03:00:00Z", maNguoiDoi: null, hoTenNguoiDoi: null },
    };
    hienThi(["CU_DAN"], false, () => Promise.resolve(moKhoa));
    await screen.findByRole("heading", { name: "Căn hộ A-0101" });
    expect(screen.getByText("Lý do mở khóa")).toBeInTheDocument();
    expect(screen.getByText("26/11/2026 10:00")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /tiện ích/ })).not.toBeInTheDocument();
  });

  it("chỉ hiện mã khi không có họ tên người đổi", async () => {
    hienThi(["BAN_QUAN_LY"], true, () =>
      Promise.resolve({ ...TAM_DUNG, lanDoiTienIch: { ...TAM_DUNG.lanDoiTienIch, hoTenNguoiDoi: null } }),
    );
    expect(await screen.findByText("26/11/2026 10:00 bởi ND00000004")).toBeInTheDocument();
  });

  it("BQT không thấy nút đổi tiện ích", async () => {
    hienThi(["BAN_QUAN_TRI"], true);
    await screen.findByRole("heading", { name: "Căn hộ A-0101" });
    expect(screen.queryByRole("button", { name: "Tạm dừng tiện ích" })).not.toBeInTheDocument();
  });

  it("Kế toán tạm dừng: chặn lý do rỗng, gửi đúng body, báo toast, focus về nút; axe sạch", async () => {
    const user = userEvent.setup();
    patch.mockResolvedValue(TAM_DUNG);
    hienThi(["KE_TOAN"], true);
    const nut = await screen.findByRole("button", { name: "Tạm dừng tiện ích" });
    await user.click(nut);
    const dialog = screen.getByRole("dialog", { name: "Tạm dừng tiện ích căn hộ A-0101" });
    const lyDo = screen.getByRole("textbox", { name: /Lý do/ });
    expect(lyDo).toHaveAccessibleDescription("Còn 500/500 ký tự");
    await user.click(screen.getByRole("button", { name: "Tạm dừng" }));
    await waitFor(() => expect(lyDo).toHaveAccessibleDescription(/Vui lòng nhập lý do/));
    expect(patch).not.toHaveBeenCalled();
    await user.type(lyDo, "Nợ phí");
    expect(lyDo).toHaveAccessibleDescription("Còn 494/500 ký tự");
    await expectNoA11yViolations(dialog);
    await user.click(screen.getByRole("button", { name: "Tạm dừng" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã tạm dừng tiện ích căn hộ A-0101"));
    expect(patch).toHaveBeenCalledWith("/api/v1/apartments/{apartmentId}/facility-suspension", {
      params: { path: { apartmentId: "A-0101" } },
      body: { biTamDungTienIch: true, lyDo: "Nợ phí" },
    });
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(nut).toHaveFocus();
  });

  it("lỗi 400 hiện dưới ô lý do; 409 hiện trong hộp thoại và làm mới căn", async () => {
    const user = userEvent.setup();
    patch.mockRejectedValueOnce(
      new ApiError({
        status: 400,
        errorCode: "VALIDATION_FAILED",
        message: "Dữ liệu không hợp lệ",
        errors: [{ field: "lyDo", message: "Lý do tối đa 500 ký tự" }],
      }),
    );
    hienThi(["BAN_QUAN_LY"], true);
    await user.click(await screen.findByRole("button", { name: "Tạm dừng tiện ích" }));
    const lyDo = screen.getByRole("textbox", { name: /Lý do/ });
    await user.type(lyDo, "x");
    await user.click(screen.getByRole("button", { name: "Tạm dừng" }));
    await waitFor(() => expect(lyDo).toHaveAccessibleDescription(/Lý do tối đa 500 ký tự/));
    patch.mockRejectedValueOnce(
      new ApiError({
        status: 409,
        errorCode: "CANHO_TIEN_ICH_KHONG_DOI",
        message: "Căn hộ đang bị tạm dừng tiện ích",
        errors: [],
      }),
    );
    const lanGetTruoc = get.mock.calls.length;
    await user.click(screen.getByRole("button", { name: "Tạm dừng" }));
    expect(await screen.findByText(/Căn hộ đang bị tạm dừng tiện ích/)).toBeInTheDocument();
    await waitFor(() => expect(get.mock.calls.length).toBeGreaterThan(lanGetTruoc));
  });

  // covers: AC-3, AC-15
  it("lý do chỉ có khoảng trắng bị chặn, không gọi API", async () => {
    const user = userEvent.setup();
    hienThi(["BAN_QUAN_LY"], true);
    await user.click(await screen.findByRole("button", { name: "Tạm dừng tiện ích" }));
    const lyDo = screen.getByRole("textbox", { name: /Lý do/ });
    await user.type(lyDo, "   ");
    expect(lyDo).toHaveAccessibleDescription("Còn 500/500 ký tự");
    await user.click(screen.getByRole("button", { name: "Tạm dừng" }));
    await waitFor(() => expect(lyDo).toHaveAccessibleDescription(/Vui lòng nhập lý do/));
    expect(patch).not.toHaveBeenCalled();
  });

  // covers: AC-15 (đếm theo code point: mỗi emoji giảm 1)
  it("số ký tự còn lại đếm emoji là một ký tự", async () => {
    const user = userEvent.setup();
    hienThi(["BAN_QUAN_LY"], true);
    await user.click(await screen.findByRole("button", { name: "Tạm dừng tiện ích" }));
    const lyDo = screen.getByRole("textbox", { name: /Lý do/ });
    await user.click(lyDo);
    await user.paste("😀😀");
    expect(lyDo).toHaveAccessibleDescription("Còn 498/500 ký tự");
  });

  // covers: AC-15 (404 hiện trong hộp thoại, hộp thoại vẫn mở)
  it("404 hiện thông báo lỗi trong hộp thoại", async () => {
    const user = userEvent.setup();
    patch.mockRejectedValueOnce(
      new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "Không tìm thấy dữ liệu yêu cầu", errors: [] }),
    );
    hienThi(["KE_TOAN"], true);
    await user.click(await screen.findByRole("button", { name: "Tạm dừng tiện ích" }));
    await user.type(screen.getByRole("textbox", { name: /Lý do/ }), "Nợ phí");
    await user.click(screen.getByRole("button", { name: "Tạm dừng" }));
    const dialog = screen.getByRole("dialog", { name: "Tạm dừng tiện ích căn hộ A-0101" });
    expect(await within(dialog).findByText(/Không tìm thấy dữ liệu yêu cầu/)).toBeInTheDocument();
    expect(notify.success).not.toHaveBeenCalled();
  });

  // covers: AC-15, AC-17 (mở khóa gửi false, báo toast; Esc đóng và focus về nút)
  it("mở khóa: gửi biTamDungTienIch false, báo toast; Esc đóng hộp thoại và trả focus", async () => {
    const user = userEvent.setup();
    patch.mockResolvedValue(A0101);
    hienThi(["BAN_QUAN_LY"], true, () => Promise.resolve(TAM_DUNG));
    const nut = await screen.findByRole("button", { name: "Mở khóa tiện ích" });

    await user.click(nut);
    expect(screen.getByRole("dialog", { name: "Mở khóa tiện ích căn hộ A-0101" })).toBeInTheDocument();
    await user.keyboard("{Escape}");
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(nut).toHaveFocus();

    await user.keyboard("{Enter}");
    await user.type(screen.getByRole("textbox", { name: /Lý do/ }), "Đã thanh toán");
    await user.click(screen.getByRole("button", { name: "Mở khóa" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã mở khóa tiện ích căn hộ A-0101"));
    expect(patch).toHaveBeenCalledWith("/api/v1/apartments/{apartmentId}/facility-suspension", {
      params: { path: { apartmentId: "A-0101" } },
      body: { biTamDungTienIch: false, lyDo: "Đã thanh toán" },
    });
  });

  // covers: AC-5, AC-15 (câu 409 theo chiều đã yêu cầu)
  it("409 khi mở khóa hiện câu căn đang dùng bình thường", async () => {
    const user = userEvent.setup();
    patch.mockRejectedValueOnce(
      new ApiError({
        status: 409,
        errorCode: "CANHO_TIEN_ICH_KHONG_DOI",
        message: "Căn hộ đang dùng tiện ích bình thường",
        errors: [],
      }),
    );
    hienThi(["KE_TOAN"], true, () => Promise.resolve(TAM_DUNG));
    await user.click(await screen.findByRole("button", { name: "Mở khóa tiện ích" }));
    await user.type(screen.getByRole("textbox", { name: /Lý do/ }), "x");
    await user.click(screen.getByRole("button", { name: "Mở khóa" }));
    const cau = await screen.findByText(/Căn hộ đang dùng tiện ích bình thường\./);
    await waitFor(() => expect(cau.closest("[role='alert']")).toHaveFocus());
  });
});
