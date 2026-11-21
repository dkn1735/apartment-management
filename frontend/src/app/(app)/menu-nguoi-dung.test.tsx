import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { Toaster } from "@/components/ui/sonner";
import { ApiError, client } from "@/lib/api/api";
import { authNavigation, createQueryClient, handleAuthError, resetAuthRedirect } from "@/lib/api/query-client";
import { dangDangXuat, lamMoiPhien, resetRefreshState } from "@/lib/api/refresh";
import { expectNoA11yViolations } from "@/test/axe";
import { MenuNguoiDung } from "./menu-nguoi-dung";

const hoSo = vi.hoisted(() => ({ current: {} as { data?: { hoTen: string; email: string; soDienThoai: string } } }));
const apiMock = vi.hoisted(() => vi.fn());

vi.mock("@/lib/auth/use-ho-so", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/auth/use-ho-so")>()),
  useHoSo: () => hoSo.current,
}));
vi.mock("@/lib/api/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api/api")>();
  return { ...actual, api: apiMock, client: { POST: vi.fn((path: string) => Promise.resolve({ path })) } };
});

const CO_HO_SO = { data: { hoTen: "Vũ Cư Dân", email: "cudan@chungcu.test", soDienThoai: "0900000007" } };

let assign: ReturnType<typeof vi.spyOn>;

function setup() {
  const user = userEvent.setup();
  const view = render(
    <QueryClientProvider client={createQueryClient()}>
      <MenuNguoiDung />
      <Toaster />
    </QueryClientProvider>,
  );
  return { user, ...view };
}

const nut = () => screen.getByRole("button", { name: /mở menu tài khoản/ });

beforeEach(() => {
  hoSo.current = CO_HO_SO;
  apiMock.mockReset();
  resetRefreshState();
  resetAuthRedirect();
  assign = vi.spyOn(authNavigation, "assign").mockImplementation(() => {});
  window.history.pushState({}, "", "/ho-so");
});

afterEach(() => {
  assign.mockRestore();
});

describe("Menu người dùng (AC-13)", () => {
  it("nút có tên truy cập gồm họ tên, mở bằng Enter, Escape trả focus về nút, axe không lỗi", async () => {
    const { user, container } = setup();
    expect(nut()).toHaveAccessibleName("Vũ Cư Dân, mở menu tài khoản");

    nut().focus();
    await user.keyboard("{Enter}");
    const menu = await screen.findByRole("menu");
    expect(screen.getByRole("menuitem", { name: "Hồ sơ" })).toHaveAttribute("href", "/ho-so");
    expect(screen.getByRole("menuitem", { name: "Đăng xuất" })).toBeInTheDocument();
    // Kiểm theo vùng như các test khác: menu nằm trong portal ngoài landmark, quy tắc region
    // chỉ có nghĩa ở cấp trang
    await expectNoA11yViolations(container);
    await expectNoA11yViolations(menu);

    await user.keyboard("{Escape}");
    await waitFor(() => expect(menu).not.toBeInTheDocument());
    expect(nut()).toHaveFocus();
  });

  it("mở bằng mũi tên xuống và di chuyển giữa các mục bằng mũi tên", async () => {
    const { user } = setup();
    nut().focus();
    await user.keyboard("{ArrowDown}");
    await screen.findByRole("menu");
    await waitFor(() => expect(screen.getByRole("menuitem", { name: "Hồ sơ" })).toHaveFocus());
    await user.keyboard("{ArrowDown}");
    expect(screen.getByRole("menuitem", { name: "Đổi mật khẩu" })).toHaveFocus();
    await user.keyboard("{ArrowDown}");
    expect(screen.getByRole("menuitem", { name: "Đăng xuất" })).toHaveFocus();
  });

  it("hồ sơ đang tải hoặc lỗi thì nút hiện Tài khoản và vẫn đăng xuất được", async () => {
    hoSo.current = {};
    apiMock.mockResolvedValue(null);
    const { user } = setup();
    expect(nut()).toHaveAccessibleName("Tài khoản, mở menu tài khoản");
    await user.click(nut());
    await user.click(await screen.findByRole("menuitem", { name: "Đăng xuất" }));
    await waitFor(() => expect(assign).toHaveBeenCalledWith("/dang-nhap?ly-do=dang-xuat"));
  });
});

describe("Đăng xuất (AC-14)", () => {
  it("gọi logout rồi tải lại trang tới ly-do=dang-xuat; 401 chen vào không chuyển het-phien", async () => {
    let xong!: () => void;
    apiMock.mockReturnValue(new Promise((resolve) => (xong = () => resolve(null))));
    const { user } = setup();
    await user.click(nut());
    await user.click(await screen.findByRole("menuitem", { name: "Đăng xuất" }));

    await waitFor(() => expect(apiMock).toHaveBeenCalledTimes(1));
    expect(client.POST).toHaveBeenCalledWith("/api/v1/auth/logout");
    expect(screen.getByRole("menuitem", { name: "Đang đăng xuất…" })).toHaveAttribute("aria-disabled", "true");
    // Một query đang chạy trả 401 trong lúc chờ
    handleAuthError(new ApiError({ status: 401, errorCode: "UNAUTHENTICATED", message: "x" }));
    expect(assign).not.toHaveBeenCalled();

    xong();
    await waitFor(() => expect(assign).toHaveBeenCalledTimes(1));
    expect(assign).toHaveBeenCalledWith("/dang-nhap?ly-do=dang-xuat");
  });

  it("một lần làm mới đang chạy thì logout chỉ được gọi sau khi nó xong", async () => {
    let moLamMoi!: () => void;
    const fetchLamMoi = vi.fn(
      () =>
        new Promise<Response>((resolve) => {
          moLamMoi = () => resolve(new Response(JSON.stringify({ data: { lamMoi: true } }), { status: 200 }));
        }),
    );
    void lamMoiPhien(fetchLamMoi, "http://localhost");
    apiMock.mockResolvedValue(null);
    const { user } = setup();
    await user.click(nut());
    await user.click(await screen.findByRole("menuitem", { name: "Đăng xuất" }));

    expect(dangDangXuat()).toBe(true);
    await new Promise((r) => setTimeout(r, 20));
    expect(apiMock).not.toHaveBeenCalled();
    moLamMoi();
    await waitFor(() => expect(apiMock).toHaveBeenCalledTimes(1));
    expect(fetchLamMoi).toHaveBeenCalledTimes(1);
  });

  it("đăng xuất lỗi thì hiện toast, ở lại trang, cờ đăng xuất tắt lại", async () => {
    apiMock.mockRejectedValue(new ApiError({ status: 0, errorCode: "BACKEND_UNAVAILABLE", message: "x" }));
    const { user } = setup();
    await user.click(nut());
    await user.click(await screen.findByRole("menuitem", { name: "Đăng xuất" }));

    expect(await screen.findByText("Không đăng xuất được, vui lòng thử lại.")).toBeInTheDocument();
    expect(assign).not.toHaveBeenCalled();
    expect(dangDangXuat()).toBe(false);
  });
});
