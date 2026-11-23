import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/api";
import { doiTenTruongLoi, laLechPhienBan, loiTruong, TRUONG_CUA_VAI_TRO } from "./tai-khoan";

const moTa = (vaiTro: keyof typeof TRUONG_CUA_VAI_TRO, i = 0) => TRUONG_CUA_VAI_TRO[vaiTro][i]!;

describe("loiTruong: kiểm trường vai trò như backend (spec 0008, AC-4)", () => {
  it("trống hoặc chỉ khoảng trắng thì báo Vui lòng nhập, chữ đầu của nhãn viết thường", () => {
    expect(loiTruong(moTa("BAN_QUAN_LY"), "   ")).toBe("Vui lòng nhập chức vụ");
    expect(loiTruong(moTa("KY_THUAT"), "")).toBe("Vui lòng nhập chuyên môn kỹ thuật");
    expect(loiTruong(moTa("CU_DAN"), "")).toBe("Vui lòng nhập số CCCD");
  });

  it("số CCCD phải đúng 12 chữ số sau khi cắt khoảng trắng", () => {
    expect(loiTruong(moTa("CU_DAN"), " 001203004567 ")).toBeNull();
    for (const sai of ["00120300456", "0012030045678", "00120300456a", "001 203 004 567"]) {
      expect(loiTruong(moTa("CU_DAN"), sai)).toBe("Số CCCD phải gồm 12 chữ số");
    }
  });

  it("đúng giới hạn thì hợp lệ, vượt một ký tự thì báo tối đa", () => {
    const nhiemKy = moTa("BAN_QUAN_TRI");
    expect(loiTruong(nhiemKy, "x".repeat(50))).toBeNull();
    expect(loiTruong(nhiemKy, "x".repeat(51))).toBe("Nhiệm kỳ tối đa 50 ký tự");
    expect(loiTruong(moTa("BAO_VE"), "x".repeat(151))).toBe("Vị trí trực tối đa 150 ký tự");
  });

  it("khoảng trắng hai đầu không tính vào độ dài", () => {
    expect(loiTruong(moTa("BAN_QUAN_TRI"), `  ${"x".repeat(50)}  `)).toBeNull();
  });

  it("Quản trị viên không có trường riêng; mỗi trường chỉ thuộc một vai trò", () => {
    expect(TRUONG_CUA_VAI_TRO.QUAN_TRI_VIEN).toEqual([]);
    const ten = Object.values(TRUONG_CUA_VAI_TRO).flatMap((ds) => ds.map((t) => t.ten));
    expect(new Set(ten).size).toBe(ten.length);
  });
});

describe("doiTenTruongLoi: lỗi vaiTro[i] về tên trường của form (spec 0008, AC-3, AC-4)", () => {
  const loi = (fields: string[]) =>
    new ApiError({
      status: 409,
      errorCode: "AUTH_IDENTITY_TAKEN",
      message: "Thông tin đã được dùng cho tài khoản khác",
      requestId: "req-1",
      errors: fields.map((field) => ({ field, message: `lỗi ${field}` })),
    });

  it("vaiTro[i].truong thành tên trường trần, giữ câu báo", () => {
    const ra = doiTenTruongLoi(loi(["vaiTro[2].soCccd"])) as ApiError;
    expect(ra.errors).toEqual([{ field: "soCccd", message: "lỗi vaiTro[2].soCccd" }]);
  });

  it("vaiTro[i] trần và vaiTro[i].vaiTro thành vaiTro", () => {
    const ra = doiTenTruongLoi(loi(["vaiTro[0]", "vaiTro[13].vaiTro"])) as ApiError;
    expect(ra.errors.map((e) => e.field)).toEqual(["vaiTro", "vaiTro"]);
  });

  it("trường chung giữ nguyên, cùng status, errorCode, message và requestId", () => {
    const vao = loi(["soDienThoai", "email"]);
    const ra = doiTenTruongLoi(vao) as ApiError;
    expect(ra).toBeInstanceOf(ApiError);
    expect(ra.errors.map((e) => e.field)).toEqual(["soDienThoai", "email"]);
    expect([ra.status, ra.errorCode, ra.message, ra.requestId]).toEqual([
      409,
      "AUTH_IDENTITY_TAKEN",
      "Thông tin đã được dùng cho tài khoản khác",
      "req-1",
    ]);
  });

  it("lỗi không phải ApiError trả lại nguyên", () => {
    const e = new Error("mạng");
    expect(doiTenTruongLoi(e)).toBe(e);
  });
});

describe("laLechPhienBan (spec 0008, AC-7)", () => {
  it("chỉ đúng với ApiError mã CONCURRENT_MODIFICATION", () => {
    expect(laLechPhienBan(new ApiError({ status: 409, errorCode: "CONCURRENT_MODIFICATION", message: "x" }))).toBe(
      true,
    );
    expect(laLechPhienBan(new ApiError({ status: 409, errorCode: "AUTH_IDENTITY_TAKEN", message: "x" }))).toBe(false);
    expect(laLechPhienBan(new Error("CONCURRENT_MODIFICATION"))).toBe(false);
  });
});
