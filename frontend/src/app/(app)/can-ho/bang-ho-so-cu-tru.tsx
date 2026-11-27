"use client";

import { StatusBadge } from "@/components/data/status-badge";
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { toneTrangThaiHoSo, toneVaiTroCuTru, type HoSoCuTru } from "@/lib/cu-tru";
import { EMPTY_VALUE, formatNgay } from "@/lib/format";
import { KetThucCuTruDialog } from "./ket-thuc-cu-tru-dialog";
import { SuaHoSoCuTruDialog } from "./sua-ho-so-cu-tru-dialog";

type NhomBangCuTru = "dang-cu-tru" | "sap-bat-dau" | "lich-su";

type BangHoSoCuTruProps = {
  danhSach: readonly HoSoCuTru[];
  nhom: NhomBangCuTru;
  /** BQL xem đủ PII, người xác minh và nút thao tác; cư dân chỉ thấy thông tin chung (spec 0014, AC-2, AC-16, AC-17). */
  laBql: boolean;
  caption: string;
};

/** Bảng danh sách hồ sơ cư trú của căn hộ theo nhóm Hiện tại, Sắp bắt đầu hoặc Lịch sử cư trú. */
export function BangHoSoCuTru({ danhSach, nhom, laBql, caption }: BangHoSoCuTruProps) {
  const laLichSu = nhom === "lich-su";
  const hienThaoTac = laBql && (!laLichSu || danhSach.some((h) => h.trangThaiHoSo === "Hợp lệ"));

  return (
    <div className="overflow-x-auto rounded-md border bg-card">
      <Table>
        <TableCaption className="sr-only">{caption}</TableCaption>
        <TableHeader>
          <TableRow>
            <TableHead scope="col">Họ và tên</TableHead>
            <TableHead scope="col">Vai trò cư trú</TableHead>
            <TableHead scope="col">Tình trạng</TableHead>
            {laBql && <TableHead scope="col">Số điện thoại</TableHead>}
            {laBql && <TableHead scope="col">Số CCCD</TableHead>}
            <TableHead scope="col">Ngày bắt đầu</TableHead>
            <TableHead scope="col">Ngày kết thúc</TableHead>
            {laLichSu && <TableHead scope="col">Trạng thái</TableHead>}
            {laBql && <TableHead scope="col">Người xác minh</TableHead>}
            {hienThaoTac && <TableHead scope="col">Thao tác</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {danhSach.map((hoSo) => {
            const ten = hoSo.hoTen ?? hoSo.maCuDan;
            const nguoiXacMinh = hoSo.hoTenNguoiXacMinh ?? hoSo.maNvbqlXacMinh ?? EMPTY_VALUE;
            const choSua = laBql && !laLichSu;
            const choKetThuc = laBql && (!laLichSu || hoSo.trangThaiHoSo === "Hợp lệ");

            return (
              <TableRow key={hoSo.maHoSo}>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium text-foreground">{ten}</span>
                    {laBql && <span className="type-caption text-muted-foreground">{hoSo.maCuDan}</span>}
                  </div>
                </TableCell>
                <TableCell>
                  <StatusBadge tone={toneVaiTroCuTru(hoSo.vaiTroCuTru)}>{hoSo.vaiTroCuTru}</StatusBadge>
                </TableCell>
                <TableCell>{hoSo.tinhTrangCuTru}</TableCell>
                {laBql && <TableCell>{hoSo.soDienThoai ?? EMPTY_VALUE}</TableCell>}
                {laBql && <TableCell>{hoSo.soCccd ?? EMPTY_VALUE}</TableCell>}
                <TableCell>{formatNgay(hoSo.ngayBatDau)}</TableCell>
                <TableCell>{hoSo.ngayKetThuc ? formatNgay(hoSo.ngayKetThuc) : "Không thời hạn"}</TableCell>
                {laLichSu && (
                  <TableCell>
                    <StatusBadge tone={toneTrangThaiHoSo(hoSo.trangThaiHoSo)}>{hoSo.trangThaiHoSo}</StatusBadge>
                  </TableCell>
                )}
                {laBql && <TableCell>{nguoiXacMinh}</TableCell>}
                {hienThaoTac && (
                  <TableCell>
                    <div className="flex flex-wrap items-center gap-2">
                      {choSua && <SuaHoSoCuTruDialog hoSo={hoSo} />}
                      {choKetThuc && <KetThucCuTruDialog hoSo={hoSo} laHuySom={nhom === "sap-bat-dau"} />}
                    </div>
                  </TableCell>
                )}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
