package ru.open.monitor.statistics.event;

import java.util.concurrent.TimeUnit;

public interface StatisticsCollector {

    void eventProcessed(String eventClass, String handlerClass, long duration, TimeUnit timeUnit);

    void eventProcessed(String eventClass, String handlerClass, long durationNanos);

    void eventPublished(String eventClass, String publisherClass);

}
