package ru.open.monitor.statistics.zabbix.config.graph;

public enum DrawValueType {
    MINIMUM(1),
    AVERAGE(2),
    MAXIMUM(4),
    ALL(7),
    LAST(9);

    private final int type;

    private DrawValueType(final int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static DrawValueType define(int type) {
        for (final DrawValueType drawValueType : values()) {
            if (drawValueType.type == type) {
                return drawValueType;
            }
        }
        return AVERAGE;
    }
}
