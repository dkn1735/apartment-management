import { describe, expect, it } from "vitest";
import {
  chuanHoaDienTich,
  dienTichGuiDi,
  dienTichNhap,
  doDaiLyDo,
  duocDoiTienIch,
  duocQuanLyCanHo,
  formatDienTich,
  khoaChiTietCanHo,
  LY_DO_TOI_DA,
  maCanSeTao,
  toneTrangThai,
  TRANG_THAI_SU_DUNG,
  TRANG_THAI_TAO_CAN_HO,
} from "./can-ho";

describe("diện tích (spec 0012, AC-17, AC-18)", () => {
  it("hiện theo vi-VN với 2 chữ số thập phân", () => {
    expect(formatDienTich(75.5)).toBe("75,50 m²");
    expect(formatDienTich(1000)).toBe("1.000,00 m²");
    expect(dienTichNhap(75.5)).toBe("75,50");
  });

  it("đổi dấu phẩy thành dấu chấm trước khi gửi, gửi số khi là số thập phân", () => {
    expect(chuanHoaDienTich(" 75,5 ")).toBe("75.5");
    expect(dienTichGuiDi("75,5")).toBe(75.5);
    expect(dienTichGuiDi("80")).toBe(80);
  });

  it.each(["abc", "1e2", "", "-5", "7,5,1"])("gửi nguyên chuỗi %j để server trả lỗi trường", (nhap) => {
    expect(typeof dienTichGuiDi(nhap)).toBe("string");
  });
});

describe("maCanSeTao (AC-18)", () => {
  it("ghép mã tầng với số căn hai chữ số", () => {
    expect(maCanSeTao("A-01", "5")).toBe("A-0105");
    expect(maCanSeTao("A-01", " 12 ")).toBe("A-0112");
    expect(maCanSeTao("A-01", "05")).toBe("A-0105");
  });

  it.each(["", "0", "100", "abc", "1.5"])("không xem trước với số căn %j", (soCan) => {
    expect(maCanSeTao("A-01", soCan)).toBeUndefined();
  });

  it("không xem trước khi chưa chọn tầng", () => {
    expect(maCanSeTao(undefined, "5")).toBeUndefined();
  });
});

describe("duocQuanLyCanHo (AC-18, AC-19)", () => {
  it("chỉ đúng khi có BAN_QUAN_LY", () => {
    expect(duocQuanLyCanHo(["BAN_QUAN_LY"])).toBe(true);
    expect(duocQuanLyCanHo(["KE_TOAN", "BAN_QUAN_TRI"])).toBe(false);
    expect(duocQuanLyCanHo(undefined)).toBe(false);
  });
});

describe("duocDoiTienIch (spec 0013, AC-15)", () => {
  // covers: AC-15
  it("đúng khi có BAN_QUAN_LY hoặc KE_TOAN", () => {
    expect(duocDoiTienIch(["BAN_QUAN_LY"])).toBe(true);
    expect(duocDoiTienIch(["KE_TOAN"])).toBe(true);
    expect(duocDoiTienIch(["CU_DAN", "KE_TOAN"])).toBe(true);
  });

  it("sai với vai trò khác hoặc chưa có hồ sơ", () => {
    expect(duocDoiTienIch(["BAN_QUAN_TRI", "KY_THUAT", "CU_DAN", "BAO_VE", "QUAN_TRI_VIEN"])).toBe(false);
    expect(duocDoiTienIch([])).toBe(false);
    expect(duocDoiTienIch(undefined)).toBe(false);
  });
});

describe("doDaiLyDo (spec 0013, AC-3, AC-15)", () => {
  // covers: AC-3, AC-15 (đếm code point sau khi cắt hai đầu, như server)
  it("đếm theo code point sau khi cắt khoảng trắng hai đầu", () => {
    expect(doDaiLyDo("")).toBe(0);
    expect(doDaiLyDo("   \n ")).toBe(0);
    expect(doDaiLyDo(" Nợ phí ")).toBe(6);
    expect(doDaiLyDo("😀😀")).toBe(2);
    expect(doDaiLyDo("a\nb")).toBe(3);
  });

  it("500 emoji vừa đủ giới hạn", () => {
    expect(doDaiLyDo("😀".repeat(LY_DO_TOI_DA))).toBe(LY_DO_TOI_DA);
  });
});

describe("trạng thái tạo căn hộ và khóa query (spec 0014, AC-10, AC-17)", () => {
  // covers: AC-10, AC-17
  it("loại bỏ Đang sử dụng khỏi lựa chọn khi tạo căn mới nhưng giữ đủ bốn trạng thái chung", () => {
    expect(TRANG_THAI_TAO_CAN_HO).toEqual(["Trống", "Đang sửa chữa", "Đã bàn giao"]);
    expect(TRANG_THAI_TAO_CAN_HO).not.toContain("Đang sử dụng");
    expect(TRANG_THAI_SU_DUNG).toEqual(["Trống", "Đang sử dụng", "Đang sửa chữa", "Đã bàn giao"]);
  });

  it("ánh xạ đúng tone trạng thái căn hộ và khóa chi tiết căn", () => {
    expect(toneTrangThai("Trống")).toBe("neutral");
    expect(toneTrangThai("Đang sử dụng")).toBe("success");
    expect(toneTrangThai("Đang sửa chữa")).toBe("warning");
    expect(toneTrangThai("Đã bàn giao")).toBe("info");
    expect(khoaChiTietCanHo("A-0101")).toEqual(["can-ho", "chi-tiet", "A-0101"]);
  });
});
