import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { ToaNhaView } from "./toa-nha-view";

const get = vi.hoisted(() => vi.fn());

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get }, api: (p: Promise<unknown>) => p };
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
