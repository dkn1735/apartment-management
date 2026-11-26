package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/** Đọc cờ, lý do và bộ lọc tiện ích (spec 0013, AC-2, AC-3, AC-11). */
class NhapLieuTienIchTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	// covers: AC-3
	@Test
	void coTamDungChiNhanBooleanJson() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.coTamDung(JSON.readTree("true"), loi)).isTrue();
		assertThat(NhapLieuTienIch.coTamDung(JSON.readTree("false"), loi)).isFalse();
		assertThat(loi).isEmpty();
	}

	// covers: AC-3
	@ParameterizedTest
	@ValueSource(strings = { "\"true\"", "1", "0", "null", "[]", "{}" })
	void coTamDungSai(String raw) {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.coTamDung(JSON.readTree(raw), loi)).isNull();
		assertThat(loi).containsExactly(new FieldError("biTamDungTienIch", NhapLieuTienIch.CO_SAI));
	}

	// covers: AC-3
	@Test
	void coTamDungVangLaSai() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.coTamDung(null, loi)).isNull();
		assertThat(loi).extracting(FieldError::field).containsExactly("biTamDungTienIch");
	}

	// covers: AC-2
	@Test
	void lyDoDoiVeNfcRoiCatHaiDau() {
		List<FieldError> loi = new ArrayList<>();
		String nfd = Normalizer.normalize("  Nợ phí quý 3 \n", Normalizer.Form.NFD);
		String gon = NhapLieuTienIch.lyDo(JsonNodeFactory.instance.stringNode(nfd), loi);
		assertThat(gon).isEqualTo(Normalizer.normalize("Nợ phí quý 3", Normalizer.Form.NFC));
		assertThat(Normalizer.isNormalized(gon, Normalizer.Form.NFC)).isTrue();
		assertThat(loi).isEmpty();
	}

	// covers: AC-2, AC-14 (xuống dòng giữa lý do được giữ nguyên)
	@Test
	void lyDoGiuXuongDongOGiua() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.lyDo(JsonNodeFactory.instance.stringNode("Dòng 1\nDòng 2"), loi))
			.isEqualTo("Dòng 1\nDòng 2");
	}

	// covers: AC-3
	@ParameterizedTest
	@ValueSource(strings = { "null", "\"\"", "\"   \"", "\"\\n\\t\"", "5", "true", "[\"x\"]" })
	void lyDoVangHoacKhongPhaiChuoi(String raw) {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.lyDo(JSON.readTree(raw), loi)).isNull();
		assertThat(loi).containsExactly(new FieldError("lyDo", NhapLieuTienIch.LY_DO_VANG));
	}

	// covers: AC-3 (đếm code point, sau khi cắt)
	@Test
	void lyDoToiDa500CodePoint() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.lyDo(JsonNodeFactory.instance.stringNode("😀".repeat(500)), loi)).isNotNull();
		assertThat(NhapLieuTienIch.lyDo(JsonNodeFactory.instance.stringNode("  " + "x".repeat(500) + "  "), loi))
			.hasSize(500);
		assertThat(loi).isEmpty();

		assertThat(NhapLieuTienIch.lyDo(JsonNodeFactory.instance.stringNode("😀".repeat(501)), loi)).isNull();
		assertThat(loi).containsExactly(new FieldError("lyDo", NhapLieuTienIch.LY_DO_DAI));
	}

	// covers: AC-11
	@Test
	void boLocNhanTrueFalseSauKhiCat() {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.boLoc(" true ", loi)).isTrue();
		assertThat(NhapLieuTienIch.boLoc("false", loi)).isFalse();
		assertThat(NhapLieuTienIch.boLoc(null, loi)).isNull();
		assertThat(NhapLieuTienIch.boLoc("   ", loi)).isNull();
		assertThat(loi).isEmpty();
	}

	// covers: AC-11
	@ParameterizedTest
	@ValueSource(strings = { "TRUE", "True", "1", "0", "yes", "tru" })
	void boLocSai(String raw) {
		List<FieldError> loi = new ArrayList<>();
		assertThat(NhapLieuTienIch.boLoc(raw, loi)).isNull();
		assertThat(loi).containsExactly(new FieldError("biTamDungTienIch", NhapLieuTienIch.BO_LOC_SAI));
	}

}
