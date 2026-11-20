package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Job không ném lỗi ra ngoài và log ERROR khi dọn thất bại (spec 0005, AC-15). */
@ExtendWith(OutputCaptureExtension.class)
class PhienDangNhapCleanupJobTest {

	@Test
	void loiKhiDonThiLogErrorVaKhongNemRaNgoai(CapturedOutput output) {
		PhienDangNhapService service = mock(PhienDangNhapService.class);
		when(service.donDep()).thenThrow(new IllegalStateException("DB tạm thời không kết nối được"));
		PhienDangNhapCleanupJob job = new PhienDangNhapCleanupJob(service);

		assertThatCode(job::chay).doesNotThrowAnyException();
		assertThat(output.getAll()).contains("ERROR")
			.contains("Dọn phiên thất bại: IllegalStateException: DB tạm thời không kết nối được");
	}

}
