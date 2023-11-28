package io.datareplication.util;

import lombok.Getter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Getter
public class SettableClock extends Clock {
    private final Instant initialTime;
    private final Duration defaultTick;
    private Instant now;

    public SettableClock(final Instant initialTime, final Duration defaultTick) {
        this.initialTime = initialTime;
        this.defaultTick = defaultTick;
        now = initialTime;
    }

    public SettableClock(final Instant initialTime) {
        this(initialTime, Duration.ofMillis(1));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(final ZoneId zone) {
        throw new UnsupportedOperationException("can't change the timezone on the test clock");
    }

    @Override
    public Instant instant() {
        return now;
    }

    public void tick() {
        tick(defaultTick);
    }

    public void tick(Duration tick) {
        now = now.plus(tick);
    }

    public void setTo(Instant when) {
        now = when;
    }

    public void reset() {
        now = initialTime;
    }
}
