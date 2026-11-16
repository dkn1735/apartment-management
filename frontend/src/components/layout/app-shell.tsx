import type { ReactNode } from "react";
import { AppNav } from "./app-nav";
import { Brand } from "./brand";
import { MobileNav } from "./mobile-nav";
import type { NavGroup } from "./nav";

export type AppShellProps = {
  /** Menu theo vai trò, do tính năng 7 truyền vào. */
  nav: NavGroup[];
  /** Vùng bên phải thanh trên, ví dụ menu người dùng. */
  userSlot?: ReactNode;
  children: ReactNode;
};

/**
 * Khung app (spec 0003, Giao diện AppShell; AC-4, AC-12): liên kết bỏ qua là điểm Tab đầu tiên,
 * thanh bên cố định từ 1024px, dưới 1024px menu nằm trong ngăn kéo.
 * Dùng được trong layout server lẫn client: icon của menu được render ở đây, chỉ link và ngăn kéo chạy phía client.
 */
export function AppShell({ nav, userSlot, children }: AppShellProps) {
  return (
    <div className="min-h-dvh">
      <a
        href="#noi-dung"
        className="sr-only z-50 rounded-full bg-surface-dark px-4 py-2 text-on-dark no-underline focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:text-on-dark"
      >
        Bỏ qua, tới nội dung chính
      </a>
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col gap-6 overflow-y-auto border-r bg-background px-3 py-5 lg:flex">
        <div className="px-3">
          <Brand />
        </div>
        <AppNav nav={nav} />
      </aside>
      <div className="flex min-h-dvh flex-col lg:pl-64">
        <header className="sticky top-0 z-40 flex h-16 items-center gap-3 border-b bg-background px-4 sm:px-6">
          <MobileNav title={<Brand />}>
            <AppNav nav={nav} />
          </MobileNav>
          <Brand className="min-w-0 lg:hidden" />
          <div className="ml-auto flex items-center gap-3">{userSlot}</div>
        </header>
        <main
          id="noi-dung"
          tabIndex={-1}
          className="mx-auto w-full max-w-[1440px] flex-1 px-4 py-8 focus:outline-none sm:px-6"
        >
          {children}
        </main>
      </div>
    </div>
  );
}
