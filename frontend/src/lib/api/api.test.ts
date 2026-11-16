import type { MiddlewareCallbackParams } from "openapi-fetch";
import { describe, expect, it } from "vitest";
import { csrfHeaderMiddleware } from "./api";

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
