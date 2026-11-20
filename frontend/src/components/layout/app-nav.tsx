import { useId } from "react";
import type { NavGroup } from "./nav";
import { NavLink } from "./nav-link";

type AppNavProps = { nav: NavGroup[]; label?: string };

/**
 * Menu của AppShell, dùng chung cho thanh bên và ngăn kéo. Render được ở server nên icon truyền thẳng.
 * Danh sách của nhóm có nhãn gắn bằng `aria-labelledby`, để trình đọc màn hình đọc tên nhóm khi vào mục
 * (spec 0006, AC-16); `useId` giữ id khác nhau giữa thanh bên và ngăn kéo.
 */
export function AppNav({ nav, label = "Menu chính" }: AppNavProps) {
  const idGoc = useId();
  const hrefs = nav.flatMap((group) => group.items.map((item) => item.href));
  return (
    <nav aria-label={label} className="flex flex-col gap-6">
      {nav.map((group, index) => (
        <div key={group.label ?? index} className="flex flex-col gap-1">
          {group.label && (
            <p
              id={`${idGoc}-nhom-${index}`}
              className="px-3 pb-1 text-[12px] leading-snug font-medium text-muted-foreground"
            >
              {group.label}
            </p>
          )}
          <ul aria-labelledby={group.label ? `${idGoc}-nhom-${index}` : undefined} className="flex flex-col gap-1">
            {group.items.map(({ href, label: itemLabel, icon: Icon }) => (
              <li key={href}>
                <NavLink href={href} hrefs={hrefs} icon={<Icon aria-hidden="true" />}>
                  {itemLabel}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}
