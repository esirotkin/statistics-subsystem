package ru.open.monitor.statistics.database.frame;

import ru.open.monitor.statistics.database.ExecutedStatement;
import ru.open.monitor.statistics.database.ProcessedResult;
import ru.open.monitor.statistics.database.StatisticsProvider;

public interface FramedDbStatisticsProvider extends StatisticsProvider {

    Double getExecutedStatementNumber(final ExecutedStatement.Key key);

    Double getProcessedRecordNumber(final ProcessedResult.Key key);

}
