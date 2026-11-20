package vn.edu.hust.chungcu.auth.phanquyen;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Handler không cần đăng nhập (spec 0006, AC-1). Annotation chỉ khai báo; đường dẫn phải
 * có trong {@code PublicPaths.CONG_KHAI} thì Spring Security mới cho qua, và test
 * {@code PhanQuyenCoverageTest} kiểm hai bên khớp nhau. Chỉ có tác dụng ở mức method.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CongKhai {

}
