import type { MiddlewareCallbackParams } from "openapi-fetch";
import { describe, expect, it } from "vitest";
import { api, ApiError, csrfHeaderMiddleware, docRetryAfter } from "./api";

async function run(method: string): Promise<Request> {
  const request = new Request("http://localhost/api/v1/auth/login", { method });
  const params = { request, schemaPath: "", params: {}, id: "1", options: {} } as unknown as MiddlewareCallbackParams;
  return (await csrfHeaderMiddleware.onRequest!(params)) as Request;
}

describe("client gắn header chống CSRF (AC-10)", () => {
  it.each(["POST", "PUT", "PATCH", "DELETE", "GET"])(
    "request %s có X-Requested-With: XMLHttpRequest",
    async (method) => {
      const request = await run(method);
      expect(request.headers.get("X-Requested-With")).toBe("XMLHttpRequest");
    },
  );
});

describe("api đọc Retry-After (spec 0009, AC-15)", () => {
  function loi429(retryAfter?: string) {
    const headers = new Headers(retryAfter === undefined ? {} : { "Retry-After": retryAfter });
    return Promise.resolve({
      error: { errorCode: "AUTH_TOO_MANY_ATTEMPTS", message: "x", errors: [], requestId: "r1" },
      response: new Response(null, { status: 429, headers }),
    });
  }

  it.each([
    ["720", 720],
    ["Wed, 21 Oct 2026 07:28:00 GMT", undefined],
    ["0", undefined],
    ["-5", undefined],
    ["1.5", undefined],
    [undefined, undefined],
  ])("Retry-After %s thành retryAfter %s", async (header, expected) => {
    const error = await api(loi429(header)).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).errorCode).toBe("AUTH_TOO_MANY_ATTEMPTS");
    expect((error as ApiError).retryAfter).toBe(expected);
  });

  it("docRetryAfter bỏ khoảng trắng", () => {
    expect(docRetryAfter(" 60 ")).toBe(60);
    expect(docRetryAfter(null)).toBeUndefined();
  });
});
