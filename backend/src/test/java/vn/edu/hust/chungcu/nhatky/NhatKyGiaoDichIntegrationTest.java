package vn.edu.hust.chungcu.nhatky;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.TestcontainersConfiguration;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * {@code ghi} không tham gia transaction của nơi gọi: không có transaction thì ghi ngay,
 * có thì ghi sau commit, rollback thì không ghi; giờ lấy lúc gọi (spec 0007, AC-6).
 */
@Import({ TestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@ActiveProfiles({ "dev", "test" })
class NhatKyGiaoDichIntegrationTest {

	@Autowired
	NhatKyService nhatKyService;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	NhatKyDb nhatKy;

	TransactionTemplate tx;

	DoiTuongTacDong doiTuong;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.nhatKy = new NhatKyDb(this.jdbc);
		this.tx = new TransactionTemplate(this.transactionManager);
		this.doiTuong = new DoiTuongTacDong("KiemThu", UUID.randomUUID().toString());
	}

	@Test
	void khongCoTransactionThiGhiNgay() {
		this.nhatKyService.ghi(suKien());
		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong(this.doiTuong.toString());
		assertThat(dong).hasSize(1);
		assertThat(dong.getFirst().get("ma_nguoi_dung")).isNull();
		assertThat(dong.getFirst().get("request_id")).isNull();
		assertThat(NhatKyDb.chiTiet(dong.getFirst())).isEqualTo(NhatKyDb.json("{\"ghiChu\":\"thử\"}"));
	}

	@Test
	void coTransactionThiGhiSauCommitVoiGioLucGoi() {
		this.tx.executeWithoutResult(status -> {
			this.nhatKyService.ghi(suKien());
			this.clock.advance(Duration.ofMinutes(5));
			assertThat(this.nhatKy.theoDoiTuong(this.doiTuong.toString())).isEmpty();
		});
		List<Map<String, Object>> dong = this.nhatKy.theoDoiTuong(this.doiTuong.toString());
		assertThat(dong).hasSize(1);
		assertThat(NhatKyDb.instant(dong.getFirst().get("thoi_gian_ghi"))).isEqualTo(MutableClock.MAC_DINH);
	}

	@Test
	void transactionRollbackThiKhongGhi() {
		this.tx.executeWithoutResult(status -> {
			this.nhatKyService.ghi(suKien());
			status.setRollbackOnly();
		});
		assertThat(this.nhatKy.theoDoiTuong(this.doiTuong.toString())).isEmpty();
	}

	private SuKienNhatKy suKien() {
		return new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_XUAT, null, this.doiTuong, new ChiTietKiemThu("thử"));
	}

	record ChiTietKiemThu(String ghiChu) implements ChiTietNhatKy {
	}

}
