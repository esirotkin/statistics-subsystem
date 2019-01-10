package ru.open.monitor.statistics.item;

public abstract class ItemUtil {

    public static final String PKG_MONITORING = "ru.open.monitor.statistics";

    public static final String APP_PROCESSED_EVENTS = "ProcessedEvents";
    public static final String APP_PUBLISHED_EVENTS = "PublishedEvents";
    public static final String APP_QUEUE_STATISTICS = "QueueStatistics";
    public static final String APP_EXECUTED_STATEMENTS = "ExecutedStatements";
    public static final String APP_PROCESSED_RESULTS = "ProcessedResultSets";

    public static String generateItemKey(final String className, final ItemKind itemKind) {
        return getSimpleClassName(className) + "." + itemKind.getItemName();
    }

    public static String generateEscapedItemKey(final String className, final ItemKind itemKind) {
        return escapeString(generateItemKey(className, itemKind));
    }

    public static String escapeString(final String string) {
        return string.replaceAll("\\.", "\\\\.");
    }

    public static String getEscapedClassName(final String className) {
        return escapeString(getCleanClassName(className));
    }

    public static String getSimpleClassName(final String className) {
        return getMainNamePart(getCleanClassName(className));
    }

    public static String getCleanClassName(final String className) {
        return className.trim().replaceAll("^(.+)(?:\\$\\$EnhancerBySpringCGLIB\\$\\$.+$|\\$\\$EnhancerByCGLIB\\$\\$.+$|\\$Proxy\\d+)", "$1");
    }

    public static String getMainNamePart(final String name) {
        return name != null ? name.replaceFirst("(?:.*\\.)([^.])", "$1") : null;
    }

    public static String prepareStatementName(final String sql) {
        String statement = sql.replaceAll("(?:\\{|\\}|\\(|\\)|\\?|:|,)", "").trim().replaceAll("\\s+", "_").toLowerCase();
        if (statement.length() > 192) {
            statement = statement.substring(0, 189).concat("...");
        }
        return statement;
    }

    public static double mathRound(double value, int decimals) {
        return Math.round(value * (Math.pow(10.0D, decimals))) / Math.pow(10.0D, decimals);
    }

    public static double toSeconds(long nanos) {
        return mathRound(nanos / 1000000000.0D, 9);
    }

    public static double toMillis(long nanos) {
        return mathRound(nanos / 1000000.0D, 6);
    }

    public static double toMicros(long nanos) {
        return mathRound(nanos / 1000.0D, 3);
    }

}
