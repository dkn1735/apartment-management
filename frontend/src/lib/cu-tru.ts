import type { QueryClient } from "@tanstack/react-query";
import type { StatusTone } from "@/components/data/status-badge";
import type { components } from "@/lib/api/schema";
import { HO_SO_QUERY_KEY } from "@/lib/auth/use-ho-so";
import { CAN_HO_QUERY_KEY } from "@/lib/can-ho";
import { ngayHomNay } from "@/lib/format";
import type { VaiTro } from "@/lib/vai-tro";

export type HoSoCuTru = components["schemas"]["HoSoCuTruResponse"];
export type HoSoCuTruCanHo = components["schemas"]["HoSoCuTruCanHoResponse"];
export type CuDanLuaChon = components["schemas"]["CuDanLuaChonResponse"];
export type VaiTroCuTru = HoSoCuTru["vaiTroCuTru"];
export type VaiTroDeNghiCuTru = components["schemas"]["GuiDeNghiCuTruRequest"]["vaiTroCuTru"];
export type TinhTrangCuTru = HoSoCuTru["tinhTrangCuTru"];
export type TrangThaiHoSo = HoSoCuTru["trangThaiHoSo"];

/** Gốc khóa query hồ sơ cư trú, phủ cả chi tiết căn, danh sách `/ho-so-cu-tru` và số đếm chờ xác minh (AC-17). */
export const HO_SO_CU_TRU_QUERY_KEY = ["ho-so-cu-tru"] as const;

export const khoaHoSoCuTru = (maCanHo: string) => [...HO_SO_CU_TRU_QUERY_KEY, maCanHo] as const;

export const CU_DAN_LUA_CHON_QUERY_KEY = ["cu-dan-lua-chon"] as const;

export const VAI_TRO_CU_TRU: readonly VaiTroCuTru[] = ["Chủ căn hộ", "Thành viên gia đình", "Khách thuê"];

export const VAI_TRO_DE_NGHI_CU_TRU: readonly VaiTroDeNghiCuTru[] = ["Thành viên gia đình", "Khách thuê"];

export const TINH_TRANG_CU_TRU: readonly TinhTrangCuTru[] = ["Thường trú", "Tạm trú"];

export const TRANG_THAI_HO_SO: readonly TrangThaiHoSo[] = [
  "Chờ xác minh",
  "Hợp lệ",
  "Hết hiệu lực",
  "Từ chối",
  "Đã hủy",
];

const TONE_VAI_TRO: Record<VaiTroCuTru, StatusTone> = {
  "Chủ căn hộ": "info",
  "Thành viên gia đình": "neutral",
  "Khách thuê": "warning",
};

const TONE_TRANG_THAI_HO_SO: Record<TrangThaiHoSo, StatusTone> = {
  "Chờ xác minh": "warning",
  "Hợp lệ": "success",
  "Hết hiệu lực": "neutral",
  "Từ chối": "danger",
  "Đã hủy": "neutral",
};

export function toneVaiTroCuTru(vaiTro: VaiTroCuTru): StatusTone {
  return TONE_VAI_TRO[vaiTro] ?? "neutral";
}

export function toneTrangThaiHoSo(trangThai: TrangThaiHoSo): StatusTone {
  return TONE_TRANG_THAI_HO_SO[trangThai] ?? "neutral";
}

/**
 * Điều kiện tải và hiện khối Hồ sơ cư trú trên `ChiTietCanHo` (spec 0014, AC-15):
 * trang quản lý căn hộ (`choPhepSua = true`) chỉ hiện với `BAN_QUAN_LY`;
 * trang của cư dân (`choPhepSua = false`) chỉ hiện với `CU_DAN`.
 */
export function hienKhoiHoSoCuTru(vaiTro: readonly VaiTro[] | undefined, choPhepSua: boolean): boolean {
  if (!vaiTro) return false;
  return choPhepSua ? vaiTro.includes("BAN_QUAN_LY") : vaiTro.includes("CU_DAN");
}

/** Hồ sơ hợp lệ chưa quá hạn kết thúc thì BQL được sửa trên `/ho-so-cu-tru` (spec 0015, AC-17). */
export function coTheSuaHoSoCuTru(
  hoSo: Pick<HoSoCuTru, "trangThaiHoSo" | "ngayKetThuc">,
  homNay = ngayHomNay(),
): boolean {
  return hoSo.trangThaiHoSo === "Hợp lệ" && (hoSo.ngayKetThuc === null || hoSo.ngayKetThuc >= homNay);
}

/** Hồ sơ chưa tới ngày bắt đầu thì nhãn đóng là "Hủy hồ sơ", ngược lại là "Kết thúc cư trú" (spec 0015, AC-17). */
export function laHoSoChuaBatDau(hoSo: Pick<HoSoCuTru, "ngayBatDau">, homNay = ngayHomNay()): boolean {
  return hoSo.ngayBatDau > homNay;
}

/**
 * Làm mới toàn bộ nhánh `HO_SO_CU_TRU_QUERY_KEY` (chi tiết căn, danh sách `/ho-so-cu-tru`, số đếm chờ xác minh),
 * thẻ trạng thái căn và phạm vi căn của hồ sơ sau mọi thay đổi cư trú (spec 0015, AC-17).
 */
export function lamMoiSauThayDoiCuTru(queryClient: QueryClient, _maCanHo?: string): void {
  void queryClient.invalidateQueries({ queryKey: HO_SO_CU_TRU_QUERY_KEY });
  void queryClient.invalidateQueries({ queryKey: CAN_HO_QUERY_KEY });
  void queryClient.invalidateQueries({ queryKey: HO_SO_QUERY_KEY });
}
