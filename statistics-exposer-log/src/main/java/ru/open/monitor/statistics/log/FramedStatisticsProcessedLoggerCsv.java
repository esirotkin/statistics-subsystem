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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsConsumer;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsSubscription;

@Component
public class FramedStatisticsProcessedLoggerCsv extends AbstractStatisticsLoggerCsv implements FramedEventStatisticsConsumer {

    @Autowired
    private FramedEventStatisticsSubscription statisticsSubscription;
    @Value("${statistics.monitor.timeFrame:15000}")
    private Long timeFrameMillis;

    @Override
    @PostConstruct
    public void init() {
        statisticsSubscription.subscribeConsumer(this);
        super.init();
    }

    @Override
    protected void appendCsvHeader() {
        logRecord(prepareHeader());
    }

    @Override
    public void consumeStatistics(final FramedEventStatisticsProvider statisticsProvider) {
        logStatistics(statisticsProvider);
    }

    protected void logStatistics(final FramedEventStatisticsProvider statisticsProvider) {
        final Date currentTime = new Date();
        logRecords(prepareRecords(statisticsProvider, currentTime));
    }

    public static String prepareHeader() {
        StringBuilder csv = new StringBuilder();

        csv.append("dt").append(CSV_DELIMITER);
        csv.append("type").append(CSV_DELIMITER);
        csv.append("handler").append(CSV_DELIMITER);
        csv.append("count").append(CSV_DELIMITER);
        csv.append("count_per_second").append(CSV_DELIMITER);
        csv.append("time_min").append(CSV_DELIMITER);
        csv.append("time_avg").append(CSV_DELIMITER);
        csv.append("time_max");

        return csv.toString();
    }

    protected Collection<String> prepareRecords(final FramedEventStatisticsProvider statisticsProvider, final Date recordDate) {
        List<String> records = new ArrayList<>();

        if (!statisticsProvider.getProcessedEventKeys().isEmpty()) {
            final SortedMap<String, ProcessedEvent.Key> processedEventKeys = new TreeMap<>();
            for (final ProcessedEvent.Key key : statisticsProvider.getProcessedEventKeys()) {
                processedEventKeys.put(buildSortingKey(key), key);
            }
            for (final String sortingKey : processedEventKeys.keySet()) {
                final ProcessedEvent statistics = statisticsProvider.getProcessedEventStatistics(processedEventKeys.get(sortingKey));
                StringBuilder csv = new StringBuilder();

                csv.append(formatDate(recordDate, DT_FORMAT)).append(CSV_DELIMITER);
                csv.append(getSimpleClassName(statistics.getEventClass())).append(CSV_DELIMITER);
                csv.append(getSimpleClassName(statistics.getHandlerClass())).append(CSV_DELIMITER);
                csv.append(statistics.getEventCount()).append(CSV_DELIMITER);
                csv.append(formatPlain(statisticsProvider.getProcessedEventNumber(processedEventKeys.get(sortingKey)), 3)).append(CSV_DELIMITER);
                csv.append(formatPlain(toMicros(statistics.getMinDurationNanos()), 3)).append(CSV_DELIMITER);
                csv.append(formatPlain(toMicros(statistics.getAvgDurationNanos()), 3)).append(CSV_DELIMITER);
                csv.append(formatPlain(toMicros(statistics.getMaxDurationNanos()), 3));

                records.add(csv.toString());
            }
            processedEventKeys.clear();
        }

        return records;
    }

}
