"use client";

import { Search } from "lucide-react";
import { useEffect, useId, useState, type ReactNode } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn } from "cn";

/** Giá trị "tất cả" của ô chọn lọc; không bao giờ lên URL. */
const TAT_CA = "TAT_CA";

/** Ô tìm gửi sau khi ngừng gõ chừng này mili giây. */
export const CHO_GO_MS = 300;

export type OTimKiemProps = {
  nhan: string;
  goiY: ReactNode;
  giaTri: string;
  onTim(q: string): void;
  className?: string;
};

/**
 * Ô tìm: gửi sau 300 ms ngừng gõ, hoặc ngay khi nhấn Enter. Ô không bị dựng lại khi URL đổi, để không mất focus
 * lúc đang gõ; chỉ lấy giá trị từ URL khi nó đổi từ bên ngoài (Back, Xóa bộ lọc), không phải do chính ô vừa gửi.
 */
export function OTimKiem({ nhan, goiY, giaTri, onTim, className }: OTimKiemProps) {
  const id = useId();
  const [nhap, setNhap] = useState(giaTri);
  const [daGui, setDaGui] = useState(giaTri);
  const [giaTriTruoc, setGiaTriTruoc] = useState(giaTri);

  // Đồng bộ trong lúc render khi prop đổi (mẫu "lưu prop trước đó" của React), không dùng effect
  if (giaTri !== giaTriTruoc) {
    setGiaTriTruoc(giaTri);
    if (giaTri !== daGui.trim()) {
      setNhap(giaTri);
      setDaGui(giaTri);
    }
  }

  function gui(q: string) {
    setDaGui(q);
    onTim(q);
  }

  useEffect(() => {
    if (nhap.trim() === daGui.trim()) return;
    const hen = setTimeout(() => {
      setDaGui(nhap);
      onTim(nhap);
    }, CHO_GO_MS);
    return () => clearTimeout(hen);
  }, [nhap, daGui, onTim]);

  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <Label htmlFor={id}>{nhan}</Label>
      <div className="relative">
        <Search
          aria-hidden="true"
          className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
        />
        <Input
          id={id}
          type="search"
          className="pl-9"
          value={nhap}
          maxLength={100}
          aria-describedby={`${id}-goi-y`}
          onChange={(e) => setNhap(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              gui(nhap);
            }
          }}
        />
      </div>
      <p id={`${id}-goi-y`} className="type-caption text-muted-foreground">
        {goiY}
      </p>
    </div>
  );
}

export type ChonLocProps = {
  nhan: string;
  giaTri: string | undefined;
  tatCa: string;
  luaChon: readonly { value: string; label: string }[];
  onChon(giaTri: string | undefined): void;
};

export function ChonLoc({ nhan, giaTri, tatCa, luaChon, onChon }: ChonLocProps) {
  const id = useId();
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{nhan}</Label>
      <Select value={giaTri ?? TAT_CA} onValueChange={(v) => onChon(v === TAT_CA ? undefined : v)}>
        <SelectTrigger id={id}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={TAT_CA}>{tatCa}</SelectItem>
          {luaChon.map((lc) => (
            <SelectItem key={lc.value} value={lc.value}>
              {lc.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
