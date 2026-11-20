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

	AUTH_PHAT_HIEN_DUNG_LAI_TOKEN("Phát hiện dùng lại token cũ");

	private final String nhan;

	LoaiThaoTac(String nhan) {
		this.nhan = nhan;
	}

	public String nhan() {
		return this.nhan;
	}

}
