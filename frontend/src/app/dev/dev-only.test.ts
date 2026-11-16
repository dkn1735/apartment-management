import { readdirSync, readFileSync } from "node:fs";
import { join, relative } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";
import { mockNotFound } from "@/test/next-navigation";
import { notFoundInProduction } from "./dev-only";

const DEV_DIR = join(process.cwd(), "src/app/dev");

function routeFiles(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return routeFiles(path);
    return /^(page|layout)\.tsx$/.test(entry.name) ? [path] : [];
  });
}

describe("chặn /dev ở production (AC-14)", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    mockNotFound.mockClear();
  });

  it("notFoundInProduction gọi notFound khi production, không gọi khi dev", () => {
    vi.stubEnv("NODE_ENV", "production");
    expect(() => notFoundInProduction()).toThrow("NEXT_NOT_FOUND");
    vi.stubEnv("NODE_ENV", "development");
    expect(() => notFoundInProduction()).not.toThrow();
    expect(mockNotFound).toHaveBeenCalledTimes(1);
  });

  // Next.js render layout, page và metadata song song: chặn ở layout thôi thì nội dung page vẫn lọt vào payload 404
  it("mọi page và layout dưới /dev gọi notFoundInProduction và không dùng metadata tĩnh", () => {
    const files = routeFiles(DEV_DIR);
    expect(files.length).toBeGreaterThanOrEqual(3);
    const offenders = files
      .map((file) => ({ file: relative(DEV_DIR, file), source: readFileSync(file, "utf8") }))
      .filter(
        ({ file, source }) =>
          !source.includes("notFoundInProduction()") ||
          /export const metadata/.test(source) ||
          (file.endsWith("page.tsx") && /export (async )?function generateMetadata/.test(source) === false),
      )
      .map(({ file }) => file);
    expect(offenders).toEqual([]);
  });
});
