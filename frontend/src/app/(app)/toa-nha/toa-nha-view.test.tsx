import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { ToaNhaView } from "./toa-nha-view";

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

const TRANG = {
  items: [{ maToaNha: "A", tenToaNha: "Tòa A", soTang: 3, diaChiKhuVuc: "Hà Nội", soCanHo: 3 }],
  page: 1,
  size: 20,
  totalItems: 1,
  totalPages: 1,
};

function hienThi(vaiTro: string[]) {
  hoSo.vaiTro = vaiTro;
  get.mockResolvedValue(TRANG);
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <ToaNhaView />
    </QueryClientProvider>,
  );
}

describe("Trang Tòa nhà theo vai trò (spec 0011, AC-15, AC-16)", () => {
  it("BQL có nút Thêm tòa nhà và tên tòa là link tới trang chi tiết", async () => {
    const { container } = hienThi(["BAN_QUAN_LY"]);
    expect(await screen.findByRole("link", { name: "Tòa A" })).toHaveAttribute("href", "/toa-nha/A");
    expect(screen.getByRole("button", { name: "Thêm tòa nhà" })).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("Kỹ thuật có link nhưng không có nút thêm", async () => {
    hienThi(["KY_THUAT"]);
    expect(await screen.findByRole("link", { name: "Tòa A" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Thêm tòa nhà" })).not.toBeInTheDocument();
  });

  it("Kế toán thấy tên tòa dạng chữ thường, không có nút thêm", async () => {
    hienThi(["KE_TOAN"]);
    expect(await screen.findByRole("cell", { name: "Tòa A" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Tòa A" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Thêm tòa nhà" })).not.toBeInTheDocument();
  });
});

describe("Trang Tòa nhà (spec 0006, AC-15, AC-16)", () => {
  it("hiện bảng và không gửi sort khi URL không có sort", async () => {
    get.mockResolvedValue({
      items: [{ maToaNha: "A", tenToaNha: "Tòa A", soTang: 3, diaChiKhuVuc: "Hà Nội", soCanHo: 3 }],
      page: 1,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });
    const { container } = render(
      <QueryClientProvider client={createQueryClient()}>
        <ToaNhaView />
      </QueryClientProvider>,
    );
    expect(await screen.findByRole("cell", { name: "Tòa A" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Tòa nhà" })).toBeInTheDocument();
    for (const cot of ["Mã tòa nhà", "Tên tòa nhà", "Số tầng", "Địa chỉ khu vực", "Số căn hộ"]) {
      expect(screen.getByRole("columnheader", { name: new RegExp(cot) })).toBeInTheDocument();
    }
    expect(get).toHaveBeenCalledWith("/api/v1/buildings", {
      params: { query: { page: 1, size: 20, sort: undefined } },
    });
    await expectNoA11yViolations(container);
  });
});
