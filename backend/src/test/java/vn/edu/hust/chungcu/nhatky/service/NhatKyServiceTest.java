package vn.edu.hust.chungcu.nhatky.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.repository.NhatKyGhi;
import vn.edu.hust.chungcu.nhatky.repository.NhatKyGhi.DongNhatKy;

/**
 * {@link NhatKyService} với lớp ghi, ngữ cảnh và transaction giả (spec 0007, AC-6 tới
 * AC-9).
 */
@ExtendWith(OutputCaptureExtension.class)
class NhatKyServiceTest {

	static final Instant LUC = Instant.parse("2026-11-20T03:04:05.123456789Z");

	final GiaNhatKyGhi nhatKyGhi = new GiaNhatKyGhi();

	final GiaNguCanh nguCanh = new GiaNguCanh();

	final GiaTransactionManager transactionManager = new GiaTransactionManager();

	final NhatKyService service = new NhatKyService(this.nhatKyGhi, this.nguCanh, Clock.fixed(LUC, ZoneOffset.UTC),
			JsonMapper.builder().build(), this.transactionManager);

	@Test
	void ghiNgayVoiDuCotVaGioCatToiMicroGiay() {
		this.service.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_XUAT, "ND00000001",
				DoiTuongTacDong.nguoiDung("ND00000001"), new ChiTietThu("x", List.of(new Con("y")))));

		assertThat(this.nhatKyGhi.dong).containsExactly(new DongNhatKy("ND00000001", "AUTH_DANG_XUAT",
				"NguoiDungHeThong:ND00000001", Instant.parse("2026-11-20T03:04:05.123456Z"),
				"{\"ghiChu\":\"x\",\"con\":[{\"ten\":\"y\"}]}", "198.51.100.1", "req-1"));
		assertThat(this.transactionManager.soLanCommit).isEqualTo(1);
	}

	@Test
	void chiTietNullThiCotChiTietNull() {
		this.service.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_XUAT, null, DoiTuongTacDong.nguoiDung(null), null));
		assertThat(this.nhatKyGhi.dong).singleElement().satisfies(d -> {
			assertThat(d.chiTietJson()).isNull();
			assertThat(d.doiTuongTacDong()).isEqualTo("NguoiDungHeThong");
		});
	}

	// AC-6: trong transaction thì chờ commit, ngữ cảnh lấy lúc gọi; rollback thì bỏ
	@Test
	void trongTransactionThiGhiSauCommit() {
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
			this.service.ghi(suKien(new ChiTietThu("x", List.of())));
			this.nguCanh.requestId = "req-doi-sau";
			this.nguCanh.diaChiIp = "203.0.113.99";
			assertThat(this.nhatKyGhi.dong).isEmpty();
		});
		assertThat(this.nhatKyGhi.dong).singleElement().satisfies(d -> {
			assertThat(d.requestId()).isEqualTo("req-1");
			assertThat(d.diaChiIp()).isEqualTo("198.51.100.1");
		});
	}

	@Test
	void transactionRollbackThiKhongGhi() {
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
			this.service.ghi(suKien(new ChiTietThu("x", List.of())));
			status.setRollbackOnly();
		});
		assertThat(this.nhatKyGhi.dong).isEmpty();
	}

	// AC-7: lỗi DB không ném ra ngoài, log ERROR đủ trường, không kèm nội dung chi tiết
	@Test
	void loiKhiGhiThiChiLogError(CapturedOutput output) {
		this.nhatKyGhi.loi = new DataAccessResourceFailureException("noi-dung-loi-khong-duoc-log");
		assertThatCode(() -> this.service.ghi(suKien(new ChiTietThu("bi-mat-chi-tiet", List.of()))))
			.doesNotThrowAnyException();
		assertThat(output.getAll()).contains("ERROR")
			.contains("loaiThaoTac=AUTH_DANG_XUAT")
			.contains("doiTuongTacDong=NguoiDungHeThong:ND00000001")
			.contains("maNguoiThucHien=ND00000001")
			.contains("requestId=req-1")
			.contains(DataAccessResourceFailureException.class.getName())
			.doesNotContain("bi-mat-chi-tiet")
			.doesNotContain("noi-dung-loi-khong-duoc-log");
		assertThat(this.transactionManager.soLanCommit).isZero();
	}

	// AC-7: lỗi thì không thử lại, lớp ghi chỉ được gọi một lần
	@Test
	void loiKhiGhiThiKhongThuLai() {
		this.nhatKyGhi.loi = new DataAccessResourceFailureException("x");
		this.service.ghi(suKien(null));
		assertThat(this.nhatKyGhi.soLanGoi).isEqualTo(1);
	}

	@Test
	void loiSauCommitCungKhongNem(CapturedOutput output) {
		this.nhatKyGhi.loi = new DataAccessResourceFailureException("x");
		assertThatCode(() -> new TransactionTemplate(this.transactionManager)
			.executeWithoutResult(status -> this.service.ghi(suKien(null)))).doesNotThrowAnyException();
		assertThat(output.getAll()).contains("Ghi nhật ký thất bại");
		assertThat(this.nhatKyGhi.soLanGoi).isEqualTo(1);
	}

	// AC-7: chi tiết không chuyển được sang JSON thì không ném, không ghi, log ERROR
	// không kèm nội dung lỗi
	@Test
	void chiTietKhongChuyenDuocSangJsonThiChiLogError(CapturedOutput output) {
		assertThatCode(() -> this.service.ghi(suKien(new ChiTietHong("x")))).doesNotThrowAnyException();

		assertThat(this.nhatKyGhi.soLanGoi).isZero();
		assertThat(output.getAll()).contains("ERROR")
			.contains("Ghi nhật ký thất bại")
			.contains("loaiThaoTac=AUTH_DANG_XUAT")
			.contains("requestId=req-1")
			.doesNotContain("noi-dung-chi-tiet-hong");
	}

	@Test
	void loiNguCanhCungKhongNem(CapturedOutput output) {
		this.nguCanh.loi = new IllegalStateException();
		assertThatCode(() -> this.service.ghi(suKien(null))).doesNotThrowAnyException();
		assertThat(this.nhatKyGhi.dong).isEmpty();
		assertThat(output.getAll()).contains("Ghi nhật ký thất bại").contains("IllegalStateException");
	}

	// AC-8 (b): khóa nhạy cảm ở bất kỳ độ sâu nào thì bỏ dòng, log tên khóa, không log
	// giá trị
	@Test
	void khoaNhayCamThiBoDongVaLogTenKhoa(CapturedOutput output) {
		ChiTietMo chiTiet = new ChiTietMo(Map.of("lop", List.of(Map.of("refreshToken", "gia-tri-bi-mat"))));
		this.service.ghi(suKien(chiTiet));

		assertThat(this.nhatKyGhi.dong).isEmpty();
		assertThat(output.getAll()).contains("ERROR")
			.contains("khoa=refreshToken")
			.contains("loaiThaoTac=AUTH_DANG_XUAT")
			.doesNotContain("gia-tri-bi-mat");
	}

	// AC-9: lớp ghi chỉ có một hàm thêm
	@Test
	void lopGhiChiCoHamThem() {
		List<String> ham = Arrays.stream(NhatKyGhi.class.getDeclaredMethods())
			.filter(m -> Modifier.isPublic(m.getModifiers()))
			.map(Method::getName)
			.toList();
		assertThat(ham).containsExactly("them");
	}

	private static SuKienNhatKy suKien(ChiTietNhatKy chiTiet) {
		return new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_XUAT, "ND00000001", DoiTuongTacDong.nguoiDung("ND00000001"),
				chiTiet);
	}

	record Con(String ten) {
	}

	record ChiTietThu(String ghiChu, List<Con> con) implements ChiTietNhatKy {
	}

	/** Chi tiết có cấu trúc mở, chỉ để thử kiểm khóa lúc ghi. */
	record ChiTietMo(Map<String, Object> noiDung) implements ChiTietNhatKy {
	}

	/** Chi tiết mà Jackson không đọc được: accessor ném lỗi. */
	record ChiTietHong(String giaTri) implements ChiTietNhatKy {
		@Override
		public String giaTri() {
			throw new IllegalStateException("noi-dung-chi-tiet-hong");
		}
	}

	static final class GiaNhatKyGhi extends NhatKyGhi {

		final List<DongNhatKy> dong = new ArrayList<>();

		RuntimeException loi;

		int soLanGoi;

		GiaNhatKyGhi() {
			super(null);
		}

		@Override
		public void them(DongNhatKy value) {
			this.soLanGoi++;
			if (this.loi != null) {
				throw this.loi;
			}
			this.dong.add(value);
		}

	}

	static final class GiaNguCanh implements NguCanhYeuCau {

		String requestId = "req-1";

		String diaChiIp = "198.51.100.1";

		RuntimeException loi;

		@Override
		public String requestId() {
			if (this.loi != null) {
				throw this.loi;
			}
			return this.requestId;
		}

		@Override
		public String diaChiIp() {
			return this.diaChiIp;
		}

	}

	static final class GiaTransactionManager extends AbstractPlatformTransactionManager {

		int soLanCommit;

		@Override
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			this.soLanCommit++;
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
		}

	}

}
