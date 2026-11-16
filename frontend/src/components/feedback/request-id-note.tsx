"use client";

import { Copy } from "lucide-react";
import { useRef, useState } from "react";
import { cn } from "cn";
import { Button } from "@/components/ui/button";

type RequestIdNoteProps = { requestId: string; className?: string };

/** "Mã tra cứu: …" kèm nút sao chép, cho lỗi hệ thống (AC-7). */
export function RequestIdNote({ requestId, className }: RequestIdNoteProps) {
  const codeRef = useRef<HTMLElement>(null);
  const [status, setStatus] = useState("");

  async function copy() {
    try {
      await navigator.clipboard.writeText(requestId);
      setStatus("Đã sao chép");
    } catch {
      // Trình duyệt chặn sao chép: bôi chọn sẵn mã để người dùng tự sao chép
      const node = codeRef.current;
      const selection = window.getSelection();
      if (node && selection) {
        const range = document.createRange();
        range.selectNodeContents(node);
        selection.removeAllRanges();
        selection.addRange(range);
      }
      setStatus("Hãy nhấn Ctrl+C để sao chép");
    }
  }

  return (
    <div className={cn("flex flex-wrap items-center gap-x-2 gap-y-1 type-caption", className)}>
      <span>
        Mã tra cứu:{" "}
        <code ref={codeRef} className="font-medium break-all select-all">
          {requestId}
        </code>
      </span>
      <Button variant="ghost" size="icon-sm" aria-label="Sao chép mã tra cứu" onClick={copy}>
        <Copy />
      </Button>
      <span aria-live="polite" className="text-muted-foreground">
        {status}
      </span>
    </div>
  );
}
