package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.service.NguoiDungTraCuuService;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.dto.DoiTienIchRequest;
import vn.edu.hust.chungcu.canho.dto.LanDoiTienIchResponse;
import vn.edu.hust.chungcu.canho.repository.CanHoGhi;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Thứ tự kiểm và số lần tra họ tên của tạm dừng tiện ích (spec 0013, AC-4, AC-5, AC-8).
 */
class TamDungTienIchServiceTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final PhamViTruyCap KE_TOAN = new PhamViTruyCap("ND00000004", Set.of(VaiTro.KE_TOAN), List.of("A"),
			List.of());

	private final CanHoQuery query = mock(CanHoQuery.class);

	private final CanHoGhi ghi = mock(CanHoGhi.class);

	private final NhatKyService nhatKy = mock(NhatKyService.class);

	private final NguoiDungTraCuuService nguoiDung = mock(NguoiDungTraCuuService.class);

	private final TamDungTienIchService service = new TamDungTienIchService(this.query, this.ghi, this.nhatKy,
			new NguoiDoiTienIch(this.nguoiDung), Clock.fixed(Instant.parse("2026-11-26T03:00:00Z"), ZoneOffset.UTC));

	@Test
	void loiBodyTruocPhamVi() {
		assertThatThrownBy(() -> this.service.doi(KE_TOAN, "B-0101", yeuCau("\"true\"", "\" \"")))
			.isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
		verifyNoInteractions(this.query, this.ghi);
	}

	@Test
	void ngoaiPhamViThi404() {
		when(this.query.maToaNhaCua("B-0101")).thenReturn(Optional.of("B"));
		assertThatThrownBy(() -> this.service.doi(KE_TOAN, " b-0101 ", yeuCau("true", "\"x\""))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
		verifyNoInteractions(this.ghi);
	}

	@Test
	void khongDoiThi409VaKhongGhi() {
		when(this.query.maToaNhaCua("A-0101")).thenReturn(Optional.of("A"));
		when(this.ghi.khoaCoTienIch("A-0101")).thenReturn(Optional.of(true));
		assertThatThrownBy(() -> this.service.doi(KE_TOAN, "A-0101", yeuCau("true", "\"x\"")))
			.isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(ex.getErrorCode()).isEqualTo(CanHoErrorCodes.CANHO_TIEN_ICH_KHONG_DOI);
			});
		verify(this.ghi, never()).capNhatTienIch(anyString(), anyBoolean(), anyString(), any(), anyString());
		verifyNoInteractions(this.nhatKy);
	}

	@Test
	void doiThiCapNhatGhiNhatKyVaGanHoTen() {
		when(this.query.maToaNhaCua("A-0101")).thenReturn(Optional.of("A"));
		when(this.ghi.khoaCoTienIch("A-0101")).thenReturn(Optional.of(false));
		Instant luc = Instant.parse("2026-11-26T03:00:00Z");
		when(this.query.cua("A-0101")).thenReturn(Optional.of(can("A-0101", true, "ND00000004")));
		when(this.nguoiDung.hoTenTheoMa(Set.of("ND00000004"))).thenReturn(Map.of("ND00000004", "Phạm Kế Toán"));
		CanHoResponse ketQua = this.service.doi(KE_TOAN, "A-0101", yeuCau("true", "\" Nợ phí \""));
		verify(this.ghi).capNhatTienIch("A-0101", true, "Nợ phí", luc, "ND00000004");
		verify(this.nhatKy).ghi(any());
		assertThat(ketQua.lanDoiTienIch().hoTenNguoiDoi()).isEqualTo("Phạm Kế Toán");
	}

	@Test
	void ganHoTenMotTruyVanChoCaTrang() {
		NguoiDoiTienIch nguoiDoi = new NguoiDoiTienIch(this.nguoiDung);
		when(this.nguoiDung.hoTenTheoMa(any())).thenReturn(Map.of("ND1", "Một"));
		List<CanHoResponse> ketQua = nguoiDoi
			.ganHoTen(List.of(can("A-0101", true, "ND1"), can("A-0102", false, "ND2"), can("A-0103", false, null)));
		verify(this.nguoiDung, times(1)).hoTenTheoMa(Set.of("ND1", "ND2"));
		assertThat(ketQua.get(0).lanDoiTienIch().hoTenNguoiDoi()).isEqualTo("Một");
		assertThat(ketQua.get(1).lanDoiTienIch().hoTenNguoiDoi()).isNull();
		assertThat(ketQua.get(2).lanDoiTienIch()).isNull();
	}

	@Test
	void khongCoNguoiDoiThiKhongTraHoTen() {
		NguoiDoiTienIch nguoiDoi = new NguoiDoiTienIch(this.nguoiDung);
		nguoiDoi.ganHoTen(List.of(can("A-0101", false, null)));
		verifyNoInteractions(this.nguoiDung);
		assertThat(nguoiDoi.anNguoiDoi(can("A-0101", true, "ND1")).lanDoiTienIch().maNguoiDoi()).isNull();
	}

	private static DoiTienIchRequest yeuCau(String tamDung, String lyDo) {
		return new DoiTienIchRequest(JSON.readTree(tamDung), JSON.readTree(lyDo));
	}

	private static CanHoResponse can(String ma, boolean tamDung, String nguoiDoi) {
		LanDoiTienIchResponse lanDoi = (nguoiDoi != null)
				? new LanDoiTienIchResponse("x", Instant.parse("2026-11-01T02:00:00Z"), nguoiDoi, null) : null;
		return new CanHoResponse(ma, "A-01", 1, "A", "Tòa A", new BigDecimal("60.00"), "Trống", tamDung, lanDoi);
	}

}
