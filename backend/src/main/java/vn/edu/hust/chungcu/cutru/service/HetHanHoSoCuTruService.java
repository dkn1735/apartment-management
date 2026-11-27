package vn.edu.hust.chungcu.cutru.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Chuyển hồ sơ hợp lệ đã qua ngày kết thúc sang {@code Hết hiệu lực} (spec 0014, AC-11).
 * Mỗi căn một transaction riêng, theo thứ tự mã căn; lỗi ở một căn được log rồi bỏ qua.
 */
@Service
public class HetHanHoSoCuTruService {

	private static final Logger log = LoggerFactory.getLogger(HetHanHoSoCuTruService.class);

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	private final CanHoCuTruService canHo;

	private final DongBoTrangThaiCanHo dongBo;

	private final NhatKyService nhatKy;

	private final Clock clock;

	private final TransactionTemplate transaction;

	public HetHanHoSoCuTruService(HoSoCuTruQuery query, HoSoCuTruGhi ghi, CanHoCuTruService canHo,
			DongBoTrangThaiCanHo dongBo, NhatKyService nhatKy, Clock clock,
			PlatformTransactionManager transactionManager) {
		this.query = query;
		this.ghi = ghi;
		this.canHo = canHo;
		this.dongBo = dongBo;
		this.nhatKy = nhatKy;
		this.clock = clock;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	/** Trả số hồ sơ đã chuyển sang {@code Hết hiệu lực}. */
	public int chay() {
		LocalDate homNay = LocalDate.now(this.clock);
		int tong = 0;
		for (String maCanHo : this.query.maCanHoCoHoSoQuaHan(homNay)) {
			try {
				Integer soDong = this.transaction.execute(status -> xuLy(maCanHo, homNay));
				tong += (soDong != null) ? soDong : 0;
			}
			catch (RuntimeException ex) {
				log.error("Hết hạn hồ sơ cư trú thất bại: maCanHo={}, loi={}: {}", maCanHo,
						ex.getClass().getSimpleName(), ex.getMessage());
			}
		}
		return tong;
	}

	private int xuLy(String maCanHo, LocalDate homNay) {
		String trangThaiCan = this.canHo.khoaVaDocTrangThai(maCanHo).orElse(null);
		if (trangThaiCan == null) {
			return 0;
		}
		Instant luc = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		List<HoSoCuTruGhi.DaHetHan> daHetHan = this.ghi.hetHan(maCanHo, homNay, luc);
		if (daHetHan.isEmpty()) {
			return 0;
		}
		ChiTietCuTru.ThayDoi<String> doiCan = this.dongBo.dongBo(maCanHo, trangThaiCan, homNay);
		for (int i = 0; i < daHetHan.size(); i++) {
			HoSoCuTruGhi.DaHetHan hs = daHetHan.get(i);
			boolean cuoi = (i == daHetHan.size() - 1);
			this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CUTRU_KET_THUC_HO_SO, null,
					DoiTuongTacDong.hoSoCuTru(hs.maHoSo()), new ChiTietCuTru.KetThuc(maCanHo, hs.maCuDan(),
							hs.ngayKetThuc().toString(), true, null, null, null, cuoi ? doiCan : null)));
		}
		return daHetHan.size();
	}

}
