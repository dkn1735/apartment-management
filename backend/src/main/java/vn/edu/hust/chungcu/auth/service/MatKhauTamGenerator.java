package vn.edu.hust.chungcu.auth.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Sinh mật khẩu tạm khi tạo tài khoản hoặc đặt lại mật khẩu (spec 0008, AC-2): 12 ký tự
 * từ bảng chữ bỏ {@code 0 O 1 l I}, luôn có ít nhất một chữ cái và một chữ số, nên đạt
 * quy tắc độ mạnh của đổi mật khẩu.
 */
@Component
public class MatKhauTamGenerator {

	static final String BANG_CHU = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

	static final int DO_DAI = 12;

	private final SecureRandom random = new SecureRandom();

	public String sinh() {
		while (true) {
			char[] kyTu = new char[DO_DAI];
			for (int i = 0; i < DO_DAI; i++) {
				kyTu[i] = BANG_CHU.charAt(this.random.nextInt(BANG_CHU.length()));
			}
			String matKhau = new String(kyTu);
			// Xác suất thiếu chữ hoặc thiếu số rất nhỏ; sinh lại cho tới khi đủ
			if (matKhau.chars().anyMatch(Character::isLetter) && matKhau.chars().anyMatch(Character::isDigit)) {
				return matKhau;
			}
		}
	}

}
