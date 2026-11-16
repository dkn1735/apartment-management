import { CircleAlert, CircleCheck, Info, TriangleAlert } from "lucide-react";
import { StatusBadge } from "@/components/data/status-badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { DemoGroup } from "./demo-section";

/** Card, StatusBadge, Alert, Skeleton, Separator. */
export function DisplayDemo() {
  return (
    <>
      <DemoGroup label="Card">
        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Căn hộ A-1205</CardTitle>
              <CardDescription>Tòa A, tầng 12, 2 phòng ngủ, 74 m².</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Chủ hộ Nguyễn Thị Ánh Tuyết, 4 nhân khẩu đang cư trú.</p>
            </CardContent>
            <CardFooter>
              <Button size="sm">Xem chi tiết</Button>
              <Button size="sm" variant="ghost">
                Sửa
              </Button>
            </CardFooter>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Đang tải</CardTitle>
              <CardDescription>Skeleton giữ chỗ khi tải lần đầu.</CardDescription>
            </CardHeader>
            <CardContent aria-busy="true">
              <span className="sr-only">Đang tải</span>
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="h-4 w-2/3" />
            </CardContent>
          </Card>
        </div>
      </DemoGroup>
      <DemoGroup label="StatusBadge">
        <div className="flex flex-wrap gap-2">
          <StatusBadge tone="neutral">Nháp</StatusBadge>
          <StatusBadge tone="info">Mới tiếp nhận</StatusBadge>
          <StatusBadge tone="warning">Đang xử lý</StatusBadge>
          <StatusBadge tone="success">Đã thanh toán</StatusBadge>
          <StatusBadge tone="danger">Quá hạn</StatusBadge>
        </div>
        <Separator className="my-4" />
        <p className="type-caption text-muted-foreground">
          Chữ trong chip luôn nói rõ trạng thái; màu chỉ hỗ trợ, không thay chữ.
        </p>
      </DemoGroup>
      <DemoGroup label="Alert">
        <div className="flex flex-col gap-3">
          <Alert tone="info">
            <Info aria-hidden="true" />
            <AlertTitle>Lịch bảo trì thang máy</AlertTitle>
            <AlertDescription>Thang máy số 2 tòa B tạm dừng từ 8:00 đến 11:00 ngày 20/11/2026.</AlertDescription>
          </Alert>
          <Alert tone="success">
            <CircleCheck aria-hidden="true" />
            <AlertTitle>Đã chốt chỉ số điện nước tháng 11</AlertTitle>
          </Alert>
          <Alert tone="warning">
            <TriangleAlert aria-hidden="true" />
            <AlertTitle>Còn 3 hóa đơn chưa thanh toán</AlertTitle>
            <AlertDescription>Hạn chót là 25/11/2026.</AlertDescription>
          </Alert>
          <Alert tone="danger">
            <CircleAlert aria-hidden="true" />
            <AlertTitle>Không tải được danh sách</AlertTitle>
            <AlertDescription>Hệ thống đang gặp sự cố, vui lòng thử lại sau.</AlertDescription>
          </Alert>
        </div>
      </DemoGroup>
    </>
  );
}
