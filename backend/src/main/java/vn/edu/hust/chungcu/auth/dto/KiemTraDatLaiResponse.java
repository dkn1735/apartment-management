package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Link đặt lại còn hiệu lực tới lúc nào (spec 0010, AC-6). */
public record KiemTraDatLaiResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant hetHanLuc) {

}
