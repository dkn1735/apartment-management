import { describe, expect, it } from "vitest";
import { ApiError } from "./api";
import { messageFor, requestIdToShow, thongBaoKhoaTam } from "./error-messages";

const error = (status: number, errorCode: string, requestId?: string) =>
  new ApiError({ status, errorCode, message: "raw", requestId });

describe("messageFor", () => {
  it("dùng câu trong từ điển theo errorCode", () => {
    expect(messageFor(error(409, "CONFLICT"))).toBe(
      "Dữ liệu bị trùng hoặc đang ở trạng thái không cho phép thao tác này.",
    );
  });

  it("mã chưa có thì dùng câu chung theo mã HTTP", () => {
    expect(messageFor(error(422, "XYZ"))).toBe("Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.");
    expect(messageFor(error(503, "XYZ"))).toBe("Hệ thống đang gặp sự cố, vui lòng thử lại sau.");
    expect(messageFor(error(0, "toString"))).toBe("Hệ thống đang gặp sự cố, vui lòng thử lại sau.");
  });
});

describe("requestIdToShow", () => {
  it("hiện mã cho 5xx và BACKEND_UNAVAILABLE, không cho 4xx", () => {
    expect(requestIdToShow(error(500, "INTERNAL_ERROR", "abc"))).toBe("abc");
    expect(requestIdToShow(error(0, "BACKEND_UNAVAILABLE", "abc"))).toBe("abc");
    expect(requestIdToShow(error(409, "CONFLICT", "abc"))).toBeUndefined();
    expect(requestIdToShow(error(500, "INTERNAL_ERROR"))).toBeUndefined();
  });
});

describe("thông báo khóa tạm (spec 0009, AC-13)", () => {
  it.each([
    [720, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 12 phút hoặc liên hệ ban quản lý."],
    [721, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 13 phút hoặc liên hệ ban quản lý."],
    [1, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 1 phút hoặc liên hệ ban quản lý."],
    [undefined, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút hoặc liên hệ ban quản lý."],
  ])("retryAfter %s", (retryAfter, text) => {
    expect(thongBaoKhoaTam(retryAfter)).toBe(text);
    expect(
      messageFor(new ApiError({ status: 429, errorCode: "AUTH_TOO_MANY_ATTEMPTS", message: "x", retryAfter })),
    ).toBe(text);
  });
});
