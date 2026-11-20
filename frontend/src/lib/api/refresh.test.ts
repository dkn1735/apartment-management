import createClient from "openapi-fetch";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError, csrfHeaderMiddleware } from "./api";
import { authNavigation, handleAuthError, resetAuthRedirect } from "./query-client";
import { lamMoiPhien, refreshMiddleware, resetRefreshState } from "./refresh";
import type { paths } from "./schema";

const LOI_401 = { success: false, code: 401, errorCode: "UNAUTHENTICATED", message: "x" };

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

const hoSo = (hoTen: string) => json(200, { success: true, data: { hoTen } });
const lamMoiOk = (lamMoi = true) => json(200, { success: true, data: { lamMoi } });

type Handler = (request: Request, body: string) => Response | Promise<Response>;

/** fetch giả: ghi lại mọi request (đường, method, body) và trả lời theo handler. */
function fakeFetch(handler: Handler) {
  const calls: { path: string; method: string; body: string; csrf: string | null }[] = [];
  const fn = vi.fn(async (request: Request) => {
    const body = request.method === "GET" ? "" : await request.clone().text();
    calls.push({
      path: new URL(request.url).pathname,
      method: request.method,
      body,
      csrf: request.headers.get("X-Requested-With"),
    });
    return handler(request, body);
  });
  return { fn, calls, count: (path: string) => calls.filter((c) => c.path === path).length };
}

function taoClient(fetchFn: (request: Request) => Promise<Response>) {
  const client = createClient<paths>({ baseUrl: "http://localhost", fetch: fetchFn });
  client.use(csrfHeaderMiddleware, refreshMiddleware);
  return client;
}

const REFRESH = "/api/v1/auth/refresh-token";
const PROFILE = "/api/v1/auth/profile";
const LOGIN = "/api/v1/auth/login";

beforeEach(() => {
  resetRefreshState();
  resetAuthRedirect();
});

afterEach(() => {
  vi.useRealTimers();
});

describe("Làm mới âm thầm khi gặp 401 (AC-10)", () => {
  it("hai query cùng 401 dùng chung một lời gọi làm mới, mỗi query gửi lại một lần", async () => {
    let daLamMoi = false;
    let moLamMoi!: () => void;
    const choLamMoi = new Promise<void>((resolve) => (moLamMoi = resolve));
    const fake = fakeFetch(async (request) => {
      const path = new URL(request.url).pathname;
      if (path === REFRESH) {
        await choLamMoi;
        daLamMoi = true;
        return lamMoiOk();
      }
      return daLamMoi ? hoSo("Vũ Cư Dân") : json(401, LOI_401);
    });
    const client = taoClient(fake.fn);

    const a = api(client.GET(PROFILE));
    const b = api(client.GET(PROFILE));
    await vi.waitFor(() => expect(fake.count(REFRESH)).toBe(1));
    moLamMoi();

    await expect(a).resolves.toEqual({ hoTen: "Vũ Cư Dân" });
    await expect(b).resolves.toEqual({ hoTen: "Vũ Cư Dân" });
    expect(fake.count(REFRESH)).toBe(1);
    expect(fake.count(PROFILE)).toBe(4);
    expect(fake.calls.find((c) => c.path === REFRESH)?.csrf).toBe("XMLHttpRequest");
  });

  it("POST có body được gửi lại với đúng body và header chống CSRF", async () => {
    let lan = 0;
    const fake = fakeFetch((request) => {
      if (new URL(request.url).pathname === REFRESH) {
        return lamMoiOk();
      }
      lan += 1;
      return lan === 1 ? json(401, LOI_401) : json(200, { success: true, data: null });
    });
    const client = taoClient(fake.fn);
    // Lời gọi tới /login trả 401 không kích hoạt làm mới
    await expect(
      api(client.POST(LOGIN as never, { body: { taiKhoan: "a", matKhau: "b" } } as never)),
    ).rejects.toMatchObject({ errorCode: "UNAUTHENTICATED" });
    expect(fake.count(REFRESH)).toBe(0);

    // openapi-fetch không kiểm đường lúc chạy: mô phỏng một request ghi bất kỳ
    lan = 0;
    await api(client.POST("/api/v1/canho" as never, { body: { ten: "Căn A" } } as never));
    const ghi = fake.calls.filter((c) => c.path === "/api/v1/canho");
    expect(ghi).toHaveLength(2);
    expect(ghi[1].body).toBe(JSON.stringify({ ten: "Căn A" }));
    expect(ghi[1].csrf).toBe("XMLHttpRequest");
  });

  it("request gửi lại vẫn 401 thì không làm mới lần hai", async () => {
    const fake = fakeFetch((request) => (new URL(request.url).pathname === REFRESH ? lamMoiOk() : json(401, LOI_401)));
    const client = taoClient(fake.fn);
    await expect(api(client.GET(PROFILE))).rejects.toMatchObject({ errorCode: "UNAUTHENTICATED" });
    expect(fake.count(REFRESH)).toBe(1);
    expect(fake.count(PROFILE)).toBe(2);
  });

  it("lamMoi false thì gửi lại ngay, không chờ, chỉ một lời gọi làm mới; lời gọi làm mới có keepalive", async () => {
    vi.useFakeTimers();
    let keepalive: boolean | undefined;
    let daLamMoi = false;
    const fake = fakeFetch((request) => {
      if (new URL(request.url).pathname === REFRESH) {
        keepalive = request.keepalive;
        daLamMoi = true;
        return lamMoiOk(false);
      }
      return daLamMoi ? hoSo("Vũ Cư Dân") : json(401, LOI_401);
    });
    const client = taoClient(fake.fn);

    // Không tua đồng hồ giả: nếu còn chờ bằng setTimeout thì promise này không bao giờ xong
    await expect(api(client.GET(PROFILE))).resolves.toEqual({ hoTen: "Vũ Cư Dân" });
    expect(fake.count(REFRESH)).toBe(1);
    expect(fake.count(PROFILE)).toBe(2);
    expect(keepalive).toBe(true);
  });

  it("làm mới trả 500 thì nơi gọi nhận ApiError, không chuyển trang, lần sau vẫn làm mới được", async () => {
    const assign = vi.spyOn(authNavigation, "assign").mockImplementation(() => {});
    let loi = true;
    const fake = fakeFetch((request) => {
      if (new URL(request.url).pathname === REFRESH) {
        return loi ? json(500, { success: false, code: 500, errorCode: "INTERNAL_ERROR", message: "x" }) : lamMoiOk();
      }
      return fake.count(REFRESH) === 2 ? hoSo("Vũ Cư Dân") : json(401, LOI_401);
    });
    const client = taoClient(fake.fn);
    const error = await api(client.GET(PROFILE)).catch((e: unknown) => e);
    expect(error).toMatchObject({ errorCode: "INTERNAL_ERROR", status: 500 });
    handleAuthError(error);
    expect(assign).not.toHaveBeenCalled();

    loi = false;
    await expect(api(client.GET(PROFILE))).resolves.toEqual({ hoTen: "Vũ Cư Dân" });
    assign.mockRestore();
  });

  it("mất mạng khi làm mới thì BACKEND_UNAVAILABLE, và promise chung đã được xóa", async () => {
    let matMang = true;
    const fake = fakeFetch((request) => {
      if (new URL(request.url).pathname === REFRESH) {
        if (matMang) {
          throw new TypeError("Failed to fetch");
        }
        return lamMoiOk();
      }
      return json(401, LOI_401);
    });
    const client = taoClient(fake.fn);
    await expect(api(client.GET(PROFILE))).rejects.toMatchObject({ errorCode: "BACKEND_UNAVAILABLE" });
    matMang = false;
    await expect(lamMoiPhien(fake.fn, "http://localhost")).resolves.toEqual({ ok: true });
  });

  it("401 có errorCode khác UNAUTHENTICATED thì không làm mới", async () => {
    const fake = fakeFetch(() => json(401, { ...LOI_401, errorCode: "AUTH_INVALID_CREDENTIALS" }));
    const client = taoClient(fake.fn);
    await expect(api(client.GET(PROFILE))).rejects.toBeInstanceOf(ApiError);
    expect(fake.count(REFRESH)).toBe(0);
  });
});

describe("Làm mới thất bại thì về trang đăng nhập (AC-11)", () => {
  let assign: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    assign = vi.spyOn(authNavigation, "assign").mockImplementation(() => {});
    window.history.pushState({}, "", "/ho-so");
  });

  afterEach(() => {
    assign.mockRestore();
  });

  it("AUTH_REFRESH_INVALID chuyển tới het-phien đúng một lần dù hai request cùng lỗi", async () => {
    const fake = fakeFetch((request) =>
      new URL(request.url).pathname === REFRESH
        ? json(401, { ...LOI_401, errorCode: "AUTH_REFRESH_INVALID" })
        : json(401, LOI_401),
    );
    const client = taoClient(fake.fn);
    const loi = await Promise.all([
      api(client.GET(PROFILE)).catch((e: unknown) => e),
      api(client.GET(PROFILE)).catch((e: unknown) => e),
    ]);
    expect(loi).toEqual([
      expect.objectContaining({ errorCode: "AUTH_REFRESH_INVALID" }),
      expect.objectContaining({ errorCode: "AUTH_REFRESH_INVALID" }),
    ]);
    loi.forEach((e) => handleAuthError(e));
    expect(assign).toHaveBeenCalledTimes(1);
    expect(assign).toHaveBeenCalledWith("/dang-nhap?next=%2Fho-so&ly-do=het-phien");
  });

  it("AUTH_ACCOUNT_LOCKED chuyển tới ly-do=khoa", async () => {
    const fake = fakeFetch((request) =>
      new URL(request.url).pathname === REFRESH
        ? json(403, { ...LOI_401, code: 403, errorCode: "AUTH_ACCOUNT_LOCKED" })
        : json(401, LOI_401),
    );
    const client = taoClient(fake.fn);
    const error = await api(client.GET(PROFILE)).catch((e: unknown) => e);
    handleAuthError(error);
    expect(assign).toHaveBeenCalledWith("/dang-nhap?ly-do=khoa");
  });
});
