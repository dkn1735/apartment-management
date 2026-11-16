"use client";

import { useSyncExternalStore } from "react";
import { DemoGroup } from "./demo-section";

const TOKENS = [
  "--background",
  "--foreground",
  "--card",
  "--muted",
  "--muted-foreground",
  "--border",
  "--input",
  "--primary",
  "--primary-hover",
  "--primary-foreground",
  "--primary-soft",
  "--secondary",
  "--accent",
  "--destructive",
  "--destructive-hover",
  "--destructive-soft",
  "--success",
  "--success-soft",
  "--warning",
  "--warning-soft",
  "--info",
  "--info-soft",
  "--highlight",
  "--highlight-foreground",
  "--surface-dark",
  "--on-dark",
  "--on-dark-muted",
  "--ring",
  "--overlay",
];

function subscribeNever() {
  return () => {};
}

function readTokenValues(): string {
  const style = getComputedStyle(document.documentElement);
  return JSON.stringify(Object.fromEntries(TOKENS.map((token) => [token, style.getPropertyValue(token).trim()])));
}

/** Bảng màu đọc thẳng từ biến CSS, nên luôn khớp globals.css. */
export function PaletteDemo() {
  // Đọc giá trị biến CSS sau khi trang chạy trên trình duyệt; lúc render phía server chưa có
  const snapshot = useSyncExternalStore(subscribeNever, readTokenValues, () => "");
  const values: Record<string, string> = snapshot ? JSON.parse(snapshot) : {};

  return (
    <DemoGroup label="Bảng màu (token)">
      <ul className="grid grid-cols-[repeat(auto-fill,minmax(160px,1fr))] gap-3">
        {TOKENS.map((token) => (
          <li key={token} className="overflow-hidden rounded-md border bg-card">
            <span aria-hidden="true" className="block h-14 border-b" style={{ background: `var(${token})` }} />
            <span className="flex flex-col px-3 py-2">
              <code className="type-caption font-medium">{token}</code>
              <span className="type-caption text-muted-foreground">{values[token] || "…"}</span>
            </span>
          </li>
        ))}
      </ul>
    </DemoGroup>
  );
}
