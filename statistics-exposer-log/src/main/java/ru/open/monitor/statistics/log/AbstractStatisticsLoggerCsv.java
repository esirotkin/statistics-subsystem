package ru.open.monitor.statistics.log;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractStatisticsLoggerCsv {
    protected static final String DT_FORMAT = "yyyyMMdd HH:mm:ss";
    protected static final String CSV_DELIMITER = ";";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${statistics.monitor.log.framedLoggerLevel:DEBUG}")
    protected LoggerLevel loggerLevel;

    public void init() {}

    protected Logger getLog() {
        return log;
    }

    protected abstract void appendCsvHeader();

    protected void logRecords(final Collection<String> records) {
        for (final String record : records) {
            logRecord(record);
        }
    }

    protected void logRecord(final String record) {
        final Logger log = getLog();
        switch (loggerLevel) {
            case ERROR: if (log.isErrorEnabled()) { log.error(record); }; break;
            case WARN:  if (log.isWarnEnabled())  { log.warn(record); };  break;
            case INFO:  if (log.isInfoEnabled())  { log.info(record); };  break;
            case DEBUG: if (log.isDebugEnabled()) { log.debug(record); }; break;
            case TRACE: if (log.isTraceEnabled()) { log.trace(record); }; break;
        }
    }

}
