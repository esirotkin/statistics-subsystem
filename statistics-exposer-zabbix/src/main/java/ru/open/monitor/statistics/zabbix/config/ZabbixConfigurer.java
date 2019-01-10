package ru.open.monitor.statistics.zabbix.config;

import ru.open.monitor.statistics.item.ItemKey;
import ru.open.monitor.statistics.item.ItemKind;
import ru.open.monitor.statistics.zabbix.config.item.ExternalItemKey;

public interface ZabbixConfigurer {

    int DEFAULT_API_PORT = 80;
    int DEFAULT_SENDER_PORT = 10051;
    int DEFAULT_AGENT_PORT = 10050;

    boolean isEnabled();

    void configure();

    boolean isConfigured();

    boolean configureItem(ItemKey itemKey);

    boolean configureItem(ExternalItemKey itemKey);

    boolean isInterestProcessedEvent(String eventClass);

    boolean isInterestPublishedEvent(String eventClass);

    boolean isInterestDatabaseStatement(String statement);

    String getZabbixItemKey(ItemKey itemKey, ItemKind itemKind);

    String getApplicationHost();

}
