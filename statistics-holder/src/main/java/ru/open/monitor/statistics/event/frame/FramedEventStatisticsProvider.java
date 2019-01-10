package ru.open.monitor.statistics.event.frame;

import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.PublishedEvent;
import ru.open.monitor.statistics.event.StatisticsProvider;
import ru.open.monitor.statistics.queue.QueueStatisticsProvider;

public interface FramedEventStatisticsProvider extends StatisticsProvider, QueueStatisticsProvider {

    /**
     * @return processed per second event count
     */
    Double getQueuedEventNumber(ProcessedEvent.Key key);

    /**
     * @return processed per second event count
     */
    Double getProcessedEventNumber(ProcessedEvent.Key key);

    /**
     * @return published per second event count
     */
    Double getPublishedEventNumber(PublishedEvent.Key key);

    /**
     * @return processed per second event count (by the processing queue)
     */
    Double getProcessedEventNumber();

}
