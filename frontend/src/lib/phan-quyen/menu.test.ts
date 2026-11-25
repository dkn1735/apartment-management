import { Building2, Wrench } from "lucide-react";
import { describe, expect, it } from "vitest";
import { duocVao, mucCuaDuongDan, nhomMenu } from "./menu";
import { MUC_CHUC_NANG, type MucChucNang } from "./muc-chuc-nang";

const tenNhom = (vaiTro: string[]) => nhomMenu(vaiTro).map((g) => [g.label, g.items.map((i) => i.label)]);

describe("nhomMenu (spec 0006, AC-10)", () => {
  it("BQL thấy nhóm Ban quản lý có Tòa nhà, Căn hộ, Khu vực chung, Nhật ký hệ thống và Tài khoản", () => {
    expect(tenNhom(["BAN_QUAN_LY"])).toEqual([
      [undefined, ["Tổng quan", "Hồ sơ"]],
      ["Ban quản lý", ["Tòa nhà", "Căn hộ", "Khu vực chung", "Nhật ký hệ thống", "Tài khoản"]],
    ]);
  });

  it("Kỹ thuật thấy Tòa nhà và Khu vực chung (spec 0011, AC-10, AC-18)", () => {
    expect(tenNhom(["KY_THUAT"])).toEqual([
      [undefined, ["Tổng quan", "Hồ sơ"]],
      ["Kỹ thuật", ["Tòa nhà", "Khu vực chung"]],
    ]);
  });

  it("Admin thấy nhóm Quản trị viên có Nhật ký hệ thống và Tài khoản", () => {
    expect(tenNhom(["QUAN_TRI_VIEN"])).toEqual([
      [undefined, ["Tổng quan", "Hồ sơ"]],
      ["Quản trị viên", ["Nhật ký hệ thống", "Tài khoản"]],
    ]);
  });

  it("Cư dân thấy Căn hộ của tôi (spec 0012, AC-20)", () => {
    expect(tenNhom(["CU_DAN"])).toEqual([
      [undefined, ["Tổng quan", "Hồ sơ"]],
      ["Cư dân", ["Căn hộ của tôi"]],
    ]);
  });

  it.each([[[]], [["VAI_TRO_LA"]]])("%j chỉ thấy nhóm đầu", (vaiTro) => {
    expect(tenNhom(vaiTro)).toEqual([[undefined, ["Tổng quan", "Hồ sơ"]]]);
  });

  it("hai vai trò: mục chỉ ở nhóm đứng trước, nhóm rỗng bị ẩn", () => {
    expect(tenNhom(["CU_DAN", "BAN_QUAN_TRI"])).toEqual([
      [undefined, ["Tổng quan", "Hồ sơ"]],
      ["Ban quản trị", ["Tòa nhà", "Căn hộ", "Nhật ký hệ thống"]],
      ["Cư dân", ["Căn hộ của tôi"]],
    ]);
  });

  it("không href nào trùng, kể cả khi nhiều vai trò cùng có mục", () => {
    const ma: MucChucNang[] = [
      { href: "/a", label: "A", vaiTro: ["BAN_QUAN_LY", "KE_TOAN"], sanSang: true, moTa: "a", icon: Building2 },
      { href: "/b", label: "B", vaiTro: ["KE_TOAN"], sanSang: true, moTa: "b", icon: Wrench },
      { href: "/c", label: "C", vaiTro: ["KE_TOAN"], sanSang: false },
    ];
    const nhom = nhomMenu(["KE_TOAN", "BAN_QUAN_LY"], ma);
    expect(nhom.map((g) => g.label)).toEqual([undefined, "Ban quản lý", "Kế toán"]);
    const hrefs = nhom.flatMap((g) => g.items.map((i) => i.href));
    expect(new Set(hrefs).size).toBe(hrefs.length);
    expect(hrefs).not.toContain("/c");
  });

  it("ma trận có 22 dòng, href không trùng", () => {
    expect(MUC_CHUC_NANG).toHaveLength(22);
    expect(new Set(MUC_CHUC_NANG.map((m) => m.href)).size).toBe(22);
  });
});

describe("mucCuaDuongDan, duocVao (AC-11)", () => {
  it.each([
    ["/toa-nha", "/toa-nha"],
    ["/toa-nha/abc", "/toa-nha"],
    ["/ho-so-cu-tru", "/ho-so-cu-tru"],
    ["/ho-so", undefined],
    ["/", undefined],
    ["/toa-nha-khac", undefined],
    ["/can-ho/A-0101", "/can-ho"],
    ["/can-ho-cua-toi/A-0101", "/can-ho-cua-toi"],
  ])("%s khớp %s", (pathname, href) => {
    expect(mucCuaDuongDan(pathname)?.href).toBe(href);
  });

  it("được vào khi có ít nhất một vai trò của mục", () => {
    const toaNha = mucCuaDuongDan("/toa-nha")!;
    expect(duocVao(toaNha, ["CU_DAN", "KE_TOAN"])).toBe(true);
    expect(duocVao(toaNha, ["CU_DAN"])).toBe(false);
    expect(duocVao(toaNha, [])).toBe(false);
  });
});
