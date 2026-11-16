import { Building2, Plus, Users, Wrench } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { KeyValueList } from "@/components/data/key-value-list";
import { StatTile } from "@/components/data/stat-tile";
import { AppShell } from "@/components/layout/app-shell";
import { HeroBand } from "@/components/layout/hero-band";
import { PageHeader } from "@/components/layout/page-header";
import { SectionHeader } from "@/components/layout/section-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { formatNgay, formatSo, formatTien } from "@/lib/format";
import { notFoundInProduction } from "../../../dev-only";
import { DEMO_NAV, demoTitle } from "../demo-nav";

export function generateMetadata(): Metadata {
  notFoundInProduction();
  return { title: "Khung app mẫu" };
}

/** Trang trưng bày AppShell với menu mẫu hai nhóm (spec 0003, Trang /dev/ui). */
export default async function ShellDemoPage({ params }: PageProps<"/dev/ui/shell/[[...duongDan]]">) {
  notFoundInProduction();
  const { duongDan } = await params;
  const title = demoTitle(duongDan?.[0]);

  return (
    <AppShell
      nav={DEMO_NAV}
      userSlot={
        <Button variant="outline" size="sm" asChild>
          <Link href="/dev/ui">Về /dev/ui</Link>
        </Button>
      }
    >
      <PageHeader
        title={title}
        description="Trang mẫu của AppShell. Thử phím Tab từ đầu trang, thu hẹp cửa sổ dưới 1024px để thấy ngăn kéo menu."
        actions={
          <Button>
            <Plus aria-hidden="true" />
            Thêm mới
          </Button>
        }
      />
      <div className="flex flex-col gap-10">
        <HeroBand greeting="Chào buổi sáng," title="Ban quản lý tòa A, B, C" subtitle="Hôm nay là 16/11/2026">
          <StatTile variant="on-dark" icon={Building2} label="Căn hộ đang ở" value={formatSo(312)} />
          <StatTile variant="on-dark" icon={Users} label="Cư dân" value={formatSo(1048)} />
          <StatTile variant="on-dark" icon={Wrench} label="Sự cố mở" value="7" />
        </HeroBand>
        <section aria-labelledby="phi-thang">
          <SectionHeader
            id="phi-thang"
            eyebrow="Tháng 11/2026"
            title="Phí dịch vụ"
            description="Tổng hợp phí đã chốt."
          />
          <Card>
            <KeyValueList
              items={[
                { label: "Tổng phải thu", value: formatTien(1395000000) },
                { label: "Đã thu", value: formatTien(1201500000) },
                { label: "Hạn thanh toán", value: formatNgay("2026-11-25") },
              ]}
            />
          </Card>
        </section>
      </div>
    </AppShell>
  );
}
