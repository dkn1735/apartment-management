package vn.edu.hust.chungcu.nhatky.dto;

/**
 * Danh mục duy nhất các loại thao tác được ghi nhật ký (spec 0007, AC-10). Tên là mã lưu
 * trong {@code loai_thao_tac}, có tiền tố của phân hệ phát ra sự kiện; phân hệ thêm loại
 * mới bằng một giá trị ở đây.
 */
public enum LoaiThaoTac {

	AUTH_DANG_NHAP_THANH_CONG("Đăng nhập thành công"),

	AUTH_DANG_NHAP_THAT_BAI("Đăng nhập thất bại"),

	AUTH_DANG_XUAT("Đăng xuất"),

	AUTH_PHAT_HIEN_DUNG_LAI_TOKEN("Phát hiện dùng lại token cũ"),

	AUTH_DOI_MAT_KHAU("Đổi mật khẩu"), AUTH_TAO_TAI_KHOAN("Tạo tài khoản"),
	AUTH_CAP_NHAT_TAI_KHOAN("Cập nhật thông tin tài khoản"), AUTH_GAN_VAI_TRO("Gán vai trò"),
	AUTH_CAP_NHAT_VAI_TRO("Cập nhật vai trò"), AUTH_GO_VAI_TRO("Gỡ vai trò"), AUTH_KHOA_TAI_KHOAN("Khóa tài khoản"),
	AUTH_MO_KHOA_TAI_KHOAN("Mở khóa tài khoản"), AUTH_DAT_LAI_MAT_KHAU("Đặt lại mật khẩu"),
	AUTH_KHOA_TAM_DANG_NHAP("Tạm khóa đăng nhập do nhập sai nhiều lần"),
	AUTH_MO_KHOA_TAM_DANG_NHAP("Mở khóa đăng nhập"),
	AUTH_YEU_CAU_DAT_LAI_MAT_KHAU("Yêu cầu đặt lại mật khẩu qua email"),
	AUTH_DAT_LAI_MAT_KHAU_QUA_EMAIL("Đặt lại mật khẩu qua email"),

	CANHO_THEM_TOA_NHA("Thêm tòa nhà"), CANHO_THEM_TANG("Thêm tầng");

	private final String nhan;

	LoaiThaoTac(String nhan) {
		this.nhan = nhan;
	}

	public String nhan() {
		return this.nhan;
	}

}
