import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ngayHomNay } from "@/lib/format";
import { expectNoA11yViolations } from "@/test/axe";
import { Combobox } from "./combobox";

const OPTIONS = [
  { value: "CD00000001", label: "Vũ Cư Dân (0900000006)" },
  { value: "CD00000002", label: "Nguyễn Thị Ánh Tuyết (0900000007)" },
];

function ComboboxMau({ errorText, loading }: { errorText?: string; loading?: boolean }) {
  const [val, setVal] = useState("");
  const [q, setQ] = useState("");
  return (
    <main>
      <Label htmlFor="cb-cu-dan">Cư dân</Label>
      <Combobox
        id="cb-cu-dan"
        value={val}
        onValueChange={setVal}
        options={OPTIONS.filter((o) => o.label.toLowerCase().includes(q.toLowerCase()))}
        placeholder="Chọn cư dân"
        searchValue={q}
        onSearchChange={setQ}
        searchLabel="Tìm cư dân"
        listLabel="Danh sách cư dân"
        loading={loading}
        errorText={errorText}
        aria-invalid={Boolean(errorText) || undefined}
      />
    </main>
  );
}

describe("Tabs, Combobox và ngayHomNay (spec 0014, AC-16, AC-17, AC-18)", () => {
  it("Tabs chuyển bằng phím mũi tên và không vi phạm axe", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <Tabs defaultValue="hien-tai">
        <TabsList>
          <TabsTrigger value="hien-tai">Hiện tại</TabsTrigger>
          <TabsTrigger value="lich-su">Lịch sử cư trú</TabsTrigger>
        </TabsList>
        <TabsContent value="hien-tai">Nội dung hiện tại</TabsContent>
        <TabsContent value="lich-su">Nội dung lịch sử</TabsContent>
      </Tabs>,
    );

    expect(screen.getByText("Nội dung hiện tại")).toBeInTheDocument();
    await expectNoA11yViolations(container);

    const tabHienTai = screen.getByRole("tab", { name: "Hiện tại" });
    tabHienTai.focus();
    await user.keyboard("{ArrowRight}");
    expect(await screen.findByText("Nội dung lịch sử")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("Combobox mở, điều hướng bàn phím, chọn mục, trả focus và đạt axe ở cả trạng thái mở và lỗi", async () => {
    const user = userEvent.setup();
    const { rerender } = render(<ComboboxMau />);

    const nutMo = screen.getByRole("combobox", { name: "Cư dân" });
    await expectNoA11yViolations(document.body);

    await user.click(nutMo);
    const oTim = await screen.findByRole("combobox", { name: "Tìm cư dân" });
    expect(oTim).toHaveFocus();
    await expectNoA11yViolations(document.body);

    await user.keyboard("{ArrowDown}{Enter}");
    await waitFor(() => expect(nutMo).toHaveTextContent("Nguyễn Thị Ánh Tuyết (0900000007)"));
    expect(nutMo).toHaveFocus();

    rerender(<ComboboxMau errorText="Không tải được danh sách cư dân." />);
    await user.click(screen.getByRole("combobox", { name: "Cư dân" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được danh sách cư dân.");
    await expectNoA11yViolations(document.body);
  });

  it("ngayHomNay tính theo múi giờ Asia/Ho_Chi_Minh qua mốc nửa đêm UTC+7", () => {
    expect(ngayHomNay(new Date("2026-11-26T16:59:59Z"))).toBe("2026-11-26");
    expect(ngayHomNay(new Date("2026-11-26T17:00:00Z"))).toBe("2026-11-27");
  });
});
