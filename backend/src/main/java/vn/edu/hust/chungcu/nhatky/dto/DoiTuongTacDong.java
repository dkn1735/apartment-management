package vn.edu.hust.chungcu.nhatky.dto;

import java.util.regex.Pattern;

/**
 * Đối tượng bị tác động, lưu dạng {@code Loai:ma} hoặc {@code Loai} khi không có mã (spec
 * 0007). Sai định dạng thì ném {@link IllegalArgumentException} ngay ở nơi dựng.
 */
public record DoiTuongTacDong(String loai, String ma) {

	static final int DO_DAI_TOI_DA = 150;

	private static final String NGUOI_DUNG = "NguoiDungHeThong";

	private static final Pattern LOAI = Pattern.compile("^[A-Z][A-Za-z]*$");

	private static final Pattern MA = Pattern.compile("^[A-Za-z0-9-]+$");

	public DoiTuongTacDong {
		if (loai == null || !LOAI.matcher(loai).matches()) {
			throw new IllegalArgumentException("Loại đối tượng không hợp lệ: " + loai);
		}
		if (ma != null && !MA.matcher(ma).matches()) {
			throw new IllegalArgumentException("Mã đối tượng không hợp lệ");
		}
		if (dang(loai, ma).length() > DO_DAI_TOI_DA) {
			throw new IllegalArgumentException("Đối tượng dài quá " + DO_DAI_TOI_DA + " ký tự");
		}
	}

	/** Người dùng hệ thống; {@code maNguoiDung} null cho ra {@code NguoiDungHeThong}. */
	public static DoiTuongTacDong nguoiDung(String maNguoiDung) {
		return new DoiTuongTacDong(NGUOI_DUNG, maNguoiDung);
	}

	@Override
	public String toString() {
		return dang(this.loai, this.ma);
	}

	private static String dang(String loai, String ma) {
		return (ma != null) ? loai + ":" + ma : loai;
	}

}
