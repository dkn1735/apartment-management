import { readdirSync, readFileSync } from "node:fs";
import { join, relative } from "node:path";
import { describe, expect, it } from "vitest";

const PALETTE =
  "slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose";
const RULES: { name: string; pattern: RegExp }[] = [
  {
    name: "class màu bảng có sẵn của Tailwind",
    pattern: new RegExp(
      `\\b(bg|text|border|ring|outline|fill|stroke|from|via|to|divide|placeholder|decoration|shadow|accent|caret)-(${PALETTE})-\\d{2,3}\\b`,
    ),
  },
  { name: "class đen trắng", pattern: /\b(bg|text|border|ring|fill|stroke)-(black|white)\b/ },
  { name: "mã hex", pattern: /#[0-9a-fA-F]{3,8}\b/ },
  { name: "hàm màu", pattern: /\b(rgb|hsl|oklch)\(/ },
];

/** Tìm vi phạm màu thô trong một đoạn mã (AC-16). */
export function findRawColors(source: string): string[] {
  return RULES.filter((rule) => rule.pattern.test(source)).map((rule) => rule.name);
}

function listTsx(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return listTsx(path);
    return entry.name.endsWith(".tsx") && !entry.name.endsWith(".test.tsx") ? [path] : [];
  });
}

describe("chặn màu thô", () => {
  it("bắt được các mẫu bị cấm", () => {
    expect(findRawColors('className="text-gray-600"')).not.toEqual([]);
    expect(findRawColors('className="bg-black/50"')).not.toEqual([]);
    expect(findRawColors('style={{ color: "#fff" }}')).not.toEqual([]);
    expect(findRawColors('style={{ color: "rgb(0 0 0)" }}')).not.toEqual([]);
    expect(findRawColors('className="bg-primary text-on-dark bg-overlay"')).toEqual([]);
  });

  it("không component hay trang nào dùng màu thô", () => {
    const root = join(process.cwd(), "src");
    const offenders = listTsx(root)
      .map((file) => ({ file: relative(root, file), rules: findRawColors(readFileSync(file, "utf8")) }))
      .filter((item) => item.rules.length > 0);
    expect(offenders).toEqual([]);
  });
});
