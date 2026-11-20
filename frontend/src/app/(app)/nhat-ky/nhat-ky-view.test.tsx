import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { mockPathname, mockReplace, mockSearchParams } from "@/test/next-navigation";
import { NhatKyView, tomTatChiTiet, type NhatKy } from "./nhat-ky-view";

const get = vi.hoisted(() => vi.fn());

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get }, api: (p: Promise<unknown>) => p };
});

const LOAI = [
  { ma: "AUTH_DANG_NHAP_THANH_CONG", nhan: "Đăng nhập thành công" },
  { ma: "AUTH_DANG_NHAP_THAT_BAI", nhan: "Đăng nhập thất bại" },
];

const DONG: NhatKy[] = [
  {
    maLog: "LOG00000002",
    maNguoiDung: "ND00000001",
    hoTen: "Nguyễn Quản Trị",
    loaiThaoTac: "AUTH_DANG_NHAP_THAT_BAI",
    tenThaoTac: "Đăng nhập thất bại",
    doiTuongTacDong: "NguoiDungHeThong:ND00000001",
    chiTietThayDoi: { lyDo: "SAI_THONG_TIN" },
    diaChiIp: "10.0.0.1",
    requestId: "req-1",
    thoiGianGhi: "2026-03-10T01:05:00Z",
  },
  {
    maLog: "LOG00000001",
    maNguoiDung: null,
    hoTen: null,
    loaiThaoTac: "AUTH_DANG_NHAP_THAT_BAI",
    tenThaoTac: "Đăng nhập thất bại",
    doiTuongTacDong: "NguoiDungHeThong",
    chiTietThayDoi: null,
    diaChiIp: null,
    requestId: null,
    thoiGianGhi: "2026-03-10T01:00:00Z",
  },
];

function trang(items: NhatKy[]) {
  return { items, page: 1, size: 20, totalItems: items.length, totalPages: items.length ? 1 : 0 };
}

function traVe(danhSach: () => Promise<unknown>) {
  get.mockImplementation((path: string) => (path.endsWith("/action-types") ? Promise.resolve(LOAI) : danhSach()));
}

function hienThi(url = "") {
  mockSearchParams.mockReturnValue(new URLSearchParams(url));
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <NhatKyView />
    </QueryClientProvider>,
  );
}

describe("Trang Nhật ký hệ thống (spec 0007, AC-11, AC-12, AC-14)", () => {
  beforeEach(() => {
    get.mockReset();
    mockReplace.mockReset();
    mockPathname.mockReturnValue("/nhat-ky");
  });

  it("hiện bảng mới nhất trước, giờ Việt Nam, người không xác định", async () => {
    traVe(() => Promise.resolve(trang(DONG)));
    const { container } = hienThi();
    expect(await screen.findByRole("cell", { name: /Nguyễn Quản Trị/ })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Nhật ký hệ thống" })).toBeInTheDocument();
    const hang = screen.getAllByRole("row").slice(1);
    expect(within(hang[0]).getByText("10/03/2026 08:05")).toBeInTheDocument();
    expect(within(hang[0]).getByText("lyDo: SAI_THONG_TIN")).toBeInTheDocument();
    expect(within(hang[1]).getByText("Không xác định")).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/api/v1/system/audit-logs", {
      params: { query: { page: 1, size: 20, sort: undefined } },
    });
    await expectNoA11yViolations(container);
  });

  it("gửi bộ lọc trên URL kèm trang và sắp xếp", async () => {
    traVe(() => Promise.resolve(trang(DONG)));
    hienThi("maNguoiDung=ND00000001&loaiThaoTac=AUTH_DANG_NHAP_THAT_BAI&tuNgay=2026-03-10&denNgay=2026-03-10&page=2");
    await screen.findByRole("cell", { name: /Nguyễn Quản Trị/ });
    expect(get).toHaveBeenCalledWith("/api/v1/system/audit-logs", {
      params: {
        query: {
          page: 2,
          size: 20,
          sort: undefined,
          maNguoiDung: "ND00000001",
          loaiThaoTac: "AUTH_DANG_NHAP_THAT_BAI",
          tuNgay: "2026-03-10",
          denNgay: "2026-03-10",
        },
      },
    });
    expect(screen.getByLabelText("Mã người dùng")).toHaveValue("ND00000001");
  });

  it("áp dụng bộ lọc thì ghi lên URL và về trang 1", async () => {
    traVe(() => Promise.resolve(trang(DONG)));
    const user = userEvent.setup();
    hienThi("page=3");
    await user.type(screen.getByLabelText("Mã người dùng"), "ND00000007");
    await user.type(screen.getByLabelText("Từ ngày"), "2026-03-01");
    await user.click(screen.getByRole("button", { name: "Lọc" }));
    expect(mockReplace).toHaveBeenCalledWith("/nhat-ky?maNguoiDung=ND00000007&tuNgay=2026-03-01", {
      scroll: false,
    });
  });

  it("báo lỗi dưới ô và không đổi URL khi bộ lọc sai", async () => {
    traVe(() => Promise.resolve(trang(DONG)));
    const user = userEvent.setup();
    const { container } = hienThi();
    await user.type(screen.getByLabelText("Mã người dùng"), "nd1");
    await user.type(screen.getByLabelText("Từ ngày"), "2026-03-11");
    await user.type(screen.getByLabelText("Đến ngày"), "2026-03-10");
    await user.click(screen.getByRole("button", { name: "Lọc" }));
    expect(await screen.findByText("Mã người dùng không đúng định dạng")).toBeInTheDocument();
    expect(screen.getByText("Đến ngày phải bằng hoặc sau từ ngày")).toBeInTheDocument();
    expect(screen.getByLabelText("Mã người dùng")).toHaveAttribute("aria-invalid", "true");
    expect(mockReplace).not.toHaveBeenCalled();
    await expectNoA11yViolations(container);
  });

  it("rỗng khi có bộ lọc thì gợi ý nới điều kiện, và có nút xóa bộ lọc", async () => {
    traVe(() => Promise.resolve(trang([])));
    const user = userEvent.setup();
    hienThi("loaiThaoTac=AUTH_DANG_NHAP_THANH_CONG");
    expect(await screen.findByText("Không có dòng nhật ký nào khớp bộ lọc")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Xóa bộ lọc" }));
    expect(mockReplace).toHaveBeenCalledWith("/nhat-ky", { scroll: false });
  });

  it("rỗng khi không lọc", async () => {
    traVe(() => Promise.resolve(trang([])));
    hienThi();
    expect(await screen.findByText("Chưa có dòng nhật ký nào")).toBeInTheDocument();
  });

  it("bộ lọc sửa tay bị backend từ chối thì nêu lỗi từng tham số", async () => {
    traVe(() =>
      Promise.reject(
        new ApiError({
          status: 400,
          errorCode: "VALIDATION_FAILED",
          message: "Dữ liệu không hợp lệ",
          errors: [{ field: "tuNgay", message: "Ngày phải có dạng yyyy-MM-dd" }],
        }),
      ),
    );
    hienThi("tuNgay=hom-qua");
    expect(await screen.findByText("Bộ lọc trên địa chỉ trang không hợp lệ")).toBeInTheDocument();
    expect(screen.getByText("Ngày phải có dạng yyyy-MM-dd")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("lỗi khác thì hiện trạng thái lỗi có Thử lại", async () => {
    traVe(() => Promise.reject(new ApiError({ status: 409, errorCode: "CONFLICT", message: "Xung đột dữ liệu" })));
    hienThi();
    expect(await screen.findByRole("button", { name: "Thử lại" })).toBeInTheDocument();
  });
});

describe("tomTatChiTiet", () => {
  it("gộp cặp khóa và giá trị, giữ JSON cho giá trị lồng", () => {
    expect(tomTatChiTiet({ lyDo: "SAI_THONG_TIN" })).toBe("lyDo: SAI_THONG_TIN");
    expect(tomTatChiTiet({ truoc: { a: 1 }, n: 2 })).toBe('truoc: {"a":1}, n: 2');
    expect(tomTatChiTiet(null)).toBe("—");
    expect(tomTatChiTiet({})).toBe("—");
  });
});
