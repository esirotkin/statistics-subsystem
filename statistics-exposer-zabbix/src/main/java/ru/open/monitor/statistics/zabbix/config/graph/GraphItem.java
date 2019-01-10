package ru.open.monitor.statistics.zabbix.config.graph;

import java.util.Objects;

public abstract class GraphItem<K extends Comparable> implements Comparable<GraphItem<K>> {
    private final K itemKey;
    private final String itemColor;
    private DrawValueType drawValueType;
    private DrawType drawType;
    private Integer sortOrder;
    private GraphItemType graphItemType;
    private YAxisSide yAxisSide;

    protected GraphItem(final K itemKey, final String itemColor) {
        this.itemKey = itemKey;
        this.itemColor = itemColor;
    }

    public K getItemKey() { return itemKey; }

    public String getItemColor() { return itemColor; }

    public DrawValueType getDrawValueType() { return drawValueType; }
    public void setDrawValueType(DrawValueType drawValueType) { this.drawValueType = drawValueType; }

    public DrawType getDrawType() { return drawType; }
    public void setDrawType(DrawType drawType) { this.drawType = drawType; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public GraphItemType getGraphItemType() { return graphItemType; }
    public void setGraphItemType(GraphItemType graphItemType) { this.graphItemType = graphItemType; }

    public YAxisSide getYAxisSide() { return yAxisSide; }
    public void setYAxisSide(YAxisSide yAxisSide) { this.yAxisSide = yAxisSide; }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GraphItem graphItem = (GraphItem) o;
        return Objects.equals(itemKey, graphItem.itemKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemKey);
    }

    @Override
    public int compareTo(GraphItem<K> anotherItem) {
        return itemKey.compareTo(anotherItem.itemKey);
    }

    @Override
    public String toString() { return itemKey.toString(); }

}
