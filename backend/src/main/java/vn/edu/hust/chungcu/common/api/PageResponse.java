package vn.edu.hust.chungcu.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Dạng {@code data} của mọi API danh sách. {@code page} bắt đầu từ 1.
 */
public record PageResponse<T>(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> items,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalItems,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

	public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
		return from(page.map(mapper));
	}

	/** Cho truy vấn không đi qua Spring Data (spec 0006). */
	public static <T> PageResponse<T> of(List<T> items, TrangYeuCau trang, long totalItems) {
		int totalPages = (int) ((totalItems + trang.size() - 1) / trang.size());
		return new PageResponse<>(List.copyOf(items), trang.page(), trang.size(), totalItems, totalPages);
	}

}
