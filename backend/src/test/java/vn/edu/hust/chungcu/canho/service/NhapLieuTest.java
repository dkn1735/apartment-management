package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/** Chuẩn hóa chữ và đọc số tầng (spec 0011, Thuật ngữ, AC-2, AC-5). */
class NhapLieuTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	// covers: AC-1 (chuẩn hóa chữ)
	@Test
	void chuanHoaChuCatGopKhoangTrangVaDoiNfc() {
		String nfd = Normalizer.normalize("  Tòa \t\n  Hướng   Dương ", Normalizer.Form.NFD);
		String ketQua = NhapLieu.chuanHoaChu(nfd);
		assertThat(ketQua).isEqualTo("Tòa Hướng Dương");
		assertThat(Normalizer.isNormalized(ketQua, Normalizer.Form.NFC)).isTrue();
	}

	// covers: AC-2 (trường vắng hoặc null tính như rỗng)
	@Test
	void chuanHoaChuNullThanhRong() {
		assertThat(NhapLieu.chuanHoaChu(null)).isEmpty();
		assertThat(NhapLieu.chuanHoaChu(" \t\n ")).isEmpty();
	}

	// covers: AC-1 (mã tòa cắt hai đầu rồi đổi chữ hoa, không gộp khoảng trắng giữa)
	@Test
	void chuanHoaMaToaNhaChiCatHaiDauVaDoiHoa() {
		assertThat(NhapLieu.chuanHoaMaToaNha("  b2 ")).isEqualTo("B2");
		assertThat(NhapLieu.chuanHoaMaToaNha("a b")).isEqualTo("A B");
		assertThat(NhapLieu.chuanHoaMaToaNha(null)).isEmpty();
	}

	// covers: AC-2 (độ dài đếm theo code point, không theo đơn vị UTF-16)
	@Test
	void doDaiDemTheoCodePoint() {
		String toaNha = "🏢";
		assertThat(toaNha.length()).isEqualTo(2);
		assertThat(NhapLieu.doDai(toaNha.repeat(100))).isEqualTo(100);
		assertThat(NhapLieu.doDai("")).isZero();
	}

	// covers: AC-2, AC-5 (biên 1 và 99 hợp lệ)
	@ParameterizedTest
	@ValueSource(strings = { "1", "50", "99" })
	void soTangNguyenTrongKhoangThiNhan(String json) {
		List<FieldError> loi = new ArrayList<>();
		Integer soTang = NhapLieu.soTang(JSON.readTree(json), loi);
		assertThat(soTang).isEqualTo(Integer.parseInt(json));
		assertThat(loi).isEmpty();
	}

	// covers: AC-2, AC-5 (chỉ nhận số nguyên JSON từ 1 đến 99)
	@ParameterizedTest
	@ValueSource(strings = { "0", "-1", "100", "3.0", "3.5", "\"3\"", "true", "null", "[3]", "{}", "4294967297" })
	void soTangSaiThemMotLoiTruong(String json) {
		List<FieldError> loi = new ArrayList<>();
		Integer soTang = NhapLieu.soTang(JSON.readTree(json), loi);
		assertThat(soTang).isNull();
		assertThat(loi).containsExactly(new FieldError("soTang", "Số tầng phải là số nguyên từ 1 đến 99"));
	}

	// covers: AC-2 (trường vắng)
	@Test
	void soTangVangLaLoiTruong() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieu.soTang((JsonNode) null, loi)).isNull();
		assertThat(loi).extracting(FieldError::field).containsExactly("soTang");
	}

}
