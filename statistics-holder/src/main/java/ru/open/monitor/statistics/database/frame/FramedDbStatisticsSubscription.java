package ru.open.monitor.statistics.database.frame;

public interface FramedDbStatisticsSubscription {

    void registerStatisticsConsumer(FramedDbStatisticsConsumer consumer);

}
