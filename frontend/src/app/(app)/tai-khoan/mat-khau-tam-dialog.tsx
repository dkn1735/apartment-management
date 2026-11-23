"use client";

import { Copy, TriangleAlert } from "lucide-react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

type MatKhauTamDialogProps = {
  /** Mật khẩu tạm vừa nhận; `null` là đóng. Chỉ nằm trong state của nơi gọi, không vào cache hay URL (AC-17). */
  matKhau: string | null;
  hoTen: string;
  onClose(): void;
};

/**
 * Hiện mật khẩu tạm đúng một lần sau khi tạo tài khoản hoặc đặt lại mật khẩu (spec 0008, AC-17). Không đóng được
 * bằng cách bấm ra ngoài, để người quản lý không lỡ tay mất mật khẩu.
 */
export function MatKhauTamDialog({ matKhau, hoTen, onClose }: MatKhauTamDialogProps) {
  const [thongBao, setThongBao] = useState("");

  async function saoChep() {
    if (!matKhau) return;
    try {
      await navigator.clipboard.writeText(matKhau);
      setThongBao("Đã sao chép");
    } catch {
      setThongBao("Không sao chép được, hãy bôi chọn mật khẩu và nhấn Ctrl+C");
    }
  }

  return (
    <Dialog
      open={matKhau !== null}
      onOpenChange={(open) => {
        if (!open) {
          setThongBao("");
          onClose();
        }
      }}
    >
      <DialogContent showCloseButton={false} onInteractOutside={(event) => event.preventDefault()}>
        <DialogHeader>
          <DialogTitle>Mật khẩu tạm của {hoTen}</DialogTitle>
          <DialogDescription>
            Người dùng đăng nhập bằng mật khẩu này và sẽ được yêu cầu đặt mật khẩu mới ngay lần đầu.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <p
            className="rounded-md border bg-muted px-4 py-3 text-center font-mono text-2xl tracking-widest break-all select-all"
            aria-label={`Mật khẩu tạm: ${matKhau?.split("").join(" ") ?? ""}`}
          >
            {matKhau}
          </p>
          <div className="flex items-center gap-3">
            <Button type="button" variant="outline" onClick={saoChep}>
              <Copy aria-hidden="true" />
              Sao chép
            </Button>
            <span role="status" aria-live="polite" className="type-caption text-muted-foreground">
              {thongBao}
            </span>
          </div>
          <Alert tone="warning">
            <TriangleAlert aria-hidden="true" />
            <AlertDescription>Mật khẩu chỉ hiện một lần. Hãy giao cho người dùng trước khi đóng.</AlertDescription>
          </Alert>
        </div>
        <DialogFooter>
          <Button
            type="button"
            onClick={() => {
              setThongBao("");
              onClose();
            }}
          >
            Tôi đã lưu mật khẩu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
