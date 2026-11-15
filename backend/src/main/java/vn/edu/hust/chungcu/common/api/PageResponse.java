package vn.edu.hust.chungcu.common.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Dạng {@code data} của mọi API danh sách. {@code page} bắt đầu từ 1.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

	public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
		return from(page.map(mapper));
	}

}
