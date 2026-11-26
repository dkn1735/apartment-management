import { describe, expect, it } from "vitest";

import { duocThemToaNha, duocXemTang, khoaChiTietToaNha, soTangGuiDi, TOA_NHA_QUERY_KEY } from "./toa-nha";

describe("soTangGuiDi", () => {
  // covers: AC-15 (gửi số nguyên khi giá trị là chữ số)
  it("đổi chuỗi toàn chữ số thành số nguyên, bỏ khoảng trắng hai đầu", () => {
    expect(soTangGuiDi("5")).toBe(5);
    expect(soTangGuiDi(" 12 ")).toBe(12);
    expect(soTangGuiDi("007")).toBe(7);
  });

  // covers: AC-15 (gửi nguyên chuỗi khi không phải chữ số để server trả lỗi trường)
  it.each(["abc", "3.0", "-1", "1e2", "3 4", ""])("gửi nguyên chuỗi %j", (nhap) => {
    expect(soTangGuiDi(nhap)).toBe(nhap.trim());
  });

  it("không đổi số ngoài 1 đến 99 thành lỗi, để server kiểm", () => {
    expect(soTangGuiDi("0")).toBe(0);
    expect(soTangGuiDi("100")).toBe(100);
  });
});

describe("duocThemToaNha", () => {
  // covers: AC-15, AC-17 (nút thêm chỉ cho BAN_QUAN_LY)
  it("chỉ đúng khi có BAN_QUAN_LY", () => {
    expect(duocThemToaNha(["BAN_QUAN_LY"])).toBe(true);
    expect(duocThemToaNha(["BAN_QUAN_TRI", "BAN_QUAN_LY"])).toBe(true);
    expect(duocThemToaNha(["KY_THUAT"])).toBe(false);
    expect(duocThemToaNha([])).toBe(false);
    expect(duocThemToaNha(undefined)).toBe(false);
  });
});

describe("duocXemTang", () => {
  // covers: AC-16 (tên tòa là link cho BAN_QUAN_LY hoặc KY_THUAT)
  it("đúng với BAN_QUAN_LY hoặc KY_THUAT, sai với vai trò khác", () => {
    expect(duocXemTang(["BAN_QUAN_LY"])).toBe(true);
    expect(duocXemTang(["KY_THUAT"])).toBe(true);
    expect(duocXemTang(["KE_TOAN", "BAN_QUAN_TRI"])).toBe(false);
    expect(duocXemTang(["CU_DAN"])).toBe(false);
    expect(duocXemTang(undefined)).toBe(false);
  });
});

describe("khoaChiTietToaNha", () => {
  // covers: AC-15, AC-17 (invalidate gốc toa-nha làm mới cả chi tiết)
  it("nằm dưới gốc khóa query toa-nha", () => {
    const khoa = khoaChiTietToaNha("B");
    expect(khoa.slice(0, TOA_NHA_QUERY_KEY.length)).toEqual([...TOA_NHA_QUERY_KEY]);
    expect(khoa).toContain("B");
  });
});
