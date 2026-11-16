"use client";

import { useState, type ReactNode } from "react";
import { FormAlert } from "@/components/form/form-alert";
import { useApiError } from "@/components/feedback/use-api-error";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";

export type ConfirmDialogProps = {
  /** Nút mở hộp thoại; focus quay về đây khi đóng. */
  trigger: ReactNode;
  title: string;
  description: ReactNode;
  /** Động từ cụ thể, ví dụ "Khóa tài khoản". */
  confirmLabel: string;
  tone?: "default" | "destructive";
  /** Ném ApiError khi lỗi; hộp thoại giữ mở và hiện lỗi. Thành công thì người gọi tự gọi notifySuccess. */
  onConfirm: () => Promise<void>;
};

/** Hộp thoại xác nhận (spec 0003, Hợp đồng ConfirmDialog). Mở ra với focus ở nút "Hủy". */
export function ConfirmDialog({
  trigger,
  title,
  description,
  confirmLabel,
  tone = "default",
  onConfirm,
}: ConfirmDialogProps) {
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const { error, focusKey, capture, clear } = useApiError();

  function handleOpenChange(next: boolean) {
    if (pending) return;
    setOpen(next);
    if (!next) clear();
  }

  async function confirm() {
    setPending(true);
    try {
      const ok = await capture(onConfirm);
      if (ok) {
        setOpen(false);
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={handleOpenChange}>
      <AlertDialogTrigger asChild>{trigger}</AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <FormAlert error={error} focusKey={focusKey} />
        <AlertDialogFooter>
          <AlertDialogCancel asChild>
            <Button variant="outline">Hủy</Button>
          </AlertDialogCancel>
          {/* Nút xác nhận không dùng AlertDialogAction để hộp thoại không tự đóng khi onConfirm còn chạy */}
          <Button variant={tone === "destructive" ? "destructive" : "primary"} loading={pending} onClick={confirm}>
            {confirmLabel}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
