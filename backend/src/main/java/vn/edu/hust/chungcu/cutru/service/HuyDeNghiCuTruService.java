package vn.edu.hust.chungcu.cutru.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Người gửi tự rút đề nghị còn chờ xác minh (spec 0015, AC-6, AC-13). Thứ tự: 403 dòng cư
 * dân, 404 chung cho mọi đề nghị không phải của mình, khóa căn rồi hồ sơ, 409 hết chờ.
 * Người xác minh giữ {@code null}.
 */
@Service
public class HuyDeNghiCuTruService {

	private final CuTruDinhDanhService dinhDanh;

	private final KhoaDeNghiCuTru khoaDeNghi;

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	private final DungHoSoCuTru dungHoSo;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public HuyDeNghiCuTruService(CuTruDinhDanhService dinhDanh, KhoaDeNghiCuTru khoaDeNghi, HoSoCuTruQuery query,
			HoSoCuTruGhi ghi, DungHoSoCuTru dungHoSo, NhatKyService nhatKy, Clock clock) {
		this.dinhDanh = dinhDanh;
		this.khoaDeNghi = khoaDeNghi;
		this.query = query;
		this.ghi = ghi;
		this.dungHoSo = dungHoSo;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public HoSoCuTruResponse huy(PhamViTruyCap phamVi, String recordId) {
		String nguoiGui = this.dinhDanh.maCuDanCua(phamVi.maNguoiDung()).orElseThrow(CuTruLoi::khongCoQuyen);
		HoSoCuTruDong d = this.khoaDeNghi.choNguoiGui(phamVi, recordId, nguoiGui).dong();
		this.ghi.ketLuanDeNghi(d.maHoSo(), GiaTriCuTru.DA_HUY, null, null,
				Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CUTRU_HUY_DE_NGHI, phamVi.maNguoiDung(),
				DoiTuongTacDong.hoSoCuTru(d.maHoSo()),
				new ChiTietCuTru.HuyDeNghi(d.maCanHo(), d.maCuDan(), d.maCuDanDeNghi())));
		return this.dungHoSo
			.dung(List.of(this.query.cua(d.maHoSo()).orElseThrow()), DungHoSoCuTru.CheDo.CU_DAN_DE_NGHI, nguoiGui)
			.get(0);
	}

}
