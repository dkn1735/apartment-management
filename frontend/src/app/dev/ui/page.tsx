import type { Metadata } from "next";
import Link from "next/link";
import { ButtonDemo } from "./_demo/button-demo";
import { DemoForm } from "./_demo/demo-form";
import { DialogDemo } from "./_demo/dialog-demo";
import { DisplayDemo } from "./_demo/display-demo";
import { FeedbackDemo } from "./_demo/feedback-demo";
import { OverviewDemo } from "./_demo/overview-demo";
import { TableDemo } from "./_demo/table-demo";
import { DemoGroup, DemoSection } from "./_demo/demo-section";
import { PaletteDemo } from "./_demo/palette-demo";
import { TypographyDemo } from "./_demo/typography-demo";

export const metadata: Metadata = { title: "Bộ component giao diện" };

const SECTIONS = [
  { id: "chu", title: "Chữ" },
  { id: "mau", title: "Màu" },
  { id: "nut", title: "Nút" },
  { id: "form", title: "Form" },
  { id: "hien-thi", title: "Thẻ và chip" },
  { id: "phan-hoi", title: "Phản hồi" },
  { id: "hop-thoai", title: "Hộp thoại" },
  { id: "bang", title: "Bảng" },
  { id: "tong-quan", title: "Số liệu và tổng quan" },
];

/** Trang trưng bày design system, chỉ có khi chạy `pnpm dev` (spec 0003, mục Trang /dev/ui). */
export default function DevUiPage() {
  return (
    <main className="mx-auto w-full max-w-[1440px] px-4 py-10 sm:px-6">
      <header className="mb-8">
        <p className="type-eyebrow text-primary">Design system</p>
        <h1 className="mt-1 type-page-title">Bộ component giao diện</h1>
        <p className="mt-2 max-w-2xl text-muted-foreground">
          Mọi component nền ở mọi trạng thái. Dùng phím Tab để xem vòng focus, rê chuột để xem trạng thái rê. Quy tắc
          dùng ở <code>frontend/design.md</code>. Khung app có thanh bên xem ở{" "}
          <Link href="/dev/ui/shell">trang AppShell mẫu</Link>.
        </p>
      </header>
      <div className="grid gap-10 lg:grid-cols-[200px_minmax(0,1fr)]">
        <nav aria-label="Mục lục" className="lg:sticky lg:top-6 lg:self-start">
          <ul className="flex flex-wrap gap-x-4 gap-y-2 lg:flex-col">
            {SECTIONS.map((section) => (
              <li key={section.id}>
                <a href={`#${section.id}`} className="type-ui">
                  {section.title}
                </a>
              </li>
            ))}
          </ul>
        </nav>
        <div className="flex min-w-0 flex-col gap-10">
          <DemoSection id="chu" title="Chữ" description="Be Vietnam Pro, subset vietnamese.">
            <TypographyDemo />
          </DemoSection>
          <DemoSection id="mau" title="Màu" description="Chỉ dùng màu qua token, không dùng màu thô.">
            <PaletteDemo />
          </DemoSection>
          <DemoSection id="nut" title="Nút">
            <ButtonDemo />
          </DemoSection>
          <DemoSection
            id="form"
            title="Form"
            description="Chọn tình huống ở bên phải rồi bấm Lưu để xem cách form báo lỗi API."
          >
            <DemoGroup label="Form mẫu">
              <DemoForm />
            </DemoGroup>
          </DemoSection>
          <DemoSection id="hien-thi" title="Thẻ và chip">
            <DisplayDemo />
          </DemoSection>
          <DemoSection id="phan-hoi" title="Phản hồi" description="Toast, trạng thái rỗng, trạng thái lỗi.">
            <FeedbackDemo />
          </DemoSection>
          <DemoSection id="hop-thoai" title="Hộp thoại">
            <DialogDemo />
          </DemoSection>
          <DemoSection
            id="bang"
            title="Bảng"
            description="Phân trang và sắp xếp phía server; dữ liệu giả trễ 600ms. Bảng rộng cuộn ngang trong khung."
          >
            <TableDemo />
          </DemoSection>
          <DemoSection
            id="tong-quan"
            title="Số liệu và tổng quan"
            description="Mảng tối đầu trang, ô số liệu, nhãn giá trị."
          >
            <OverviewDemo />
          </DemoSection>
        </div>
      </div>
    </main>
  );
}
