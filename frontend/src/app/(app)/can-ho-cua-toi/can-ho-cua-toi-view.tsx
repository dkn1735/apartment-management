"use client";

import { House } from "lucide-react";
import Link from "next/link";
import { KeyValueList } from "@/components/data/key-value-list";
import { EmptyState } from "@/components/feedback/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { formatDienTich } from "@/lib/can-ho";
import { formatSo } from "@/lib/format";
import { ChiTietCanHo, useChiTietCanHo } from "../can-ho/chi-tiet-can-ho";

/**
 * Căn hộ của tôi (spec 0012, AC-20): danh sách căn lấy từ `phamVi.maCanHo` của hồ sơ. Không căn nào thì hiện trạng
 * thái rỗng; một căn thì hiện luôn chi tiết; nhiều căn thì mỗi căn một thẻ tự tải.
 */
export function CanHoCuaToiView() {
  const hoSo = useHoSo();
  if (hoSo.isPending) {
    return (
      <div aria-busy="true" className="flex flex-col gap-6">
        <span className="sr-only">Đang tải</span>
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }
  const maCanHo = hoSo.data?.phamVi.maCanHo ?? [];
  if (maCanHo.length === 1) {
    return <ChiTietCanHo maCanHo={maCanHo[0]} />;
  }
  return (
    <>
      <PageHeader title="Căn hộ của tôi" description="Các căn hộ bạn đang cư trú theo hồ sơ cư trú hợp lệ." />
      {maCanHo.length === 0 ? (
        <EmptyState
          icon={House}
          title="Bạn chưa có hồ sơ cư trú hợp lệ"
          description="Liên hệ ban quản lý để được xác minh hồ sơ cư trú."
        />
      ) : (
        <ul aria-label="Căn hộ đang cư trú" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {maCanHo.map((ma) => (
            <li key={ma}>
              <TheCanHo ma={ma} />
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

/** Một thẻ căn, tự tải; lỗi của thẻ này không ảnh hưởng thẻ khác. */
function TheCanHo({ ma }: { ma: string }) {
  const query = useChiTietCanHo(ma);
  return (
    <Card className="h-full">
      <CardHeader>
        <h2 className="type-card-title">
          <Link href={`/can-ho-cua-toi/${ma}`}>Căn hộ {ma}</Link>
        </h2>
      </CardHeader>
      <CardContent>
        {query.isPending ? (
          <div aria-busy="true" className="flex flex-col gap-2">
            <span className="sr-only">Đang tải căn hộ {ma}</span>
            <Skeleton className="h-6 w-full" />
            <Skeleton className="h-6 w-2/3" />
          </div>
        ) : query.isError ? (
          <p className="type-ui text-muted-foreground">
            {query.error.errorCode === "NOT_FOUND" ? "Không còn quyền xem căn này" : "Không tải được thông tin căn hộ"}
          </p>
        ) : (
          <KeyValueList
            items={[
              { label: "Tòa nhà", value: query.data.tenToaNha },
              { label: "Tầng", value: formatSo(query.data.soTang) },
              { label: "Diện tích", value: formatDienTich(query.data.dienTich) },
            ]}
          />
        )}
      </CardContent>
    </Card>
  );
}
