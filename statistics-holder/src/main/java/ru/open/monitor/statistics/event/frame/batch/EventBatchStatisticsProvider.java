package ru.open.monitor.statistics.event.frame.batch;

import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;

public interface EventBatchStatisticsProvider extends FramedEventStatisticsProvider {

    Long getBatchProcessingTime();

}
