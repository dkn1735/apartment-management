package vn.edu.hust.chungcu.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.hust.chungcu.common.config.AsyncConfig;
import vn.edu.hust.chungcu.common.email.EmailSender;

/**
 * Gửi email đặt lại mật khẩu sau khi yêu cầu đã commit, trên {@code emailExecutor} (spec
 * 0010, AC-3). Rollback thì không bao giờ gửi. Không gửi lại tự động: lỗi SMTP hay hàng
 * đợi đầy chỉ được ghi log ERROR kèm mã yêu cầu và tên lớp lỗi, không kèm token, link hay
 * địa chỉ email (thông điệp lỗi SMTP có thể chép địa chỉ người nhận nên không log).
 */
@Component
public class EmailDatLaiMatKhauListener {

	private static final Logger log = LoggerFactory.getLogger(EmailDatLaiMatKhauListener.class);

	private final EmailSender sender;

	private final EmailDatLaiMatKhau mau;

	private final TaskExecutor executor;

	public EmailDatLaiMatKhauListener(EmailSender sender, EmailDatLaiMatKhau mau,
			@Qualifier(AsyncConfig.EMAIL_EXECUTOR) TaskExecutor executor) {
		this.sender = sender;
		this.mau = mau;
		this.executor = executor;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
	public void khiTao(YeuCauDatLaiDaTao suKien) {
		try {
			this.executor.execute(() -> gui(suKien));
		}
		catch (TaskRejectedException ex) {
			log.error("Bỏ email đặt lại mật khẩu vì hàng đợi gửi đã đầy: maYeuCau={}", suKien.maYeuCau());
		}
	}

	void gui(YeuCauDatLaiDaTao suKien) {
		try {
			this.sender.gui(this.mau.dung(suKien));
			log.info("Đã gửi email đặt lại mật khẩu: maYeuCau={}", suKien.maYeuCau());
		}
		catch (RuntimeException ex) {
			log.error("Gửi email đặt lại mật khẩu thất bại: maYeuCau={}, loi={}", suKien.maYeuCau(),
					ex.getClass().getName());
		}
	}

}
