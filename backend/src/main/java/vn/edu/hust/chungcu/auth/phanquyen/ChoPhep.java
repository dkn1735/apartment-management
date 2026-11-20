package vn.edu.hust.chungcu.auth.phanquyen;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import vn.edu.hust.chungcu.auth.dto.VaiTro;

/**
 * Handler chỉ chạy khi access token có ít nhất một vai trò trong {@link #value()} (spec
 * 0006, AC-2); ngược lại 403 {@code FORBIDDEN} trước khi đọc tham số và body. Chỉ có tác
 * dụng ở mức method.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChoPhep {

	VaiTro[] value();

}
