package vn.edu.hust.chungcu.auth.phanquyen;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Handler dành cho mọi người đã đăng nhập, không cần vai trò nào (spec 0006, AC-1). Chỉ
 * có tác dụng ở mức method.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DaDangNhap {

}
