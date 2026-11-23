package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.YeuCauDatLaiRequest;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository.TaiKhoanNhanEmail;

/**
 * Yêu cầu link đặt lại mật khẩu qua email (spec 0010, Luồng yêu cầu). Không trả gì: nơi
 * gọi luôn trả cùng một câu, dù email có tài khoản hay không (AC-2). Email gửi sau commit
 * trên luồng nền, nên thời gian phản hồi không phụ thuộc SMTP (AC-3). Log không chứa
 * email người dùng gõ.
 */
@Service
public class QuenMatKhauService {

	/** Khoảng cách tối thiểu giữa hai yêu cầu của một tài khoản (AC-4). */
	static final Duration KHOANG_CACH = Duration.ofSeconds(60);

	/** Số yêu cầu tối đa của một tài khoản trong {@link #CUA_SO} (AC-4). */
	static final int TOI_DA_MOI_GIO = 5;

	static final Duration CUA_SO = Duration.ofMinutes(60);

	/** Trần chung của cả hệ thống trong {@link #CUA_SO}, giữ hạn mức gửi của Gmail. */
	static final int TRAN_CHUNG_MOI_GIO = 100;

	public static final String PHAN_HOI_CHUNG = "Nếu email này thuộc một tài khoản đang hoạt động, link đặt lại mật khẩu sẽ được gửi tới trong vài phút.";

	private static final Logger log = LoggerFactory.getLogger(QuenMatKhauService.class);

	private final QuenMatKhauRepository repository;

	private final GhiYeuCauDatLaiService ghi;

	private final Clock clock;

	public QuenMatKhauService(QuenMatKhauRepository repository, GhiYeuCauDatLaiService ghi, Clock clock) {
		this.repository = repository;
		this.ghi = ghi;
		this.clock = clock;
	}

	public void yeuCau(YeuCauDatLaiRequest request) {
		// PostgreSQL lưu tới micro giây; cắt trước để het_han_luc = tao_luc + 30 phút
		// đúng
		// từng micro giây
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		TaiKhoanNhanEmail tk = this.repository.timTheoEmail(request.email()).orElse(null);
		KetQuaYeuCauDatLai ketQua;
		if (tk == null) {
			ketQua = KetQuaYeuCauDatLai.KHONG_CO_TAI_KHOAN;
		}
		else if (!TrangThaiTaiKhoan.HOAT_DONG.equals(tk.trangThai())) {
			ketQua = KetQuaYeuCauDatLai.KHONG_HOAT_DONG;
		}
		else {
			ketQua = this.ghi.ghi(tk, now);
		}
		String maNguoiDung = (tk != null) ? tk.maNguoiDung() : null;
		switch (ketQua) {
			case DA_TAO -> log.info("Đã tạo yêu cầu đặt lại mật khẩu: maNguoiDung={}", maNguoiDung);
			case GIOI_HAN_CHUNG ->
				log.warn("Không tạo yêu cầu đặt lại mật khẩu: lyDo=GIOI_HAN_CHUNG, maNguoiDung={}", maNguoiDung);
			default -> log.info("Không tạo yêu cầu đặt lại mật khẩu: lyDo={}, maNguoiDung={}", ketQua, maNguoiDung);
		}
	}

}
