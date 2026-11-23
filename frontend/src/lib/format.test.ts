import { describe, expect, it } from "vitest";
import { formatGioNgay, formatNgay, formatNgayGio, formatSo, formatTien } from "./format";

describe("định dạng (AC-11), chạy ở múi giờ America/New_York", () => {
  it("múi giờ của bộ test khác Việt Nam", () => {
    expect(new Date("2026-11-16T07:05:00Z").getTimezoneOffset()).toBe(300);
  });

  it("tiền có NBSP trước ₫", () => {
    expect(formatTien(4850000)).toBe("4.850.000 ₫");
    expect(formatTien(0)).toBe("0 ₫");
    expect(formatSo(135000)).toBe("135.000");
  });

  it("ngày giờ theo Asia/Ho_Chi_Minh, 24 giờ, ngày trước giờ", () => {
    expect(formatNgayGio("2026-11-16T07:05:00Z")).toBe("16/11/2026 14:05");
    expect(formatNgayGio("2026-11-16T14:05:00+07:00")).toBe("16/11/2026 14:05");
    expect(formatNgayGio("2026-11-16T17:30:00Z")).toBe("17/11/2026 00:30");
  });

  it("LocalDate không bị lệch ngày", () => {
    expect(formatNgay("2026-11-16")).toBe("16/11/2026");
    expect(formatNgay("2026-01-01")).toBe("01/01/2026");
    expect(formatNgay("2026-11-16T20:00:00Z")).toBe("17/11/2026");
  });

  it("null, undefined, không hợp lệ cho ra —", () => {
    expect(formatNgay(null)).toBe("—");
    expect(formatNgay(undefined)).toBe("—");
    expect(formatNgay("2026-02-30")).toBe("—");
    expect(formatNgay("abc")).toBe("—");
    expect(formatNgayGio("abc")).toBe("—");
    expect(formatNgayGio("2026-11-16")).toBe("—");
    expect(formatTien(null)).toBe("—");
    expect(formatTien(Number.NaN)).toBe("—");
  });
});

describe("formatGioNgay (spec 0009, AC-14)", () => {
  it("giờ trước, ngày sau, theo giờ Việt Nam", () => {
    expect(formatGioNgay("2026-11-16T01:15:00Z")).toBe("08:15 16/11/2026");
    expect(formatGioNgay("2026-11-16T23:30:00Z")).toBe("06:30 17/11/2026");
    expect(formatGioNgay(null)).toBe("—");
    expect(formatGioNgay("khong-hop-le")).toBe("—");
  });
});
