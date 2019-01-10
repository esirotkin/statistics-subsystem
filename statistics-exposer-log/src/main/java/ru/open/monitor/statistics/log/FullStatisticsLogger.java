package ru.open.monitor.statistics.log;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.StatisticsProvider;

@Component("fullStatisticsLogger")
public class FullStatisticsLogger extends AbstractStatisticsLogger<StatisticsProvider> {

    @Autowired
    private StatisticsProvider statisticsProvider;
    @Value("${statistics.monitor.log.fullLoggerLevel:TRACE}")
    private LoggerLevel loggerLevel;

    public void logStatistics() {
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
    protected String prepareDescription(final StatisticsProvider statisticsProvider) {
        StringBuilder description = new StringBuilder();
        description.append("Collected Statistics on ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date()));
        return description.toString();
    }

}
