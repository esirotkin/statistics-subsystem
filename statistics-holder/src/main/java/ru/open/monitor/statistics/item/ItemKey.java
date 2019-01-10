package ru.open.monitor.statistics.item;

public interface ItemKey<T> extends Comparable<T> {

    AppKind getApplicationKind();

    String getItemName();

    String getServiceName();

}
