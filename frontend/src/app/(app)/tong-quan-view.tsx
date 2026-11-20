"use client";

import { ShieldQuestion, UserRound } from "lucide-react";
import Link from "next/link";
import { EmptyState } from "@/components/feedback/empty-state";
import { QueryErrorState } from "@/components/feedback/query-error-state";
import { PageHeader } from "@/components/layout/page-header";
import { SectionHeader } from "@/components/layout/section-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { laLoiChuyenVeDangNhap, useHoSo } from "@/lib/auth/use-ho-so";
import { nhomVaiTro, type NhomVaiTro } from "@/lib/phan-quyen/menu";

/** Trang Tổng quan (spec 0006, AC-14): lời chào và lối tắt theo từng nhóm vai trò của menu. */
export function TongQuanView() {
  const { data, error, isError, refetch, isRefetching } = useHoSo();

  if (!data) {
    if (isError && !laLoiChuyenVeDangNhap(error)) {
      return (
        <>
          <PageHeader title="Tổng quan" />
          <Card>
            <QueryErrorState
              title="Chưa tải được thông tin tài khoản"
              error={error}
              onRetry={refetch}
              retrying={isRefetching}
            />
          </Card>
        </>
      );
    }
    return <DangTai />;
  }

  const nhom = nhomVaiTro(data.vaiTro);
  return (
    <>
      <PageHeader title={`Xin chào, ${data.hoTen}`} description="Các chức năng dành cho vai trò của bạn." />
      {data.vaiTro.length === 0 ? (
        <EmptyState
          icon={ShieldQuestion}
          title="Chưa được gán vai trò"
          description="Vui lòng liên hệ ban quản lý để được gán vai trò."
        />
      ) : nhom.length === 0 ? (
        <EmptyState
          title="Các chức năng cho vai trò của bạn đang được xây dựng"
          action={
            <Button asChild variant="outline">
              <Link href="/ho-so">
                <UserRound aria-hidden="true" />
                Xem hồ sơ
              </Link>
            </Button>
          }
        />
      ) : (
        <div className="flex flex-col gap-10">
          {nhom.map((n) => (
            <NhomLoiTat key={n.vaiTro} nhom={n} />
          ))}
        </div>
      )}
    </>
  );
}

function NhomLoiTat({ nhom }: { nhom: NhomVaiTro }) {
  const id = `tong-quan-${nhom.vaiTro.toLowerCase()}`;
  return (
    <section aria-labelledby={id}>
      <SectionHeader id={id} title={nhom.label} />
      <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {nhom.items.map(({ href, label, moTa, icon: Icon }) => {
          const ma = `${id}-${href.slice(1)}`;
          return (
            <li key={href}>
              <Link
                href={href}
                aria-labelledby={`${ma}-ten`}
                aria-describedby={`${ma}-mo-ta`}
                className="flex h-full items-start gap-4 rounded-lg border bg-card p-5 no-underline transition-colors duration-150 ease-out hover:bg-accent hover:text-accent-foreground"
              >
                <span className="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary-soft">
                  <Icon aria-hidden="true" className="size-5 text-primary" />
                </span>
                <span className="flex min-w-0 flex-col gap-1">
                  <span id={`${ma}-ten`} className="type-card-title text-foreground">
                    {label}
                  </span>
                  <span id={`${ma}-mo-ta`} className="type-ui text-muted-foreground">
                    {moTa}
                  </span>
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

function DangTai() {
  return (
    <div role="status" className="flex flex-col gap-8">
      <span className="sr-only">Đang tải</span>
      <Skeleton className="h-9 w-72" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 3 }, (_, index) => (
          <Skeleton key={index} className="h-24 w-full" />
        ))}
      </div>
    </div>
  );
}
