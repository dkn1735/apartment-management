"use client";

import { Building2, Home, RotateCcw, ShieldCheck, UserRound, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { KeyValueList } from "@/components/data/key-value-list";
import { StatusBadge, type StatusTone } from "@/components/data/status-badge";
import { ErrorState } from "@/components/feedback/error-state";
import { PageHeader } from "@/components/layout/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError } from "@/lib/api/api";
import { laLoiChuyenVeDangNhap, useHoSo, type HoSo } from "@/lib/auth/use-ho-so";
import { chiTietCua, NHAN_VAI_TRO } from "@/lib/vai-tro";

const TONE_TRANG_THAI: Readonly<Record<string, StatusTone>> = {
  "Hoạt động": "success",
  "Tạm khóa": "warning",
  "Ngừng hoạt động": "neutral",
};

/** Trang hồ sơ (spec 0004, AC-14): thông tin tài khoản, vai trò, phạm vi. Không bao giờ hiện CCCD. */
export function HoSoView() {
  const { data, error, isError, refetch, isRefetching } = useHoSo();

  return (
    <>
      <PageHeader title="Hồ sơ của tôi" description="Thông tin hệ thống đang ghi nhận cho tài khoản của bạn." />
      {data ? (
        <NoiDung hoSo={data} />
      ) : isError && !laLoiChuyenVeDangNhap(error) ? (
        <Card>
          <ErrorState
            title="Chưa tải được hồ sơ"
            error={error instanceof ApiError ? error : null}
            actions={
              <Button variant="outline" loading={isRefetching} onClick={() => void refetch()}>
                <RotateCcw aria-hidden="true" />
                Thử lại
              </Button>
            }
          />
        </Card>
      ) : (
        <DangTai />
      )}
    </>
  );
}

function NoiDung({ hoSo }: { hoSo: HoSo }) {
  return (
    <div className="grid items-start gap-6 lg:grid-cols-2">
      <Khoi id="tai-khoan" icon={UserRound} title="Thông tin tài khoản">
        <KeyValueList
          items={[
            { label: "Họ và tên", value: hoSo.hoTen },
            { label: "Mã người dùng", value: hoSo.maNguoiDung },
            { label: "Số điện thoại", value: hoSo.soDienThoai },
            { label: "Email", value: hoSo.email },
            {
              label: "Trạng thái",
              value: (
                <StatusBadge tone={TONE_TRANG_THAI[hoSo.trangThaiTaiKhoan] ?? "neutral"}>
                  {hoSo.trangThaiTaiKhoan}
                </StatusBadge>
              ),
            },
          ]}
        />
      </Khoi>

      <Khoi id="pham-vi" icon={Building2} title="Phạm vi">
        <KeyValueList
          items={[
            { label: "Tòa nhà phụ trách", value: <DanhSachMa ma={hoSo.phamVi.maToaNha} /> },
            { label: "Căn hộ cư trú", value: <DanhSachMa ma={hoSo.phamVi.maCanHo} /> },
          ]}
        />
      </Khoi>

      <Khoi id="vai-tro" icon={ShieldCheck} title="Vai trò" className="lg:col-span-2">
        {hoSo.vaiTro.length === 0 ? (
          <p className="type-ui text-muted-foreground">Chưa được gán vai trò.</p>
        ) : (
          <ul className="grid gap-4 md:grid-cols-2">
            {hoSo.vaiTro.map((vaiTro) => {
              const dong = chiTietCua(vaiTro, hoSo.chiTietVaiTro);
              return (
                <li key={vaiTro} className="flex flex-col gap-3 rounded-md border p-4">
                  <h3 className="flex items-center gap-2 type-card-title">
                    {vaiTro === "CU_DAN" ? (
                      <Home aria-hidden="true" className="size-[18px] text-primary" />
                    ) : (
                      <ShieldCheck aria-hidden="true" className="size-[18px] text-primary" />
                    )}
                    {NHAN_VAI_TRO[vaiTro]}
                  </h3>
                  {dong.length > 0 && <KeyValueList variant="divided" items={dong} />}
                </li>
              );
            })}
          </ul>
        )}
      </Khoi>
    </div>
  );
}

/** Danh sách mã dạng chip; rỗng thì "Không có". */
function DanhSachMa({ ma }: { ma: string[] }) {
  if (ma.length === 0) {
    return <span className="font-normal text-muted-foreground">Không có</span>;
  }
  return (
    <span className="inline-flex flex-wrap gap-1.5 sm:justify-end">
      {ma.map((value) => (
        <Badge key={value}>{value}</Badge>
      ))}
    </span>
  );
}

type KhoiProps = { id: string; icon: LucideIcon; title: string; className?: string; children: ReactNode };

/** Một thẻ có tiêu đề `h2`, gắn nhãn vùng bằng tiêu đề đó. */
function Khoi({ id, icon: Icon, title, className, children }: KhoiProps) {
  return (
    <Card className={className}>
      <section aria-labelledby={`ho-so-${id}`} className="flex flex-col gap-4">
        <h2 id={`ho-so-${id}`} className="flex items-center gap-2 type-card-title">
          <span className="flex size-8 items-center justify-center rounded-md bg-primary-soft">
            <Icon aria-hidden="true" className="size-[18px] text-primary" />
          </span>
          {title}
        </h2>
        {children}
      </section>
    </Card>
  );
}

function DangTai() {
  return (
    <div role="status" aria-busy="true" className="grid gap-6 lg:grid-cols-2">
      <span className="sr-only">Đang tải hồ sơ</span>
      {[5, 2].map((rows, index) => (
        <Card key={index}>
          <Skeleton className="h-8 w-48" />
          {Array.from({ length: rows }, (_, row) => (
            <Skeleton key={row} className="h-10 w-full" />
          ))}
        </Card>
      ))}
      <Card className="lg:col-span-2">
        <Skeleton className="h-8 w-32" />
        <Skeleton className="h-24 w-full" />
      </Card>
    </div>
  );
}
