"use client";

import { Lock } from "lucide-react";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { notifySuccess } from "@/components/feedback/notify";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { fakeSubmit } from "./demo-api";
import { DemoGroup } from "./demo-section";

/** Dialog thường và ConfirmDialog thành công, lỗi. */
export function DialogDemo() {
  return (
    <DemoGroup label="Hộp thoại">
      <div className="flex flex-wrap gap-3">
        <Dialog>
          <DialogTrigger asChild>
            <Button variant="outline">Mở hộp thoại</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Thông tin phí dịch vụ</DialogTitle>
              <DialogDescription>Phí quản lý tính theo diện tích thông thủy của căn hộ.</DialogDescription>
            </DialogHeader>
            <p>Đơn giá hiện hành là 12.000 ₫ mỗi m² mỗi tháng, áp dụng từ 01/01/2026.</p>
            <DialogFooter>
              <DialogClose asChild>
                <Button>Đã hiểu</Button>
              </DialogClose>
            </DialogFooter>
          </DialogContent>
        </Dialog>
        <ConfirmDialog
          trigger={
            <Button variant="destructive">
              <Lock aria-hidden="true" />
              Khóa tài khoản (thành công)
            </Button>
          }
          title="Khóa tài khoản này?"
          description="Người dùng sẽ không đăng nhập được cho tới khi được mở khóa."
          confirmLabel="Khóa tài khoản"
          tone="destructive"
          onConfirm={async () => {
            await fakeSubmit("ok");
            notifySuccess("Đã khóa tài khoản");
          }}
        />
        <ConfirmDialog
          trigger={<Button variant="outline">Gửi thông báo (lỗi)</Button>}
          title="Gửi thông báo cho cư dân?"
          description="Thông báo sẽ được gửi tới 312 căn hộ của tòa A."
          confirmLabel="Gửi thông báo"
          onConfirm={() => fakeSubmit("internal")}
        />
      </div>
    </DemoGroup>
  );
}
