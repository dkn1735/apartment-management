import { QueryClient } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import { HO_SO_QUERY_KEY } from "@/lib/auth/use-ho-so";
import { CAN_HO_QUERY_KEY } from "@/lib/can-ho";
import {
  coTheSuaHoSoCuTru,
  CU_DAN_LUA_CHON_QUERY_KEY,
  hienKhoiHoSoCuTru,
  HO_SO_CU_TRU_QUERY_KEY,
  khoaHoSoCuTru,
  laHoSoChuaBatDau,
  lamMoiSauThayDoiCuTru,
  TINH_TRANG_CU_TRU,
  toneTrangThaiHoSo,
  toneVaiTroCuTru,
  TRANG_THAI_HO_SO,
  type TrangThaiHoSo,
  VAI_TRO_CU_TRU,
  VAI_TRO_DE_NGHI_CU_TRU,
  type VaiTroCuTru,
} from "./cu-tru";

describe("khóa query và danh mục cư trú (spec 0014, 0015)", () => {
  // covers: AC-16, AC-17
  it("ghép khóa query hồ sơ cư trú theo mã căn hộ", () => {
    expect(khoaHoSoCuTru("A-0101")).toEqual(["ho-so-cu-tru", "A-0101"]);
    expect(HO_SO_CU_TRU_QUERY_KEY).toEqual(["ho-so-cu-tru"]);
    expect(CU_DAN_LUA_CHON_QUERY_KEY).toEqual(["cu-dan-lua-chon"]);
  });

  // covers: AC-15, AC-16, AC-17
  it("giữ đúng danh sách vai trò, vai trò đề nghị, tình trạng và năm trạng thái hồ sơ", () => {
    expect(VAI_TRO_CU_TRU).toEqual(["Chủ căn hộ", "Thành viên gia đình", "Khách thuê"]);
    expect(VAI_TRO_DE_NGHI_CU_TRU).toEqual(["Thành viên gia đình", "Khách thuê"]);
    expect(TINH_TRANG_CU_TRU).toEqual(["Thường trú", "Tạm trú"]);
    expect(TRANG_THAI_HO_SO).toEqual(["Chờ xác minh", "Hợp lệ", "Hết hiệu lực", "Từ chối", "Đã hủy"]);
  });
});

describe("tone badge cư trú (spec 0014, 0015)", () => {
  // covers: AC-16
  it("ánh xạ đúng màu cho từng vai trò cư trú và an toàn với giá trị lạ", () => {
    expect(toneVaiTroCuTru("Chủ căn hộ")).toBe("info");
    expect(toneVaiTroCuTru("Thành viên gia đình")).toBe("neutral");
    expect(toneVaiTroCuTru("Khách thuê")).toBe("warning");
    expect(toneVaiTroCuTru("Khác" as VaiTroCuTru)).toBe("neutral");
  });

  // covers: AC-15, AC-16
  it("ánh xạ đúng màu cho năm trạng thái hồ sơ và an toàn với giá trị lạ", () => {
    expect(toneTrangThaiHoSo("Chờ xác minh")).toBe("warning");
    expect(toneTrangThaiHoSo("Hợp lệ")).toBe("success");
    expect(toneTrangThaiHoSo("Hết hiệu lực")).toBe("neutral");
    expect(toneTrangThaiHoSo("Từ chối")).toBe("danger");
    expect(toneTrangThaiHoSo("Đã hủy")).toBe("neutral");
    expect(toneTrangThaiHoSo("Khác" as TrangThaiHoSo)).toBe("neutral");
  });
});

describe("hienKhoiHoSoCuTru (spec 0014, AC-15)", () => {
  // covers: AC-15
  it("trên trang quản lý căn hộ chỉ hiện khi người dùng có vai trò BAN_QUAN_LY", () => {
    expect(hienKhoiHoSoCuTru(["BAN_QUAN_LY"], true)).toBe(true);
    expect(hienKhoiHoSoCuTru(["CU_DAN", "BAN_QUAN_LY"], true)).toBe(true);
    expect(hienKhoiHoSoCuTru(["KE_TOAN"], true)).toBe(false);
    expect(hienKhoiHoSoCuTru(["BAN_QUAN_TRI"], true)).toBe(false);
    expect(hienKhoiHoSoCuTru(["BAN_QUAN_TRI", "CU_DAN"], true)).toBe(false);
    expect(hienKhoiHoSoCuTru(["KE_TOAN", "CU_DAN"], true)).toBe(false);
    expect(hienKhoiHoSoCuTru([], true)).toBe(false);
    expect(hienKhoiHoSoCuTru(undefined, true)).toBe(false);
  });

  // covers: AC-15
  it("trên trang căn hộ của tôi chỉ hiện khi người dùng có vai trò CU_DAN", () => {
    expect(hienKhoiHoSoCuTru(["CU_DAN"], false)).toBe(true);
    expect(hienKhoiHoSoCuTru(["BAN_QUAN_TRI", "CU_DAN"], false)).toBe(true);
    expect(hienKhoiHoSoCuTru(["BAN_QUAN_LY"], false)).toBe(false);
    expect(hienKhoiHoSoCuTru(["KE_TOAN", "BAN_QUAN_TRI"], false)).toBe(false);
    expect(hienKhoiHoSoCuTru([], false)).toBe(false);
    expect(hienKhoiHoSoCuTru(undefined, false)).toBe(false);
  });
});

describe("điều kiện thao tác dòng trên /ho-so-cu-tru (spec 0015, AC-17)", () => {
  // covers: AC-17
  it("chỉ cho sửa hồ sơ Hợp lệ chưa qua ngày kết thúc", () => {
    expect(coTheSuaHoSoCuTru({ trangThaiHoSo: "Hợp lệ", ngayKetThuc: null }, "2026-11-26")).toBe(true);
    expect(coTheSuaHoSoCuTru({ trangThaiHoSo: "Hợp lệ", ngayKetThuc: "2026-11-26" }, "2026-11-26")).toBe(true);
    expect(coTheSuaHoSoCuTru({ trangThaiHoSo: "Hợp lệ", ngayKetThuc: "2026-11-25" }, "2026-11-26")).toBe(false);
    expect(coTheSuaHoSoCuTru({ trangThaiHoSo: "Chờ xác minh", ngayKetThuc: null }, "2026-11-26")).toBe(false);
    expect(coTheSuaHoSoCuTru({ trangThaiHoSo: "Hết hiệu lực", ngayKetThuc: "2026-12-31" }, "2026-11-26")).toBe(false);
  });

  // covers: AC-17
  it("nhận biết hồ sơ chưa tới ngày bắt đầu để hiện nhãn Hủy hồ sơ", () => {
    expect(laHoSoChuaBatDau({ ngayBatDau: "2026-11-27" }, "2026-11-26")).toBe(true);
    expect(laHoSoChuaBatDau({ ngayBatDau: "2026-11-26" }, "2026-11-26")).toBe(false);
    expect(laHoSoChuaBatDau({ ngayBatDau: "2026-01-01" }, "2026-11-26")).toBe(false);
  });
});

describe("lamMoiSauThayDoiCuTru (spec 0015, AC-17)", () => {
  // covers: AC-17
  it("làm mới gốc ho-so-cu-tru, thẻ căn hộ và hồ sơ phiên hiện tại", () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, "invalidateQueries");

    lamMoiSauThayDoiCuTru(queryClient, "A-0102");

    expect(spy).toHaveBeenCalledTimes(3);
    expect(spy).toHaveBeenCalledWith({ queryKey: ["ho-so-cu-tru"] });
    expect(spy).toHaveBeenCalledWith({ queryKey: CAN_HO_QUERY_KEY });
    expect(spy).toHaveBeenCalledWith({ queryKey: HO_SO_QUERY_KEY });
  });
});
