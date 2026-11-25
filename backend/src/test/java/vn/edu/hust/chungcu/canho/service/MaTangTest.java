package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Mã tầng do server ghép (spec 0011, AC-6). */
class MaTangTest {

	// covers: AC-6
	@ParameterizedTest
	@CsvSource({ "A,1,A-01", "A,12,A-12", "B,6,B-06", "T2,99,T2-99" })
	void ghepMaToaVaSoTangHaiChuSo(String maToaNha, int soTang, String expected) {
		assertThat(MaTang.cua(maToaNha, soTang)).isEqualTo(expected);
	}

	// covers: AC-6 (mã tòa dài nhất cho mã tầng tối đa 13 ký tự, vừa VARCHAR(20))
	@Test
	void maTangDaiNhatLa13KyTu() {
		assertThat(MaTang.cua("ABCDEFGHIJ", 99)).hasSize(13);
	}

}
