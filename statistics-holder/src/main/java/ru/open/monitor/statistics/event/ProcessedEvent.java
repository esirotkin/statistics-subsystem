package ru.open.monitor.statistics.event;

import static ru.open.monitor.statistics.item.ItemUtil.getCleanClassName;
import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;
import static ru.open.monitor.statistics.item.ItemUtil.toMicros;
import static ru.open.monitor.statistics.item.ItemUtil.toMillis;

import java.util.Objects;

import ru.open.monitor.statistics.item.AppKind;
import ru.open.monitor.statistics.item.ItemKey;

public class ProcessedEvent implements Comparable<ProcessedEvent> {

    public static class Key implements ItemKey<Key> {
        private final String eventClass;
        private final String handlerClass;

        public Key(final String eventClass, final String handlerClass) {
            this.eventClass = eventClass;
            this.handlerClass = handlerClass;
        }

        @Override
        public AppKind getApplicationKind() {
            return AppKind.PROCESSED_EVENTS;
        }

        public String getEventClass() {
            return eventClass;
        }

        @Override
        public String getItemName() {
            return getEventClass();
        }

        @Override
        public String getServiceName() {
            return getHandlerClass();
        }

        public String getHandlerClass() {
            return getCleanClassName(handlerClass);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Key key = (Key) o;
            return Objects.equals(eventClass, key.eventClass) &&
                   Objects.equals(handlerClass, key.handlerClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventClass, handlerClass);
        }

        @Override
        public int compareTo(Key anotherKey) {
            return eventClass.compareTo(anotherKey.eventClass);
        }

        @Override
        public String toString() {
            return getSimpleClassName(eventClass) + "." + getSimpleClassName(handlerClass);
        }
    };

    private final Key key;
    private final long eventCount;
    private final long minDurationNanos;
    private final long maxDurationNanos;
    private final long avgDurationNanos;
    private final long lastDurationNanos;
    private final long summaryDurationNanos;

    ProcessedEvent(Key key, long durationNanos) {
        this(key, 1L, durationNanos, durationNanos, durationNanos, durationNanos, durationNanos);
    }

    private ProcessedEvent(Key key, long eventCount,
                           long minDurationNanos, long maxDurationNanos, long avgDurationNanos, long lastDurationNanos,
                           long summaryDurationNanos) {
        this.key = key;
        this.eventCount = eventCount;
        this.minDurationNanos = minDurationNanos;
        this.maxDurationNanos = maxDurationNanos;
        this.avgDurationNanos = avgDurationNanos;
        this.lastDurationNanos = lastDurationNanos;
        this.summaryDurationNanos = summaryDurationNanos;
    }

    ProcessedEvent update(final long durationNanos) {
        return new ProcessedEvent(key,
                                  eventCount + 1,
                                  Math.min(minDurationNanos, durationNanos),
                                  Math.max(maxDurationNanos, durationNanos),
                                  (avgDurationNanos * eventCount + durationNanos) / (eventCount + 1),
                                  durationNanos,
                                  summaryDurationNanos + durationNanos);
    }

    public Key getKey() {
        return key;
    }

    public String getEventClass() {
        return key.getEventClass();
    }

    public String getHandlerClass() {
        return key.getHandlerClass();
    }

    public long getEventCount() {
        return eventCount;
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

    public long getSummaryDurationNanos() {
        return summaryDurationNanos;
    }

    @Override
    public int compareTo(ProcessedEvent anotherEvent) {
        return key.compareTo(anotherEvent.key);
    }

    @Override
    public String toString() {
        return "ProcessedEvent [key=" + key + ", eventCount=" + eventCount + ", minDuration=" + toMicros(minDurationNanos) +
               ", maxDuration=" + toMicros(maxDurationNanos) + ", avgDuration=" + toMicros(avgDurationNanos) +
               ", lastDuration=" + toMicros(lastDurationNanos) + ", sumDuration=" + toMillis(summaryDurationNanos) + "]";
    }

}
