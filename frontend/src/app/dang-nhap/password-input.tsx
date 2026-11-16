"use client";

import { Eye, EyeOff } from "lucide-react";
import { useState, type ComponentProps } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

/**
 * Ô mật khẩu có nút hiện/ẩn (spec 0004, AC-12). Nhãn nút cố định, trạng thái báo bằng `aria-pressed`.
 * Tính năng đổi mật khẩu (10) cần tới thì chuyển sang `components/form` và thêm vào /dev/ui.
 */
export function PasswordInput(props: Omit<ComponentProps<typeof Input>, "type">) {
  const [hien, setHien] = useState(false);
  const Icon = hien ? EyeOff : Eye;

  return (
    <div className="relative">
      <Input {...props} type={hien ? "text" : "password"} className="pr-12" />
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="Hiện mật khẩu"
        aria-pressed={hien}
        aria-controls={props.id}
        onClick={() => setHien((value) => !value)}
        className="absolute top-1/2 right-1 -translate-y-1/2"
      >
        <Icon aria-hidden="true" />
      </Button>
    </div>
  );
}
