import { ApiError } from "@/lib/api/api";
import type { PageResponse } from "@/lib/api/page";
import type { TableSort } from "@/components/data/table-params";

export type CanHoMau = {
  maCanHo: string;
  toaNha: string;
  dienTich: number;
  chuHo: string;
  phiThang: number;
  trangThai: "dang-o" | "trong" | "sua-chua";
  ngayBanGiao: string;
  ghiChu: string;
};

const HO = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Hồ"];
const TEN = ["Thị Ánh Tuyết", "Văn Đức", "Minh Khôi", "Ngọc Hân", "Quốc Bảo", "Thu Trang", "Gia Huy", "Bảo Ngọc"];
const GHI_CHU = [
  "Căn góc, hai mặt thoáng, đã lắp thêm lưới an toàn ban công theo đăng ký với ban quản lý tháng trước.",
  "",
  "Chủ hộ đang ở nước ngoài, người thuê liên hệ qua số điện thoại đã đăng ký.",
  "Đang chờ nghiệm thu sửa chữa đường ống nước nóng trong phòng tắm chính.",
];
const TRANG_THAI: CanHoMau["trangThai"][] = ["dang-o", "dang-o", "trong", "dang-o", "sua-chua"];

/** 135 căn hộ giả, sinh cố định để demo bảng. */
export const CAN_HO_MAU: CanHoMau[] = Array.from({ length: 135 }, (_, i) => {
  const toa = "ABC"[i % 3];
  const tang = String(3 + (i % 25)).padStart(2, "0");
  const dienTich = 48 + ((i * 7) % 70);
  return {
    maCanHo: `${toa}-${tang}${String(1 + (i % 12)).padStart(2, "0")}`,
    toaNha: `Tòa ${toa}`,
    dienTich,
    chuHo: `${HO[i % HO.length]} ${TEN[(i * 3) % TEN.length]}`,
    phiThang: dienTich * 12000,
    trangThai: TRANG_THAI[i % TRANG_THAI.length],
    ngayBanGiao: `20${18 + (i % 8)}-${String(1 + (i % 12)).padStart(2, "0")}-${String(1 + (i % 28)).padStart(2, "0")}`,
    ghiChu: GHI_CHU[i % GHI_CHU.length],
  };
});

export type DemoTableMode = "data" | "empty" | "error";

/** API danh sách giả: trễ 600ms, sắp xếp và cắt trang như server. */
export async function fetchCanHoMau(
  params: { page: number; size: number; sort?: TableSort },
  mode: DemoTableMode,
): Promise<PageResponse<CanHoMau>> {
  await new Promise((resolve) => setTimeout(resolve, 600));
  if (mode === "error") {
    throw new ApiError({ status: 503, errorCode: "BACKEND_UNAVAILABLE", message: "raw", requestId: "d4c3b2a1-0f9e" });
  }
  const all = mode === "empty" ? [] : [...CAN_HO_MAU];
  if (params.sort) {
    const { field, dir } = params.sort;
    const key = field as keyof CanHoMau;
    all.sort((a, b) => {
      const result = String(a[key]).localeCompare(String(b[key]), "vi", { numeric: true });
      return dir === "asc" ? result : -result;
    });
  }
  const start = (params.page - 1) * params.size;
  return {
    items: all.slice(start, start + params.size),
    page: params.page,
    size: params.size,
    totalItems: all.length,
    totalPages: Math.ceil(all.length / params.size),
  };
}
