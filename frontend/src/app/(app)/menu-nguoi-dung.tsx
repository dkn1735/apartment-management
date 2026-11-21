"use client";

import { ChevronDown, KeyRound, LogOut, UserRound } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Spinner } from "@/components/ui/spinner";
import { useDangXuat } from "@/lib/auth/use-dang-xuat";
import { useHoSo } from "@/lib/auth/use-ho-so";

// Viền focus rõ trên nền menu, ngoài nền --accent mặc định của mục
const MUC = "min-h-10 gap-2.5 px-2.5 type-ui no-underline focus-visible:ring-2 focus-visible:ring-ring";

/**
 * Menu người dùng ở góc phải thanh trên (spec 0005, AC-13): họ tên là nút mở menu "Hồ sơ",
 * "Đổi mật khẩu" và "Đăng xuất". Khi hồ sơ đang tải hoặc lỗi, nút hiện "Tài khoản" để đăng xuất luôn dùng được.
 */
export function MenuNguoiDung() {
  const { data } = useHoSo();
  const dangXuat = useDangXuat();
  const ten = data?.hoTen ?? "Tài khoản";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="h-10 max-w-64 gap-2 px-1.5 sm:px-2">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-soft">
            <UserRound aria-hidden="true" className="size-4 text-primary" />
          </span>
          <span className="truncate type-ui font-medium">{ten}</span>
          <span className="sr-only">, mở menu tài khoản</span>
          <ChevronDown aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" sideOffset={8} className="w-auto min-w-56">
        {data && (
          <>
            <DropdownMenuLabel className="flex flex-col gap-0.5 px-2.5 py-2">
              <span className="truncate type-ui font-medium text-foreground">{data.hoTen}</span>
              <span className="truncate type-caption">{data.email ?? data.soDienThoai}</span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
          </>
        )}
        <DropdownMenuItem asChild className={MUC}>
          <Link href="/ho-so">
            <UserRound aria-hidden="true" />
            Hồ sơ
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild className={MUC}>
          <Link href="/doi-mat-khau">
            <KeyRound aria-hidden="true" />
            Đổi mật khẩu
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem
          className={MUC}
          disabled={dangXuat.isPending}
          // Giữ menu mở để thấy trạng thái đang đăng xuất; thành công thì trang tải lại
          onSelect={(event) => {
            event.preventDefault();
            dangXuat.mutate();
          }}
        >
          {dangXuat.isPending ? <Spinner aria-hidden="true" /> : <LogOut aria-hidden="true" />}
          {dangXuat.isPending ? "Đang đăng xuất…" : "Đăng xuất"}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
