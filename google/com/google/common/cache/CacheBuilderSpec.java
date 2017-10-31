

package com.google.common.cache;

import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;


public final class CacheBuilderSpec {

    private interface ValueParser {
        void parse(CacheBuilderSpec spec, String key, @Nullable String value);
    }


    private static final Splitter KEYS_SPLITTER = Splitter.on(',').trimResults();


    private static final Splitter KEY_VALUE_SPLITTER = Splitter.on('=').trimResults();


    private static final ImmutableMap<String, ValueParser> VALUE_PARSERS =
            ImmutableMap.<String, ValueParser>builder()
                    .put("initialCapacity", new InitialCapacityParser())
                    .put("maximumSize", new MaximumSizeParser())
                    .put("maximumWeight", new MaximumWeightParser())
                    .put("concurrencyLevel", new ConcurrencyLevelParser())
                    .put("weakKeys", new KeyStrengthParser(Strength.WEAK))
                    .put("softValues", new ValueStrengthParser(Strength.SOFT))
                    .put("weakValues", new ValueStrengthParser(Strength.WEAK))
                    .put("recordStats", new RecordStatsParser())
                    .put("expireAfterAccess", new AccessDurationParser())
                    .put("expireAfterWrite", new WriteDurationParser())
                    .put("refreshAfterWrite", new RefreshDurationParser())
                    .put("refreshInterval", new RefreshDurationParser())
                    .build();

    @VisibleForTesting
    Integer initialCapacity;
    @VisibleForTesting
    Long maximumSize;
    @VisibleForTesting
    Long maximumWeight;
    @VisibleForTesting
    Integer concurrencyLevel;
    @VisibleForTesting
    Strength keyStrength;
    @VisibleForTesting
    Strength valueStrength;
    @VisibleForTesting
    Boolean recordStats;
    @VisibleForTesting
    long writeExpirationDuration;
    @VisibleForTesting
    TimeUnit writeExpirationTimeUnit;
    @VisibleForTesting
    long accessExpirationDuration;
    @VisibleForTesting
    TimeUnit accessExpirationTimeUnit;
    @VisibleForTesting
    long refreshDuration;
    @VisibleForTesting
    TimeUnit refreshTimeUnit;

    private final String specification;

    private CacheBuilderSpec(String specification) {
        this.specification = specification;
    }


    public static CacheBuilderSpec parse(String cacheBuilderSpecification) {
        CacheBuilderSpec spec = new CacheBuilderSpec(cacheBuilderSpecification);
        if (!cacheBuilderSpecification.isEmpty()) {
            for (String keyValuePair : KEYS_SPLITTER.split(cacheBuilderSpecification)) {
                List<String> keyAndValue = ImmutableList.copyOf(KEY_VALUE_SPLITTER.split(keyValuePair));
                checkArgument(!keyAndValue.isEmpty(), "blank key-value pair");
                checkArgument(keyAndValue.size() <= 2,
                        "key-value pair %s with more than one equals sign", keyValuePair);

                // Find the ValueParser for the current key.
                String key = keyAndValue.get(0);
                ValueParser valueParser = VALUE_PARSERS.get(key);
                checkArgument(valueParser != null, "unknown key %s", key);

                String value = keyAndValue.size() == 1 ? null : keyAndValue.get(1);
                valueParser.parse(spec, key, value);
            }
        }

        return spec;
    }


    public static CacheBuilderSpec disableCaching() {
        // Maximum size of zero is one way to block caching
        return CacheBuilderSpec.parse("maximumSize=0");
    }


    CacheBuilder<Object, Object> toCacheBuilder() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        if (initialCapacity != null) {
            builder.initialCapacity(initialCapacity);
        }
        if (maximumSize != null) {
            builder.maximumSize(maximumSize);
        }
        if (maximumWeight != null) {
            builder.maximumWeight(maximumWeight);
        }
        if (concurrencyLevel != null) {
            builder.concurrencyLevel(concurrencyLevel);
        }
        if (keyStrength != null) {
            switch (keyStrength) {
                case WEAK:
                    builder.weakKeys();
                    break;
                default:
                    throw new AssertionError();
            }
        }
        if (valueStrength != null) {
            switch (valueStrength) {
                case SOFT:
                    builder.softValues();
                    break;
                case WEAK:
                    builder.weakValues();
                    break;
                default:
                    throw new AssertionError();
            }
        }
        if (recordStats != null && recordStats) {
            builder.recordStats();
        }
        if (writeExpirationTimeUnit != null) {
            builder.expireAfterWrite(writeExpirationDuration, writeExpirationTimeUnit);
        }
        if (accessExpirationTimeUnit != null) {
            builder.expireAfterAccess(accessExpirationDuration, accessExpirationTimeUnit);
        }
        if (refreshTimeUnit != null) {
            builder.refreshAfterWrite(refreshDuration, refreshTimeUnit);
        }

        return builder;
    }


    public String toParsableString() {
        return specification;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(toParsableString()).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                initialCapacity,
                maximumSize,
                maximumWeight,
                concurrencyLevel,
                keyStrength,
                valueStrength,
                recordStats,
                durationInNanos(writeExpirationDuration, writeExpirationTimeUnit),
                durationInNanos(accessExpirationDuration, accessExpirationTimeUnit),
                durationInNanos(refreshDuration, refreshTimeUnit));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CacheBuilderSpec)) {
            return false;
        }
        CacheBuilderSpec that = (CacheBuilderSpec) obj;
        return Objects.equal(initialCapacity, that.initialCapacity)
                && Objects.equal(maximumSize, that.maximumSize)
                && Objects.equal(maximumWeight, that.maximumWeight)
                && Objects.equal(concurrencyLevel, that.concurrencyLevel)
                && Objects.equal(keyStrength, that.keyStrength)
                && Objects.equal(valueStrength, that.valueStrength)
                && Objects.equal(recordStats, that.recordStats)
                && Objects.equal(durationInNanos(writeExpirationDuration, writeExpirationTimeUnit),
                durationInNanos(that.writeExpirationDuration, that.writeExpirationTimeUnit))
                && Objects.equal(durationInNanos(accessExpirationDuration, accessExpirationTimeUnit),
                durationInNanos(that.accessExpirationDuration, that.accessExpirationTimeUnit))
                && Objects.equal(durationInNanos(refreshDuration, refreshTimeUnit),
                durationInNanos(that.refreshDuration, that.refreshTimeUnit));
    }


    @Nullable
    private static Long durationInNanos(long duration, @Nullable TimeUnit unit) {
        return (unit == null) ? null : unit.toNanos(duration);
    }


    abstract static class IntegerParser implements ValueParser {
        protected abstract void parseInteger(CacheBuilderSpec spec, int value);

        @Override
        public void parse(CacheBuilderSpec spec, String key, String value) {
            checkArgument(value != null && !value.isEmpty(), "value of key %s omitted", key);
            try {
                parseInteger(spec, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        format("key %s value set to %s, must be integer", key, value), e);
            }
        }
    }


    abstract static class LongParser implements ValueParser {
        protected abstract void parseLong(CacheBuilderSpec spec, long value);

        @Override
        public void parse(CacheBuilderSpec spec, String key, String value) {
            checkArgument(value != null && !value.isEmpty(), "value of key %s omitted", key);
            try {
                parseLong(spec, Long.parseLong(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        format("key %s value set to %s, must be integer", key, value), e);
            }
        }
    }


    static class InitialCapacityParser extends IntegerParser {
        @Override
        protected void parseInteger(CacheBuilderSpec spec, int value) {
            checkArgument(spec.initialCapacity == null,
                    "initial capacity was already set to ", spec.initialCapacity);
            spec.initialCapacity = value;
        }
    }


    static class MaximumSizeParser extends LongParser {
        @Override
        protected void parseLong(CacheBuilderSpec spec, long value) {
            checkArgument(spec.maximumSize == null,
                    "maximum size was already set to ", spec.maximumSize);
            checkArgument(spec.maximumWeight == null,
                    "maximum weight was already set to ", spec.maximumWeight);
            spec.maximumSize = value;
        }
    }


    static class MaximumWeightParser extends LongParser {
        @Override
        protected void parseLong(CacheBuilderSpec spec, long value) {
            checkArgument(spec.maximumWeight == null,
                    "maximum weight was already set to ", spec.maximumWeight);
            checkArgument(spec.maximumSize == null,
                    "maximum size was already set to ", spec.maximumSize);
            spec.maximumWeight = value;
        }
    }


    static class ConcurrencyLevelParser extends IntegerParser {
        @Override
        protected void parseInteger(CacheBuilderSpec spec, int value) {
            checkArgument(spec.concurrencyLevel == null,
                    "concurrency level was already set to ", spec.concurrencyLevel);
            spec.concurrencyLevel = value;
        }
    }


    static class KeyStrengthParser implements ValueParser {
        private final Strength strength;

        public KeyStrengthParser(Strength strength) {
            this.strength = strength;
        }

        @Override
        public void parse(CacheBuilderSpec spec, String key, @Nullable String value) {
            checkArgument(value == null, "key %s does not take values", key);
            checkArgument(spec.keyStrength == null, "%s was already set to %s", key, spec.keyStrength);
            spec.keyStrength = strength;
        }
    }


    static class ValueStrengthParser implements ValueParser {
        private final Strength strength;

        public ValueStrengthParser(Strength strength) {
            this.strength = strength;
        }

        @Override
        public void parse(CacheBuilderSpec spec, String key, @Nullable String value) {
            checkArgument(value == null, "key %s does not take values", key);
            checkArgument(spec.valueStrength == null,
                    "%s was already set to %s", key, spec.valueStrength);

            spec.valueStrength = strength;
        }
    }


    static class RecordStatsParser implements ValueParser {

        @Override
        public void parse(CacheBuilderSpec spec, String key, @Nullable String value) {
            checkArgument(value == null, "recordStats does not take values");
            checkArgument(spec.recordStats == null, "recordStats already set");
            spec.recordStats = true;
        }
    }


    abstract static class DurationParser implements ValueParser {
        protected abstract void parseDuration(
                CacheBuilderSpec spec,
                long duration,
                TimeUnit unit);

        @Override
        public void parse(CacheBuilderSpec spec, String key, String value) {
            checkArgument(value != null && !value.isEmpty(), "value of key %s omitted", key);
            try {
                char lastChar = value.charAt(value.length() - 1);
                TimeUnit timeUnit;
                switch (lastChar) {
                    case 'd':
                        timeUnit = TimeUnit.DAYS;
                        break;
                    case 'h':
                        timeUnit = TimeUnit.HOURS;
                        break;
                    case 'm':
                        timeUnit = TimeUnit.MINUTES;
                        break;
                    case 's':
                        timeUnit = TimeUnit.SECONDS;
                        break;
                    default:
                        throw new IllegalArgumentException(
                                format("key %s invalid format.  was %s, must end with one of [dDhHmMsS]",
                                        key, value));
                }

                long duration = Long.parseLong(value.substring(0, value.length() - 1));
                parseDuration(spec, duration, timeUnit);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        format("key %s value set to %s, must be integer", key, value));
            }
        }
    }


    static class AccessDurationParser extends DurationParser {
        @Override
        protected void parseDuration(CacheBuilderSpec spec, long duration, TimeUnit unit) {
            checkArgument(spec.accessExpirationTimeUnit == null, "expireAfterAccess already set");
            spec.accessExpirationDuration = duration;
            spec.accessExpirationTimeUnit = unit;
        }
    }


    static class WriteDurationParser extends DurationParser {
        @Override
        protected void parseDuration(CacheBuilderSpec spec, long duration, TimeUnit unit) {
            checkArgument(spec.writeExpirationTimeUnit == null, "expireAfterWrite already set");
            spec.writeExpirationDuration = duration;
            spec.writeExpirationTimeUnit = unit;
        }
    }


    static class RefreshDurationParser extends DurationParser {
        @Override
        protected void parseDuration(CacheBuilderSpec spec, long duration, TimeUnit unit) {
            checkArgument(spec.refreshTimeUnit == null, "refreshAfterWrite already set");
            spec.refreshDuration = duration;
            spec.refreshTimeUnit = unit;
        }
    }

    private static String format(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }
}
