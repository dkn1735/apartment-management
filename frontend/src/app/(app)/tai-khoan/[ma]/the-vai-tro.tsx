"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { KeyValueList } from "@/components/data/key-value-list";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { notifyInfo, notifySuccess } from "@/components/feedback/notify";
import { SectionHeader } from "@/components/layout/section-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { api, client } from "@/lib/api/api";
import {
  CAU_LECH_PHIEN_BAN,
  khoaChiTiet,
  laLechPhienBan,
  TRUONG_CUA_VAI_TRO,
  type TaiKhoanChiTiet,
  type VaiTroTaiKhoan,
} from "@/lib/tai-khoan";
import { NHAN_VAI_TRO, THU_TU_VAI_TRO } from "@/lib/vai-tro";
import { VaiTroDialog } from "./vai-tro-dialog";

type TheVaiTroProps = { taiKhoan: TaiKhoanChiTiet; laAdmin: boolean; laChinhMinh: boolean };

/**
 * Thẻ Vai trò (spec 0008, AC-8, AC-9, AC-17): mỗi vai trò một dòng có Sửa và Gỡ, nút Thêm vai trò. BQL chỉ sửa được
 * số CCCD ở dòng Cư dân. Nút mà backend chắc chắn từ chối thì ẩn (gỡ vai trò cuối, tự gỡ quyền quản trị).
 */
export function TheVaiTro({ taiKhoan, laAdmin, laChinhMinh }: TheVaiTroProps) {
  const coTheThem = THU_TU_VAI_TRO.filter((v) => !taiKhoan.vaiTro.some((x) => x.vaiTro === v));
  return (
    <Card>
      <SectionHeader
        as="h2"
        title="Vai trò"
        actions={
          laAdmin &&
          coTheThem.length > 0 && (
            <VaiTroDialog
              taiKhoan={taiKhoan}
              coTheThem={coTheThem}
              trigger={
                <Button type="button" variant="outline" size="sm">
                  <Plus aria-hidden="true" />
                  Thêm vai trò
                </Button>
              }
            />
          )
        }
      />
      <ul className="mt-4 flex flex-col gap-4">
        {taiKhoan.vaiTro.map((v) => (
          <li key={v.vaiTro} className="rounded-md border p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h3 className="type-card-title">{NHAN_VAI_TRO[v.vaiTro]}</h3>
              <div className="flex flex-wrap gap-2">
                {(laAdmin || v.vaiTro === "CU_DAN") && TRUONG_CUA_VAI_TRO[v.vaiTro].length > 0 && (
                  <VaiTroDialog
                    taiKhoan={taiKhoan}
                    dangSua={v}
                    trigger={
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        aria-label={`Sửa vai trò ${NHAN_VAI_TRO[v.vaiTro]}`}
                      >
                        Sửa
                      </Button>
                    }
                  />
                )}
                {laAdmin && taiKhoan.vaiTro.length > 1 && !(laChinhMinh && v.vaiTro === "QUAN_TRI_VIEN") && (
                  <NutGo taiKhoan={taiKhoan} vaiTro={v} />
                )}
              </div>
            </div>
            <KeyValueList
              className="mt-3"
              variant="divided"
              items={[
                { label: "Mã", value: v.maVaiTro },
                ...TRUONG_CUA_VAI_TRO[v.vaiTro].map((moTa) => ({ label: moTa.nhan, value: v[moTa.ten] ?? "" })),
              ]}
            />
          </li>
        ))}
      </ul>
    </Card>
  );
}

function NutGo({ taiKhoan, vaiTro }: { taiKhoan: TaiKhoanChiTiet; vaiTro: VaiTroTaiKhoan }) {
  const queryClient = useQueryClient();
  const nhan = NHAN_VAI_TRO[vaiTro.vaiTro];
  const mutation = useMutation({
    mutationFn: () =>
      api<TaiKhoanChiTiet>(
        client.DELETE("/api/v1/accounts/{maNguoiDung}/roles/{vaiTro}", {
          params: {
            path: { maNguoiDung: taiKhoan.maNguoiDung, vaiTro: vaiTro.vaiTro },
            query: { phienBan: taiKhoan.phienBan },
          },
        }),
      ),
  });
  return (
    <ConfirmDialog
      trigger={
        <Button type="button" variant="outline" size="sm" aria-label={`Gỡ vai trò ${nhan}`}>
          Gỡ
        </Button>
      }
      title={`Gỡ vai trò ${nhan}?`}
      description="Người dùng mất quyền của vai trò này trong vòng 15 phút. Vai trò đang gắn với dữ liệu khác thì không gỡ được."
      confirmLabel="Gỡ vai trò"
      tone="destructive"
      onConfirm={async () => {
        try {
          const data = await mutation.mutateAsync();
          queryClient.setQueryData(khoaChiTiet(taiKhoan.maNguoiDung), data);
          void queryClient.invalidateQueries({ queryKey: ["tai-khoan", "danh-sach"] });
          notifySuccess(`Đã gỡ vai trò ${nhan}`);
        } catch (error) {
          if (!laLechPhienBan(error)) throw error;
          notifyInfo(CAU_LECH_PHIEN_BAN);
          await queryClient.invalidateQueries({ queryKey: khoaChiTiet(taiKhoan.maNguoiDung) });
        }
      }}
    />
  );
}
