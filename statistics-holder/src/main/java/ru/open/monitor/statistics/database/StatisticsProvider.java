package ru.open.monitor.statistics.database;

import java.util.Collection;
import java.util.Date;

public interface StatisticsProvider {

    Date getBeginningTime();

    Collection<ExecutedStatement.Key> getExecutedStatementKeys();

    Collection<ExecutedStatement.Key> getExecutedStatementKeys(final String statement);

    ExecutedStatement getExecutedStatementStatistics(final ExecutedStatement.Key key);

    Collection<ProcessedResult.Key> getProcessedResultKeys();

    Collection<ProcessedResult.Key> getProcessedResultKeys(final String statement);

    ProcessedResult getProcessedResultStatistics(final ProcessedResult.Key key);

}
