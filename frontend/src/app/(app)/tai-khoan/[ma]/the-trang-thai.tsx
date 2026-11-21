"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
import { KeyValueList } from "@/components/data/key-value-list";
import { StatusBadge } from "@/components/data/status-badge";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { SectionHeader } from "@/components/layout/section-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { api, client } from "@/lib/api/api";
import type { components } from "@/lib/api/schema";
import { formatNgayGio } from "@/lib/format";
import { khoaChiTiet, TONE_TRANG_THAI, type TaiKhoanChiTiet } from "@/lib/tai-khoan";
import { MatKhauTamDialog } from "../mat-khau-tam-dialog";
import { DoiTrangThaiDialog } from "./doi-trang-thai-dialog";

type DatLaiMatKhauResponse = components["schemas"]["DatLaiMatKhauResponse"];

type TheTrangThaiProps = { taiKhoan: TaiKhoanChiTiet; laAdmin: boolean; laChinhMinh: boolean };

/**
 * Thẻ Trạng thái và bảo mật (spec 0008, AC-10, AC-12, AC-17). Nút mà backend chắc chắn từ chối thì ẩn: tự khóa
 * hay tự đặt lại mật khẩu của mình, BQL đổi trạng thái tài khoản đang Ngừng hoạt động.
 */
export function TheTrangThai({ taiKhoan, laAdmin, laChinhMinh }: TheTrangThaiProps) {
  const queryClient = useQueryClient();
  const [matKhauTam, setMatKhauTam] = useState<string | null>(null);
  const nhomNut = useRef<HTMLDivElement>(null);
  // Nút vừa bấm đã biến mất sau khi đổi trạng thái: đưa focus tới nút đầu tiên còn lại (thường là nút mới hiện ra)
  const focusNutDau = () => requestAnimationFrame(() => nhomNut.current?.querySelector("button")?.focus());
  const trangThai = taiKhoan.trangThaiTaiKhoan;
  const doiDuoc = !laChinhMinh && (laAdmin || trangThai !== "Ngừng hoạt động");
  const datLai = useMutation({
    mutationFn: () =>
      api<DatLaiMatKhauResponse>(
        client.POST("/api/v1/accounts/{maNguoiDung}/reset-password", {
          params: { path: { maNguoiDung: taiKhoan.maNguoiDung } },
        }),
      ),
    // Không giữ mật khẩu tạm trong cache của mutation (AC-17)
    gcTime: 0,
  });

  return (
    <Card className="lg:col-span-2">
      <SectionHeader as="h2" title="Trạng thái và bảo mật" />
      <KeyValueList
        className="mt-4"
        items={[
          {
            label: "Trạng thái",
            value: <StatusBadge tone={TONE_TRANG_THAI[trangThai]}>{trangThai}</StatusBadge>,
          },
          { label: "Mật khẩu", value: taiKhoan.phaiDoiMatKhau ? "Đang dùng mật khẩu tạm" : "Người dùng đã tự đặt" },
          { label: "Đổi mật khẩu gần nhất", value: formatNgayGio(taiKhoan.doiMatKhauLuc) },
        ]}
      />
      {laChinhMinh ? (
        <p className="mt-4 type-caption text-muted-foreground">
          Đây là tài khoản của bạn. Hãy đổi mật khẩu ở trang Đổi mật khẩu.
        </p>
      ) : (
        <div ref={nhomNut} className="mt-4 flex flex-wrap gap-2">
          {doiDuoc && trangThai === "Hoạt động" && (
            <DoiTrangThaiDialog
              taiKhoan={taiKhoan}
              trangThaiMoi="Tạm khóa"
              title="Tạm khóa tài khoản này?"
              description="Người dùng bị đăng xuất khỏi mọi thiết bị và không đăng nhập được cho tới khi được mở lại."
              confirmLabel="Tạm khóa"
              tone="destructive"
              onFocusSauKhiDoi={focusNutDau}
              trigger={
                <Button type="button" variant="outline">
                  Tạm khóa
                </Button>
              }
            />
          )}
          {doiDuoc && trangThai !== "Hoạt động" && (
            <DoiTrangThaiDialog
              taiKhoan={taiKhoan}
              trangThaiMoi="Hoạt động"
              title="Mở lại tài khoản này?"
              description="Người dùng đăng nhập lại được; số lần đăng nhập sai được đặt về 0."
              confirmLabel="Mở lại"
              onFocusSauKhiDoi={focusNutDau}
              trigger={<Button type="button">Mở lại</Button>}
            />
          )}
          {doiDuoc && trangThai !== "Ngừng hoạt động" && (
            <DoiTrangThaiDialog
              taiKhoan={taiKhoan}
              trangThaiMoi="Ngừng hoạt động"
              title="Ngừng hoạt động tài khoản này?"
              description={
                laAdmin
                  ? "Dùng khi người này đã nghỉ việc hoặc chuyển đi. Chỉ quản trị viên mở lại được."
                  : "Dùng khi cư dân đã chuyển đi. Sau đó chỉ quản trị viên mở lại được."
              }
              confirmLabel="Ngừng hoạt động"
              onFocusSauKhiDoi={focusNutDau}
              tone="destructive"
              trigger={
                <Button type="button" variant="outline">
                  Ngừng hoạt động
                </Button>
              }
            />
          )}
          <ConfirmDialog
            trigger={
              <Button type="button" variant="outline">
                Đặt lại mật khẩu
              </Button>
            }
            title="Đặt lại mật khẩu?"
            description="Hệ thống sinh mật khẩu tạm mới và đăng xuất người dùng khỏi mọi thiết bị. Người dùng phải đổi mật khẩu ở lần đăng nhập tới."
            confirmLabel="Đặt lại mật khẩu"
            onConfirm={async () => {
              const data = await datLai.mutateAsync();
              void queryClient.invalidateQueries({ queryKey: khoaChiTiet(taiKhoan.maNguoiDung) });
              setMatKhauTam(data.matKhauTam);
            }}
          />
        </div>
      )}
      <MatKhauTamDialog matKhau={matKhauTam} hoTen={taiKhoan.hoTen} onClose={() => setMatKhauTam(null)} />
    </Card>
  );
}
