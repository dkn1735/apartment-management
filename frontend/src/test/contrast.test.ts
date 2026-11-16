import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

// Đọc token thật trong globals.css, để đổi màu mà làm hỏng tương phản thì test này đỏ (AC-1)
const css = readFileSync(join(process.cwd(), "src/app/globals.css"), "utf8");
const rootBlock = css.match(/:root\s*\{([^}]*)\}/)?.[1] ?? "";
const tokens = Object.fromEntries(
  [...rootBlock.matchAll(/--([\w-]+):\s*(#[0-9a-fA-F]{6})\s*;/g)].map((m) => [m[1], m[2]]),
);

function luminance(hex: string): number {
  const channels = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255);
  const [r, g, b] = channels.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

const TEXT = 4.5;
const UI = 3;

// [chữ hoặc viền, nền, ngưỡng]
const PAIRS: [string, string, number][] = [
  ["foreground", "background", TEXT],
  ["foreground", "card", TEXT],
  ["foreground", "muted", TEXT],
  ["foreground", "accent", TEXT],
  ["foreground", "primary-soft", TEXT],
  ["muted-foreground", "background", TEXT],
  ["muted-foreground", "card", TEXT],
  ["muted-foreground", "muted", TEXT],
  ["primary", "background", TEXT],
  ["primary", "card", TEXT],
  ["primary", "muted", TEXT],
  ["primary-hover", "background", TEXT],
  ["primary-foreground", "primary", TEXT],
  ["primary-foreground", "primary-hover", TEXT],
  ["secondary-foreground", "secondary", TEXT],
  ["accent-foreground", "accent", TEXT],
  ["destructive", "card", TEXT],
  ["destructive", "background", TEXT],
  ["destructive", "destructive-soft", TEXT],
  ["foreground", "destructive-soft", TEXT],
  ["destructive-foreground", "destructive", TEXT],
  ["destructive-foreground", "destructive-hover", TEXT],
  ["success", "success-soft", TEXT],
  ["success", "card", TEXT],
  ["warning", "warning-soft", TEXT],
  ["info", "info-soft", TEXT],
  ["highlight-foreground", "highlight", TEXT],
  ["on-dark", "surface-dark", TEXT],
  ["on-dark-muted", "surface-dark", TEXT],
  ["input", "background", UI],
  ["input", "card", UI],
  ["ring", "background", UI],
  ["ring", "card", UI],
  ["ring", "muted", UI],
  ["on-dark", "surface-dark", UI],
  ["success", "background", UI],
];

describe("bảng tương phản của token", () => {
  it("đọc được token từ globals.css", () => {
    expect(Object.keys(tokens).length).toBeGreaterThan(20);
  });

  it.each(PAIRS)("--%s trên --%s đạt %s:1", (fg, bg, min) => {
    expect(tokens[fg], `thiếu --${fg}`).toBeDefined();
    expect(tokens[bg], `thiếu --${bg}`).toBeDefined();
    expect(contrast(tokens[fg], tokens[bg])).toBeGreaterThanOrEqual(min);
  });

  it("không còn biến --chart-* và --sidebar-* do shadcn sinh", () => {
    expect(css).not.toMatch(/--(chart|sidebar)-/);
  });
});
