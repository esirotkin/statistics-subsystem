package ru.open.monitor.statistics.queue;

import java.util.concurrent.atomic.AtomicLong;

public class QueueStatisticsHolder implements QueueStatisticsCollector, QueueStatisticsProvider, QueueStatisticsCleaner {

    private final AtomicLong processedEventCount = new AtomicLong(0);
    private final AtomicLong queuedEventCount = new AtomicLong(0);

    @Override
    public void eventQueued() {
        this.queuedEventCount.incrementAndGet();
    }

    @Override
    public void eventProcessed() {
        this.processedEventCount.incrementAndGet();
        this.queuedEventCount.decrementAndGet();
    }

    @Override
    public boolean isQueueStatisticsPresent() {
        return processedEventCount.get() != 0 || queuedEventCount.get() != 0;
    }

    @Override
    public Long getProcessedEventCount() {
        return processedEventCount.get();
    }

    @Override
    public Long getQueuedEventCount() {
        return queuedEventCount.get();
    }

    @Override
    public void timeFrame() {
        processedEventCount.set(0);
    }

    @Override
    public void clear() {
        processedEventCount.set(0);
        queuedEventCount.set(0);
    }

}
