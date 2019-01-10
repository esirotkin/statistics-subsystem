package ru.open.monitor.statistics.database;

import static ru.open.monitor.statistics.item.ItemUtil.toMicros;

import java.util.Objects;

import ru.open.monitor.statistics.item.AppKind;
import ru.open.monitor.statistics.item.ItemKey;

public class ProcessedResult implements Comparable<ProcessedResult> {

    public static class Key implements ItemKey<Key> {
        private final String statement;
        private final String resultSet;

        public Key(final String statement, final int resultSetNumber) {
            this.statement = statement;
            this.resultSet = Integer.toString(resultSetNumber);
        }

        public Key(final String statement, final String resultSet) {
            this.statement = statement;
            this.resultSet = resultSet;
        }

        @Override
        public AppKind getApplicationKind() {
            return AppKind.PROCESSED_RESULTS;
        }

        public String getStatement() {
            return statement;
        }

        public String getResultSet() {
            return resultSet;
        }

        @Override
        public String getItemName() {
            return getResultSet();
        }

        @Override
        public String getServiceName() {
            return getStatement();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Key key = (Key) o;
            return Objects.equals(statement, key.statement) &&
                   Objects.equals(resultSet, key.resultSet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statement, resultSet);
        }

        @Override
        public int compareTo(Key anotherKey) {
            int procCompare = statement.compareTo(anotherKey.statement);
            return procCompare != 0 ? resultSet.compareTo(anotherKey.resultSet) : procCompare;
        }

        @Override
        public String toString() {
            return statement + "." + resultSet;
        }
    };

    private final Key key;
    private final long callCount;
    private final long minDurationNanos;
    private final long maxDurationNanos;
    private final long avgDurationNanos;
    private final long lastDurationNanos;

    ProcessedResult(Key key, long durationNanos) {
        this(key, 1L, durationNanos, durationNanos, durationNanos, durationNanos);
    }

    private ProcessedResult(Key key, long eventCount, long minDurationNanos, long maxDurationNanos, long avgDurationNanos, long lastDurationNanos) {
        this.key = key;
        this.callCount = eventCount;
        this.minDurationNanos = minDurationNanos;
        this.maxDurationNanos = maxDurationNanos;
        this.avgDurationNanos = avgDurationNanos;
        this.lastDurationNanos = lastDurationNanos;
    }

    ProcessedResult update(final long durationNanos) {
        return new ProcessedResult(key,
                                   callCount + 1,
                                   Math.min(minDurationNanos, durationNanos),
                                   Math.max(maxDurationNanos, durationNanos),
                                   (avgDurationNanos * callCount + durationNanos) / (callCount + 1),
                                   durationNanos);
    }

    public Key getKey() {
        return key;
    }

    public String getStatement() {
        return key.getStatement();
    }

    public String getResultSet() {
        return key.getResultSet();
    }

    public long getCallCount() {
        return callCount;
    }

    public long getMinDurationNanos() {
        return minDurationNanos;
    }

    public long getMaxDurationNanos() {
        return maxDurationNanos;
    }

    public long getAvgDurationNanos() {
        return avgDurationNanos;
    }

    public long getLastDurationNanos() {
        return lastDurationNanos;
    }

    @Override
    public int compareTo(ProcessedResult anotherEvent) {
        return key.compareTo(anotherEvent.key);
    }

    @Override
    public String toString() {
        return "ProcessedResult [key=" + key + ", callCount=" + callCount + ", minDuration=" + toMicros(minDurationNanos) +
               ", maxDuration=" + toMicros(maxDurationNanos) + ", avgDuration=" + toMicros(avgDurationNanos) +
               ", lastDuration=" + toMicros(lastDurationNanos) + "]";
    }

}
