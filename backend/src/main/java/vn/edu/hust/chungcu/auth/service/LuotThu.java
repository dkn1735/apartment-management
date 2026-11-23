package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;

/** Kết quả giữ lượt của một lần đăng nhập (spec 0009, mục Luồng đăng nhập mới). */
public sealed interface LuotThu {

	/** Đang khóa tới {@code khoaDen}: trả 429, không so mật khẩu, không ghi gì. */
	record DangKhoa(Instant khoaDen) implements LuotThu {
	}

	/** Lượt thứ N vừa đặt khóa tới {@code khoaDen}; vẫn so mật khẩu. */
	record LuotCuoi(Instant khoaDen) implements LuotThu {
	}

	/** Lượt thường, được so mật khẩu. */
	record LuotThuong() implements LuotThu {
	}

}
