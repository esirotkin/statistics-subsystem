package ru.open.monitor.statistics.event;

import static ru.open.monitor.statistics.item.ItemUtil.getCleanClassName;
import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;

import java.util.Objects;

import ru.open.monitor.statistics.item.AppKind;
import ru.open.monitor.statistics.item.ItemKey;

public class PublishedEvent implements Comparable<PublishedEvent> {

    public static class Key implements ItemKey<Key> {
        private final String eventClass;
        private final String publisherClass;

        public Key(final String eventClass, final String publisherClass) {
            this.eventClass = eventClass;
            this.publisherClass = publisherClass;
        }

        @Override
        public AppKind getApplicationKind() {
            return AppKind.PUBLISHED_EVENTS;
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
            return getPublisherClass();
        }

        public String getPublisherClass() {
            return getCleanClassName(publisherClass);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Key key = (Key) o;
            return Objects.equals(eventClass, key.eventClass) &&
                   Objects.equals(publisherClass, key.publisherClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventClass, publisherClass);
        }

        @Override
        public int compareTo(Key anotherKey) {
            return eventClass.compareTo(anotherKey.eventClass);
        }

        @Override
        public String toString() {
            return getSimpleClassName(eventClass) + "." + getSimpleClassName(publisherClass);
        }
    };

    private final Key key;
    private final long eventCount;
    private final long lastUpdateTime;

    PublishedEvent(Key key) {
        this(key, 1L);
    }

    private PublishedEvent(Key key, long eventCount) {
        this.key = key;
        this.eventCount = eventCount;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    PublishedEvent update() {
        return new PublishedEvent(key, eventCount + 1);
    }

    public Key getKey() {
        return key;
    }

    public String getEventClass() {
        return key.getEventClass();
    }

    public String getPublisherClass() {
        return key.getPublisherClass();
    }

    public long getEventCount() {
        return eventCount;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public int compareTo(PublishedEvent anotherEvent) {
        return key.compareTo(anotherEvent.key);
    }

    @Override
    public String toString() {
        return "PublishedEvent [key=" + key + ", eventCount=" + eventCount + ", lastUpdateTime=" + lastUpdateTime + "]";
    }

}
