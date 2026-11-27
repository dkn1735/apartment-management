package vn.edu.hust.chungcu.phuongtien.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.phuongtien.dto.DangKyPhuongTienRequest;
import vn.edu.hust.chungcu.phuongtien.dto.DoiTrangThaiPhuongTienRequest;

/** Chuẩn hóa biển số, kiểm dữ liệu nhập và ma trận trạng thái (spec 0016). */
class PhuongTienLogicTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@ParameterizedTest
	@CsvSource({ "30a-123.45,30A12345", "' 29 B1 - 123.45 ',29B112345", "30A 12345,30A12345" })
	void chuanHoaBienSo(String nhap, String chuan) {
		assertThat(BienSo.chuanHoa(nhap)).isEqualTo(chuan);
		assertThat(BienSo.hopLe(chuan)).isTrue();
	}

	// AC-9, AC-10
	@ParameterizedTest
	@CsvSource({ "Chờ duyệt,Đang hoạt động,true", "Chờ duyệt,Từ chối,true", "Chờ duyệt,Tạm dừng,false",
			"Chờ duyệt,Đã hủy,false", "Đang hoạt động,Tạm dừng,true", "Đang hoạt động,Đã hủy,true",
			"Đang hoạt động,Đang hoạt động,false", "Đang hoạt động,Từ chối,false", "Tạm dừng,Đang hoạt động,true",
			"Tạm dừng,Đã hủy,true", "Tạm dừng,Tạm dừng,false", "Từ chối,Đang hoạt động,false",
			"Đã hủy,Đang hoạt động,false" })
	void maTranTrangThai(String tu, String sang, boolean duoc) {
		assertThat(MaTranTrangThai.chuyenDuoc(tu, sang)).isEqualTo(duoc);
	}

	// AC-6: đủ năm trường theo thứ tự
	@Test
	void dangKySaiBaoLoiTheoThuTu() {
		assertThat(loiDangKy("""
				{"maCanHo": 1, "maCuDanSoHuu": "X1", "loaiPhuongTien": "Tàu", "bienSo": 5, "nhanHieuMauSac": 3}"""))
			.extracting(FieldError::message)
			.containsExactly("Căn hộ không hợp lệ", "Chủ xe không hợp lệ", "Loại phương tiện không hợp lệ",
					"Biển số không hợp lệ", "Nhãn hiệu, màu sắc không hợp lệ");
		assertThat(loiDangKy("{}")).extracting(FieldError::field)
			.containsExactly("maCanHo", "maCuDanSoHuu", "loaiPhuongTien", "bienSo");
	}

	// AC-6: biển số
	@Test
	void bienSoBatBuocTruXeDap() {
		String khung = "{\"maCanHo\": \"a-0101\", \"maCuDanSoHuu\": \"cd00000001\", \"loaiPhuongTien\": \"%s\", "
				+ "\"bienSo\": \"%s\", \"nhanHieuMauSac\": \"  \"}";
		NhapLieuPhuongTien.DangKy xeDap = NhapLieuPhuongTien.dangKy(dangKy(khung.formatted("Xe đạp", " .- ")));
		assertThat(xeDap).isEqualTo(new NhapLieuPhuongTien.DangKy("A-0101", "CD00000001", "Xe đạp", null, null));
		assertThat(loiDangKy(khung.formatted("Xe máy", " .- "))).extracting(FieldError::message)
			.containsExactly("Vui lòng nhập biển số");
		assertThat(loiDangKy(khung.formatted("Xe đạp", "AB1"))).extracting(FieldError::message)
			.containsExactly("Biển số chỉ gồm chữ và số, dài từ 5 đến 12 ký tự");
	}

	// AC-9
	@Test
	void doiTrangThaiKiemLyDo() {
		assertThat(loiDoi("{}")).extracting(FieldError::message).containsExactly("Vui lòng chọn trạng thái");
		assertThat(loiDoi("{\"trangThaiDangKy\": \"Chờ duyệt\", \"lyDo\": 1}")).extracting(FieldError::message)
			.containsExactly("Trạng thái không hợp lệ", "Lý do không hợp lệ");
		assertThat(loiDoi("{\"trangThaiDangKy\": \"Tạm dừng\", \"lyDo\": \"  \"}")).extracting(FieldError::message)
			.containsExactly("Vui lòng nhập lý do");
		assertThat(loiDoi("{\"trangThaiDangKy\": \"Đã hủy\", \"lyDo\": \"" + "ạ".repeat(501) + "\"}"))
			.extracting(FieldError::message)
			.containsExactly("Lý do tối đa 500 ký tự");
		assertThat(NhapLieuPhuongTien.doiTrangThai(doi("{\"trangThaiDangKy\": \"Đang hoạt động\"}")).lyDo()).isNull();
	}

	// AC-10: trạng thái cuối không đi đâu được; mọi cặp ngoài bảng đều bị chặn
	@ParameterizedTest
	@CsvSource({ "Tạm dừng,Từ chối", "Từ chối,Từ chối", "Từ chối,Tạm dừng", "Từ chối,Đã hủy", "Đã hủy,Đã hủy",
			"Đã hủy,Tạm dừng", "Đã hủy,Từ chối" })
	void maTranChanCacCapConLai(String tu, String sang) {
		assertThat(MaTranTrangThai.chuyenDuoc(tu, sang)).isFalse();
	}

	// AC-6: NBSP bị bỏ; độ dài biển chuẩn từ 5 đến 12
	@Test
	void bienSoBoNbspVaGioiHanDoDai() {
		assertThat(BienSo.chuanHoa("29\u00A0b1\u00A0123.45")).isEqualTo("29B112345");
		assertThat(BienSo.hopLe("A".repeat(4))).isFalse();
		assertThat(BienSo.hopLe("A".repeat(5))).isTrue();
		assertThat(BienSo.hopLe("A".repeat(12))).isTrue();
		assertThat(BienSo.hopLe("A".repeat(13))).isFalse();
		assertThat(BienSo.hopLe("30Đ12345")).isFalse();
	}

	// AC-6: 100 ký tự tính theo code point, không theo đơn vị UTF-16
	@Test
	void nhanHieuToiDaMotTramCodePoint() {
		String khung = "{\"maCanHo\": \"A-0101\", \"maCuDanSoHuu\": \"CD00000001\", \"loaiPhuongTien\": \"Xe đạp\", "
				+ "\"nhanHieuMauSac\": \"%s\"}";
		String motTram = "🚲".repeat(100);

		assertThat(NhapLieuPhuongTien.dangKy(dangKy(khung.formatted(motTram))).nhanHieuMauSac()).isEqualTo(motTram);
		assertThat(loiDangKy(khung.formatted(motTram + "a"))).extracting(FieldError::message)
			.containsExactly("Nhãn hiệu, màu sắc tối đa 100 ký tự");
	}

	// AC-9: từ chối và hủy cũng bắt buộc lý do; 500 ký tự vẫn được
	@ParameterizedTest
	@CsvSource({ "Từ chối", "Đã hủy" })
	void tuChoiVaHuyBatBuocLyDo(String trangThai) {
		assertThat(loiDoi("{\"trangThaiDangKy\": \"" + trangThai + "\"}")).extracting(FieldError::message)
			.containsExactly("Vui lòng nhập lý do");
		String lyDo = "ạ".repeat(500);
		assertThat(NhapLieuPhuongTien
			.doiTrangThai(doi("{\"trangThaiDangKy\": \"" + trangThai + "\", \"lyDo\": \"" + lyDo + "\"}"))
			.lyDo()).isEqualTo(lyDo);
	}

	// AC-10, AC-11: mã trên đường dẫn được cắt và viết hoa, sai mẫu thì không có
	@Test
	void maPhuongTienTrenDuongDan() {
		assertThat(NhapLieuPhuongTien.maPhuongTien(" pt00000001 ")).isEqualTo("PT00000001");
		assertThat(NhapLieuPhuongTien.maPhuongTien("PT1")).isNull();
		assertThat(NhapLieuPhuongTien.maPhuongTien(null)).isNull();
	}

	private static List<FieldError> loiDangKy(String body) {
		return catchThrowableOfType(ApiException.class, () -> NhapLieuPhuongTien.dangKy(dangKy(body))).getErrors();
	}

	private static List<FieldError> loiDoi(String body) {
		return catchThrowableOfType(ApiException.class, () -> NhapLieuPhuongTien.doiTrangThai(doi(body))).getErrors();
	}

	private static DangKyPhuongTienRequest dangKy(String body) {
		return JSON.readValue(body, DangKyPhuongTienRequest.class);
	}

	private static DoiTrangThaiPhuongTienRequest doi(String body) {
		return JSON.readValue(body, DoiTrangThaiPhuongTienRequest.class);
	}

}
