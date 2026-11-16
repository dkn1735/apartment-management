"use client";

import { KeyValueList } from "@/components/data/key-value-list";
import { PageHeader } from "@/components/layout/page-header";
import { Card, CardTitle } from "@/components/ui/card";
import { useHoSo } from "@/lib/auth/use-ho-so";
import { NHAN_VAI_TRO } from "@/lib/vai-tro";

/** Trang hồ sơ (spec 0004, AC-14). */
export function HoSoView() {
  const { data } = useHoSo();

  return (
    <>
      <PageHeader title="Hồ sơ của tôi" />
      {data && (
        <Card>
          <CardTitle>Thông tin tài khoản</CardTitle>
          <KeyValueList
            items={[
              { label: "Họ và tên", value: data.hoTen },
              { label: "Mã người dùng", value: data.maNguoiDung },
              { label: "Số điện thoại", value: data.soDienThoai },
              { label: "Email", value: data.email },
              { label: "Trạng thái", value: data.trangThaiTaiKhoan },
              {
                label: "Vai trò",
                value: data.vaiTro.map((v) => NHAN_VAI_TRO[v]).join(", ") || "Chưa được gán vai trò",
              },
            ]}
          />
        </Card>
      )}
    </>
  );
}
