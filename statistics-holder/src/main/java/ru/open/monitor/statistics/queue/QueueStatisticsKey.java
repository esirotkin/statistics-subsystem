package ru.open.monitor.statistics.queue;

import ru.open.monitor.statistics.item.AppKind;
import ru.open.monitor.statistics.item.ItemKey;
import ru.open.monitor.statistics.item.ItemKind;

public enum QueueStatisticsKey implements ItemKey<QueueStatisticsKey> {
    QUEUED_EVENT_COUNT(ItemKind.QUEUED_EVENT_COUNT, "QueuedEvent"),
    PROCESSED_EVENT_NUMBER(ItemKind.PROCESSED_EVENT_NUMBER, "ProcessedEvent");

    private final ItemKind itemKind;
    private final String itemName;

    private QueueStatisticsKey(final ItemKind itemKind, final String itemName) {
        this.itemKind = itemKind;
        this.itemName = itemName;
    }

    public ItemKind getItemKind() {
        return itemKind;
    }

    @Override
    public AppKind getApplicationKind() {
        return itemKind.getApplicationKind();
    }

    @Override
    public String getItemName() {
        return itemName;
    }

    @Override
    public String getServiceName() {
        return getApplicationKind().getApplicationName();
    }

    @Override
    public String toString() {
        return itemName + "." + itemKind.getItemName();
    }
}
