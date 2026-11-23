package vn.edu.hust.chungcu.auth.dto;

import java.time.Instant;

/** Dữ liệu trả về khi đổi mật khẩu thành công (API-AUTH-04). */
public record DoiMatKhauResponse(Instant capNhatLuc) {

}
