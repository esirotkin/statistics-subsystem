package ru.open.monitor.statistics.queue;

public interface QueueStatisticsProvider {

    boolean isQueueStatisticsPresent();

    Long getQueuedEventCount();

    Long getProcessedEventCount();

}
