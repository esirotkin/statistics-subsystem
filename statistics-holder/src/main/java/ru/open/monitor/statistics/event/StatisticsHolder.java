package ru.open.monitor.statistics.event;

import static ru.open.monitor.statistics.item.ItemUtil.mathRound;
import static ru.open.monitor.statistics.item.ItemUtil.toMicros;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import ru.open.monitor.statistics.event.frame.FramedEventStatisticsConsumer;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsSubscription;
import ru.open.monitor.statistics.event.frame.batch.EventBatchProcessedListener;
import ru.open.monitor.statistics.event.frame.batch.EventBatchStatisticsConsumer;
import ru.open.monitor.statistics.event.frame.batch.EventBatchStatisticsProvider;
import ru.open.monitor.statistics.event.frame.batch.EventBatchStatisticsSubscription;
import ru.open.monitor.statistics.queue.QueueStatisticsCleaner;
import ru.open.monitor.statistics.queue.QueueStatisticsProvider;

public class StatisticsHolder implements StatisticsCollector, StatisticsProvider, StatisticsCleaner, EventBatchProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsHolder.class);

    private final ConcurrentMap<ProcessedEvent.Key, ProcessedEvent> processedEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<PublishedEvent.Key, PublishedEvent> publishedEvents = new ConcurrentHashMap<>();

    private final QueueStatisticsProvider queueStatisticsProvider;
    private final QueueStatisticsCleaner queueStatisticsCleaner;

    private final FramedEventStatisticsSubscription framedStatisticsSubscription;
    private final FramedStatisticsHolder framedStatisticsHolder = new FramedStatisticsHolder();

    private final EventBatchStatisticsSubscription batchStatisticsSubscription;
    private final BatchStatisticsHolder batchStatisticsHolder = new BatchStatisticsHolder();
    private final ExecutorService batchStatisticsExecutor = Executors.newSingleThreadExecutor();

    private final Set<String> queuePreProcessors;
    private final Double timeFrameSeconds;

    private volatile Date beginningTime = new Date();

    private class FramedStatisticsHolder implements FramedEventStatisticsProvider {
        private final ConcurrentMap<ProcessedEvent.Key, ProcessedEvent> processedEvents = new ConcurrentHashMap<>();
        private final ConcurrentMap<PublishedEvent.Key, PublishedEvent> publishedEvents = new ConcurrentHashMap<>();

        private volatile Date frameBeginningTime = new Date();

        @Override
        public Date getBeginningTime() {
            return frameBeginningTime;
        }

        @Override
        public Collection<ProcessedEvent.Key> getQueuedEventKeys() {
            return processedEvents.keySet().stream().filter(key -> isQueuePreProcessor(key)).collect(Collectors.toSet());
        }

        @Override
        public Collection<ProcessedEvent.Key> getQueuedEventKeys(final String eventClass) {
            return processedEvents.keySet().stream().filter(key -> isQueuePreProcessor(key) && key.getEventClass().equals(eventClass)).collect(Collectors.toSet());
        }

        @Override
        public ProcessedEvent getQueuedEventStatistics(final ProcessedEvent.Key key) {
            return getProcessedEventStatistics(key);
        }

        @Override
        public Double getQueuedEventNumber(final ProcessedEvent.Key key) {
            return getProcessedEventNumber(key);
        }

        @Override
        public Collection<ProcessedEvent.Key> getProcessedEventKeys() {
            return processedEvents.keySet().stream().filter(key -> !isQueuePreProcessor(key)).collect(Collectors.toSet());
        }

        @Override
        public Collection<ProcessedEvent.Key> getProcessedEventKeys(final String eventClass) {
            return processedEvents.keySet().stream().filter(key -> !isQueuePreProcessor(key) && key.getEventClass().equals(eventClass)).collect(Collectors.toSet());
        }

        @Override
        public ProcessedEvent getProcessedEventStatistics(final ProcessedEvent.Key key) {
            return processedEvents.get(key);
        }

        @Override
        public Double getProcessedEventNumber(final ProcessedEvent.Key key) {
            return mathRound((double) getProcessedEventStatistics(key).getEventCount() / timeFrameSeconds, 3);
        }

        @Override
        public Collection<PublishedEvent.Key> getPublishedEventKeys() {
            return publishedEvents.keySet();
        }

        @Override
        public Collection<PublishedEvent.Key> getPublishedEventKeys(final String eventClass) {
            return publishedEvents.keySet().stream().filter(key -> key.getEventClass().equals(eventClass)).collect(Collectors.toSet());
        }

        @Override
        public PublishedEvent getPublishedEventStatistics(final PublishedEvent.Key key) {
            return publishedEvents.get(key);
        }

        @Override
        public Double getPublishedEventNumber(final PublishedEvent.Key key) {
            return mathRound((double) getPublishedEventStatistics(key).getEventCount() / timeFrameSeconds, 3);
        }

        @Override
        public boolean isQueueStatisticsPresent() {
            return queueStatisticsProvider.isQueueStatisticsPresent();
        }

        @Override
        public Long getQueuedEventCount() {
            return queueStatisticsProvider.getQueuedEventCount();
        }

        @Override
        public Long getProcessedEventCount() {
            return queueStatisticsProvider.getProcessedEventCount();
        }

        @Override
        public Double getProcessedEventNumber() {
            return mathRound((double) queueStatisticsProvider.getProcessedEventCount() / timeFrameSeconds, 3);
        }

        protected void eventProcessed(final ProcessedEvent.Key key, final long durationNanos) {
            processedEvents.compute(key, (k, v) -> (v == null) ? new ProcessedEvent(key, durationNanos) : v.update(durationNanos));
        }

        protected void eventPublished(final PublishedEvent.Key key) {
            publishedEvents.compute(key, (k, v) -> (v == null) ? new PublishedEvent(key) : v.update());
        }

        protected void resetFrame(final Date frameBeginningTime) {
            this.frameBeginningTime = frameBeginningTime;
            processedEvents.clear();
            publishedEvents.clear();
        }
    };

    private class BatchStatisticsHolder extends FramedStatisticsHolder implements EventBatchStatisticsProvider {
        private volatile long processedEventCount;
        private volatile long batchProcessingDurationMillis;

        @Override
        public Double getProcessedEventNumber(final ProcessedEvent.Key key) {
            return mathRound((double) getProcessedEventStatistics(key).getEventCount() / (double) batchProcessingDurationMillis / 1000.0, 3);
        }

        @Override
        public Long getProcessedEventCount() {
            return processedEventCount;
        }

        @Override
        public Double getProcessedEventNumber() {
            return mathRound((double) processedEventCount / (double) batchProcessingDurationMillis / 1000.0, 3);
        }

        @Override
        public Long getBatchProcessingTime() {
            return batchProcessingDurationMillis;
        }
    };

    public StatisticsHolder(final QueueStatisticsProvider queueStatisticsProvider, final QueueStatisticsCleaner queueStatisticsCleaner,
                            final FramedEventStatisticsSubscription framedStatisticsSubscription, final EventBatchStatisticsSubscription batchStatisticsSubscription,
                            final Set<String> queuePreProcessors, final Double timeFrameSeconds) {
        this.queueStatisticsProvider = queueStatisticsProvider;
        this.queueStatisticsCleaner = queueStatisticsCleaner;
        this.framedStatisticsSubscription = framedStatisticsSubscription;
        this.batchStatisticsSubscription = batchStatisticsSubscription;
        this.queuePreProcessors = prepareQueuePreProcessors(queuePreProcessors);
        this.timeFrameSeconds = timeFrameSeconds;
    }

    protected Set<String> prepareQueuePreProcessors(final Set<String> queuePreProcessors) {
        final Set<String> cleanedQueuePreProcessors = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        queuePreProcessors.stream().filter(queuePreProcessor -> StringUtils.hasText(queuePreProcessor)).forEach(queuePreProcessor -> cleanedQueuePreProcessors.add(queuePreProcessor));
        return cleanedQueuePreProcessors;
    }

    @Override
    public void eventBatchProcessed(final long processedCount, final long startTime, final long endTime) {
        batchStatisticsExecutor.execute(() -> {
            batchStatisticsHolder.processedEventCount = processedCount;
            batchStatisticsHolder.batchProcessingDurationMillis = endTime - startTime;

            for (final EventBatchStatisticsConsumer consumer : batchStatisticsSubscription.getConsumers()) {
                consumer.consumeStatistics(batchStatisticsHolder);
            }
            batchStatisticsHolder.resetFrame(new Date());
        });
    }

    public void timeFrame() {
        for (final FramedEventStatisticsConsumer consumer : framedStatisticsSubscription.getConsumers()) {
            consumer.consumeStatistics(framedStatisticsHolder);
        }
        queueStatisticsCleaner.timeFrame();
        framedStatisticsHolder.resetFrame(new Date());
    }

    private boolean isQueuePreProcessor(final ProcessedEvent.Key key) {
        boolean isQueuePreProcessor = false;
        for (final String queuePreProcessor : queuePreProcessors) {
            isQueuePreProcessor |= key.getHandlerClass().equals(queuePreProcessor);
        }
        return isQueuePreProcessor;
    }

    @Override
    public Date getBeginningTime() {
        return beginningTime;
    }

    @Override
    public Collection<ProcessedEvent.Key> getQueuedEventKeys() {
        return processedEvents.keySet().stream().filter(key -> isQueuePreProcessor(key)).collect(Collectors.toSet());
    }

    @Override
    public Collection<ProcessedEvent.Key> getQueuedEventKeys(final String eventClass) {
        return processedEvents.keySet().stream().filter(key -> isQueuePreProcessor(key) && key.getEventClass().equals(eventClass)).collect(Collectors.toSet());
    }

    @Override
    public ProcessedEvent getQueuedEventStatistics(final ProcessedEvent.Key key) {
        return getProcessedEventStatistics(key);
    }

    @Override
    public Collection<ProcessedEvent.Key> getProcessedEventKeys() {
        return processedEvents.keySet().stream().filter(key -> !isQueuePreProcessor(key)).collect(Collectors.toSet());
    }

    @Override
    public Collection<ProcessedEvent.Key> getProcessedEventKeys(final String eventClass) {
        return processedEvents.keySet().stream().filter(key -> !isQueuePreProcessor(key) && key.getEventClass().equals(eventClass)).collect(Collectors.toSet());
    }

    @Override
    public ProcessedEvent getProcessedEventStatistics(final ProcessedEvent.Key key) {
        return processedEvents.get(key);
    }

    @Override
    public void eventProcessed(final String eventClass, final String handlerClass, final long duration, final TimeUnit timeUnit) {
        switch (timeUnit) {
            case NANOSECONDS: eventProcessed(eventClass, handlerClass, duration); break;
            case MICROSECONDS: eventProcessed(eventClass, handlerClass, TimeUnit.MICROSECONDS.convert(duration, TimeUnit.NANOSECONDS)); break;
            case MILLISECONDS: eventProcessed(eventClass, handlerClass, TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS)); break;
            case SECONDS: eventProcessed(eventClass, handlerClass, TimeUnit.SECONDS.convert(duration, TimeUnit.NANOSECONDS)); break;
            case MINUTES: eventProcessed(eventClass, handlerClass, TimeUnit.MINUTES.convert(duration, TimeUnit.NANOSECONDS)); break;
            case HOURS: eventProcessed(eventClass, handlerClass, TimeUnit.HOURS.convert(duration, TimeUnit.NANOSECONDS)); break;
            case DAYS: eventProcessed(eventClass, handlerClass, TimeUnit.DAYS.convert(duration, TimeUnit.NANOSECONDS)); break;
        }
    }

    @Override
    public void eventProcessed(final String eventClass, final String handlerClass, final long durationNanos) {
        ProcessedEvent.Key key = new ProcessedEvent.Key(eventClass, handlerClass);
        ProcessedEvent statistics = processedEvents.compute(key, (k, v) -> (v == null) ? new ProcessedEvent(key, durationNanos) : v.update(durationNanos));
        LOG.trace("Currently processed {} events of type {} by handler {}.", statistics.getEventCount(), eventClass, handlerClass);
        framedStatisticsHolder.eventProcessed(key, durationNanos);
        batchStatisticsHolder.eventProcessed(key, durationNanos);
    }

    @Override
    public Collection<PublishedEvent.Key> getPublishedEventKeys() {
        return publishedEvents.keySet();
    }

    @Override
    public Collection<PublishedEvent.Key> getPublishedEventKeys(final String eventClass) {
        return publishedEvents.keySet().stream().filter(key -> key.getEventClass().equals(eventClass)).collect(Collectors.toSet());
    }

    @Override
    public PublishedEvent getPublishedEventStatistics(final PublishedEvent.Key key) {
        return publishedEvents.get(key);
    }

    @Override
    public void eventPublished(final String eventClass, final String publisherClass) {
        PublishedEvent.Key key = new PublishedEvent.Key(eventClass, publisherClass);
        PublishedEvent statistics = publishedEvents.compute(key, (k, v) -> (v == null) ? new PublishedEvent(key) : v.update());
        LOG.trace("Currently published {} events of type {} by publisher {}.", statistics.getEventCount(), eventClass, publisherClass);
        framedStatisticsHolder.eventPublished(key);
        batchStatisticsHolder.eventPublished(key);
    }

    @Override
    public void clear() {
        timeFrame();

        logStatistics();

        processedEvents.clear();
        publishedEvents.clear();

        beginningTime = new Date();

        LOG.info("Statistics cleaned.");
    }

    private void logStatistics() {
        LOG.info("=== Processed event statistics ===");
        for (ProcessedEvent statistics : processedEvents.values()) {
            LOG.info("EVENT={}, HANDLER={}, COUNT={}; DURATION: MIN={} µs, AVG={} µs, MAX={} µs", statistics.getEventClass(), statistics.getHandlerClass(), statistics.getEventCount(),
                     toMicros(statistics.getMinDurationNanos()), toMicros(statistics.getAvgDurationNanos()), toMicros(statistics.getMaxDurationNanos()));
        }
        LOG.info("=== Published event statistics ===");
        for (PublishedEvent statistics : publishedEvents.values()) {
            LOG.info("EVENT={}, PUBLISHER={}, COUNT={}", statistics.getEventClass(), statistics.getPublisherClass(), statistics.getEventCount());
        }
        LOG.info("==================================");
    }

}
