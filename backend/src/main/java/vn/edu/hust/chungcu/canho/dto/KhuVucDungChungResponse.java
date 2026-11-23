package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Một khu vực dùng chung kèm tòa chứa nó (spec 0011, AC-11). */
public record KhuVucDungChungResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maKhuVuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String tenKhuVuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String viTri,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String tenToaNha) {
}
