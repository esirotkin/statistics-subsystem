package ru.open.monitor.statistics.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.StatisticsCleaner;

@Component
@ManagedResource(objectName = "ru.open.monitor.statistics:name=StatisticsCleaner", description = "ProcessedEvent cleaner.")
public class StatisticsCleanerMBean {
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsCleanerMBean.class);

    @Autowired
    private StatisticsCleaner statisticsCleaner;

    @ManagedOperation(description = "Clear statistics data.")
    public void clearStatistics() {
        LOG.info("Cleaning statistics ...");
        statisticsCleaner.clear();
    }

}
