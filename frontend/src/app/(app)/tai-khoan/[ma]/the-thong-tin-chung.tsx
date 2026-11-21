"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import * as z from "zod";
import { KeyValueList } from "@/components/data/key-value-list";
import { notifyInfo, notifySuccess } from "@/components/feedback/notify";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { SectionHeader } from "@/components/layout/section-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { api, client } from "@/lib/api/api";
import { formatNgayGio } from "@/lib/format";
import { CAU_LECH_PHIEN_BAN, khoaChiTiet, laLechPhienBan, type TaiKhoanChiTiet } from "@/lib/tai-khoan";

const schema = z.object({
  hoTen: z.string().trim().min(1, "Vui lòng nhập họ tên").max(100, "Họ tên tối đa 100 ký tự"),
  soDienThoai: z.string().trim().min(1, "Vui lòng nhập số điện thoại"),
  email: z.string().trim().min(1, "Vui lòng nhập email").max(150, "Email không hợp lệ"),
});

type Values = z.output<typeof schema>;

/** Thẻ Thông tin chung (spec 0008, AC-7, AC-17): xem, bấm Sửa thì sửa tại chỗ, gửi kèm `phienBan`. */
export function TheThongTinChung({ taiKhoan }: { taiKhoan: TaiKhoanChiTiet }) {
  const [dangSua, setDangSua] = useState(false);
  const queryClient = useQueryClient();
  const form = useApiForm({
    schema,
    defaultValues: { hoTen: taiKhoan.hoTen, soDienThoai: taiKhoan.soDienThoai, email: taiKhoan.email },
  });
  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<TaiKhoanChiTiet>(
        client.PUT("/api/v1/accounts/{maNguoiDung}", {
          params: { path: { maNguoiDung: taiKhoan.maNguoiDung } },
          body: { ...values, phienBan: taiKhoan.phienBan },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    try {
      const data = await mutation.mutateAsync(values);
      queryClient.setQueryData(khoaChiTiet(taiKhoan.maNguoiDung), data);
      void queryClient.invalidateQueries({ queryKey: ["tai-khoan", "danh-sach"] });
      notifySuccess("Đã lưu thông tin tài khoản");
      setDangSua(false);
    } catch (error) {
      if (!laLechPhienBan(error)) throw error;
      notifyInfo(CAU_LECH_PHIEN_BAN);
      await queryClient.invalidateQueries({ queryKey: khoaChiTiet(taiKhoan.maNguoiDung) });
      setDangSua(false);
    }
  });

  function batDauSua() {
    form.reset({ hoTen: taiKhoan.hoTen, soDienThoai: taiKhoan.soDienThoai, email: taiKhoan.email });
    setDangSua(true);
  }

  return (
    <Card>
      <SectionHeader
        as="h2"
        title="Thông tin chung"
        actions={
          !dangSua && (
            <Button type="button" variant="outline" size="sm" onClick={batDauSua}>
              Sửa
            </Button>
          )
        }
      />
      {dangSua ? (
        <form onSubmit={onSubmit} noValidate aria-label="Sửa thông tin chung" className="mt-4 flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup className="grid gap-5 sm:grid-cols-2">
            <FormField
              className="sm:col-span-2"
              control={form.control}
              name="hoTen"
              label="Họ tên"
              required
              render={(field) => <Input autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="soDienThoai"
              label="Số điện thoại"
              required
              render={(field) => <Input type="tel" inputMode="tel" autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="email"
              label="Email"
              required
              render={(field) => <Input type="email" autoComplete="off" spellCheck={false} {...field} />}
            />
          </FieldGroup>
          <div className="flex flex-wrap gap-2">
            <SubmitButton form={form}>Lưu</SubmitButton>
            <Button type="button" variant="outline" onClick={() => setDangSua(false)}>
              Hủy
            </Button>
          </div>
        </form>
      ) : (
        <KeyValueList
          className="mt-4"
          items={[
            { label: "Mã người dùng", value: taiKhoan.maNguoiDung },
            { label: "Họ tên", value: taiKhoan.hoTen },
            { label: "Số điện thoại", value: taiKhoan.soDienThoai },
            { label: "Email", value: <span className="break-all">{taiKhoan.email}</span> },
            { label: "Tạo lúc", value: formatNgayGio(taiKhoan.taoLuc) },
            { label: "Đăng nhập gần nhất", value: formatNgayGio(taiKhoan.dangNhapCuoiLuc) },
          ]}
        />
      )}
    </Card>
  );
}
