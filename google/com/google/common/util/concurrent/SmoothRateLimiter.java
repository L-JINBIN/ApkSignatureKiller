

package com.google.common.util.concurrent;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.SECONDS;

abstract class SmoothRateLimiter extends RateLimiter {


    static final class SmoothWarmingUp extends SmoothRateLimiter {
        private final long warmupPeriodMicros;

        private double thresholdPermits;
        private double coldFactor;

        SmoothWarmingUp(
                SleepingStopwatch stopwatch, long warmupPeriod, TimeUnit timeUnit, double coldFactor) {
            super(stopwatch);
            this.warmupPeriodMicros = timeUnit.toMicros(warmupPeriod);
            this.coldFactor = coldFactor;
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = maxPermits;
            double coldIntervalMicros = stableIntervalMicros * coldFactor;
            thresholdPermits = 0.5 * warmupPeriodMicros / stableIntervalMicros;
            maxPermits = thresholdPermits
                    + 2.0 * warmupPeriodMicros / (stableIntervalMicros + coldIntervalMicros);
            if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                // if we don't special-case this, we would get storedPermits == NaN, below
                storedPermits = 0.0;
            } else {
                storedPermits = (oldMaxPermits == 0.0)
                        ? maxPermits // initial state is cold
                        : storedPermits * maxPermits / oldMaxPermits;
            }
        }

        @Override
        double coolDownIntervalMicros() {
            return warmupPeriodMicros / maxPermits;
        }
    }


    static final class SmoothBursty extends SmoothRateLimiter {

        final double maxBurstSeconds;

        SmoothBursty(SleepingStopwatch stopwatch, double maxBurstSeconds) {
            super(stopwatch);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            maxPermits = maxBurstSeconds * permitsPerSecond;
            if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                // if we don't special-case this, we would get storedPermits == NaN, below
                storedPermits = maxPermits;
            } else {
                storedPermits = (oldMaxPermits == 0.0)
                        ? 0.0 // initial state
                        : storedPermits * maxPermits / oldMaxPermits;
            }
        }

        @Override
        double coolDownIntervalMicros() {
            return stableIntervalMicros;
        }
    }


    double storedPermits;


    double maxPermits;


    double stableIntervalMicros;


    private long nextFreeTicketMicros = 0L; // could be either in the past or future

    private SmoothRateLimiter(SleepingStopwatch stopwatch) {
        super(stopwatch);
    }

    @Override
    final void doSetRate(double permitsPerSecond, long nowMicros) {
        resync(nowMicros);
        double stableIntervalMicros = SECONDS.toMicros(1L) / permitsPerSecond;
        this.stableIntervalMicros = stableIntervalMicros;
        doSetRate(permitsPerSecond, stableIntervalMicros);
    }

    abstract void doSetRate(double permitsPerSecond, double stableIntervalMicros);

    @Override
    final double doGetRate() {
        return SECONDS.toMicros(1L) / stableIntervalMicros;
    }


    abstract double coolDownIntervalMicros();


    void resync(long nowMicros) {
        // if nextFreeTicket is in the past, resync to now
        if (nowMicros > nextFreeTicketMicros) {
            storedPermits = min(maxPermits,
                    storedPermits
                            + (nowMicros - nextFreeTicketMicros) / coolDownIntervalMicros());
            nextFreeTicketMicros = nowMicros;
        }
    }
}
