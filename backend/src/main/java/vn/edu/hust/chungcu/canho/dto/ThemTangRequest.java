package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code POST /api/v1/buildings/{buildingId}/floors} (spec 0011, AC-4, AC-5).
 * {@code soTang} phải bằng số tầng hiện có cộng một.
 */
public record ThemTangRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class,
		minimum = "1", maximum = "99", example = "4") JsonNode soTang) {
}
