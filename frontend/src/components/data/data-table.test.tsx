import { keepPreviousData, QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import { expectNoA11yViolations } from "@/test/axe";
import { DataTable } from "./data-table";
import { dataTableColumns } from "./data-table-columns";
import type { PageSize, TableSort } from "./table-params";

type Row = { ma: string; ten: string; moTa: string };
type Params = { page: number; size: number; sort?: TableSort };

const col = dataTableColumns<Row>();
const columns = col.columns([
  col.accessor("ma", { header: "Mã", meta: { sortField: "ma" } }),
  col.accessor("ten", { header: "Tên" }),
  col.accessor("moTa", { header: "Mô tả", meta: { description: true } }),
]);

function pageOf(params: Params, total: number): PageResponse<Row> {
  const start = (params.page - 1) * params.size;
  const count = Math.max(0, Math.min(params.size, total - start));
  return {
    items: Array.from({ length: count }, (_, i) => ({
      ma: `M${start + i + 1}`,
      ten: `Tên ${start + i + 1}`,
      moTa: "Dài",
    })),
    page: params.page,
    size: params.size,
    totalItems: total,
    totalPages: Math.ceil(total / params.size),
  };
}

function Harness({ fetcher }: { fetcher: (params: Params) => Promise<PageResponse<Row>> }) {
  const [page, setPage] = useState(1);
  const [size, setSize] = useState<PageSize>(10);
  const [sort, setSort] = useState<TableSort>();
  const query = useQuery<PageResponse<Row>, ApiError>({
    queryKey: ["t", page, size, sort],
    queryFn: () => fetcher({ page, size, sort }),
    placeholderData: keepPreviousData,
  });
  return (
    <DataTable
      caption="Danh sách thử"
      columns={columns}
      query={query}
      page={page}
      size={size}
      sort={sort}
      onPageChange={setPage}
      onSizeChange={(next) => setSize(next as PageSize)}
      onSortChange={setSort}
    />
  );
}

function setup(fetcher: (params: Params) => Promise<PageResponse<Row>>) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const user = userEvent.setup();
  const view = render(
    <QueryClientProvider client={client}>
      <Harness fetcher={fetcher} />
    </QueryClientProvider>,
  );
  return { user, client, ...view };
}

/** Lời gọi giả có thể giữ lại để quan sát trạng thái đang tải. */
function controllableFetcher(total = 35) {
  let release: () => void = () => {};
  let hold = false;
  const fetcher = vi.fn(async (params: Params) => {
    if (hold) await new Promise<void>((resolve) => (release = resolve));
    return pageOf(params, total);
  });
  return { fetcher, holdNext: () => (hold = true), release: () => ((hold = false), release()) };
}

describe("DataTable (AC-9, AC-10)", () => {
  it("tải đầu thấy skeleton, rồi có dữ liệu, caption và vùng cuộn có nhãn; không vi phạm axe", async () => {
    const { fetcher } = controllableFetcher();
    const { container } = setup(fetcher);
    expect(screen.getByRole("table")).toHaveAttribute("aria-busy", "true");
    expect(screen.getByText("Đang tải")).toBeInTheDocument();
    expect(await screen.findByText("M1")).toBeInTheDocument();
    expect(screen.getByRole("table", { name: "Danh sách thử" })).not.toHaveAttribute("aria-busy");
    expect(screen.getByRole("region", { name: "Danh sách thử" })).toHaveAttribute("tabindex", "0");
    expect(screen.getByText("35 kết quả")).toBeInTheDocument();
    expect(screen.getByText("Trang 1 / 4")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Trang trước/ })).toHaveAttribute("aria-disabled", "true");
    await expectNoA11yViolations(container);
  });

  it("chuyển trang giữ dòng cũ với aria-busy rồi hiện trang mới", async () => {
    const control = controllableFetcher();
    const { user } = setup(control.fetcher);
    await screen.findByText("M1");
    control.holdNext();
    await user.click(screen.getByRole("button", { name: /Trang sau/ }));
    expect(screen.getByText("M1")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("table")).toHaveAttribute("aria-busy", "true"));
    control.release();
    expect(await screen.findByText("M11")).toBeInTheDocument();
    expect(screen.getByText("Trang 2 / 4")).toBeInTheDocument();
  });

  it("đổi số dòng thì về trang 1", async () => {
    const { fetcher } = controllableFetcher();
    const { user } = setup(fetcher);
    await screen.findByText("M1");
    await user.click(screen.getByRole("button", { name: /Trang sau/ }));
    await screen.findByText("M11");
    await user.click(screen.getByRole("combobox", { name: "Số dòng mỗi trang" }));
    await user.click(await screen.findByRole("option", { name: "20" }));
    await screen.findByText("Trang 1 / 2");
    expect(fetcher).toHaveBeenLastCalledWith({ page: 1, size: 20, sort: undefined });
  });

  it("bấm tiêu đề ba lần đi qua tăng, giảm, bỏ; cột không có sortField không bấm được", async () => {
    const { fetcher } = controllableFetcher();
    const { user } = setup(fetcher);
    await screen.findByText("M1");
    const header = () => screen.getByRole("columnheader", { name: /Mã/ });
    expect(within(screen.getByRole("columnheader", { name: "Tên" })).queryByRole("button")).toBeNull();
    await user.click(within(header()).getByRole("button"));
    await waitFor(() => expect(header()).toHaveAttribute("aria-sort", "ascending"));
    expect(fetcher).toHaveBeenLastCalledWith({ page: 1, size: 10, sort: { field: "ma", dir: "asc" } });
    await user.click(within(header()).getByRole("button"));
    await waitFor(() => expect(header()).toHaveAttribute("aria-sort", "descending"));
    await user.click(within(header()).getByRole("button"));
    await waitFor(() => expect(header()).not.toHaveAttribute("aria-sort"));
  });

  it("rỗng thì hiện EmptyState và ẩn thanh phân trang", async () => {
    setup(async (params) => pageOf(params, 0));
    expect(await screen.findByText("Chưa có dữ liệu")).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Phân trang" })).not.toBeInTheDocument();
  });

  it("lỗi khi chưa có dữ liệu thì thay bảng bằng ErrorState có Thử lại", async () => {
    const fetcher = vi.fn(() =>
      Promise.reject(new ApiError({ status: 500, errorCode: "INTERNAL_ERROR", message: "raw" })),
    );
    const { user } = setup(fetcher);
    expect(await screen.findByRole("heading", { name: "Không tải được dữ liệu" })).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Thử lại" }));
    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  });

  it("lỗi khi đã có dữ liệu thì giữ bảng, hiện Thử lại và bấm thì gọi lại", async () => {
    let fail = false;
    const fetcher = vi.fn(async (params: Params) => {
      if (fail) throw new ApiError({ status: 409, errorCode: "CONFLICT", message: "raw" });
      return pageOf(params, 35);
    });
    const { user, client } = setup(fetcher);
    await screen.findByText("M1");
    // Tải lại cùng trang (ví dụ khi quay lại tab) và thất bại: dữ liệu cũ vẫn còn
    fail = true;
    await act(() => client.refetchQueries());
    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Dữ liệu bị trùng");
    expect(screen.getByText("M1")).toBeInTheDocument();
    fail = false;
    await user.click(within(alert).getByRole("button", { name: "Thử lại" }));
    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
    expect(fetcher).toHaveBeenCalledTimes(3);
  });

  it("trang vượt quá tổng số trang thì về trang cuối", async () => {
    let total = 35;
    const { user } = setup(async (params) => pageOf(params, total));
    await screen.findByText("M1");
    await user.click(screen.getByRole("button", { name: /Trang sau/ }));
    await user.click(await screen.findByRole("button", { name: /Trang sau/ }));
    await user.click(await screen.findByRole("button", { name: /Trang sau/ }));
    await screen.findByText("Trang 4 / 4");
    total = 25;
    await user.click(screen.getByRole("button", { name: /Trang trước/ }));
    await user.click(screen.getByRole("button", { name: /Trang sau/ }));
    expect(await screen.findByText("Trang 3 / 3")).toBeInTheDocument();
  });
});
