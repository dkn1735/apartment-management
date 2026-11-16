package vn.edu.hust.chungcu;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Đồng hồ chỉnh được cho test: vừa cố định giờ (AC-6) vừa tua giờ (token hết hạn, AC-7).
 * Dùng qua {@link MutableClockConfiguration}.
 */
public final class MutableClock extends Clock {

	public static final Instant MAC_DINH = Instant.parse("2026-11-16T01:00:00Z");

	private final ZoneId zone;

	private volatile Instant instant;

	public MutableClock(Instant instant, ZoneId zone) {
		this.instant = instant;
		this.zone = zone;
	}

	public void set(Instant value) {
		this.instant = value;
	}

	public void reset() {
		this.instant = MAC_DINH;
	}

	public void advance(Duration duration) {
		this.instant = this.instant.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return this.zone;
	}

	@Override
	public Clock withZone(ZoneId value) {
		return new MutableClock(this.instant, value);
	}

	@Override
	public Instant instant() {
		return this.instant;
	}

}
