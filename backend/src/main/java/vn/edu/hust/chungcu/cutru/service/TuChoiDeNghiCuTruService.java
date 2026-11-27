package vn.edu.hust.chungcu.cutru.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.dto.TuChoiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * BQL từ chối một đề nghị cư trú kèm lý do (spec 0015, AC-5, AC-13). Thứ tự: 403 dòng
 * nhân viên BQL, 400 body, 404 phạm vi, khóa căn rồi hồ sơ, 409 hết chờ. Không đổi trạng
 * thái căn.
 */
@Service
public class TuChoiDeNghiCuTruService {

	private final CuTruDinhDanhService dinhDanh;

	private final KhoaDeNghiCuTru khoaDeNghi;

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	private final DungHoSoCuTru dungHoSo;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public TuChoiDeNghiCuTruService(CuTruDinhDanhService dinhDanh, KhoaDeNghiCuTru khoaDeNghi, HoSoCuTruQuery query,
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
	public HoSoCuTruResponse tuChoi(PhamViTruyCap phamVi, String recordId, TuChoiDeNghiCuTruRequest request) {
		String maNv = this.dinhDanh.maNvbqlCua(phamVi.maNguoiDung()).orElseThrow(CuTruLoi::khongCoQuyen);
		String lyDo = NhapLieuDeNghi.lyDoTuChoi(request);
		HoSoCuTruDong d = this.khoaDeNghi.choBql(phamVi, recordId).dong();
		this.ghi.ketLuanDeNghi(d.maHoSo(), GiaTriCuTru.TU_CHOI, maNv, lyDo,
				Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CUTRU_TU_CHOI_DE_NGHI, phamVi.maNguoiDung(),
				DoiTuongTacDong.hoSoCuTru(d.maHoSo()),
				new ChiTietCuTru.TuChoiDeNghi(d.maCanHo(), d.maCuDan(), d.maCuDanDeNghi(), lyDo)));
		return this.dungHoSo.dung(this.query.cua(d.maHoSo()).orElseThrow());
	}

}
