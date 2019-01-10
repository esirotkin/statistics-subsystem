package ru.open.monitor.statistics.log;

import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;
import static ru.open.monitor.statistics.item.ItemUtil.toMicros;
import static ru.open.monitor.statistics.item.ItemUtil.toMillis;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.PublishedEvent;
import ru.open.monitor.statistics.event.StatisticsProvider;
import ru.open.monitor.statistics.item.ItemKey;

public abstract class AbstractStatisticsLogger<P extends StatisticsProvider> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractStatisticsLogger.class);

    protected static final String LS = System.getProperty("line.separator");

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("#{'${statistics.monitor.log.interestProcessedEvents:}'.split(',')}")
    private List<String> interestProcessedEvents;
    @Value("#{'${statistics.monitor.log.interestPublishedEvents:}'.split(',')}")
    private List<String> interestPublishedEvents;

    @PostConstruct
    public void init() {
        clearInterestEvents();
    }

    private void clearInterestEvents() {
        final List<Integer> emptyValueIndexes = new ArrayList<>();

        for (int valueIndex = 0; valueIndex < interestProcessedEvents.size(); valueIndex++) {
            if (!StringUtils.hasText(interestProcessedEvents.get(valueIndex))) {
                emptyValueIndexes.add(valueIndex);
            }
        }
        for (int emptyValueIndex : emptyValueIndexes) {
            interestProcessedEvents.remove(emptyValueIndex);
        }
        emptyValueIndexes.clear();
        LOG.debug("Interest PROCESSED events: {}", interestProcessedEvents);

        for (int valueIndex = 0; valueIndex < interestPublishedEvents.size(); valueIndex++) {
            if (!StringUtils.hasText(interestPublishedEvents.get(valueIndex))) {
                emptyValueIndexes.add(valueIndex);
            }
        }
        for (int emptyValueIndex : emptyValueIndexes) {
            interestPublishedEvents.remove(emptyValueIndex);
        }
        emptyValueIndexes.clear();
        LOG.debug("Interest PUBLISHED events: {}", interestPublishedEvents);
    }

    protected boolean isInterestProcessedEvent(final String eventClass) {
        if (!interestProcessedEvents.isEmpty()) {
            return interestProcessedEvents.contains(eventClass);
        } else {
            return true;
        }
    }

    protected boolean isInterestPublishedEvent(final String eventClass) {
        if (!interestPublishedEvents.isEmpty()) {
            return interestPublishedEvents.contains(eventClass);
        } else {
            return true;
        }
    }

    protected Logger getLog() {
        return log;
    }

    protected abstract String prepareDescription(final P statisticsProvider);

    protected String prepareLog(final P statisticsProvider) {
        if (statisticsProvider.getProcessedEventKeys().isEmpty() && statisticsProvider.getPublishedEventKeys().isEmpty()) {
            return "There is no statistics data!";
        }

        final StringBuilder log = new StringBuilder();
        log.append(prepareDescription(statisticsProvider)).append(LS);

        if (!statisticsProvider.getQueuedEventKeys().isEmpty()) {
            appendQueuedEventsHeader(log);
            final SortedMap<String, ProcessedEvent.Key> queuedEventKeys = new TreeMap<>();
            for (final ProcessedEvent.Key key : statisticsProvider.getQueuedEventKeys()) {
                if (isInterestProcessedEvent(key.getEventClass())) {
                    queuedEventKeys.put(buildSortingKey(key), key);
                }
            }
            for (final String sortingKey : queuedEventKeys.keySet()) {
                log.append(prepareQueuedEventRecord(statisticsProvider, queuedEventKeys.get(sortingKey))).append(LS);
            }
            queuedEventKeys.clear();
            appendQueuedEventsFooter(log);
        }

        if (!statisticsProvider.getProcessedEventKeys().isEmpty()) {
            appendProcessedEventsHeader(log);
            final SortedMap<String, ProcessedEvent.Key> processedEventKeys = new TreeMap<>();
            for (final ProcessedEvent.Key key : statisticsProvider.getProcessedEventKeys()) {
                if (isInterestProcessedEvent(key.getEventClass())) {
                    processedEventKeys.put(buildSortingKey(key), key);
                }
            }
            for (final String sortingKey : processedEventKeys.keySet()) {
                log.append(prepareProcessedEventRecord(statisticsProvider, processedEventKeys.get(sortingKey))).append(LS);
            }
            processedEventKeys.clear();
            appendProcessedEventsFooter(log);
        }

        if (!statisticsProvider.getPublishedEventKeys().isEmpty()) {
            appendPublishedEventsHeader(log);
            final SortedMap<String, PublishedEvent.Key> publishedEventKeys = new TreeMap<>();
            for (final PublishedEvent.Key key : statisticsProvider.getPublishedEventKeys()) {
                if (isInterestPublishedEvent(key.getEventClass())) {
                    publishedEventKeys.put(buildSortingKey(key), key);
                }
            }
            for (final String sortingKey : publishedEventKeys.keySet()) {
                log.append(preparePublishedEventRecord(statisticsProvider, publishedEventKeys.get(sortingKey))).append(LS);
            }
            publishedEventKeys.clear();
            appendPublishedEventsFooter(log);
        }

        appendLog(statisticsProvider, log);

        return log.toString();
    }

    protected void appendLog(final P statisticsProvider, final StringBuilder log) {}

    public static String buildSortingKey(final ItemKey itemKey) {
        return getSimpleClassName(itemKey.getItemName()) + "." + getSimpleClassName(itemKey.getServiceName());
    }

    protected void appendQueuedEventsHeader(final StringBuilder log) {
        log.append("┌─── QueuedEvent Statistics ───────────────┬──────────────────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐").append(LS);
        log.append("│ Event Class                              │ Queue Implementation Class               │ Count        │ Min (µs)     │ Avg (µs)     │ Max (µs)     │ Last (µs)    │ Summary (ms) │").append(LS);
        log.append("├──────────────────────────────────────────┼──────────────────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤").append(LS);
    }

    protected String prepareQueuedEventRecord(final P statisticsProvider, final ProcessedEvent.Key key) {
        final ProcessedEvent statistics = statisticsProvider.getQueuedEventStatistics(key);
        return prepareProcessedEventRecord(statistics);
    }

    protected void appendQueuedEventsFooter(final StringBuilder log) {
        appendProcessedEventsFooter(log);
    }

    protected void appendProcessedEventsHeader(final StringBuilder log) {
        log.append("┌─── ProcessedEvent Statistics ────────────┬──────────────────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐").append(LS);
        log.append("│ Event Class                              │ Handler Class                            │ Count        │ Min (µs)     │ Avg (µs)     │ Max (µs)     │ Last (µs)    │ Summary (ms) │").append(LS);
        log.append("├──────────────────────────────────────────┼──────────────────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤").append(LS);
    }

    protected String prepareProcessedEventRecord(final P statisticsProvider, final ProcessedEvent.Key key) {
        final ProcessedEvent statistics = statisticsProvider.getProcessedEventStatistics(key);
        return prepareProcessedEventRecord(statistics);
    }

    private String prepareProcessedEventRecord(final ProcessedEvent statistics) {
        return String.format("│ %1$-40s │ %2$-40s │ %3$12d │ %4$12.3f │ %5$12.3f │ %6$12.3f │ %7$12.3f │ %8$12.3f │",
                             truncateToColumn(getSimpleClassName(statistics.getEventClass())),
                             truncateToColumn(getSimpleClassName(statistics.getHandlerClass())),
                             statistics.getEventCount(),
                             toMicros(statistics.getMinDurationNanos()),
                             toMicros(statistics.getAvgDurationNanos()),
                             toMicros(statistics.getMaxDurationNanos()),
                             toMicros(statistics.getLastDurationNanos()),
                             toMillis(statistics.getSummaryDurationNanos()));

    }

    protected void appendProcessedEventsFooter(final StringBuilder log) {
        log.append("└──────────────────────────────────────────┴──────────────────────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘").append(LS);
    }

    protected void appendPublishedEventsHeader(final StringBuilder log) {
        log.append("┌─── PublishedEvent Statistics ────────────┬──────────────────────────────────────────┬──────────────┐").append(LS);
        log.append("│ Event Class                              │ Publisher Class                          │ Count        │").append(LS);
        log.append("├──────────────────────────────────────────┼──────────────────────────────────────────┼──────────────┤").append(LS);
    }

    protected String preparePublishedEventRecord(final P statisticsProvider, final PublishedEvent.Key key) {
        final PublishedEvent statistics = statisticsProvider.getPublishedEventStatistics(key);
        return String.format("│ %1$-40s │ %2$-40s │ %3$12d │",
                             truncateToColumn(getSimpleClassName(statistics.getEventClass())),
                             truncateToColumn(getSimpleClassName(statistics.getPublisherClass())),
                             statistics.getEventCount());
    }

    protected void appendPublishedEventsFooter(final StringBuilder log) {
        log.append("└──────────────────────────────────────────┴──────────────────────────────────────────┴──────────────┘").append(LS);
    }

    protected static String truncateToColumn(final String simpleClassName) {
        return simpleClassName.length() <= 40 ? simpleClassName : simpleClassName.substring(0, 37).concat("...");
    }

}
