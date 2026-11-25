import type { StatusTone } from "@/components/data/status-badge";
import type { components } from "@/lib/api/schema";
import type { VaiTro } from "@/lib/vai-tro";

export type CanHo = components["schemas"]["CanHoResponse"];
export type TrangThaiSuDung = CanHo["trangThaiSuDung"];

/** Gốc khóa query của căn hộ; invalidate gốc này làm mới danh sách và chi tiết căn (spec 0012, AC-18, AC-19). */
export const CAN_HO_QUERY_KEY = ["can-ho"] as const;

export const khoaChiTietCanHo = (ma: string) => [...CAN_HO_QUERY_KEY, "chi-tiet", ma] as const;

/** Bốn trạng thái theo đúng chuỗi DB và thứ tự nghiệp vụ (spec 0012, Thuật ngữ). */
export const TRANG_THAI_SU_DUNG: readonly TrangThaiSuDung[] = ["Trống", "Đang sử dụng", "Đang sửa chữa", "Đã bàn giao"];

const TONE_TRANG_THAI: Record<TrangThaiSuDung, StatusTone> = {
  Trống: "neutral",
  "Đang sử dụng": "success",
  "Đang sửa chữa": "warning",
  "Đã bàn giao": "info",
};

export function toneTrangThai(trangThai: TrangThaiSuDung): StatusTone {
  return TONE_TRANG_THAI[trangThai] ?? "neutral";
}

const DIEN_TICH = new Intl.NumberFormat("vi-VN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** `75.5` thành `75,50 m²` (spec 0012, Value sourcing). */
export function formatDienTich(dienTich: number): string {
  return `${DIEN_TICH.format(dienTich)} m²`;
}

/** Giá trị đặt sẵn vào ô diện tích: `75.5` thành `75,50`. */
export function dienTichNhap(dienTich: number): string {
  return dienTich.toFixed(2).replace(".", ",");
}

/** Ô diện tích: cắt khoảng trắng, đổi dấu phẩy thành dấu chấm (AC-17). */
export function chuanHoaDienTich(nhap: string): string {
  return nhap.trim().replace(",", ".");
}

/** Cùng dạng chữ backend nhận cho bộ lọc diện tích (AC-2). */
export const DIEN_TICH_LOC = /^[0-9]{1,4}(\.[0-9]{1,2})?$/;

/**
 * Diện tích gửi trong body: chuỗi số thập phân thành số, còn lại gửi nguyên chuỗi để server trả lỗi trường
 * (AC-17, AC-18).
 */
export function dienTichGuiDi(nhap: string): number | string {
  const gon = chuanHoaDienTich(nhap);
  return /^[0-9]+(\.[0-9]+)?$/.test(gon) ? Number(gon) : gon;
}

/** Xem trước mã căn khi số căn là số nguyên 1 đến 99; server vẫn tự ghép (AC-18). */
export function maCanSeTao(maTang: string | undefined, soCan: string): string | undefined {
  const gon = soCan.trim();
  if (!maTang || !/^[0-9]{1,2}$/.test(gon) || Number(gon) < 1) return undefined;
  return `${maTang}${String(Number(gon)).padStart(2, "0")}`;
}

/** Vai trò tạo và sửa được căn hộ (AC-15, AC-18, AC-19). */
export function duocQuanLyCanHo(vaiTro: readonly VaiTro[] | undefined): boolean {
  return vaiTro?.includes("BAN_QUAN_LY") ?? false;
}
