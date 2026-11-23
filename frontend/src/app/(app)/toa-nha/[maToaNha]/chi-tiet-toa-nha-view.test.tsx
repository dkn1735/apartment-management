import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { mockSearchParams } from "@/test/next-navigation";
import { ChiTietToaNhaView } from "./chi-tiet-toa-nha-view";

const get = vi.hoisted(() => vi.fn());
const hoSo = vi.hoisted(() => ({ vaiTro: ["BAN_QUAN_LY"] as string[] }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => ({ data: { vaiTro: hoSo.vaiTro } }),
}));

vi.mock("next/navigation", async () => {
  const nav = await import("@/test/next-navigation");
  return {
    usePathname: () => "/toa-nha/a",
    useRouter: () => ({ push: nav.mockPush, replace: nav.mockReplace }),
    useSearchParams: () => nav.mockSearchParams(),
    useParams: () => ({ maToaNha: "a" }),
  };
});

const TOA = { maToaNha: "A", tenToaNha: "Tòa A", soTang: 3, diaChiKhuVuc: "Hà Nội", soCanHo: 3 };
const TANG = {
  items: [{ maTang: "A-01", soTang: 1, soCanHo: 1 }],
  page: 1,
  size: 20,
  totalItems: 1,
  totalPages: 1,
};
const loi = (status: number, errorCode: string) => new ApiError({ status, errorCode, message: "x", errors: [] });

function hienThi(toa: () => Promise<unknown>, tang: () => Promise<unknown> = () => Promise.resolve(TANG)) {
  mockSearchParams.mockReturnValue(new URLSearchParams("sort=maTang,desc"));
  get.mockImplementation((path: string) => (path.endsWith("/floors") ? tang() : toa()));
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <ChiTietToaNhaView />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  get.mockReset();
  hoSo.vaiTro = ["BAN_QUAN_LY"];
});

describe("Chi tiết tòa nhà (spec 0011, AC-16, AC-17)", () => {
  it("hiện thông tin, bảng tầng theo tham số URL và nút Thêm tầng cho BQL; axe sạch", async () => {
    const { container } = hienThi(() => Promise.resolve(TOA));
    expect(await screen.findByRole("heading", { level: 1, name: "Tòa A" })).toBeInTheDocument();
    expect(await screen.findByRole("cell", { name: "A-01" })).toBeInTheDocument();
    expect(screen.getByText("Hà Nội")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Thêm tầng" })).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/api/v1/buildings/{buildingId}", { params: { path: { buildingId: "A" } } });
    expect(get).toHaveBeenCalledWith("/api/v1/buildings/{buildingId}/floors", {
      params: { path: { buildingId: "A" }, query: { page: 1, size: 20, sort: "maTang,desc" } },
    });
    await expectNoA11yViolations(container);
  });

  it("Kỹ thuật không thấy nút Thêm tầng", async () => {
    hoSo.vaiTro = ["KY_THUAT"];
    hienThi(() => Promise.resolve(TOA));
    await screen.findByRole("cell", { name: "A-01" });
    expect(screen.queryByRole("button", { name: "Thêm tầng" })).not.toBeInTheDocument();
  });

  it("tòa ngoài phạm vi (404) thì hiện Không tìm thấy tòa nhà có link về danh mục", async () => {
    hienThi(() => Promise.reject(loi(404, "NOT_FOUND")));
    expect(await screen.findByRole("heading", { level: 1, name: "Không tìm thấy tòa nhà" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Danh mục tòa nhà" })).toHaveAttribute("href", "/toa-nha");
  });

  it("không có quyền xem tầng (403) thì hiện câu thay cho bảng", async () => {
    hoSo.vaiTro = ["KE_TOAN"];
    hienThi(
      () => Promise.resolve(TOA),
      () => Promise.reject(loi(403, "FORBIDDEN")),
    );
    expect(await screen.findByText("Bạn không có quyền xem danh sách tầng")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
