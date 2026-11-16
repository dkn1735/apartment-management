import { QueryClientProvider, useMutation, useQuery, type QueryClient } from "@tanstack/react-query";
import { render, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./api";
import { authNavigation, BO_QUA_CHUYEN_TRANG, createQueryClient, resetAuthRedirect } from "./query-client";

const loi401 = () => new ApiError({ status: 401, errorCode: "UNAUTHENTICATED", message: "x" });
const loiKhoa = () => new ApiError({ status: 403, errorCode: "AUTH_ACCOUNT_LOCKED", message: "x" });

function QueryLoi({ id, error }: { id: string; error: () => ApiError }) {
  useQuery({ queryKey: [id], queryFn: () => Promise.reject(error()) });
  return null;
}

function MutationLoi({ boQua }: { boQua: boolean }) {
  const mutation = useMutation({
    mutationFn: () => Promise.reject(loiKhoa()),
    meta: boQua ? { [BO_QUA_CHUYEN_TRANG]: true } : undefined,
  });
  return (
    <button type="button" onClick={() => mutation.mutate()}>
      gửi
    </button>
  );
}

let assign: ReturnType<typeof vi.spyOn>;
let client: QueryClient;

beforeEach(() => {
  resetAuthRedirect();
  client = createQueryClient();
  assign = vi.spyOn(authNavigation, "assign").mockImplementation(() => {});
  window.history.pushState({}, "", "/ho-so?tab=vai-tro");
});

afterEach(() => {
  assign.mockRestore();
  client.clear();
});

describe("Chuyển về đăng nhập khi lỗi xác thực (AC-15)", () => {
  it("query 401 chuyển tới /dang-nhap với next đã mã hóa và ly-do=het-phien", async () => {
    render(
      <QueryClientProvider client={client}>
        <QueryLoi id="a" error={loi401} />
      </QueryClientProvider>,
    );
    await waitFor(() => expect(assign).toHaveBeenCalled());
    expect(assign).toHaveBeenCalledWith("/dang-nhap?next=%2Fho-so%3Ftab%3Dvai-tro&ly-do=het-phien");
  });

  it("hai query cùng 401 chỉ chuyển trang một lần", async () => {
    render(
      <QueryClientProvider client={client}>
        <QueryLoi id="a" error={loi401} />
        <QueryLoi id="b" error={loi401} />
      </QueryClientProvider>,
    );
    await waitFor(() => expect(assign).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(assign).toHaveBeenCalledOnce();
  });

  it("tài khoản bị khóa chuyển tới ly-do=khoa", async () => {
    render(
      <QueryClientProvider client={client}>
        <QueryLoi id="a" error={loiKhoa} />
      </QueryClientProvider>,
    );
    await waitFor(() => expect(assign).toHaveBeenCalledWith("/dang-nhap?ly-do=khoa"));
  });

  it("mutation có meta boQuaChuyenTrang thì không chuyển trang", async () => {
    const view = render(
      <QueryClientProvider client={client}>
        <MutationLoi boQua />
      </QueryClientProvider>,
    );
    view.getByRole("button").click();
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(assign).not.toHaveBeenCalled();
  });

  it("mutation thường bị khóa thì chuyển trang", async () => {
    const view = render(
      <QueryClientProvider client={client}>
        <MutationLoi boQua={false} />
      </QueryClientProvider>,
    );
    view.getByRole("button").click();
    await waitFor(() => expect(assign).toHaveBeenCalledWith("/dang-nhap?ly-do=khoa"));
  });

  it.each(["/dang-nhap", "/dev/ui"])("đang ở %s thì không chuyển trang (tránh vòng lặp)", async (path) => {
    window.history.pushState({}, "", path);
    render(
      <QueryClientProvider client={client}>
        <QueryLoi id="a" error={loi401} />
      </QueryClientProvider>,
    );
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(assign).not.toHaveBeenCalled();
  });
});
