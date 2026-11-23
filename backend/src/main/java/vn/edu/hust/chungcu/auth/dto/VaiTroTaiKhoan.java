package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Một vai trò của tài khoản ở trang chi tiết (spec 0008, AC-6): record phẳng, trường
 * không thuộc vai trò là {@code null}. {@code maVaiTro} là khóa chính của bảng con.
 */
public record VaiTroTaiKhoan(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) VaiTro vaiTro,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "BQL00000001") String maVaiTro, String chucVu,
		String boPhan, String nhiemKy, String vaiTroBqt, String phamViPhuTrach, String chuyenMonKyThuat,
		String viTriTruc, String soCccd) {

	@Override
	public String toString() {
		return "VaiTroTaiKhoan[vaiTro=" + this.vaiTro + ", maVaiTro=" + this.maVaiTro + ", ***]";
	}

}
