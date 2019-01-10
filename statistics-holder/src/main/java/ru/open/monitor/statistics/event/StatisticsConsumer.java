package ru.open.monitor.statistics.event;

public interface StatisticsConsumer<P extends StatisticsProvider> {

    void consumeStatistics(P statisticsProvider);

}
