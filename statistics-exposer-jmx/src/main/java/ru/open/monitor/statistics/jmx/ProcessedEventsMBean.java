package ru.open.monitor.statistics.jmx;

import static javax.management.openmbean.SimpleType.DOUBLE;
import static javax.management.openmbean.SimpleType.LONG;
import static ru.open.monitor.statistics.item.ItemUtil.toSeconds;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PROCESSED_EVENTS;
import static ru.open.monitor.statistics.item.ItemUtil.PKG_MONITORING;
import static ru.open.monitor.statistics.item.ItemUtil.generateItemKey;
import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.ProcessedEvent;
import ru.open.monitor.statistics.event.StatisticsProvider;
import ru.open.monitor.statistics.item.ItemKind;

@Component
@ManagedResource(objectName = PKG_MONITORING + ":name=" + APP_PROCESSED_EVENTS, description = ProcessedEventsMBean.DESCRIPTION)
public class ProcessedEventsMBean implements DynamicMBean {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessedEventsMBean.class);

    static final String DESCRIPTION = "Processed events statistics.";

    static final String GET_PROCESSED_EVENTS_CLASSES_OPERATION = "getProcessedEventsClasses";
    static final String GET_INTEREST_EVENTS_CLASSES_OPERATION = "getInterestEventsClasses";

    @Value("#{'${statistics.monitor.jmx.interestProcessedEvents:}'.split(',')}")
    private List<String> interestProcessedEvents;
    @Value("#{'${statistics.monitor.zabbix.interestProcessedEvents:}'.split(',')}")
    private List<String> interestProcessedEventsZabbix;

    @Autowired
    private StatisticsProvider statisticsProvider;

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        try {
            return convertToCompositeData(attribute);
        } catch (OpenDataException e) {
            LOG.error("Failed to get attribute!", e);
            throw new MBeanException(e, "Failed to get attribute!");
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        if (attributes == null) {
            return new AttributeList();
        }

        final List<Attribute> result = new ArrayList<>(attributes.length);
        for (String attribute : attributes) {
            try {
                result.add(new Attribute(attribute, convertToCompositeData(attribute)));
            } catch (OpenDataException e) {
                LOG.error("Failed to get attribute!", e);
            }
        }
        return new AttributeList(result);
    }

    private CompositeDataSupport convertToCompositeData(String eventClass) throws OpenDataException {
        Collection<ProcessedEvent.Key> statisticsKeys = new HashSet<>();
        statisticsKeys.addAll(statisticsProvider.getQueuedEventKeys(eventClass));
        statisticsKeys.addAll(statisticsProvider.getProcessedEventKeys(eventClass));

        List<String> itemNames = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size() * 5);
        List<Object> itemValues = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size() * 5);

        for (ProcessedEvent.Key statisticsKey : statisticsKeys) {
            ProcessedEvent statistics = statisticsProvider.getProcessedEventStatistics(statisticsKey);

            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_COUNT));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_MIN));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_AVG));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_MAX));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_LAST));

            itemValues.add(statistics.getEventCount());
            itemValues.add(toSeconds(statistics.getMinDurationNanos()));
            itemValues.add(toSeconds(statistics.getAvgDurationNanos()));
            itemValues.add(toSeconds(statistics.getMaxDurationNanos()));
            itemValues.add(toSeconds(statistics.getLastDurationNanos()));
        }

        return new CompositeDataSupport(generateCompositeType(statisticsKeys),
                                        itemNames.toArray(new String[itemNames.size()]),
                                        itemValues.toArray(new Object[itemValues.size()]));
    }

    private CompositeType generateCompositeType(Collection<ProcessedEvent.Key> statisticsKeys) throws OpenDataException {
        List<String> itemNames = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size() * 5);
        List<String> itemDescriptions = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size() * 5);
        List<OpenType> itemTypes = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size() * 5);

        for (ProcessedEvent.Key statisticsKey : statisticsKeys) {
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_COUNT));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_MIN));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_AVG));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_MAX));
            itemNames.add(generateItemKey(statisticsKey.getHandlerClass(), ItemKind.PROCESSED_EVENT_D_LAST));

            itemDescriptions.add("The count of processed by " + statisticsKey.getHandlerClass() + " events");
            itemDescriptions.add("The minimum time of event processing by " + statisticsKey.getHandlerClass() + " (in seconds)");
            itemDescriptions.add("The average time of event processing by " + statisticsKey.getHandlerClass() + " (in seconds)");
            itemDescriptions.add("The maximum time of event processing by " + statisticsKey.getHandlerClass() + " (in seconds)");
            itemDescriptions.add("The last time of event processing by " + statisticsKey.getHandlerClass() + " (in seconds)");

            itemTypes.addAll(Arrays.asList(new OpenType[] { LONG, DOUBLE, DOUBLE, DOUBLE, DOUBLE }));
        }

        return new CompositeType(APP_PROCESSED_EVENTS,
                                 DESCRIPTION,
                                 itemNames.toArray(new String[itemNames.size()]),
                                 itemDescriptions.toArray(new String[itemDescriptions.size()]),
                                 itemTypes.toArray(new OpenType[itemTypes.size()]));
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {}

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    @Override
    public Object invoke(String actionName, Object params[], String signature[]) throws MBeanException, ReflectionException {
        switch (actionName) {
            case GET_PROCESSED_EVENTS_CLASSES_OPERATION: {
                Collection<String> eventClasses = new TreeSet<>();
                for (ProcessedEvent.Key key : statisticsProvider.getProcessedEventKeys()) {
                    eventClasses.add(key.getEventClass());
                }
                return eventClasses.toArray(new String[eventClasses.size()]);
            }
            case GET_INTEREST_EVENTS_CLASSES_OPERATION: {
                return interestProcessedEvents.toArray(new String[interestProcessedEvents.size()]);
            }
            default: return null;
        }
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(getClass().getName(), DESCRIPTION, collectAttributes(), null, collectOperations(), null);
    }

    private MBeanAttributeInfo[] collectAttributes() {
        final List<MBeanAttributeInfo> attributes = new ArrayList<>();
        final SortedSet<String> interestProcessedEvents = new TreeSet<>();
        interestProcessedEvents.addAll(this.interestProcessedEvents);
        interestProcessedEvents.addAll(interestProcessedEventsZabbix);
        for (final String eventClass : interestProcessedEvents) {
            attributes.add(new MBeanAttributeInfo(eventClass, CompositeType.class.getName(), "The statistics of processed events of type " + getSimpleClassName(eventClass), true, false, false));
        }
        return attributes.toArray(new MBeanAttributeInfo[attributes.size()]);
    }

    private MBeanOperationInfo[] collectOperations() {
        MBeanOperationInfo getProcessedEventClassesOperation = new MBeanOperationInfo(GET_PROCESSED_EVENTS_CLASSES_OPERATION, "Returns all the processed event classes.", null, "[Ljava/lang/String;", MBeanOperationInfo.INFO);
        MBeanOperationInfo getInterestEventClassesOperation = new MBeanOperationInfo(GET_INTEREST_EVENTS_CLASSES_OPERATION, "Returns the interest event classes.", null, "[Ljava/lang/String;", MBeanOperationInfo.INFO);
        return new MBeanOperationInfo[] { getProcessedEventClassesOperation, getInterestEventClassesOperation };
    }

}
