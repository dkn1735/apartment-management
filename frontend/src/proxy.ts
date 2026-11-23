import { NextResponse, type NextRequest } from "next/server";
import { safeNext, TRANG_DANG_NHAP } from "@/lib/auth/safe-next";

/** Cookie dấu có phiên do backend đặt (spec 0004, mục Cookie); không chứa bí mật. */
export const COOKIE_PHIEN = "phien";

/**
 * Chặn trang được bảo vệ khi chưa đăng nhập (spec 0004, AC-13). Chỉ là kiểm tra lạc quan theo
 * hướng dẫn của Next.js 16: đọc cookie `phien`, không gọi backend, không kiểm chữ ký. Backend vẫn
 * kiểm access token ở mọi API.
 */
export function proxy(request: NextRequest) {
  if (request.cookies.has(COOKIE_PHIEN)) {
    return NextResponse.next();
  }
  // Điều hướng phía client của Next.js mang tham số nội bộ _rsc; không đưa nó vào next
  const params = new URLSearchParams(request.nextUrl.searchParams);
  params.delete("_rsc");
  const query = params.toString();
  const dich = request.nextUrl.pathname + (query ? `?${query}` : "");

  const url = new URL(TRANG_DANG_NHAP, request.url);
  url.searchParams.set("next", safeNext(dich));
  return NextResponse.redirect(url);
}

export const config = {
  // Mọi trang trừ /api, tài nguyên tĩnh, file có đuôi, /dang-nhap, hai trang quên mật khẩu (spec 0010) và /dev/**
  matcher: [
    "/((?!api/|api$|_next/static|_next/image|favicon\\.ico|dang-nhap|quen-mat-khau|dat-lai-mat-khau|dev/|dev$|.*\\.[A-Za-z0-9]+$).*)",
  ],
};
