import { describe, expect, it } from "vitest";
import { safeNext } from "./safe-next";

describe("safeNext (AC-12, AC-13)", () => {
  it.each([
    ["/ho-so?x=1", "/ho-so?x=1"],
    ["/can-ho/A-0101#ghi-chu", "/can-ho/A-0101#ghi-chu"],
    ["/", "/"],
  ])("nhận đường dẫn nội bộ %s", (input, expected) => {
    expect(safeNext(input)).toBe(expected);
  });

  it.each([
    ["//evil.com"],
    ["/\\evil.com"],
    ["https://evil.com"],
    ["evil.com"],
    ["/dang-nhap"],
    ["/dang-nhap?next=/ho-so"],
    ["/dang-nhap/khac"],
    ["/\t/evil.com"],
    ["/ho-so\n"],
    [""],
    [null],
    [undefined],
  ])("trả / với %j", (input) => {
    expect(safeNext(input)).toBe("/");
  });

  it("không nhầm trang có tiền tố giống /dang-nhap", () => {
    expect(safeNext("/dang-nhap-lai")).toBe("/dang-nhap-lai");
  });
});
