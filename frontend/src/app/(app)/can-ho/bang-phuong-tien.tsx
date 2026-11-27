"use client";

import { StatusBadge } from "@/components/data/status-badge";
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { EMPTY_VALUE, formatNgayGio } from "@/lib/format";
import { toneTrangThaiPhuongTien, trangThaiDichHopLe, type PhuongTien } from "@/lib/phuong-tien";
import { DoiTrangThaiXeDialog } from "./doi-trang-thai-xe-dialog";
import { RutDonXeDialog } from "./rut-don-xe-dialog";

type BangPhuongTienProps = {
  danhSach: readonly PhuongTien[];
  caption: string;
  laBql: boolean;
  laGocNhinNhanVien: boolean;
};

/**
 * Bảng phương tiện của căn hộ (spec 0016, AC-14, AC-16).
 */
export function BangPhuongTien({ danhSach, caption, laBql, laGocNhinNhanVien }: BangPhuongTienProps) {
  const hienThaoTac = danhSach.some(
    (xe) => (laBql && trangThaiDichHopLe(xe.trangThaiDangKy).length > 0) || xe.coTheRutDon,
  );

  return (
    <div className="overflow-x-auto rounded-md border bg-card">
      <Table>
        <TableCaption className="sr-only">{caption}</TableCaption>
        <TableHeader>
          <TableRow>
            <TableHead scope="col">Loại xe và nhãn hiệu</TableHead>
            <TableHead scope="col">Biển số</TableHead>
            <TableHead scope="col">Chủ xe</TableHead>
            <TableHead scope="col">Trạng thái</TableHead>
            <TableHead scope="col">Cập nhật</TableHead>
            {hienThaoTac && <TableHead scope="col">Thao tác</TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {danhSach.map((xe) => {
            const coTheDoiTrangThai = laBql && trangThaiDichHopLe(xe.trangThaiDangKy).length > 0;
            const coThaoTac = coTheDoiTrangThai || xe.coTheRutDon;

            return (
              <TableRow key={xe.maPhuongTien}>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium text-foreground">{xe.loaiPhuongTien}</span>
                    <span className="type-caption text-muted-foreground">{xe.nhanHieuMauSac ?? EMPTY_VALUE}</span>
                  </div>
                </TableCell>
                <TableCell className="font-medium whitespace-nowrap">
                  {xe.bienSo ? xe.bienSo : <span className="text-muted-foreground">Không có biển</span>}
                </TableCell>
                <TableCell>
                  <div className="flex flex-col items-start gap-1">
                    <span className="text-foreground">{xe.hoTenChuXe}</span>
                    {laGocNhinNhanVien && !xe.chuXeConCuTru && (
                      <StatusBadge tone="warning">Chủ xe không còn cư trú</StatusBadge>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col items-start gap-1">
                    <StatusBadge tone={toneTrangThaiPhuongTien(xe.trangThaiDangKy)}>{xe.trangThaiDangKy}</StatusBadge>
                    {xe.lyDoTrangThai && (
                      <span
                        className={
                          xe.trangThaiDangKy === "Từ chối"
                            ? "type-caption text-destructive"
                            : "type-caption text-muted-foreground"
                        }
                      >
                        Lý do: {xe.lyDoTrangThai}
                      </span>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-col gap-0.5">
                    <span className="whitespace-nowrap">{formatNgayGio(xe.capNhatLuc)}</span>
                    {laGocNhinNhanVien && xe.hoTenNguoiCapNhat && (
                      <span className="type-caption text-muted-foreground">Bởi: {xe.hoTenNguoiCapNhat}</span>
                    )}
                    {xe.hoTenNguoiDangKy && (
                      <span className="type-caption text-muted-foreground">Người gửi: {xe.hoTenNguoiDangKy}</span>
                    )}
                  </div>
                </TableCell>
                {hienThaoTac && (
                  <TableCell>
                    {coThaoTac && (
                      <div className="flex flex-wrap items-center gap-2">
                        {coTheDoiTrangThai && <DoiTrangThaiXeDialog phuongTien={xe} />}
                        {xe.coTheRutDon && <RutDonXeDialog phuongTien={xe} />}
                      </div>
                    )}
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
