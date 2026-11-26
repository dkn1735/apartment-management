import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { expectNoA11yViolations } from "@/test/axe";
import { mockPathname, mockPush, mockSearchParams } from "@/test/next-navigation";
import { KhuVucChungView } from "./khu-vuc-chung-view";

const get = vi.hoisted(() => vi.fn());

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { GET: get }, api: (p: Promise<unknown>) => p };
});

const TOA = { items: [{ maToaNha: "A", tenToaNha: "Tòa A", soTang: 3, diaChiKhuVuc: "HN", soCanHo: 3 }] };
const KHU_VUC = {
  items: [{ maKhuVuc: "KV00000001", tenKhuVuc: "Sảnh chính", viTri: "Tầng 1", maToaNha: "A", tenToaNha: "Tòa A" }],
  page: 1,
  size: 20,
  totalItems: 1,
  totalPages: 1,
};

function hienThi(url: string, khuVuc: () => Promise<unknown> = () => Promise.resolve(KHU_VUC)) {
  mockSearchParams.mockReturnValue(new URLSearchParams(url));
  get.mockImplementation((path: string) =>
    path === "/api/v1/buildings"
      ? Promise.resolve({ ...TOA, page: 1, size: 100, totalItems: 1, totalPages: 1 })
      : khuVuc(),
  );
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <KhuVucChungView />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  get.mockReset();
  mockPush.mockClear();
  mockPathname.mockReturnValue("/khu-vuc-chung");
});

describe("Khu vực chung (spec 0011, AC-18)", () => {
  it("bộ lọc trên URL được gửi cho API; bảng có cột mã, tên, vị trí, tòa; axe sạch", async () => {
    const { container } = hienThi("maToaNha=A&q=s%E1%BA%A3nh&page=2&sort=tenToaNha,desc");
    expect(await screen.findByRole("cell", { name: "Sảnh chính" })).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith("/api/v1/common-areas", {
      params: { query: { page: 2, size: 20, sort: "tenToaNha,desc", maToaNha: "A", q: "sảnh" } },
    });
    expect(get).toHaveBeenCalledWith("/api/v1/buildings", { params: { query: { size: 100, sort: "tenToaNha,asc" } } });
    for (const cot of ["Mã khu vực", "Tên khu vực", "Vị trí", "Tòa nhà"]) {
      expect(screen.getByRole("columnheader", { name: new RegExp(cot) })).toBeInTheDocument();
    }
    expect(screen.getByRole("combobox", { name: "Tòa nhà" })).toBeInTheDocument();
    expect(screen.getByRole("searchbox", { name: "Tìm theo tên" })).toHaveValue("sảnh");
    await expectNoA11yViolations(container);
  });

  it("gõ tìm thì ghi q lên URL và về trang 1", async () => {
    const user = userEvent.setup();
    hienThi("page=3");
    await user.type(screen.getByRole("searchbox", { name: "Tìm theo tên" }), "hành{Enter}");
    await waitFor(() => expect(mockPush).toHaveBeenCalled());
    const url = decodeURIComponent(String(mockPush.mock.calls.at(-1)?.[0]));
    expect(url).toContain("q=hành");
    expect(url).not.toContain("page=");
  });

  it("không có kết quả khi đang lọc thì hiện trạng thái rỗng", async () => {
    hienThi("q=xyz", () => Promise.resolve({ ...KHU_VUC, items: [], totalItems: 0, totalPages: 0 }));
    expect(await screen.findByText("Không có khu vực nào khớp bộ lọc")).toBeInTheDocument();
  });

  it("tòa trên URL trả 404 thì hiện Không tìm thấy tòa nhà, nút Xóa bộ lọc bỏ maToaNha", async () => {
    const user = userEvent.setup();
    hienThi("maToaNha=ZZ&q=s", () =>
      Promise.reject(new ApiError({ status: 404, errorCode: "NOT_FOUND", message: "x", errors: [] })),
    );
    expect(await screen.findByRole("heading", { name: "Không tìm thấy tòa nhà" })).toBeInTheDocument();
    const nut = screen.getAllByRole("button", { name: "Xóa bộ lọc" }).at(-1)!;
    await user.click(nut);
    const url = String(mockPush.mock.calls.at(-1)?.[0]);
    expect(url).not.toContain("maToaNha");
    expect(url).toContain("q=s");
  });
});
