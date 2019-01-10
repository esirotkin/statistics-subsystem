package ru.open.monitor.statistics.log.logback.rolling;

import ru.open.monitor.statistics.log.FramedStatisticsQueueLoggerCsv;

public class QueueStatisticsRollingFileAppender extends AbstractRollingFileWithHeaderAppender {

    @Override
    protected String prepareHeader() {
        return FramedStatisticsQueueLoggerCsv.prepareHeader();
    }

}
