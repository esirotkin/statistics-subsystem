package ru.open.monitor.statistics.log.logback.rolling;

import ru.open.monitor.statistics.log.FramedStatisticsProcessedLoggerCsv;

public class ProcessedEventStatisticsRollingFileAppender extends AbstractRollingFileWithHeaderAppender {

    @Override
    protected String prepareHeader() {
        return FramedStatisticsProcessedLoggerCsv.prepareHeader();
    }

}
