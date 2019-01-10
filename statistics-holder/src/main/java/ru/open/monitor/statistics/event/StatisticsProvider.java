package ru.open.monitor.statistics.event;

import java.util.Collection;
import java.util.Date;

public interface StatisticsProvider {

    Date getBeginningTime();

    Collection<ProcessedEvent.Key> getQueuedEventKeys();

    Collection<ProcessedEvent.Key> getQueuedEventKeys(String eventClass);

    ProcessedEvent getQueuedEventStatistics(ProcessedEvent.Key key);

    Collection<ProcessedEvent.Key> getProcessedEventKeys();

    Collection<ProcessedEvent.Key> getProcessedEventKeys(String eventClass);

    ProcessedEvent getProcessedEventStatistics(ProcessedEvent.Key key);

    Collection<PublishedEvent.Key> getPublishedEventKeys();

    Collection<PublishedEvent.Key> getPublishedEventKeys(String eventClass);

    PublishedEvent getPublishedEventStatistics(PublishedEvent.Key key);

}
