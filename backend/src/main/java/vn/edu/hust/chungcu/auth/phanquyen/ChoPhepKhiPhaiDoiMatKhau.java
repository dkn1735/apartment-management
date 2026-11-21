package vn.edu.hust.chungcu.auth.phanquyen;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu handler vẫn gọi được khi người dùng đang dùng mật khẩu tạm (spec 0008, AC-13):
 * hồ sơ và đổi mật khẩu. Không phải annotation phân quyền; luôn đi cùng
 * {@link DaDangNhap}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChoPhepKhiPhaiDoiMatKhau {

}
