"use client";

import { useQuery } from "@tanstack/react-query";
import { ClipboardCheck, History, Users } from "lucide-react";
import { useId, useState } from "react";
import { EmptyState } from "@/components/feedback/empty-state";
import { QueryErrorState } from "@/components/feedback/query-error-state";
import { SectionHeader } from "@/components/layout/section-header";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { api, client, type ApiError } from "@/lib/api/api";
import { khoaHoSoCuTru, type HoSoCuTruCanHo } from "@/lib/cu-tru";
import { BangDeNghiCuTru } from "./bang-de-nghi-cu-tru";
import { BangHoSoCuTru } from "./bang-ho-so-cu-tru";
import { GuiDeNghiCuTruDialog } from "./gui-de-nghi-cu-tru-dialog";
import { ThemHoSoCuTruDialog } from "./them-ho-so-cu-tru-dialog";

type KhoiHoSoCuTruProps = {
  maCanHo: string;
  /** `true` trên trang quản lý của BQL; `false` trên trang căn hộ của cư dân. */
  laBql: boolean;
};

/**
 * Khối "Hồ sơ cư trú" trên trang chi tiết căn hộ (spec 0014, 0015, AC-15, AC-16, AC-17, AC-18).
 * Tải độc lập với thẻ thông tin căn hộ phía trên để lỗi hoặc độ trễ không che mất thông tin căn.
 */
export function KhoiHoSoCuTru({ maCanHo, laBql }: KhoiHoSoCuTruProps) {
  const headingId = useId();
  const [tab, setTab] = useState("hien-tai");
  const query = useQuery<HoSoCuTruCanHo, ApiError>({
    queryKey: khoaHoSoCuTru(maCanHo),
    queryFn: () =>
      api<HoSoCuTruCanHo>(
        client.GET("/api/v1/apartments/{apartmentId}/residents", { params: { path: { apartmentId: maCanHo } } }),
      ),
  });

  const dangCuTru = query.data?.dangCuTru ?? [];
  const sapBatDau = query.data?.sapBatDau ?? [];
  const lichSuCuTru = query.data?.lichSuCuTru ?? [];
  const deNghiCuTru = query.data?.deNghiCuTru ?? [];
  const soChoXacMinh = deNghiCuTru.filter((d) => d.trangThaiHoSo === "Chờ xác minh").length;

  return (
    <section aria-labelledby={headingId} className="mt-10">
      <SectionHeader
        id={headingId}
        as="h2"
        title="Hồ sơ cư trú"
        description={
          laBql
            ? "Nhân khẩu đang cư trú, hồ sơ sắp bắt đầu, lịch sử và các đề nghị cư trú của căn hộ."
            : "Thành viên đang cùng cư trú, lịch sử cư trú và đề nghị cập nhật hồ sơ của bạn."
        }
        actions={
          laBql ? (
            <ThemHoSoCuTruDialog maCanHo={maCanHo} />
          ) : (
            <GuiDeNghiCuTruDialog maCanHo={maCanHo} onThanhCong={() => setTab("de-nghi")} />
          )
        }
      />
      {query.isPending ? (
        <Card aria-busy="true">
          <CardContent className="flex flex-col gap-3">
            <span className="sr-only">Đang tải hồ sơ cư trú</span>
            <Skeleton className="h-9 w-56" />
            <Skeleton className="h-32 w-full" />
          </CardContent>
        </Card>
      ) : query.isError ? (
        <QueryErrorState
          headingLevel="h3"
          title="Không tải được hồ sơ cư trú"
          error={query.error}
          onRetry={() => query.refetch()}
          retrying={query.isFetching}
        />
      ) : (
        <Tabs value={tab} onValueChange={setTab}>
          <TabsList>
            <TabsTrigger value="hien-tai">Hiện tại</TabsTrigger>
            <TabsTrigger value="lich-su">Lịch sử cư trú</TabsTrigger>
            <TabsTrigger value="de-nghi">
              {soChoXacMinh > 0 ? `Đề nghị cư trú (${soChoXacMinh})` : "Đề nghị cư trú"}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="hien-tai">
            {dangCuTru.length === 0 && sapBatDau.length === 0 ? (
              <EmptyState
                icon={Users}
                title="Căn hộ chưa có người đang cư trú"
                description={
                  laBql
                    ? "Thêm hồ sơ cư trú khi cư dân chuyển vào căn hộ."
                    : "Hiện chưa có hồ sơ cư trú đang hiệu lực tại căn hộ này."
                }
              />
            ) : (
              <div className="flex flex-col gap-6">
                {dangCuTru.length > 0 ? (
                  <BangHoSoCuTru
                    danhSach={dangCuTru}
                    nhom="dang-cu-tru"
                    laBql={laBql}
                    caption="Danh sách nhân khẩu đang cư trú"
                  />
                ) : (
                  <p className="rounded-md border bg-card px-4 py-3 type-ui text-muted-foreground">
                    Căn hộ chưa có người đang cư trú hôm nay.
                  </p>
                )}
                {sapBatDau.length > 0 && (
                  <div className="flex flex-col gap-3">
                    <h3 className="type-card-title">Sắp bắt đầu</h3>
                    <BangHoSoCuTru
                      danhSach={sapBatDau}
                      nhom="sap-bat-dau"
                      laBql={laBql}
                      caption="Danh sách hồ sơ cư trú sắp bắt đầu"
                    />
                  </div>
                )}
              </div>
            )}
          </TabsContent>
          <TabsContent value="lich-su">
            {!laBql && <p className="mb-3 type-caption text-muted-foreground">Chỉ hiển thị lịch sử cư trú của bạn.</p>}
            {lichSuCuTru.length === 0 ? (
              <EmptyState
                icon={History}
                title="Chưa có lịch sử cư trú"
                description={
                  laBql
                    ? "Các hồ sơ cư trú đã kết thúc sẽ được lưu tại đây."
                    : "Bạn chưa có đợt cư trú nào đã kết thúc tại căn hộ này."
                }
              />
            ) : (
              <BangHoSoCuTru danhSach={lichSuCuTru} nhom="lich-su" laBql={laBql} caption="Lịch sử cư trú của căn hộ" />
            )}
          </TabsContent>
          <TabsContent value="de-nghi">
            {!laBql && (
              <p className="mb-3 type-caption text-muted-foreground">
                Chỉ hiển thị các đề nghị do bạn gửi hoặc đề nghị liên quan đến bạn.
              </p>
            )}
            {deNghiCuTru.length === 0 ? (
              <EmptyState
                icon={ClipboardCheck}
                title="Chưa có đề nghị cư trú"
                description={
                  laBql
                    ? "Căn hộ hiện không có đề nghị cư trú nào từ cư dân."
                    : "Bấm Gửi đề nghị cư trú để đề nghị thêm thành viên gia đình, khách thuê hoặc đăng ký tạm trú."
                }
              />
            ) : (
              <BangDeNghiCuTru danhSach={deNghiCuTru} laBql={laBql} />
            )}
          </TabsContent>
        </Tabs>
      )}
    </section>
  );
}
