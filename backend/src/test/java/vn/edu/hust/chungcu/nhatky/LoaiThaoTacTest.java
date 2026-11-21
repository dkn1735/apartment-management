package vn.edu.hust.chungcu.nhatky;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;

/** Danh mục loại thao tác (spec 0007, AC-10). */
class LoaiThaoTacTest {

	@Test
	void coCacGiaTriCuaTinhNang8Va10() {
		assertThat(LoaiThaoTac.values()).extracting(LoaiThaoTac::name, LoaiThaoTac::nhan)
			.contains(org.assertj.core.groups.Tuple.tuple("AUTH_DANG_NHAP_THANH_CONG", "Đăng nhập thành công"),
					org.assertj.core.groups.Tuple.tuple("AUTH_DANG_NHAP_THAT_BAI", "Đăng nhập thất bại"),
					org.assertj.core.groups.Tuple.tuple("AUTH_DANG_XUAT", "Đăng xuất"),
					org.assertj.core.groups.Tuple.tuple("AUTH_PHAT_HIEN_DUNG_LAI_TOKEN", "Phát hiện dùng lại token cũ"),
					org.assertj.core.groups.Tuple.tuple("AUTH_DOI_MAT_KHAU", "Đổi mật khẩu"));
	}

	@Test
	void moiGiaTriCoTienToPhanHeVaNhanHopLe() {
		for (LoaiThaoTac loai : LoaiThaoTac.values()) {
			assertThat(loai.name()).as(loai.name()).matches("^[A-Z]+_[A-Z0-9_]+$").hasSizeLessThanOrEqualTo(100);
			assertThat(loai.nhan()).as(loai.name()).isNotBlank();
			assertThat(Normalizer.isNormalized(loai.nhan(), Normalizer.Form.NFC)).as(loai.name()).isTrue();
		}
		assertThat(Arrays.stream(LoaiThaoTac.values()).map(LoaiThaoTac::nhan)).doesNotHaveDuplicates();
	}

}
