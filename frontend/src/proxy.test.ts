// @vitest-environment node
import { NextRequest } from "next/server";
import { getRedirectUrl, unstable_doesMiddlewareMatch } from "next/experimental/testing/server";
import { describe, expect, it } from "vitest";
import { config, proxy } from "./proxy";

const BASE = "http://localhost:3000";

function request(path: string, phien = false) {
  const headers = new Headers();
  if (phien) headers.set("cookie", "phien=1");
  return new NextRequest(new URL(path, BASE), { headers });
}

function matches(url: string) {
  return unstable_doesMiddlewareMatch({ config, url });
}

describe("proxy (AC-13)", () => {
  it.each(["/ho-so", "/", "/ho-so/chi-tiet", "/devices"])("chạy trên trang được bảo vệ %s", (url) => {
    expect(matches(url)).toBe(true);
  });

  it.each([
    "/dang-nhap",
    "/dev",
    "/dev/ui",
    "/dev/ui/shell",
    "/api/v1/auth/profile",
    "/_next/static/a.js",
    "/favicon.ico",
    "/logo.png",
  ])("không chạy trên %s", (url) => {
    expect(matches(url)).toBe(false);
  });

  it("chưa có cookie phien thì chuyển về /dang-nhap kèm next có query", () => {
    const response = proxy(request("/ho-so?tab=vai-tro"));
    const url = new URL(getRedirectUrl(response)!);
    expect(url.pathname).toBe("/dang-nhap");
    expect(url.searchParams.get("next")).toBe("/ho-so?tab=vai-tro");
  });

  it("bỏ tham số nội bộ _rsc khỏi next", () => {
    const url = new URL(getRedirectUrl(proxy(request("/ho-so?_rsc=abc123&tab=1")))!);
    expect(url.searchParams.get("next")).toBe("/ho-so?tab=1");
  });

  it("trang / chuyển về đăng nhập với next=/", () => {
    const url = new URL(getRedirectUrl(proxy(request("/")))!);
    expect(url.searchParams.get("next")).toBe("/");
  });

  it("có cookie phien thì cho qua", () => {
    const response = proxy(request("/ho-so", true));
    expect(getRedirectUrl(response)).toBeNull();
    expect(response.headers.get("x-middleware-next")).toBe("1");
  });
});
