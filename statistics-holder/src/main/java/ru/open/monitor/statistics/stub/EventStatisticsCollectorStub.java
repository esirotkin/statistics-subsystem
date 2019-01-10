package ru.open.monitor.statistics.stub;

import java.util.concurrent.TimeUnit;

import ru.open.monitor.statistics.event.StatisticsCollector;

public class EventStatisticsCollectorStub implements StatisticsCollector {

    @Override public void eventProcessed(final String eventClass, final String handlerClass, final long duration, final TimeUnit timeUnit) {}

    @Override public void eventProcessed(String eventClass, String handlerClass, long durationNanos) {}

    @Override public void eventPublished(String eventClass, String publisherClass) {}

}
