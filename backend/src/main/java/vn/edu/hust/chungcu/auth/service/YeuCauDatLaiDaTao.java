package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;

/**
 * Sự kiện: một yêu cầu đặt lại mật khẩu vừa được ghi (spec 0010). Chỉ
 * {@link GhiYeuCauDatLaiService} phát, luôn trong transaction; listener gửi email sau
 * commit. Mang token gốc vì đây là nơi duy nhất ngoài email cần nó.
 */
public record YeuCauDatLaiDaTao(long maYeuCau, String maNguoiDung, String email, String hoTen, String token,
		Instant hetHanLuc) {

	/** Không in token hay email ra log. */
	@Override
	public String toString() {
		return "YeuCauDatLaiDaTao[maYeuCau=" + this.maYeuCau + ", maNguoiDung=" + this.maNguoiDung + "]";
	}

}
