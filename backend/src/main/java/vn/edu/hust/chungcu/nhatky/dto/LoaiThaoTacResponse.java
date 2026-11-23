package vn.edu.hust.chungcu.nhatky.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Một mục của danh mục loại thao tác (spec 0007, AC-14). */
public record LoaiThaoTacResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String ma,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nhan) {

	public static LoaiThaoTacResponse tu(LoaiThaoTac loai) {
		return new LoaiThaoTacResponse(loai.name(), loai.nhan());
	}

}
