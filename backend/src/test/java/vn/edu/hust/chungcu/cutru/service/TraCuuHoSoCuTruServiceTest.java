package vn.edu.hust.chungcu.cutru.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.CuDanLuaChonResponse;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruCanHoResponse;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;

/**
 * Kiểm thử đơn vị cho tra cứu hồ sơ cư trú theo phạm vi và tìm cư dân (spec 0014, AC-1,
 * AC-2, AC-3).
 */
class TraCuuHoSoCuTruServiceTest {

	private static final Clock DONG_HO = Clock.fixed(Instant.parse("2026-11-15T17:30:00Z"),
			ZoneId.of("Asia/Ho_Chi_Minh"));

	// covers: AC-2, AC-13
	@Test
	void bqlXemCanNgoaiPhamViToaNhaThi404() {
		CanHoCuTruService canHo = mock(CanHoCuTruService.class);
		when(canHo.maToaNhaCua("B-0101")).thenReturn(Optional.of("B"));
		HoSoCuTruQuery query = mock(HoSoCuTruQuery.class);
		TraCuuHoSoCuTruService service = new TraCuuHoSoCuTruService(canHo, query, mock(CuTruDinhDanhService.class),
				new DungHoSoCuTru(mock(CuTruDinhDanhService.class)), DONG_HO);
		PhamViTruyCap bqlToaA = new PhamViTruyCap("ND00000002", Set.of(VaiTro.BAN_QUAN_LY), List.of("A"), List.of());

		assertThatThrownBy(() -> service.xem(" b-0101 ", bqlToaA)).isInstanceOfSatisfying(ApiException.class, ex -> {
			assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
		});
		verifyNoInteractions(query);
	}

	// covers: AC-2
	@Test
	void bqlToaAKiemCuDanCanToaBThiChiThayQuyenCuDanKhongLoPii() {
		CanHoCuTruService canHo = mock(CanHoCuTruService.class);
		when(canHo.maToaNhaCua("B-0101")).thenReturn(Optional.of("B"));
		HoSoCuTruQuery query = mock(HoSoCuTruQuery.class);
		CuTruDinhDanhService dinhDanh = mock(CuTruDinhDanhService.class);

		HoSoCuTruDong dangO = dong("HS00000001", "CD00000001", "2026-11-16", null, "Hợp lệ");
		HoSoCuTruDong sapCuaToi = dong("HS00000002", "CD00000001", "2026-12-01", "2027-01-01", "Hợp lệ");
		HoSoCuTruDong sapNguoiKhac = dong("HS00000003", "CD00000002", "2026-12-01", "2027-01-01", "Hợp lệ");
		when(query.theoCanHo("B-0101")).thenReturn(List.of(dangO, sapCuaToi, sapNguoiKhac));
		when(dinhDanh.maCuDanCua("ND00000002")).thenReturn(Optional.of("CD00000001"));
		when(dinhDanh.chiTietCuDanTheoMa(Set.of("CD00000001")))
			.thenReturn(Map.of("CD00000001", new ThongTinCuDan("CD00000001", "ND00000002", "Trần Kiêm Nhiệm",
					"0900000002", "bql@chungcu.test", "001026000002", "Hoạt động")));

		TraCuuHoSoCuTruService service = new TraCuuHoSoCuTruService(canHo, query, dinhDanh, new DungHoSoCuTru(dinhDanh),
				DONG_HO);
		PhamViTruyCap phamVi = new PhamViTruyCap("ND00000002", Set.of(VaiTro.BAN_QUAN_LY, VaiTro.CU_DAN), List.of("A"),
				List.of("B-0101"));

		HoSoCuTruCanHoResponse kq = service.xem("B-0101", phamVi);
		assertThat(kq.dangCuTru()).extracting(HoSoCuTruResponse::maHoSo).containsExactly("HS00000001");
		assertThat(kq.sapBatDau()).extracting(HoSoCuTruResponse::maHoSo).containsExactly("HS00000002");
		assertThat(kq.lichSuCuTru()).isEmpty();
		HoSoCuTruResponse dongTraVe = kq.dangCuTru().get(0);
		assertThat(dongTraVe.hoTen()).isEqualTo("Trần Kiêm Nhiệm");
		assertThat(dongTraVe.soCccd()).isNull();
		assertThat(dongTraVe.soDienThoai()).isNull();
		assertThat(dongTraVe.email()).isNull();
		assertThat(dongTraVe.maNguoiDung()).isNull();
		assertThat(dongTraVe.maNvbqlXacMinh()).isNull();
		assertThat(dongTraVe.hoTenNguoiXacMinh()).isNull();
		verify(dinhDanh, never()).hoTenNvbqlTheoMa(any());
	}

	// covers: AC-2
	@Test
	void bqlCungToaKiemCuDanThiDuocUuTienQuyenBqlThayDuPii() {
		CanHoCuTruService canHo = mock(CanHoCuTruService.class);
		when(canHo.maToaNhaCua("A-0101")).thenReturn(Optional.of("A"));
		HoSoCuTruQuery query = mock(HoSoCuTruQuery.class);
		CuTruDinhDanhService dinhDanh = mock(CuTruDinhDanhService.class);

		HoSoCuTruDong lichSuKhac = dong("HS00000005", "CD00000009", "2025-01-01", "2025-12-31", "Hết hiệu lực");
		when(query.theoCanHo("A-0101")).thenReturn(List.of(lichSuKhac));
		when(dinhDanh.chiTietCuDanTheoMa(Set.of("CD00000009")))
			.thenReturn(Map.of("CD00000009", new ThongTinCuDan("CD00000009", "ND00000009", "Bùi Chủ Hộ", "0900000009",
					"bui@chungcu.test", "001026000009", "Hoạt động")));
		when(dinhDanh.hoTenNvbqlTheoMa(Set.of("BQL00000001"))).thenReturn(Map.of("BQL00000001", "Trần Ban Quản Lý"));

		TraCuuHoSoCuTruService service = new TraCuuHoSoCuTruService(canHo, query, dinhDanh, new DungHoSoCuTru(dinhDanh),
				DONG_HO);
		PhamViTruyCap phamVi = new PhamViTruyCap("ND00000002", Set.of(VaiTro.BAN_QUAN_LY, VaiTro.CU_DAN), List.of("A"),
				List.of("A-0101"));

		HoSoCuTruCanHoResponse kq = service.xem("A-0101", phamVi);
		assertThat(kq.lichSuCuTru()).hasSize(1);
		assertThat(kq.lichSuCuTru().get(0).soCccd()).isEqualTo("001026000009");
		assertThat(kq.lichSuCuTru().get(0).hoTenNguoiXacMinh()).isEqualTo("Trần Ban Quản Lý");
	}

	// covers: AC-3
	@Test
	void timCuDanChuanHoaNfcTruocKhiDemDoDaiVaGioiHan20() {
		CuTruDinhDanhService dinhDanh = mock(CuTruDinhDanhService.class);
		when(dinhDanh.timCuDanLuaChon(anyString(), anyInt())).thenReturn(List.of(new ThongTinCuDan("CD00000001",
				"ND00000006", "Vũ Cư Dân", "0900000006", "vu@chungcu.test", "001026000006", "Hoạt động")));
		TimCuDanService service = new TimCuDanService(dinhDanh);

		String nfd100KyTu = " " + "a\u0323".repeat(100) + " ";
		List<CuDanLuaChonResponse> kq = service.tim(nfd100KyTu);
		assertThat(kq).extracting(CuDanLuaChonResponse::maCuDan).containsExactly("CD00000001");
		verify(dinhDanh).timCuDanLuaChon("ạ".repeat(100), 20);

		CuTruDinhDanhService khongGoi = mock(CuTruDinhDanhService.class);
		TimCuDanService chanDai = new TimCuDanService(khongGoi);
		assertThatThrownBy(() -> chanDai.tim("ạ".repeat(101))).isInstanceOfSatisfying(ApiException.class, ex -> {
			assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(ex.getErrors()).extracting(FieldError::field, FieldError::message)
				.containsExactly(org.assertj.core.groups.Tuple.tuple("q", "Từ khóa tối đa 100 ký tự"));
		});
		verifyNoInteractions(khongGoi);
	}

	private static HoSoCuTruDong dong(String maHoSo, String maCuDan, String batDau, String ketThuc, String trangThai) {
		return new HoSoCuTruDong(maHoSo, "B-0101", maCuDan, "BQL00000001", "Chủ căn hộ", "Tạm trú",
				LocalDate.parse(batDau), (ketThuc != null) ? LocalDate.parse(ketThuc) : null, trangThai, Instant.EPOCH,
				Instant.EPOCH, null, null, null);
	}

}
