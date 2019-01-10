package ru.open.monitor.statistics.zabbix.config.graph;

import ru.open.monitor.statistics.item.AppKind;
import ru.open.monitor.statistics.item.ItemKey;
import ru.open.monitor.statistics.item.ItemKind;
import ru.open.monitor.statistics.queue.QueueStatisticsKey;

public class AppGraphItem extends GraphItem<ItemKey> implements ItemKey<GraphItem<ItemKey>> {
    private final ItemKind itemKind;

    public AppGraphItem(final ItemKey itemKey, final ItemKind itemKind, final String itemColor) {
        super(itemKey, itemColor);
        this.itemKind = itemKind;
    }

    public AppGraphItem(final QueueStatisticsKey itemKey, final String itemColor) {
        this(itemKey, itemKey.getItemKind(), itemColor);
    }

    public ItemKind getItemKind() {
        return itemKind;
    }

    @Override
    public AppKind getApplicationKind() {
        return getItemKey().getApplicationKind();
    }

    @Override
    public String getItemName() {
        return getItemKey().getItemName();
    }

    @Override
    public String getServiceName() {
        return getItemKey().getServiceName();
    }

}
