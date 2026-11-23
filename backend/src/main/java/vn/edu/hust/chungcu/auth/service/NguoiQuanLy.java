package vn.edu.hust.chungcu.auth.service;

import java.util.Set;
import org.springframework.security.core.Authentication;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.phanquyen.VaiTroTrongToken;

/**
 * Người gọi API quản lý tài khoản (spec 0008, Thuật ngữ): mã và vai trò lấy từ access
 * token, không đọc DB.
 */
public record NguoiQuanLy(String maNguoiDung, Set<VaiTro> vaiTro) {

	public NguoiQuanLy {
		vaiTro = Set.copyOf(vaiTro);
	}

	public static NguoiQuanLy tu(Authentication authentication) {
		return new NguoiQuanLy(authentication.getName(), VaiTroTrongToken.cua(authentication));
	}

	/** Có {@code QUAN_TRI_VIEN}: thấy mọi tài khoản; có cả BQL thì vẫn theo Admin. */
	public boolean laAdmin() {
		return this.vaiTro.contains(VaiTro.QUAN_TRI_VIEN);
	}

}
