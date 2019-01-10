package ru.open.monitor.statistics.event.frame.batch;

public interface EventBatchProcessedListener {

    void eventBatchProcessed(long processedCount, long startTime, long endTime);

}
