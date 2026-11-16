package vn.edu.hust.chungcu.auth.service;

/**
 * Nơi đăng nhập, ghi vào {@code phien_dang_nhap} chỉ để tham khảo (không dùng cho quyết
 * định bảo mật nào). Chuỗi rỗng thành {@code null}, chuỗi dài bị cắt cho vừa cột.
 */
public record ThongTinThietBi(String diaChiIp, String userAgent) {

	static final int IP_TOI_DA = 45;

	static final int USER_AGENT_TOI_DA = 500;

	public ThongTinThietBi {
		diaChiIp = cat(diaChiIp, IP_TOI_DA);
		userAgent = cat(userAgent, USER_AGENT_TOI_DA);
	}

	private static String cat(String value, int max) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return (value.length() > max) ? value.substring(0, max) : value;
	}

}
