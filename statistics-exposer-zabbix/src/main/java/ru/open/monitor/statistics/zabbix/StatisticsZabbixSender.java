package ru.open.monitor.statistics.zabbix;

import static ru.open.monitor.statistics.item.ItemUtil.toSeconds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.hengyunabc.zabbix.sender.DataObject;

import ru.open.monitor.statistics.database.ExecutedStatement;
import ru.open.monitor.statistics.database.ProcessedResult;
import ru.open.monitor.statistics.database.frame.FramedDbStatisticsConsumer;
import ru.open.monitor.statistics.database.frame.FramedDbStatisticsProvider;
import ru.open.monitor.statistics.database.frame.FramedDbStatisticsSubscription;
import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.PublishedEvent;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsConsumer;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsSubscription;
import ru.open.monitor.statistics.item.ItemKey;
import ru.open.monitor.statistics.item.ItemKind;
import ru.open.monitor.statistics.queue.QueueStatisticsKey;
import ru.open.monitor.statistics.zabbix.config.ZabbixConfigurer;

@Component
public class StatisticsZabbixSender implements FramedEventStatisticsConsumer, FramedDbStatisticsConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsZabbixSender.class);

    private static final double ZERO_VALUE = 0.0D;

    private final Set<ItemKey> collectedEventItemKeys = new HashSet<>();
    private final Set<ItemKey> collectedDbItemKeys = new HashSet<>();

    @Autowired
    private FramedEventStatisticsSubscription eventStatisticsSubscription;
    @Autowired
    private FramedDbStatisticsSubscription dbStatisticsSubscription;
    @Autowired
    private ZabbixConfigurer zabbixConfigurer;
    @Autowired
    private ZabbixSenderWrapper zabbixSender;

    @PostConstruct
    public void init() {
        if (zabbixConfigurer.isEnabled()) {
            zabbixConfigurer.configure();
            eventStatisticsSubscription.subscribeConsumer(this);
            dbStatisticsSubscription.registerStatisticsConsumer(this);
        } else {
            LOG.warn("Statistics Exposer Zabbix is DISABLED!");
        }
    }

    @Override
    public void consumeStatistics(final FramedEventStatisticsProvider statisticsProvider) {
        sendStatisticsToZabbix(statisticsProvider);
    }

    private void sendStatisticsToZabbix(final FramedEventStatisticsProvider statisticsProvider) {
        if (zabbixConfigurer.isEnabled() && zabbixConfigurer.isConfigured()) {
            LOG.debug("Sending statistics to Zabbix ...");
            List<DataObject> zabbixData = prepareZabbixData(statisticsProvider);
            LOG.debug("Collected {} items to send.", zabbixData.size());

            if (!zabbixData.isEmpty()) {
                try {
                    zabbixSender.sendToZabbix(zabbixData);
                } catch (Exception e) {
                    LOG.warn("Failed to send statistics to Zabbix!", e);
                }
            }
        }
    }

    @Override
    public void timeFrame(final FramedDbStatisticsProvider statisticsProvider) {
        sendStatisticsToZabbix(statisticsProvider);
    }

    private void sendStatisticsToZabbix(final FramedDbStatisticsProvider statisticsProvider) {
        if (zabbixConfigurer.isEnabled() && zabbixConfigurer.isConfigured()) {
            LOG.debug("Sending statistics to Zabbix ...");
            List<DataObject> zabbixData = prepareZabbixData(statisticsProvider);
            LOG.debug("Collected {} items to send.", zabbixData.size());

            if (!zabbixData.isEmpty()) {
                try {
                    zabbixSender.sendToZabbix(zabbixData);
                } catch (Exception e) {
                    LOG.warn("Failed to send statistics to Zabbix!", e);
                }
            }
        }
    }

    private List<DataObject> prepareZabbixData(final FramedEventStatisticsProvider statisticsProvider) {
        List<DataObject> zabbixData = new ArrayList<>();
        Set<ItemKey> currentItemKeys = new HashSet<>();

        for (final ProcessedEvent.Key key : statisticsProvider.getQueuedEventKeys()) {
            if (zabbixConfigurer.isInterestProcessedEvent(key.getEventClass())) {
                if (zabbixConfigurer.configureItem(key)) {
                    currentItemKeys.add(key);
                    zabbixData.addAll(prepareQueuedEventData(statisticsProvider, key));
                }
            }
        }

        for (final ProcessedEvent.Key key : statisticsProvider.getProcessedEventKeys()) {
            if (zabbixConfigurer.isInterestProcessedEvent(key.getEventClass())) {
                if (zabbixConfigurer.configureItem(key)) {
                    currentItemKeys.add(key);
                    zabbixData.addAll(prepareProcessedEventData(statisticsProvider, key));
                }
            }
        }

        for (final PublishedEvent.Key key : statisticsProvider.getPublishedEventKeys()) {
            if (zabbixConfigurer.isInterestPublishedEvent(key.getEventClass())) {
                if (zabbixConfigurer.configureItem(key)) {
                    currentItemKeys.add(key);
                    zabbixData.addAll(preparePublishedEventData(statisticsProvider, key));
                }
            }
        }

        if (statisticsProvider.isQueueStatisticsPresent()) {
            for (QueueStatisticsKey key : QueueStatisticsKey.values()) {
                if (zabbixConfigurer.configureItem(key)) {
                    currentItemKeys.add(key);
                    zabbixData.add(prepareQueueStatisticsData(statisticsProvider, key));
                }
            }
        }

        zabbixData.addAll(collectZeroItems(collectedEventItemKeys, currentItemKeys));

        return zabbixData;
    }

    private List<DataObject> prepareQueuedEventData(final FramedEventStatisticsProvider statisticsProvider, final ProcessedEvent.Key key) {
        Double count = statisticsProvider.getQueuedEventNumber(key);
        DataObject itemCount = createDataObject();
        itemCount.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.PROCESSED_EVENT_COUNT));
        itemCount.setValue(count.toString());

        ProcessedEvent statistics = statisticsProvider.getQueuedEventStatistics(key);
        Double avgTime = toSeconds(statistics.getAvgDurationNanos());
        DataObject itemAvgTime = createDataObject();
        itemAvgTime.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.PROCESSED_EVENT_D_AVG));
        itemAvgTime.setValue(String.format("%+01.3f", avgTime));

        collectedEventItemKeys.add(key);
        return Arrays.asList(itemCount, itemAvgTime);
    }

    private List<DataObject> prepareProcessedEventData(final FramedEventStatisticsProvider statisticsProvider, final ProcessedEvent.Key key) {
        Double count = statisticsProvider.getProcessedEventNumber(key);
        DataObject itemCount = createDataObject();
        itemCount.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.PROCESSED_EVENT_COUNT));
        itemCount.setValue(count.toString());

        ProcessedEvent statistics = statisticsProvider.getProcessedEventStatistics(key);
        Double avgTime = toSeconds(statistics.getAvgDurationNanos());
        DataObject itemAvgTime = createDataObject();
        itemAvgTime.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.PROCESSED_EVENT_D_AVG));
        itemAvgTime.setValue(String.format("%+01.3f", avgTime));

        collectedEventItemKeys.add(key);
        return Arrays.asList(itemCount, itemAvgTime);
    }

    private List<DataObject> preparePublishedEventData(final FramedEventStatisticsProvider statisticsProvider, final PublishedEvent.Key key) {
        Double count = statisticsProvider.getPublishedEventNumber(key);
        DataObject itemCount = createDataObject();
        itemCount.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.PUBLISHED_EVENT_COUNT));
        itemCount.setValue(count.toString());

        collectedEventItemKeys.add(key);
        return Arrays.asList(itemCount);
    }

    private DataObject prepareQueueStatisticsData(final FramedEventStatisticsProvider statisticsProvider, final QueueStatisticsKey key) {
        DataObject item = createDataObject();
        item.setKey(zabbixConfigurer.getZabbixItemKey(key, key.getItemKind()));
        switch (key) {
            case QUEUED_EVENT_COUNT:
                Long queuedEventCount = statisticsProvider.getQueuedEventCount();
                item.setValue(String.format("%+01d", queuedEventCount));
                collectedEventItemKeys.add(key);
                break;
            case PROCESSED_EVENT_NUMBER:
                Double processedEventCount = statisticsProvider.getProcessedEventNumber();
                item.setValue(String.format("%+01.3f", processedEventCount));
                collectedEventItemKeys.add(key);
                break;
        }

        return item;
    }

    private List<DataObject> prepareZabbixData(final FramedDbStatisticsProvider statisticsProvider) {
        List<DataObject> zabbixData = new ArrayList<>();
        Set<ItemKey> currentItemKeys = new HashSet<>();

        for (final ExecutedStatement.Key key : statisticsProvider.getExecutedStatementKeys()) {
            if (zabbixConfigurer.isInterestDatabaseStatement(key.getStatement())) {
                if (zabbixConfigurer.configureItem(key)) {
                    currentItemKeys.add(key);
                    zabbixData.addAll(prepareExecutedStatementsData(statisticsProvider, key));
                }
            }
        }

        for (final ProcessedResult.Key key : statisticsProvider.getProcessedResultKeys()) {
            if (zabbixConfigurer.isInterestDatabaseStatement(key.getStatement())) {
                if (zabbixConfigurer.configureItem(key)) {
                    currentItemKeys.add(key);
                    zabbixData.addAll(prepareProcessedResultsData(statisticsProvider, key));
                }
            }
        }

        zabbixData.addAll(collectZeroItems(collectedDbItemKeys, currentItemKeys));

        return zabbixData;
    }

    private List<DataObject> prepareExecutedStatementsData(final FramedDbStatisticsProvider statisticsProvider, final ExecutedStatement.Key key) {
        Double count = statisticsProvider.getExecutedStatementNumber(key);
        DataObject itemCount = createDataObject();
        itemCount.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.STATEMENT_COUNT));
        itemCount.setValue(count.toString());

        ExecutedStatement statistics = statisticsProvider.getExecutedStatementStatistics(key);
        Double avgTime = toSeconds(statistics.getAvgDurationNanos());
        DataObject itemAvgTime = createDataObject();
        itemAvgTime.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.STATEMENT_D_AVG));
        itemAvgTime.setValue(String.format("%+01.3f", avgTime));

        collectedDbItemKeys.add(key);
        return Arrays.asList(itemCount, itemAvgTime);
    }

    private List<DataObject> prepareProcessedResultsData(final FramedDbStatisticsProvider statisticsProvider, final ProcessedResult.Key key) {
        Double count = statisticsProvider.getProcessedRecordNumber(key);
        DataObject itemCount = createDataObject();
        itemCount.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.RESULT_COUNT));
        itemCount.setValue(count.toString());

        ProcessedResult statistics = statisticsProvider.getProcessedResultStatistics(key);
        Double avgTime = toSeconds(statistics.getAvgDurationNanos());
        DataObject itemAvgTime = createDataObject();
        itemAvgTime.setKey(zabbixConfigurer.getZabbixItemKey(key, ItemKind.RESULT_D_AVG));
        itemAvgTime.setValue(String.format("%+01.3f", avgTime));

        collectedDbItemKeys.add(key);
        return Arrays.asList(itemCount, itemAvgTime);
    }

    private List<DataObject> collectZeroItems(final Set<ItemKey> collectedItemKeys, final Set<ItemKey> currentItemKeys) {
        List<DataObject> zeroItems = new ArrayList<>();

        Set<ItemKey> emptyValueKeys = new HashSet<>(collectedItemKeys);
        emptyValueKeys.removeAll(currentItemKeys);

        for (final ItemKey emptyValueKey : emptyValueKeys) {
            DataObject countItem = createDataObject();
            DataObject avgItem = createDataObject();
            switch (emptyValueKey.getApplicationKind()) {
                case PROCESSED_EVENTS:
                    countItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ItemKind.PROCESSED_EVENT_COUNT));
                    countItem.setValue(Double.toString(ZERO_VALUE));

                    avgItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ItemKind.PROCESSED_EVENT_D_AVG));
                    avgItem.setValue(Double.toString(ZERO_VALUE));
                    zeroItems.add(avgItem);
                    break;
                case PUBLISHED_EVENTS:
                    countItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ItemKind.PUBLISHED_EVENT_COUNT));
                    countItem.setValue(Double.toString(ZERO_VALUE));
                    break;
                case QUEUE_STATISTICS:
                    countItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ((QueueStatisticsKey) emptyValueKey).getItemKind()));
                    switch (((QueueStatisticsKey) emptyValueKey).getItemKind()) {
                        case QUEUED_EVENT_COUNT: countItem.setValue(Integer.toString(0)); break;
                        case PROCESSED_EVENT_NUMBER: countItem.setValue(Double.toString(ZERO_VALUE)); break;
                    }
                    break;
                case EXECUTED_STATEMENTS:
                    countItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ItemKind.STATEMENT_COUNT));
                    countItem.setValue(Double.toString(ZERO_VALUE));

                    avgItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ItemKind.STATEMENT_D_AVG));
                    avgItem.setValue(Double.toString(ZERO_VALUE));
                    zeroItems.add(avgItem);
                    break;
                case PROCESSED_RESULTS:
                    countItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ItemKind.RESULT_COUNT));
                    countItem.setValue(Double.toString(ZERO_VALUE));

                    avgItem.setKey(zabbixConfigurer.getZabbixItemKey(emptyValueKey, ItemKind.RESULT_D_AVG));
                    avgItem.setValue(Double.toString(ZERO_VALUE));
                    zeroItems.add(avgItem);
                    break;
            }

            zeroItems.add(countItem);
        }

        LOG.debug("Collected {} empty (zero) items.", zeroItems.size());
        return zeroItems;
    }

    private DataObject createDataObject() {
        return DataObject.builder().host(zabbixConfigurer.getApplicationHost()).build();
    }

}
