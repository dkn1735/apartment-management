import { render, screen } from "@testing-library/react";
import { Building2 } from "lucide-react";
import { describe, expect, it } from "vitest";
import { HeroBand } from "@/components/layout/hero-band";
import { PageHeader } from "@/components/layout/page-header";
import { SectionHeader } from "@/components/layout/section-header";
import { expectNoA11yViolations } from "@/test/axe";
import { KeyValueList } from "./key-value-list";
import { StatTile } from "./stat-tile";
import { StatusBadge } from "./status-badge";

describe("component hiển thị dữ liệu", () => {
  it("KeyValueList là dl với từng cặp dt, dd", async () => {
    const { container } = render(<KeyValueList items={[{ label: "Mã căn hộ", value: "A-1205" }]} variant="divided" />);
    expect(container.querySelector("dl dt")).toHaveTextContent("Mã căn hộ");
    expect(container.querySelector("dl dd")).toHaveTextContent("A-1205");
    await expectNoA11yViolations(container);
  });

  it("HeroBand có lớp on-dark và chứa StatTile", async () => {
    const { container } = render(
      <HeroBand title="Nguyễn Thị Ánh Tuyết" greeting="Chào buổi sáng,">
        <StatTile variant="on-dark" icon={Building2} label="Căn hộ" value="312" />
      </HeroBand>,
    );
    expect(container.querySelector(".on-dark")).not.toBeNull();
    expect(screen.getByRole("heading", { level: 2, name: "Nguyễn Thị Ánh Tuyết" })).toBeInTheDocument();
    expect(screen.getByText("312")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });

  it("PageHeader có đúng một h1, SectionHeader có h2 và nhãn nhỏ, StatusBadge có chữ", async () => {
    const { container } = render(
      <>
        <PageHeader title="Danh sách căn hộ" description="Mô tả" />
        <SectionHeader eyebrow="Tháng 11" title="Chỉ số" />
        <StatusBadge tone="warning">Đang xử lý</StatusBadge>
        <StatTile icon={Building2} label="Tỷ lệ lấp đầy" value="87%" variant="highlight" />
      </>,
    );
    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getByRole("heading", { level: 2, name: "Chỉ số" })).toBeInTheDocument();
    expect(screen.getByText("Đang xử lý")).toBeInTheDocument();
    await expectNoA11yViolations(container);
  });
});
