import { activeHref, type NavGroup } from "@/components/layout/nav";
import { NHAN_VAI_TRO, THU_TU_VAI_TRO, type VaiTro } from "@/lib/vai-tro";
import { MUC_CHUC_NANG, MUC_CHUNG, type MucChucNang, type MucSanSang } from "./muc-chuc-nang";

/** Một nhóm vai trò của menu và trang Tổng quan. */
export type NhomVaiTro = { vaiTro: VaiTro; label: string; items: MucSanSang[] };

/**
 * Các nhóm theo vai trò (spec 0006, AC-10): theo thứ tự `THU_TU_VAI_TRO`, mỗi nhóm có các mục đã bật mà
 * vai trò được phép và chưa có ở nhóm trước; nhóm rỗng bị ẩn. Mã vai trò lạ bị bỏ qua.
 */
export function nhomVaiTro(vaiTro: readonly string[], ma: readonly MucChucNang[] = MUC_CHUC_NANG): NhomVaiTro[] {
  const daCo = new Set<string>();
  const nhom: NhomVaiTro[] = [];
  for (const v of THU_TU_VAI_TRO) {
    if (!vaiTro.includes(v)) continue;
    const items = ma.filter((muc): muc is MucSanSang => muc.sanSang && muc.vaiTro.includes(v) && !daCo.has(muc.href));
    items.forEach((muc) => daCo.add(muc.href));
    if (items.length > 0) nhom.push({ vaiTro: v, label: NHAN_VAI_TRO[v], items });
  }
  return nhom;
}

/** Menu của khung app: nhóm đầu không nhãn ("Tổng quan", "Hồ sơ"), rồi các nhóm vai trò. */
export function nhomMenu(vaiTro: readonly string[], ma: readonly MucChucNang[] = MUC_CHUC_NANG): NavGroup[] {
  return [{ items: [...MUC_CHUNG] }, ...nhomVaiTro(vaiTro, ma).map(({ label, items }) => ({ label, items }))];
}

/** Mục của ma trận mà đường dẫn thuộc về (khớp theo đoạn, `href` dài nhất thắng); không thuộc mục nào thì undefined. */
export function mucCuaDuongDan(pathname: string, ma: readonly MucChucNang[] = MUC_CHUC_NANG): MucChucNang | undefined {
  const href = activeHref(
    pathname,
    ma.map((muc) => muc.href),
  );
  return ma.find((muc) => muc.href === href);
}

/** Người có `vaiTro` được vào trang của mục khi có ít nhất một vai trò của mục (AC-11). */
export function duocVao(muc: MucChucNang, vaiTro: readonly string[]): boolean {
  return muc.vaiTro.some((v) => vaiTro.includes(v));
}
