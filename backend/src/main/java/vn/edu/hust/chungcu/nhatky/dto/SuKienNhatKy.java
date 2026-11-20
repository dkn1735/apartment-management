package vn.edu.hust.chungcu.nhatky.dto;

/**
 * Một sự kiện cần ghi nhật ký (spec 0007). {@code loai} và {@code doiTuong} bắt buộc;
 * {@code maNguoiThucHien} và {@code chiTiet} có thể null.
 */
public record SuKienNhatKy(LoaiThaoTac loai, String maNguoiThucHien, DoiTuongTacDong doiTuong, ChiTietNhatKy chiTiet) {

	public SuKienNhatKy {
		if (loai == null) {
			throw new IllegalArgumentException("Thiếu loại thao tác");
		}
		if (doiTuong == null) {
			throw new IllegalArgumentException("Thiếu đối tượng tác động");
		}
	}

}
