package vn.edu.hust.chungcu.auth.dto;

/**
 * Định danh của một cư dân cho phân hệ cư trú (spec 0014). Có dữ liệu cá nhân: nơi nhận
 * phải tự ẩn khi người xem chỉ có quyền cư dân.
 */
public record ThongTinCuDan(String maCuDan, String maNguoiDung, String hoTen, String soDienThoai, String email,
		String soCccd, String trangThaiTaiKhoan) {
}
