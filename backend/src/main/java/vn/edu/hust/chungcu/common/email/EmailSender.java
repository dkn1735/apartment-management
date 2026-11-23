package vn.edu.hust.chungcu.common.email;

/**
 * Gửi một email (spec 0010). Có interface để unit test thay bằng bản giả ghi vào bộ nhớ.
 * Gọi đồng bộ: nơi gọi tự quyết định chạy trên luồng nào.
 */
public interface EmailSender {

	/** Ném {@link RuntimeException} khi máy chủ SMTP từ chối hoặc không kết nối được. */
	void gui(EmailGui email);

}
