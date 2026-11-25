import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { mucCuaDuongDan } from "@/lib/phan-quyen/menu";
import { expectNoA11yViolations } from "@/test/axe";
import { CanHoCuaToiView } from "./can-ho-cua-toi-view";

const get = vi.hoisted(() => vi.fn());
const hoSo = vi.hoisted(() => ({ maCanHo: [] as string[] }));

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({
    isPending: false,
    data: { vaiTro: ["CU_DAN"], phamVi: { maToaNha: [], maCanHo: hoSo.maCanHo } },
  }),
}));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get }, api: (p: Promise<unknown>) => p };
});

const can = (ma: string) => ({
  maCanHo: ma,
  maTang: ma.slice(0, 4),
  soTang: Number(ma.slice(2, 4)),
  maToaNha: "A",
  tenToaNha: "Tòa A",
  dienTich: 75.5,
  trangThaiSuDung: "Đang sử dụng",
  biTamDungTienIch: false,
});

function hienThi(maCanHo: string[]) {
  hoSo.maCanHo = maCanHo;
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <CanHoCuaToiView />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  get.mockReset();
});

describe("Căn hộ của tôi (spec 0012, AC-20)", () => {
  it("không có căn nào thì hiện trạng thái rỗng", () => {
    hienThi([]);
    expect(screen.getByText("Bạn chưa có hồ sơ cư trú hợp lệ")).toBeInTheDocument();
    expect(get).not.toHaveBeenCalled();
  });

  it("đúng một căn thì hiện luôn chi tiết", async () => {
    get.mockResolvedValue(can("A-0201"));
    hienThi(["A-0201"]);
    expect(await screen.findByRole("heading", { name: "Căn hộ A-0201" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Sửa" })).not.toBeInTheDocument();
  });

  it("nhiều căn thì mỗi thẻ tự tải, lỗi của thẻ này không ảnh hưởng thẻ khác; axe sạch", async () => {
    get.mockImplementation((_path: string, init?: { params: { path: { apartmentId: string } } }) => {
      const ma = init?.params.path.apartmentId;
      if (ma === "A-0101") return Promise.resolve(can(ma));
      if (ma === "A-0201")
        return Promise.reject(new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "x", errors: [] }));
      return Promise.reject(new ApiError({ status: 403, errorCode: "FORBIDDEN", message: "x", errors: [] }));
    });
    const { container } = hienThi(["A-0101", "A-0201", "A-0301"]);
    expect(await screen.findByText("75,50 m²")).toBeInTheDocument();
    expect(await screen.findByText("Không còn quyền xem căn này")).toBeInTheDocument();
    expect(await screen.findByText("Không tải được thông tin căn hộ")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Căn hộ A-0301" })).toHaveAttribute("href", "/can-ho-cua-toi/A-0301");
    await expectNoA11yViolations(container);
  });
});

describe("mucCuaDuongDan (AC-20)", () => {
  it("/can-ho-cua-toi/... không khớp nhầm /can-ho", () => {
    expect(mucCuaDuongDan("/can-ho-cua-toi/A-0101")?.href).toBe("/can-ho-cua-toi");
    expect(mucCuaDuongDan("/can-ho/A-0101")?.href).toBe("/can-ho");
  });
});
