"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { cn } from "cn";
import { useCloseDrawer } from "./mobile-nav";
import { activeHref } from "./nav";

type NavLinkProps = {
  href: string;
  /** Mọi href trong menu, để chọn mục khớp dài nhất. */
  hrefs: readonly string[];
  icon: ReactNode;
  children: ReactNode;
};

/** Một mục menu; đang chọn thì là viên thuốc tối, có `aria-current="page"`; bấm thì đóng ngăn kéo. */
export function NavLink({ href, hrefs, icon, children }: NavLinkProps) {
  const isActive = activeHref(usePathname(), hrefs) === href;
  const closeDrawer = useCloseDrawer();

  return (
    <Link
      href={href}
      aria-current={isActive ? "page" : undefined}
      onClick={closeDrawer}
      className={cn(
        "flex min-h-10 items-center gap-3 rounded-full px-3 py-2 type-ui no-underline transition-colors duration-150 ease-out [&>svg]:size-[18px] [&>svg]:shrink-0",
        isActive
          ? "on-dark bg-surface-dark font-medium text-on-dark hover:text-on-dark"
          : "text-foreground hover:bg-accent hover:text-accent-foreground",
      )}
    >
      {icon}
      <span className="min-w-0 break-words">{children}</span>
    </Link>
  );
}
