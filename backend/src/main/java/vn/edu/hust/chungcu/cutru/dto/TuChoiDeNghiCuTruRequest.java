package vn.edu.hust.chungcu.cutru.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code POST /api/v1/residency-records/{recordId}/reject} (spec 0015, AC-5).
 */
public record TuChoiDeNghiCuTruRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
		minLength = 1, maxLength = 500) JsonNode lyDoTuChoi) {
}
