import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/api";
import { createQueryClient } from "@/lib/api/query-client";
import { khoaChiTietToaNha, type ToaNha } from "@/lib/toa-nha";
import { ThemTangButton } from "./them-tang-button";

const post = vi.hoisted(() => vi.fn());
const notify = vi.hoisted(() => ({ success: vi.fn(), info: vi.fn() }));

vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, client: { POST: post }, api: (p: Promise<unknown>) => p };
});

vi.mock("@/components/feedback/notify", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/components/feedback/notify")>()),
  notifySuccess: notify.success,
  notifyInfo: notify.info,
}));

const TOA: ToaNha = { maToaNha: "A", tenToaNha: "Tòa A", soTang: 3, diaChiKhuVuc: "Hà Nội", soCanHo: 3 };

const sai422 = () =>
  new ApiError({
    status: 422,
    errorCode: "CANHO_TANG_SAI_THU_TU",
    message: "Tầng mới phải nối tiếp tầng cao nhất của tòa",
    errors: [{ field: "soTang", message: "Tầng tiếp theo của tòa này là tầng 5" }],
  });

function hienThi(toa: ToaNha, moiTaiVe?: ToaNha) {
  const queryClient = createQueryClient();
  // Chi tiết tòa đang hiển thị; tải lại thì trả bản mới
  queryClient.setQueryDefaults(khoaChiTietToaNha(toa.maToaNha), { queryFn: () => moiTaiVe ?? toa });
  queryClient.setQueryData(khoaChiTietToaNha(toa.maToaNha), toa);
  render(
    <QueryClientProvider client={queryClient}>
      <ThemTangButton toaNha={toa} />
    </QueryClientProvider>,
  );
  return queryClient;
}

beforeEach(() => {
  post.mockReset();
  notify.success.mockReset();
  notify.info.mockReset();
});

describe("Nút Thêm tầng (spec 0011, AC-17)", () => {
  it("hỏi xác nhận với tầng kế tiếp rồi gửi số đó", async () => {
    const user = userEvent.setup();
    post.mockResolvedValue({ maTang: "A-04", soTang: 4, soCanHo: 0 });
    hienThi(TOA);
    await user.click(screen.getByRole("button", { name: "Thêm tầng" }));
    expect(await screen.findByRole("alertdialog", { name: "Thêm tầng 4 vào Tòa A?" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Thêm tầng 4" }));
    await waitFor(() => expect(notify.success).toHaveBeenCalledWith("Đã thêm tầng 4"));
    expect(post).toHaveBeenCalledWith("/api/v1/buildings/{buildingId}/floors", {
      params: { path: { buildingId: "A" } },
      body: { soTang: 4 },
    });
  });

  it("422 thì làm mới chi tiết tòa và báo tòa vừa được cập nhật", async () => {
    const user = userEvent.setup();
    post.mockRejectedValue(sai422());
    hienThi(TOA, { ...TOA, soTang: 4 });
    await user.click(screen.getByRole("button", { name: "Thêm tầng" }));
    await user.click(await screen.findByRole("button", { name: "Thêm tầng 4" }));
    await waitFor(() => expect(notify.info).toHaveBeenCalledWith("Tòa vừa được cập nhật, vui lòng thử lại"));
  });

  it("422 mà số tầng mới là 99 thì báo đã đạt tối đa", async () => {
    const user = userEvent.setup();
    post.mockRejectedValue(sai422());
    hienThi({ ...TOA, soTang: 98 }, { ...TOA, soTang: 99 });
    await user.click(screen.getByRole("button", { name: "Thêm tầng" }));
    await user.click(await screen.findByRole("button", { name: "Thêm tầng 99" }));
    await waitFor(() => expect(notify.info).toHaveBeenCalledWith("Tòa đã đạt tối đa 99 tầng"));
  });

  it("tòa đủ 99 tầng thì nút bị vô hiệu kèm lời giải thích", () => {
    hienThi({ ...TOA, soTang: 99 });
    const nut = screen.getByRole("button", { name: "Thêm tầng" });
    expect(nut).toBeDisabled();
    expect(nut).toHaveAccessibleDescription("Tòa đã đạt tối đa 99 tầng");
  });
});
