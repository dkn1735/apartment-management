package vn.edu.hust.chungcu.nhatky.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.repository.NhatKyGhi;
import vn.edu.hust.chungcu.nhatky.repository.NhatKyGhi.DongNhatKy;

/**
 * Cửa duy nhất để ghi nhật ký kiểm toán (spec 0007). Mỗi dòng ghi trong một transaction
 * riêng; nếu nơi gọi đang có transaction thì chờ tới sau commit (rollback thì không ghi).
 * Giờ, request id và IP lấy lúc gọi. {@link #ghi} không bao giờ ném lỗi: mọi lỗi thành
 * một dòng log ERROR không kèm nội dung chi tiết (AC-6, AC-7, AC-8).
 */
@Service
public class NhatKyService {

	private static final Logger log = LoggerFactory.getLogger(NhatKyService.class);

	private final NhatKyGhi nhatKyGhi;

	private final NguCanhYeuCau nguCanh;

	private final Clock clock;

	private final JsonMapper json;

	private final TransactionTemplate transactionMoi;

	public NhatKyService(NhatKyGhi nhatKyGhi, NguCanhYeuCau nguCanh, Clock clock, JsonMapper json,
			PlatformTransactionManager transactionManager) {
		this.nhatKyGhi = nhatKyGhi;
		this.nguCanh = nguCanh;
		this.clock = clock;
		this.json = json;
		this.transactionMoi = new TransactionTemplate(transactionManager);
		this.transactionMoi.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void ghi(SuKienNhatKy suKien) {
		String requestId = null;
		try {
			requestId = this.nguCanh.requestId();
			Instant luc = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
			String diaChiIp = this.nguCanh.diaChiIp();
			String chiTiet = chiTietJson(suKien.chiTiet());
			DongNhatKy dong = new DongNhatKy(suKien.maNguoiThucHien(), suKien.loai().name(),
					suKien.doiTuong().toString(), luc, chiTiet, diaChiIp, requestId);
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				String id = requestId;
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						them(suKien, dong, id);
					}
				});
			}
			else {
				them(suKien, dong, requestId);
			}
		}
		catch (KhoaNhayCamException ex) {
			log.error(
					"Bỏ dòng nhật ký có khóa nhạy cảm: khoa={}, loaiThaoTac={}, doiTuongTacDong={}, "
							+ "maNguoiThucHien={}, requestId={}",
					ex.khoa, loai(suKien), doiTuong(suKien), nguoiThucHien(suKien), requestId);
		}
		catch (RuntimeException ex) {
			logLoi(suKien, requestId, ex);
		}
	}

	private void them(SuKienNhatKy suKien, DongNhatKy dong, String requestId) {
		try {
			this.transactionMoi.executeWithoutResult(status -> this.nhatKyGhi.them(dong));
		}
		catch (RuntimeException ex) {
			logLoi(suKien, requestId, ex);
		}
	}

	/** Chi tiết thành JSON object; khóa nhạy cảm ở bất kỳ độ sâu nào thì không ghi. */
	private String chiTietJson(ChiTietNhatKy chiTiet) {
		if (chiTiet == null) {
			return null;
		}
		JsonNode cay = this.json.valueToTree(chiTiet);
		if (!cay.isObject()) {
			throw new IllegalArgumentException("Chi tiết nhật ký phải là JSON object");
		}
		kiemKhoa(cay);
		return this.json.writeValueAsString(cay);
	}

	private static void kiemKhoa(JsonNode nut) {
		if (nut.isObject()) {
			for (Map.Entry<String, JsonNode> muc : nut.properties()) {
				if (TenKhoaNhayCam.nhayCam(muc.getKey())) {
					throw new KhoaNhayCamException(muc.getKey());
				}
				kiemKhoa(muc.getValue());
			}
		}
		else if (nut.isArray()) {
			for (JsonNode con : nut) {
				kiemKhoa(con);
			}
		}
	}

	// Không log message hay stack trace: lỗi CHECK của PostgreSQL chép cả dòng bị từ chối
	private static void logLoi(SuKienNhatKy suKien, String requestId, RuntimeException ex) {
		log.error("Ghi nhật ký thất bại: loaiThaoTac={}, doiTuongTacDong={}, maNguoiThucHien={}, requestId={}, loi={}",
				loai(suKien), doiTuong(suKien), nguoiThucHien(suKien), requestId, ex.getClass().getName());
	}

	private static Object loai(SuKienNhatKy suKien) {
		return (suKien != null) ? suKien.loai() : null;
	}

	private static Object doiTuong(SuKienNhatKy suKien) {
		return (suKien != null) ? suKien.doiTuong() : null;
	}

	private static String nguoiThucHien(SuKienNhatKy suKien) {
		return (suKien != null) ? suKien.maNguoiThucHien() : null;
	}

	private static final class KhoaNhayCamException extends RuntimeException {

		private final String khoa;

		KhoaNhayCamException(String khoa) {
			super(null, null, false, false);
			this.khoa = khoa;
		}

	}

}
