import {
  Building2,
  LayoutDashboard,
  MapPinned,
  ScrollText,
  UserRound,
  UsersRound,
  type LucideIcon,
} from "lucide-react";
import type { NavItem } from "@/components/layout/nav";
import type { VaiTro } from "@/lib/vai-tro";

type MucCoBan = { href: string; label: string; vaiTro: readonly VaiTro[] };

/** Mục đã bật: hiện ở menu và trang Tổng quan, nên phải có icon và mô tả. */
export type MucSanSang = MucCoBan & { sanSang: true; moTa: string; icon: LucideIcon };

/** Mục chưa bật: không hiện ở đâu, nhưng vẫn chặn trang nếu ai đó tạo trang mà quên bật cờ. */
export type MucChuaSanSang = MucCoBan & { sanSang: false };

export type MucChucNang = MucSanSang | MucChuaSanSang;

/** Hai mục cho mọi người đã đăng nhập; không thuộc ma trận chặn trang. */
export const MUC_CHUNG: readonly NavItem[] = [
  { href: "/", label: "Tổng quan", icon: LayoutDashboard },
  { href: "/ho-so", label: "Hồ sơ", icon: UserRound },
];

/**
 * Ma trận chức năng (spec 0006, mục Ma trận chức năng): nguồn duy nhất cho menu, trang Tổng quan và chặn trang.
 * Mỗi phân hệ sở hữu dòng của mình; khi làm phân hệ, thêm `icon`, `moTa` và bật `sanSang`.
 * Vai trò ở đây chỉ để tiện dụng, backend vẫn là nơi quyết định cuối cùng.
 */
export const MUC_CHUC_NANG: readonly MucChucNang[] = [
  {
    href: "/toa-nha",
    label: "Tòa nhà",
    vaiTro: ["BAN_QUAN_LY", "BAN_QUAN_TRI", "KE_TOAN", "KY_THUAT"],
    sanSang: true,
    moTa: "Danh mục tòa nhà, số tầng và số căn hộ",
    icon: Building2,
  },
  { href: "/can-ho", label: "Căn hộ", vaiTro: ["BAN_QUAN_LY", "BAN_QUAN_TRI", "KE_TOAN"], sanSang: false },
  {
    href: "/khu-vuc-chung",
    label: "Khu vực chung",
    vaiTro: ["BAN_QUAN_LY", "KY_THUAT"],
    sanSang: true,
    moTa: "Sảnh, hành lang, phòng kỹ thuật của từng tòa",
    icon: MapPinned,
  },
  { href: "/ho-so-cu-tru", label: "Hồ sơ cư trú", vaiTro: ["BAN_QUAN_LY"], sanSang: false },
  { href: "/can-ho-cua-toi", label: "Căn hộ của tôi", vaiTro: ["CU_DAN"], sanSang: false },
  { href: "/chi-so", label: "Chỉ số điện nước", vaiTro: ["BAN_QUAN_LY", "KE_TOAN", "KY_THUAT"], sanSang: false },
  { href: "/bieu-phi", label: "Biểu phí", vaiTro: ["BAN_QUAN_LY", "KE_TOAN"], sanSang: false },
  { href: "/thong-bao-phi", label: "Thông báo phí", vaiTro: ["BAN_QUAN_TRI", "KE_TOAN"], sanSang: false },
  { href: "/doi-soat", label: "Đối soát", vaiTro: ["KE_TOAN"], sanSang: false },
  { href: "/phi-cua-toi", label: "Phí của tôi", vaiTro: ["CU_DAN"], sanSang: false },
  { href: "/yeu-cau-ho-tro", label: "Yêu cầu hỗ trợ", vaiTro: ["CU_DAN"], sanSang: false },
  { href: "/dieu-phoi", label: "Điều phối sự cố", vaiTro: ["BAN_QUAN_LY"], sanSang: false },
  { href: "/cong-viec", label: "Công việc của tôi", vaiTro: ["KY_THUAT"], sanSang: false },
  { href: "/bao-tri", label: "Bảo trì", vaiTro: ["BAN_QUAN_LY"], sanSang: false },
  { href: "/thiet-bi", label: "Thiết bị kỹ thuật", vaiTro: ["BAN_QUAN_LY", "KY_THUAT"], sanSang: false },
  { href: "/tien-ich", label: "Tiện ích", vaiTro: ["BAN_QUAN_LY", "CU_DAN"], sanSang: false },
  { href: "/check-in", label: "Check in tiện ích", vaiTro: ["BAN_QUAN_LY", "BAO_VE"], sanSang: false },
  { href: "/thong-bao", label: "Thông báo", vaiTro: ["BAN_QUAN_LY", "CU_DAN"], sanSang: false },
  { href: "/khao-sat", label: "Khảo sát", vaiTro: ["BAN_QUAN_LY", "BAN_QUAN_TRI", "CU_DAN"], sanSang: false },
  { href: "/bao-cao", label: "Báo cáo", vaiTro: ["BAN_QUAN_LY", "BAN_QUAN_TRI", "KE_TOAN"], sanSang: false },
  {
    href: "/nhat-ky",
    label: "Nhật ký hệ thống",
    vaiTro: ["QUAN_TRI_VIEN", "BAN_QUAN_LY", "BAN_QUAN_TRI"],
    sanSang: true,
    moTa: "Lịch sử đăng nhập, đăng xuất và thao tác cần kiểm toán",
    icon: ScrollText,
  },
  {
    href: "/tai-khoan",
    label: "Tài khoản",
    vaiTro: ["QUAN_TRI_VIEN", "BAN_QUAN_LY"],
    sanSang: true,
    moTa: "Tạo, tìm, khóa tài khoản và đặt lại mật khẩu",
    icon: UsersRound,
  },
];
