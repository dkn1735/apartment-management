package vn.edu.hust.chungcu.cutru.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.CapNhatHoSoCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.ThemHoSoCuTruRequest;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/** Luật thuần của phân hệ cư trú với phụ thuộc giả (spec 0014). */
class CuTruLogicTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 16);

	// AC-5: luật ngày kết thúc khi hồ sơ hợp lệ
	@Test
	void ngayKetThucKhiHopLe() {
		NhapLieuCuTru.NgayNhap vang = NhapLieuCuTru.ngayKetThuc(null);
		assertThat(NhapLieuCuTru.loiKhiHopLe(vang, HOM_NAY, "Tạm trú", HOM_NAY))
			.isEqualTo("Tạm trú bắt buộc có ngày kết thúc");
		assertThat(NhapLieuCuTru.loiKhiHopLe(vang, HOM_NAY, "Thường trú", HOM_NAY)).isNull();
		assertThat(NhapLieuCuTru.loiKhiHopLe(NhapLieuCuTru.ngayKetThuc(JSON.valueToTree("2026-13-01")), HOM_NAY,
				"Thường trú", HOM_NAY))
			.isEqualTo("Ngày kết thúc không hợp lệ");
		assertThat(NhapLieuCuTru.loiKhiHopLe(NhapLieuCuTru.ngayKetThuc(JSON.valueToTree(20261116)), HOM_NAY,
				"Thường trú", HOM_NAY))
			.isEqualTo("Ngày kết thúc không hợp lệ");
		assertThat(NhapLieuCuTru.loiKhiHopLe(ngay("2026-11-16"), HOM_NAY, "Tạm trú", HOM_NAY)).isNull();
	}

	// AC-8: luật ngày kết thúc khi đóng hồ sơ
	@Test
	void ngayKetThucKhiKetThuc() {
		LocalDate tuongLai = HOM_NAY.plusDays(10);
		assertThat(NhapLieuCuTru.loiKhiKetThuc(ngay("2026-11-16"), HOM_NAY, HOM_NAY)).isNull();
		assertThat(NhapLieuCuTru.loiKhiKetThuc(ngay("2026-11-17"), HOM_NAY, HOM_NAY))
			.isEqualTo("Ngày kết thúc khi đóng hồ sơ không được lớn hơn hôm nay");
		assertThat(NhapLieuCuTru.loiKhiKetThuc(ngay("2026-11-15"), HOM_NAY, HOM_NAY))
			.isEqualTo("Ngày kết thúc phải từ ngày bắt đầu trở đi");
		assertThat(NhapLieuCuTru.loiKhiKetThuc(ngay("2026-11-26"), tuongLai, HOM_NAY)).isNull();
		assertThat(NhapLieuCuTru.loiKhiKetThuc(ngay("2026-11-27"), tuongLai, HOM_NAY))
			.isEqualTo("Hủy hồ sơ chưa tới ngày bắt đầu cần đặt ngày kết thúc bằng ngày bắt đầu");
		assertThat(NhapLieuCuTru.loiKhiKetThuc(NhapLieuCuTru.ngayKetThuc(null), HOM_NAY, HOM_NAY))
			.isEqualTo("Kết thúc hồ sơ bắt buộc có ngày kết thúc");
	}

	@Test
	void maHoSoVaMaCuDanChuanHoa() {
		assertThat(NhapLieuCuTru.maHoSo(" hs00000001 ")).isEqualTo("HS00000001");
		assertThat(NhapLieuCuTru.maHoSo("HS0000001")).isNull();
		assertThat(NhapLieuCuTru.tuKhoa(null)).isEmpty();
		assertThat(NhapLieuCuTru.doDaiTuKhoa("ạ".repeat(100))).isNull();
		assertThat(NhapLieuCuTru.doDaiTuKhoa("ạ".repeat(101))).isEqualTo("Từ khóa tối đa 100 ký tự");
	}

	// AC-10: bảng luật đồng bộ trạng thái căn
	@Test
	void luatDongBoTrangThaiCan() {
		assertThat(DongBoTrangThaiCanHo.trangThaiMoi("Trống", 1)).isEqualTo("Đang sử dụng");
		assertThat(DongBoTrangThaiCanHo.trangThaiMoi("Đã bàn giao", 2)).isEqualTo("Đang sử dụng");
		assertThat(DongBoTrangThaiCanHo.trangThaiMoi("Đang sử dụng", 1)).isEqualTo("Đang sử dụng");
		assertThat(DongBoTrangThaiCanHo.trangThaiMoi("Đang sửa chữa", 1)).isEqualTo("Đang sửa chữa");
		assertThat(DongBoTrangThaiCanHo.trangThaiMoi("Đang sử dụng", 0)).isEqualTo("Trống");
		assertThat(DongBoTrangThaiCanHo.trangThaiMoi("Đã bàn giao", 0)).isEqualTo("Đã bàn giao");
		assertThat(DongBoTrangThaiCanHo.trangThaiMoi("Đang sửa chữa", 0)).isEqualTo("Đang sửa chữa");
	}

	// AC-1: chia nhóm và thứ tự, Chờ xác minh bị bỏ
	@Test
	void chiaNhomVaSapXep() {
		HoSoCuTruDong khach = dong("HS00000001", "Khách thuê", "2026-01-01", null, "Hợp lệ");
		HoSoCuTruDong chu = dong("HS00000002", "Chủ căn hộ", "2026-06-01", null, "Hợp lệ");
		HoSoCuTruDong sap = dong("HS00000003", "Khách thuê", "2026-12-01", "2027-01-01", "Hợp lệ");
		HoSoCuTruDong quaHan = dong("HS00000004", "Khách thuê", "2026-01-01", "2026-11-15", "Hợp lệ");
		HoSoCuTruDong huySom = dong("HS00000005", "Khách thuê", "2027-01-01", "2027-01-01", "Hết hiệu lực");
		HoSoCuTruDong cu = dong("HS00000006", "Khách thuê", "2025-01-01", "2025-12-31", "Hết hiệu lực");
		HoSoCuTruDong cho = dong("HS00000007", "Khách thuê", "2026-01-01", null, "Chờ xác minh");
		PhanLoaiHoSo.Nhom nhom = PhanLoaiHoSo.chia(List.of(khach, chu, sap, quaHan, huySom, cu, cho), HOM_NAY);
		assertThat(nhom.dangCuTru()).containsExactly(chu, khach);
		assertThat(nhom.sapBatDau()).containsExactly(sap);
		assertThat(nhom.lichSuCuTru()).containsExactly(quaHan, cu, huySom);
	}

	// AC-4: kiểm dòng nhân viên BQL trước cả kiểm body
	@Test
	void matDongNhanVienBqlThi403TruocKiemBody() {
		CuTruDinhDanhService dinhDanh = mock(CuTruDinhDanhService.class);
		when(dinhDanh.maNvbqlCua(anyString())).thenReturn(Optional.empty());
		CanHoCuTruService canHo = mock(CanHoCuTruService.class);
		ThemHoSoCuTruService service = new ThemHoSoCuTruService(dinhDanh, canHo, mock(HoSoCuTruQuery.class),
				mock(HoSoCuTruGhi.class), mock(KiemChongNgay.class), mock(DongBoTrangThaiCanHo.class),
				mock(DungHoSoCuTru.class), mock(NhatKyService.class),
				Clock.fixed(Instant.parse("2026-11-16T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
		PhamViTruyCap phamVi = new PhamViTruyCap("ND00000002", Set.of(VaiTro.BAN_QUAN_LY), List.of("A"), List.of());
		assertThatThrownBy(() -> service.them(phamVi, new ThemHoSoCuTruRequest(null, null, null, null, null, null)))
			.isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
				assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
			});
		verifyNoInteractions(canHo);
	}

	// AC-9: vi phạm ràng buộc loại trừ được dịch về đúng mã 409
	@Test
	void dichLoiRangBuocLoaiTru() {
		assertThat(KiemChongNgay.dich(loi("23P01", "ex_hsct_mot_chu_can_ho"))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("CUTRU_DA_CO_CHU_CAN_HO"));
		assertThat(KiemChongNgay.dich(loi("23P01", "ex_hsct_cu_dan_khong_chong_ngay"))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN"));
		DataIntegrityViolationException khac = loi("23514", "chk_hsct_ngay");
		assertThat(KiemChongNgay.dich(khac)).isSameAs(khac);
	}

	// covers: AC-1, AC-5 (sắp xếp thứ cấp và chuẩn hóa NFC từ chuỗi tổ hợp rời)
	@Test
	void sapXepThuCapVaChuanHoaNfc() {
		HoSoCuTruDong chuMuon = dong("HS00000005", "Chủ căn hộ", "2026-06-01", null, "Hợp lệ");
		HoSoCuTruDong thanhVienB = dong("HS00000004", "Thành viên gia đình", "2026-02-01", null, "Hợp lệ");
		HoSoCuTruDong thanhVienA = dong("HS00000003", "Thành viên gia đình", "2026-02-01", null, "Hợp lệ");
		HoSoCuTruDong khach = dong("HS00000001", "Khách thuê", "2026-01-01", null, "Hợp lệ");
		HoSoCuTruDong sapB = dong("HS00000009", "Khách thuê", "2026-12-01", "2027-01-01", "Hợp lệ");
		HoSoCuTruDong sapA = dong("HS00000008", "Khách thuê", "2026-12-01", "2027-01-01", "Hợp lệ");
		HoSoCuTruDong ls1 = dong("HS00000010", "Khách thuê", "2025-01-01", "2025-12-31", "Hết hiệu lực");
		HoSoCuTruDong ls2 = dong("HS00000011", "Khách thuê", "2025-06-01", "2025-12-31", "Hết hiệu lực");

		PhanLoaiHoSo.Nhom nhom = PhanLoaiHoSo
			.chia(List.of(khach, thanhVienB, thanhVienA, chuMuon, sapB, sapA, ls1, ls2), HOM_NAY);
		assertThat(nhom.dangCuTru()).containsExactly(chuMuon, thanhVienA, thanhVienB, khach);
		assertThat(nhom.sapBatDau()).containsExactly(sapA, sapB);
		assertThat(nhom.lichSuCuTru()).containsExactly(ls2, ls1);

		List<FieldError> loiNfc = new java.util.ArrayList<>();
		String nfd = java.text.Normalizer.normalize(" Chủ căn hộ ", java.text.Normalizer.Form.NFD);
		assertThat(nfd).isNotEqualTo(" Chủ căn hộ ");
		assertThat(NhapLieuCuTru.chon(JSON.valueToTree(nfd), "vaiTroCuTru", GiaTriCuTru.VAI_TRO, "vắng", "sai", loiNfc))
			.isEqualTo("Chủ căn hộ");
		assertThat(loiNfc).isEmpty();
	}

	// covers: AC-7, AC-8, AC-9 (PUT kiểm dòng nhân viên BQL trước, thứ tự lỗi 400 và dịch
	// lỗi GIST)
	@Test
	void capNhatKiemNhanVienBqlThuTuLoiVaDichLoiDb() {
		CuTruDinhDanhService dinhDanh = mock(CuTruDinhDanhService.class);
		when(dinhDanh.maNvbqlCua(anyString())).thenReturn(Optional.empty());
		CanHoCuTruService canHo = mock(CanHoCuTruService.class);
		Clock clock = Clock.fixed(Instant.parse("2026-11-16T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
		CapNhatHoSoCuTruService chan403 = new CapNhatHoSoCuTruService(dinhDanh, canHo, mock(HoSoCuTruQuery.class),
				mock(HoSoCuTruGhi.class), mock(KiemChongNgay.class), mock(DongBoTrangThaiCanHo.class),
				mock(DungHoSoCuTru.class), mock(NhatKyService.class), clock);
		PhamViTruyCap phamVi = new PhamViTruyCap("ND00000002", Set.of(VaiTro.BAN_QUAN_LY), List.of("A"), List.of());

		assertThatThrownBy(
				() -> chan403.capNhat(phamVi, "HS00000001", new CapNhatHoSoCuTruRequest(null, null, null, null, null)))
			.isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
		verifyNoInteractions(canHo);

		assertThatThrownBy(() -> CapNhatHoSoCuTruService
			.kiem(new CapNhatHoSoCuTruRequest(JSON.valueToTree("Chủ căn hộ"), JSON.valueToTree("Thường trú"),
					JSON.valueToTree("2026-11-01"), JSON.valueToTree("sai-ngay"), null), HOM_NAY))
			.isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrors()).extracting(FieldError::field)
				.containsExactly("ngayKetThuc", "trangThaiHoSo"));

		when(dinhDanh.maNvbqlCua("ND00000002")).thenReturn(Optional.of("BQL00000001"));
		HoSoCuTruQuery query = mock(HoSoCuTruQuery.class);
		HoSoCuTruDong cu = dong("HS00000001", "Khách thuê", "2026-11-01", "2026-12-01", "Hợp lệ");
		when(query.cua("HS00000001")).thenReturn(Optional.of(cu));
		when(canHo.maToaNhaCua("A-0101")).thenReturn(Optional.of("A"));
		when(canHo.khoaVaDocTrangThai("A-0101")).thenReturn(Optional.of("Đang sử dụng"));
		HoSoCuTruGhi ghi = mock(HoSoCuTruGhi.class);
		when(ghi.khoaDong("HS00000001")).thenReturn(Optional.of(cu));
		doThrow(loi("23P01", "ex_hsct_cu_dan_khong_chong_ngay")).when(ghi)
			.capNhat(anyString(), any(), anyString(), any());
		NhatKyService nhatKy = mock(NhatKyService.class);
		CapNhatHoSoCuTruService dich409 = new CapNhatHoSoCuTruService(dinhDanh, canHo, query, ghi,
				new KiemChongNgay(query), mock(DongBoTrangThaiCanHo.class), mock(DungHoSoCuTru.class), nhatKy, clock);

		assertThatThrownBy(() -> dich409.capNhat(phamVi, "HS00000001",
				new CapNhatHoSoCuTruRequest(JSON.valueToTree("Khách thuê"), JSON.valueToTree("Tạm trú"),
						JSON.valueToTree("2026-11-01"), JSON.valueToTree("2027-01-01"), JSON.valueToTree("Hợp lệ"))))
			.isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.getErrorCode()).isEqualTo("CUTRU_TRUNG_KHOANG_NGAY_CU_DAN"));
		verifyNoInteractions(nhatKy);
	}

	private static NhapLieuCuTru.NgayNhap ngay(String value) {
		return NhapLieuCuTru.ngayKetThuc(JSON.valueToTree(value));
	}

	private static HoSoCuTruDong dong(String ma, String vaiTro, String batDau, String ketThuc, String trangThai) {
		return new HoSoCuTruDong(ma, "A-0101", "CD00000001", "BQL00000001", vaiTro, "Tạm trú", LocalDate.parse(batDau),
				(ketThuc != null) ? LocalDate.parse(ketThuc) : null, trangThai, Instant.EPOCH, Instant.EPOCH, null,
				null, null);
	}

	private static DataIntegrityViolationException loi(String sqlState, String rangBuoc) {
		ServerErrorMessage msg = new ServerErrorMessage("SERROR\0C" + sqlState + "\0Mvi phạm\0n" + rangBuoc + "\0\0");
		return new DataIntegrityViolationException("vi phạm", new PSQLException(msg));
	}

}
