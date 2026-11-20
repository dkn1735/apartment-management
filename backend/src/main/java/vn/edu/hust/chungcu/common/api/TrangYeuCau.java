package vn.edu.hust.chungcu.common.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse;

/**
 * Tham số phân trang của một API danh sách (spec 0001, spec 0006 AC-9). Controller khai
 * {@code page}, {@code size}, {@code sort} là chuỗi không bắt buộc rồi gọi {@link #tu},
 * để mọi giá trị sai đều thành 400 {@code VALIDATION_FAILED} có lỗi theo từng tham số.
 */
public record TrangYeuCau(int page, int size, String sortField, Sort.Direction dir) {

	public static final int SIZE_MAC_DINH = 20;

	public static final int SIZE_TOI_DA = 100;

	private static final Pattern SO = Pattern.compile("[0-9]{1,9}");

	private static final Pattern SORT = Pattern.compile("^([A-Za-z]+),(asc|desc)$");

	/**
	 * @param choPhepSort danh sách trắng tên trường sắp xếp của endpoint
	 * @param sortMacDinh dạng {@code truong,asc}, dùng khi không gửi {@code sort}
	 */
	public static TrangYeuCau tu(String page, String size, String sort, Set<String> choPhepSort, String sortMacDinh) {
		List<ErrorResponse.FieldError> loi = new ArrayList<>();
		TrangYeuCau trang = tu(page, size, sort, choPhepSort, sortMacDinh, loi);
		if (!loi.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ", loi);
		}
		return trang;
	}

	/**
	 * Như {@link #tu(String, String, String, Set, String)} nhưng không ném: thêm lỗi vào
	 * {@code loi} và trả {@code null} khi sai, để endpoint có thêm bộ lọc gộp mọi lỗi vào
	 * một response 400.
	 */
	public static TrangYeuCau tu(String page, String size, String sort, Set<String> choPhepSort, String sortMacDinh,
			List<ErrorResponse.FieldError> loi) {
		int truoc = loi.size();
		int p = soNguyen(page, 1);
		if (p < 1) {
			loi.add(new ErrorResponse.FieldError("page", "Số trang phải từ 1 trở lên"));
		}
		int s = soNguyen(size, SIZE_MAC_DINH);
		if (s < 1 || s > SIZE_TOI_DA) {
			loi.add(new ErrorResponse.FieldError("size", "Số dòng mỗi trang phải từ 1 đến 100"));
		}
		Matcher m = SORT.matcher(rong(sort) ? sortMacDinh : sort);
		boolean sortDung = m.matches() && choPhepSort.contains(m.group(1));
		if (!sortDung) {
			loi.add(new ErrorResponse.FieldError("sort", "Không sắp xếp được theo trường này"));
		}
		if (loi.size() > truoc) {
			return null;
		}
		return new TrangYeuCau(p, s, m.group(1), "asc".equals(m.group(2)) ? Sort.Direction.ASC : Sort.Direction.DESC);
	}

	/** Số dòng bỏ qua trước trang này. */
	public long offset() {
		return (long) (this.page - 1) * this.size;
	}

	/** Giá trị không phải số nguyên không âm thì trả -1 (sai). */
	private static int soNguyen(String value, int macDinh) {
		if (rong(value)) {
			return macDinh;
		}
		return SO.matcher(value).matches() ? Integer.parseInt(value) : -1;
	}

	private static boolean rong(String value) {
		return value == null || value.isEmpty();
	}

}
