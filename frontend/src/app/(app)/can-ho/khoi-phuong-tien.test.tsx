import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { messageFor } from "@/lib/api/error-messages";
import { createQueryClient } from "@/lib/api/query-client";
import type { PhuongTien, PhuongTienCanHo } from "@/lib/phuong-tien";
import { expectNoA11yViolations } from "@/test/axe";
import { ChiTietCanHo } from "./chi-tiet-can-ho";

const get = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn());
const put = vi.hoisted(() => vi.fn());
const notify = vi.hoisted(() => ({ success: vi.fn() }));
const hoSo = vi.hoisted(() => ({
  vaiTro: ["BAN_QUAN_LY"] as string[],
  maCuDan: null as string | null,
}));

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({
    data: {
      vaiTro: hoSo.vaiTro,
      chiTietVaiTro: hoSo.maCuDan ? { cuDan: { maCuDan: hoSo.maCuDan } } : {},
    },
  }),
}));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get, POST: post, PUT: put }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/components/feedback/notify", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/components/feedback/notify")>()),
  notifySuccess: notify.success,
}));

const CAN_HO = {
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

const RESIDENTS = {
  dangCuTru: [
    {
      maHoSo: "HS00000001",
      maCanHo: "A-0101",
      maCuDan: "CD00000001",
      maNguoiDung: "ND00000006",
      hoTen: "Vũ Cư Dân",
      soDienThoai: "0900000006",
      email: "cudan@chungcu.test",
      soCccd: "001026000006",
      vaiTroCuTru: "Chủ căn hộ",
      tinhTrangCuTru: "Thường trú",
      ngayBatDau: "2026-01-01",
      ngayKetThuc: null,
      trangThaiHoSo: "Hợp lệ",
      maNvbqlXacMinh: "BQL00000001",
      hoTenNguoiXacMinh: "Trần Ban Quản Lý",
      taoLuc: "2026-01-01T01:00:00Z",
      capNhatLuc: "2026-01-01T01:00:00Z",
      maCuDanDeNghi: null,
      hoTenNguoiDeNghi: null,
      ghiChuDeNghi: null,
      lyDoTuChoi: null,
      coTheHuy: false,
    },
  ],
  sapBatDau: [],
  lichSuCuTru: [],
  deNghiCuTru: [],
};

const XE_DANG_HOAT_DONG: PhuongTien = {
  maPhuongTien: "PT00000001",
  maCanHo: "A-0101",
  loaiPhuongTien: "Ô tô",
  bienSo: "30A12345",
  nhanHieuMauSac: "Toyota trắng",
  trangThaiDangKy: "Đang hoạt động",
  maCuDanSoHuu: "CD00000001",
  hoTenChuXe: "Vũ Cư Dân",
  chuXeConCuTru: true,
  maCuDanDangKy: null,
  hoTenNguoiDangKy: null,
  lyDoTrangThai: null,
  maNvbqlCapNhat: "BQL00000001",
  hoTenNguoiCapNhat: "Trần Ban Quản Lý",
  taoLuc: "2026-11-16T07:05:00Z",
  capNhatLuc: "2026-11-16T07:05:00Z",
  coTheRutDon: false,
};

const XE_DAP_KHONG_CON_CU_TRU: PhuongTien = {
  maPhuongTien: "PT00000002",
  maCanHo: "A-0101",
  loaiPhuongTien: "Xe đạp",
  bienSo: null,
  nhanHieuMauSac: "Thống Nhất xanh",
  trangThaiDangKy: "Tạm dừng",
  maCuDanSoHuu: "CD00000002",
  hoTenChuXe: "Nguyễn Đã Chuyển",
  chuXeConCuTru: false,
  maCuDanDangKy: null,
  hoTenNguoiDangKy: null,
  lyDoTrangThai: "Tạm dừng theo yêu cầu",
  maNvbqlCapNhat: "BQL00000001",
  hoTenNguoiCapNhat: "Trần Ban Quản Lý",
  taoLuc: "2026-11-10T03:00:00Z",
  capNhatLuc: "2026-11-15T08:30:00Z",
  coTheRutDon: false,
};

const DON_CHO_DUYET_CU_DAN: PhuongTien = {
  maPhuongTien: "PT00000003",
  maCanHo: "A-0101",
  loaiPhuongTien: "Xe máy",
  bienSo: "29B167890",
  nhanHieuMauSac: "Honda Vision",
  trangThaiDangKy: "Chờ duyệt",
  maCuDanSoHuu: "CD00000001",
  hoTenChuXe: "Vũ Cư Dân",
  chuXeConCuTru: true,
  maCuDanDangKy: "CD00000001",
  hoTenNguoiDangKy: "Vũ Cư Dân",
  lyDoTrangThai: null,
  maNvbqlCapNhat: null,
  hoTenNguoiCapNhat: null,
  taoLuc: "2026-11-20T02:00:00Z",
  capNhatLuc: "2026-11-20T02:00:00Z",
  coTheRutDon: true,
};

const VEHICLES_BQL: PhuongTienCanHo = {
  dangGui: [XE_DANG_HOAT_DONG, XE_DAP_KHONG_CON_CU_TRU],
  donVaLichSu: [{ ...DON_CHO_DUYET_CU_DAN, coTheRutDon: false }],
};

const VEHICLES_CU_DAN: PhuongTienCanHo = {
  dangGui: [
    { ...XE_DANG_HOAT_DONG, maNvbqlCapNhat: null, hoTenNguoiCapNhat: null },
    { ...XE_DAP_KHONG_CON_CU_TRU, maNvbqlCapNhat: null, hoTenNguoiCapNhat: null, lyDoTrangThai: null },
  ],
  donVaLichSu: [DON_CHO_DUYET_CU_DAN],
};

function setupDefaultGets(vehiclesResponse: PhuongTienCanHo = VEHICLES_BQL) {
  get.mockImplementation((url: string) => {
    if (url === "/api/v1/apartments/{apartmentId}") return Promise.resolve(CAN_HO);
    if (url === "/api/v1/apartments/{apartmentId}/residents") return Promise.resolve(RESIDENTS);
    if (url === "/api/v1/apartments/{apartmentId}/vehicles") return Promise.resolve(vehiclesResponse);
    return Promise.reject(new Error(`Unexpected GET ${url}`));
  });
}

function renderChiTiet(choPhepSua: boolean) {
  const queryClient = createQueryClient();
  const view = render(
    <QueryClientProvider client={queryClient}>
      <ChiTietCanHo maCanHo="A-0101" choPhepSua={choPhepSua} />
    </QueryClientProvider>,
  );
  return { ...view, queryClient };
}

describe("KhoiPhuongTien trên ChiTietCanHo", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    hoSo.maCuDan = null;
    setupDefaultGets();
  });

  it("hiển thị đúng danh sách phương tiện hai tab, nhãn cảnh báo cư trú cho BQL và đạt WCAG AA", async () => {
    const { container } = renderChiTiet(true);

    expect(await screen.findByRole("heading", { name: "Phương tiện" })).toBeInTheDocument();
    expect(await screen.findByText("30A12345")).toBeInTheDocument();
    expect(screen.getByText("Không có biển")).toBeInTheDocument();
    expect(screen.getByText("Chủ xe không còn cư trú")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Đăng ký phương tiện" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Đơn và lịch sử (1)" })).toBeInTheDocument();

    await expectNoA11yViolations(container);

    await userEvent.click(screen.getByRole("tab", { name: "Đơn và lịch sử (1)" }));
    expect(await screen.findByText("29B167890")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("kế toán và BQT xem được danh sách nhưng không thấy nút đăng ký hay đổi trạng thái", async () => {
    hoSo.vaiTro = ["KE_TOAN"];
    renderChiTiet(true);

    expect(await screen.findByText("30A12345")).toBeInTheDocument();
    expect(screen.getByText("Chủ xe không còn cư trú")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Đăng ký phương tiện" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Đổi trạng thái/ })).not.toBeInTheDocument();
  });

  it("cư dân không thấy nhãn cảnh báo chủ xe không còn cư trú, thấy nút Gửi đơn và Rút đơn", async () => {
    hoSo.vaiTro = ["KE_TOAN", "CU_DAN"];
    hoSo.maCuDan = "CD00000001";
    setupDefaultGets(VEHICLES_CU_DAN);
    renderChiTiet(false);

    expect(await screen.findByText("30A12345")).toBeInTheDocument();
    expect(screen.queryByText("Chủ xe không còn cư trú")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Gửi đơn đăng ký xe" })).toBeInTheDocument();

    await userEvent.click(screen.getByRole("tab", { name: "Đơn và lịch sử (1)" }));
    expect(await screen.findByRole("button", { name: /Rút đơn đăng ký xe 29B167890/ })).toBeInTheDocument();
  });

  it("lỗi API phương tiện chỉ hiện QueryErrorState có nút Thử lại, không che thông tin căn hộ", async () => {
    let failCount = 0;
    get.mockImplementation((url: string) => {
      if (url === "/api/v1/apartments/{apartmentId}") return Promise.resolve(CAN_HO);
      if (url === "/api/v1/apartments/{apartmentId}/residents") return Promise.resolve(RESIDENTS);
      if (url === "/api/v1/apartments/{apartmentId}/vehicles") {
        failCount += 1;
        if (failCount <= 2) {
          return Promise.reject(
            new ApiError({ status: 500, errorCode: "INTERNAL_ERROR", message: "fail", requestId: "req-1" }),
          );
        }
        return Promise.resolve(VEHICLES_BQL);
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    renderChiTiet(true);

    expect(await screen.findByRole("heading", { name: "Căn hộ A-0101" })).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: "Không tải được danh sách phương tiện" }, { timeout: 3000 }),
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(await screen.findByText("30A12345")).toBeInTheDocument();
  });

  it("hộp thoại đăng ký gắn lỗi 409 PHUONGTIEN_TRUNG_BIEN_SO vào ô biển số và đạt WCAG AA", async () => {
    post.mockRejectedValueOnce(
      new ApiError({
        status: 409,
        errorCode: "PHUONGTIEN_TRUNG_BIEN_SO",
        message: "Biển số này đang được đăng ký cho một phương tiện khác",
        errors: [
          {
            field: "bienSo",
            message: "Biển số đang được giữ bởi phương tiện PT00000001 của căn hộ A-0101",
          },
        ],
      }),
    );

    renderChiTiet(true);
    await userEvent.click(await screen.findByRole("button", { name: "Đăng ký phương tiện" }));

    const dialog = await screen.findByRole("dialog");
    await expectNoA11yViolations(dialog);

    const chuXeSelect = within(dialog).getByRole("combobox", { name: /Chủ xe/ });
    await userEvent.click(chuXeSelect);
    await userEvent.click(await screen.findByRole("option", { name: /Vũ Cư Dân/ }));

    const loaiSelect = within(dialog).getByRole("combobox", { name: /Loại phương tiện/ });
    await userEvent.click(loaiSelect);
    await userEvent.click(await screen.findByRole("option", { name: "Ô tô" }));

    const bienSoInput = within(dialog).getByLabelText(/Biển số/);
    await userEvent.type(bienSoInput, "30A-123.45");

    await userEvent.click(within(dialog).getByRole("button", { name: "Đăng ký" }));

    await waitFor(() => {
      expect(bienSoInput).toHaveAccessibleDescription(
        /Biển số đang được giữ bởi phương tiện PT00000001 của căn hộ A-0101/,
      );
    });
  });

  it("hộp thoại đổi trạng thái chỉ liệt kê đích hợp lệ theo ma trận AC-10 và bắt buộc lý do khi Tạm dừng", async () => {
    put.mockResolvedValueOnce({ ...XE_DANG_HOAT_DONG, trangThaiDangKy: "Tạm dừng", lyDoTrangThai: "Bảo trì xe" });
    const { queryClient } = renderChiTiet(true);
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");

    await userEvent.click(await screen.findByRole("button", { name: /Đổi trạng thái thẻ xe 30A12345/ }));
    const dialog = await screen.findByRole("dialog");

    const statusSelect = within(dialog).getByRole("combobox", { name: /Trạng thái mới/ });
    await userEvent.click(statusSelect);

    expect(await screen.findByRole("option", { name: "Tạm dừng" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Đã hủy" })).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: "Từ chối" })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("option", { name: "Tạm dừng" }));
    await userEvent.click(within(dialog).getByRole("button", { name: "Lưu trạng thái" }));

    const lyDoInput = within(dialog).getByLabelText(/Lý do/);
    await waitFor(() => {
      expect(lyDoInput).toHaveAccessibleDescription(/Vui lòng nhập lý do/);
    });

    await userEvent.type(lyDoInput, "Bảo trì xe");
    await userEvent.click(within(dialog).getByRole("button", { name: "Lưu trạng thái" }));

    await waitFor(() => {
      expect(put).toHaveBeenCalledWith("/api/v1/vehicles/{vehicleId}/status", {
        params: { path: { vehicleId: "PT00000001" } },
        body: { trangThaiDangKy: "Tạm dừng", lyDo: "Bảo trì xe" },
      });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["phuong-tien", "A-0101"] });
      expect(notify.success).toHaveBeenCalledWith("Đã cập nhật trạng thái thẻ xe");
    });
  });

  it("cư dân xác nhận rút đơn qua ConfirmDialog và làm mới danh sách", async () => {
    hoSo.vaiTro = ["CU_DAN"];
    hoSo.maCuDan = "CD00000001";
    setupDefaultGets(VEHICLES_CU_DAN);
    post.mockResolvedValueOnce({ ...DON_CHO_DUYET_CU_DAN, trangThaiDangKy: "Đã hủy", coTheRutDon: false });

    const { queryClient } = renderChiTiet(false);
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");

    await userEvent.click(await screen.findByRole("tab", { name: "Đơn và lịch sử (1)" }));
    await userEvent.click(await screen.findByRole("button", { name: /Rút đơn đăng ký xe 29B167890/ }));

    const alertDialog = await screen.findByRole("alertdialog");
    await userEvent.click(within(alertDialog).getByRole("button", { name: "Rút đơn" }));

    await waitFor(() => {
      expect(post).toHaveBeenCalledWith("/api/v1/vehicles/{vehicleId}/cancel", {
        params: { path: { vehicleId: "PT00000003" } },
      });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["phuong-tien", "A-0101"] });
      expect(notify.success).toHaveBeenCalledWith("Đã rút đơn đăng ký phương tiện");
    });
  });
});

const loi = (status: number, errorCode: string, errors?: { field: string; message: string }[]) =>
  new ApiError({ status, errorCode, message: "raw", errors });

async function chon(dialog: HTMLElement, oChon: RegExp, giaTri: RegExp | string) {
  await userEvent.click(within(dialog).getByRole("combobox", { name: oChon }));
  await userEvent.click(await screen.findByRole("option", { name: giaTri }));
}

describe("KhoiPhuongTien: trạng thái tải, rỗng và nội dung dòng", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    hoSo.maCuDan = null;
    setupDefaultGets();
  });

  // AC-14
  it("báo đang tải khi API phương tiện chưa trả về", async () => {
    get.mockImplementation((url: string) => {
      if (url === "/api/v1/apartments/{apartmentId}") return Promise.resolve(CAN_HO);
      if (url === "/api/v1/apartments/{apartmentId}/residents") return Promise.resolve(RESIDENTS);
      return new Promise(() => {});
    });
    renderChiTiet(true);

    expect(await screen.findByText("Đang tải phương tiện")).toBeInTheDocument();
    expect(screen.queryByRole("tab", { name: "Đang gửi" })).not.toBeInTheDocument();
  });

  // AC-14
  it("mỗi tab có trạng thái rỗng riêng và tab đơn không kèm số khi không có đơn chờ", async () => {
    setupDefaultGets({ dangGui: [], donVaLichSu: [] });
    renderChiTiet(true);

    expect(await screen.findByText("Chưa có phương tiện đang gửi")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("tab", { name: "Đơn và lịch sử" }));
    expect(await screen.findByText("Chưa có đơn hoặc lịch sử phương tiện")).toBeInTheDocument();
  });

  // AC-14
  it("dòng xe hiện lý do và thời điểm cập nhật theo giờ Việt Nam", async () => {
    renderChiTiet(true);

    const dong = (await screen.findByText("Không có biển")).closest("tr")!;
    expect(within(dong).getByText("Lý do: Tạm dừng theo yêu cầu")).toBeInTheDocument();
    expect(within(dong).getByText("15/11/2026 15:30")).toBeInTheDocument();
  });

  // AC-16
  it("đơn chờ duyệt chỉ có hai đích Đang hoạt động và Từ chối", async () => {
    renderChiTiet(true);
    await userEvent.click(await screen.findByRole("tab", { name: "Đơn và lịch sử (1)" }));
    await userEvent.click(await screen.findByRole("button", { name: "Đổi trạng thái thẻ xe 29B167890" }));

    const dialog = await screen.findByRole("dialog");
    await userEvent.click(within(dialog).getByRole("combobox", { name: /Trạng thái mới/ }));

    const luaChon = (await screen.findAllByRole("option")).map((o) => o.textContent);
    expect(luaChon).toEqual(["Đang hoạt động", "Từ chối"]);
  });

  // AC-16
  it("dòng ở trạng thái cuối không có nút đổi trạng thái", async () => {
    setupDefaultGets({
      dangGui: [],
      donVaLichSu: [{ ...XE_DANG_HOAT_DONG, trangThaiDangKy: "Đã hủy", lyDoTrangThai: "Đã bán xe" }],
    });
    renderChiTiet(true);
    await userEvent.click(await screen.findByRole("tab", { name: "Đơn và lịch sử" }));

    expect(await screen.findByText("Lý do: Đã bán xe")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Đổi trạng thái/ })).not.toBeInTheDocument();
  });
});

describe("hộp thoại phương tiện", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoSo.vaiTro = ["CU_DAN"];
    hoSo.maCuDan = "CD00000001";
    setupDefaultGets(VEHICLES_CU_DAN);
  });

  async function moDangKy(nut = "Gửi đơn đăng ký xe") {
    await userEvent.click(await screen.findByRole("button", { name: nut }));
    const dialog = await screen.findByRole("dialog");
    await waitFor(() =>
      expect(within(dialog).queryByText("Đang tải danh sách cư dân đang cư trú...")).not.toBeInTheDocument(),
    );
    return dialog;
  }

  // AC-15
  it("cư dân được chọn sẵn chính mình, xe đạp gửi được không cần biển, xong thì sang tab đơn", async () => {
    post.mockResolvedValueOnce(DON_CHO_DUYET_CU_DAN);
    const { queryClient } = renderChiTiet(false);
    const lamMoi = vi.spyOn(queryClient, "invalidateQueries");
    const dialog = await moDangKy();
    expect(within(dialog).getByRole("combobox", { name: /Chủ xe/ })).toHaveTextContent("Vũ Cư Dân");

    await chon(dialog, /Loại phương tiện/, "Xe đạp");
    expect(within(dialog).getByLabelText(/Biển số \(không bắt buộc với xe đạp\)/)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole("button", { name: "Gửi đơn" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(post).toHaveBeenCalledWith("/api/v1/vehicles", {
      body: {
        maCanHo: "A-0101",
        maCuDanSoHuu: "CD00000001",
        loaiPhuongTien: "Xe đạp",
        bienSo: null,
        nhanHieuMauSac: null,
      },
    });
    expect(lamMoi).toHaveBeenCalledWith({ queryKey: ["phuong-tien", "A-0101"] });
    expect(notify.success).toHaveBeenCalledWith("Đã gửi đơn đăng ký xe");
    expect(screen.getByRole("tab", { name: /Đơn và lịch sử/ })).toHaveAttribute("aria-selected", "true");
  });

  // AC-6, AC-15
  it("xe máy thiếu biển thì báo lỗi tại ô biển số và không gọi API; biển hợp lệ được chuẩn hóa", async () => {
    post.mockResolvedValueOnce(DON_CHO_DUYET_CU_DAN);
    renderChiTiet(false);
    const dialog = await moDangKy();
    await chon(dialog, /Loại phương tiện/, "Xe máy");
    await userEvent.click(within(dialog).getByRole("button", { name: "Gửi đơn" }));

    const bienSo = within(dialog).getByLabelText(/Biển số/);
    await waitFor(() => expect(bienSo).toHaveAccessibleDescription(/Vui lòng nhập biển số/));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(bienSo, "29-b1 678.90");
    await userEvent.click(within(dialog).getByRole("button", { name: "Gửi đơn" }));

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith("/api/v1/vehicles", {
        body: expect.objectContaining({ bienSo: "29B167890" }),
      }),
    );
  });

  // AC-15
  it("lỗi 422 chủ xe không cư trú gắn vào ô Chủ xe", async () => {
    const cau = "Chủ xe không đang cư trú hợp lệ tại căn hộ này";
    post.mockRejectedValueOnce(loi(422, "PHUONGTIEN_CHU_XE_KHONG_CU_TRU", [{ field: "maCuDanSoHuu", message: cau }]));
    renderChiTiet(false);
    const dialog = await moDangKy();
    await chon(dialog, /Loại phương tiện/, "Xe đạp");
    await userEvent.click(within(dialog).getByRole("button", { name: "Gửi đơn" }));

    await waitFor(() =>
      expect(within(dialog).getByRole("combobox", { name: /Chủ xe/ })).toHaveAccessibleDescription(new RegExp(cau)),
    );
  });

  // AC-15, AC-16
  it("lỗi 422 vượt số đơn chờ hiện ở đầu hộp thoại theo errorCode và hộp thoại vẫn mở", async () => {
    post.mockRejectedValueOnce(loi(422, "PHUONGTIEN_VUOT_SO_DON_CHO_DUYET"));
    renderChiTiet(false);
    const dialog = await moDangKy();
    await chon(dialog, /Loại phương tiện/, "Xe đạp");
    await userEvent.click(within(dialog).getByRole("button", { name: "Gửi đơn" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent(
      messageFor(loi(422, "PHUONGTIEN_VUOT_SO_DON_CHO_DUYET")),
    );
    expect(notify.success).not.toHaveBeenCalled();
  });

  // AC-16
  it("kích hoạt lại không cần lý do và gửi lyDo là null", async () => {
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    setupDefaultGets();
    put.mockResolvedValueOnce(XE_DAP_KHONG_CON_CU_TRU);
    renderChiTiet(true);
    await userEvent.click(await screen.findByRole("button", { name: "Đổi trạng thái thẻ xe PT00000002" }));
    const dialog = await screen.findByRole("dialog");
    await chon(dialog, /Trạng thái mới/, "Đang hoạt động");
    await userEvent.click(within(dialog).getByRole("button", { name: "Lưu trạng thái" }));

    await waitFor(() =>
      expect(put).toHaveBeenCalledWith("/api/v1/vehicles/{vehicleId}/status", {
        params: { path: { vehicleId: "PT00000002" } },
        body: { trangThaiDangKy: "Đang hoạt động", lyDo: null },
      }),
    );
  });

  // AC-16
  it("lỗi khi đổi trạng thái hiện trong hộp thoại theo errorCode", async () => {
    hoSo.vaiTro = ["BAN_QUAN_LY"];
    setupDefaultGets();
    put.mockRejectedValueOnce(loi(409, "PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE"));
    renderChiTiet(true);
    await userEvent.click(await screen.findByRole("button", { name: "Đổi trạng thái thẻ xe PT00000002" }));
    const dialog = await screen.findByRole("dialog");
    await chon(dialog, /Trạng thái mới/, "Đang hoạt động");
    await userEvent.click(within(dialog).getByRole("button", { name: "Lưu trạng thái" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent(
      messageFor(loi(409, "PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE")),
    );
  });

  // AC-16: bàn phím
  it("mở bằng bàn phím và đóng bằng Escape thì focus trở lại nút mở", async () => {
    renderChiTiet(false);
    const nut = await screen.findByRole("button", { name: "Gửi đơn đăng ký xe" });
    nut.focus();
    await userEvent.keyboard("{Enter}");
    expect(await screen.findByRole("dialog")).toBeInTheDocument();

    await userEvent.keyboard("{Escape}");

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(nut).toHaveFocus();
  });
});
