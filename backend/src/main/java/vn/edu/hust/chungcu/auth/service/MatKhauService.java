package vn.edu.hust.chungcu.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * So khớp mật khẩu sao cho mọi nhánh thất bại tốn đúng một lần bcrypt (spec 0004, AC-3,
 * AC-5): tài khoản không tồn tại và mật khẩu quá 72 byte so với một hash giả cùng cost,
 * để thời gian phản hồi không lộ tài khoản có tồn tại hay không.
 */
@Service
public class MatKhauService {

	/** Giới hạn của bcrypt; dài hơn thì bcrypt bỏ phần đuôi hoặc ném lỗi. */
	static final int BCRYPT_TOI_DA_BYTE = 72;

	private static final String THAY_THE = "mat-khau-qua-dai";

	private final PasswordEncoder encoder;

	private final String hashGia;

	public MatKhauService(PasswordEncoder encoder) {
		this.encoder = encoder;
		// Tạo một lần lúc khởi động, cùng encoder nên cùng cost với hash thật
		this.hashGia = encoder.encode(UUID.randomUUID().toString());
	}

	public boolean khop(String matKhau, String hash) {
		if (quaDai(matKhau)) {
			soKhopGia(matKhau);
			return false;
		}
		return this.encoder.matches(matKhau, hash);
	}

	/** Một lần so khớp vô ích, cho nhánh không tìm thấy tài khoản. */
	public void soKhopGia(String matKhau) {
		this.encoder.matches(quaDai(matKhau) ? THAY_THE : matKhau, this.hashGia);
	}

	/** Hash mới cost hiện hành khi hash cũ cần nâng; chỉ gọi sau khi đã khớp. */
	public Optional<String> hashMoiNeuCan(String matKhau, String hashCu) {
		return this.encoder.upgradeEncoding(hashCu) ? Optional.of(this.encoder.encode(matKhau)) : Optional.empty();
	}

	private static boolean quaDai(String matKhau) {
		return matKhau.getBytes(StandardCharsets.UTF_8).length > BCRYPT_TOI_DA_BYTE;
	}

}
