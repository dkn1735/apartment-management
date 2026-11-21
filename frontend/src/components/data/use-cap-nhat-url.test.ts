import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { mockPathname, mockPush, mockReplace } from "@/test/next-navigation";
import { useCapNhatUrl } from "./use-cap-nhat-url";

function dung(qs: string) {
  return renderHook(({ q }) => useCapNhatUrl(new URLSearchParams(q)), { initialProps: { q: qs } });
}

describe("useCapNhatUrl", () => {
  beforeEach(() => {
    mockReplace.mockReset();
    mockPush.mockReset();
    mockPathname.mockReturnValue("/nhat-ky");
  });

  it("gộp hai lần gọi liền nhau trong cùng một lượt", () => {
    const { result } = dung("page=3");
    act(() => {
      result.current({ sort: "thoiGianGhi,asc" });
      result.current({ page: undefined });
    });
    expect(mockReplace).toHaveBeenLastCalledWith("/nhat-ky?sort=thoiGianGhi%2Casc", { scroll: false });
  });

  it("sau khi Back về URL cũ thì không mang lại tham số của lần ghi trước", () => {
    const { result, rerender } = dung("");
    act(() => result.current({ maNguoiDung: "ND00000007" }, { themLichSu: true }));
    rerender({ q: "maNguoiDung=ND00000007" });
    rerender({ q: "" }); // nút Back của trình duyệt
    act(() => result.current({ page: "2" }));
    expect(mockReplace).toHaveBeenLastCalledWith("/nhat-ky?page=2", { scroll: false });
  });
});
