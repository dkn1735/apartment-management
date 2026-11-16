import type { LucideIcon } from "lucide-react";

export type NavItem = { href: string; label: string; icon: LucideIcon };
export type NavGroup = { label?: string; items: NavItem[] };

/** Mục khớp đường dẫn theo từng đoạn: bằng nhau, hoặc đường dẫn bắt đầu bằng `href + "/"`; `/` chỉ khớp `/`. */
function matches(pathname: string, href: string): boolean {
  if (href === "/") return pathname === "/";
  const base = href.endsWith("/") ? href.slice(0, -1) : href;
  return pathname === base || pathname.startsWith(`${base}/`);
}

/** `href` của mục đang chọn: trong các mục khớp, chọn `href` dài nhất (AC-12). */
export function activeHref(pathname: string, hrefs: readonly string[]): string | undefined {
  return hrefs.filter((href) => matches(pathname, href)).sort((a, b) => b.length - a.length)[0];
}
