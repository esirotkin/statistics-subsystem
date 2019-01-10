package ru.open.monitor.statistics.stub;

import ru.open.monitor.statistics.database.StatisticsCollector;

public class DatabaseStatisticsCollectorStub implements StatisticsCollector {

    @Override public void recordProcessed(String statement, String resultSet, long durationNanos) {}

    @Override public void statementExecuted(String statement, long durationNanos) {}

}
