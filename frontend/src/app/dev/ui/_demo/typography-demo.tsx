import { DemoGroup } from "./demo-section";

const SAMPLE = "Ẳ Ẵ Ặ Ỷ Ữ ặ ỡ ừ";

const SCALE = [
  { name: "Số lớn (type-stat)", className: "type-stat", text: "4.850.000" },
  { name: "Tiêu đề trang (type-page-title)", className: "type-page-title", text: SAMPLE },
  { name: "Tiêu đề mục (type-section-title)", className: "type-section-title", text: SAMPLE },
  { name: "Tiêu đề thẻ (type-card-title)", className: "type-card-title", text: SAMPLE },
  { name: "Thân bài (type-body)", className: "type-body", text: SAMPLE },
  { name: "Bảng, ô nhập, nút (type-ui)", className: "type-ui", text: SAMPLE },
  { name: "Nhãn nhỏ (type-eyebrow)", className: "type-eyebrow text-primary", text: SAMPLE },
  { name: "Chú thích (type-caption)", className: "type-caption text-muted-foreground", text: SAMPLE },
];

/** Chuỗi thử dấu ở mọi cỡ chữ và kiểm tra chữ số đều cột (AC-2). */
export function TypographyDemo() {
  return (
    <>
      <DemoGroup label="Thang chữ, thử dấu tiếng Việt">
        <dl className="flex flex-col gap-4">
          {SCALE.map((item) => (
            <div key={item.name} className="flex flex-col gap-1 border-b pb-4 last:border-b-0 last:pb-0">
              <dt className="type-caption text-muted-foreground">{item.name}</dt>
              <dd className={item.className}>{item.text}</dd>
            </div>
          ))}
        </dl>
      </DemoGroup>
      <DemoGroup label="Chữ số đều cột">
        <p className="type-ui text-muted-foreground">
          Be Vietnam Pro không có tính năng <code>tnum</code>: hai dòng dưới đây khác độ rộng dù bật{" "}
          <code>tabular-nums</code>. Vì vậy cột số trong bảng và ô số liệu căn phải (xem <code>design.md</code>).
        </p>
        <p className="mt-3 inline-flex flex-col type-section-title tabular-nums">
          <span className="w-fit bg-muted">1111</span>
          <span className="w-fit bg-muted">8888</span>
        </p>
        <p className="mt-3 flex w-40 flex-col text-right type-section-title">
          <span>1.111</span>
          <span>88.888</span>
          <span>4.850.000</span>
        </p>
      </DemoGroup>
    </>
  );
}
