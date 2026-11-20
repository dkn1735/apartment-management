package vn.edu.hust.chungcu.nhatky;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.TestcontainersConfiguration;

/**
 * DB tự giữ tính bất biến của {@code nhat_ky_he_thong}: trigger chặn sửa và xóa, CHECK
 * chặn mã sai dạng (spec 0007, AC-9). Mỗi kịch bản chạy trong transaction tự rollback.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles({ "dev", "test" })
@Transactional
class NhatKyBangIntegrationTest {

	static final String CHI_THEM = "Nhật ký hệ thống chỉ được thêm, không được sửa hay xóa";

	@Autowired
	JdbcClient jdbc;

	@Test
	void khongSuaDuocDong() {
		String ma = them("AUTH_DANG_XUAT", "NguoiDungHeThong", "{\"a\": 1}");
		assertThatThrownBy(() -> this.jdbc.sql("UPDATE nhat_ky_he_thong SET loai_thao_tac = 'X' WHERE ma_log = ?")
			.param(ma)
			.update()).hasMessageContaining(CHI_THEM);
	}

	@Test
	void khongXoaDuocDong() {
		String ma = them("AUTH_DANG_XUAT", "NguoiDungHeThong", null);
		assertThatThrownBy(() -> this.jdbc.sql("DELETE FROM nhat_ky_he_thong WHERE ma_log = ?").param(ma).update())
			.hasMessageContaining(CHI_THEM);
	}

	@Test
	void dongHopLeDuocChapNhanVaChiTietLaObject() {
		String ma = them("AUTH_DANG_XUAT", "NguoiDungHeThong:ND00000001-a", "{\"a\": {\"b\": [1]}}");
		assertThat(this.jdbc.sql("SELECT jsonb_typeof(chi_tiet_thay_doi) FROM nhat_ky_he_thong WHERE ma_log = ?")
			.param(ma)
			.query(String.class)
			.single()).isEqualTo("object");
		assertThat(ma).matches("^LOG[0-9]{8}$");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', nullValues = "NULL",
			value = { "dang nhap|NguoiDungHeThong|NULL", "auth_dang_xuat|NguoiDungHeThong|NULL",
					"_AUTH|NguoiDungHeThong|NULL", "AUTH-X|NguoiDungHeThong|NULL", "AUTH_X|nguoiDung|NULL",
					"AUTH_X|NguoiDung:|NULL", "AUTH_X|NguoiDung:a:b|NULL", "AUTH_X|NguoiDung:có dấu|NULL",
					"AUTH_X|Nguoi_Dung:a|NULL", "AUTH_X|NguoiDungHeThong|[1]", "AUTH_X|NguoiDungHeThong|\"chuoi\"",
					"AUTH_X|NguoiDungHeThong|null" })
	void checkTuChoiDongSaiDang(String loai, String doiTuong, String chiTiet) {
		assertThatThrownBy(() -> them(loai, doiTuong, chiTiet)).hasMessageContaining("chk_nhat_ky_he_thong_");
	}

	private String them(String loai, String doiTuong, String chiTiet) {
		return this.jdbc.sql("""
				INSERT INTO nhat_ky_he_thong (loai_thao_tac, doi_tuong_tac_dong, chi_tiet_thay_doi)
				VALUES (?, ?, CAST(? AS jsonb)) RETURNING ma_log""")
			.param(loai)
			.param(doiTuong)
			.param(chiTiet)
			.query(String.class)
			.single();
	}

}
