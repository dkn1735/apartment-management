"use client";

import { Menu } from "lucide-react";
import { usePathname } from "next/navigation";
import { createContext, useContext, useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetDescription, SheetTitle, SheetTrigger } from "@/components/ui/sheet";

const CloseDrawerContext = createContext<() => void>(() => {});

/** Hàm đóng ngăn kéo menu; ngoài ngăn kéo thì không làm gì. */
export function useCloseDrawer() {
  return useContext(CloseDrawerContext);
}

type MobileNavProps = { title: ReactNode; children: ReactNode };

/** Nút "Mở menu" và ngăn kéo dưới 1024px. Tự đóng khi đường dẫn đổi hoặc khi bấm một mục. */
export function MobileNav({ title, children }: MobileNavProps) {
  const pathname = usePathname();
  const [drawer, setDrawer] = useState({ open: false, pathname });

  // Đường dẫn đổi thì đóng ngăn kéo (điều chỉnh state lúc render, không cần effect)
  if (drawer.pathname !== pathname) {
    setDrawer({ open: false, pathname });
  }
  const setOpen = (open: boolean) => setDrawer({ open, pathname });

  return (
    <Sheet open={drawer.open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Mở menu" className="lg:hidden">
          <Menu aria-hidden="true" />
        </Button>
      </SheetTrigger>
      <SheetContent className="gap-6 overflow-y-auto px-3 py-5">
        <SheetTitle className="px-3">{title}</SheetTitle>
        <SheetDescription className="sr-only">Menu điều hướng của ứng dụng</SheetDescription>
        <CloseDrawerContext.Provider value={() => setOpen(false)}>{children}</CloseDrawerContext.Provider>
      </SheetContent>
    </Sheet>
  );
}
