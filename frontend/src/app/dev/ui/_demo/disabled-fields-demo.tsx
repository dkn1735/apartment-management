"use client";

import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldContent, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";

/** Trạng thái vô hiệu của mọi loại ô nhập (AC-3): mờ 50%, con trỏ not-allowed, không nhận focus. */
export function DisabledFieldsDemo() {
  return (
    <FieldGroup className="grid gap-5 md:grid-cols-2">
      <Field>
        <FieldLabel htmlFor="vo-hieu-ma-can-ho">Mã căn hộ</FieldLabel>
        <Input id="vo-hieu-ma-can-ho" disabled defaultValue="A-1205" aria-describedby="vo-hieu-ma-can-ho-goi-y" />
        <FieldDescription id="vo-hieu-ma-can-ho-goi-y">Mã do hệ thống cấp, không sửa được.</FieldDescription>
      </Field>
      <Field>
        <FieldLabel htmlFor="vo-hieu-loai">Loại căn hộ</FieldLabel>
        <Select disabled defaultValue="2pn">
          <SelectTrigger id="vo-hieu-loai">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="2pn">2 phòng ngủ</SelectItem>
          </SelectContent>
        </Select>
      </Field>
      <Field className="md:col-span-2">
        <FieldLabel htmlFor="vo-hieu-ghi-chu">Ghi chú</FieldLabel>
        <Textarea id="vo-hieu-ghi-chu" disabled defaultValue="Hồ sơ đã khóa sau khi bàn giao." />
      </Field>
      <Field orientation="horizontal">
        <Switch id="vo-hieu-thong-bao" disabled defaultChecked />
        <FieldContent>
          <FieldLabel htmlFor="vo-hieu-thong-bao">Nhận thông báo qua email</FieldLabel>
        </FieldContent>
      </Field>
      <Field orientation="horizontal">
        <Checkbox id="vo-hieu-xac-nhan" disabled defaultChecked />
        <FieldContent>
          <FieldLabel htmlFor="vo-hieu-xac-nhan">Đã xác nhận thông tin</FieldLabel>
        </FieldContent>
      </Field>
    </FieldGroup>
  );
}
