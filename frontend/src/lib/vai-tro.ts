import type { components } from "@/lib/api/schema";

export type ChiTietVaiTro = components["schemas"]["ChiTietVaiTro"];

/** Mã vai trò cố định của backend (spec 0004, mục Mã vai trò). */
export type VaiTro = components["schemas"]["HoSoResponse"]["vaiTro"][number];

/** Thứ tự cố định của vai trò (spec 0004), nguồn thứ tự duy nhất cho nhóm menu (spec 0006, AC-10). */
export const THU_TU_VAI_TRO: readonly VaiTro[] = [
  "QUAN_TRI_VIEN",
  "BAN_QUAN_LY",
  "BAN_QUAN_TRI",
  "KE_TOAN",
  "KY_THUAT",
  "BAO_VE",
  "CU_DAN",
];

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

export type DongChiTiet = { label: string; value: string };

/** Các dòng chi tiết riêng của một vai trò (spec 0004, mục Giao diện); không có chi tiết thì rỗng. */
export function chiTietCua(vaiTro: VaiTro, chiTiet: ChiTietVaiTro): DongChiTiet[] {
  switch (vaiTro) {
    case "QUAN_TRI_VIEN":
      return chiTiet.quanTriVien ? [{ label: "Mã quản trị viên", value: chiTiet.quanTriVien.maQuanTriVien }] : [];
    case "BAN_QUAN_LY":
      return chiTiet.banQuanLy
        ? [
            { label: "Mã nhân viên", value: chiTiet.banQuanLy.maNv },
            { label: "Chức vụ", value: chiTiet.banQuanLy.chucVu },
            { label: "Bộ phận", value: chiTiet.banQuanLy.boPhan },
          ]
        : [];
    case "BAN_QUAN_TRI":
      return chiTiet.banQuanTri
        ? [
            { label: "Mã thành viên", value: chiTiet.banQuanTri.maThanhVien },
            { label: "Nhiệm kỳ", value: chiTiet.banQuanTri.nhiemKy },
            { label: "Vai trò trong ban quản trị", value: chiTiet.banQuanTri.vaiTro },
          ]
        : [];
    case "KE_TOAN":
      return chiTiet.keToan
        ? [
            { label: "Mã nhân viên", value: chiTiet.keToan.maNv },
            { label: "Phạm vi phụ trách", value: chiTiet.keToan.phamViPhuTrach },
          ]
        : [];
    case "KY_THUAT":
      return chiTiet.kyThuat
        ? [
            { label: "Mã nhân viên", value: chiTiet.kyThuat.maNv },
            { label: "Chuyên môn", value: chiTiet.kyThuat.chuyenMonKyThuat },
          ]
        : [];
    case "BAO_VE":
      return chiTiet.baoVe
        ? [
            { label: "Mã nhân viên", value: chiTiet.baoVe.maNv },
            { label: "Vị trí trực", value: chiTiet.baoVe.viTriTruc },
          ]
        : [];
    case "CU_DAN":
      return chiTiet.cuDan ? [{ label: "Mã cư dân", value: chiTiet.cuDan.maCuDan }] : [];
  }
}
