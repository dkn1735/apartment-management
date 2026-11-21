import type { StatusTone } from "@/components/data/status-badge";
import { ApiError } from "@/lib/api/api";
import type { components } from "@/lib/api/schema";
import type { VaiTro } from "@/lib/vai-tro";

export type TaiKhoanTomTat = components["schemas"]["TaiKhoanTomTat"];
export type TaiKhoanChiTiet = components["schemas"]["TaiKhoanChiTietResponse"];
export type VaiTroTaiKhoan = components["schemas"]["VaiTroTaiKhoan"];
export type TrangThaiTaiKhoan = TaiKhoanChiTiet["trangThaiTaiKhoan"];

/** Ba trạng thái tài khoản, đúng chuỗi backend lưu (spec 0008, State transitions). */
export const TRANG_THAI: readonly TrangThaiTaiKhoan[] = ["Hoạt động", "Tạm khóa", "Ngừng hoạt động"];

export const TONE_TRANG_THAI: Readonly<Record<TrangThaiTaiKhoan, StatusTone>> = {
  "Hoạt động": "success",
  "Tạm khóa": "warning",
  "Ngừng hoạt động": "neutral",
};

/** Tên trường riêng của vai trò, trùng tên JSON của backend. */
export type TruongVaiTro =
  "chucVu" | "boPhan" | "nhiemKy" | "vaiTroBqt" | "phamViPhuTrach" | "chuyenMonKyThuat" | "viTriTruc" | "soCccd";

export type MoTaTruong = { ten: TruongVaiTro; nhan: string; toiDa: number; goiY?: string };

const T: Readonly<Record<TruongVaiTro, MoTaTruong>> = {
  chucVu: { ten: "chucVu", nhan: "Chức vụ", toiDa: 100 },
  boPhan: { ten: "boPhan", nhan: "Bộ phận", toiDa: 100 },
  nhiemKy: { ten: "nhiemKy", nhan: "Nhiệm kỳ", toiDa: 50, goiY: "Ví dụ 2025 đến 2030" },
  vaiTroBqt: { ten: "vaiTroBqt", nhan: "Vai trò trong ban quản trị", toiDa: 100 },
  phamViPhuTrach: { ten: "phamViPhuTrach", nhan: "Phạm vi phụ trách", toiDa: 150 },
  chuyenMonKyThuat: { ten: "chuyenMonKyThuat", nhan: "Chuyên môn kỹ thuật", toiDa: 150 },
  viTriTruc: { ten: "viTriTruc", nhan: "Vị trí trực", toiDa: 150 },
  soCccd: { ten: "soCccd", nhan: "Số CCCD", toiDa: 12, goiY: "12 chữ số" },
};

/** Trường bắt buộc của từng vai trò (spec 0008, bảng VaiTroRequest). */
export const TRUONG_CUA_VAI_TRO: Readonly<Record<VaiTro, readonly MoTaTruong[]>> = {
  QUAN_TRI_VIEN: [],
  BAN_QUAN_LY: [T.chucVu, T.boPhan],
  BAN_QUAN_TRI: [T.nhiemKy, T.vaiTroBqt],
  KE_TOAN: [T.phamViPhuTrach],
  KY_THUAT: [T.chuyenMonKyThuat],
  BAO_VE: [T.viTriTruc],
  CU_DAN: [T.soCccd],
};

/** Kiểm một trường vai trò như backend (AC-4); hợp lệ thì `null`. */
export function loiTruong(moTa: MoTaTruong, raw: string): string | null {
  const v = raw.trim();
  if (!v) return `Vui lòng nhập ${moTa.nhan.charAt(0).toLowerCase()}${moTa.nhan.slice(1)}`;
  if (moTa.ten === "soCccd") return /^\d{12}$/.test(v) ? null : "Số CCCD phải gồm 12 chữ số";
  return v.length > moTa.toiDa ? `${moTa.nhan} tối đa ${moTa.toiDa} ký tự` : null;
}

/**
 * Backend báo lỗi vai trò theo `vaiTro[i].<trường>`; form dùng tên trường trần vì mỗi trường chỉ thuộc một vai trò.
 */
export function doiTenTruongLoi(error: unknown): unknown {
  if (!(error instanceof ApiError)) return error;
  return new ApiError({
    status: error.status,
    errorCode: error.errorCode,
    message: error.message,
    requestId: error.requestId,
    errors: error.errors.map((e) => {
      const m = /^vaiTro\[\d+\](?:\.(.+))?$/.exec(e.field ?? "");
      if (!m) return e;
      return { ...e, field: !m[1] || m[1] === "vaiTro" ? "vaiTro" : m[1] };
    }),
  });
}
