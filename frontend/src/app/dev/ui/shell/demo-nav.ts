import { Bell, Building2, ClipboardList, Gauge, Receipt, ShieldCheck, Users, Wrench } from "lucide-react";
import type { NavGroup } from "@/components/layout/nav";

const BASE = "/dev/ui/shell";

/** Menu mẫu hai nhóm cho trang trưng bày AppShell. */
export const DEMO_NAV: NavGroup[] = [
  {
    items: [{ href: BASE, label: "Tổng quan", icon: Gauge }],
  },
  {
    label: "Vận hành",
    items: [
      { href: `${BASE}/can-ho`, label: "Căn hộ", icon: Building2 },
      { href: `${BASE}/cu-dan`, label: "Cư dân", icon: Users },
      { href: `${BASE}/hoa-don`, label: "Hóa đơn và phí", icon: Receipt },
      { href: `${BASE}/su-co`, label: "Sự cố", icon: Wrench },
      { href: `${BASE}/thong-bao`, label: "Thông báo", icon: Bell },
    ],
  },
  {
    label: "Quản trị",
    items: [
      { href: `${BASE}/tai-khoan`, label: "Tài khoản", icon: ShieldCheck },
      { href: `${BASE}/nhat-ky`, label: "Nhật ký kiểm toán", icon: ClipboardList },
    ],
  },
];

/** Tiêu đề trang theo đoạn đường dẫn đầu tiên. */
export function demoTitle(segment?: string): string {
  const item = DEMO_NAV.flatMap((group) => group.items).find(
    (nav) => nav.href === (segment ? `${BASE}/${segment}` : BASE),
  );
  return item?.label ?? "Trang mẫu";
}
