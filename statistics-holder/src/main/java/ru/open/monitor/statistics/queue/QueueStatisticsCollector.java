package ru.open.monitor.statistics.queue;

public interface QueueStatisticsCollector {

    void eventQueued();

    void eventProcessed();

}
