package ru.open.monitor.statistics.stub;

import ru.open.monitor.statistics.event.frame.batch.EventBatchProcessedListener;

public class EventBatchProcessedListenerStub implements EventBatchProcessedListener {

    @Override public void eventBatchProcessed(long processedCount, long startTime, long endTime) {}

}
