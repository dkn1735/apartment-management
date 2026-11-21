package vn.edu.hust.chungcu.auth.service;

import java.util.Optional;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

/**
 * Đọc tên ràng buộc và SQLSTATE từ lỗi PostgreSQL trong chuỗi cause, để service đổi lỗi
 * UNIQUE hay khóa ngoại thành {@code ApiException} có nghĩa (spec 0008, AC-3, AC-9).
 */
final class LoiCoSoDuLieu {

	/** Vi phạm khóa ngoại. */
	static final String KHOA_NGOAI = "23503";

	/** Vi phạm khóa ngoại {@code ON DELETE RESTRICT}. */
	static final String RANG_BUOC_RESTRICT = "23001";

	/** Vi phạm UNIQUE. */
	static final String TRUNG_KHOA = "23505";

	private LoiCoSoDuLieu() {
	}

	static Optional<String> tenRangBuoc(Throwable ex) {
		return psql(ex).map(PSQLException::getServerErrorMessage).map(ServerErrorMessage::getConstraint);
	}

	static Optional<String> sqlState(Throwable ex) {
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
