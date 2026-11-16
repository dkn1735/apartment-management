import { Building2, CircleDollarSign, Users, Wrench } from "lucide-react";
import { KeyValueList } from "@/components/data/key-value-list";
import { StatTile } from "@/components/data/stat-tile";
import { HeroBand } from "@/components/layout/hero-band";
import { formatNgay, formatNgayGio, formatSo, formatTien } from "@/lib/format";
import { DemoGroup } from "./demo-section";

/** HeroBand, StatTile, KeyValueList và hàm định dạng. */
export function OverviewDemo() {
  return (
    <>
      <DemoGroup label="HeroBand với StatTile on-dark">
        <HeroBand greeting="Chào buổi sáng," title="Nguyễn Thị Ánh Tuyết" subtitle="Ban quản lý · Tòa A, B, C">
          <StatTile
            variant="on-dark"
            icon={Building2}
            label="Căn hộ đang ở"
            value={formatSo(312)}
            hint="trên 360 căn"
          />
          <StatTile variant="on-dark" icon={Wrench} label="Sự cố đang xử lý" value="7" />
          <StatTile variant="on-dark" icon={Users} label="Cư dân" value={formatSo(1048)} />
        </HeroBand>
      </DemoGroup>
      <DemoGroup label="StatTile">
        <div className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-3">
          <StatTile icon={CircleDollarSign} label="Đã thu tháng 11" value="1,2 tỷ" hint="86% số hóa đơn" />
          <StatTile
            variant="highlight"
            icon={Building2}
            label="Tỷ lệ lấp đầy"
            value="87%"
            hint="tăng 2% so với tháng 10"
          />
          <StatTile icon={Wrench} label="Lịch bảo trì tuần này" value="4" />
        </div>
      </DemoGroup>
      <DemoGroup label="KeyValueList và định dạng">
        <div className="grid gap-6 md:grid-cols-2">
          <KeyValueList
            items={[
              { label: "Phí quản lý tháng 11", value: formatTien(4850000) },
              { label: "Hạn thanh toán", value: formatNgay("2026-11-25") },
              { label: "Ghi nhận lúc", value: formatNgayGio("2026-11-16T07:05:00Z") },
              { label: "Ngày chuyển đi", value: formatNgay(null) },
            ]}
          />
          <KeyValueList
            variant="divided"
            items={[
              { label: "Mã căn hộ", value: "A-1205" },
              { label: "Chủ hộ", value: "Nguyễn Thị Ánh Tuyết" },
              { label: "Diện tích", value: "74 m²" },
              { label: "Email", value: "nguyen.thi.anh.tuyet.chu.ho.can.a1205@example.com" },
            ]}
          />
        </div>
      </DemoGroup>
    </>
  );
}
