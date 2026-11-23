package vn.edu.hust.chungcu.auth.dto;

/**
 * Các trường riêng của vai trò mà người quản lý gửi lên (spec 0008, bảng VaiTroRequest).
 * Trường không thuộc vai trò đã chọn phải vắng hoặc {@code null}.
 */
public interface TruongVaiTroNhap {

	String chucVu();

	String boPhan();

	String nhiemKy();

	String vaiTroBqt();

	String phamViPhuTrach();

	String chuyenMonKyThuat();

	String viTriTruc();

	String soCccd();

}
