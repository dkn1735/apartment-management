package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository;
import vn.edu.hust.chungcu.auth.service.YeuCauDatLaiCleanupJob;

/** Job dọn xóa yêu cầu cũ hơn 7 ngày (spec 0010, AC-12). */
@ExtendWith(OutputCaptureExtension.class)
class QuenMatKhauDonDepIntegrationTest extends QuenMatKhauTestBase {

	@Autowired
	YeuCauDatLaiCleanupJob job;

	void them(Duration tuoi, String hash) {
		Instant tao = MutableClock.MAC_DINH.minus(tuoi);
		this.jdbc.update("""
				INSERT INTO yeu_cau_dat_lai_mat_khau (ma_nguoi_dung, token_hash, tao_luc, het_han_luc, huy_luc)
				VALUES (?, ?, ?, ?, ?)""", this.tk.maNguoiDung(), hash, Timestamp.from(tao),
				Timestamp.from(tao.plus(Duration.ofMinutes(30))), Timestamp.from(tao));
	}

	@Test
	void xoaDong8NgayGiuDong6Ngay() {
		them(Duration.ofDays(8), "a".repeat(64));
		them(Duration.ofDays(6), "b".repeat(64));
		this.job.chay();
		assertThat(QuenMatKhauThu.yeuCauCua(this.jdbc, this.tk.maNguoiDung())).extracting(d -> d.get("token_hash"))
			.containsExactly("b".repeat(64));
	}

	@Test
	void loiTrongJobDuocLogKhongNemRa(CapturedOutput output) {
		QuenMatKhauRepository hong = mock(QuenMatKhauRepository.class);
		when(hong.xoaTruoc(any())).thenThrow(new DataAccessResourceFailureException("mất kết nối"));
		new YeuCauDatLaiCleanupJob(hong, Clock.systemUTC()).chay();
		assertThat(output.getAll()).contains("Dọn yêu cầu đặt lại mật khẩu thất bại");
	}

}
