package ru.open.monitor.statistics.jdbc;

import java.sql.Connection;

import org.springframework.beans.factory.annotation.Autowired;

import ru.open.monitor.statistics.database.StatisticsCollector;

public class ConnectionWrapperFactory {

    @Autowired
    private StatisticsCollector statisticsCollector;

    public Connection wrapConnection(final Connection connection) {
        return new ConnectionWrapper(connection, statisticsCollector);
    }

}
