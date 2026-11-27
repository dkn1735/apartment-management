import type { QueryClient } from "@tanstack/react-query";
import type { StatusTone } from "@/components/data/status-badge";
import type { components } from "@/lib/api/schema";
import type { VaiTro } from "@/lib/vai-tro";

export type PhuongTien = components["schemas"]["PhuongTienResponse"];
export type PhuongTienCanHo = components["schemas"]["PhuongTienCanHoResponse"];
export type LoaiPhuongTien = PhuongTien["loaiPhuongTien"];
export type TrangThaiPhuongTien = PhuongTien["trangThaiDangKy"];
export type TrangThaiDichPhuongTien = components["schemas"]["DoiTrangThaiPhuongTienRequest"]["trangThaiDangKy"];

export const PHUONG_TIEN_QUERY_KEY = ["phuong-tien"] as const;

export const khoaPhuongTienCanHo = (maCanHo: string) => [...PHUONG_TIEN_QUERY_KEY, maCanHo] as const;

export const LOAI_PHUONG_TIEN: readonly LoaiPhuongTien[] = ["Ô tô", "Xe máy", "Xe máy điện", "Xe đạp"];

export const TRANG_THAI_PHUONG_TIEN: readonly TrangThaiPhuongTien[] = [
  "Chờ duyệt",
  "Đang hoạt động",
  "Tạm dừng",
  "Từ chối",
  "Đã hủy",
];

const TONE_TRANG_THAI_PHUONG_TIEN: Record<TrangThaiPhuongTien, StatusTone> = {
  "Chờ duyệt": "warning",
  "Đang hoạt động": "success",
  "Tạm dừng": "warning",
  "Từ chối": "danger",
  "Đã hủy": "neutral",
};

export function toneTrangThaiPhuongTien(trangThai: TrangThaiPhuongTien): StatusTone {
  return TONE_TRANG_THAI_PHUONG_TIEN[trangThai] ?? "neutral";
}

/** Ma trận chuyển trạng thái hợp lệ cho `PUT /api/v1/vehicles/{vehicleId}/status` (spec 0016, AC-10). */
export const MA_TRAN_CHUYEN_TRANG_THAI: Readonly<Record<TrangThaiPhuongTien, readonly TrangThaiDichPhuongTien[]>> = {
  "Chờ duyệt": ["Đang hoạt động", "Từ chối"],
  "Đang hoạt động": ["Tạm dừng", "Đã hủy"],
  "Tạm dừng": ["Đang hoạt động", "Đã hủy"],
  "Từ chối": [],
  "Đã hủy": [],
};

export function trangThaiDichHopLe(hienTai: TrangThaiPhuongTien): readonly TrangThaiDichPhuongTien[] {
  return MA_TRAN_CHUYEN_TRANG_THAI[hienTai] ?? [];
}

/** Lý do bắt buộc khi chuyển sang Tạm dừng, Từ chối hoặc Đã hủy (spec 0016, AC-9). */
export function canNhapLyDoKhiChuyen(trangThaiDich: TrangThaiDichPhuongTien | ""): boolean {
  return trangThaiDich === "Tạm dừng" || trangThaiDich === "Từ chối" || trangThaiDich === "Đã hủy";
}

/**
 * Chuẩn hóa biển số theo đúng quy tắc backend (`BienSo.chuanHoa`):
 * NFC, viết hoa, bỏ khoảng trắng Unicode, dấu chấm `.` và gạch nối `-`.
 */
export function chuanHoaBienSo(bienSo: string): string {
  return bienSo
    .normalize("NFC")
    .trim()
    .toUpperCase()
    .replace(/[\s\p{Z}.-]+/gu, "");
}

export const BIEN_SO_HOP_LE_REGEX = /^[A-Z0-9]{5,12}$/;

/**
 * Điều kiện hiện khối "Phương tiện" trên `ChiTietCanHo` (spec 0016, AC-14):
 * trang quản lý căn hộ (`choPhepSua = true`) hiện cho `BAN_QUAN_LY`, `BAN_QUAN_TRI`, `KE_TOAN`;
 * trang của cư dân (`choPhepSua = false`) hiện cho `CU_DAN`.
 */
export function hienKhoiPhuongTien(vaiTro: readonly VaiTro[] | undefined, choPhepSua: boolean): boolean {
  if (!vaiTro) return false;
  return choPhepSua
    ? vaiTro.some((v) => v === "BAN_QUAN_LY" || v === "BAN_QUAN_TRI" || v === "KE_TOAN")
    : vaiTro.includes("CU_DAN");
}

/**
 * Quyền bấm nút đăng ký phương tiện trên `ChiTietCanHo` (spec 0016, AC-15):
 * BQL trên trang quản lý căn hộ; cư dân không kiêm BQL trên trang căn hộ của tôi.
 * Kế toán và BQT không có quyền đăng ký.
 */
export function coTheDangKyPhuongTien(vaiTro: readonly VaiTro[] | undefined, choPhepSua: boolean): boolean {
  if (!vaiTro) return false;
  return choPhepSua ? vaiTro.includes("BAN_QUAN_LY") : vaiTro.includes("CU_DAN") && !vaiTro.includes("BAN_QUAN_LY");
}

/** Làm mới danh sách phương tiện của căn hộ sau khi đăng ký, đổi trạng thái hoặc rút đơn. */
export function lamMoiPhuongTienCanHo(queryClient: QueryClient, maCanHo: string): void {
  void queryClient.invalidateQueries({ queryKey: khoaPhuongTienCanHo(maCanHo) });
}
