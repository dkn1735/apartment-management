package vn.edu.hust.chungcu.common.email;

/**
 * Một email cần gửi: một người nhận, tiêu đề, phần chữ thường và phần HTML (spec 0010).
 * Không in người nhận hay nội dung ra log, vì nội dung có thể chứa link bí mật.
 */
public record EmailGui(String nguoiNhan, String tieuDe, String chuThuong, String html) {

	public EmailGui {
		if (nguoiNhan == null || nguoiNhan.isBlank()) {
			throw new IllegalArgumentException("Thiếu người nhận");
		}
		if (tieuDe == null || chuThuong == null || html == null) {
			throw new IllegalArgumentException("Thiếu tiêu đề hoặc nội dung");
		}
	}

	@Override
	public String toString() {
		return "EmailGui[***]";
	}

}
