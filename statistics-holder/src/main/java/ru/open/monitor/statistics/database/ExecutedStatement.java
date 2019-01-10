package ru.open.monitor.statistics.database;

import static ru.open.monitor.statistics.item.ItemUtil.toMicros;

import java.util.Objects;

import ru.open.monitor.statistics.item.AppKind;
import ru.open.monitor.statistics.item.ItemKey;

public class ExecutedStatement implements Comparable<ExecutedStatement>  {

    public static class Key implements ItemKey<Key> {
        private final String statement;

        public Key(final String statement) {
            this.statement = statement;
        }

        @Override
        public AppKind getApplicationKind() {
            return AppKind.EXECUTED_STATEMENTS;
        }

        public String getStatement() {
            return statement;
        }

        @Override
        public String getItemName() {
            return "statement";
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
            return Objects.equals(statement, key.statement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statement);
        }

        @Override
        public int compareTo(Key anotherKey) {
            return statement.compareTo(anotherKey.statement);
        }

        @Override
        public String toString() {
            return statement;
        }
    };

    private final Key key;
    private final long executeCount;
    private final long minDurationNanos;
    private final long maxDurationNanos;
    private final long avgDurationNanos;
    private final long lastDurationNanos;

    ExecutedStatement(Key key, long durationNanos) {
        this(key, 1L, durationNanos, durationNanos, durationNanos, durationNanos);
    }

    private ExecutedStatement(Key key, long executeCount, long minDurationNanos, long maxDurationNanos, long avgDurationNanos, long lastDurationNanos) {
        this.key = key;
        this.executeCount = executeCount;
        this.minDurationNanos = minDurationNanos;
        this.maxDurationNanos = maxDurationNanos;
        this.avgDurationNanos = avgDurationNanos;
        this.lastDurationNanos = lastDurationNanos;
    }

    ExecutedStatement update(final long durationNanos) {
        return new ExecutedStatement(key,
                                     executeCount + 1,
                                     Math.min(minDurationNanos, durationNanos),
                                     Math.max(maxDurationNanos, durationNanos),
                                     (avgDurationNanos * executeCount + durationNanos) / (executeCount + 1),
                                     durationNanos);
    }

    public Key getKey() {
        return key;
    }

    public String getStatement() {
        return key.getStatement();
    }

    public long getExecuteCount() {
        return executeCount;
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
    public int compareTo(ExecutedStatement anotherEvent) {
        return key.compareTo(anotherEvent.key);
    }

    @Override
    public String toString() {
        return "ExecutedStatement [key=" + key + ", executeCount=" + executeCount + ", minDuration=" + toMicros(minDurationNanos) +
               ", maxDuration=" + toMicros(maxDurationNanos) + ", avgDuration=" + toMicros(avgDurationNanos) +
               ", lastDuration=" + toMicros(lastDurationNanos) + "]";
    }

}
