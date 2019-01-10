package ru.open.monitor.statistics.zabbix.config.graph;

public enum GraphItemType {
    SIMPLE(0),
    GRAPH_SUM(2);

    private final int type;

    private GraphItemType(final int type) {
        this.type = type;
    }

    public static GraphItemType define(int type) {
        for (final GraphItemType graphItemType : values()) {
            if (graphItemType.type == type) {
                return graphItemType;
            }
        }
        return SIMPLE;
    }
}
