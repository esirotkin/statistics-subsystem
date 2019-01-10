package ru.open.monitor.statistics.log.logback.rolling;

import ru.open.monitor.statistics.log.FramedStatisticsPublishedLoggerCsv;

public class PublishedEventStatisticsRollingFileAppender extends AbstractRollingFileWithHeaderAppender {

    @Override
    protected String prepareHeader() {
        return FramedStatisticsPublishedLoggerCsv.prepareHeader();
    }

}
