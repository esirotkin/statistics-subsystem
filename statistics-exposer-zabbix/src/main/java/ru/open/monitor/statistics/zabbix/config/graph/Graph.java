package ru.open.monitor.statistics.zabbix.config.graph;

import java.util.Objects;

public abstract class Graph<I extends GraphItem<?>> implements Comparable<Graph<I>> {
    private final String name;
    private final int width;
    private final int height;
    private final I[] graphItems;
    private GraphType graphType = GraphType.NORMAL;
    private float percentLeft;
    private float percentRight;
    private boolean show3D;
    private boolean showLegend = true;
    private boolean showWorkPeriod = true;
    private float yAxisMax = 100.0f;
    private float yAxisMin;
    private GraphItem yMaxItem;
    private ValueCalculationMethod yMaxType = ValueCalculationMethod.CALCULATED;
    private GraphItem yMinItem;
    private ValueCalculationMethod yMinType = ValueCalculationMethod.CALCULATED;

    protected Graph(String name, int width, int height, I ... graphItems) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.graphItems = graphItems;
    }

    public String getName() { return name; }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public I[] getGraphItems() { return graphItems; }

    public GraphType getGraphType() { return graphType; }
    public void setGraphType(GraphType graphType) { this.graphType = graphType; }

    public float getPercentLeft() { return percentLeft; }
    public void setPercentLeft(float percentLeft) { this.percentLeft = percentLeft; }

    public float getPercentRight() { return percentRight; }
    public void setPercentRight(float percentRight) { this.percentRight = percentRight; }

    public boolean isShow3D() { return show3D; }
    public void setShow3D(boolean show3D) { this.show3D = show3D; }

    public boolean isShowLegend() { return showLegend; }
    public void setShowLegend(boolean showLegend) { this.showLegend = showLegend; }

    public boolean isShowWorkPeriod() { return showWorkPeriod; }
    public void setShowWorkPeriod(boolean showWorkPeriod) { this.showWorkPeriod = showWorkPeriod; }

    public float getYAxisMax() { return yAxisMax; }
    public void setYAxisMax(float yAxisMax) { this.yAxisMax = yAxisMax; }

    public float getYAxisMin() { return yAxisMin; }
    public void setYAxisMin(float yAxisMin) { this.yAxisMin = yAxisMin; }

    public GraphItem getYMaxItem() { return yMaxItem; }
    public void setYMaxItem(GraphItem yMaxItem) { this.yMaxItem = yMaxItem;}

    public ValueCalculationMethod getYMaxType() { return yMaxType; }
    public void setYMaxType(ValueCalculationMethod yMaxType) { this.yMaxType = yMaxType; }

    public GraphItem getYMinItem() { return yMinItem; }
    public void setYMinItem(GraphItem yMinItem) { this.yMinItem = yMinItem; }

    public ValueCalculationMethod getYMinType() { return yMinType; }
    public void setYMinType(ValueCalculationMethod yMinType) { this.yMinType = yMinType; }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Graph graph = (Graph) o;
        return Objects.equals(name, graph.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(Graph anotherGraph) {
        return name.compareTo(anotherGraph.name);
    }

    @Override
    public String toString() { return name; }

}
