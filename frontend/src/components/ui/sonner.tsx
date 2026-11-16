"use client";

import { CircleCheck, CircleAlert, Info, TriangleAlert } from "lucide-react";
import { Toaster as Sonner } from "sonner";

/** Nơi hiện toast, đặt một lần trong providers.tsx, góc dưới phải; nhãn tiếng Việt (AC-8). */
function Toaster() {
  return (
    <Sonner
      theme="light"
      position="bottom-right"
      duration={5000}
      customAriaLabel="Thông báo (Alt+T)"
      icons={{
        success: <CircleCheck aria-hidden="true" className="size-4 text-success" />,
        error: <CircleAlert aria-hidden="true" className="size-4 text-destructive" />,
        info: <Info aria-hidden="true" className="size-4 text-info" />,
        warning: <TriangleAlert aria-hidden="true" className="size-4 text-warning" />,
      }}
      style={
        {
          "--normal-bg": "var(--card)",
          "--normal-text": "var(--foreground)",
          "--normal-border": "var(--border)",
          "--border-radius": "var(--radius)",
        } as React.CSSProperties
      }
      toastOptions={{
        closeButtonAriaLabel: "Đóng",
        classNames: {
          toast: "!font-sans !shadow-overlay type-ui",
          description: "!text-muted-foreground",
        },
      }}
    />
  );
}

export { Toaster };
