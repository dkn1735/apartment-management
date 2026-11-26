package vn.edu.hust.chungcu.cutru.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.auth.service.TrangThaiTaiKhoan;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.cutru.dto.GuiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Kiểm thử đơn vị cho {@link GuiDeNghiCuTruService} với phụ thuộc giả: thứ tự kiểm lỗi,
 * nguồn giá trị và nhật ký không lộ PII (spec 0015, AC-1, AC-3, AC-13).
 */
class GuiDeNghiCuTruServiceTest {

	/** 17:30 UTC ngày 15/11 đã là 00:30 ngày 16/11 giờ Việt Nam. */
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T17:30:00Z"),
			ZoneId.of("Asia/Ho_Chi_Minh"));

	private static final PhamViTruyCap CU_DAN = new PhamViTruyCap("ND00000006", Set.of(VaiTro.CU_DAN), List.of(),
			List.of("A-0101"));

	private static final String NGUOI_GUI = "CD00000001";

	private static final String NGUOI_DUOC_DE_NGHI = "CD00000007";

	private CuTruDinhDanhService dinhDanh;

	private CanHoCuTruService canHo;

	private HoSoCuTruQuery query;

	private HoSoCuTruGhi ghi;

	private DungHoSoCuTru dungHoSo;

	private NhatKyService nhatKy;

	private GuiDeNghiCuTruService service;

	@BeforeEach
	void setUp() {
		this.dinhDanh = mock(CuTruDinhDanhService.class);
		this.canHo = mock(CanHoCuTruService.class);
		this.query = mock(HoSoCuTruQuery.class);
		this.ghi = mock(HoSoCuTruGhi.class);
		this.dungHoSo = mock(DungHoSoCuTru.class);
		this.nhatKy = mock(NhatKyService.class);
		this.service = new GuiDeNghiCuTruService(this.dinhDanh, this.canHo, this.query, this.ghi, this.dungHoSo,
				this.nhatKy, CLOCK);
		when(this.dinhDanh.maCuDanCua("ND00000006")).thenReturn(Optional.of(NGUOI_GUI));
		when(this.canHo.khoaVaDocTrangThai("A-0101")).thenReturn(Optional.of("Trống"));
		when(this.dinhDanh.timTheoCccdVaSdt("001026000007", "0900000007"))
			.thenReturn(Optional.of(cuDan(NGUOI_DUOC_DE_NGHI, TrangThaiTaiKhoan.HOAT_DONG)));
	}

	// covers: AC-1 (người gọi đã bị gỡ dòng cư dân thì 403 trước cả kiểm 400)
	@Test
	void khongConDongCuDanThi403TruocKiemDuLieu() {
		when(this.dinhDanh.maCuDanCua("ND00000006")).thenReturn(Optional.empty());

		ApiException ex = loi(new GuiDeNghiCuTruRequest(null, null, null, null, null, null, null, null, null));

		assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
		verifyNoInteractions(this.canHo, this.ghi, this.nhatKy);
	}

	// covers: AC-2 (hôm nay tính theo Asia/Ho_Chi_Minh, không theo UTC)
	@Test
	void homNayTheoGioVietNam() {
		ApiException homQua = loi(nguoiKhac("2026-11-15", null));
		assertThat(homQua.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(homQua.getErrors()).extracting(e -> e.field()).containsExactly("ngayBatDau");

		PhamViTruyCap canKhac = new PhamViTruyCap("ND00000006", Set.of(VaiTro.CU_DAN), List.of(), List.of("A-0202"));
		ApiException homNay = catchThrowableOfType(ApiException.class,
				() -> this.service.gui(canKhac, nguoiKhac("2026-11-16", null)));
		assertThat(homNay.getStatus()).as("ngày 16/11 qua được kiểm 400, dừng ở 404 phạm vi")
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	// covers: AC-3 (căn ngoài phạm vi thì 404 và không khóa căn, không dò định danh)
	@Test
	void canNgoaiPhamViThi404KhongKhoaCan() {
		PhamViTruyCap canKhac = new PhamViTruyCap("ND00000006", Set.of(VaiTro.CU_DAN), List.of(), List.of("A-0202"));

		ApiException ex = catchThrowableOfType(ApiException.class,
				() -> this.service.gui(canKhac, nguoiKhac("2026-11-16", null)));

		assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
		verifyNoInteractions(this.canHo, this.ghi, this.nhatKy);
		verify(this.dinhDanh, never()).timTheoCccdVaSdt(anyString(), anyString());
	}

	// covers: AC-1, AC-13 (ghi đúng người gửi và người được đề nghị, căn không đổi trạng
	// thái, nhật ký chỉ có cờ coGhiChu)
	@Test
	void guiChoNguoiKhacGhiDungNguonVaNhatKyKhongLoPii() {
		when(this.query.demDeNghiChoXacMinh("A-0101")).thenReturn(9L);
		when(this.ghi.themDeNghi(eq("A-0101"), eq(NGUOI_DUOC_DE_NGHI), eq(NGUOI_GUI), any(), any(), any()))
			.thenReturn("HS00000099");
		HoSoCuTruDong dong = new HoSoCuTruDong("HS00000099", "A-0101", NGUOI_DUOC_DE_NGHI, null, "Khách thuê",
				"Tạm trú", LocalDate.of(2026, 11, 16), LocalDate.of(2027, 5, 16), "Chờ xác minh",
				Instant.parse("2026-11-15T17:30:00Z"), Instant.parse("2026-11-15T17:30:00Z"), NGUOI_GUI,
				"Hợp đồng thuê bí mật", null);
		when(this.query.cua("HS00000099")).thenReturn(Optional.of(dong));
		when(this.dungHoSo.dung(List.of(dong), DungHoSoCuTru.CheDo.CU_DAN_DE_NGHI, NGUOI_GUI))
			.thenReturn(Arrays.asList((HoSoCuTruResponse) null));

		this.service.gui(CU_DAN, nguoiKhac("2026-11-16", "2027-05-16"));

		ArgumentCaptor<HoSoCuTruGhi.GiaTri> giaTri = ArgumentCaptor.forClass(HoSoCuTruGhi.GiaTri.class);
		verify(this.ghi).themDeNghi(eq("A-0101"), eq(NGUOI_DUOC_DE_NGHI), eq(NGUOI_GUI), giaTri.capture(),
				eq("Hợp đồng thuê bí mật"), eq(Instant.parse("2026-11-15T17:30:00Z")));
		assertThat(giaTri.getValue().trangThaiHoSo()).isEqualTo("Chờ xác minh");
		verify(this.canHo, never()).capNhatTrangThaiSuDung(anyString(), anyString());
		verify(this.dungHoSo).dung(List.of(dong), DungHoSoCuTru.CheDo.CU_DAN_DE_NGHI, NGUOI_GUI);

		ArgumentCaptor<SuKienNhatKy> suKien = ArgumentCaptor.forClass(SuKienNhatKy.class);
		verify(this.nhatKy).ghi(suKien.capture());
		assertThat(suKien.getValue().loai()).isEqualTo(LoaiThaoTac.CUTRU_GUI_DE_NGHI);
		assertThat(suKien.getValue().maNguoiThucHien()).isEqualTo("ND00000006");
		assertThat(suKien.getValue().doiTuong()).isEqualTo(DoiTuongTacDong.hoSoCuTru("HS00000099"));
		assertThat(suKien.getValue().chiTiet()).isInstanceOfSatisfying(ChiTietCuTru.GuiDeNghi.class, ct -> {
			assertThat(ct.coGhiChu()).isTrue();
			assertThat(ct.maCuDan()).isEqualTo(NGUOI_DUOC_DE_NGHI);
			assertThat(ct.maCuDanDeNghi()).isEqualTo(NGUOI_GUI);
			assertThat(ct.toString()).doesNotContain("001026000007", "0900000007", "bí mật");
		});
	}

	// covers: AC-3 (người khớp định danh nhưng đã ngừng hoạt động trả chung mã không
	// khớp)
	@Test
	void nguoiDuocDeNghiNgungHoatDongTraChungMaKhongKhop() {
		when(this.dinhDanh.timTheoCccdVaSdt("001026000007", "0900000007"))
			.thenReturn(Optional.of(cuDan(NGUOI_DUOC_DE_NGHI, TrangThaiTaiKhoan.NGUNG_HOAT_DONG)));

		ApiException ex = loi(nguoiKhac("2026-11-16", null));

		assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(ex.getErrorCode()).isEqualTo("CUTRU_KHONG_KHOP_CU_DAN");
		verifyNoInteractions(this.ghi, this.nhatKy);
	}

	// covers: AC-3 (đề nghị cho chính mình khi tài khoản đã ngừng hoạt động)
	@Test
	void chinhToiNgungHoatDongThi422() {
		when(this.dinhDanh.chiTietCuDanTheoMa(List.of(NGUOI_GUI)))
			.thenReturn(Map.of(NGUOI_GUI, cuDan(NGUOI_GUI, TrangThaiTaiKhoan.NGUNG_HOAT_DONG)));

		ApiException ex = loi(NhapLieuDeNghiTest.request("A-0101", true, null, null, "Khách thuê", "Thường trú",
				"2026-11-16", null, null));

		assertThat(ex.getErrorCode()).isEqualTo("CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG");
		verify(this.dinhDanh, never()).timTheoCccdVaSdt(anyString(), anyString());
		verifyNoInteractions(this.ghi, this.nhatKy);
	}

	// covers: AC-3 (đề nghị trùng kiểm trước trần 10 đề nghị chờ)
	@Test
	void deNghiTrungKiemTruocTranDeNghi() {
		when(this.query.coDeNghiChoXacMinh("A-0101", NGUOI_DUOC_DE_NGHI)).thenReturn(true);
		when(this.query.demDeNghiChoXacMinh("A-0101")).thenReturn(10L);

		ApiException ex = loi(nguoiKhac("2026-11-16", null));

		assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(ex.getErrorCode()).isEqualTo("CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH");
		verify(this.query, never()).demDeNghiChoXacMinh(anyString());
	}

	// covers: AC-3 (căn đã có đủ 10 đề nghị chờ xác minh)
	@Test
	void tranMuoiDeNghiChoXacMinh() {
		when(this.query.demDeNghiChoXacMinh("A-0101")).thenReturn(10L);

		ApiException ex = loi(nguoiKhac("2026-11-16", null));

		assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(ex.getErrorCode()).isEqualTo("CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH");
		verifyNoInteractions(this.ghi, this.nhatKy);
	}

	// covers: AC-3 (giao ngày với hồ sơ hợp lệ của cùng người)
	@Test
	void chongNgayVoiHoSoHopLeThi409() {
		when(this.query.coChongNgayCuDan("A-0101", NGUOI_DUOC_DE_NGHI, LocalDate.of(2026, 11, 16), null, null))
			.thenReturn(true);

		ApiException ex = loi(nguoiKhac("2026-11-16", null));

		assertThat(ex.getErrorCode()).isEqualTo("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN");
		verifyNoInteractions(this.ghi, this.nhatKy);
	}

	private ApiException loi(GuiDeNghiCuTruRequest request) {
		ApiException ex = catchThrowableOfType(ApiException.class, () -> this.service.gui(CU_DAN, request));
		assertThat(ex).as("phải ném ApiException").isNotNull();
		return ex;
	}

	private static GuiDeNghiCuTruRequest nguoiKhac(String batDau, String ketThuc) {
		return NhapLieuDeNghiTest.request("A-0101", false, "001026000007", "+84 900.000.007", "Khách thuê",
				(ketThuc == null) ? "Thường trú" : "Tạm trú", batDau, ketThuc,
				(ketThuc == null) ? null : "Hợp đồng thuê bí mật");
	}

	private static ThongTinCuDan cuDan(String maCuDan, String trangThai) {
		return new ThongTinCuDan(maCuDan, "ND00000007", "Đặng Người Thân", "0900000007", "nguoithan@chungcu.test",
				"001026000007", trangThai);
	}

}
