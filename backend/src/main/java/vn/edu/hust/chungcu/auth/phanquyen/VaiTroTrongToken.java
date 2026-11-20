package vn.edu.hust.chungcu.auth.phanquyen;

import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import vn.edu.hust.chungcu.auth.dto.VaiTro;

/**
 * Đọc vai trò từ authority {@code ROLE_<mã>} của access token (spec 0004, AC-7). Mã lạ bị
 * bỏ qua.
 */
public final class VaiTroTrongToken {

	private static final String PREFIX = "ROLE_";

	private VaiTroTrongToken() {
	}

	/** Tập vai trò theo thứ tự enum; không có {@code Authentication} thì rỗng. */
	public static Set<VaiTro> cua(Authentication authentication) {
		Set<VaiTro> result = EnumSet.noneOf(VaiTro.class);
		if (authentication == null) {
			return result;
		}
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String ten = authority.getAuthority();
			if (ten != null && ten.startsWith(PREFIX)) {
				for (VaiTro v : VaiTro.values()) {
					if (v.name().equals(ten.substring(PREFIX.length()))) {
						result.add(v);
					}
				}
			}
		}
		return result;
	}

}
