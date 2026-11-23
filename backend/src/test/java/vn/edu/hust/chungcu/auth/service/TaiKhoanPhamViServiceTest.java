package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;

/** Phạm vi của người quản lý với mọi tổ hợp vai trò (spec 0008, AC-15). */
class TaiKhoanPhamViServiceTest {

	static final NguoiQuanLy ADMIN = new NguoiQuanLy("ND00000001", Set.of(VaiTro.QUAN_TRI_VIEN));

	static final NguoiQuanLy ADMIN_KIEM_BQL = new NguoiQuanLy("ND00000001",
			Set.of(VaiTro.QUAN_TRI_VIEN, VaiTro.BAN_QUAN_LY));

	static final NguoiQuanLy BQL = new NguoiQuanLy("ND00000002", Set.of(VaiTro.BAN_QUAN_LY));

	static final NguoiQuanLy BQL_KIEM_CU_DAN = new NguoiQuanLy("ND00000002", Set.of(VaiTro.BAN_QUAN_LY, VaiTro.CU_DAN));

	final TaiKhoanPhamViService service = new TaiKhoanPhamViService();

	static Stream<NguoiQuanLy> admins() {
		return Stream.of(ADMIN, ADMIN_KIEM_BQL);
	}

	static Stream<NguoiQuanLy> bqls() {
		return Stream.of(BQL, BQL_KIEM_CU_DAN);
	}

	/** Mọi tổ hợp vai trò của tài khoản đích không phải cư dân thuần. */
	static Stream<Arguments> khongPhaiCuDanThuan() {
		Stream<List<VaiTro>> motVaiTroKhac = EnumSet.complementOf(EnumSet.of(VaiTro.CU_DAN)).stream().map(List::of);
		Stream<List<VaiTro>> cuDanKiemVaiTroKhac = EnumSet.complementOf(EnumSet.of(VaiTro.CU_DAN))
			.stream()
			.map((v) -> List.of(v, VaiTro.CU_DAN));
		return Stream.concat(Stream.concat(motVaiTroKhac, cuDanKiemVaiTroKhac), Stream.of(List.<VaiTro>of()))
			.flatMap((dich) -> bqls().map((nguoi) -> Arguments.of(nguoi, dich)));
	}

	static void laCam(Throwable t) {
		assertThat(t).isInstanceOf(ApiException.class);
		ApiException e = (ApiException) t;
		assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(e.getErrorCode()).isEqualTo(ErrorCodes.FORBIDDEN);
		assertThat(e.getMessage()).isEqualTo("Bạn không có quyền thực hiện thao tác này");
	}

	@ParameterizedTest
	@MethodSource("admins")
	void adminThayMoiTaiKhoanKeCaKhiKiemBql(NguoiQuanLy admin) {
		assertThat(this.service.chiCuDanThuan(admin)).isFalse();
		for (VaiTro v : VaiTro.values()) {
			assertThat(this.service.trongPhamVi(admin, List.of(v))).as(v.name()).isTrue();
		}
		assertThat(this.service.trongPhamVi(admin, List.of(VaiTro.QUAN_TRI_VIEN, VaiTro.CU_DAN))).isTrue();
	}

	@ParameterizedTest
	@MethodSource("bqls")
	void bqlChiThayCuDanThuan(NguoiQuanLy bql) {
		assertThat(this.service.chiCuDanThuan(bql)).isTrue();
		assertThat(this.service.trongPhamVi(bql, List.of(VaiTro.CU_DAN))).isTrue();
		assertThatCode(() -> this.service.kiemThay(bql, List.of(VaiTro.CU_DAN))).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "{0} với {1}")
	@MethodSource("khongPhaiCuDanThuan")
	void bqlNhan404CungCauKhiTaiKhoanNgoaiPhamVi(NguoiQuanLy bql, List<VaiTro> dich) {
		assertThat(this.service.trongPhamVi(bql, dich)).isFalse();
		assertThatThrownBy(() -> this.service.kiemThay(bql, dich)).isInstanceOfSatisfying(ApiException.class, (e) -> {
			assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(e.getErrorCode()).isEqualTo(ErrorCodes.NOT_FOUND);
			assertThat(e.getMessage()).isEqualTo("Không tìm thấy tài khoản");
		});
	}

	@ParameterizedTest(name = "{0} tạo {1} bị cấm")
	@MethodSource("khongPhaiCuDanThuan")
	void bqlKhongTaoDuocTaiKhoanNgoaiCuDanThuan(NguoiQuanLy bql, List<VaiTro> vaiTroMoi) {
		assertThatThrownBy(() -> this.service.kiemTao(bql, vaiTroMoi)).satisfies(TaiKhoanPhamViServiceTest::laCam);
	}

	@ParameterizedTest
	@MethodSource("bqls")
	void bqlTaoDuocCuDanThuan(NguoiQuanLy bql) {
		assertThatCode(() -> this.service.kiemTao(bql, List.of(VaiTro.CU_DAN))).doesNotThrowAnyException();
	}

	@Test
	void adminTaoDuocMoiTapVaiTro() {
		assertThatCode(() -> this.service.kiemTao(ADMIN, List.of(VaiTro.values()))).doesNotThrowAnyException();
		assertThatCode(() -> this.service.kiemTao(ADMIN, List.of(VaiTro.KE_TOAN))).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(value = VaiTro.class, names = "CU_DAN", mode = EnumSource.Mode.EXCLUDE)
	void bqlChiSuaVaiTroCuDan(VaiTro vaiTro) {
		assertThatCode(() -> this.service.kiemSuaVaiTro(BQL, VaiTro.CU_DAN)).doesNotThrowAnyException();
		assertThatThrownBy(() -> this.service.kiemSuaVaiTro(BQL, vaiTro)).satisfies(TaiKhoanPhamViServiceTest::laCam);
		assertThatCode(() -> this.service.kiemSuaVaiTro(ADMIN, vaiTro)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@MethodSource("bqls")
	void chiAdminGoDuocVaiTro(NguoiQuanLy bql) {
		assertThatThrownBy(() -> this.service.kiemGoVaiTro(bql)).satisfies(TaiKhoanPhamViServiceTest::laCam);
		assertThatCode(() -> this.service.kiemGoVaiTro(ADMIN)).doesNotThrowAnyException();
		assertThatCode(() -> this.service.kiemGoVaiTro(ADMIN_KIEM_BQL)).doesNotThrowAnyException();
	}

	@Test
	void bqlKhongDoiTrangThaiTaiKhoanNgungHoatDongNhungDoiDuocTrangThaiKhac() {
		assertThatThrownBy(() -> this.service.kiemDoiTrangThai(BQL, TrangThaiTaiKhoan.NGUNG_HOAT_DONG))
			.satisfies(TaiKhoanPhamViServiceTest::laCam);
		assertThatCode(() -> this.service.kiemDoiTrangThai(BQL, TrangThaiTaiKhoan.HOAT_DONG))
			.doesNotThrowAnyException();
		assertThatCode(() -> this.service.kiemDoiTrangThai(BQL, TrangThaiTaiKhoan.TAM_KHOA)).doesNotThrowAnyException();
		assertThatCode(() -> this.service.kiemDoiTrangThai(ADMIN, TrangThaiTaiKhoan.NGUNG_HOAT_DONG))
			.doesNotThrowAnyException();
	}

}
