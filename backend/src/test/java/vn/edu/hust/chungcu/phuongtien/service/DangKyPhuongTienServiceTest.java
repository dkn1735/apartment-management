package vn.edu.hust.chungcu.phuongtien.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;
import vn.edu.hust.chungcu.phuongtien.dto.DangKyPhuongTienRequest;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienDong;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienGhi;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienQuery;

/** Chọn luồng BQL hay cư dân khi đăng ký phương tiện (spec 0016, AC-5, AC-7). */
class DangKyPhuongTienServiceTest {

	private static final Instant LUC = Instant.parse("2026-11-16T01:00:00Z");

	private final CuTruDinhDanhService dinhDanh = mock(CuTruDinhDanhService.class);

	private final CanHoCuTruService canHo = mock(CanHoCuTruService.class);

	private final PhuongTienQuery query = mock(PhuongTienQuery.class);

	private final PhuongTienGhi ghi = mock(PhuongTienGhi.class);

	private final HienThiPhuongTien hienThi = mock(HienThiPhuongTien.class);

	private final DangKyPhuongTienService service = new DangKyPhuongTienService(this.dinhDanh, this.canHo,
			mock(KiemChuXe.class), mock(KiemTrungBien.class), this.query, this.ghi, this.hienThi,
			mock(NhatKyService.class), Clock.fixed(LUC, ZoneId.of("Asia/Ho_Chi_Minh")),
			mock(PlatformTransactionManager.class));

	// AC-5: vừa là BQL vừa là cư dân thì đi luồng BQL, không bị trần đơn chờ
	@Test
	void bqlKiemCuDanDangKyThiHoatDongNgay() {
		when(this.dinhDanh.maNvbqlCua("ND00000001")).thenReturn(Optional.of("BQL00000001"));
		when(this.dinhDanh.maCuDanCua("ND00000001")).thenReturn(Optional.of("CD00000001"));
		when(this.canHo.maToaNhaCua("A-0101")).thenReturn(Optional.of("A"));
		when(this.canHo.khoaVaDocTrangThai("A-0101")).thenReturn(Optional.of("Đang sử dụng"));
		when(this.ghi.them(any(), any())).thenReturn("PT00000001");
		when(this.query.cua("PT00000001")).thenReturn(Optional.of(new PhuongTienDong("PT00000001", "A-0101",
				"CD00000001", "Xe đạp", null, null, "Đang hoạt động", null, "BQL00000001", null, LUC, LUC)));
		when(this.hienThi.dung(any(), any(), any(), any())).thenReturn(List.of(mock(PhuongTienResponse.class)));
		ArgumentCaptor<PhuongTienGhi.Moi> moi = ArgumentCaptor.forClass(PhuongTienGhi.Moi.class);

		this.service.dangKy(phamVi(VaiTro.BAN_QUAN_LY, VaiTro.CU_DAN), yeuCau("""
				{"maCanHo": "A-0101", "maCuDanSoHuu": "CD00000001", "loaiPhuongTien": "Xe đạp"}"""));

		verify(this.ghi).them(moi.capture(), any());
		assertThat(moi.getValue()).isEqualTo(new PhuongTienGhi.Moi("A-0101", "CD00000001", "Xe đạp", null, null,
				"Đang hoạt động", null, "BQL00000001"));
		verify(this.query, never()).demChoDuyet(any());
	}

	// AC-7: bước 403 chạy trước kiểm 400
	@Test
	void khongConHoSoNhanVienThi403TruocKhiKiemDuLieu() {
		when(this.dinhDanh.maNvbqlCua("ND00000001")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.dangKy(phamVi(VaiTro.BAN_QUAN_LY), yeuCau("{}"))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
		verifyNoInteractions(this.ghi);
	}

	private static PhamViTruyCap phamVi(VaiTro... vaiTro) {
		return new PhamViTruyCap("ND00000001", Set.of(vaiTro), List.of("A"), List.of());
	}

	private static DangKyPhuongTienRequest yeuCau(String body) {
		return JsonMapper.builder().build().readValue(body, DangKyPhuongTienRequest.class);
	}

}
