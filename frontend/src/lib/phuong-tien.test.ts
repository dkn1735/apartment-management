import { describe, expect, it } from "vitest";
import {
  canNhapLyDoKhiChuyen,
  chuanHoaBienSo,
  coTheDangKyPhuongTien,
  hienKhoiPhuongTien,
  trangThaiDichHopLe,
} from "./phuong-tien";

describe("chuanHoaBienSo", () => {
  it("đưa biển số về dạng chuẩn viết hoa, bỏ khoảng trắng, dấu chấm và gạch nối", () => {
    expect(chuanHoaBienSo(" 30a-123.45 ")).toBe("30A12345");
    expect(chuanHoaBienSo("29\u00A0b1 - 678.90")).toBe("29B167890");
    expect(chuanHoaBienSo("   ")).toBe("");
  });
});

describe("trangThaiDichHopLe", () => {
  it("trả đúng ma trận chuyển trạng thái AC-10", () => {
    expect(trangThaiDichHopLe("Chờ duyệt")).toEqual(["Đang hoạt động", "Từ chối"]);
    expect(trangThaiDichHopLe("Đang hoạt động")).toEqual(["Tạm dừng", "Đã hủy"]);
    expect(trangThaiDichHopLe("Tạm dừng")).toEqual(["Đang hoạt động", "Đã hủy"]);
    expect(trangThaiDichHopLe("Từ chối")).toEqual([]);
    expect(trangThaiDichHopLe("Đã hủy")).toEqual([]);
  });

  it("bắt buộc lý do khi chuyển sang Tạm dừng, Từ chối hoặc Đã hủy", () => {
    expect(canNhapLyDoKhiChuyen("Đang hoạt động")).toBe(false);
    expect(canNhapLyDoKhiChuyen("Tạm dừng")).toBe(true);
    expect(canNhapLyDoKhiChuyen("Từ chối")).toBe(true);
    expect(canNhapLyDoKhiChuyen("Đã hủy")).toBe(true);
  });
});

describe("phân quyền hiển thị khối và nút đăng ký phương tiện", () => {
  it("hiện khối trên trang nhân viên cho BQL, BQT, Kế toán và trên trang cư dân cho Cư dân", () => {
    expect(hienKhoiPhuongTien(["BAN_QUAN_LY"], true)).toBe(true);
    expect(hienKhoiPhuongTien(["BAN_QUAN_TRI"], true)).toBe(true);
    expect(hienKhoiPhuongTien(["KE_TOAN"], true)).toBe(true);
    expect(hienKhoiPhuongTien(["CU_DAN"], true)).toBe(false);
    expect(hienKhoiPhuongTien(["CU_DAN"], false)).toBe(true);
    expect(hienKhoiPhuongTien(["KE_TOAN", "CU_DAN"], false)).toBe(true);
  });

  it("chỉ cho BQL đăng ký trên trang nhân viên và chỉ cho cư dân không kiêm BQL gửi đơn trên trang cư dân", () => {
    expect(coTheDangKyPhuongTien(["BAN_QUAN_LY"], true)).toBe(true);
    expect(coTheDangKyPhuongTien(["KE_TOAN"], true)).toBe(false);
    expect(coTheDangKyPhuongTien(["BAN_QUAN_TRI"], true)).toBe(false);
    expect(coTheDangKyPhuongTien(["CU_DAN"], false)).toBe(true);
    expect(coTheDangKyPhuongTien(["KE_TOAN", "CU_DAN"], false)).toBe(true);
    expect(coTheDangKyPhuongTien(["BAN_QUAN_LY", "CU_DAN"], false)).toBe(false);
  });
});
