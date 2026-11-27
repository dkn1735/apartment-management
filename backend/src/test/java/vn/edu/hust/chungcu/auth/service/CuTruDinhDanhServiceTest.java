package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.repository.CuTruDinhDanhQuery;
import vn.edu.hust.chungcu.auth.repository.CuTruDinhDanhQuery.TuKhoa;

/** Chuẩn hóa từ khóa tìm cư dân (spec 0014, AC-3). */
class CuTruDinhDanhServiceTest {

	@Test
	void tuKhoaRongThiKhongLoc() {
		assertThat(CuTruDinhDanhService.tuKhoa("")).isEqualTo(new TuKhoa(null, null, null));
		assertThat(CuTruDinhDanhService.tuKhoa(null)).isEqualTo(new TuKhoa(null, null, null));
	}

	@Test
	void soDienThoaiDoiDau84VaBoKyTuNgan() {
		assertThat(CuTruDinhDanhService.tuKhoa("+84 912.345-678").soDienThoai()).isEqualTo("0912345678");
		assertThat(CuTruDinhDanhService.tuKhoa("84912345678").soDienThoai()).isEqualTo("0912345678");
		assertThat(CuTruDinhDanhService.tuKhoa("0912").soDienThoai()).isEqualTo("0912");
		assertThat(CuTruDinhDanhService.tuKhoa("Nguyễn").soDienThoai()).isNull();
	}

	@Test
	void soCccdKhongDoiDau84() {
		TuKhoa tuKhoa = CuTruDinhDanhService.tuKhoa("840 123-456");
		assertThat(tuKhoa.soCccd()).isEqualTo("840123456");
		assertThat(tuKhoa.soDienThoai()).isEqualTo("00123456");
		assertThat(CuTruDinhDanhService.tuKhoa("0012.34").soCccd()).isNull();
	}

	@Test
	void mauLikeThoatKyTuDacBiet() {
		assertThat(CuTruDinhDanhService.tuKhoa("a%b_c\\").mau()).isEqualTo("%a\\%b\\_c\\\\%");
	}

	// covers: AC-2, AC-3
	@Test
	void tapRongKhongTruyVanVaChiTietCuDanGomTheoMa() {
		CuTruDinhDanhQuery query = mock(CuTruDinhDanhQuery.class);
		CuTruDinhDanhService service = new CuTruDinhDanhService(query);

		assertThat(service.chiTietCuDanTheoMa(List.of())).isEmpty();
		assertThat(service.hoTenNvbqlTheoMa(Set.of())).isEmpty();
		verifyNoInteractions(query);

		ThongTinCuDan vu = new ThongTinCuDan("CD00000001", "ND00000006", "Vũ Cư Dân", "0900000006", "vu@chungcu.test",
				"001026000006", "Hoạt động");
		when(query.theoMa(Set.of("CD00000001"))).thenReturn(List.of(vu));
		assertThat(service.chiTietCuDanTheoMa(Set.of("CD00000001"))).isEqualTo(Map.of("CD00000001", vu));
	}

}
