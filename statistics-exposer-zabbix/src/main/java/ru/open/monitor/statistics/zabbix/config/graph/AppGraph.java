package ru.open.monitor.statistics.zabbix.config.graph;

import ru.open.monitor.statistics.item.AppKind;
import ru.open.monitor.statistics.item.ItemKey;

public class AppGraph extends Graph<AppGraphItem> implements ItemKey<Graph<AppGraphItem>> {

    public AppGraph(String name, int width, int height, AppGraphItem ... graphItems) {
        super(name, width, height, graphItems);
    }

    @Override
    public AppKind getApplicationKind() {
        AppKind applicationKind = null;
        for (AppGraphItem graphItem : getGraphItems()) {
            applicationKind = graphItem.getApplicationKind();
        }
        return applicationKind;
    }

    @Override
    public String getItemName() {
        return getName();
    }

    @Override
    public String getServiceName() {
        return getApplicationKind().getApplicationName();
    }

}
