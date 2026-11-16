package vn.edu.hust.chungcu.auth.dto;

import java.util.List;

/**
 * Vai trò của một người dùng đọc từ 7 bảng vai trò: mã theo thứ tự của {@link VaiTro} và
 * chi tiết từng vai trò.
 */
public record VaiTroNguoiDung(List<VaiTro> vaiTro, ChiTietVaiTro chiTiet) {

	public static final VaiTroNguoiDung KHONG_CO = new VaiTroNguoiDung(List.of(),
			new ChiTietVaiTro(null, null, null, null, null, null, null));

	public boolean co(VaiTro value) {
		return this.vaiTro.contains(value);
	}

	/** Có ít nhất một vai trò khác {@code CU_DAN}: là nhân viên hoặc quản trị. */
	public boolean coVaiTroKhacCuDan() {
		return this.vaiTro.stream().anyMatch(v -> v != VaiTro.CU_DAN);
	}

}
