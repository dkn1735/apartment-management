import type { ReactNode } from "react";

type DemoSectionProps = { id: string; title: string; description?: string; children: ReactNode };

/** Một mục trên trang /dev/ui, có id để mục lục trỏ tới. */
export function DemoSection({ id, title, description, children }: DemoSectionProps) {
  return (
    <section id={id} aria-labelledby={`${id}-tieu-de`} className="scroll-mt-6">
      <h2 id={`${id}-tieu-de`} className="type-section-title">
        {title}
      </h2>
      {description && <p className="mt-1 type-ui text-muted-foreground">{description}</p>}
      <div className="mt-5 flex flex-col gap-6">{children}</div>
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
