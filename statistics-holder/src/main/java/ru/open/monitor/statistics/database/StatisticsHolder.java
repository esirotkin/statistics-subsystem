package ru.open.monitor.statistics.database;

import static ru.open.monitor.statistics.item.ItemUtil.mathRound;
import static ru.open.monitor.statistics.item.ItemUtil.toMicros;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.open.monitor.statistics.database.frame.FramedDbStatisticsConsumer;
import ru.open.monitor.statistics.database.frame.FramedDbStatisticsProvider;
import ru.open.monitor.statistics.database.frame.FramedDbStatisticsSubscription;

public class StatisticsHolder implements StatisticsCollector, StatisticsProvider, StatisticsCleaner, FramedDbStatisticsSubscription {
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsHolder.class);

    private final ConcurrentMap<ExecutedStatement.Key, ExecutedStatement> executedStatements = new ConcurrentHashMap<>();
    private final ConcurrentMap<ProcessedResult.Key, ProcessedResult> processedResults = new ConcurrentHashMap<>();

    private final ConcurrentMap<ExecutedStatement.Key, ExecutedStatement> executedStatementsFramed = new ConcurrentHashMap<>();
    private final ConcurrentMap<ProcessedResult.Key, ProcessedResult> processedResultsFramed = new ConcurrentHashMap<>();

    private final Set<FramedDbStatisticsConsumer> framedStatisticsConsumers = new CopyOnWriteArraySet<>();

    private final Double timeFrameSeconds;

    private volatile Date beginningTime = new Date();
    private volatile Date frameBeginningTime = new Date();

    private final FramedDbStatisticsProvider framedStatisticsProvider = new FramedDbStatisticsProvider() {

        @Override
        public Date getBeginningTime() {
            return frameBeginningTime;
        }

        @Override
        public Collection<ExecutedStatement.Key> getExecutedStatementKeys() {
            return executedStatementsFramed.keySet();
        }

        @Override
        public Collection<ExecutedStatement.Key> getExecutedStatementKeys(final String statement) {
            return executedStatementsFramed.keySet().stream().filter(key -> key.getStatement().equals(statement)).collect(Collectors.toSet());
        }

        @Override
        public ExecutedStatement getExecutedStatementStatistics(final ExecutedStatement.Key key) {
            return executedStatementsFramed.get(key);
        }

        @Override
        public Double getExecutedStatementNumber(final ExecutedStatement.Key key) {
            return mathRound((double) getExecutedStatementStatistics(key).getExecuteCount() / timeFrameSeconds, 3);
        }

        @Override
        public Collection<ProcessedResult.Key> getProcessedResultKeys() {
            return processedResultsFramed.keySet();
        }

        @Override
        public Collection<ProcessedResult.Key> getProcessedResultKeys(final String statement) {
            return processedResultsFramed.keySet().stream().filter(key -> key.getStatement().equals(statement)).collect(Collectors.toSet());
        }

        @Override
        public ProcessedResult getProcessedResultStatistics(final ProcessedResult.Key key) {
            return processedResultsFramed.get(key);
        }

        @Override
        public Double getProcessedRecordNumber(final ProcessedResult.Key key) {
            return mathRound((double) getProcessedResultStatistics(key).getCallCount() / timeFrameSeconds, 3);
        }

    };

    public void timeFrame() {
        for (final FramedDbStatisticsConsumer consumer : framedStatisticsConsumers) {
            consumer.timeFrame(framedStatisticsProvider);
        }

        processedResultsFramed.clear();
        executedStatementsFramed.clear();

        frameBeginningTime = new Date();
    }

    public StatisticsHolder(final Double timeFrameSeconds) {
        this.timeFrameSeconds = timeFrameSeconds;
    }

    @Override
    public Date getBeginningTime() {
        return beginningTime;
    }

    @Override
    public Collection<ExecutedStatement.Key> getExecutedStatementKeys() {
        return executedStatements.keySet();
    }

    @Override
    public Collection<ExecutedStatement.Key> getExecutedStatementKeys(final String statement) {
        return executedStatements.keySet().stream().filter(key -> key.getStatement().equals(statement)).collect(Collectors.toSet());
    }

    @Override
    public ExecutedStatement getExecutedStatementStatistics(final ExecutedStatement.Key key) {
        return executedStatements.get(key);
    }

    @Override
    public void statementExecuted(final String statement, final long durationNanos) {
        ExecutedStatement.Key key = new ExecutedStatement.Key(statement);
        ExecutedStatement statistics = executedStatements.compute(key, (k, v) -> (v == null) ? new ExecutedStatement(key, durationNanos) : v.update(durationNanos));
        LOG.trace("Currently procedure {} executed {} times.", statement, statistics.getExecuteCount());
        executedStatementsFramed.compute(key, (k, v) -> (v == null) ? new ExecutedStatement(key, durationNanos) : v.update(durationNanos));
    }

    @Override
    public Collection<ProcessedResult.Key> getProcessedResultKeys() {
        return processedResults.keySet();
    }

    @Override
    public Collection<ProcessedResult.Key> getProcessedResultKeys(final String statement) {
        return processedResults.keySet().stream().filter(key -> key.getStatement().equals(statement)).collect(Collectors.toSet());
    }

    @Override
    public ProcessedResult getProcessedResultStatistics(final ProcessedResult.Key key) {
        return processedResults.get(key);
    }

    @Override
    public void recordProcessed(final String statement, final String resultSet, final long durationNanos) {
        ProcessedResult.Key key = new ProcessedResult.Key(statement, resultSet);
        ProcessedResult statistics = processedResults.compute(key, (k, v) -> (v == null) ? new ProcessedResult(key, durationNanos) : v.update(durationNanos));
        LOG.trace("Currently processed {} records of result set {} of {} procedure.", statistics.getCallCount(), resultSet, statement);
        processedResultsFramed.compute(key, (k, v) -> (v == null) ? new ProcessedResult(key, durationNanos) : v.update(durationNanos));
    }

    @Override
    public void registerStatisticsConsumer(final FramedDbStatisticsConsumer consumer) {
        framedStatisticsConsumers.add(consumer);
    }

    @Override
    public void clear() {
        timeFrame();

        logStatistics();

        executedStatements.clear();
        processedResults.clear();

        beginningTime = new Date();

        LOG.info("Statistics cleaned.");
    }

    private void logStatistics() {
        if (!executedStatements.isEmpty()) {
            LOG.info("=== Executed procedure statistics ===");
            for (ExecutedStatement statistics : executedStatements.values()) {
                LOG.info("STMT={}, COUNT={}; DURATION: MIN={} µs, AVG={} µs, MAX={} µs", statistics.getStatement(), statistics.getExecuteCount(),
                         toMicros(statistics.getMinDurationNanos()), toMicros(statistics.getAvgDurationNanos()), toMicros(statistics.getMaxDurationNanos()));
            }
        }
        if (!processedResults.isEmpty()) {
            LOG.info("=== Processed result set statistics ===");
            for (ProcessedResult statistics : processedResults.values()) {
                LOG.info("STMT={}, RESULTSET={}, COUNT={}; DURATION: MIN={} µs, AVG={} µs, MAX={} µs", statistics.getStatement(), statistics.getResultSet(), statistics.getCallCount(),
                         toMicros(statistics.getMinDurationNanos()), toMicros(statistics.getAvgDurationNanos()), toMicros(statistics.getMaxDurationNanos()));
            }
        }
        if (!executedStatements.isEmpty() || !processedResults.isEmpty()) {
            LOG.info("===================================");
        }
    }

}
