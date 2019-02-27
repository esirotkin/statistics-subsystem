package ru.open.monitor.statistics.zabbix.config;

import static ru.open.monitor.statistics.item.ItemUtil.APP_EXECUTED_STATEMENTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PROCESSED_EVENTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PROCESSED_RESULTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PUBLISHED_EVENTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_QUEUE_STATISTICS;
import static ru.open.monitor.statistics.item.ItemUtil.PKG_MONITORING;
import static ru.open.monitor.statistics.item.ItemUtil.generateEscapedItemKey;
import static ru.open.monitor.statistics.item.ItemUtil.getEscapedClassName;
import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import io.github.hengyunabc.zabbix.api.SimpleRequest;
import io.github.hengyunabc.zabbix.api.SimpleRequestBuilder;

import ru.open.monitor.statistics.database.ExecutedStatement;
import ru.open.monitor.statistics.database.ProcessedResult;
import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.PublishedEvent;
import ru.open.monitor.statistics.event.StatisticsProvider;
import ru.open.monitor.statistics.item.ItemKey;
import ru.open.monitor.statistics.item.ItemKind;
import ru.open.monitor.statistics.queue.QueueStatisticsKey;
import ru.open.monitor.statistics.zabbix.config.graph.AppGraph;
import ru.open.monitor.statistics.zabbix.config.graph.AppGraphItem;
import ru.open.monitor.statistics.zabbix.config.graph.DrawType;
import ru.open.monitor.statistics.zabbix.config.graph.DrawValueType;
import ru.open.monitor.statistics.zabbix.config.graph.Graph;
import ru.open.monitor.statistics.zabbix.config.graph.GraphItem;
import ru.open.monitor.statistics.zabbix.config.graph.GraphItemType;
import ru.open.monitor.statistics.zabbix.config.graph.GraphType;
import ru.open.monitor.statistics.zabbix.config.graph.ValueCalculationMethod;
import ru.open.monitor.statistics.zabbix.config.graph.YAxisSide;
import ru.open.monitor.statistics.zabbix.config.item.ExternalItemKey;
import ru.open.monitor.statistics.zabbix.config.item.ValueType;

public class ZabbixConfigurerImpl extends DefaultZabbixApi implements ZabbixConfigurer {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixConfigurerImpl.class);

    private static final String ZABBIX_TEMPLATE_PATH = "/templates/";
    private static final String DEFAULT_JVM_TEMPLATE = "template-jvm-generic.xml";

    private static final String APP_JVM = "JVM", APP_JVM_KEY = "APP_JVM_ID",
                                HOST_KEY = "HOST_ID", JMX_IF_KEY = "HOST_IF",
                                APP_PROCESSED_EVENTS_KEY = "APP_PROCESSED_EVENTS_ID", APP_PUBLISHED_EVENTS_KEY = "APP_PUBLISHED_EVENTS_ID",
                                APP_QUEUE_STATISTICS_KEY = "APP_QUEUE_STATISTICS_ID",
                                APP_EXECUTED_STATEMENTS_KEY = "APP_EXECUTED_STATEMENTS_ID", APP_PROCESSED_RESULTS_KEY = "APP_PROCESSED_RESULTS_ID";

    private static final Map<String, String> DEFAULT_APP_NAMES = new HashMap<String, String>() {{
        put(APP_PROCESSED_EVENTS_KEY, APP_PROCESSED_EVENTS);
        put(APP_PUBLISHED_EVENTS_KEY, APP_PUBLISHED_EVENTS);
        put(APP_QUEUE_STATISTICS_KEY, APP_QUEUE_STATISTICS);
        put(APP_EXECUTED_STATEMENTS_KEY, APP_EXECUTED_STATEMENTS);
        put(APP_PROCESSED_RESULTS_KEY, APP_PROCESSED_RESULTS);
    }};

    private enum InterfaceType {
        ZBX(1), SNMP(2), IPMI(3), JMX(4);

        private final int type;

        private InterfaceType(final int type) {
            this.type = type;
        }

        private int getType() {
            return type;
        }

        private static InterfaceType define(int type) {
            for (final InterfaceType interfaceType : values()) {
                if (interfaceType.type == type) {
                    return interfaceType;
                }
            }
            return InterfaceType.ZBX;
        }
    };

    private enum ItemType {
        ZABBIX_AGENT(0), SNMPv1_AGENT(1), ZABBIX_TRAPPER(2), SIMPLE_CHECK(3), SNMPv2_AGENT(4), ZABBIX_INTERNAL(5), SNMPv3_AGENT(6),
        ZABBIX_AGENT_ACTIVE(7), ZABBIX_AGGREGATE(8), WEB_ITEM(9), EXTERNAL_CHECK(10), DATABASE_MONITOR(11), IPMI_AGENT(12), SSH_AGENT(13),
        TELNET_AGENT(14), CALCULATED(15), JMX_AGENT(16), SNMP_TRAP(17), DEPENDENT_ITEM(18);

        private final int type;

        private ItemType(final int type) {
            this.type = type;
        }

        private int getType() {
            return type;
        }

        @SuppressWarnings("unused")
        private static ItemType define(int type) {
            for (final ItemType itemType : values()) {
                if (itemType.type == type) {
                    return itemType;
                }
            }
            return ItemType.ZABBIX_AGENT;
        }
    };

    private enum ZabbixVersion {
        V_1_8("1\\.8\\..*"),
        V_2_0("2\\.0\\..*"),
        V_2_2("2\\.2\\..*"),
        V_2_4("2\\.4\\..*"),
        V_3_0("3\\.0\\..*"),
        V_3_2("3\\.2\\..*"),
        V_3_4("3\\.4\\..*"),
        V_4_0("4\\.0\\..*"),
        V_NEW("(?:4\\.[1-9]\\..*|\\d*?[5-9]\\.\\d+\\..*)");

        private final String pattern;

        private ZabbixVersion(final String pattern) {
            this.pattern = pattern;
        }

        private boolean supportFeaturesOf(final ZabbixVersion version) {
            return ordinal() >= version.ordinal();
        }

        private String branch() {
            return name().substring(2).replace("_", ".");
        }

        private static ZabbixVersion define(String version) {
            for (final ZabbixVersion zabbixVersion : values()) {
                if (version != null && version.matches(zabbixVersion.pattern)) {
                    return zabbixVersion;
                }
            }
            return V_1_8;
        }
    };

    private static class TemplateGraphItem extends GraphItem<String> {
        private TemplateGraphItem(final String itemKey, final String itemColor) {
            super(itemKey, itemColor);
        }
    };

    private static class TemplateGraph extends Graph<TemplateGraphItem> {
        private TemplateGraph(String name, int width, int height, TemplateGraphItem ... graphItems) {
            super(name, width, height, graphItems);
        }
    };

    private final AppGraph queueStatisticsGraph = new AppGraph("Queue Bandwidth", 900, 200,
                                                               new AppGraphItem(QueueStatisticsKey.PROCESSED_EVENT_NUMBER, "00aa00"),
                                                               new AppGraphItem(QueueStatisticsKey.QUEUED_EVENT_COUNT, "ff0000"));

    private final Set<ItemKey> configuredItemKeys = new CopyOnWriteArraySet<>();
    private final Set<ExternalItemKey> configuredExtItemKeys = new CopyOnWriteArraySet<>();
    private final Map<String, String> configurationContext = new ConcurrentHashMap<>();

    private StatisticsProvider statisticsProvider;

    @Value("${com.sun.management.jmxremote.port}")
    private Integer jmxRemotePort;
    @Value("${statistics.monitor.zabbix.jmx.delay:30}")
    private Integer jmxItemDelay;

    @Value("${statistics.monitor.zabbix.agent.app:${spring.application.name}}")
    private String zabbixAgentApplication;
    @Value("#{zabbixAgentAddress.hostIP}")
    private String zabbixAgentHost;
    @Value("#{zabbixAgentAddress.hostName}")
    private String zabbixAgentHostName;
    @Value("${statistics.monitor.zabbix.server.host}")
    private String zabbixServerHost;
    @Value("${statistics.monitor.zabbix.api.port:" + DEFAULT_API_PORT + "}")
    private Integer zabbixServerPort;
    @Value("${statistics.monitor.zabbix.server.user}")
    private String zabbixUser;
    @Value("${statistics.monitor.zabbix.server.password}")
    private String zabbixPassword;
    @Value("${statistics.monitor.zabbix.host.group}")
    private String zabbixHostGroup;

    @Value("${statistics.monitor.zabbix.applyTemplates:false}")
    private boolean applyZabbixTemplates;
    @Value("#{'${statistics.monitor.zabbix.templates:}'.split(',')}")
    private List<String> zabbixTemplates;

    @Value("#{'${statistics.monitor.zabbix.interestProcessedEvents:}'.split(',')}")
    private List<String> interestProcessedEventsZabbix;
    @Value("#{'${statistics.monitor.zabbix.interestPublishedEvents:}'.split(',')}")
    private List<String> interestPublishedEventsZabbix;

    @Value("#{'${statistics.monitor.jmx.interestProcessedEvents:}'.split(',')}")
    private List<String> interestProcessedEventsJmx;
    @Value("#{'${statistics.monitor.jmx.interestPublishedEvents:}'.split(',')}")
    private List<String> interestPublishedEventsJmx;

    @Value("${statistics.monitor.zabbix.includeEventsExposedToJmx:false}")
    private boolean includeEventsExposedToJmx;

    @Value("#{'${statistics.monitor.zabbix.interestDatabaseStatements:}'.split(',')}")
    private List<String> interestDbStatementsZabbix;

    @Value("#{'${statistics.monitor.zabbix.protectedItemNames:}'.split(',')}")
    private List<String> protectedItemNames;
    @Value("${statistics.monitor.zabbix.deleteObsoleteItems:true}")
    private boolean deleteObsoleteItems;
    @Value("${statistics.monitor.zabbix.numberOfEmptyDaysForObsoleteItem:90}")
    private int numberOfEmptyDaysForObsoleteItem;

    @Value("${statistics.monitor.zabbix.enable:false}")
    private boolean enableZabbix;
    @Value("${statistics.monitor.zabbix.numberOfConfigurationAttempts:5}")
    private int numberOfConfigurationAttempts;

    @Value("${statistics.monitor.zabbix.configureJvmMonitor:false}")
    private boolean configureJvmMonitor;

    private ZabbixVersion zabbixVersion;

    private volatile String zabbixHost;

    private volatile boolean configured;

    public ZabbixConfigurerImpl(final String zabbixApiUrl) {
        super(zabbixApiUrl);
    }

    public void setStatisticsProvider(StatisticsProvider statisticsProvider) {
        this.statisticsProvider = statisticsProvider;
    }

    @Override
    public boolean isEnabled() {
        return enableZabbix;
    }

    @Override
    @PostConstruct
    public void init() {
        configuredItemKeys.clear();
        clearInterestEvents();
        clearTemplates();
        clearProtectedItemNames();

        if (enableZabbix) {
            super.init();
            String apiVersion = apiVersion();
            zabbixVersion = ZabbixVersion.define(apiVersion);
            if (zabbixVersion.supportFeaturesOf(ZabbixVersion.V_2_0)) {
                if (super.login(zabbixUser, zabbixPassword)) {
                    LOG.info("Connected to the Zabbix Server API version {} ({}) on {}.", zabbixVersion.branch(), apiVersion, zabbixServerHost);
                } else {
                    enableZabbix = false;
                    LOG.error("Failed to authenticate in Zabbix Server ({}) on {}!", apiVersion, zabbixServerHost);
                }
            } else {
                LOG.error("Unsupported Zabbix version {} ({}) on {}!", zabbixVersion.branch(), apiVersion, zabbixServerHost);
            }
        }
    }

    private void clearInterestEvents() {
        final List<Integer> emptyValueIndexes = new ArrayList<>();

        for (int valueIndex = 0; valueIndex < interestProcessedEventsZabbix.size(); valueIndex++) {
            if (!StringUtils.hasText(interestProcessedEventsZabbix.get(valueIndex))) {
                emptyValueIndexes.add(valueIndex);
            }
        }
        for (int emptyValueIndex : emptyValueIndexes) {
            interestProcessedEventsZabbix.remove(emptyValueIndex);
        }
        emptyValueIndexes.clear();
        LOG.debug("Interest PROCESSED events: {}", interestProcessedEventsZabbix);

        for (int valueIndex = 0; valueIndex < interestPublishedEventsZabbix.size(); valueIndex++) {
            if (!StringUtils.hasText(interestPublishedEventsZabbix.get(valueIndex))) {
                emptyValueIndexes.add(valueIndex);
            }
        }
        for (int emptyValueIndex : emptyValueIndexes) {
            interestPublishedEventsZabbix.remove(emptyValueIndex);
        }
        emptyValueIndexes.clear();
        LOG.debug("Interest PUBLISHED events: {}", interestPublishedEventsZabbix);

        for (int valueIndex = 0; valueIndex < interestDbStatementsZabbix.size(); valueIndex++) {
            if (!StringUtils.hasText(interestDbStatementsZabbix.get(valueIndex))) {
                emptyValueIndexes.add(valueIndex);
            }
        }
        for (int emptyValueIndex : emptyValueIndexes) {
            interestDbStatementsZabbix.remove(emptyValueIndex);
        }
        emptyValueIndexes.clear();
        LOG.debug("Interest DB statements: {}", interestDbStatementsZabbix);
    }

    private void clearTemplates() {
        final List<Integer> emptyValueIndexes = new ArrayList<>();

        for (int valueIndex = 0; valueIndex < zabbixTemplates.size(); valueIndex++) {
            if (!StringUtils.hasText(zabbixTemplates.get(valueIndex))) {
                emptyValueIndexes.add(valueIndex);
            }
        }
        for (int emptyValueIndex : emptyValueIndexes) {
            zabbixTemplates.remove(emptyValueIndex);
        }
        LOG.debug("Zabbix templates: {}", zabbixTemplates);
    }

    private void clearProtectedItemNames() {
        final List<Integer> emptyValueIndexes = new ArrayList<>();

        for (int valueIndex = 0; valueIndex < protectedItemNames.size(); valueIndex++) {
            if (!StringUtils.hasText(protectedItemNames.get(valueIndex))) {
                emptyValueIndexes.add(valueIndex);
            }
        }
        for (int emptyValueIndex : emptyValueIndexes) {
            protectedItemNames.remove(emptyValueIndex);
        }
        LOG.debug("Protected item names: {}", protectedItemNames);
    }

    @Override
    @PreDestroy
    public void destroy() {
        checkForObsoleteItems(configurationContext);
        super.destroy();
    }

    @Override
    public String getApplicationHost() {
        return zabbixHost;
    }

    protected String getTrapperHosts() {
        return "";
    }

    @Override
    public boolean isInterestProcessedEvent(final String eventClass) {
        if (!interestProcessedEventsZabbix.isEmpty()) {
            return interestProcessedEventsZabbix.contains(eventClass);
        } else {
            return true;
        }
    }

    @Override
    public boolean isInterestPublishedEvent(final String eventClass) {
        if (!interestPublishedEventsZabbix.isEmpty()) {
            return interestPublishedEventsZabbix.contains(eventClass);
        } else {
            return true;
        }
    }

    @Override
    public boolean isInterestDatabaseStatement(final String statement) {
        if (!interestDbStatementsZabbix.isEmpty()) {
            return interestDbStatementsZabbix.contains(statement);
        } else {
            return true;
        }
    }

    @Override
    public String getZabbixItemKey(final ItemKey itemKey, final ItemKind itemKind) {
        switch (itemKind.getApplicationKind()) {
            case PROCESSED_EVENTS: return getProcessedEventKey((ProcessedEvent.Key) itemKey, itemKind);
            case PUBLISHED_EVENTS: return getPublishedEventKey((PublishedEvent.Key) itemKey, itemKind);
            case QUEUE_STATISTICS: return getQueueStatisticsKey((QueueStatisticsKey) itemKey);
            case EXECUTED_STATEMENTS: return getExecutedStatementKey((ExecutedStatement.Key) itemKey, itemKind);
            case PROCESSED_RESULTS: return getProcessedResultKey((ProcessedResult.Key) itemKey, itemKind);
        }
        return null;
    }

    private String getProcessedEventKey(final ProcessedEvent.Key key, final ItemKind itemKind) {
        switch (itemKind) {
            case PROCESSED_EVENT_COUNT:
            case PROCESSED_EVENT_D_AVG:
                if (isInterestProcessedEvent(key.getEventClass())) {
                    return zabbixAgentApplication + "." + APP_PROCESSED_EVENTS + "." + getSimpleClassName(key.getEventClass()) + "." + getSimpleClassName(key.getHandlerClass()) + "." + itemKind.getItemName();
                } else {
                    return getProcessedEventJmxKey(key, itemKind);
                }
            default:
                return getProcessedEventJmxKey(key, itemKind);
        }
    }

    private String getProcessedEventJmxKey(final ProcessedEvent.Key key, final ItemKind itemKind) {
        return "jmx[\"" + PKG_MONITORING + ":name=" + APP_PROCESSED_EVENTS + "\",\"" + getEscapedClassName(key.getEventClass()) + "." + generateEscapedItemKey(key.getHandlerClass(), itemKind) + "\"]";
    }

    private String getProcessedEventName(final ProcessedEvent.Key key, final ItemKind itemKind) {
        return "Processed." + getSimpleClassName(key.getHandlerClass()) + "." + getSimpleClassName(key.getEventClass()) + "." + itemKind.getItemName();
    }

    private String getProcessedEventDescription(final ProcessedEvent.Key key, final ItemKind itemKind) {
        switch (itemKind) {
            case PROCESSED_EVENT_COUNT:
                return "The count of processed by " + getSimpleClassName(key.getHandlerClass()) + " events of type " + getSimpleClassName(key.getEventClass() + " per second");
            case PROCESSED_EVENT_D_MIN:
                return "The minimum time of processed by " + getSimpleClassName(key.getHandlerClass()) + " events of type " + getSimpleClassName(key.getEventClass());
            case PROCESSED_EVENT_D_AVG:
                return "The average time of processed by " + getSimpleClassName(key.getHandlerClass()) + " events of type " + getSimpleClassName(key.getEventClass());
            case PROCESSED_EVENT_D_MAX:
                return "The maximum time of processed by " + getSimpleClassName(key.getHandlerClass()) + " events of type " + getSimpleClassName(key.getEventClass());
            case PROCESSED_EVENT_D_LAST:
                return "The last processing time of processed by " + getSimpleClassName(key.getHandlerClass()) + " events of type " + getSimpleClassName(key.getEventClass());
        }
        return null;
    }

    private String getPublishedEventKey(final PublishedEvent.Key key, final ItemKind itemKind) {
        switch (itemKind) {
            case PUBLISHED_EVENT_COUNT:
                if (isInterestPublishedEvent(key.getEventClass())) {
                    return zabbixAgentApplication + "." + APP_PUBLISHED_EVENTS + "." + getSimpleClassName(key.getEventClass()) + "." + getSimpleClassName(key.getPublisherClass()) + "." + itemKind.getItemName();
                } else {
                    return getPublishedEventJmxKey(key, itemKind);
                }
            default:
                return getPublishedEventJmxKey(key, itemKind);
        }
    }

    private String getPublishedEventJmxKey(final PublishedEvent.Key key, final ItemKind itemKind) {
        return "jmx[\"" + PKG_MONITORING + ":name=" + APP_PUBLISHED_EVENTS + "\",\"" + getEscapedClassName(key.getEventClass()) + "." + generateEscapedItemKey(key.getPublisherClass(), itemKind) + "\"]";
    }

    private String getPublishedEventName(final PublishedEvent.Key key, final ItemKind itemKind) {
        return "Published." + getSimpleClassName(key.getPublisherClass()) + "." + getSimpleClassName(key.getEventClass()) + "." + itemKind.getItemName();
    }

    private String getPublishedEventDescription(final PublishedEvent.Key key, final ItemKind itemKind) {
        switch (itemKind) {
            case PUBLISHED_EVENT_COUNT:
                return "The count of published by " + getSimpleClassName(key.getPublisherClass()) + " events of type " + getSimpleClassName(key.getEventClass() + " per second");
        }
        return null;
    }

    private String getQueueStatisticsKey(final QueueStatisticsKey key) {
        return zabbixAgentApplication + "." + key.getServiceName() + "." + key.getItemName() + "." + key.getItemKind().getItemName();
    }

    private String getQueueStatisticsName(final QueueStatisticsKey key) {
        return key.getServiceName() + "." + key.getItemName() + "." + key.getItemKind().getItemName();
    }

    private String getQueueStatisticsDescription(final QueueStatisticsKey key) {
        switch (key) {
            case QUEUED_EVENT_COUNT: return "The number of queued events in the processing queue";
            case PROCESSED_EVENT_NUMBER: return "The number of processed events by the processing queue per second";
        }
        return null;
    }

    private String getExecutedStatementKey(final ExecutedStatement.Key key, final ItemKind itemKind) {
        return zabbixAgentApplication + "." + APP_EXECUTED_STATEMENTS + "." + key.getStatement() + "." + itemKind.getItemName();
    }

    private String getExecutedStatementName(final ExecutedStatement.Key key, final ItemKind itemKind) {
        return APP_EXECUTED_STATEMENTS + "." + key.getStatement().toUpperCase() + "." + itemKind.getItemName();
    }

    private String getExecutedStatementDescription(final ExecutedStatement.Key key, final ItemKind itemKind) {
        switch (itemKind) {
            case STATEMENT_COUNT:
                return "The count of statement executions per second";
            case STATEMENT_D_MIN:
                return "The minimum time of statement execution";
            case STATEMENT_D_AVG:
                return "The average time of statement execution";
            case STATEMENT_D_MAX:
                return "The maximum time of statement execution";
            case STATEMENT_D_LAST:
                return "The last time (duration) of statement execution";
        }
        return null;
    }

    private String getProcessedResultKey(final ProcessedResult.Key key, final ItemKind itemKind) {
        return zabbixAgentApplication + "." + APP_PROCESSED_RESULTS + "." + key.getStatement() + "." + key.getResultSet() + "." + itemKind.getItemName();
    }

    private String getProcessedResultName(final ProcessedResult.Key key, final ItemKind itemKind) {
        return APP_PROCESSED_RESULTS + "." + key.getStatement().toUpperCase() + "." + key.getResultSet().toUpperCase() + "." + itemKind.getItemName();
    }

    private String getProcessedResultDescription(final ProcessedResult.Key key, final ItemKind itemKind) {
        switch (itemKind) {
            case RESULT_COUNT:
                return "The count of results processed per second";
            case RESULT_D_MIN:
                return "The minimum time of result processing";
            case RESULT_D_AVG:
                return "The average time of result processing";
            case RESULT_D_MAX:
                return "The maximum time of result processing";
            case RESULT_D_LAST:
                return "The last time (duration) of result processing";
        }
        return null;
    }

    private String prepareApplicationAwareName(final String name) {
        return zabbixAgentApplication + "::" + name;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public void configure() {
        if (enableZabbix && zabbixVersion.supportFeaturesOf(ZabbixVersion.V_2_0)) {
            LOG.info("Configuring the Zabbix Server to monitor the {} application ...", zabbixAgentApplication);
            new Thread(new ConfigurationTask(), "zabbixConfigurer").run();
        }
    }

    private class ConfigurationTask implements Runnable {
        @Override public void run() {
            for (int i = 1; !configured && i <= numberOfConfigurationAttempts; i++) {
                try {
                    LOG.debug("Attempt to configure the Zabbix Sender ...");
                    configureHost(configurationContext);
                    configureTemplates(configurationContext);
                    configureApps(configurationContext);
                    configureItems(configurationContext);
                    configured = true;
                    LOG.info("The Zabbix Sender is successfully configured!");
                } catch (Throwable t) {
                    if (i < numberOfConfigurationAttempts) {
                        LOG.warn("Failed to configure the Zabbix Sender!", t);
                        try { Thread.sleep(500); } catch (InterruptedException ie) {}
                    } else {
                        LOG.error("Failed to configure the Zabbix Sender!", t);
                    }
                }
            }
        }
    };

    private String getApplicationKey(final ExternalItemKey itemKey) {
        return "APP_" + itemKey.getApplication().trim().toUpperCase() + "_ID";
    }

    @Override
    public boolean configureItem(final ExternalItemKey itemKey) {
        if (!enableZabbix || !zabbixVersion.supportFeaturesOf(ZabbixVersion.V_2_0)) {
            return false;
        }

        if (configuredExtItemKeys.contains(itemKey)) {
            return true;
        }

        if (configureApplication(configurationContext, itemKey.getApplication(), getApplicationKey(itemKey))) {
            return configureItem(configurationContext, itemKey);
        } else {
            return false;
        }
    }

    @Override
    public boolean configureItem(final ItemKey itemKey) {
        if (!enableZabbix || !zabbixVersion.supportFeaturesOf(ZabbixVersion.V_2_0)) {
            return false;
        }

        if (configuredItemKeys.contains(itemKey)) {
            return true;
        }

        if (itemKey instanceof AppGraph) {
            return configureGraph(configurationContext, (AppGraph) itemKey);
        }

        boolean configured = true;
        switch (itemKey.getApplicationKind()) {
            case PROCESSED_EVENTS: {
                if (!configurationContext.containsKey(APP_PROCESSED_EVENTS_KEY)) {
                    configured &= configureApplication(configurationContext, APP_PROCESSED_EVENTS, APP_PROCESSED_EVENTS_KEY);
                }
                configured &= configureItem(configurationContext, itemKey, ItemKind.PROCESSED_EVENT_COUNT);
                              configureItem(configurationContext, itemKey, ItemKind.PROCESSED_EVENT_D_MIN);
                configured &= configureItem(configurationContext, itemKey, ItemKind.PROCESSED_EVENT_D_AVG);
                              configureItem(configurationContext, itemKey, ItemKind.PROCESSED_EVENT_D_MAX);
                              configureItem(configurationContext, itemKey, ItemKind.PROCESSED_EVENT_D_LAST);
            }; break;
            case PUBLISHED_EVENTS: {
                if (!configurationContext.containsKey(APP_PUBLISHED_EVENTS_KEY)) {
                    configured &= configureApplication(configurationContext, APP_PUBLISHED_EVENTS, APP_PUBLISHED_EVENTS_KEY);
                }
                configured &= configureItem(configurationContext, itemKey, ItemKind.PUBLISHED_EVENT_COUNT);
            }; break;
            case QUEUE_STATISTICS: {
                if (!configurationContext.containsKey(APP_QUEUE_STATISTICS_KEY)) {
                    configured &= configureApplication(configurationContext, APP_QUEUE_STATISTICS, APP_QUEUE_STATISTICS_KEY);
                }
                configured &= configureItem(configurationContext, itemKey, ((QueueStatisticsKey) itemKey).getItemKind());
            }; break;
            case EXECUTED_STATEMENTS: {
                if (!configurationContext.containsKey(APP_EXECUTED_STATEMENTS_KEY)) {
                    configured &= configureApplication(configurationContext, APP_EXECUTED_STATEMENTS, APP_EXECUTED_STATEMENTS_KEY);
                }
                configured &= configureItem(configurationContext, itemKey, ItemKind.STATEMENT_COUNT);
                configured &= configureItem(configurationContext, itemKey, ItemKind.STATEMENT_D_AVG);
            }; break;
            case PROCESSED_RESULTS: {
                if (!configurationContext.containsKey(APP_PROCESSED_RESULTS_KEY)) {
                    configured &= configureApplication(configurationContext, APP_PROCESSED_RESULTS, APP_PROCESSED_RESULTS_KEY);
                }
                configured &= configureItem(configurationContext, itemKey, ItemKind.RESULT_COUNT);
                configured &= configureItem(configurationContext, itemKey, ItemKind.RESULT_D_AVG);
            }; break;
        }

        configureQueueStatisticsGraph(configurationContext);

        return configured;
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/hostinterface/object">HostInterface object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/hostinterface/get">hostinterface.get method description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/host/object">Host object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/host/get">host.get method description</a>
     */
    private void configureHost(final Map<String, String> context) {
        LOG.info("Configuring the Zabbix Server to monitor the {} application on {} ({}:{}) ...", zabbixAgentApplication, zabbixAgentHostName, zabbixAgentHost, jmxRemotePort);

        if (!context.containsKey(HOST_KEY) || !context.containsKey(JMX_IF_KEY)) {
            JSONObject getHostInterfaceFilter = new JSONObject();
            getHostInterfaceFilter.put("ip", zabbixAgentHost);
            Request getHostInterfaceRequest = RequestBuilder.newBuilder().method("hostinterface.get")
                                                            .paramEntry("filter", getHostInterfaceFilter)
                                                            .paramEntry("output", "extend")
                                                            .build();
            JSONObject getHostInterfaceResponse = call(getHostInterfaceRequest);

            boolean hasPrimaryZbxInterface = false, hasPrimaryJmxInterface = false;
            JSONArray getHostInterfaceResult = getHostInterfaceResponse.getJSONArray("result");
            for (int i = 0; i < getHostInterfaceResult.size(); i++) {
                JSONObject hostInterface = getHostInterfaceResult.getJSONObject(i);

                String hostId = hostInterface.getString("hostid");
                Request getHostRequest = RequestBuilder.newBuilder().method("host.get")
                                                       .paramEntry("hostids", hostId)
                                                       .build();
                JSONObject getHostResponse = call(getHostRequest);
                zabbixHost = getHostResponse.getJSONArray("result").getJSONObject(0).getString("host");
                LOG.debug("Configured the Zabbix host {} with id {}.", zabbixHost, hostId);

                context.put(HOST_KEY, hostId);
                boolean mainInterface = hostInterface.getBoolean("main");
                InterfaceType hostInterfaceType = InterfaceType.define(hostInterface.getIntValue("type"));
                if (hostInterfaceType == InterfaceType.JMX) {
                    if (mainInterface) {
                        hasPrimaryJmxInterface = true;
                    }
                    Integer jmxPort = Integer.valueOf(hostInterface.getString("port"));
                    if (jmxRemotePort.intValue() == jmxPort.intValue()) {
                        context.put(JMX_IF_KEY, hostInterface.getString("interfaceid"));
                    }
                } else if (hostInterfaceType == InterfaceType.ZBX && mainInterface) {
                    hasPrimaryZbxInterface = true;
                }
            }

            if (!context.containsKey(HOST_KEY)) {
                createHost(context);
            } else if (!hasPrimaryZbxInterface) {
                createHostInterface(context, InterfaceType.ZBX, !hasPrimaryZbxInterface);
            }

            if (!context.containsKey(JMX_IF_KEY)) {
                createHostInterface(context, InterfaceType.JMX, !hasPrimaryJmxInterface);
            }
        }

        LOG.debug("Configured the host {} with the JMX interface {}.", context.get(HOST_KEY), context.get(JMX_IF_KEY));
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/hostgroup/object">HostGroup object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/hostgroup/get">hostgroup.get method description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/host/object">Host object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/host/create">host.create method description</a>
     */
    private void createHost(final Map<String, String> context) {
        LOG.info("Creating the host {} ({}) ...", zabbixAgentHostName, zabbixAgentHost);

        JSONObject getHostGroupFilter = new JSONObject();
        getHostGroupFilter.put("name", new JSONArray(Arrays.asList(zabbixHostGroup)));
        Request getHostGroupRequest = RequestBuilder.newBuilder().method("hostgroup.get")
                                                    .paramEntry("output", "extend")
                                                    .paramEntry("filter", getHostGroupFilter)
                                                    .build();
        JSONObject getHostGroupResponse = call(getHostGroupRequest);
        String hostGroupId = getHostGroupResponse.getJSONArray("result").getJSONObject(0).getString("groupid");
        LOG.debug("Configured the host group {}.", hostGroupId);

        JSONObject hostGroup = new JSONObject();
        hostGroup.put("groupid", hostGroupId);
        JSONObject hostInterfaceZbx = new JSONObject();
        hostInterfaceZbx.put("type", InterfaceType.ZBX.getType());
        hostInterfaceZbx.put("main", 1); // default
        hostInterfaceZbx.put("dns", zabbixAgentHostName);
        hostInterfaceZbx.put("ip", zabbixAgentHost);
        hostInterfaceZbx.put("useip", 1); // true
        hostInterfaceZbx.put("port", DEFAULT_AGENT_PORT);
        Request createHostRequest = RequestBuilder.newBuilder().method("host.create")
                                                  .paramEntry("host", zabbixAgentHostName)
                                                  .paramEntry("groups", new JSONArray(Arrays.asList(hostGroup)))
                                                  .paramEntry("interfaces", new JSONArray(Arrays.asList(hostInterfaceZbx)))
                                                  .build();
        JSONObject createHostResponse = call(createHostRequest);
        String hostId = createHostResponse.getJSONObject("result").getJSONArray("hostids").getString(0);
        context.put(HOST_KEY, hostId);

        configureHost(context);

        LOG.debug("Created the host {}.", hostId);
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/hostinterface/object">HostInterface object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/hostinterface/create">hostinterface.create method description</a>
     */
    private void createHostInterface(final Map<String, String> context, final InterfaceType interfaceType, final boolean main) {
        LOG.info("Creating the {} interface on the host {} ({}:{}) ...", interfaceType, zabbixAgentHostName, zabbixAgentHost, jmxRemotePort);

        Request createHostInterfaceRequest = RequestBuilder.newBuilder().method("hostinterface.create")
                                                           .paramEntry("hostid", context.get(HOST_KEY))
                                                           .paramEntry("main", main ? 1 : 0)
                                                           .paramEntry("type", interfaceType.getType())
                                                           .paramEntry("dns", zabbixAgentHostName)
                                                           .paramEntry("ip", zabbixAgentHost)
                                                           .paramEntry("useip", 1) // true
                                                           .paramEntry("port", interfaceType == InterfaceType.JMX ? jmxRemotePort : DEFAULT_AGENT_PORT)
                                                           .build();
        JSONObject createHostInterfaceResponse = call(createHostInterfaceRequest);
        String hostInterfaceId = createHostInterfaceResponse.getJSONObject("result").getJSONArray("interfaceids").getString(0);
        if (interfaceType == InterfaceType.JMX) {
            context.put(JMX_IF_KEY, hostInterfaceId);
        }

        LOG.debug("Created the {} interface {} on the host {}.", interfaceType, hostInterfaceId, context.get(HOST_KEY));
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/template/object">Template object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/template/get">template.get method description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/template/massadd">template.massadd method description</a>
     */
    private void configureTemplates(final Map<String, String> context) {
        if (!applyZabbixTemplates || zabbixTemplates.isEmpty()) {
            return;
        }

        LOG.info("Configuring the templates on {} ({}:{}) ...", zabbixAgentHostName, zabbixAgentHost, jmxRemotePort);

        JSONObject getTemplatesFilter = new JSONObject();
        getTemplatesFilter.put("host", new JSONArray((List) zabbixTemplates));
        Request getTemplatesRequest = RequestBuilder.newBuilder().method("template.get")
                                                    .paramEntry("output", "extend")
                                                    .paramEntry("filter", getTemplatesFilter)
                                                    .build();
        JSONObject getTemplatesResponse = call(getTemplatesRequest);
        JSONArray result = getTemplatesResponse.getJSONArray("result");
        List<String> templateIds = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            templateIds.add(result.getJSONObject(i).getString("templateid"));
        }

        JSONArray templates = new JSONArray();
        for (final String templateId : templateIds) {
            templates.add(new JSONObject(new HashMap<String, Object>() {{ put("templateid", templateId); }}));
        }
        Request massAddTemplateRequest = RequestBuilder.newBuilder().method("template.massadd")
                                                       .paramEntry("templates", templates)
                                                       .paramEntry("hosts", new JSONArray(Arrays.asList(new JSONObject(new HashMap<String, Object>() {{ put("hostid", context.get(HOST_KEY)); }}))))
                                                       .build();
        JSONObject massAddTemplatesResponse = call(massAddTemplateRequest);
        JSONArray resultTemplateIds = massAddTemplatesResponse.getJSONObject("result").getJSONArray("templateids");

        LOG.debug("Applied template ids: {}", Arrays.asList(resultTemplateIds.toArray(new String[resultTemplateIds.size()])));
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/application/object">Application object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/application/get">application.get method description</a>
     */
    private void configureApps(final Map<String, String> context) {
        LOG.debug("Configuring the Zabbix monitoring applications ...");

        if (!(context.containsKey(APP_JVM_KEY) || context.containsKey(APP_PROCESSED_EVENTS_KEY) || context.containsKey(APP_PUBLISHED_EVENTS_KEY) || context.containsKey(APP_EXECUTED_STATEMENTS_KEY) || context.containsKey(APP_PROCESSED_RESULTS_KEY))) {
            JSONObject getApplicationFilter = new JSONObject();
            getApplicationFilter.put("name", new JSONArray(Arrays.asList(prepareApplicationAwareName(APP_JVM),
                                                                         prepareApplicationAwareName(APP_PROCESSED_EVENTS),
                                                                         prepareApplicationAwareName(APP_PUBLISHED_EVENTS),
                                                                         prepareApplicationAwareName(APP_QUEUE_STATISTICS),
                                                                         prepareApplicationAwareName(APP_EXECUTED_STATEMENTS),
                                                                         prepareApplicationAwareName(APP_PROCESSED_RESULTS))));
            Request getApplicationRequest = RequestBuilder.newBuilder().method("application.get")
                                                          .paramEntry("output", "extend")
                                                          .paramEntry("hostids", context.get(HOST_KEY))
                                                          .paramEntry("sortfield", "name")
                                                          .paramEntry("filter", getApplicationFilter)
                                                          .build();
            JSONObject getApplicationResponse = call(getApplicationRequest);

            JSONArray getApplicationResult = getApplicationResponse.getJSONArray("result");
            for (int i = 0; i < getApplicationResult.size(); i++) {
                JSONObject application = getApplicationResult.getJSONObject(i);
                if (application.getString("name").equals(prepareApplicationAwareName(APP_JVM))) {
                    context.put(APP_JVM_KEY, application.getString("applicationid"));
                }
                if (application.getString("name").equals(prepareApplicationAwareName(APP_PROCESSED_EVENTS))) {
                    context.put(APP_PROCESSED_EVENTS_KEY, application.getString("applicationid"));
                }
                if (application.getString("name").equals(prepareApplicationAwareName(APP_PUBLISHED_EVENTS))) {
                    context.put(APP_PUBLISHED_EVENTS_KEY, application.getString("applicationid"));
                }
                if (application.getString("name").equals(prepareApplicationAwareName(APP_QUEUE_STATISTICS))) {
                    context.put(APP_QUEUE_STATISTICS_KEY, application.getString("applicationid"));
                }
                if (application.getString("name").equals(prepareApplicationAwareName(APP_EXECUTED_STATEMENTS))) {
                    context.put(APP_EXECUTED_STATEMENTS_KEY, application.getString("applicationid"));
                }
                if (application.getString("name").equals(prepareApplicationAwareName(APP_PROCESSED_RESULTS))) {
                    context.put(APP_PROCESSED_RESULTS_KEY, application.getString("applicationid"));
                }
            }
        }

        if (context.containsKey(APP_JVM_KEY)) {
            LOG.debug("Configured the Zabbix monitoring application {} {}.", APP_JVM, context.get(APP_JVM_KEY));
        } else {
            configureJvmMonitoring(context);
        }

        if (context.containsKey(APP_PROCESSED_EVENTS_KEY)) {
            LOG.debug("Configured the Zabbix monitoring application {} {}.", APP_PROCESSED_EVENTS, context.get(APP_PROCESSED_EVENTS_KEY));
        }
        if (context.containsKey(APP_PUBLISHED_EVENTS_KEY)) {
            LOG.debug("Configured the Zabbix monitoring application {} {}.", APP_PUBLISHED_EVENTS, context.get(APP_PUBLISHED_EVENTS_KEY));
        }

        if (context.containsKey(APP_QUEUE_STATISTICS_KEY)) {
            LOG.debug("Configured the Zabbix monitoring application {} {}.", APP_QUEUE_STATISTICS, context.get(APP_QUEUE_STATISTICS_KEY));
        }

        if (context.containsKey(APP_EXECUTED_STATEMENTS_KEY)) {
            LOG.debug("Configured the Zabbix monitoring application {} {}.", APP_EXECUTED_STATEMENTS, context.get(APP_EXECUTED_STATEMENTS_KEY));
        }
        if (context.containsKey(APP_PROCESSED_RESULTS_KEY)) {
            LOG.debug("Configured the Zabbix monitoring application {} {}.", APP_PROCESSED_RESULTS, context.get(APP_PROCESSED_RESULTS_KEY));
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/application/object">Application object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/application/get">application.get method description</a>
     */
    private boolean configureApplication(final Map<String, String> context, final String applicationName, final String applicationKey) {
        LOG.debug("Configuring the Zabbix monitoring application {} ({}) ...", applicationName, applicationKey);

        if (!context.containsKey(applicationKey)) {
            JSONObject getApplicationFilter = new JSONObject();
            getApplicationFilter.put("name", new JSONArray(Arrays.asList(prepareApplicationAwareName(applicationName))));
            Request getApplicationRequest = RequestBuilder.newBuilder().method("application.get")
                                                          .paramEntry("output", "extend")
                                                          .paramEntry("hostids", context.get(HOST_KEY))
                                                          .paramEntry("sortfield", "name")
                                                          .paramEntry("filter", getApplicationFilter)
                                                          .build();
            JSONObject getApplicationResponse = call(getApplicationRequest);

            JSONArray getApplicationResult = getApplicationResponse.getJSONArray("result");
            for (int i = 0; i < getApplicationResult.size(); i++) {
                JSONObject application = getApplicationResult.getJSONObject(i);
                if (application.getString("name").equals(prepareApplicationAwareName(applicationName))) {
                    context.put(applicationKey, application.getString("applicationid"));
                }
            }
        }

        if (context.containsKey(applicationKey)) {
            LOG.debug("Configured the Zabbix monitoring application {} {}.", applicationName, context.get(applicationKey));
            return true;
        } else {
            return createApplication(context, applicationName, applicationKey);
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/application/object">Application object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/application/create">application.create method description</a>
     */
    private boolean createApplication(final Map<String, String> context, final String applicationName, final String applicationKey) {
        LOG.info("Creating the {} application on the host {} ({}) ...", applicationName, zabbixAgentApplication, zabbixAgentHost);

        Request createApplicationRequest = RequestBuilder.newBuilder().method("application.create")
                                                         .paramEntry("hostid", context.get(HOST_KEY))
                                                         .paramEntry("name", prepareApplicationAwareName(applicationName))
                                                         .build();
        JSONObject createApplicationResponse = call(createApplicationRequest);
        String applicationId = createApplicationResponse.getJSONObject("result").getJSONArray("applicationids").getString(0);

        LOG.debug("Created the {} application {} on the host {}.", applicationName, applicationId, context.get(HOST_KEY));

        if (applicationId != null) {
            context.put(applicationKey, applicationId);
            return true;
        } else {
            return false;
        }
    }

    private void configureItems(final Map<String, String> context) {
        if (statisticsProvider != null) {
            LOG.debug("Configuring the {} application ...", APP_PROCESSED_EVENTS);
            final Set<String> interestProcessedEvents = new HashSet<>();
            if (includeEventsExposedToJmx) {
                interestProcessedEvents.addAll(interestProcessedEventsJmx);
            }
            interestProcessedEvents.addAll(interestProcessedEventsZabbix);

            for (final String eventClass : interestProcessedEvents) {
                for (final ProcessedEvent.Key key : statisticsProvider.getProcessedEventKeys(eventClass)) {
                    if (!configuredItemKeys.contains(key)) {
                        configureItem(context, key, ItemKind.PROCESSED_EVENT_COUNT);
                        configureItem(context, key, ItemKind.PROCESSED_EVENT_D_MIN);
                        configureItem(context, key, ItemKind.PROCESSED_EVENT_D_AVG);
                        configureItem(context, key, ItemKind.PROCESSED_EVENT_D_MAX);
                        configureItem(context, key, ItemKind.PROCESSED_EVENT_D_LAST);
                    }
                }
            }

            LOG.debug("Configuring the {} application ...", APP_PUBLISHED_EVENTS);
            final Set<String> interestPublishedEvents = new HashSet<>();
            interestPublishedEvents.addAll(interestPublishedEventsJmx);
            interestPublishedEvents.addAll(interestPublishedEventsZabbix);
            for (final String eventClass : interestPublishedEvents) {
                for (final PublishedEvent.Key key : statisticsProvider.getPublishedEventKeys(eventClass)) {
                    if (!configuredItemKeys.contains(key)) {
                        configureItem(context, key, ItemKind.PUBLISHED_EVENT_COUNT);
                    }
                }
            }
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/object">Item object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/get">item.get method description</a>
     */
    private boolean configureItem(final Map<String, String> context, final ExternalItemKey itemKey) {
        LOG.debug("Configuring the Zabbix monitoring item {} (in {}) of {} ...",
                  itemKey.getKey(), itemKey.getUnits(), itemKey.getApplication());

        final String zabbixItemKey = itemKey.getKey();
        JSONObject getItemFilter = new JSONObject();
        getItemFilter.put("key_", zabbixItemKey);
        Request getItemRequest = RequestBuilder.newBuilder().method("item.get")
                                               .paramEntry("output", "extend")
                                               .paramEntry("hostids", context.get(HOST_KEY))
                                               .paramEntry("sortfield", "name")
                                               .paramEntry("filter", getItemFilter)
                                               .build();
        JSONObject getItemResponse = call(getItemRequest);
        if (getItemResponse.getJSONArray("result").isEmpty()) {
            final String applicationId = context.get(getApplicationKey(itemKey));
            boolean created = createItem(context, applicationId, zabbixItemKey, itemKey.getName(), itemKey.getDescription(), itemKey.getUnits(), ItemType.ZABBIX_TRAPPER, itemKey.getValueType());
            if (created) {
                configuredExtItemKeys.add(itemKey);
            }
            return created;
        } else {
            String itemId = getItemResponse.getJSONArray("result").getJSONObject(0).getString("itemid");
            LOG.debug("Configured the Zabbix monitoring item {}.", itemId);

            if (itemId != null) {
                configuredExtItemKeys.add(itemKey);
                context.put(zabbixItemKey, itemId);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/object">Item object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/get">item.get method description</a>
     */
    private boolean configureItem(final Map<String, String> context, final ItemKey itemKey, final ItemKind itemKind) {
        LOG.debug("Configuring the Zabbix monitoring item kind {} of {} by {} ...",
                  itemKind,
                  getSimpleClassName(itemKey.getItemName()),
                  getSimpleClassName(itemKey.getServiceName()));

        final String zabbixItemKey = getZabbixItemKey(itemKey, itemKind);
        JSONObject getItemFilter = new JSONObject();
        getItemFilter.put("key_", zabbixItemKey);
        Request getItemRequest = RequestBuilder.newBuilder().method("item.get")
                                               .paramEntry("output", "extend")
                                               .paramEntry("hostids", context.get(HOST_KEY))
                                               .paramEntry("sortfield", "name")
                                               .paramEntry("filter", getItemFilter)
                                               .build();
        JSONObject getItemResponse = call(getItemRequest);
        if (getItemResponse.getJSONArray("result").isEmpty()) {
            boolean created = createItem(context, itemKey, itemKind);
            if (created) {
                configuredItemKeys.add(itemKey);
            }
            return created;
        } else {
            String itemId = getItemResponse.getJSONArray("result").getJSONObject(0).getString("itemid");
            LOG.debug("Configured the Zabbix monitoring item {}.", itemId);

            if (itemId != null) {
                configuredItemKeys.add(itemKey);
                context.put(zabbixItemKey, itemId);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean createItem(final Map<String, String> context, final ItemKey itemKey, final ItemKind itemKind) {
        LOG.info("Creating the {}.{}.{} item for application {} on the host {} ({}) ...",
                 getSimpleClassName(itemKey.getServiceName()),
                 getSimpleClassName(itemKey.getItemName()),
                 itemKind.getItemName(),
                 zabbixAgentApplication, zabbixAgentHostName, zabbixAgentHost);

        final String zabbixItemKey = getZabbixItemKey(itemKey, itemKind);
        String applicationId = null;
        String itemName = null;
        String itemDescription = null;
        String itemUnits = "eps"; // events per second
        ValueType itemValueType = ValueType.NUMERIC_FLOAT;
        ItemType itemType = ItemType.JMX_AGENT;
        switch (itemKind) {
            case PROCESSED_EVENT_D_AVG:
                if (isInterestProcessedEvent(itemKey.getItemName())) {
                    itemType = ItemType.ZABBIX_TRAPPER;
                }
            case PROCESSED_EVENT_D_MIN:
            case PROCESSED_EVENT_D_MAX:
            case PROCESSED_EVENT_D_LAST:
                applicationId = getApplicationId(context, APP_PROCESSED_EVENTS_KEY);
                itemName = getProcessedEventName((ProcessedEvent.Key) itemKey, itemKind);
                itemDescription = getProcessedEventDescription((ProcessedEvent.Key) itemKey, itemKind);
                itemUnits = "s"; // seconds
                break;
            case PROCESSED_EVENT_COUNT:
                applicationId = getApplicationId(context, APP_PROCESSED_EVENTS_KEY);
                itemName = getProcessedEventName((ProcessedEvent.Key) itemKey, itemKind);
                itemDescription = getProcessedEventDescription((ProcessedEvent.Key) itemKey, itemKind);
                if (isInterestProcessedEvent(itemKey.getItemName())) {
                    itemType = ItemType.ZABBIX_TRAPPER;
                }
                break;
            case PUBLISHED_EVENT_COUNT:
                applicationId = getApplicationId(context, APP_PUBLISHED_EVENTS_KEY);
                itemName = getPublishedEventName((PublishedEvent.Key) itemKey, itemKind);
                itemDescription = getPublishedEventDescription((PublishedEvent.Key) itemKey, itemKind);
                if (isInterestPublishedEvent(itemKey.getItemName())) {
                    itemType = ItemType.ZABBIX_TRAPPER;
                }
                break;
            case QUEUED_EVENT_COUNT:
                itemUnits = "pcs"; // pieces
                itemValueType = ValueType.NUMERIC_UNSIGNED;
            case PROCESSED_EVENT_NUMBER:
                applicationId = getApplicationId(context, APP_QUEUE_STATISTICS_KEY);
                itemName = getQueueStatisticsName((QueueStatisticsKey) itemKey);
                itemDescription = getQueueStatisticsDescription((QueueStatisticsKey) itemKey);
                itemType = ItemType.ZABBIX_TRAPPER;
                break;
            case STATEMENT_COUNT:
                applicationId = getApplicationId(context, APP_EXECUTED_STATEMENTS_KEY);
                itemName = getExecutedStatementName((ExecutedStatement.Key) itemKey, itemKind);
                itemDescription = getExecutedStatementDescription((ExecutedStatement.Key) itemKey, itemKind);
                itemType = ItemType.ZABBIX_TRAPPER;
                itemUnits = "eps"; // executions per second
                break;
            case STATEMENT_D_MIN:
            case STATEMENT_D_AVG:
            case STATEMENT_D_MAX:
            case STATEMENT_D_LAST:
                applicationId = getApplicationId(context, APP_EXECUTED_STATEMENTS_KEY);
                itemName = getExecutedStatementName((ExecutedStatement.Key) itemKey, itemKind);
                itemDescription = getExecutedStatementDescription((ExecutedStatement.Key) itemKey, itemKind);
                itemType = ItemType.ZABBIX_TRAPPER;
                itemUnits = "s"; // seconds
                break;
            case RESULT_COUNT:
                applicationId = getApplicationId(context, APP_PROCESSED_RESULTS_KEY);
                itemName = getProcessedResultName((ProcessedResult.Key) itemKey, itemKind);
                itemDescription = getProcessedResultDescription((ProcessedResult.Key) itemKey, itemKind);
                itemType = ItemType.ZABBIX_TRAPPER;
                itemUnits = "rps"; // records per second
                break;
            case RESULT_D_MIN:
            case RESULT_D_AVG:
            case RESULT_D_MAX:
            case RESULT_D_LAST:
                applicationId = getApplicationId(context, APP_PROCESSED_RESULTS_KEY);
                itemName = getProcessedResultName((ProcessedResult.Key) itemKey, itemKind);
                itemDescription = getProcessedResultDescription((ProcessedResult.Key) itemKey, itemKind);
                itemType = ItemType.ZABBIX_TRAPPER;
                itemUnits = "s"; // seconds
                break;
        }

        if (itemType == ItemType.JMX_AGENT) {
            boolean jmxExposed = false;
            switch (itemKind.getApplicationKind()) {
                case PROCESSED_EVENTS: jmxExposed = interestProcessedEventsJmx.contains(itemKey.getItemName()); break;
                case PUBLISHED_EVENTS: jmxExposed = interestPublishedEventsJmx.contains(itemKey.getItemName()); break;
            }
            if (!jmxExposed || zabbixVersion.supportFeaturesOf(ZabbixVersion.V_2_0)) { // do not create Zabbix item if it isn't exposed to JMX or Zabbix have no JMX support
                configuredItemKeys.add(itemKey);
                return false;
            }
        }

        return createItem(context, applicationId, zabbixItemKey, itemName, itemDescription, itemUnits, itemType, itemValueType);
    }

    private String getApplicationId(final Map<String, String> context, final String applicationKey) {
        String applicationId = context.get(applicationKey);
        if (applicationId == null) {
            if (configureApplication(context, DEFAULT_APP_NAMES.get(applicationKey), applicationKey)) {
                applicationId = context.get(applicationKey);
            } else {
                LOG.error("Failed to configure application {} ({})!", DEFAULT_APP_NAMES.get(applicationKey), applicationKey);
            }
        }
        return applicationId;
    }

    private boolean createItem(final Map<String, String> context, final String applicationId, final String itemKey, final String itemName, final String itemDescription, final String itemUnits, final ItemType itemType, final ValueType itemValueType) {
        return createItem(context, applicationId, itemKey, itemName, itemDescription, itemUnits, itemType, itemValueType, jmxItemDelay, null, null, null, null, null, 0);
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/object">Item object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/create">item.create method description</a>
     */
    private boolean createItem(final Map<String, String> context, final String applicationId, final String itemKey, final String itemName, final String itemDescription,
                               final String itemUnits, final ItemType itemType, final ValueType itemValueType, final Integer itemDelay,
                               final Integer itemMultiplier, final Float itemFormula, final Integer itemDelta, final Integer itemHistory, final Integer itemTrends, final Integer itemStatus) {
        LOG.debug("Creating the {} item for application {} on the host {} ({}) ...", itemKey, zabbixAgentApplication, zabbixAgentHostName, zabbixAgentHost);

        RequestBuilder createItemRequest = RequestBuilder.newBuilder().method("item.create")
                                                         .paramEntry("hostid", context.get(HOST_KEY))
                                                         .paramEntry("interfaceid", context.get(JMX_IF_KEY))
                                                         .paramEntry("applications", new JSONArray(Arrays.asList(applicationId)))
                                                         .paramEntry("delay", zabbixVersion.supportFeaturesOf(ZabbixVersion.V_3_4) ? itemDelay + "s" : itemDelay)
                                                         .paramEntry("key_", itemKey)
                                                         .paramEntry("name", itemName)
                                                         .paramEntry("description", itemDescription)
                                                         .paramEntry("units", itemUnits)
                                                         .paramEntry("value_type", itemValueType.getType());
        switch (itemType) {
            case SNMP_TRAP: {
                if (zabbixVersion.supportFeaturesOf(ZabbixVersion.V_2_2)) {
                    createItemRequest.paramEntry("type", itemType.getType()); break;
                } else {
                    return unsupportedZabbixVersion(itemType);
                }
            }
            case DEPENDENT_ITEM: {
                if (zabbixVersion.supportFeaturesOf(ZabbixVersion.V_3_4)) {
                    createItemRequest.paramEntry("type", itemType.getType());
                } else {
                    return unsupportedZabbixVersion(itemType);
                }
            }
            default: createItemRequest.paramEntry("type", itemType.getType()); break;
        }
        if (itemType == ItemType.JMX_AGENT && zabbixVersion.supportFeaturesOf(ZabbixVersion.V_3_4)) {
            createItemRequest.paramEntry("jmx_endpoint", buildJmxEndpoint(context));
        }
        if (itemType == ItemType.ZABBIX_TRAPPER && zabbixVersion.supportFeaturesOf(ZabbixVersion.V_3_4)) {
            createItemRequest.paramEntry("trapper_hosts", getTrapperHosts()); // see http://support.zabbix.com/browse/ZBX-13248
        }
        if (itemMultiplier != null) {
            createItemRequest.paramEntry("multiplier", itemMultiplier);
        }
        if (itemFormula != null) {
            createItemRequest.paramEntry("formula", itemFormula);
        }
        if (itemDelta != null) {
            createItemRequest.paramEntry("delta", itemDelta);
        }
        if (itemHistory != null) {
            createItemRequest.paramEntry("history", zabbixVersion.supportFeaturesOf(ZabbixVersion.V_3_4) ? itemHistory + "d" : itemHistory);
        }
        if (itemTrends != null) {
            createItemRequest.paramEntry("trends", zabbixVersion.supportFeaturesOf(ZabbixVersion.V_3_4) ? itemTrends + "d" : itemTrends);
        }
        if (itemStatus != null) {
            createItemRequest.paramEntry("status", itemStatus);
        }
        JSONObject createItemResponse = call(createItemRequest.build());
        String itemId = createItemResponse.getJSONObject("result").getJSONArray("itemids").getString(0);
        LOG.debug("Created the {} item {}.", itemName, itemId);

        if (itemId != null) {
            context.put(itemKey, itemId);
            return true;
        } else {
            return false;
        }
    }

    private boolean unsupportedZabbixVersion(final ItemType itemType) {
        LOG.error("Item type {} ({}) is unsupported in version {}!", itemType, itemType.getType(), zabbixVersion.branch());
        return false;
    }

    protected String buildJmxEndpoint(final Map<String, String> context) {
        return "service:jmx:rmi:///jndi/rmi://" + zabbixAgentHost + ":" + jmxRemotePort + "/jmxrmi";
    }

    private void configureQueueStatisticsGraph(final Map<String, String> context) {
        if (!configuredItemKeys.contains(queueStatisticsGraph) && configuredItemKeys.contains(QueueStatisticsKey.QUEUED_EVENT_COUNT) && configuredItemKeys.contains(QueueStatisticsKey.PROCESSED_EVENT_NUMBER)) {
            configureGraph(context, queueStatisticsGraph);
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/graph/object">Graph object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/graph/get">graph.get method description</a>
     */
    private boolean configureGraph(final Map<String, String> context, final AppGraph graph) {
        LOG.debug("Configuring the Zabbix graph {} ...", graph.getName());

        final Map<GraphItem, String> graphItemIds = collectGraphItemIds(context, graph.getGraphItems());

        final List<String> zabbixItemIds = new ArrayList<>();
        for (GraphItem graphItem : graph.getGraphItems()) {
            String graphItemId = graphItemIds.get(graphItem);
            if (graphItemId != null) {
                zabbixItemIds.add(graphItemId);
            }
        }

        Request getGraphRequest = RequestBuilder.newBuilder().method("graph.get")
                                                .paramEntry("output", "extend")
                                                .paramEntry("hostids", context.get(HOST_KEY))
                                                .paramEntry("sortfield", "name")
                                                .paramEntry("itemids",new JSONArray((List) zabbixItemIds))
                                                .build();
        JSONObject getGraphResponse = call(getGraphRequest);
        if (getGraphResponse.getJSONArray("result").isEmpty() || findGraph(getGraphResponse.getJSONArray("result"), graph.getName()) == null) {
            return createGraph(context, graph);
        } else {
            String graphId = findGraph(getGraphResponse.getJSONArray("result"), graph.getName());
            LOG.debug("Configured the Zabbix graph {}.", graphId);
            configuredItemKeys.add(graph);
            return true;
        }
    }

    private Map<GraphItem, String> collectGraphItemIds(final Map<String, String> context, final GraphItem ... graphItems) {
        final Map<GraphItem, String> graphItemIds = new HashMap<>();

        for (final GraphItem graphItem : graphItems) {
            final String zabbixItemKey = getZabbixItemKey(graphItem);
            final String zabbixItemId = context.get(zabbixItemKey);
            if (zabbixItemId != null) {
                graphItemIds.put(graphItem, zabbixItemId);
            } else {
                LOG.warn("Graph item {} were not configured!", zabbixItemKey);
            }
        }

        return graphItemIds;
    }

    private String findGraph(final JSONArray graphs, final String graphName) {
        for (int graphIndex = 0; graphIndex < graphs.size(); graphIndex++) {
            JSONObject graph = graphs.getJSONObject(graphIndex);
            if (graph.getString("name").trim().equals(prepareApplicationAwareName(graphName.trim()))) {
                return graph.getString("graphid");
            }
        }

        return null;
    }

    private String getZabbixItemKey(final GraphItem graphItem) {
        final String zabbixItemKey;
        if (graphItem instanceof AppGraphItem) {
            zabbixItemKey = getZabbixItemKey(((AppGraphItem) graphItem).getItemKey(), ((AppGraphItem) graphItem).getItemKind());
        } else {
            zabbixItemKey = graphItem.getItemKey().toString();
        }
        return zabbixItemKey;
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/graph/object">Graph object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/graph/create">graph.create method description</a>
     */
    private boolean createGraph(final Map<String, String> context, final Graph graph) {
        LOG.info("Creating the Zabbix graph {} ...", graph.getName());

        Map<GraphItem, String> graphItemIds = collectGraphItemIds(context, graph.getGraphItems());
        JSONArray graphItems = new JSONArray();
        for (GraphItem graphItem : graph.getGraphItems()) {
            graphItems.add(new JSONObject(new HashMap<String, Object>() {{
                put("itemid", graphItemIds.get(graphItem));
                put("color", graphItem.getItemColor());
                if (graphItem.getDrawValueType() != null) { put("calc_fnc", graphItem.getDrawValueType().getType()); }
                if (graphItem.getDrawType() != null) { put("drawtype", graphItem.getDrawType().ordinal()); }
                if (graphItem.getSortOrder() != null) { put("sortorder", graphItem.getSortOrder()); }
                if (graphItem.getGraphItemType() != null) { put("type", graphItem.getDrawValueType().getType()); }
                if (graphItem.getYAxisSide() != null) { put("yaxisside", graphItem.getYAxisSide().ordinal()); }
            }}));
        }

        Request createGraphRequest = RequestBuilder.newBuilder().method("graph.create")
                                                   .paramEntry("name", prepareApplicationAwareName(graph.getName()))
                                                   .paramEntry("width", graph.getWidth())
                                                   .paramEntry("height", graph.getHeight())
                                                   .paramEntry("gitems", graphItems)
                                                   .paramEntry("graphtype", graph.getGraphType().ordinal())
                                                   .paramEntry("percent_left", graph.getPercentLeft())
                                                   .paramEntry("percent_right", graph.getPercentRight())
                                                   .paramEntry("show_3d", graph.isShow3D() ? 1 : 0)
                                                   .paramEntry("show_legend", graph.isShowLegend() ? 1 : 0)
                                                   .paramEntry("show_work_period", graph.isShowWorkPeriod() ? 1 : 0)
                                                   .paramEntry("yaxismax", graph.getYAxisMax())
                                                   .paramEntry("yaxismin", graph.getYAxisMin())
                                                   .paramEntry("ymax_type", graph.getYMaxType().ordinal())
                                                   .paramEntry("ymin_type", graph.getYMinType().ordinal())
                                                   .build();
        if (graph.getYMaxItem() != null && graph.getYMaxType() == ValueCalculationMethod.ITEM) {
            Map<GraphItem, String> yMaxItem = collectGraphItemIds(context, graph.getYMaxItem());
            if (yMaxItem.containsKey(graph.getYMaxItem())) {
                createGraphRequest.putParam("ymax_itemid", yMaxItem.get(graph.getYMaxItem()));
            } else {
                LOG.warn("Graph item {} were not configured!", getZabbixItemKey(graph.getYMaxItem()));
            }
        }
        if (graph.getYMinItem() != null && graph.getYMaxType() == ValueCalculationMethod.ITEM) {
            Map<GraphItem, String> yMaxItem = collectGraphItemIds(context, graph.getYMinItem());
            if (yMaxItem.containsKey(graph.getYMinItem())) {
                createGraphRequest.putParam("ymin_itemid", yMaxItem.get(graph.getYMinItem()));
            } else {
                LOG.warn("Graph item {} were not configured!", getZabbixItemKey(graph.getYMinItem()));
            }
        }
        JSONObject createGraphResponse = call(createGraphRequest);
        String graphId = createGraphResponse.getJSONObject("result").getJSONArray("graphids").getString(0);
        LOG.debug("Created the {} graph {}.", graph.getName(), graphId);

        if (graphId != null && graph instanceof AppGraph) {
            configuredItemKeys.add((AppGraph) graph);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/object">Item object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/get">item.get method description</a>
     */
    private void checkForObsoleteItems(final Map<String, String> context) {
        if (enableZabbix && deleteObsoleteItems) {
            LOG.info("Check for obsolete items ...");

            JSONArray applicationIds = new JSONArray();
            DEFAULT_APP_NAMES.keySet().forEach(applicationKey -> {
                if (context.containsKey(applicationKey))
                    applicationIds.add(context.get(applicationKey));
            });
            Request getItemsRequest = RequestBuilder.newBuilder().method("item.get")
                                                    .paramEntry("output", "extend")
                                                    .paramEntry("hostids", context.get(HOST_KEY))
                                                    .paramEntry("applicationids", applicationIds)
                                                    .paramEntry("sortfield", "name")
                                                    .build();
            JSONObject getItemsResponse = call(getItemsRequest);
            JSONArray configuredItems = getItemsResponse.getJSONArray("result");
            LOG.info("Zabbix contains {} items for {} applications ...", configuredItems.size(), applicationIds.size());

            Set<String> obsoleteItemIds = new HashSet<>();
            for (int i = 0; i < configuredItems.size(); i++) {
                JSONObject configuredItem = configuredItems.getJSONObject(i);
                String itemKey = configuredItem.getString("key_");
                String itemId = configuredItem.getString("itemid");
                Date itemLastClock = new Date(configuredItem.getLongValue("lastclock") * 1000);
                if ((context.containsKey(itemKey) && !context.get(itemKey).equals(itemId)) || daysPast(itemLastClock) > numberOfEmptyDaysForObsoleteItem) {
                    if (!isProtectedItem(itemKey)) {
                        obsoleteItemIds.add(itemId);
                    } else {
                        LOG.warn("Item with key '{}' and id '{}' is protected. But there are no updates on it since {} ({} days).",
                                 itemKey, itemId, new SimpleDateFormat("dd.MM.yyyy").format(itemLastClock), daysPast(itemLastClock));
                    }
                }
            }

            if (!obsoleteItemIds.isEmpty()) {
                LOG.warn("Found {} obsolete items. Delete them ...", obsoleteItemIds.size());
                deleteItems(context, obsoleteItemIds.toArray(new String[obsoleteItemIds.size()]));
            } else {
                LOG.info("Found no obsolete items.");
            }
        }
    }

    private int daysPast(final Date itemLastClock) {
        return (int) ((System.currentTimeMillis() - itemLastClock.getTime()) / (1000 * 3600 * 24));
    }

    private boolean isProtectedItem(final String itemKey) {
        return protectedItemNames.stream().filter(protectedItemName -> itemKey.contains(protectedItemName)).count() > 0;
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/object">Item object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/delete">item.delete method description</a>
     */
    private boolean deleteItems(final Map<String, String> context, final String ... itemIds) {
        LOG.debug("Deleting the obsolete Zabbix monitoring items ({}) ...", Arrays.asList(itemIds));

        SimpleRequest deleteItemsRequest = SimpleRequestBuilder.newBuilder().method("item.delete")
                                                               .params(itemIds)
                                                               .build();
        JSONObject deleteItemsResponse = call(deleteItemsRequest);
        JSONArray deletedItemIds = deleteItemsResponse.getJSONObject("result").getJSONArray("itemids");
        LOG.debug("Deleted the items: {}.", Arrays.asList(deletedItemIds.toArray()));

        if (itemIds.length == deletedItemIds.size()) {
            boolean success = true;
            for (int i = 0; i < deletedItemIds.size(); i++) {
                success &= deletedItemIds.getString(i).equals(itemIds[i]);
            }
            return success;
        } else {
            return false;
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/forum/showthread.php?t=35002">Monitoring java processes discission</a>
     * @see <a href="http://support.zabbix.com/browse/ZBXNEXT-1490">Monitoring multiple java processes issue</a>
     */
    private void configureJvmMonitoring(final Map<String, String> context) {
        if (!configureJvmMonitor) {
            LOG.warn("JVM monitoring is disabled!");
            return;
        }

        LOG.info("Configuring JVM monitoring ...");
        createApplication(context, APP_JVM, APP_JVM_KEY);

        processTemplate(context, DEFAULT_JVM_TEMPLATE);
    }

    private void processTemplate(final Map<String, String> context, final String templateFileName) {
        LOG.debug("Configuring JVM monitoring by template {} ...", templateFileName);
        try {
            Document template = loadZabbixTemplate(DEFAULT_JVM_TEMPLATE);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            String templateKey = xpath.compile("/zabbix_export/templates/template/template/text()")
                                      .evaluate(template, XPathConstants.STRING).toString();

            XPathExpression itemExpression = xpath.compile("/zabbix_export/templates/template/items/item");
            NodeList itemNodes = (NodeList) itemExpression.evaluate(template, XPathConstants.NODESET);
            for (int itemIndex = 0; itemIndex < itemNodes.getLength(); itemIndex++) {
                createTemplateItem(context, itemNodes.item(itemIndex));
            }

            XPathExpression graphExpression = xpath.compile("/zabbix_export/graphs/graph");
            NodeList graphNodes = (NodeList) graphExpression.evaluate(template, XPathConstants.NODESET);
            for (int graphIndex = 0; graphIndex < graphNodes.getLength(); graphIndex++) {
                createTemplateGraph(context, graphNodes.item(graphIndex));
            }

            XPathExpression triggerExpression = xpath.compile("/zabbix_export/triggers/trigger");
            NodeList triggerNodes = (NodeList) triggerExpression.evaluate(template, XPathConstants.NODESET);
            for (int triggerIndex = 0; triggerIndex < triggerNodes.getLength(); triggerIndex++) {
                createTemplateTrigger(context, triggerNodes.item(triggerIndex), templateKey);
            }
        } catch (Exception e) {
            LOG.error("Failed to configure JVM monitoring!", e);
        }
    }

    private Document loadZabbixTemplate(final String templateFileName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(getClass().getResourceAsStream(ZABBIX_TEMPLATE_PATH + templateFileName));
        return document;
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/object">Item object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/item/get">item.get method description</a>
     */
    private boolean createTemplateItem(final Map<String, String> context, final Node itemNode) {
        String applicationId = context.get(APP_JVM_KEY);
        String itemKey = null;
        String itemName = null;
        String itemDescription = null;
        String itemUnits = "";
        Integer itemMultiplier = null;
        Float itemFormula = null;
        Integer itemDelta = 0;
        ItemType itemType = ItemType.JMX_AGENT;
        ValueType itemValueType = ValueType.NUMERIC_UNSIGNED;
        Integer itemDelay = jmxItemDelay;
        Integer itemHistory = 90;
        Integer itemTrends = 365;
        Integer itemStatus = 0;

        try {
            NodeList itemTags = itemNode.getChildNodes();
            for (int tagIndex = 0; tagIndex < itemTags.getLength(); tagIndex++) {
                String tagValue = itemTags.item(tagIndex).getTextContent();

                switch (itemTags.item(tagIndex).getNodeName()) {
                    case "name": itemName = tagValue; break;
                    case "type": itemType = ItemType.define(Integer.parseInt(tagValue)); break;
                    case "multiplier": if (tagValue != null && !tagValue.trim().isEmpty()) { itemMultiplier = Integer.valueOf(tagValue); } break;
                    case "key": itemKey = tagValue; break;
                    case "delay": itemDelay = Integer.valueOf(tagValue); break;
                    case "history": itemHistory = Integer.valueOf(tagValue); break;
                    case "trends": itemTrends = Integer.valueOf(tagValue); break;
                    case "status": itemStatus = Integer.valueOf(tagValue); break;
                    case "value_type": itemValueType = ValueType.define(Integer.parseInt(tagValue)); break;
                    case "units": itemUnits = tagValue; break;
                    case "delta": itemDelta = Integer.valueOf(tagValue); break;
                    case "formula": if (tagValue != null && !tagValue.trim().isEmpty() && !tagValue.trim().equals("1")) { itemFormula = Float.valueOf(tagValue); } break;
                    case "description": itemDescription = tagValue; break;
                }
            }
            if (itemDescription == null || itemDescription.trim().isEmpty()) {
                itemDescription = itemName;
            }

            int sameKeyCount = 0;

            JSONObject getItemFilter = new JSONObject();
            getItemFilter.put("name", itemName);
            Request getItemRequest = RequestBuilder.newBuilder().method("item.get")
                                                   .paramEntry("output", "extend")
                                                   .paramEntry("hostids", context.get(HOST_KEY))
                                                   .paramEntry("sortfield", "key_")
                                                   .paramEntry("filter", getItemFilter)
                                                   .build();
            JSONObject getItemResponse = call(getItemRequest);
            if (!getItemResponse.getJSONArray("result").isEmpty()) {
                sameKeyCount = getItemResponse.getJSONArray("result").size();
            }

            String trickItemKey = buildTrickItemKey(itemKey, sameKeyCount);
            if (createItem(context, applicationId, trickItemKey, itemName, itemDescription, itemUnits, itemType, itemValueType, itemDelay,
                           itemMultiplier, itemFormula, itemDelta, itemHistory, itemTrends, itemStatus)) {
                context.put(itemKey, context.get(trickItemKey));
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Unable to create item '{}' in application {} on host {}.", itemName, APP_JVM, getApplicationHost());
            return false;
        }
    }

    /**
     * @see <a href="http://www.zabbix.com/forum/showthread.php?t=35002">Multiple java processes monitoring discission</a>
     */
    private String buildTrickItemKey(String itemKey, int sameKeyCount) {
        StringBuilder spaceString = new StringBuilder(sameKeyCount);
        for (int i = 0; i < sameKeyCount; i++) {
            spaceString.append(" ");
        }
        return itemKey.replace("[", "[".concat(spaceString.toString()));
    }

    /**
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/trigger/object">Trigger object description</a>
     * @see <a href="http://www.zabbix.com/documentation/3.0/manual/api/reference/trigger/create">trigger.create method description</a>
     */
    private boolean createTemplateTrigger(final Map<String, String> context, final Node triggerNode, final String templateKey) {
        String triggerExpression = "";
        String triggerName = "";
        String triggerUrl = null;
        Integer triggerStatus = 0;
        Integer triggerPriority = 0;
        String truggerDescription = null;
        Integer triggerType = 0;

        try {
            NodeList triggerTags = triggerNode.getChildNodes();
            for (int tagIndex = 0; tagIndex < triggerTags.getLength(); tagIndex++) {
                String tagValue = triggerTags.item(tagIndex).getTextContent();

                switch (triggerTags.item(tagIndex).getNodeName()) {
                    case "expression": triggerExpression = prepareTriggerExpression(tagValue, templateKey); break;
                    case "name": triggerName = tagValue; break;
                    case "url": triggerUrl = tagValue; break;
                    case "status": triggerStatus = Integer.valueOf(tagValue); break;
                    case "priority": triggerPriority = Integer.valueOf(tagValue); break;
                    case "description": truggerDescription = tagValue; break;
                    case "type": triggerType = Integer.valueOf(tagValue); break;
                }
            }

            LOG.debug("Creating the {} trigger on the host {} ({}) ...", triggerName, zabbixAgentHostName, zabbixAgentHost);

            RequestBuilder createTriggerRequest = RequestBuilder.newBuilder().method("trigger.create")
                                                                .paramEntry("description", triggerName)
                                                                .paramEntry("expression", triggerExpression)
                                                                .paramEntry("priority", triggerPriority)
                                                                .paramEntry("status", triggerStatus)
                                                                .paramEntry("type", triggerType);
            if (triggerUrl != null && !triggerUrl.trim().isEmpty()) {
                createTriggerRequest.paramEntry("url", triggerUrl);
            }
            if (truggerDescription != null && !truggerDescription.trim().isEmpty()) {
                createTriggerRequest.paramEntry("comments", truggerDescription);
            }
            JSONObject createTriggerResponse = call(createTriggerRequest.build());
            String triggerId = createTriggerResponse.getJSONObject("result").getJSONArray("triggerids").getString(0);
            LOG.debug("Created the {} trigger {}.", triggerName, triggerId);

            return triggerId != null;
        } catch (Exception e) {
            LOG.warn("Unable to create trigger '{}' on host {}.", triggerName, getApplicationHost());
            return false;
        }
    }

    private String prepareTriggerExpression(final String triggerExpression, final String templateKey) {
        return triggerExpression.replace(templateKey, getApplicationHost());
    }

    private boolean createTemplateGraph(final Map<String, String> context, final Node graphNode) {
        String graphName = null;
        int graphWidth = 900;
        int graphHeight = 200;
        TemplateGraphItem[] graphItems = null;
        GraphType graphType = GraphType.NORMAL;
        Float percentLeft = null;
        Float percentRight = null;
        boolean show3D = false;
        boolean showLegend = true;
        boolean showWorkPeriod = true;
        Float yAxisMax = 100.0f;
        Float yAxisMin = null;
        Integer yMaxItem = null;
        ValueCalculationMethod yMaxType = ValueCalculationMethod.CALCULATED;
        Integer yMinItem = null;
        ValueCalculationMethod yMinType = ValueCalculationMethod.CALCULATED;

        try {
            NodeList graphTags = graphNode.getChildNodes();
            for (int tagIndex = 0; tagIndex < graphTags.getLength(); tagIndex++) {
                String tagValue = graphTags.item(tagIndex).getTextContent();

                switch (graphTags.item(tagIndex).getNodeName()) {
                    case "name": graphName = tagValue; break;
                    case "width": graphWidth = Integer.parseInt(tagValue); break;
                    case "height": graphHeight = Integer.parseInt(tagValue); break;
                    case "yaxismin": yAxisMin = Float.valueOf(tagValue); break;
                    case "yaxismax": yAxisMax = Float.valueOf(tagValue); break;
                    case "show_work_period": showWorkPeriod = Integer.parseInt(tagValue) == 1; break;
                    case "type": graphType = GraphType.values()[Integer.parseInt(tagValue)]; break;
                    case "show_legend": showLegend = Integer.parseInt(tagValue) == 1; break;
                    case "show_3d": show3D = Integer.parseInt(tagValue) == 1; break;
                    case "percent_left": percentLeft = Float.valueOf(tagValue); break;
                    case "percent_right": percentRight = Float.valueOf(tagValue); break;
                    case "ymin_type_1": yMinType = ValueCalculationMethod.values()[Integer.parseInt(tagValue)]; break;
                    case "ymax_type_1": yMaxType = ValueCalculationMethod.values()[Integer.parseInt(tagValue)]; break;
                    case "ymin_item_1": yMinItem = Integer.valueOf(tagValue); break;
                    case "ymax_item_1": yMaxItem = Integer.valueOf(tagValue); break;
                    case "graph_items": graphItems = collectTemplateGraphItems(context, graphTags.item(tagIndex).getChildNodes()); break;
                }
            }

            TemplateGraph graph = new TemplateGraph(graphName, graphWidth, graphHeight, graphItems);
            graph.setGraphType(graphType);
            if (percentLeft != null) {
                graph.setPercentLeft(percentLeft);
            }
            if (percentRight != null) {
                graph.setPercentRight(percentRight);
            }
            graph.setShow3D(show3D);
            graph.setShowLegend(showLegend);
            graph.setShowWorkPeriod(showWorkPeriod);
            if (yAxisMax != null) {
                graph.setYAxisMax(yAxisMax);
            }
            if (yAxisMin != null) {
                graph.setYAxisMin(yAxisMin);
            }
            if (yMaxItem != null && yMaxItem < graphItems.length) {
                graph.setYMaxItem(graphItems[yMaxItem]);
                graph.setYMaxType(yMaxType);
            }
            if (yMinItem != null && yMinItem < graphItems.length) {
                graph.setYMinItem(graphItems[yMinItem]);
                graph.setYMinType(yMinType);
            }

            return createGraph(context, graph);
        } catch (Exception e) {
            LOG.warn("Unable to create graph '{}' in application {} on host {}.", graphName, APP_JVM, getApplicationHost());
            return false;
        }
    }

    private TemplateGraphItem[] collectTemplateGraphItems(final Map<String, String> context, final NodeList graphItemNodes) throws Exception {
        List<TemplateGraphItem> graphItems = new ArrayList<>();

        for (int graphItemIndex = 0; graphItemIndex < graphItemNodes.getLength(); graphItemIndex++) {
            NodeList graphItemTags = graphItemNodes.item(graphItemIndex).getChildNodes();

            String itemKey = null;
            String itemColor = null;
            DrawValueType drawValueType = DrawValueType.AVERAGE;
            DrawType drawType = DrawType.LINE;
            Integer sortOrder = null;
            GraphItemType graphItemType = GraphItemType.SIMPLE;
            YAxisSide yAxisSide = YAxisSide.LEFT_SIDE;

            for (int itemTagIndex = 0; itemTagIndex < graphItemTags.getLength(); itemTagIndex++) {
                String tagValue = graphItemTags.item(itemTagIndex).getTextContent();

                switch (graphItemTags.item(itemTagIndex).getNodeName()) {
                    case "sortorder": sortOrder = Integer.valueOf(tagValue); break;
                    case "drawtype": drawType = DrawType.values()[Integer.parseInt(tagValue)]; break;
                    case "color": itemColor = tagValue; break;
                    case "yaxisside": yAxisSide = YAxisSide.values()[Integer.parseInt(tagValue)]; break;
                    case "calc_fnc": drawValueType = DrawValueType.define(Integer.parseInt(tagValue)); break;
                    case "type": graphItemType = GraphItemType.define(Integer.parseInt(tagValue)); break;
                    case "item": itemKey = extractItemKey(context, graphItemTags.item(itemTagIndex)); break;
                }
            }

            if (itemKey != null) {
                TemplateGraphItem graphItem = new TemplateGraphItem(itemKey, itemColor);
                graphItem.setDrawValueType(drawValueType);
                graphItem.setDrawType(drawType);
                graphItem.setSortOrder(sortOrder);
                graphItem.setGraphItemType(graphItemType);
                graphItem.setYAxisSide(yAxisSide);

                graphItems.add(graphItem);
            }
        }

        return graphItems.toArray(new TemplateGraphItem[graphItems.size()]);
    }

    private String extractItemKey(final Map<String, String> context, final Node itemNode) {
        for (int tagIndex = 0; tagIndex < itemNode.getChildNodes().getLength(); tagIndex++) {
            if ("key".equals(itemNode.getChildNodes().item(tagIndex).getNodeName())) {
                return itemNode.getChildNodes().item(tagIndex).getTextContent();
            }
        }

        return null;
    }

}
