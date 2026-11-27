package vn.edu.hust.chungcu.cutru.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.GuiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.TuChoiDeNghiCuTruRequest;

/**
 * Kiểm thử đơn vị cho việc đọc dữ liệu nhập của đề nghị cư trú (spec 0015, AC-2, AC-5).
 */
class NhapLieuDeNghiTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 16);

	// covers: AC-2 (đề nghị cho người khác hợp lệ, chuẩn hóa mã căn, CCCD và SĐT)
	@Test
	void deNghiChoNguoiKhacDuocChuanHoa() {
		NhapLieuDeNghi.DeNghiMoi moi = NhapLieuDeNghi.kiem(request("a-0101", false, "0010 2600-0010", "+84 900.000-010",
				"Khách thuê", "Tạm trú", "2026-11-16", "2027-05-16", "  Hợp đồng 6 tháng  "), HOM_NAY);

		assertThat(moi.maCanHo()).isEqualTo("A-0101");
		assertThat(moi.laChinhToi()).isFalse();
		assertThat(moi.soCccd()).isEqualTo("001026000010");
		assertThat(moi.soDienThoai()).isEqualTo("0900000010");
		assertThat(moi.ghiChu()).isEqualTo("Hợp đồng 6 tháng");
		assertThat(moi.giaTri().trangThaiHoSo()).isEqualTo("Chờ xác minh");
		assertThat(moi.giaTri().ngayBatDau()).isEqualTo(HOM_NAY);
		assertThat(moi.giaTri().ngayKetThuc()).isEqualTo(LocalDate.of(2027, 5, 16));
	}

	// covers: AC-2 (laChinhToi = true bỏ qua CCCD, SĐT dù body gửi kèm giá trị rác)
	@Test
	void chinhToiBoQuaCccdVaSdt() {
		NhapLieuDeNghi.DeNghiMoi moi = NhapLieuDeNghi.kiem(
				request("A-0101", true, "abc", "xyz", "Thành viên gia đình", "Thường trú", "2026-12-01", null, "   "),
				HOM_NAY);

		assertThat(moi.laChinhToi()).isTrue();
		assertThat(moi.soCccd()).isNull();
		assertThat(moi.soDienThoai()).isNull();
		assertThat(moi.giaTri().ngayKetThuc()).isNull();
		assertThat(moi.ghiChu()).as("ghi chú chỉ gồm khoảng trắng thành null").isNull();
	}

	// covers: AC-2 (mỗi trường sai một mục, đúng thứ tự trường)
	@Test
	void moiTruongSaiMotMucTheoDungThuTu() {
		GuiDeNghiCuTruRequest sai = new GuiDeNghiCuTruRequest(null, JSON.valueToTree("false"), null, null,
				JSON.valueToTree("Chủ căn hộ"), JSON.valueToTree("Vĩnh viễn"), JSON.valueToTree("2026-11-15"),
				JSON.valueToTree("2026-13-01"), JSON.valueToTree(42));

		List<FieldError> loi = loiCua(sai);

		assertThat(loi).extracting(FieldError::field)
			.containsExactly("maCanHo", "laChinhToi", "vaiTroCuTru", "tinhTrangCuTru", "ngayBatDau", "ngayKetThuc",
					"ghiChuDeNghi");
		assertThat(loi).extracting(FieldError::message)
			.contains("Vui lòng chọn đối tượng cư trú",
					"Cư dân chỉ được đề nghị vai trò Thành viên gia đình hoặc Khách thuê",
					"Ngày bắt đầu của đề nghị phải từ hôm nay trở đi", "Ngày kết thúc không hợp lệ",
					"Ghi chú đề nghị không hợp lệ");
	}

	// covers: AC-2 (thiếu hoặc sai CCCD, SĐT khi đề nghị cho người khác)
	@Test
	void nguoiKhacBatBuocCccdVaSdtHopLe() {
		assertThat(loiCua(request("A-0101", false, null, "  ", "Khách thuê", "Thường trú", "2026-11-16", null, null)))
			.containsExactly(new FieldError("soCccd", "Vui lòng nhập số CCCD"),
					new FieldError("soDienThoai", "Vui lòng nhập số điện thoại"));
		assertThat(loiCua(
				request("A-0101", false, "00102600001", "0800", "Khách thuê", "Thường trú", "2026-11-16", null, null)))
			.containsExactly(new FieldError("soCccd", "Số CCCD phải gồm đúng 12 chữ số"),
					new FieldError("soDienThoai", "Số điện thoại không hợp lệ"));
	}

	// covers: AC-2 (biên ngày bắt đầu là hôm nay, Tạm trú bắt buộc ngày kết thúc)
	@Test
	void luatNgayCuaDeNghi() {
		assertThat(NhapLieuDeNghi
			.kiem(request("A-0101", true, null, null, "Khách thuê", "Thường trú", "2026-11-16", null, null), HOM_NAY)
			.giaTri()
			.ngayBatDau()).isEqualTo(HOM_NAY);
		assertThat(loiCua(request("A-0101", true, null, null, "Khách thuê", "Tạm trú", "2026-11-16", null, null)))
			.containsExactly(new FieldError("ngayKetThuc", "Tạm trú bắt buộc có ngày kết thúc"));
		assertThat(loiCua(
				request("A-0101", true, null, null, "Khách thuê", "Thường trú", "2026-11-20", "2026-11-19", null)))
			.containsExactly(new FieldError("ngayKetThuc", "Ngày kết thúc phải từ ngày bắt đầu trở đi"));
		assertThat(NhapLieuDeNghi
			.kiem(request("A-0101", true, null, null, "Khách thuê", "Tạm trú", "2026-11-20", "2026-11-20", null),
					HOM_NAY)
			.giaTri()
			.ngayKetThuc()).as("ngày kết thúc bằng ngày bắt đầu vẫn hợp lệ").isEqualTo(LocalDate.of(2026, 11, 20));
	}

	// covers: AC-2 (ghi chú đếm theo ký tự Unicode, tối đa 500)
	@Test
	void ghiChuToiDa500KyTuUnicode() {
		String emoji500 = "😀".repeat(500);
		assertThat(NhapLieuDeNghi
			.kiem(request("A-0101", true, null, null, "Khách thuê", "Thường trú", "2026-11-16", null, emoji500),
					HOM_NAY)
			.ghiChu()).isEqualTo(emoji500);
		assertThat(loiCua(
				request("A-0101", true, null, null, "Khách thuê", "Thường trú", "2026-11-16", null, "a".repeat(501))))
			.containsExactly(new FieldError("ghiChuDeNghi", "Ghi chú đề nghị tối đa 500 ký tự"));
	}

	// covers: AC-5 (lý do từ chối bắt buộc, cắt khoảng trắng, đổi NFC, tối đa 500)
	@Test
	void lyDoTuChoi() {
		assertThat(NhapLieuDeNghi.lyDoTuChoi(lyDo(JSON.valueToTree("  Thie\u0302\u0301u ho\u031b\u0323p đồng  "))))
			.isEqualTo("Thiếu hợp đồng");
		assertThat(NhapLieuDeNghi.lyDoTuChoi(lyDo(JSON.valueToTree("x".repeat(500))))).hasSize(500);

		assertThat(loiTuChoi(null)).isEqualTo("Vui lòng nhập lý do từ chối");
		assertThat(loiTuChoi(lyDo(null))).isEqualTo("Vui lòng nhập lý do từ chối");
		assertThat(loiTuChoi(lyDo(JSON.valueToTree("   ")))).isEqualTo("Vui lòng nhập lý do từ chối");
		assertThat(loiTuChoi(lyDo(JSON.valueToTree(123)))).isEqualTo("Lý do từ chối không hợp lệ");
		assertThat(loiTuChoi(lyDo(JSON.valueToTree("x".repeat(501))))).isEqualTo("Lý do từ chối tối đa 500 ký tự");
	}

	private static List<FieldError> loiCua(GuiDeNghiCuTruRequest request) {
		ApiException ex = catchThrowableOfType(ApiException.class, () -> NhapLieuDeNghi.kiem(request, HOM_NAY));
		assertThat(ex).as("phải ném 400").isNotNull();
		assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_FAILED");
		return ex.getErrors();
	}

	private static String loiTuChoi(TuChoiDeNghiCuTruRequest request) {
		ApiException ex = catchThrowableOfType(ApiException.class, () -> NhapLieuDeNghi.lyDoTuChoi(request));
		assertThat(ex).as("phải ném 400").isNotNull();
		assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(ex.getErrors()).hasSize(1).first().extracting(FieldError::field).isEqualTo("lyDoTuChoi");
		return ex.getErrors().get(0).message();
	}

	private static TuChoiDeNghiCuTruRequest lyDo(JsonNode value) {
		return new TuChoiDeNghiCuTruRequest(value);
	}

	static GuiDeNghiCuTruRequest request(String maCanHo, Boolean laChinhToi, String soCccd, String soDienThoai,
			String vaiTro, String tinhTrang, String batDau, String ketThuc, String ghiChu) {
		return new GuiDeNghiCuTruRequest(node(maCanHo), node(laChinhToi), node(soCccd), node(soDienThoai), node(vaiTro),
				node(tinhTrang), node(batDau), node(ketThuc), node(ghiChu));
	}

	private static JsonNode node(Object value) {
		return (value == null) ? null : JSON.valueToTree(value);
	}

}
