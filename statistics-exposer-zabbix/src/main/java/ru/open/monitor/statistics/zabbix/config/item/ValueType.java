package ru.open.monitor.statistics.zabbix.config.item;

public enum ValueType {
    NUMERIC_FLOAT(0),
    CHARACTER(1),
    LOG(2),
    NUMERIC_UNSIGNED(3),
    TEXT(4);

    private final int type;

    private ValueType(final int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static ValueType define(int type) {
        for (ValueType itemType : values()) {
            if (itemType.type == type) {
                return itemType;
            }
        }
        return NUMERIC_FLOAT;
    }
}
