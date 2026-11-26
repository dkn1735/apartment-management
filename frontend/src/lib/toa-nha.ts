import type { components } from "@/lib/api/schema";
import type { VaiTro } from "@/lib/vai-tro";

export type ToaNha = components["schemas"]["ToaNhaResponse"];
export type Tang = components["schemas"]["TangResponse"];
export type KhuVucDungChung = components["schemas"]["KhuVucDungChungResponse"];

/** Gốc khóa query của tòa nhà; invalidate gốc này làm mới danh mục, chi tiết và bảng tầng (spec 0011, AC-15, AC-17). */
export const TOA_NHA_QUERY_KEY = ["toa-nha"] as const;

export const khoaChiTietToaNha = (ma: string) => [...TOA_NHA_QUERY_KEY, "chi-tiet", ma] as const;

/** Trần số tầng của một tòa (spec 0011, AC-5). */
export const SO_TANG_TOI_DA = 99;

export const CAU_TOI_DA_TANG = "Tòa đã đạt tối đa 99 tầng";

/** Vai trò ghi được tòa và tầng. */
export function duocThemToaNha(vaiTro: readonly VaiTro[] | undefined): boolean {
  return vaiTro?.includes("BAN_QUAN_LY") ?? false;
}

/** Vai trò xem được tầng, nên tên tòa ở danh mục là link tới trang chi tiết (AC-16). */
export function duocXemTang(vaiTro: readonly VaiTro[] | undefined): boolean {
  return vaiTro?.some((v) => v === "BAN_QUAN_LY" || v === "KY_THUAT") ?? false;
}

/**
 * Giá trị ô số tầng gửi lên server: chuỗi toàn chữ số thành số nguyên, còn lại gửi nguyên chuỗi để server trả lỗi
 * trường (AC-15).
 */
export function soTangGuiDi(nhap: string): number | string {
  const gon = nhap.trim();
  return /^[0-9]+$/.test(gon) ? Number(gon) : gon;
}
