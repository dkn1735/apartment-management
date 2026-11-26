package vn.edu.hust.chungcu.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

/** Đọc tên ràng buộc và SQLSTATE từ chuỗi cause (spec 0011, AC-3). */
class LoiCoSoDuLieuTest {

	private static PSQLException psql(String sqlState, String rangBuoc) {
		return new PSQLException(
				new ServerErrorMessage("SERROR\0C" + sqlState + "\0Mduplicate key\0n" + rangBuoc + "\0"));
	}

	// covers: AC-3
	@Test
	void timPsqlExceptionSauNhieuTangCause() {
		DataIntegrityViolationException ex = new DataIntegrityViolationException("insert",
				new RuntimeException("wrap", psql(LoiCoSoDuLieu.TRUNG_KHOA, "uq_toa_nha_ten_lower")));
		assertThat(LoiCoSoDuLieu.tenRangBuoc(ex)).contains("uq_toa_nha_ten_lower");
		assertThat(LoiCoSoDuLieu.sqlState(ex)).contains("23505");
	}

	// covers: AC-3 (không đọc được tên thì service ném lại lỗi gốc)
	@Test
	void khongCoPsqlExceptionThiRong() {
		DataIntegrityViolationException ex = new DataIntegrityViolationException("insert",
				new SQLException("khác", "23505"));
		assertThat(LoiCoSoDuLieu.tenRangBuoc(ex)).isEmpty();
		assertThat(LoiCoSoDuLieu.sqlState(ex)).isEmpty();
		assertThat(LoiCoSoDuLieu.tenRangBuoc(null)).isEmpty();
	}

	// covers: AC-3
	@Test
	void psqlKhongCoTenRangBuocThiRong() {
		PSQLException ex = new PSQLException(new ServerErrorMessage("SERROR\0C23505\0Mduplicate key\0"));
		assertThat(LoiCoSoDuLieu.tenRangBuoc(ex)).isEmpty();
		assertThat(LoiCoSoDuLieu.sqlState(ex)).contains("23505");
	}

}
