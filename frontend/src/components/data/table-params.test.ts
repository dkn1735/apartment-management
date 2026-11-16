import { describe, expect, it } from "vitest";
import { parseTableParams, toSortParam } from "./table-params";

const parse = (query: string) => parseTableParams(new URLSearchParams(query));

describe("parseTableParams", () => {
  it("mặc định trang 1, 20 dòng, không sắp xếp", () => {
    expect(parse("")).toEqual({ page: 1, size: 20, sort: undefined });
  });

  it("page không phải số nguyên dương thì 1", () => {
    expect(parse("page=0").page).toBe(1);
    expect(parse("page=-2").page).toBe(1);
    expect(parse("page=2.5").page).toBe(1);
    expect(parse("page=abc").page).toBe(1);
    expect(parse("page=3").page).toBe(3);
  });

  it("size ngoài 10, 20, 50 thì 20", () => {
    expect(parse("size=50").size).toBe(50);
    expect(parse("size=100").size).toBe(20);
  });

  it("đọc sort dạng truong,asc|desc, bỏ qua giá trị sai", () => {
    expect(parse("sort=maCanHo,desc").sort).toEqual({ field: "maCanHo", dir: "desc" });
    expect(parse("sort=maCanHo,up").sort).toBeUndefined();
    expect(toSortParam({ field: "maCanHo", dir: "asc" })).toBe("maCanHo,asc");
  });

  it("nhận searchParams dạng object của trang server", () => {
    expect(parseTableParams({ page: "2", size: ["10", "50"] })).toEqual({ page: 2, size: 10, sort: undefined });
  });
});
