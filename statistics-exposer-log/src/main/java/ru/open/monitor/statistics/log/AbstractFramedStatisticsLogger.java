package ru.open.monitor.statistics.log;

import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;
import static ru.open.monitor.statistics.item.ItemUtil.toMicros;
import static ru.open.monitor.statistics.item.ItemUtil.toMillis;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.PublishedEvent;
import ru.open.monitor.statistics.event.StatisticsConsumer;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;

public abstract class AbstractFramedStatisticsLogger<P extends FramedEventStatisticsProvider> extends AbstractStatisticsLogger<P> implements StatisticsConsumer<P> {

    @Value("${statistics.monitor.log.framedLoggerLevel:DEBUG}")
    private LoggerLevel loggerLevel;

    @Override
    public void consumeStatistics(final P statisticsProvider) {
        logStatistics(statisticsProvider);
    }

    private void logStatistics(final P statisticsProvider) {
        final Logger log = getLog();
        switch (loggerLevel) {
            case ERROR: if (log.isErrorEnabled()) { log.error(prepareLog(statisticsProvider)); }; break;
            case WARN:  if (log.isWarnEnabled())  { log.warn(prepareLog(statisticsProvider));  }; break;
            case INFO:  if (log.isInfoEnabled())  { log.info(prepareLog(statisticsProvider));  }; break;
            case DEBUG: if (log.isDebugEnabled()) { log.debug(prepareLog(statisticsProvider)); }; break;
            case TRACE: if (log.isTraceEnabled()) { log.trace(prepareLog(statisticsProvider)); }; break;
        }
    }

    @Override
    protected abstract String prepareDescription(final P statisticsProvider);

    @Override
    protected void appendQueuedEventsHeader(StringBuilder log) {
        log.append("┌─── QueuedEvent Statistics ───────────────┬──────────────────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐").append(LS);
        log.append("│ Event Class                              │ Queue Implementation Class               │ Count        │ Min (µs)     │ Avg (µs)     │ Max (µs)     │ Last (µs)    │ Summary (ms) │").append(LS);
        log.append("├──────────────────────────────────────────┼──────────────────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤").append(LS);
    }

    @Override
    protected String prepareQueuedEventRecord(final FramedEventStatisticsProvider statisticsProvider, final ProcessedEvent.Key key) {
        final ProcessedEvent statistics = statisticsProvider.getQueuedEventStatistics(key);
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

    @Override
    protected void appendQueuedEventsFooter(StringBuilder log) {
        appendProcessedEventsFooter(log);
    }

    @Override
    protected void appendProcessedEventsHeader(final StringBuilder log) {
        log.append("┌─── ProcessedEvent Statistics ────────────┬──────────────────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐").append(LS);
        log.append("│ Event Class                              │ Handler Class                            │ Count        │ Min (µs)     │ Avg (µs)     │ Max (µs)     │ Last (µs)    │ Summary (ms) │").append(LS);
        log.append("├──────────────────────────────────────────┼──────────────────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤").append(LS);
    }

    @Override
    protected String prepareProcessedEventRecord(final FramedEventStatisticsProvider statisticsProvider, final ProcessedEvent.Key key) {
        final ProcessedEvent statistics = statisticsProvider.getProcessedEventStatistics(key);
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

    @Override
    protected void appendProcessedEventsFooter(final StringBuilder log) {
        log.append("└──────────────────────────────────────────┴──────────────────────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘").append(LS);
    }

    @Override
    protected void appendPublishedEventsHeader(final StringBuilder log) {
        log.append("┌─── PublishedEvent Statistics ────────────┬──────────────────────────────────────────┬──────────────┬──────────────┐").append(LS);
        log.append("│ Event Class                              │ Publisher Class                          │ Count        │ Count per 1s │").append(LS);
        log.append("├──────────────────────────────────────────┼──────────────────────────────────────────┼──────────────┼──────────────┤").append(LS);
    }

    @Override
    protected String preparePublishedEventRecord(final FramedEventStatisticsProvider statisticsProvider, final PublishedEvent.Key key) {
        final PublishedEvent statistics = statisticsProvider.getPublishedEventStatistics(key);
        return String.format("│ %1$-40s │ %2$-40s │ %3$12d │ %4$12.3f │",
                             truncateToColumn(getSimpleClassName(statistics.getEventClass())),
                             truncateToColumn(getSimpleClassName(statistics.getPublisherClass())),
                             statistics.getEventCount(),
                             statisticsProvider.getPublishedEventNumber(key));
    }

    @Override
    protected void appendPublishedEventsFooter(final StringBuilder log) {
        log.append("└──────────────────────────────────────────┴──────────────────────────────────────────┴──────────────┴──────────────┘").append(LS);
    }

    @Override
    protected void appendLog(final FramedEventStatisticsProvider statisticsProvider, StringBuilder log) {
        if (statisticsProvider.isQueueStatisticsPresent()) {
            appendQueueStatisticsHeader(log);
            log.append(prepareQueueStatisticsRecord(statisticsProvider)).append(LS);
            appendQueueStatisticsFooter(log);
        }
    }

    private void appendQueueStatisticsHeader(final StringBuilder log) {
        log.append("┌─── Queue Statistics ──┬───────────────────────┬───────────────────────┐").append(LS);
        log.append("│ Queued Event Count    │ Processed Event Count │ Processed per 1s      │").append(LS);
        log.append("├───────────────────────┼───────────────────────┼───────────────────────┤").append(LS);
    }

    protected String prepareQueueStatisticsRecord(final FramedEventStatisticsProvider statisticsProvider) {
        return String.format("│ %1$21d │ %2$21d │ %3$21.3f │",
                             statisticsProvider.getQueuedEventCount(),
                             statisticsProvider.getProcessedEventCount(),
                             statisticsProvider.getProcessedEventNumber());
    }

    private void appendQueueStatisticsFooter(final StringBuilder log) {
        log.append("└───────────────────────┴───────────────────────┴───────────────────────┘").append(LS);
    }

}
