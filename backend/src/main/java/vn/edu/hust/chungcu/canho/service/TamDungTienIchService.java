package vn.edu.hust.chungcu.canho.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.dto.DoiTienIchRequest;
import vn.edu.hust.chungcu.canho.repository.CanHoGhi;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Tạm dừng hoặc mở khóa quyền tiện ích của một căn (spec 0013, AC-1 đến AC-7). Đây là
 * đường duy nhất ghi cờ và lần đổi tiện ích; cờ hiện tại đọc dưới khóa dòng, nên hai yêu
 * cầu song song được xử lý lần lượt và mỗi dòng nhật ký ứng với một lần cờ thật sự đổi.
 */
@Service
public class TamDungTienIchService {

	private final CanHoQuery query;

	private final CanHoGhi ghi;

	private final NhatKyService nhatKy;

	private final NguoiDoiTienIch nguoiDoi;

	private final Clock clock;

	public TamDungTienIchService(CanHoQuery query, CanHoGhi ghi, NhatKyService nhatKy, NguoiDoiTienIch nguoiDoi,
			Clock clock) {
		this.query = query;
		this.ghi = ghi;
		this.nhatKy = nhatKy;
		this.nguoiDoi = nguoiDoi;
		this.clock = clock;
	}

	/** Thứ tự kiểm: body (400), phạm vi (404), rồi trạng thái dưới khóa dòng (409). */
	@Transactional
	public CanHoResponse doi(PhamViTruyCap phamVi, String apartmentId, DoiTienIchRequest request) {
		YeuCau yeuCau = kiem(request);
		String ma = NhapLieu.chuanHoaMaToaNha(apartmentId);
		if (this.query.maToaNhaCua(ma).filter(phamVi.maToaNha()::contains).isEmpty()) {
			throw khongThay();
		}
		boolean dangTamDung = this.ghi.khoaCoTienIch(ma).orElseThrow(TamDungTienIchService::khongThay);
		if (dangTamDung == yeuCau.tamDung()) {
			throw CanHoLoi.tienIchKhongDoi(dangTamDung);
		}
		Instant luc = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		this.ghi.capNhatTienIch(ma, yeuCau.tamDung(), yeuCau.lyDo(), luc, phamVi.maNguoiDung());
		LoaiThaoTac loai = yeuCau.tamDung() ? LoaiThaoTac.CANHO_TAM_DUNG_TIEN_ICH : LoaiThaoTac.CANHO_MO_KHOA_TIEN_ICH;
		this.nhatKy.ghi(new SuKienNhatKy(loai, phamVi.maNguoiDung(), DoiTuongTacDong.canHo(ma),
				new ChiTietCanHo.TienIch(yeuCau.lyDo())));
		return this.nguoiDoi.ganHoTen(this.query.cua(ma).orElseThrow(TamDungTienIchService::khongThay));
	}

	/** Thứ tự mục lỗi của AC-3: biTamDungTienIch, lyDo. */
	static YeuCau kiem(DoiTienIchRequest request) {
		List<FieldError> loi = new ArrayList<>();
		Boolean tamDung = NhapLieuTienIch.coTamDung(request.biTamDungTienIch(), loi);
		String lyDo = NhapLieuTienIch.lyDo(request.lyDo(), loi);
		if (!loi.isEmpty()) {
			throw CanHoLoi.duLieuKhongHopLe(loi);
		}
		return new YeuCau(tamDung, lyDo);
	}

	private static ApiException khongThay() {
		return ApiException.notFound(PhamViTruyCap.KHONG_TIM_THAY);
	}

	/** Yêu cầu đã kiểm. */
	record YeuCau(boolean tamDung, String lyDo) {
	}

}
