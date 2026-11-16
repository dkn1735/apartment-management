package vn.edu.hust.chungcu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ràng buộc của các bảng và cột xác thực mới (spec 0002, mục Bảng mới và bảng thay đổi):
 * {@code phien_dang_nhap}, {@code yeu_cau_dat_lai_mat_khau} và bộ đếm, trạng thái trên
 * {@code nguoi_dung_he_thong}. Cùng cấu hình context với {@link SchemaMigrationTest} để
 * dùng chung container đã seed. Mỗi kịch bản tự rollback.
 */
@Import(SeedTestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles({ "dev", "seed", "test" })
@Transactional
class BangXacThucSchemaTest {

	private static final String CHECK_VIOLATION = "23514";

	private static final String NOT_NULL_VIOLATION = "23502";

	private static final String UNIQUE_VIOLATION = "23505";

	private static final String RESTRICT_VIOLATION = "23001";

	private static final String TAO_LUC = "TIMESTAMPTZ '2026-11-16 08:00:00+07'";

	private static final String HET_HAN_LUC = "TIMESTAMPTZ '2026-11-23 08:00:00+07'";

	private static final String HASH_HOP_LE = "0123456789abcdef".repeat(4);

	@Autowired
	JdbcTemplate jdbc;

	// Value sourcing: token_hash là SHA-256 hex chữ thường, đúng 64 ký tự
	@ParameterizedTest(name = "phien_dang_nhap từ chối token_hash {0}")
	@MethodSource("tokenHashSai")
	void phienTuChoiTokenHashSaiDinhDang(String truongHop, String tokenHash) {
		String nguoiDung = taoNguoiDung("0911000001");
		assertSqlState(CHECK_VIOLATION, () -> chenPhien(nguoiDung, tokenHash, TAO_LUC, HET_HAN_LUC));
	}

	// Value sourcing: token_hash là SHA-256 hex chữ thường, đúng 64 ký tự
	@ParameterizedTest(name = "yeu_cau_dat_lai_mat_khau từ chối token_hash {0}")
	@MethodSource("tokenHashSai")
	void yeuCauDatLaiTuChoiTokenHashSaiDinhDang(String truongHop, String tokenHash) {
		String nguoiDung = taoNguoiDung("0911000002");
		assertSqlState(CHECK_VIOLATION, () -> chenYeuCauDatLai(nguoiDung, tokenHash, TAO_LUC, HET_HAN_LUC));
	}

	static Stream<Arguments> tokenHashSai() {
		return Stream.of(Arguments.of("chữ hoa", "A".repeat(64)), Arguments.of("63 ký tự", "a".repeat(63)),
				Arguments.of("ký tự ngoài hex", "g".repeat(64)), Arguments.of("rỗng", ""));
	}

	@Test
	void phienHopLeDuocChapNhan() {
		String nguoiDung = taoNguoiDung("0911000003");
		assertThat(chenPhien(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC)).isEqualTo(1);
	}

	@Test
	void yeuCauDatLaiHopLeDuocChapNhan() {
		String nguoiDung = taoNguoiDung("0911000004");
		assertThat(chenYeuCauDatLai(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC)).isEqualTo(1);
	}

	@Test
	void haiPhienKhongDungChungTokenHash() {
		String nguoiDung = taoNguoiDung("0911000005");
		chenPhien(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC);
		assertSqlState(UNIQUE_VIOLATION, () -> chenPhien(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC));
	}

	@Test
	void haiYeuCauDatLaiKhongDungChungTokenHash() {
		String nguoiDung = taoNguoiDung("0911000006");
		chenYeuCauDatLai(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC);
		assertSqlState(UNIQUE_VIOLATION, () -> chenYeuCauDatLai(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC));
	}

	@Test
	void phienHetHanTruocKhiTaoBiTuChoi() {
		String nguoiDung = taoNguoiDung("0911000007");
		assertSqlState(CHECK_VIOLATION, () -> chenPhien(nguoiDung, HASH_HOP_LE, TAO_LUC, TAO_LUC));
	}

	@Test
	void yeuCauDatLaiHetHanTruocKhiTaoBiTuChoi() {
		String nguoiDung = taoNguoiDung("0911000008");
		assertSqlState(CHECK_VIOLATION, () -> chenYeuCauDatLai(nguoiDung, HASH_HOP_LE, HET_HAN_LUC, TAO_LUC));
	}

	// Value sourcing: tao_luc do code nghiệp vụ ghi từ Clock
	@Test
	void phienKhongCoGiaTriMacDinhChoTaoLuc() {
		String nguoiDung = taoNguoiDung("0911000009");
		assertSqlState(NOT_NULL_VIOLATION, () -> this.jdbc.update("""
				INSERT INTO phien_dang_nhap (ma_nguoi_dung, ma_ho_phien, token_hash, het_han_luc)
				VALUES (?, gen_random_uuid(), ?, %s)""".formatted(HET_HAN_LUC), nguoiDung, HASH_HOP_LE));
	}

	// Value sourcing: ma_ho_phien do app sinh. Tách riêng vì lỗi đầu tiên làm hỏng
	// transaction
	@Test
	void phienKhongCoGiaTriMacDinhChoMaHoPhien() {
		String nguoiDung = taoNguoiDung("0911000018");
		assertSqlState(NOT_NULL_VIOLATION, () -> this.jdbc.update("""
				INSERT INTO phien_dang_nhap (ma_nguoi_dung, token_hash, tao_luc, het_han_luc)
				VALUES (?, ?, %s, %s)""".formatted(TAO_LUC, HET_HAN_LUC), nguoiDung, HASH_HOP_LE));
	}

	// Value sourcing: tao_luc do code nghiệp vụ ghi từ Clock
	@Test
	void yeuCauDatLaiKhongCoGiaTriMacDinhChoTaoLuc() {
		String nguoiDung = taoNguoiDung("0911000010");
		assertSqlState(NOT_NULL_VIOLATION, () -> this.jdbc.update("""
				INSERT INTO yeu_cau_dat_lai_mat_khau (ma_nguoi_dung, token_hash, het_han_luc)
				VALUES (?, ?, %s)""".formatted(HET_HAN_LUC), nguoiDung, HASH_HOP_LE));
	}

	@ParameterizedTest(name = "thu hồi phiên: {0}")
	@MethodSource("thuHoiSai")
	void thuHoiPhienPhaiCoCaThoiDiemVaLyDoHopLe(String truongHop, String thuHoiLuc, String lyDo) {
		String nguoiDung = taoNguoiDung("0911000011");
		chenPhien(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC);
		assertSqlState(CHECK_VIOLATION, () -> this.jdbc.update(
				"UPDATE phien_dang_nhap SET thu_hoi_luc = " + thuHoiLuc + ", ly_do_thu_hoi = ? WHERE token_hash = ?",
				lyDo, HASH_HOP_LE));
	}

	static Stream<Arguments> thuHoiSai() {
		return Stream.of(Arguments.of("có thời điểm, thiếu lý do", TAO_LUC, null),
				Arguments.of("có lý do, thiếu thời điểm", "NULL", "Đăng xuất"),
				Arguments.of("lý do ngoài danh sách", TAO_LUC, "Hết hứng"));
	}

	@Test
	void thuHoiPhienVoiThoiDiemVaLyDoHopLeDuocChapNhan() {
		String nguoiDung = taoNguoiDung("0911000012");
		chenPhien(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC);
		int soDong = this.jdbc.update("UPDATE phien_dang_nhap SET thu_hoi_luc = " + TAO_LUC
				+ ", ly_do_thu_hoi = 'Dùng lại token cũ' WHERE token_hash = ?", HASH_HOP_LE);
		assertThat(soDong).isEqualTo(1);
	}

	// AC-5: phiên còn lưu thì không xóa được người dùng (không cascade âm thầm)
	@Test
	void khongXoaDuocNguoiDungConPhienDangNhap() {
		String nguoiDung = taoNguoiDung("0911000013");
		chenPhien(nguoiDung, HASH_HOP_LE, TAO_LUC, HET_HAN_LUC);
		assertSqlState(RESTRICT_VIOLATION,
				() -> this.jdbc.update("DELETE FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?", nguoiDung));
	}

	// AC-3: tài khoản mới bắt đầu với bộ đếm, khóa và phiên bản ở trạng thái gốc
	@Test
	void nguoiDungMoiCoGiaTriKhoiDauChoXacThuc() {
		String nguoiDung = taoNguoiDung("0911000014");
		var dong = this.jdbc.queryForMap("""
				SELECT trang_thai_tai_khoan, so_lan_dang_nhap_sai, khoa_tam_den, phien_ban,
				       tao_luc IS NOT NULL AS co_tao_luc, cap_nhat_luc IS NOT NULL AS co_cap_nhat_luc
				FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?""", nguoiDung);
		assertThat(dong).containsEntry("trang_thai_tai_khoan", "Hoạt động")
			.containsEntry("so_lan_dang_nhap_sai", 0)
			.containsEntry("khoa_tam_den", null)
			.containsEntry("phien_ban", 0L)
			.containsEntry("co_tao_luc", true)
			.containsEntry("co_cap_nhat_luc", true);
	}

	@Test
	void soLanDangNhapSaiKhongDuocAm() {
		String nguoiDung = taoNguoiDung("0911000015");
		assertSqlState(CHECK_VIOLATION, () -> this.jdbc
			.update("UPDATE nguoi_dung_he_thong SET so_lan_dang_nhap_sai = -1 WHERE ma_nguoi_dung = ?", nguoiDung));
	}

	@ParameterizedTest(name = "trạng thái {0} hợp lệ")
	@ValueSource(strings = { "Hoạt động", "Tạm khóa", "Ngừng hoạt động" })
	void trangThaiTaiKhoanTrongDanhSachDuocChapNhan(String trangThai) {
		String nguoiDung = taoNguoiDung("0911000016");
		assertThat(this.jdbc.update("UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = ? WHERE ma_nguoi_dung = ?",
				trangThai, nguoiDung))
			.isEqualTo(1);
	}

	@Test
	void trangThaiTaiKhoanNgoaiDanhSachBiTuChoi() {
		String nguoiDung = taoNguoiDung("0911000017");
		assertSqlState(CHECK_VIOLATION,
				() -> this.jdbc.update(
						"UPDATE nguoi_dung_he_thong SET trang_thai_tai_khoan = 'Bị xóa' WHERE ma_nguoi_dung = ?",
						nguoiDung));
	}

	private String taoNguoiDung(String soDienThoai) {
		return this.jdbc.queryForObject("""
				INSERT INTO nguoi_dung_he_thong (ho_ten, so_dien_thoai, email, mat_khau_hash)
				VALUES ('Người Thử', ?, ?, '{noop}x')
				RETURNING ma_nguoi_dung""", String.class, soDienThoai, soDienThoai + "@chungcu.test");
	}

	private int chenPhien(String nguoiDung, String tokenHash, String taoLuc, String hetHanLuc) {
		return this.jdbc.update("""
				INSERT INTO phien_dang_nhap (ma_nguoi_dung, ma_ho_phien, token_hash, tao_luc, het_han_luc)
				VALUES (?, gen_random_uuid(), ?, %s, %s)""".formatted(taoLuc, hetHanLuc), nguoiDung, tokenHash);
	}

	private int chenYeuCauDatLai(String nguoiDung, String tokenHash, String taoLuc, String hetHanLuc) {
		return this.jdbc.update("""
				INSERT INTO yeu_cau_dat_lai_mat_khau (ma_nguoi_dung, token_hash, tao_luc, het_han_luc)
				VALUES (?, ?, %s, %s)""".formatted(taoLuc, hetHanLuc), nguoiDung, tokenHash);
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
