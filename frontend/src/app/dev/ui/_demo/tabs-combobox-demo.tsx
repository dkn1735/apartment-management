"use client";

import { useState } from "react";
import { Combobox } from "@/components/ui/combobox";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DemoGroup } from "./demo-section";

const CUDAN_MAU = [
  { value: "CD00000001", label: "Vũ Cư Dân (0900000006)" },
  { value: "CD00000002", label: "Nguyễn Thị Ánh Tuyết (0900000007)" },
  { value: "CD00000003", label: "Trần Minh Quân (0900000009)" },
];

/** Demo Tabs và Combobox ở các trạng thái mặc định, đang tải, rỗng, vô hiệu và báo lỗi. */
export function TabsComboboxDemo() {
  const [giaTri, setGiaTri] = useState("");
  const [tuKhoa, setTuKhoa] = useState("");

  const loc = CUDAN_MAU.filter((c) => c.label.toLowerCase().includes(tuKhoa.trim().toLowerCase()));

  return (
    <>
      <DemoGroup label="Tabs">
        <Tabs defaultValue="hien-tai">
          <TabsList>
            <TabsTrigger value="hien-tai">Hiện tại</TabsTrigger>
            <TabsTrigger value="lich-su">Lịch sử cư trú</TabsTrigger>
            <TabsTrigger value="khoa" disabled>
              Chưa mở
            </TabsTrigger>
          </TabsList>
          <TabsContent value="hien-tai">
            <p className="rounded-md border bg-card p-4 type-ui">
              Nội dung tab Hiện tại: điều hướng bằng phím mũi tên.
            </p>
          </TabsContent>
          <TabsContent value="lich-su">
            <p className="rounded-md border bg-card p-4 type-ui">Nội dung tab Lịch sử cư trú.</p>
          </TabsContent>
        </Tabs>
      </DemoGroup>
      <DemoGroup label="Combobox">
        <div className="grid max-w-2xl gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="demo-cb-chinh">Cư dân (mặc định)</Label>
            <Combobox
              id="demo-cb-chinh"
              value={giaTri}
              onValueChange={setGiaTri}
              options={loc}
              placeholder="Chọn cư dân"
              searchValue={tuKhoa}
              onSearchChange={setTuKhoa}
              searchLabel="Tìm cư dân mẫu"
              listLabel="Danh sách cư dân mẫu"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="demo-cb-vo-hieu">Cư dân (vô hiệu)</Label>
            <Combobox
              id="demo-cb-vo-hieu"
              value=""
              onValueChange={() => {}}
              options={CUDAN_MAU}
              placeholder="Không khả dụng"
              searchValue=""
              onSearchChange={() => {}}
              disabled
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="demo-cb-tai">Cư dân (đang tải)</Label>
            <Combobox
              id="demo-cb-tai"
              value=""
              onValueChange={() => {}}
              options={[]}
              placeholder="Mở để xem trạng thái tải"
              searchValue=""
              onSearchChange={() => {}}
              loading
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="demo-cb-loi">Cư dân (lỗi tải)</Label>
            <Combobox
              id="demo-cb-loi"
              value=""
              onValueChange={() => {}}
              options={[]}
              placeholder="Mở để xem trạng thái lỗi"
              searchValue=""
              onSearchChange={() => {}}
              aria-invalid={true}
              errorText="Không tải được danh sách cư dân."
            />
          </div>
        </div>
      </DemoGroup>
    </>
  );
}
