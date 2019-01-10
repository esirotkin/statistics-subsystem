package ru.open.monitor.statistics.stub;

import ru.open.monitor.statistics.queue.QueueStatisticsCollector;

public class QueueStatisticsCollectorStub implements QueueStatisticsCollector {

    @Override public void eventQueued() {}

    @Override public void eventProcessed() {}

}
