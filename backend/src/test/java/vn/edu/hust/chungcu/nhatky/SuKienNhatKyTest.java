package vn.edu.hust.chungcu.nhatky;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;

/** Kiểm tra ở constructor của record, để nơi gọi sai thì đỏ ngay (spec 0007). */
class SuKienNhatKyTest {

	@Test
	void doiTuongDungDang() {
		assertThat(DoiTuongTacDong.nguoiDung("ND00000001")).hasToString("NguoiDungHeThong:ND00000001");
		assertThat(DoiTuongTacDong.nguoiDung(null)).hasToString("NguoiDungHeThong");
		assertThat(new DoiTuongTacDong("HoaDon", "HD-2026-01")).hasToString("HoaDon:HD-2026-01");
	}

	@Test
	void doiTuongSaiDangThiNem() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DoiTuongTacDong(null, "x"));
		assertThatIllegalArgumentException().isThrownBy(() -> new DoiTuongTacDong("nguoiDung", "x"));
		assertThatIllegalArgumentException().isThrownBy(() -> new DoiTuongTacDong("Nguoi_Dung", "x"));
		assertThatIllegalArgumentException().isThrownBy(() -> new DoiTuongTacDong("NguoiDung", "có dấu"));
		assertThatIllegalArgumentException().isThrownBy(() -> new DoiTuongTacDong("NguoiDung", "a:b"));
		assertThatIllegalArgumentException().isThrownBy(() -> new DoiTuongTacDong("NguoiDung", ""));
		assertThatIllegalArgumentException().isThrownBy(() -> new DoiTuongTacDong("A", "x".repeat(149)));
		assertThat(new DoiTuongTacDong("A", "x".repeat(148)).toString()).hasSize(150);
	}

	@Test
	void suKienThieuLoaiHayDoiTuongThiNem() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new SuKienNhatKy(null, null, DoiTuongTacDong.nguoiDung(null), null));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_XUAT, null, null, null));
	}

}
