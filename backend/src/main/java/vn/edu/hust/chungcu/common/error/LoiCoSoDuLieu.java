package vn.edu.hust.chungcu.common.error;

import java.util.Optional;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

/**
 * Đọc tên ràng buộc và SQLSTATE từ lỗi PostgreSQL trong chuỗi cause, để service đổi lỗi
 * UNIQUE hay khóa ngoại thành {@code ApiException} có nghĩa (spec 0008 AC-3, AC-9; spec
 * 0011 AC-3).
 */
public final class LoiCoSoDuLieu {

	/** Vi phạm khóa ngoại. */
	public static final String KHOA_NGOAI = "23503";

	/** Vi phạm khóa ngoại {@code ON DELETE RESTRICT}. */
	public static final String RANG_BUOC_RESTRICT = "23001";

	/** Vi phạm UNIQUE. */
	public static final String TRUNG_KHOA = "23505";

	/** Vi phạm ràng buộc loại trừ {@code EXCLUDE USING gist} (spec 0014, AC-9). */
	public static final String LOAI_TRU_GIST = "23P01";

	private LoiCoSoDuLieu() {
	}

	public static Optional<String> tenRangBuoc(Throwable ex) {
		return psql(ex).map(PSQLException::getServerErrorMessage).map(ServerErrorMessage::getConstraint);
	}

	public static Optional<String> sqlState(Throwable ex) {
		return psql(ex).map(PSQLException::getSQLState);
	}

	private static Optional<PSQLException> psql(Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof PSQLException p) {
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}

}
