"use client";

import { useQuery } from "@tanstack/react-query";
import { Car, ClipboardList } from "lucide-react";
import { useId, useState } from "react";
import { EmptyState } from "@/components/feedback/empty-state";
import { QueryErrorState } from "@/components/feedback/query-error-state";
import { SectionHeader } from "@/components/layout/section-header";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { api, client, type ApiError } from "@/lib/api/api";
import { khoaPhuongTienCanHo, type PhuongTienCanHo } from "@/lib/phuong-tien";
import { BangPhuongTien } from "./bang-phuong-tien";
import { DangKyPhuongTienDialog } from "./dang-ky-phuong-tien-dialog";

type KhoiPhuongTienProps = {
  maCanHo: string;
  laBql: boolean;
  laGocNhinNhanVien: boolean;
  choPhepDangKy: boolean;
};

/**
 * Khối "Phương tiện" trên trang chi tiết căn hộ (spec 0016, AC-14, AC-15, AC-16).
 * Tải độc lập để lỗi API xe không che mất thông tin căn hộ phía trên.
 */
export function KhoiPhuongTien({ maCanHo, laBql, laGocNhinNhanVien, choPhepDangKy }: KhoiPhuongTienProps) {
  const headingId = useId();
  const [tab, setTab] = useState("dang-gui");

  const query = useQuery<PhuongTienCanHo, ApiError>({
    queryKey: khoaPhuongTienCanHo(maCanHo),
    queryFn: () =>
      api<PhuongTienCanHo>(
        client.GET("/api/v1/apartments/{apartmentId}/vehicles", { params: { path: { apartmentId: maCanHo } } }),
      ),
  });

  const dangGui = query.data?.dangGui ?? [];
  const donVaLichSu = query.data?.donVaLichSu ?? [];
  const soChoDuyet = donVaLichSu.filter((xe) => xe.trangThaiDangKy === "Chờ duyệt").length;

  return (
    <section aria-labelledby={headingId} className="mt-10">
      <SectionHeader
        id={headingId}
        as="h2"
        title="Phương tiện"
        description={
          laGocNhinNhanVien
            ? "Danh sách xe đang gửi, thẻ xe tạm dừng, đơn chờ duyệt và lịch sử đăng ký của căn hộ."
            : "Xe đang gửi tại căn hộ, đơn đăng ký xe chờ duyệt và lịch sử của bạn."
        }
        actions={
          choPhepDangKy ? (
            <DangKyPhuongTienDialog
              maCanHo={maCanHo}
              laBql={laBql}
              onThanhCong={laBql ? undefined : () => setTab("don-va-lich-su")}
            />
          ) : undefined
        }
      />
      {query.isPending ? (
        <Card aria-busy="true">
          <CardContent className="flex flex-col gap-3">
            <span className="sr-only">Đang tải phương tiện</span>
            <Skeleton className="h-9 w-56" />
            <Skeleton className="h-32 w-full" />
          </CardContent>
        </Card>
      ) : query.isError ? (
        <QueryErrorState
          headingLevel="h3"
          title="Không tải được danh sách phương tiện"
          error={query.error}
          onRetry={() => query.refetch()}
          retrying={query.isFetching}
        />
      ) : (
        <Tabs value={tab} onValueChange={setTab}>
          <TabsList>
            <TabsTrigger value="dang-gui">Đang gửi</TabsTrigger>
            <TabsTrigger value="don-va-lich-su">
              {soChoDuyet > 0 ? `Đơn và lịch sử (${soChoDuyet})` : "Đơn và lịch sử"}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="dang-gui">
            {dangGui.length === 0 ? (
              <EmptyState
                icon={Car}
                title="Chưa có phương tiện đang gửi"
                description={
                  choPhepDangKy
                    ? laBql
                      ? "Đăng ký phương tiện mới cho cư dân đang cư trú tại căn hộ."
                      : "Gửi đơn đăng ký xe để Ban quản lý xem xét cấp thẻ xe."
                    : "Căn hộ hiện chưa có phương tiện nào đang hoạt động hoặc tạm dừng."
                }
              />
            ) : (
              <BangPhuongTien
                danhSach={dangGui}
                caption="Danh sách phương tiện đang gửi của căn hộ"
                laBql={laBql}
                laGocNhinNhanVien={laGocNhinNhanVien}
              />
            )}
          </TabsContent>
          <TabsContent value="don-va-lich-su">
            {donVaLichSu.length === 0 ? (
              <EmptyState
                icon={ClipboardList}
                title="Chưa có đơn hoặc lịch sử phương tiện"
                description={
                  laGocNhinNhanVien
                    ? "Các đơn chờ duyệt, bị từ chối hoặc thẻ xe đã hủy của căn hộ sẽ hiện tại đây."
                    : "Các đơn đăng ký xe của bạn hoặc gắn với xe của bạn sẽ hiện tại đây."
                }
              />
            ) : (
              <BangPhuongTien
                danhSach={donVaLichSu}
                caption="Danh sách đơn đăng ký và lịch sử phương tiện của căn hộ"
                laBql={laBql}
                laGocNhinNhanVien={laGocNhinNhanVien}
              />
            )}
          </TabsContent>
        </Tabs>
      )}
    </section>
  );
}
