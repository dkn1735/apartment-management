package vn.edu.hust.chungcu.phuongtien.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.cutru.service.CuTruService;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienDong;

/** Góc nhìn và lọc trường của danh sách phương tiện (spec 0016, AC-2, AC-3, AC-4). */
class HienThiPhuongTienTest {

	private static final String TOI = "CD00000001";

	private static final String KHAC = "CD00000002";

	private static final Instant LUC = Instant.parse("2026-11-01T00:00:00Z");

	private final CuTruService cuTru = mock(CuTruService.class);

	private final CuTruDinhDanhService dinhDanh = mock(CuTruDinhDanhService.class);

	// 17:30 UTC ngày 15 là ngày 16 ở Việt Nam
	private final HienThiPhuongTien hienThi = new HienThiPhuongTien(this.cuTru, this.dinhDanh,
			Clock.fixed(Instant.parse("2026-11-15T17:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

	// AC-3
	@Test
	void gocNhinNhanVienThayDuNguoiCapNhatVaLyDo() {
		giaLap();

		PhuongTienResponse xe = dung(GocNhin.NHAN_VIEN, null, dong("PT00000001", KHAC, null, "Tạm dừng")).get(0);

		assertThat(xe.lyDoTrangThai()).isEqualTo("Nợ phí");
		assertThat(xe.maNvbqlCapNhat()).isEqualTo("BQL00000001");
		assertThat(xe.hoTenNguoiCapNhat()).isEqualTo("Trần BQL");
	}

	// AC-3: cư dân chỉ thấy lý do trên xe mình sở hữu hoặc mình gửi, không thấy nhân viên
	@Test
	void gocNhinCuDanAnNhanVienVaLyDoCuaHoKhac() {
		giaLap();

		List<PhuongTienResponse> xe = dung(GocNhin.CU_DAN_CAN_HO, TOI, dong("PT00000001", TOI, null, "Tạm dừng"),
				dong("PT00000002", KHAC, TOI, "Từ chối"), dong("PT00000003", KHAC, KHAC, "Tạm dừng"));

		assertThat(xe).extracting(PhuongTienResponse::lyDoTrangThai).containsExactly("Nợ phí", "Nợ phí", null);
		assertThat(xe).allSatisfy(x -> {
			assertThat(x.maNvbqlCapNhat()).isNull();
			assertThat(x.hoTenNguoiCapNhat()).isNull();
		});
	}

	// AC-3: chỉ người gửi, chỉ khi đơn còn chờ, và không phụ thuộc góc nhìn
	@Test
	void coTheRutDonChiChoNguoiGuiDonConCho() {
		giaLap();

		List<PhuongTienResponse> xe = dung(GocNhin.NHAN_VIEN, TOI, dong("PT00000001", KHAC, TOI, "Chờ duyệt"),
				dong("PT00000002", TOI, KHAC, "Chờ duyệt"), dong("PT00000003", TOI, TOI, "Từ chối"));

		assertThat(xe).extracting(PhuongTienResponse::coTheRutDon).containsExactly(true, false, false);
	}

	// AC-2, AC-4: cờ cư trú theo ngày Việt Nam; mỗi loại tra cứu đúng một lần cho cả danh
	// sách
	@Test
	void coCuTruVaTraCuuTheoLo() {
		giaLap();

		List<PhuongTienResponse> xe = dung(GocNhin.NHAN_VIEN, null, dong("PT00000001", TOI, null, "Đang hoạt động"),
				dong("PT00000002", KHAC, TOI, "Chờ duyệt"));

		assertThat(xe).extracting(PhuongTienResponse::chuXeConCuTru).containsExactly(true, false);
		assertThat(xe).extracting(PhuongTienResponse::hoTenChuXe).containsExactly("Người Một", "Người Hai");
		assertThat(xe.get(1).hoTenNguoiDangKy()).isEqualTo("Người Một");
		verify(this.cuTru, times(1)).maCuDanDangCuTru("A-0101", LocalDate.of(2026, 11, 16));
		verify(this.dinhDanh, times(1)).chiTietCuDanTheoMa(anyCollection());
		verify(this.dinhDanh, times(1)).hoTenNvbqlTheoMa(anyCollection());
	}

	// AC-2, AC-3: nhân viên cần đúng tòa; ngoài tòa thì chỉ còn quyền cư dân nếu đang ở
	// căn
	@Test
	void chonGocNhinTheoVaiTroVaPhamVi() {
		PhamViTruyCap bqtKiemCuDan = phamVi(Set.of(VaiTro.BAN_QUAN_TRI, VaiTro.CU_DAN), "A", "B-0101");

		assertThat(GocNhin.cua(bqtKiemCuDan, "A", "A-0101")).contains(GocNhin.NHAN_VIEN);
		assertThat(GocNhin.cua(bqtKiemCuDan, "B", "B-0101")).contains(GocNhin.CU_DAN_CAN_HO);
		assertThat(GocNhin.cua(bqtKiemCuDan, "C", "C-0101")).isEmpty();
		assertThat(GocNhin.cua(phamVi(Set.of(VaiTro.BAN_QUAN_LY), "A", "B-0101"), "B", "B-0101")).isEmpty();
	}

	private static PhamViTruyCap phamVi(Set<VaiTro> vaiTro, String maToaNha, String maCanHo) {
		return new PhamViTruyCap("ND00000001", vaiTro, List.of(maToaNha), List.of(maCanHo));
	}

	private void giaLap() {
		when(this.cuTru.maCuDanDangCuTru(any(), any())).thenReturn(List.of(TOI));
		when(this.dinhDanh.chiTietCuDanTheoMa(anyCollection()))
			.thenReturn(Map.of(TOI, cuDan(TOI, "Người Một"), KHAC, cuDan(KHAC, "Người Hai")));
		when(this.dinhDanh.hoTenNvbqlTheoMa(anyCollection())).thenReturn(Map.of("BQL00000001", "Trần BQL"));
	}

	private List<PhuongTienResponse> dung(GocNhin gocNhin, String toi, PhuongTienDong... dong) {
		return this.hienThi.dung("A-0101", List.of(dong), gocNhin, toi);
	}

	private static ThongTinCuDan cuDan(String ma, String hoTen) {
		return new ThongTinCuDan(ma, "ND00000009", hoTen, "0900000000", "x@chungcu.test", "001000000000", "Hoạt động");
	}

	private static PhuongTienDong dong(String ma, String chuXe, String nguoiGui, String trangThai) {
		return new PhuongTienDong(ma, "A-0101", chuXe, "Xe đạp", null, null, trangThai, nguoiGui, "BQL00000001",
				"Nợ phí", LUC, LUC);
	}

}
