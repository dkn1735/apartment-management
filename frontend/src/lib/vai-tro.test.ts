import { describe, expect, it } from "vitest";
import { type ChiTietVaiTro, chiTietCua, NHAN_VAI_TRO, type VaiTro } from "./vai-tro";

const DAY_DU: ChiTietVaiTro = {
  quanTriVien: { maQuanTriVien: "QTV00000001" },
  banQuanLy: { maNv: "NV00000001", chucVu: "Trưởng ban", boPhan: "Vận hành" },
  banQuanTri: { maThanhVien: "BQT00000001", nhiemKy: "2026-2028", vaiTro: "Trưởng ban" },
  keToan: { maNv: "NV00000002", phamViPhuTrach: "Phí dịch vụ" },
  kyThuat: { maNv: "NV00000003", chuyenMonKyThuat: "Điện nước" },
  baoVe: { maNv: "NV00000004", viTriTruc: "Cổng chính" },
  cuDan: { maCuDan: "CD00000001" },
} as ChiTietVaiTro;

const TAT_CA = Object.keys(NHAN_VAI_TRO) as VaiTro[];

describe("NHAN_VAI_TRO (AC-14)", () => {
  it("mọi mã vai trò có nhãn tiếng Việt không rỗng", () => {
    expect(TAT_CA).toHaveLength(7);
    for (const vaiTro of TAT_CA) {
      expect(NHAN_VAI_TRO[vaiTro].trim()).not.toBe("");
    }
  });
});

describe("chiTietCua (AC-14)", () => {
  it.each<[VaiTro, string[], string[]]>([
    ["QUAN_TRI_VIEN", ["Mã quản trị viên"], ["QTV00000001"]],
    ["BAN_QUAN_LY", ["Mã nhân viên", "Chức vụ", "Bộ phận"], ["NV00000001", "Trưởng ban", "Vận hành"]],
    [
      "BAN_QUAN_TRI",
      ["Mã thành viên", "Nhiệm kỳ", "Vai trò trong ban quản trị"],
      ["BQT00000001", "2026-2028", "Trưởng ban"],
    ],
    ["KE_TOAN", ["Mã nhân viên", "Phạm vi phụ trách"], ["NV00000002", "Phí dịch vụ"]],
    ["KY_THUAT", ["Mã nhân viên", "Chuyên môn"], ["NV00000003", "Điện nước"]],
    ["BAO_VE", ["Mã nhân viên", "Vị trí trực"], ["NV00000004", "Cổng chính"]],
    ["CU_DAN", ["Mã cư dân"], ["CD00000001"]],
  ])("%s trả đúng các dòng theo thứ tự", (vaiTro, labels, values) => {
    const dong = chiTietCua(vaiTro, DAY_DU);
    expect(dong.map((d) => d.label)).toEqual(labels);
    expect(dong.map((d) => d.value)).toEqual(values);
  });

  it.each(TAT_CA)("%s không có chi tiết thì trả mảng rỗng", (vaiTro) => {
    expect(chiTietCua(vaiTro, {})).toEqual([]);
  });

  it("chỉ lấy chi tiết của đúng vai trò được hỏi", () => {
    const chiCuDan: ChiTietVaiTro = { cuDan: { maCuDan: "CD00000009" } };
    expect(chiTietCua("BAN_QUAN_TRI", chiCuDan)).toEqual([]);
    expect(chiTietCua("CU_DAN", chiCuDan)).toEqual([{ label: "Mã cư dân", value: "CD00000009" }]);
  });

  it("không dòng nào nhắc tới CCCD", () => {
    const labels = TAT_CA.flatMap((v) => chiTietCua(v, DAY_DU).map((d) => d.label));
    expect(labels.join(" ")).not.toMatch(/CCCD/i);
  });
});
