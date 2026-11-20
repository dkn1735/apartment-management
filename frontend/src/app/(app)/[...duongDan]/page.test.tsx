import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { expectNoA11yViolations } from "@/test/axe";
import { mockNotFound } from "@/test/next-navigation";
import DuongDanChuaCoTrang from "./page";

const trang = (duongDan: string[]) =>
  DuongDanChuaCoTrang({ params: Promise.resolve({ duongDan }), searchParams: Promise.resolve({}) });

beforeEach(() => {
  mockNotFound.mockClear();
});

describe("đường dẫn chưa có trang (spec 0006, AC-11)", () => {
  it.each([[["doi-soat"]], [["toa-nha", "abc"]], [["doi-soat", "2026", "11"]]])(
    "%j thuộc ma trận: render trong khung app để ChanTrang chặn",
    async (d) => {
      render(await trang(d));
      expect(screen.getByRole("heading", { level: 1, name: "Không tìm thấy trang" })).toBeInTheDocument();
      expect(mockNotFound).not.toHaveBeenCalled();
    },
  );

  it("ngoài ma trận: 404 thật", async () => {
    await expect(trang(["abc"])).rejects.toThrow("NEXT_NOT_FOUND");
    expect(mockNotFound).toHaveBeenCalled();
  });

  it("chỉ trùng tiền tố chữ, không trùng cả đoạn (/toa-nha-khac): 404 thật, không lọt vào ma trận", async () => {
    await expect(trang(["toa-nha-khac"])).rejects.toThrow("NEXT_NOT_FOUND");
  });

  it("đường dẫn con của mục chung (/ho-so/abc) không thuộc ma trận: 404 thật", async () => {
    await expect(trang(["ho-so", "abc"])).rejects.toThrow("NEXT_NOT_FOUND");
  });

  it("trang render có nút Về trang chủ trỏ về / và không vi phạm axe (AC-16)", async () => {
    const { container } = render(await trang(["doi-soat"]));
    expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    await expectNoA11yViolations(container);
  });
});
