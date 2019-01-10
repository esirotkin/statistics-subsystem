package ru.open.monitor.statistics.item;

public enum ItemKind {
    PROCESSED_EVENT_COUNT(AppKind.PROCESSED_EVENTS, "count"),
    PROCESSED_EVENT_D_MIN(AppKind.PROCESSED_EVENTS, "min"),
    PROCESSED_EVENT_D_AVG(AppKind.PROCESSED_EVENTS, "avg"),
    PROCESSED_EVENT_D_MAX(AppKind.PROCESSED_EVENTS, "max"),
    PROCESSED_EVENT_D_LAST(AppKind.PROCESSED_EVENTS, "last"),
    PUBLISHED_EVENT_COUNT(AppKind.PUBLISHED_EVENTS, "count"),
    QUEUED_EVENT_COUNT(AppKind.QUEUE_STATISTICS, "count"),
    PROCESSED_EVENT_NUMBER(AppKind.QUEUE_STATISTICS, "count"),
    STATEMENT_COUNT(AppKind.EXECUTED_STATEMENTS, "count"),
    STATEMENT_D_MIN(AppKind.EXECUTED_STATEMENTS, "min"),
    STATEMENT_D_AVG(AppKind.EXECUTED_STATEMENTS, "avg"),
    STATEMENT_D_MAX(AppKind.EXECUTED_STATEMENTS, "max"),
    STATEMENT_D_LAST(AppKind.EXECUTED_STATEMENTS, "last"),
    RESULT_COUNT(AppKind.PROCESSED_RESULTS, "count"),
    RESULT_D_MIN(AppKind.PROCESSED_RESULTS, "min"),
    RESULT_D_AVG(AppKind.PROCESSED_RESULTS, "avg"),
    RESULT_D_MAX(AppKind.PROCESSED_RESULTS, "max"),
    RESULT_D_LAST(AppKind.PROCESSED_RESULTS, "last");

    private final AppKind applicationKind;
    private final String itemName;

    private ItemKind(final AppKind applicationKind, final String itemName) {
        this.applicationKind = applicationKind;
        this.itemName = itemName;
    }

    public AppKind getApplicationKind() {
        return applicationKind;
    }

    public String getItemName() {
        return itemName;
    }
};
