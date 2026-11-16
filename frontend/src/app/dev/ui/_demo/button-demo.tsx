import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DemoGroup } from "./demo-section";

const VARIANTS = ["primary", "secondary", "outline", "ghost", "destructive", "link"] as const;

/** Mọi biến thể, cỡ và trạng thái của Button. */
export function ButtonDemo() {
  return (
    <>
      <DemoGroup label="Biến thể">
        <div className="flex flex-wrap items-center gap-3">
          {VARIANTS.map((variant) => (
            <Button key={variant} variant={variant}>
              {variant}
            </Button>
          ))}
        </div>
      </DemoGroup>
      <DemoGroup label="Cỡ và icon">
        <div className="flex flex-wrap items-center gap-3">
          <Button size="sm">Nhỏ</Button>
          <Button size="md">Vừa</Button>
          <Button size="lg">Lớn</Button>
          <Button>
            <Plus aria-hidden="true" />
            Thêm căn hộ
          </Button>
          <Button variant="outline" size="icon" aria-label="Xóa">
            <Trash2 aria-hidden="true" />
          </Button>
        </div>
      </DemoGroup>
      <DemoGroup label="Trạng thái">
        <div className="flex flex-wrap items-center gap-3">
          <Button loading>Đang lưu</Button>
          <Button variant="destructive" loading>
            Đang xóa
          </Button>
          <Button disabled>Vô hiệu</Button>
          <Button variant="outline" disabled>
            Vô hiệu
          </Button>
        </div>
      </DemoGroup>
    </>
  );
}
