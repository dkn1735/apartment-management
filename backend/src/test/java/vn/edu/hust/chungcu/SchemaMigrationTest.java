package vn.edu.hust.chungcu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm tra schema do Flyway tạo và dữ liệu mẫu (spec 0002). Mỗi kịch bản ghi dữ liệu chạy
 * trong {@code @Transactional} để tự rollback; lỗi được kiểm tra theo SQLSTATE.
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-not-for-real-use")
@ActiveProfiles({ "dev", "seed" })
class SchemaMigrationTest {

	private static final String CHECK_VIOLATION = "23514";

	/** ON DELETE RESTRICT báo restrict_violation (23001); 23503 là của NO ACTION. */
	private static final String RESTRICT_VIOLATION = "23001";

	private static final String SEQUENCE_LIMIT_EXCEEDED = "2200H";

	private static final String HASH_HOP_LE = "{noop}x";

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	DataSource dataSource;

	// AC-4
	@Test
	@Transactional
	void chenKhongGhiMaThiDbTuSinhMa() {
		String ma = this.jdbc.queryForObject("""
				INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash)
				VALUES ('Người Thử', '0911111111', 'thu@chungcu.test', ?)
				RETURNING ma_nguoi_dung""", String.class, HASH_HOP_LE);
		assertThat(ma).matches("^ND[0-9]{8}$");
	}

	// AC-4
	@Test
	@Transactional
	void sequenceVuotMaxValueThiBaoLoi() {
		this.jdbc.execute(
				"CREATE TEMP SEQUENCE seq_thu_tran AS bigint MINVALUE 1 MAXVALUE 99999999 NO CYCLE START 99999999");
		assertThat(this.jdbc.queryForObject("SELECT nextval('seq_thu_tran')", Long.class)).isEqualTo(99999999L);
		assertSqlState(SEQUENCE_LIMIT_EXCEEDED,
				() -> this.jdbc.queryForObject("SELECT nextval('seq_thu_tran')", Long.class));
	}

	// AC-4
	@Test
	void moiSequenceMaTuSinhDeuChanO99999999() {
		List<Long> maxValues = this.jdbc.queryForList(
				"SELECT max_value FROM pg_sequences WHERE schemaname = 'public' AND sequencename LIKE 'seq\\_%'",
				Long.class);
		assertThat(maxValues).isNotEmpty().allMatch((max) -> max == 99999999L);
	}

	// AC-5
	@ParameterizedTest(name = "{0}")
	@MethodSource("duLieuSai")
	@Transactional
	void dbTuChoiDuLieuSaiDinhDang(String truongHop, String sql) {
		assertSqlState(CHECK_VIOLATION, () -> this.jdbc.update(sql));
	}

	static Stream<Arguments> duLieuSai() {
		String nguoiDung = "INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash) VALUES ";
		return Stream.of(Arguments.of("email có chữ hoa", nguoiDung + "('A', '0911111112', 'An@x.vn', '{noop}x')"),
				Arguments.of("email có khoảng trắng", nguoiDung + "('A', '0911111113', ' an@x.vn', '{noop}x')"),
				Arguments.of("số điện thoại +84", nguoiDung + "('A', '+84900000009', 'b@x.vn', '{noop}x')"),
				Arguments.of("hash không tiền tố", nguoiDung + "('A', '0911111114', 'c@x.vn', 'abc')"),
				Arguments.of("CCCD 9 chữ số",
						"""
								INSERT INTO cu_dan (ma_nguoi_dung, so_cccd)
								SELECT ma_nguoi_dung, '001026000' FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000001'"""));
	}

	// AC-5
	@Test
	@Transactional
	void khongXoaDuocNguoiDungConVaiTro() {
		assertSqlState(RESTRICT_VIOLATION,
				() -> this.jdbc.update("DELETE FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000007'"));
	}

	// AC-6
	@Test
	void seedCoTamTaiKhoanPhuDuVaiTro() {
		assertThat(count("SELECT count(*) FROM nguoi_dung_he_thong WHERE so_dien_thoai LIKE '09000000%'")).isEqualTo(8);
		for (String bangVaiTro : List.of("quan_tri_vien", "nhan_vien_bql", "nhan_vien_ky_thuat", "ke_toan",
				"nhan_vien_bao_ve", "thanh_vien_ban_quan_tri", "cu_dan")) {
			assertThat(count("SELECT count(*) FROM " + bangVaiTro)).as(bangVaiTro).isPositive();
		}
		assertThat(count("""
				SELECT count(*) FROM nguoi_dung_he_thong n
				JOIN cu_dan c USING (ma_nguoi_dung)
				JOIN thanh_vien_ban_quan_tri t USING (ma_nguoi_dung)
				WHERE n.so_dien_thoai = '0900000006'""")).isEqualTo(1);
		assertThat(this.jdbc.queryForObject(
				"SELECT trang_thai_tai_khoan FROM nguoi_dung_he_thong WHERE so_dien_thoai = '0900000008'",
				String.class))
			.isEqualTo("Tạm khóa");
	}

	// AC-6
	@Test
	void matKhauDevKhopMoiTaiKhoanSeed() {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		List<String> hashes = this.jdbc.queryForList(
				"SELECT mat_khau_hash FROM nguoi_dung_he_thong WHERE so_dien_thoai LIKE '09000000%'", String.class);
		assertThat(hashes).hasSize(8).allMatch((hash) -> encoder.matches("ChungCu@2026", hash));
	}

	// AC-6
	@Test
	void seedCoToaNhaVaHoSoCuTruChoMoiCuDan() {
		assertThat(count("SELECT count(*) FROM toa_nha")).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM tang WHERE ma_toa_nha = 'A'")).isEqualTo(3);
		assertThat(count("SELECT count(*) FROM can_ho")).isEqualTo(3);
		assertThat(count("SELECT count(*) FROM ho_so_cu_tru WHERE trang_thai_ho_so = 'Hợp lệ'")).isEqualTo(3);
		assertThat(count("""
				SELECT count(*) FROM cu_dan c
				WHERE NOT EXISTS (
					SELECT 1 FROM ho_so_cu_tru h WHERE h.ma_cu_dan = c.ma_cu_dan AND h.trang_thai_ho_so = 'Hợp lệ')"""))
			.isZero();
	}

	// AC-8
	@Test
	@Transactional
	void chayLaiSeedKhongDoiSoDong() throws SQLException {
		Map<String, Integer> truoc = soDongMoiBang();
		Connection connection = DataSourceUtils.getConnection(this.dataSource);
		try {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(
					new ClassPathResource("db/seed/R__seed_01_xac_thuc.sql"), StandardCharsets.UTF_8));
		}
		finally {
			DataSourceUtils.releaseConnection(connection, this.dataSource);
		}
		assertThat(soDongMoiBang()).isEqualTo(truoc);
	}

	// AC-7
	@Test
	void boProfileSeedTrenDatabaseDaSeedVanMigrateDuoc() {
		Flyway flyway = Flyway.configure()
			.dataSource(this.dataSource)
			.locations("classpath:db/migration")
			.placeholderReplacement(false)
			.ignoreMigrationPatterns("*:future", "repeatable:missing")
			.load();
		flyway.validate();
		flyway.migrate();
	}

	private Map<String, Integer> soDongMoiBang() {
		Map<String, Integer> soDong = new TreeMap<>();
		for (String bang : tenCacBang()) {
			soDong.put(bang, count("SELECT count(*) FROM " + bang));
		}
		return soDong;
	}

	private List<String> tenCacBang() {
		return this.jdbc.queryForList(
				"""
						SELECT table_name FROM information_schema.tables
						WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'""",
				String.class);
	}

	private int count(String sql) {
		Integer value = this.jdbc.queryForObject(sql, Integer.class);
		return (value != null) ? value : 0;
	}

	private static void assertSqlState(String expected, Runnable action) {
		assertThatThrownBy(action::run).satisfies((ex) -> assertThat(sqlState(ex)).isEqualTo(expected));
	}

	private static String sqlState(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sql) {
				return sql.getSQLState();
			}
		}
		return null;
	}

}
