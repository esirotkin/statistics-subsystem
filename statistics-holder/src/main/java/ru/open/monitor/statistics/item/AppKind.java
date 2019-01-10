package ru.open.monitor.statistics.item;

import static ru.open.monitor.statistics.item.ItemUtil.APP_EXECUTED_STATEMENTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PROCESSED_RESULTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PROCESSED_EVENTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PUBLISHED_EVENTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_QUEUE_STATISTICS;

public enum AppKind {
    PROCESSED_EVENTS(APP_PROCESSED_EVENTS),
    PUBLISHED_EVENTS(APP_PUBLISHED_EVENTS),
    QUEUE_STATISTICS(APP_QUEUE_STATISTICS),
    EXECUTED_STATEMENTS(APP_EXECUTED_STATEMENTS),
    PROCESSED_RESULTS(APP_PROCESSED_RESULTS);

    private final String name;

    private AppKind(final String name) {
        this.name = name;
    }

    public String getApplicationName() {
        return name;
    }
};
