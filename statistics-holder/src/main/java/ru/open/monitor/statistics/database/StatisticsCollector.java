package ru.open.monitor.statistics.database;

public interface StatisticsCollector {

    void statementExecuted(String statement, long durationNanos);

    void recordProcessed(String statement, String resultSet, long durationNanos);

}
