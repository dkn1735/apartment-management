import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import type { HoSoCuTru } from "@/lib/cu-tru";
import { expectNoA11yViolations } from "@/test/axe";
import { mockPathname, mockPush, mockReplace, mockSearchParams } from "@/test/next-navigation";
import { HoSoCuTruView } from "./ho-so-cu-tru-view";

const get = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn());
const put = vi.hoisted(() => vi.fn());
const notify = vi.hoisted(() => ({ success: vi.fn() }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get, POST: post, PUT: put }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/components/feedback/notify", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/components/feedback/notify")>()),
  notifySuccess: notify.success,
}));

const TOA_NHA = [
  { maToaNha: "A", tenToaNha: "Tòa A", soTang: 3, diaChiKhuVuc: "Khu A", soCanHo: 10 },
  { maToaNha: "B", tenToaNha: "Tòa B", soTang: 2, diaChiKhuVuc: "Khu B", soCanHo: 5 },
];

const CAN_HO_TOA_A = [
  {
    maCanHo: "A-0101",
    maTang: "A-01",
    soTang: 1,
    maToaNha: "A",
    tenToaNha: "Tòa A",
    dienTich: 75.5,
    trangThaiSuDung: "Đang sử dụng",
    biTamDungTienIch: false,
    lanDoiTienIch: null,
  },
];

const HO_SO_CHO_XAC_MINH: HoSoCuTru = {
  maHoSo: "HS00000010",
  maCanHo: "A-0101",
  maCuDan: "CD00000006",
  maNguoiDung: "ND00000010",
  hoTen: "Đặng Tạm Trú",
  soDienThoai: "0900000010",
  email: "tamtru@chungcu.test",
  soCccd: "001026000010",
  vaiTroCuTru: "Khách thuê",
  tinhTrangCuTru: "Tạm trú",
  ngayBatDau: "2027-01-01",
  ngayKetThuc: "2027-06-30",
  trangThaiHoSo: "Chờ xác minh",
  maNvbqlXacMinh: null,
  hoTenNguoiXacMinh: null,
  taoLuc: "2026-11-26T02:00:00Z",
  capNhatLuc: "2026-11-26T02:00:00Z",
  maCuDanDeNghi: "CD00000001",
  hoTenNguoiDeNghi: "Vũ Cư Dân",
  ghiChuDeNghi: "Thuê căn hộ 6 tháng",
  lyDoTuChoi: null,
  coTheHuy: false,
};

const HO_SO_DANG_O: HoSoCuTru = {
  ...HO_SO_CHO_XAC_MINH,
  maHoSo: "HS00000001",
  maCuDan: "CD00000001",
  hoTen: "Vũ Chủ Hộ",
  soDienThoai: "0900000006",
  soCccd: "001026000006",
  vaiTroCuTru: "Chủ căn hộ",
  tinhTrangCuTru: "Thường trú",
  ngayBatDau: "2026-01-01",
  ngayKetThuc: null,
  trangThaiHoSo: "Hợp lệ",
  maNvbqlXacMinh: "BQL00000001",
  hoTenNguoiXacMinh: "Trần Ban Quản Lý",
  maCuDanDeNghi: null,
  hoTenNguoiDeNghi: null,
  ghiChuDeNghi: null,
};

const HO_SO_SAP_BAT_DAU: HoSoCuTru = {
  ...HO_SO_DANG_O,
  maHoSo: "HS00000002",
  hoTen: "Lê Sắp Chuyển Đến",
  vaiTroCuTru: "Khách thuê",
  tinhTrangCuTru: "Tạm trú",
  ngayBatDau: "2099-05-01",
  ngayKetThuc: "2099-12-31",
};

const HO_SO_QUA_HAN: HoSoCuTru = {
  ...HO_SO_DANG_O,
  maHoSo: "HS00000003",
  hoTen: "Hoàng Quá Hạn",
  vaiTroCuTru: "Khách thuê",
  tinhTrangCuTru: "Tạm trú",
  ngayBatDau: "2025-01-01",
  ngayKetThuc: "2025-12-31",
};

const HO_SO_TU_CHOI: HoSoCuTru = {
  ...HO_SO_CHO_XAC_MINH,
  maHoSo: "HS00000011",
  hoTen: "Phạm Bị Từ Chối",
  trangThaiHoSo: "Từ chối",
  maNvbqlXacMinh: "BQL00000001",
  hoTenNguoiXacMinh: "Trần Ban Quản Lý",
  lyDoTuChoi: "Chưa bổ sung hợp đồng thuê",
};

const LUA_CHON_CU_DAN = [
  {
    maCuDan: "CD00000005",
    maNguoiDung: "ND00000009",
    hoTen: "Nguyễn Văn Mới",
    soDienThoai: "0900000009",
    email: "moi@chungcu.test",
    soCccd: "001026000009",
    trangThaiTaiKhoan: "Hoạt động",
  },
];

function moManHinh(
  qs = "",
  danhSach: HoSoCuTru[] = [HO_SO_CHO_XAC_MINH, HO_SO_DANG_O, HO_SO_SAP_BAT_DAU, HO_SO_QUA_HAN, HO_SO_TU_CHOI],
  soChoXacMinh = 2,
) {
  mockPathname.mockReturnValue("/ho-so-cu-tru");
  mockSearchParams.mockReturnValue(new URLSearchParams(qs));
  get.mockImplementation(
    (path: string, opts?: { params?: { path?: { buildingId?: string }; query?: Record<string, unknown> } }) => {
      if (path === "/api/v1/buildings") {
        return Promise.resolve({ items: TOA_NHA, page: 1, size: 100, totalItems: 2, totalPages: 1 });
      }
      if (path === "/api/v1/buildings/{buildingId}") {
        const id = opts?.params?.path?.buildingId;
        const toa = TOA_NHA.find((t) => t.maToaNha === id) ?? TOA_NHA[0];
        return Promise.resolve(toa);
      }
      if (path === "/api/v1/apartments") {
        const maToaNha = opts?.params?.query?.maToaNha;
        const items = maToaNha === "A" ? CAN_HO_TOA_A : [];
        return Promise.resolve({ items, page: 1, size: 50, totalItems: items.length, totalPages: 1 });
      }
      if (path === "/api/v1/residency-records/resident-options") {
        return Promise.resolve(LUA_CHON_CU_DAN);
      }
      if (path === "/api/v1/residency-records") {
        const q = opts?.params?.query;
        if (q?.size === 1) {
          return Promise.resolve({
            items: soChoXacMinh > 0 ? [HO_SO_CHO_XAC_MINH] : [],
            page: 1,
            size: 1,
            totalItems: soChoXacMinh,
            totalPages: soChoXacMinh,
          });
        }
        return Promise.resolve({
          items: danhSach,
          page: 1,
          size: 20,
          totalItems: danhSach.length,
          totalPages: 1,
        });
      }
      return Promise.reject(new Error(`Unexpected GET ${path}`));
    },
  );
  const queryClient = createQueryClient();
  const view = render(
    <QueryClientProvider client={queryClient}>
      <HoSoCuTruView />
    </QueryClientProvider>,
  );
  return { ...view, queryClient };
}

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  put.mockReset();
  notify.success.mockReset();
  mockPush.mockReset();
  mockReplace.mockReset();
});

describe("Màn hình /ho-so-cu-tru (spec 0015, AC-16, AC-17, AC-18)", () => {
  it("hiện số đếm chờ xác minh, cột Tạo lúc mặc định aria-sort descending, đủ 9 cột và hành động theo trạng thái; axe sạch", async () => {
    const { container } = moManHinh();

    expect(await screen.findByText("Đặng Tạm Trú")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Tất cả hồ sơ" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "Chờ xác minh (2)" })).toHaveAttribute("aria-pressed", "false");

    const cotTaoLuc = screen.getByRole("columnheader", { name: /Tạo lúc/ });
    expect(cotTaoLuc).toHaveAttribute("aria-sort", "descending");
    expect(screen.getAllByRole("columnheader")).toHaveLength(9);

    // Dòng Chờ xác minh có Duyệt + Từ chối
    expect(screen.getByRole("button", { name: "Duyệt đề nghị cư trú của Đặng Tạm Trú" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Từ chối đề nghị cư trú của Đặng Tạm Trú" })).toBeInTheDocument();

    // Dòng Hợp lệ đang ở có Sửa + Kết thúc cư trú
    expect(screen.getByRole("button", { name: "Sửa hồ sơ cư trú của Vũ Chủ Hộ" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Kết thúc cư trú của Vũ Chủ Hộ" })).toBeInTheDocument();

    // Dòng Hợp lệ sắp bắt đầu có Sửa + Hủy hồ sơ
    expect(screen.getByRole("button", { name: "Sửa hồ sơ cư trú của Lê Sắp Chuyển Đến" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Hủy hồ sơ cư trú của Lê Sắp Chuyển Đến" })).toBeInTheDocument();

    // Dòng Hợp lệ đã qua ngayKetThuc ẩn Sửa nhưng vẫn có Kết thúc cư trú
    expect(screen.queryByRole("button", { name: "Sửa hồ sơ cư trú của Hoàng Quá Hạn" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Kết thúc cư trú của Hoàng Quá Hạn" })).toBeInTheDocument();

    // Dòng Từ chối hiện lý do và không có nút thao tác ghi
    expect(screen.getByText(/Chưa bổ sung hợp đồng thuê/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Phạm Bị Từ Chối/ })).not.toBeInTheDocument();

    await expectNoA11yViolations(container);
  });

  it("thanh lọc nhanh giữ nguyên các bộ lọc khác, đưa về trang 1 và ẩn số đếm khi bằng 0", async () => {
    const user = userEvent.setup();
    moManHinh("q=Mai&page=2", [HO_SO_CHO_XAC_MINH], 0);

    expect(await screen.findByRole("button", { name: "Chờ xác minh" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Chờ xác minh" }));
    expect(mockPush).toHaveBeenLastCalledWith("/ho-so-cu-tru?q=Mai&trangThaiHoSo=Ch%E1%BB%9D+x%C3%A1c+minh", {
      scroll: false,
    });
  });

  it("bấm cột mặc định Tạo lúc đảo từ descending sang ascending, và sắp xếp được trên Ngày bắt đầu", async () => {
    const user = userEvent.setup();
    moManHinh();
    await screen.findByText("Đặng Tạm Trú");

    const cotTaoLuc = screen.getByRole("columnheader", { name: /Tạo lúc/ });
    await user.click(within(cotTaoLuc).getByRole("button"));
    expect(mockReplace).toHaveBeenLastCalledWith("/ho-so-cu-tru?sort=taoLuc%2Casc", { scroll: false });

    const cotThoiGian = screen.getByRole("columnheader", { name: /Thời gian cư trú/ });
    await user.click(within(cotThoiGian).getByRole("button", { name: "Ngày bắt đầu" }));
    expect(mockReplace).toHaveBeenLastCalledWith("/ho-so-cu-tru?sort=ngayBatDau%2Casc", { scroll: false });
  });

  it(
    "hộp thoại Thêm hồ sơ cư trú trên /ho-so-cu-tru chọn tòa, tìm căn hộ và xóa căn khi đổi tòa; axe sạch",
    { timeout: 15000 },
    async () => {
      const user = userEvent.setup();
      post.mockResolvedValue(HO_SO_DANG_O);
      const { queryClient } = moManHinh("", []);
      const spyInvalidate = vi.spyOn(queryClient, "invalidateQueries");

      await user.click(await screen.findByRole("button", { name: "Thêm hồ sơ cư trú" }));
      const dialog = screen.getByRole("dialog", { name: "Thêm hồ sơ cư trú" });
      await expectNoA11yViolations(dialog);

      const oCanHo = within(dialog).getByRole("combobox", { name: /Căn hộ/ });
      expect(oCanHo).toBeDisabled();

      // Chọn Tòa A -> bật combobox Căn hộ và chọn A-0101
      await user.click(within(dialog).getByRole("combobox", { name: /Tòa nhà/ }));
      await user.click(await screen.findByRole("option", { name: "Tòa A" }));
      await waitFor(() => expect(within(dialog).getByRole("combobox", { name: /Căn hộ/ })).not.toBeDisabled());

      await user.click(within(dialog).getByRole("combobox", { name: /Căn hộ/ }));
      await user.click(await screen.findByRole("option", { name: /A-0101/ }));
      expect(within(dialog).getByRole("combobox", { name: /Căn hộ/ })).toHaveTextContent("A-0101");

      // Đổi sang Tòa B -> tự xóa căn hộ đang chọn, rồi chọn lại Tòa A
      await user.click(within(dialog).getByRole("combobox", { name: /Tòa nhà/ }));
      await user.click(await screen.findByRole("option", { name: "Tòa B" }));
      expect(within(dialog).getByRole("combobox", { name: /Căn hộ/ })).toHaveTextContent("Chọn căn hộ");

      await user.click(within(dialog).getByRole("combobox", { name: /Tòa nhà/ }));
      await user.click(await screen.findByRole("option", { name: "Tòa A" }));
      await user.click(within(dialog).getByRole("combobox", { name: /Căn hộ/ }));
      await user.click(await screen.findByRole("option", { name: /A-0101/ }));

      await user.click(within(dialog).getByRole("combobox", { name: /Cư dân/ }));
      await user.click(await screen.findByRole("option", { name: /Nguyễn Văn Mới/ }));
      await user.click(within(dialog).getByRole("combobox", { name: /Vai trò cư trú/ }));
      await user.click(await screen.findByRole("option", { name: "Chủ căn hộ" }));
      await user.click(within(dialog).getByRole("combobox", { name: /Tình trạng cư trú/ }));
      await user.click(await screen.findByRole("option", { name: "Thường trú" }));

      await user.click(within(dialog).getByRole("button", { name: "Thêm hồ sơ" }));
      await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã thêm hồ sơ cư trú"));
      expect(post).toHaveBeenCalledWith(
        "/api/v1/residency-records",
        expect.objectContaining({
          body: expect.objectContaining({ maCanHo: "A-0101", maCuDan: "CD00000005" }),
        }),
      );
      expect(spyInvalidate).toHaveBeenCalledWith({ queryKey: ["ho-so-cu-tru"] });
    },
  );

  it("hiện ErrorState kèm nút Xóa bộ lọc khi lọc tòa nhà ngoài phạm vi (404)", async () => {
    const user = userEvent.setup();
    mockPathname.mockReturnValue("/ho-so-cu-tru");
    mockSearchParams.mockReturnValue(new URLSearchParams("maToaNha=Z"));
    get.mockImplementation((path: string) => {
      if (path === "/api/v1/buildings") {
        return Promise.resolve({ items: TOA_NHA, page: 1, size: 100, totalItems: 2, totalPages: 1 });
      }
      if (path === "/api/v1/buildings/{buildingId}" || path === "/api/v1/residency-records") {
        return Promise.reject(new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "x", errors: [] }));
      }
      return Promise.resolve({ items: [], page: 1, size: 1, totalItems: 0, totalPages: 0 });
    });

    render(
      <QueryClientProvider client={createQueryClient()}>
        <HoSoCuTruView />
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("heading", { name: "Không tìm thấy tòa nhà" })).toBeInTheDocument();
    await user.click(screen.getAllByRole("button", { name: "Xóa bộ lọc" })[0]);
    expect(mockPush).toHaveBeenLastCalledWith("/ho-so-cu-tru", { scroll: false });
  });
});
