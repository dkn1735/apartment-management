"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";
import { useWatch } from "react-hook-form";
import * as z from "zod";
import { notifySuccess } from "@/components/feedback/notify";
import { SelectControl } from "@/components/form/field-controls";
import { FormAlert } from "@/components/form/form-alert";
import { FormField } from "@/components/form/form-field";
import { SubmitButton } from "@/components/form/submit-button";
import { useApiForm } from "@/components/form/use-api-form";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { SelectItem } from "@/components/ui/select";
import { api, client, type ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import {
  CAN_HO_QUERY_KEY,
  dienTichGuiDi,
  maCanSeTao,
  TRANG_THAI_SU_DUNG,
  type CanHo,
  type TrangThaiSuDung,
} from "@/lib/can-ho";
import { soTangGuiDi, TOA_NHA_QUERY_KEY, type Tang, type ToaNha } from "@/lib/toa-nha";

// Chỉ kiểm ô rỗng ở trình duyệt, cùng câu với backend; số căn và diện tích để server kiểm (AC-8, AC-18)
const schema = z.object({
  maToaNha: z.string().min(1, "Vui lòng chọn tòa nhà"),
  maTang: z.string().min(1, "Vui lòng chọn tầng"),
  soCan: z.string(),
  dienTich: z.string(),
  trangThaiSuDung: z.string(),
});

type Values = z.output<typeof schema>;

const GIA_TRI_DAU: Values = { maToaNha: "", maTang: "", soCan: "", dienTich: "", trangThaiSuDung: "Trống" };

/**
 * Nút và hộp thoại "Thêm căn hộ" (spec 0012, AC-18). BQL chọn tòa, tầng và nhập số căn; mã căn do server ghép, hộp
 * thoại chỉ xem trước. Lỗi 400 và 409 gắn vào đúng ô (409 ở ô số căn).
 */
export function ThemCanHoDialog({ toaNha }: { toaNha: readonly ToaNha[] }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const form = useApiForm({ schema, defaultValues: GIA_TRI_DAU });
  const [maToaNha, maTang, soCan] = useWatch({ control: form.control, name: ["maToaNha", "maTang", "soCan"] });

  const tang = useQuery<PageResponse<Tang>, ApiError>({
    queryKey: [...TOA_NHA_QUERY_KEY, "tang", maToaNha, "lua-chon"],
    queryFn: () =>
      api<PageResponse<Tang>>(
        client.GET("/api/v1/buildings/{buildingId}/floors", {
          params: { path: { buildingId: maToaNha }, query: { size: 100, sort: "soTang,asc" } },
        }),
      ),
    enabled: open && Boolean(maToaNha),
  });

  const mutation = useMutation({
    mutationFn: (values: Values) =>
      api<CanHo>(
        client.POST("/api/v1/apartments", {
          body: {
            maTang: values.maTang,
            // Chuỗi không phải số vẫn gửi đi để server trả lỗi đúng trường
            soCan: soTangGuiDi(values.soCan) as number,
            dienTich: dienTichGuiDi(values.dienTich) as number,
            trangThaiSuDung: values.trangThaiSuDung as TrangThaiSuDung,
          },
        }),
      ),
  });

  const onSubmit = form.submitWith(async (values) => {
    const canHo = await mutation.mutateAsync(values);
    // soCanHo của tòa và tầng đổi, nên làm mới cả gốc khóa tòa nhà
    void queryClient.invalidateQueries({ queryKey: CAN_HO_QUERY_KEY });
    void queryClient.invalidateQueries({ queryKey: TOA_NHA_QUERY_KEY });
    notifySuccess(`Đã thêm căn hộ ${canHo.maCanHo}`);
    setOpen(false);
  });

  const xemTruoc = maCanSeTao(maTang || undefined, soCan);

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (next) form.reset(GIA_TRI_DAU);
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <Plus aria-hidden="true" />
          Thêm căn hộ
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Thêm căn hộ</DialogTitle>
          <DialogDescription>
            Mã căn được ghép từ mã tầng và số căn, ví dụ tầng A-01 và căn số 5 thành A-0105.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} noValidate aria-label="Thêm căn hộ" className="flex flex-col gap-5">
          <FormAlert error={form.formError} focusKey={form.formErrorKey} />
          <FieldGroup>
            <FormField
              control={form.control}
              name="maToaNha"
              label="Tòa nhà"
              required
              render={(field) => (
                <SelectControl
                  placeholder="Chọn tòa nhà"
                  field={{
                    ...field,
                    onChange: (giaTri: string) => {
                      field.onChange(giaTri);
                      form.setValue("maTang", "");
                    },
                  }}
                >
                  {toaNha.map((t) => (
                    <SelectItem key={t.maToaNha} value={t.maToaNha}>
                      {t.tenToaNha}
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
            />
            <FormField
              control={form.control}
              name="maTang"
              label="Tầng"
              required
              description={maToaNha ? undefined : "Chọn tòa nhà trước"}
              render={(field) => (
                <fieldset disabled={!maToaNha} className="contents">
                  <SelectControl field={field} placeholder="Chọn tầng">
                    {(tang.data?.items ?? []).map((t) => (
                      <SelectItem key={t.maTang} value={t.maTang}>
                        Tầng {t.soTang} ({t.maTang})
                      </SelectItem>
                    ))}
                  </SelectControl>
                </fieldset>
              )}
            />
            <FormField
              control={form.control}
              name="soCan"
              label="Số căn"
              description="Từ 1 đến 99"
              required
              render={(field) => <Input inputMode="numeric" autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="dienTich"
              label="Diện tích (m²)"
              description="Lớn hơn 0, tối đa 1000, tối đa 2 chữ số thập phân, ví dụ 75,5"
              required
              render={(field) => <Input inputMode="decimal" autoComplete="off" {...field} />}
            />
            <FormField
              control={form.control}
              name="trangThaiSuDung"
              label="Trạng thái sử dụng"
              render={(field) => (
                <SelectControl field={field}>
                  {TRANG_THAI_SU_DUNG.map((tt) => (
                    <SelectItem key={tt} value={tt}>
                      {tt}
                    </SelectItem>
                  ))}
                </SelectControl>
              )}
            />
          </FieldGroup>
          <p aria-live="polite" className="type-ui text-muted-foreground">
            {xemTruoc && (
              <>
                Mã căn sẽ tạo: <span className="font-medium text-foreground">{xemTruoc}</span>
              </>
            )}
          </p>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                Hủy
              </Button>
            </DialogClose>
            <SubmitButton form={form}>Thêm căn hộ</SubmitButton>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
