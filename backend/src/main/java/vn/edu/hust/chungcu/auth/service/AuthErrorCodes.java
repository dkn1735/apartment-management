package vn.edu.hust.chungcu.auth.service;

/** Mã lỗi riêng của phân hệ xác thực (spec 0004). Câu thông báo trùng với frontend. */
public final class AuthErrorCodes {

	public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";

	public static final String AUTH_INVALID_CREDENTIALS_MESSAGE = "Số điện thoại, email hoặc mật khẩu không đúng.";

	public static final String AUTH_ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED";

	public static final String AUTH_ACCOUNT_LOCKED_MESSAGE = "Tài khoản đang bị khóa hoặc đã ngừng hoạt động. Vui lòng liên hệ ban quản lý.";

	private AuthErrorCodes() {
	}

}
