package vn.edu.hust.chungcu.cutru.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;

/**
 * Kiểm thử đơn vị cho phần thuần logic của đề nghị cư trú (spec 0015, AC-2, AC-8, AC-11).
 */
class DeNghiCuTruLogicTest {

	// covers: AC-11 (hai request cùng lọt qua bước kiểm thì unique index chặn)
	@Test
	void loiUniqueDeNghiChoThanh409() {
		RuntimeException trung = GuiDeNghiCuTruService.dich(loi("23505", "uq_hsct_cho_xac_minh_canho_cudan"));
		assertThat(trung).isInstanceOfSatisfying(ApiException.class, ex -> {
			assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(ex.getErrorCode()).isEqualTo("CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH");
		});
		DataIntegrityViolationException khac = loi("23505", "uq_khac");
		assertThat(GuiDeNghiCuTruService.dich(khac)).isSameAs(khac);
		assertThat(GuiDeNghiCuTruService.dich(loi("23P01", "ex_hsct_cu_dan_khong_chong_ngay"))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN"));
	}

	// covers: AC-2 (chuẩn hóa số điện thoại trước khi kiểm mẫu)
	@Test
	void chuanHoaSoDienThoai() {
		assertThat(NhapLieuDeNghi.chuanHoaSdt("+84 912.345-678")).isEqualTo("0912345678");
		assertThat(NhapLieuDeNghi.chuanHoaSdt("84912345678")).isEqualTo("0912345678");
		assertThat(NhapLieuDeNghi.chuanHoaSdt("0912 345 678")).isEqualTo("0912345678");
		assertThat(NhapLieuDeNghi.chuanHoaSdt("84")).isEqualTo("84");
		assertThat(NhapLieuDeNghi.chuanHoaSdt("+84")).isEqualTo("+84");
	}

	// covers: AC-8 (chờ xác minh lên trước theo tạo lúc tăng dần, đã kết thúc theo cập
	// nhật lúc giảm dần)
	@Test
	void thuTuMangDeNghi() {
		HoSoCuTruDong choSau = dong("HS00000003", "Chờ xác minh", 30, 30);
		HoSoCuTruDong choTruoc = dong("HS00000004", "Chờ xác minh", 10, 10);
		HoSoCuTruDong tuChoiCu = dong("HS00000001", "Từ chối", 1, 50);
		HoSoCuTruDong huyMoi = dong("HS00000002", "Đã hủy", 2, 90);
		HoSoCuTruDong hopLe = dong("HS00000005", "Hợp lệ", 1, 1);
		PhanLoaiHoSo.Nhom nhom = PhanLoaiHoSo.chia(List.of(tuChoiCu, choSau, huyMoi, choTruoc, hopLe),
				LocalDate.parse("2026-11-16"));
		assertThat(nhom.deNghiCuTru()).containsExactly(choTruoc, choSau, huyMoi, tuChoiCu);
		assertThat(nhom.dangCuTru()).containsExactly(hopLe);
	}

	private static HoSoCuTruDong dong(String ma, String trangThai, long taoLuc, long capNhatLuc) {
		return new HoSoCuTruDong(ma, "A-0101", "CD00000001", null, "Khách thuê", "Thường trú",
				LocalDate.parse("2026-01-01"), null, trangThai, Instant.ofEpochSecond(taoLuc),
				Instant.ofEpochSecond(capNhatLuc), "CD00000002", null, null);
	}

	private static DataIntegrityViolationException loi(String sqlState, String rangBuoc) {
		ServerErrorMessage msg = new ServerErrorMessage("SERROR\0C" + sqlState + "\0Mvi phạm\0n" + rangBuoc + "\0\0");
		return new DataIntegrityViolationException("vi phạm", new PSQLException(msg));
	}

}
