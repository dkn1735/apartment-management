package vn.edu.hust.chungcu.auth.service;

/**
 * Mã lỗi riêng của phân hệ xác thực (spec 0004, 0005). Câu thông báo trùng với frontend.
 */
public final class AuthErrorCodes {

	public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";

	public static final String AUTH_INVALID_CREDENTIALS_MESSAGE = "Số điện thoại, email hoặc mật khẩu không đúng.";

	public static final String AUTH_ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED";

	public static final String AUTH_ACCOUNT_LOCKED_MESSAGE = "Tài khoản đang bị khóa hoặc đã ngừng hoạt động. Vui lòng liên hệ ban quản lý.";

	/** Mọi lý do làm mới thất bại dùng cùng mã và cùng câu (spec 0005, AC-3, AC-4). */
	public static final String AUTH_REFRESH_INVALID = "AUTH_REFRESH_INVALID";

	public static final String AUTH_REFRESH_INVALID_MESSAGE = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";

	private AuthErrorCodes() {
	}

}
