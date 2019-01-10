package ru.open.monitor.statistics.log;

import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;
import static ru.open.monitor.statistics.item.ItemUtil.toMicros;
import static ru.open.monitor.statistics.log.AbstractStatisticsLogger.buildSortingKey;
import static ru.open.monitor.statistics.log.util.DateTimeUtil.formatDate;
import static ru.open.monitor.statistics.log.util.NumberUtil.formatPlain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;

@Component
public class FramedStatisticsQueuedLoggerCsv extends FramedStatisticsProcessedLoggerCsv {

    @Override
    protected void logStatistics(final FramedEventStatisticsProvider statisticsProvider) {
        final Date currentTime = new Date();
        logRecords(prepareRecords(statisticsProvider, currentTime));
    }

    @Override
    protected Collection<String> prepareRecords(final FramedEventStatisticsProvider statisticsProvider, final Date recordDate) {
        List<String> records = new ArrayList<>();

        if (!statisticsProvider.getQueuedEventKeys().isEmpty()) {
            final SortedMap<String, ProcessedEvent.Key> queuedEventKeys = new TreeMap<>();
            for (final ProcessedEvent.Key key : statisticsProvider.getQueuedEventKeys()) {
                queuedEventKeys.put(buildSortingKey(key), key);
            }
            for (final String sortingKey : queuedEventKeys.keySet()) {
                final ProcessedEvent statistics = statisticsProvider.getQueuedEventStatistics(queuedEventKeys.get(sortingKey));
                StringBuilder csv = new StringBuilder();

                csv.append(formatDate(recordDate, DT_FORMAT)).append(CSV_DELIMITER);
                csv.append(getSimpleClassName(statistics.getEventClass())).append(CSV_DELIMITER);
                csv.append(getSimpleClassName(statistics.getHandlerClass())).append(CSV_DELIMITER);
                csv.append(statistics.getEventCount()).append(CSV_DELIMITER);
                csv.append(formatPlain(statisticsProvider.getProcessedEventNumber(queuedEventKeys.get(sortingKey)), 3)).append(CSV_DELIMITER);
                csv.append(formatPlain(toMicros(statistics.getMinDurationNanos()), 3)).append(CSV_DELIMITER);
                csv.append(formatPlain(toMicros(statistics.getAvgDurationNanos()), 3)).append(CSV_DELIMITER);
                csv.append(formatPlain(toMicros(statistics.getMaxDurationNanos()), 3));

                records.add(csv.toString());
            }
            queuedEventKeys.clear();
        }

        return records;
    }

}
