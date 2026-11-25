package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/** Đọc diện tích, số căn, trạng thái (spec 0012, Thuật ngữ, AC-2, AC-8). */
class NhapLieuCanHoTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Test
	void dienTichHopLeDuocSetScale2() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuCanHo.dienTich(JSON.readTree("75.50"), loi)).isEqualTo(new BigDecimal("75.50"));
		assertThat(NhapLieuCanHo.dienTich(JSON.readTree("70.5"), loi)).isEqualTo(new BigDecimal("70.50"));
		assertThat(NhapLieuCanHo.dienTich(JSON.readTree("1000"), loi)).isEqualTo(new BigDecimal("1000.00"));
		assertThat(NhapLieuCanHo.dienTich(JSON.readTree("0.01"), loi)).isEqualTo(new BigDecimal("0.01"));
		assertThat(loi).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1", "1000.01", "75.505", "\"75\"", "null", "true", "[1]", "{}", "1e400" })
	void dienTichSai(String raw) {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuCanHo.dienTich(JSON.readTree(raw), loi)).isNull();
		assertThat(loi).extracting(FieldError::field).containsExactly("dienTich");
	}

	@ParameterizedTest
	@ValueSource(strings = { "1e2", "50,5", ".5", "+5", "-1", "75.505", "1000.01", "12345" })
	void dienTichLocSai(String raw) {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuCanHo.dienTichLoc(raw, "dienTichTu", loi)).isNull();
		assertThat(loi).extracting(FieldError::field).containsExactly("dienTichTu");
	}

	@Test
	void dienTichLocRongLaKhongLoc() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuCanHo.dienTichLoc("  ", "dienTichTu", loi)).isNull();
		assertThat(NhapLieuCanHo.dienTichLoc(null, "dienTichTu", loi)).isNull();
		assertThat(NhapLieuCanHo.dienTichLoc(" 0 ", "dienTichTu", loi)).isEqualByComparingTo("0");
		assertThat(NhapLieuCanHo.dienTichLoc("1000.00", "dienTichTu", loi)).isEqualByComparingTo("1000");
		assertThat(loi).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "100", "1.0", "\"5\"", "null", "2147483648" })
	void soCanSai(String raw) {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuCanHo.soCan(JSON.readTree(raw), loi)).isNull();
		assertThat(loi).extracting(FieldError::message).containsExactly("Số căn phải là số nguyên từ 1 đến 99");
	}

	@Test
	void trangThaiDoiVeNfcVaChiNhanChuoi() {
		List<FieldError> loi = new ArrayList<>();
		String nfd = Normalizer.normalize("Đang sửa chữa", Normalizer.Form.NFD);
		assertThat(NhapLieuCanHo.trangThai(JSON.valueToTree(nfd), loi)).isEqualTo("Đang sửa chữa");
		assertThat(NhapLieuCanHo.trangThai(JSON.readTree("5"), loi)).isNull();
		assertThat(NhapLieuCanHo.trangThai(JSON.valueToTree(""), loi)).isNull();
		assertThat(NhapLieuCanHo.trangThai(JSON.valueToTree("Sửa chữa"), loi)).isNull();
		assertThat(loi).hasSize(3);
	}

	@Test
	void maCanHoGhepTuTangVaSoCan() {
		assertThat(MaCanHo.cua("A-01", 5)).isEqualTo("A-0105");
		assertThat(MaCanHo.cua("T2-12", 99)).isEqualTo("T2-1299");
	}

}
