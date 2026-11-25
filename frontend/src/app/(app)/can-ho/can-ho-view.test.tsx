import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { mockPathname, mockPush, mockSearchParams } from "@/test/next-navigation";
import { CanHoView } from "./can-ho-view";

const get = vi.hoisted(() => vi.fn());
const hoSo = vi.hoisted(() => ({ vaiTro: ["KE_TOAN"] as string[] }));

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({ data: { vaiTro: hoSo.vaiTro } }),
}));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get }, api: (p: Promise<unknown>) => p };
});

const TOA = { items: [{ maToaNha: "A", tenToaNha: "Tòa A", soTang: 3, diaChiKhuVuc: "HN", soCanHo: 12 }] };
const CAN_HO = {
  items: [
    {
      maCanHo: "A-0101",
      maTang: "A-01",
      soTang: 1,
      maToaNha: "A",
      tenToaNha: "Tòa A",
      dienTich: 75.5,
      trangThaiSuDung: "Đang sử dụng",
      biTamDungTienIch: true,
    },
  ],
  page: 1,
  size: 20,
  totalItems: 1,
  totalPages: 1,
};

function hienThi(url: string, vaiTro = ["KE_TOAN"], canHo: () => Promise<unknown> = () => Promise.resolve(CAN_HO)) {
  hoSo.vaiTro = vaiTro;
  mockSearchParams.mockReturnValue(new URLSearchParams(url));
  get.mockImplementation((path: string) => {
    if (path === "/api/v1/buildings")
      return Promise.resolve({ ...TOA, page: 1, size: 100, totalItems: 1, totalPages: 1 });
    if (path === "/api/v1/buildings/{buildingId}") return Promise.resolve(TOA.items[0]);
    return canHo();
  });
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <CanHoView />
    </QueryClientProvider>,
  );
}

function urlCuoi() {
  return new URLSearchParams(String(mockPush.mock.calls.at(-1)?.[0]).split("?")[1] ?? "");
}

beforeEach(() => {
  get.mockReset();
  mockPush.mockClear();
  mockPathname.mockReturnValue("/can-ho");
});

describe("Trang Căn hộ (spec 0012, AC-17, AC-18)", () => {
  it("bộ lọc trên URL gửi cho API với đúng tên tham số; bảng đủ cột; axe sạch", async () => {
    const { container } = hienThi(
      "maToaNha=A&soTang=2&dienTichTu=50&trangThaiSuDung=Tr%E1%BB%91ng&trangThaiSuDung=%C4%90%C3%A3+b%C3%A0n+giao&q=01&page=2",
    );
    expect(await screen.findByRole("link", { name: "A-0101" })).toHaveAttribute("href", "/can-ho/A-0101");
    expect(get).toHaveBeenCalledWith("/api/v1/apartments", {
      params: {
        query: {
          page: 2,
          size: 20,
          sort: undefined,
          maToaNha: "A",
          soTang: "2",
          dienTichTu: "50",
          dienTichDen: undefined,
          trangThaiSuDung: ["Trống", "Đã bàn giao"],
          q: "01",
        },
      },
    });
    expect(screen.getByRole("cell", { name: "75,50 m²" })).toBeInTheDocument();
    expect(screen.getByText("Tạm dừng tiện ích")).toBeInTheDocument();
    for (const cot of ["Mã căn hộ", "Tòa nhà", "Tầng", "Diện tích", "Trạng thái"]) {
      expect(screen.getByRole("columnheader", { name: new RegExp(`^${cot}`) })).toBeInTheDocument();
    }
    const nhom = screen.getByRole("group", { name: "Trạng thái sử dụng" });
    expect(within(nhom).getByRole("checkbox", { name: "Trống" })).toBeChecked();
    expect(within(nhom).getByRole("checkbox", { name: "Đang sử dụng" })).not.toBeChecked();
    expect(screen.getByRole("textbox", { name: "Diện tích từ (m²)" })).toHaveAttribute("inputmode", "decimal");
    await expectNoA11yViolations(container);
  });

  it("soTang không có maToaNha trên URL thì bỏ trước khi gọi API", async () => {
    hienThi("soTang=2");
    await screen.findByRole("link", { name: "A-0101" });
    const lanGoi = get.mock.calls.find(([path]) => path === "/api/v1/apartments")!;
    expect(lanGoi[1].params.query.soTang).toBeUndefined();
  });

  it("lọc: dấu phẩy diện tích thành dấu chấm, về trang 1, trạng thái lặp lại trên URL", async () => {
    const user = userEvent.setup();
    hienThi("page=3");
    await screen.findByRole("link", { name: "A-0101" });
    await user.type(screen.getByRole("textbox", { name: "Diện tích từ (m²)" }), "75,5");
    await user.click(screen.getByRole("checkbox", { name: "Đang sửa chữa" }));
    await user.click(screen.getByRole("checkbox", { name: "Trống" }));
    await user.click(screen.getByRole("button", { name: "Lọc" }));
    await waitFor(() => expect(mockPush).toHaveBeenCalled());
    const url = urlCuoi();
    expect(url.get("dienTichTu")).toBe("75.5");
    expect(url.getAll("trangThaiSuDung")).toEqual(["Trống", "Đang sửa chữa"]);
    expect(url.has("page")).toBe(false);
    expect(url.has("soTang")).toBe(false);
  });

  it("đổi tòa thì bỏ tầng", async () => {
    const user = userEvent.setup();
    hienThi("maToaNha=A&soTang=2");
    await screen.findByRole("link", { name: "A-0101" });
    await user.click(screen.getByRole("combobox", { name: "Tòa nhà" }));
    await user.click(await screen.findByRole("option", { name: "Tất cả tòa nhà" }));
    expect(screen.getByRole("combobox", { name: /Tầng/ })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Lọc" }));
    await waitFor(() => expect(mockPush).toHaveBeenCalled());
    expect(urlCuoi().has("maToaNha")).toBe(false);
    expect(urlCuoi().has("soTang")).toBe(false);
  });

  it("diện tích sai hiện lỗi dưới ô, không ghi URL", async () => {
    const user = userEvent.setup();
    hienThi("");
    await screen.findByRole("link", { name: "A-0101" });
    const tu = screen.getByRole("textbox", { name: "Diện tích từ (m²)" });
    await user.type(tu, "80");
    await user.type(screen.getByRole("textbox", { name: "Diện tích đến (m²)" }), "50");
    await user.click(screen.getByRole("button", { name: "Lọc" }));
    expect(await screen.findByText("Diện tích đến phải lớn hơn hoặc bằng diện tích từ")).toBeInTheDocument();
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("lỗi 400 của API gắn dưới đúng ô", async () => {
    hienThi("q=ABCDEFGHIJKLMNOPQRSTU", ["KE_TOAN"], () =>
      Promise.reject(
        new ApiError({
          status: 400,
          errorCode: "VALIDATION_FAILED",
          message: "Dữ liệu không hợp lệ",
          errors: [{ field: "q", message: "Từ khóa tìm kiếm tối đa 20 ký tự" }],
        }),
      ),
    );
    const o = await screen.findByRole("searchbox", { name: "Mã căn hộ" });
    await waitFor(() => expect(o).toHaveAccessibleDescription(/Từ khóa tìm kiếm tối đa 20 ký tự/));
  });

  it("không có kết quả khi đang lọc thì hiện trạng thái rỗng", async () => {
    hienThi("q=xyz", ["KE_TOAN"], () => Promise.resolve({ ...CAN_HO, items: [], totalItems: 0, totalPages: 0 }));
    expect(await screen.findByText("Không có căn hộ nào khớp bộ lọc")).toBeInTheDocument();
  });

  it("nút Thêm căn hộ chỉ hiện cho BQL", async () => {
    hienThi("", ["KE_TOAN", "BAN_QUAN_TRI"]);
    await screen.findByRole("link", { name: "A-0101" });
    expect(screen.queryByRole("button", { name: "Thêm căn hộ" })).not.toBeInTheDocument();
  });
});
