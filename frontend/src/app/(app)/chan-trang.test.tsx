import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import type { useHoSo } from "@/lib/auth/use-ho-so";
import { expectNoA11yViolations } from "@/test/axe";
import { mockPathname } from "@/test/next-navigation";
import { ChanTrang } from "./chan-trang";

type HoSoQuery = ReturnType<typeof useHoSo>;

const loi = (status: number, errorCode: string) => new ApiError({ status, errorCode, message: "", requestId: "r1" });

function hoSo(partial: Partial<HoSoQuery>): HoSoQuery {
  return {
    data: undefined,
    isError: false,
    error: null,
    refetch: vi.fn(),
    isRefetching: false,
    ...partial,
  } as HoSoQuery;
}

const daTai = (vaiTro: string[]) => hoSo({ data: { vaiTro } as never });

beforeEach(() => mockPathname.mockReturnValue("/toa-nha"));

describe("ChanTrang (spec 0006, AC-11, AC-13)", () => {
  it("đường dẫn ngoài ma trận luôn render, kể cả khi chưa biết vai trò", () => {
    mockPathname.mockReturnValue("/ho-so");
    render(<ChanTrang hoSo={hoSo({})}>trang</ChanTrang>);
    expect(screen.getByText("trang")).toBeInTheDocument();
  });

  it("đủ vai trò thì render trang", () => {
    mockPathname.mockReturnValue("/toa-nha/abc");
    render(<ChanTrang hoSo={daTai(["CU_DAN", "KE_TOAN"])}>trang</ChanTrang>);
    expect(screen.getByText("trang")).toBeInTheDocument();
  });

  it("không đủ vai trò thì hiện KhongCoQuyen thay cho trang", async () => {
    const { container } = render(<ChanTrang hoSo={daTai(["CU_DAN"])}>trang</ChanTrang>);
    expect(screen.queryByText("trang")).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Không có quyền truy cập" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    expect(screen.queryByRole("button", { name: "Thử lại" })).not.toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("mục chưa sẵn sàng vẫn chặn được trang", () => {
    mockPathname.mockReturnValue("/doi-soat");
    render(<ChanTrang hoSo={daTai(["CU_DAN"])}>trang</ChanTrang>);
    expect(screen.getByText("Không có quyền truy cập")).toBeInTheDocument();
  });

  it("đang tải thì hiện skeleton, chưa render trang", () => {
    render(<ChanTrang hoSo={hoSo({})}>trang</ChanTrang>);
    expect(screen.queryByText("trang")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
  });

  it("lỗi tải quyền thì có Thử lại gọi lại hồ sơ", async () => {
    const refetch = vi.fn();
    render(<ChanTrang hoSo={hoSo({ isError: true, error: loi(500, "INTERNAL_ERROR"), refetch })}>trang</ChanTrang>);
    expect(screen.queryByText("trang")).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Không tải được quyền truy cập" })).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(refetch).toHaveBeenCalled();
  });

  it("lỗi 401 giữ skeleton trong lúc chuyển về đăng nhập", () => {
    render(<ChanTrang hoSo={hoSo({ isError: true, error: loi(401, "UNAUTHENTICATED") })}>trang</ChanTrang>);
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
  });
});
