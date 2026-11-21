package vn.edu.hust.chungcu.auth.service;

/**
 * Mã lỗi riêng của phân hệ xác thực (spec 0004, 0005, API-AUTH-04). Câu thông báo trùng
 * với frontend.
 */
public final class AuthErrorCodes {

	public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";

	public static final String AUTH_INVALID_CREDENTIALS_MESSAGE = "Số điện thoại, email hoặc mật khẩu không đúng.";

	public static final String AUTH_ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED";

	public static final String AUTH_ACCOUNT_LOCKED_MESSAGE = "Tài khoản đang bị khóa hoặc đã ngừng hoạt động. Vui lòng liên hệ ban quản lý.";

	/** Mọi lý do làm mới thất bại dùng cùng mã và cùng câu (spec 0005, AC-3, AC-4). */
	public static final String AUTH_REFRESH_INVALID = "AUTH_REFRESH_INVALID";

	public static final String AUTH_REFRESH_INVALID_MESSAGE = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";

	/** Mật khẩu hiện tại gửi lên khi đổi mật khẩu không đúng (API-AUTH-04). */
	public static final String AUTH_OLD_PASSWORD_INCORRECT = "AUTH_OLD_PASSWORD_INCORRECT";

	/** Số điện thoại, email hoặc số CCCD đã thuộc tài khoản khác (spec 0008, AC-3). */
	public static final String AUTH_IDENTITY_TAKEN = "AUTH_IDENTITY_TAKEN";

	/** Dòng vai trò đang bị dữ liệu khác tham chiếu nên không gỡ được (AC-9). */
	public static final String AUTH_ROLE_IN_USE = "AUTH_ROLE_IN_USE";

	/** Gỡ vai trò cuối cùng của tài khoản (AC-9). */
	public static final String AUTH_LAST_ROLE = "AUTH_LAST_ROLE";

	/**
	 * Người quản lý tự khóa, tự gỡ quyền quản trị hay tự đặt lại mật khẩu (AC-9, AC-11,
	 * AC-12).
	 */
	public static final String AUTH_SELF_PROTECTED = "AUTH_SELF_PROTECTED";

	/** Thao tác khiến không còn quản trị viên đang hoạt động (AC-11). */
	public static final String AUTH_LAST_ADMIN = "AUTH_LAST_ADMIN";

	/** Đang dùng mật khẩu tạm, phải đổi trước khi gọi API khác (AC-13). */
	public static final String AUTH_PASSWORD_CHANGE_REQUIRED = "AUTH_PASSWORD_CHANGE_REQUIRED";

	public static final String AUTH_PASSWORD_CHANGE_REQUIRED_MESSAGE = "Bạn cần đổi mật khẩu tạm trước khi tiếp tục.";

	private AuthErrorCodes() {
	}

}
