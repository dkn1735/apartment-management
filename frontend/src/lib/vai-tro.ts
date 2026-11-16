import type { components } from "@/lib/api/schema";

/** Mã vai trò cố định của backend (spec 0004, mục Mã vai trò). */
export type VaiTro = components["schemas"]["HoSoResponse"]["vaiTro"][number];

/** Nhãn tiếng Việt của từng mã vai trò; chỉ nằm ở frontend. */
export const NHAN_VAI_TRO: Readonly<Record<VaiTro, string>> = {
  QUAN_TRI_VIEN: "Quản trị viên",
  BAN_QUAN_LY: "Ban quản lý",
  BAN_QUAN_TRI: "Ban quản trị",
  KE_TOAN: "Kế toán",
  KY_THUAT: "Kỹ thuật",
  BAO_VE: "Bảo vệ",
  CU_DAN: "Cư dân",
};
