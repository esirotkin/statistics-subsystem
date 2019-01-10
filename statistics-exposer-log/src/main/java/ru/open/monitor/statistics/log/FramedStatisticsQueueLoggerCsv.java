package ru.open.monitor.statistics.log;

import static ru.open.monitor.statistics.log.util.DateTimeUtil.formatDate;
import static ru.open.monitor.statistics.log.util.NumberUtil.formatPlain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.frame.FramedEventStatisticsConsumer;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsSubscription;

@Component
public class FramedStatisticsQueueLoggerCsv extends AbstractStatisticsLoggerCsv implements FramedEventStatisticsConsumer {

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

    private void logStatistics(final FramedEventStatisticsProvider statisticsProvider) {
        final Date currentTime = new Date();
        logRecords(prepareRecords(statisticsProvider, currentTime));
    }

    public static String prepareHeader() {
        StringBuilder csv = new StringBuilder();

        csv.append("dt").append(CSV_DELIMITER);
        csv.append("queued_count").append(CSV_DELIMITER);
        csv.append("processed_count").append(CSV_DELIMITER);
        csv.append("processed_per_second");

        return csv.toString();
    }

    private Collection<String> prepareRecords(final FramedEventStatisticsProvider statisticsProvider, final Date recordDate) {
        List<String> records = new ArrayList<>();

        if (statisticsProvider.isQueueStatisticsPresent()) {
            StringBuilder csv = new StringBuilder();

            csv.append(formatDate(recordDate, DT_FORMAT)).append(CSV_DELIMITER);
            csv.append(statisticsProvider.getQueuedEventCount()).append(CSV_DELIMITER);
            csv.append(statisticsProvider.getProcessedEventCount()).append(CSV_DELIMITER);
            csv.append(formatPlain(statisticsProvider.getProcessedEventNumber(), 3));

            records.add(csv.toString());
        }

        return records;
    }

}
