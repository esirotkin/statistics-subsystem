package ru.open.monitor.statistics.zabbix.config.item;

public interface ExternalItemKey {

    String getApplication();

    String getKey();

    String getName();

    String getDescription();

    String getUnits();

    ValueType getValueType();

}
