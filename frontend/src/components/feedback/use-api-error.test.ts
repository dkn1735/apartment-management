import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/api";
import { useApiError } from "./use-api-error";

const conflict = () => new ApiError({ status: 409, errorCode: "CONFLICT", message: "raw" });

describe("useApiError (AC-6)", () => {
  it("fn chạy xong thì capture trả true và không có lỗi", async () => {
    const { result } = renderHook(() => useApiError());

    let ok: boolean | undefined;
    await act(async () => {
      ok = await result.current.capture(async () => "xong");
    });

    expect(ok).toBe(true);
    expect(result.current.error).toBeNull();
    expect(result.current.focusKey).toBe(0);
  });

  it("bắt ApiError thì giữ lỗi, trả false và tăng focusKey mỗi lần", async () => {
    const { result } = renderHook(() => useApiError());
    const error = conflict();

    let ok: boolean | undefined;
    await act(async () => {
      ok = await result.current.capture(() => Promise.reject(error));
    });
    await act(async () => {
      await result.current.capture(() => Promise.reject(error));
    });

    expect(ok).toBe(false);
    expect(result.current.error).toBe(error);
    expect(result.current.focusKey).toBe(2);
  });

  it("lần capture sau xóa lỗi cũ khi thành công", async () => {
    const { result } = renderHook(() => useApiError());
    await act(async () => {
      await result.current.capture(() => Promise.reject(conflict()));
    });

    await act(async () => {
      await result.current.capture(async () => undefined);
    });

    expect(result.current.error).toBeNull();
  });

  it("lỗi không phải ApiError được ném tiếp", async () => {
    const { result } = renderHook(() => useApiError());

    await act(async () => {
      await expect(result.current.capture(() => Promise.reject(new TypeError("hỏng")))).rejects.toThrow("hỏng");
    });

    expect(result.current.error).toBeNull();
  });

  it("clear xóa lỗi đang giữ", async () => {
    const { result } = renderHook(() => useApiError());
    await act(async () => {
      await result.current.capture(() => Promise.reject(conflict()));
    });

    act(() => result.current.clear());

    expect(result.current.error).toBeNull();
  });
});
