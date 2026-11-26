import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { ngayHomNay } from "@/lib/format";
import { expectNoA11yViolations } from "@/test/axe";
import { ChiTietCanHo } from "./chi-tiet-can-ho";

const get = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn());
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
  return { ...actual, client: { GET: get, POST: post, PUT: put, PATCH: patch }, api: (p: Promise<unknown>) => p };
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

const DANG_CU_TRU_BQL = {
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
};

const SAP_BAT_DAU_BQL = {
  ...DANG_CU_TRU_BQL,
  maHoSo: "HS00000002",
  maCuDan: "CD00000002",
  hoTen: "Lê Khách Thuê",
  soDienThoai: "0900000009",
  soCccd: "001026000009",
  vaiTroCuTru: "Khách thuê",
  tinhTrangCuTru: "Tạm trú",
  ngayBatDau: "2099-05-01",
  ngayKetThuc: "2099-12-31",
};

const LICH_SU_HET_HAN = {
  ...DANG_CU_TRU_BQL,
  maHoSo: "HS00000003",
  maCuDan: "CD00000003",
  hoTen: "Phạm Cư Dân Cũ",
  vaiTroCuTru: "Khách thuê",
  tinhTrangCuTru: "Tạm trú",
  ngayBatDau: "2025-01-01",
  ngayKetThuc: "2025-12-31",
  trangThaiHoSo: "Hết hiệu lực",
};

const LICH_SU_QUA_HAN_CHUA_QUET = {
  ...LICH_SU_HET_HAN,
  maHoSo: "HS00000004",
  hoTen: "Hoàng Quá Hạn",
  trangThaiHoSo: "Hợp lệ",
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
  {
    maCuDan: "CD00000006",
    maNguoiDung: "ND00000010",
    hoTen: "Đặng Tạm Khóa",
    soDienThoai: "0900000010",
    email: "tamkhoa@chungcu.test",
    soCccd: "001026000010",
    trangThaiTaiKhoan: "Tạm khóa",
  },
];

function moTrang(
  vaiTro: string[],
  choPhepSua: boolean,
  cuTruImpl: () => Promise<unknown> = () =>
    Promise.resolve({
      dangCuTru: [DANG_CU_TRU_BQL],
      sapBatDau: [SAP_BAT_DAU_BQL],
      lichSuCuTru: [LICH_SU_QUA_HAN_CHUA_QUET, LICH_SU_HET_HAN],
      deNghiCuTru: [],
    }),
  canHoData = CAN_HO,
) {
  hoSo.vaiTro = vaiTro;
  get.mockImplementation((path: string, opts?: { params?: { query?: { q?: string } } }) => {
    if (path === "/api/v1/apartments/{apartmentId}") return Promise.resolve(canHoData);
    if (path === "/api/v1/apartments/{apartmentId}/residents") return cuTruImpl();
    if (path === "/api/v1/residency-records/resident-options") {
      const q = opts?.params?.query?.q?.toLowerCase() ?? "";
      return Promise.resolve(LUA_CHON_CU_DAN.filter((c) => !q || c.hoTen.toLowerCase().includes(q)));
    }
    return Promise.reject(new Error(`Unexpected GET ${path}`));
  });
  const queryClient = createQueryClient();
  const view = render(
    <QueryClientProvider client={queryClient}>
      <ChiTietCanHo maCanHo="A-0101" choPhepSua={choPhepSua} />
    </QueryClientProvider>,
  );
  return { ...view, queryClient };
}

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  put.mockReset();
  patch.mockReset();
  notify.success.mockReset();
});

describe("Khối Hồ sơ cư trú trên ChiTietCanHo (spec 0014, AC-15, AC-16, AC-18)", () => {
  it("ẩn khối và không gọi API nhân khẩu với Kế toán hoặc BQT dù kiêm vai trò Cư dân", async () => {
    moTrang(["KE_TOAN", "CU_DAN"], true);
    expect(await screen.findByRole("heading", { name: "Căn hộ A-0101" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Hồ sơ cư trú" })).not.toBeInTheDocument();
    expect(get).not.toHaveBeenCalledWith("/api/v1/apartments/{apartmentId}/residents", expect.anything());
  });

  it("lỗi tải nhân khẩu hiện nút Thử lại độc lập mà vẫn giữ nguyên thông tin căn hộ", async () => {
    const user = userEvent.setup();
    let lanGoi = 0;
    moTrang(["BAN_QUAN_LY"], true, () => {
      lanGoi += 1;
      // QueryClient thử lại lỗi 5xx đúng 1 lần, nên 2 lần đầu trả lỗi để vào trạng thái lỗi
      if (lanGoi <= 2) {
        return Promise.reject(new ApiError({ status: 500, errorCode: "INTERNAL_ERROR", message: "x", errors: [] }));
      }
      return Promise.resolve({ dangCuTru: [], sapBatDau: [], lichSuCuTru: [] });
    });

    expect(
      await screen.findByRole("heading", { name: "Không tải được hồ sơ cư trú" }, { timeout: 3000 }),
    ).toBeInTheDocument();
    expect(screen.getByText("Tòa A (A)")).toBeInTheDocument();
    expect(screen.getByText("75,50 m²")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(await screen.findByText("Căn hộ chưa có người đang cư trú")).toBeInTheDocument();
  });

  it("BQL thấy đủ hai tab, mục Sắp bắt đầu, PII, người xác minh và ẩn đúng nút ở tab Lịch sử; axe sạch", async () => {
    const user = userEvent.setup();
    const { container } = moTrang(["BAN_QUAN_LY"], true);

    expect(await screen.findByText("Vũ Cư Dân")).toBeInTheDocument();
    expect(screen.getByText("0900000006")).toBeInTheDocument();
    expect(screen.getByText("001026000006")).toBeInTheDocument();
    expect(screen.getByText("Không thời hạn")).toBeInTheDocument();
    expect(screen.getAllByText("Trần Ban Quản Lý")).toHaveLength(2);
    expect(screen.getByRole("heading", { name: "Sắp bắt đầu" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Hủy hồ sơ cư trú của Lê Khách Thuê" })).toBeInTheDocument();
    await expectNoA11yViolations(container);

    await user.click(screen.getByRole("tab", { name: "Lịch sử cư trú" }));
    expect(await screen.findByText("Phạm Cư Dân Cũ")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Sửa hồ sơ cư trú/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Kết thúc cư trú của Phạm Cư Dân Cũ" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Kết thúc cư trú của Hoàng Quá Hạn" })).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("Cư dân trên /can-ho-cua-toi không thấy cột PII, không có nút thao tác và có chú thích ở Lịch sử; axe sạch", async () => {
    const user = userEvent.setup();
    const dangCuTruCuDan = {
      ...DANG_CU_TRU_BQL,
      maNguoiDung: null,
      soDienThoai: null,
      email: null,
      soCccd: null,
      maNvbqlXacMinh: null,
      hoTenNguoiXacMinh: null,
    };
    const { container } = moTrang(["CU_DAN"], false, () =>
      Promise.resolve({ dangCuTru: [dangCuTruCuDan], sapBatDau: [], lichSuCuTru: [] }),
    );

    expect(await screen.findByText("Vũ Cư Dân")).toBeInTheDocument();
    expect(screen.queryByText("Số CCCD")).not.toBeInTheDocument();
    expect(screen.queryByText("Số điện thoại")).not.toBeInTheDocument();
    expect(screen.queryByText("Người xác minh")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Thêm hồ sơ cư trú" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Sửa hồ sơ cư trú/ })).not.toBeInTheDocument();

    await user.click(screen.getByRole("tab", { name: "Lịch sử cư trú" }));
    expect(await screen.findByText("Chỉ hiển thị lịch sử cư trú của bạn.")).toBeInTheDocument();
    expect(screen.getByText("Chưa có lịch sử cư trú")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });
});

describe("Thao tác BQL trên Hồ sơ cư trú và đồng bộ căn hộ (spec 0014, AC-17, AC-18)", () => {
  it("Thêm hồ sơ cư trú: tải 20 cư dân đầu, hiện badge Tạm khóa, bắt ngày kết thúc Tạm trú, gửi POST và làm mới cache", async () => {
    const user = userEvent.setup();
    post.mockResolvedValue(DANG_CU_TRU_BQL);
    const { queryClient } = moTrang(["BAN_QUAN_LY"], true, () =>
      Promise.resolve({ dangCuTru: [], sapBatDau: [], lichSuCuTru: [] }),
    );
    const spyInvalidate = vi.spyOn(queryClient, "invalidateQueries");

    const nutMo = await screen.findByRole("button", { name: "Thêm hồ sơ cư trú" });
    await user.click(nutMo);
    const dialog = screen.getByRole("dialog", { name: "Thêm hồ sơ cư trú căn hộ A-0101" });
    await expectNoA11yViolations(dialog);

    await user.click(within(dialog).getByRole("combobox", { name: /Cư dân/ }));
    expect(await screen.findByText("Tạm khóa")).toBeInTheDocument();
    await user.click(screen.getByRole("option", { name: /Nguyễn Văn Mới/ }));

    await user.click(within(dialog).getByRole("combobox", { name: /Vai trò cư trú/ }));
    await user.click(await screen.findByRole("option", { name: "Chủ căn hộ" }));

    await user.click(within(dialog).getByRole("combobox", { name: /Tình trạng cư trú/ }));
    await user.click(await screen.findByRole("option", { name: "Tạm trú" }));

    const ngayBatDau = within(dialog).getByLabelText(/Ngày bắt đầu/);
    expect(ngayBatDau).toHaveValue(ngayHomNay());

    await user.click(within(dialog).getByRole("button", { name: "Thêm hồ sơ" }));
    const ngayKetThuc = within(dialog).getByLabelText(/Ngày kết thúc/);
    await waitFor(() => expect(ngayKetThuc).toHaveAccessibleDescription(/Tạm trú bắt buộc có ngày kết thúc/));
    expect(post).not.toHaveBeenCalled();

    await user.type(ngayKetThuc, "2027-12-31");
    await user.click(within(dialog).getByRole("button", { name: "Thêm hồ sơ" }));

    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã thêm hồ sơ cư trú"));
    expect(post).toHaveBeenCalledWith("/api/v1/residency-records", {
      body: {
        maCanHo: "A-0101",
        maCuDan: "CD00000005",
        vaiTroCuTru: "Chủ căn hộ",
        tinhTrangCuTru: "Tạm trú",
        ngayBatDau: ngayHomNay(),
        ngayKetThuc: "2027-12-31",
      },
    });
    expect(spyInvalidate).toHaveBeenCalledWith({ queryKey: ["ho-so-cu-tru"] });
    expect(spyInvalidate).toHaveBeenCalledWith({ queryKey: ["can-ho"] });
    expect(spyInvalidate).toHaveBeenCalledWith({ queryKey: ["ho-so"] });
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(nutMo).toHaveFocus();
  });

  it("Thêm hồ sơ cư trú hiện lỗi 409 trùng khoảng ngày và 422 tài khoản ngừng hoạt động trong FormAlert", async () => {
    const user = userEvent.setup();
    post.mockRejectedValueOnce(
      new ApiError({
        status: 409,
        errorCode: "CUTRU_DA_CO_CHU_CAN_HO",
        message: "x",
        errors: [],
      }),
    );
    moTrang(["BAN_QUAN_LY"], true, () => Promise.resolve({ dangCuTru: [], sapBatDau: [], lichSuCuTru: [] }));

    await user.click(await screen.findByRole("button", { name: "Thêm hồ sơ cư trú" }));
    const dialog = screen.getByRole("dialog", { name: "Thêm hồ sơ cư trú căn hộ A-0101" });
    await user.click(within(dialog).getByRole("combobox", { name: /Cư dân/ }));
    await user.click(await screen.findByRole("option", { name: /Nguyễn Văn Mới/ }));
    await user.click(within(dialog).getByRole("combobox", { name: /Vai trò cư trú/ }));
    await user.click(await screen.findByRole("option", { name: "Chủ căn hộ" }));
    await user.click(within(dialog).getByRole("combobox", { name: /Tình trạng cư trú/ }));
    await user.click(await screen.findByRole("option", { name: "Thường trú" }));

    await user.click(within(dialog).getByRole("button", { name: "Thêm hồ sơ" }));
    expect(
      await within(dialog).findByText("Căn hộ đã có chủ căn hộ hợp lệ trong khoảng ngày này."),
    ).toBeInTheDocument();

    post.mockRejectedValueOnce(
      new ApiError({
        status: 422,
        errorCode: "CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG",
        message: "x",
        errors: [],
      }),
    );
    await user.click(within(dialog).getByRole("button", { name: "Thêm hồ sơ" }));
    expect(
      await within(dialog).findByText(
        "Tài khoản cư dân đã ngừng hoạt động, không thể ghi nhận hoặc duyệt hồ sơ cư trú.",
      ),
    ).toBeInTheDocument();
  });

  it("Sửa, Kết thúc cư trú và Hủy hồ sơ sắp bắt đầu gửi đúng payload PUT", async () => {
    const user = userEvent.setup();
    put.mockResolvedValue(DANG_CU_TRU_BQL);
    moTrang(["BAN_QUAN_LY"], true);

    await user.click(await screen.findByRole("button", { name: "Sửa hồ sơ cư trú của Vũ Cư Dân" }));
    const suaDialog = screen.getByRole("dialog", { name: "Sửa hồ sơ cư trú" });
    await expectNoA11yViolations(suaDialog);
    await user.click(within(suaDialog).getByRole("button", { name: "Lưu thay đổi" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã cập nhật hồ sơ cư trú"));
    expect(put).toHaveBeenCalledWith("/api/v1/residency-records/{recordId}", {
      params: { path: { recordId: "HS00000001" } },
      body: {
        vaiTroCuTru: "Chủ căn hộ",
        tinhTrangCuTru: "Thường trú",
        ngayBatDau: "2026-01-01",
        ngayKetThuc: null,
        trangThaiHoSo: "Hợp lệ",
      },
    });

    await user.click(screen.getByRole("button", { name: "Kết thúc cư trú của Vũ Cư Dân" }));
    const ketThucDialog = screen.getByRole("dialog", { name: "Kết thúc hồ sơ cư trú" });
    const oKetThuc = within(ketThucDialog).getByLabelText(/Ngày kết thúc/);
    expect(oKetThuc).toHaveValue(ngayHomNay());
    expect(oKetThuc).not.toHaveAttribute("readonly");
    await user.click(within(ketThucDialog).getByRole("button", { name: "Kết thúc cư trú" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã kết thúc hồ sơ cư trú"));
    expect(put).toHaveBeenLastCalledWith("/api/v1/residency-records/{recordId}", {
      params: { path: { recordId: "HS00000001" } },
      body: {
        vaiTroCuTru: "Chủ căn hộ",
        tinhTrangCuTru: "Thường trú",
        ngayBatDau: "2026-01-01",
        ngayKetThuc: ngayHomNay(),
        trangThaiHoSo: "Hết hiệu lực",
      },
    });

    await user.click(screen.getByRole("button", { name: "Hủy hồ sơ cư trú của Lê Khách Thuê" }));
    const huyDialog = screen.getByRole("dialog", { name: "Hủy hồ sơ cư trú" });
    const oNgayHuy = within(huyDialog).getByLabelText(/Ngày kết thúc/);
    expect(oNgayHuy).toHaveValue("2099-05-01");
    expect(oNgayHuy).toHaveAttribute("readonly");
    await user.click(within(huyDialog).getByRole("button", { name: "Hủy hồ sơ" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã hủy hồ sơ cư trú"));
    expect(put).toHaveBeenLastCalledWith("/api/v1/residency-records/{recordId}", {
      params: { path: { recordId: "HS00000002" } },
      body: {
        vaiTroCuTru: "Khách thuê",
        tinhTrangCuTru: "Tạm trú",
        ngayBatDau: "2099-05-01",
        ngayKetThuc: "2099-05-01",
        trangThaiHoSo: "Hết hiệu lực",
      },
    });
  });

  it("SuaCanHoDialog gắn lỗi 422 mâu thuẫn cư trú ngay dưới ô trạng thái sử dụng", async () => {
    const user = userEvent.setup();
    put.mockRejectedValueOnce(
      new ApiError({
        status: 422,
        errorCode: "CANHO_TRANG_THAI_MAU_THUAN_CU_TRU",
        message: "Trạng thái sử dụng không khớp với hồ sơ cư trú hiện tại",
        errors: [
          {
            field: "trangThaiSuDung",
            message: "Căn hộ đang có người cư trú, không thể đặt trạng thái Trống hoặc Đã bàn giao",
          },
        ],
      }),
    );
    moTrang(["BAN_QUAN_LY"], true);

    await user.click(await screen.findByRole("button", { name: "Sửa" }));
    const dialog = screen.getByRole("dialog", { name: "Sửa căn hộ A-0101" });
    await user.click(within(dialog).getByRole("button", { name: "Lưu thay đổi" }));

    const oTrangThai = within(dialog).getByRole("combobox", { name: /Trạng thái sử dụng/ });
    await waitFor(() =>
      expect(oTrangThai).toHaveAccessibleDescription(/Căn hộ đang có người cư trú, không thể đặt trạng thái Trống/),
    );
  });
});

describe("Đề nghị cư trú trên ChiTietCanHo (spec 0015, AC-15, AC-17, AC-18)", () => {
  const DE_NGHI_CHO_CU_DAN = {
    maHoSo: "HS00000010",
    maCanHo: "A-0101",
    maCuDan: "CD00000006",
    maNguoiDung: null,
    hoTen: "Đặng Tạm Trú",
    soDienThoai: null,
    email: null,
    soCccd: null,
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
    ghiChuDeNghi: "Hợp đồng thuê 6 tháng",
    lyDoTuChoi: null,
    coTheHuy: true,
  };

  const DE_NGHI_TU_CHOI_CU_DAN = {
    ...DE_NGHI_CHO_CU_DAN,
    maHoSo: "HS00000011",
    hoTen: "Phạm Người Thân",
    vaiTroCuTru: "Thành viên gia đình",
    trangThaiHoSo: "Từ chối",
    lyDoTuChoi: "Chưa nộp bản sao giấy khai sinh",
    coTheHuy: false,
  };

  it(
    "Cư dân gửi đề nghị (ẩn/hiện CCCD và SĐT theo đối tượng), xem lý do từ chối và tự hủy đề nghị; axe sạch",
    { timeout: 15000 },
    async () => {
      const user = userEvent.setup();
      post.mockResolvedValue(DE_NGHI_CHO_CU_DAN);
      const { container, queryClient } = moTrang(["CU_DAN"], false, () =>
        Promise.resolve({
          dangCuTru: [],
          sapBatDau: [],
          lichSuCuTru: [],
          deNghiCuTru: [DE_NGHI_CHO_CU_DAN, DE_NGHI_TU_CHOI_CU_DAN],
        }),
      );
      const spyInvalidate = vi.spyOn(queryClient, "invalidateQueries");

      expect(await screen.findByRole("tab", { name: "Đề nghị cư trú (1)" })).toBeInTheDocument();
      const nutGui = screen.getByRole("button", { name: "Gửi đề nghị cư trú" });
      await user.click(nutGui);

      const dialog = screen.getByRole("dialog", { name: "Gửi đề nghị cư trú căn hộ A-0101" });
      await expectNoA11yViolations(dialog);

      // Mặc định "Người khác" hiển thị Số CCCD và Số điện thoại
      expect(within(dialog).getByRole("radio", { name: /Người khác/ })).toBeChecked();
      expect(within(dialog).getByLabelText(/Số CCCD/)).toBeInTheDocument();
      expect(within(dialog).getByLabelText(/Số điện thoại/)).toBeInTheDocument();

      // Lỗi 422 không khớp cư dân hiện trong FormAlert
      post.mockRejectedValueOnce(
        new ApiError({ status: 422, errorCode: "CUTRU_KHONG_KHOP_CU_DAN", message: "raw", errors: [] }),
      );
      await user.type(within(dialog).getByLabelText(/Số CCCD/), "001026000010");
      await user.type(within(dialog).getByLabelText(/Số điện thoại/), "0900000010");
      await user.click(within(dialog).getByRole("combobox", { name: /Vai trò cư trú/ }));
      await user.click(await screen.findByRole("option", { name: "Thành viên gia đình" }));
      await user.click(within(dialog).getByRole("combobox", { name: /Tình trạng cư trú/ }));
      await user.click(await screen.findByRole("option", { name: "Thường trú" }));
      await user.click(within(dialog).getByRole("button", { name: "Gửi đề nghị" }));
      expect(
        await within(dialog).findByText(
          "Không tìm thấy tài khoản cư dân khớp đồng thời số CCCD và số điện thoại đã nhập.",
        ),
      ).toBeInTheDocument();

      // Đổi sang "Chính tôi": ẩn CCCD và SĐT, gửi boolean laChinhToi = true
      await user.click(within(dialog).getByRole("radio", { name: /Chính tôi/ }));
      expect(within(dialog).queryByLabelText(/Số CCCD/)).not.toBeInTheDocument();
      expect(within(dialog).queryByLabelText(/Số điện thoại/)).not.toBeInTheDocument();
      await user.type(within(dialog).getByLabelText(/Ghi chú đề nghị/), "Gia hạn đợt ở mới");
      await user.click(within(dialog).getByRole("button", { name: "Gửi đề nghị" }));

      await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã gửi đề nghị cư trú"));
      expect(post).toHaveBeenLastCalledWith("/api/v1/residency-records/resident-request", {
        body: {
          maCanHo: "A-0101",
          laChinhToi: true,
          vaiTroCuTru: "Thành viên gia đình",
          tinhTrangCuTru: "Thường trú",
          ngayBatDau: ngayHomNay(),
          ngayKetThuc: null,
          ghiChuDeNghi: "Gia hạn đợt ở mới",
        },
      });
      expect(spyInvalidate).toHaveBeenCalledWith({ queryKey: ["ho-so-cu-tru"] });

      // Tự chuyển sang tab Đề nghị cư trú, không hiện Người xác minh, hiện lý do từ chối và nút Hủy đề nghị
      expect(await screen.findByText("Đặng Tạm Trú")).toBeInTheDocument();
      expect(screen.getByText(/Chưa nộp bản sao giấy khai sinh/)).toBeInTheDocument();
      expect(screen.queryByText("Người xác minh")).not.toBeInTheDocument();
      expect(screen.queryByRole("button", { name: "Hủy đề nghị cư trú của Phạm Người Thân" })).not.toBeInTheDocument();
      await expectNoA11yViolations(container);

      await user.click(screen.getByRole("button", { name: "Hủy đề nghị cư trú của Đặng Tạm Trú" }));
      const huyDialog = screen.getByRole("alertdialog", { name: "Hủy đề nghị cư trú này?" });
      await user.click(within(huyDialog).getByRole("button", { name: "Hủy đề nghị" }));
      await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã hủy đề nghị cư trú"));
      expect(post).toHaveBeenLastCalledWith("/api/v1/residency-records/{recordId}/cancel", {
        params: { path: { recordId: "HS00000010" } },
      });
    },
  );

  it(
    "hộp thoại gửi đề nghị chặn dữ liệu sai ở client, gắn lỗi 400 của server vào ô nhập và trả focus khi Escape",
    { timeout: 15000 },
    async () => {
      // covers: AC-2, AC-15, AC-18
      const user = userEvent.setup();
      moTrang(["CU_DAN"], false, () =>
        Promise.resolve({ dangCuTru: [], sapBatDau: [], lichSuCuTru: [], deNghiCuTru: [] }),
      );

      const nutGui = await screen.findByRole("button", { name: "Gửi đề nghị cư trú" });
      await user.click(nutGui);
      const dialog = screen.getByRole("dialog", { name: "Gửi đề nghị cư trú căn hộ A-0101" });
      expect(within(dialog).getByLabelText(/Ngày bắt đầu/)).toHaveValue(ngayHomNay());

      // Gửi trống: lỗi gắn vào từng ô qua aria-describedby, không gọi API
      await user.click(within(dialog).getByRole("button", { name: "Gửi đề nghị" }));
      await waitFor(() =>
        expect(within(dialog).getByLabelText(/Số CCCD/)).toHaveAccessibleDescription(/Vui lòng nhập số CCCD/),
      );
      expect(within(dialog).getByLabelText(/Số điện thoại/)).toHaveAccessibleDescription(/Vui lòng nhập số điện thoại/);
      expect(within(dialog).getByRole("combobox", { name: /Vai trò cư trú/ })).toHaveAccessibleDescription(
        /Vui lòng chọn vai trò cư trú/,
      );
      expect(post).not.toHaveBeenCalled();

      // CCCD sai độ dài, SĐT có +84 và dấu chấm được chuẩn hóa, Tạm trú thiếu ngày kết thúc
      await user.type(within(dialog).getByLabelText(/Số CCCD/), "00102600001");
      await user.type(within(dialog).getByLabelText(/Số điện thoại/), "+84 900.000.010");
      await user.click(within(dialog).getByRole("combobox", { name: /Vai trò cư trú/ }));
      await user.click(await screen.findByRole("option", { name: "Khách thuê" }));
      await user.click(within(dialog).getByRole("combobox", { name: /Tình trạng cư trú/ }));
      await user.click(await screen.findByRole("option", { name: "Tạm trú" }));
      await user.click(within(dialog).getByRole("button", { name: "Gửi đề nghị" }));
      await waitFor(() =>
        expect(within(dialog).getByLabelText(/Số CCCD/)).toHaveAccessibleDescription(/Số CCCD phải gồm đúng 12 chữ số/),
      );
      expect(within(dialog).getByLabelText(/Số điện thoại/)).not.toHaveAccessibleDescription(/không hợp lệ/);
      expect(within(dialog).getByLabelText(/Ngày kết thúc/)).toHaveAccessibleDescription(
        /Tạm trú bắt buộc có ngày kết thúc/,
      );
      expect(post).not.toHaveBeenCalled();

      // Lỗi 400 của server gắn vào đúng ô nhập
      post.mockRejectedValueOnce(
        new ApiError({
          status: 400,
          errorCode: "VALIDATION_FAILED",
          message: "Dữ liệu không hợp lệ",
          errors: [{ field: "ngayKetThuc", message: "Ngày kết thúc phải từ ngày bắt đầu trở đi" }],
        }),
      );
      await user.type(within(dialog).getByLabelText(/Số CCCD/), "0");
      await user.type(within(dialog).getByLabelText(/Ngày kết thúc/), "2099-06-30");
      await user.click(within(dialog).getByRole("button", { name: "Gửi đề nghị" }));
      await waitFor(() =>
        expect(within(dialog).getByLabelText(/Ngày kết thúc/)).toHaveAccessibleDescription(
          /Ngày kết thúc phải từ ngày bắt đầu trở đi/,
        ),
      );
      expect(post).toHaveBeenCalledWith("/api/v1/residency-records/resident-request", {
        body: expect.objectContaining({
          laChinhToi: false,
          soCccd: "001026000010",
          soDienThoai: "+84 900.000.010",
          tinhTrangCuTru: "Tạm trú",
          ngayKetThuc: "2099-06-30",
          ghiChuDeNghi: null,
        }),
      });

      // Escape đóng hộp thoại và trả focus về nút mở
      await user.keyboard("{Escape}");
      await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
      expect(nutGui).toHaveFocus();
    },
  );

  it("BQL duyệt và từ chối đề nghị cư trú ngay trên tab Đề nghị cư trú; axe sạch", async () => {
    const user = userEvent.setup();
    const deNghiBql = {
      ...DE_NGHI_CHO_CU_DAN,
      soDienThoai: "0900000010",
      soCccd: "001026000010",
      coTheHuy: false,
    };
    post.mockResolvedValue({ ...deNghiBql, trangThaiHoSo: "Hợp lệ" });
    moTrang(["BAN_QUAN_LY"], true, () =>
      Promise.resolve({
        dangCuTru: [],
        sapBatDau: [],
        lichSuCuTru: [],
        deNghiCuTru: [deNghiBql],
      }),
    );

    await user.click(await screen.findByRole("tab", { name: "Đề nghị cư trú (1)" }));
    expect(await screen.findByText(/001026000010/)).toBeInTheDocument();

    // Duyệt đề nghị
    await user.click(screen.getByRole("button", { name: "Duyệt đề nghị cư trú của Đặng Tạm Trú" }));
    const duyetDialog = screen.getByRole("alertdialog", { name: "Duyệt đề nghị cư trú?" });
    expect(duyetDialog).toHaveTextContent("Hợp đồng thuê 6 tháng");
    await user.click(within(duyetDialog).getByRole("button", { name: "Duyệt đề nghị" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã duyệt đề nghị cư trú"));
    expect(post).toHaveBeenCalledWith("/api/v1/residency-records/{recordId}/approve", {
      params: { path: { recordId: "HS00000010" } },
    });

    // Từ chối đề nghị bắt buộc nhập lý do
    await user.click(screen.getByRole("button", { name: "Từ chối đề nghị cư trú của Đặng Tạm Trú" }));
    const tuChoiDialog = screen.getByRole("dialog", { name: "Từ chối đề nghị cư trú" });
    await expectNoA11yViolations(tuChoiDialog);
    await user.click(within(tuChoiDialog).getByRole("button", { name: "Từ chối đề nghị" }));
    const oLyDo = within(tuChoiDialog).getByLabelText(/Lý do từ chối/);
    await waitFor(() => expect(oLyDo).toHaveAccessibleDescription(/Vui lòng nhập lý do từ chối/));

    await user.type(oLyDo, "  Thiếu hợp đồng thuê có chữ ký chủ hộ  ");
    await user.click(within(tuChoiDialog).getByRole("button", { name: "Từ chối đề nghị" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã từ chối đề nghị cư trú"));
    expect(post).toHaveBeenLastCalledWith("/api/v1/residency-records/{recordId}/reject", {
      params: { path: { recordId: "HS00000010" } },
      body: { lyDoTuChoi: "Thiếu hợp đồng thuê có chữ ký chủ hộ" },
    });
  });
});
