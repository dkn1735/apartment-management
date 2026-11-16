import type { ReactNode } from "react";
import { SectionHeader } from "@/components/layout/section-header";

type DemoSectionProps = { id: string; title: string; description?: string; children: ReactNode };

/** Một mục trên trang /dev/ui, dựng bằng SectionHeader; có id để mục lục trỏ tới. */
export function DemoSection({ id, title, description, children }: DemoSectionProps) {
  return (
    <section id={id} aria-labelledby={`${id}-tieu-de`} className="scroll-mt-6">
      <SectionHeader id={`${id}-tieu-de`} eyebrow="Design system" title={title} description={description} />
      <div className="flex flex-col gap-6">{children}</div>
    </section>
  );
}

/** Nhóm nhỏ trong một mục, có nhãn. */
export function DemoGroup({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="rounded-md border bg-card p-4 sm:p-5">
      <h3 className="mb-4 type-eyebrow text-primary">{label}</h3>
      {children}
    </div>
  );
}
