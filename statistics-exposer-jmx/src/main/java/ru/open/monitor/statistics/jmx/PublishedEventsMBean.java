package ru.open.monitor.statistics.jmx;

import static javax.management.openmbean.SimpleType.LONG;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PROCESSED_EVENTS;
import static ru.open.monitor.statistics.item.ItemUtil.APP_PUBLISHED_EVENTS;
import static ru.open.monitor.statistics.item.ItemUtil.PKG_MONITORING;
import static ru.open.monitor.statistics.item.ItemUtil.generateItemKey;
import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import ru.open.monitor.statistics.event.PublishedEvent;
import ru.open.monitor.statistics.event.StatisticsProvider;
import ru.open.monitor.statistics.item.ItemKind;

@Component
@ManagedResource(objectName = PKG_MONITORING + ":name=" + APP_PUBLISHED_EVENTS, description = PublishedEventsMBean.DESCRIPTION)
public class PublishedEventsMBean implements DynamicMBean {
    private static final Logger LOG = LoggerFactory.getLogger(PublishedEventsMBean.class);

    static final String DESCRIPTION = "Published events statistics.";

    static final String GET_PUBLISHED_EVENTS_CLASSES_OPERATION = "getPublishedEventsClasses";
    static final String GET_INTEREST_EVENTS_CLASSES_OPERATION = "getInterestEventsClasses";

    @Value("#{'${statistics.monitor.jmx.interestPublishedEvents:}'.split(',')}")
    private List<String> interestPublishedEvents;
    @Value("#{'${statistics.monitor.zabbix.interestPublishedEvents:}'.split(',')}")
    private List<String> interestPublishedEventsZabbix;

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
        Collection<PublishedEvent.Key> statisticsKeys = statisticsProvider.getPublishedEventKeys(eventClass);

        List<String> itemNames = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size());
        List<Object> itemValues = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size());

        for (PublishedEvent.Key statisticsKey : statisticsKeys) {
            PublishedEvent statistics = statisticsProvider.getPublishedEventStatistics(statisticsKey);

            itemNames.add(generateItemKey(statisticsKey.getPublisherClass(), ItemKind.PUBLISHED_EVENT_COUNT));

            itemValues.add(statistics.getEventCount());
        }

        return new CompositeDataSupport(generateCompositeType(statisticsKeys),
                                        itemNames.toArray(new String[itemNames.size()]),
                                        itemValues.toArray(new Object[itemValues.size()]));
    }

    private CompositeType generateCompositeType(Collection<PublishedEvent.Key> statisticsKeys) throws OpenDataException {
        List<String> itemNames = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size());
        List<String> itemDescriptions = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size());
        List<OpenType> itemTypes = statisticsKeys.isEmpty() ? Collections.emptyList() : new ArrayList<>(statisticsKeys.size());

        for (PublishedEvent.Key statisticsKey : statisticsKeys) {
            itemNames.add(generateItemKey(statisticsKey.getPublisherClass(), ItemKind.PUBLISHED_EVENT_COUNT));

            itemDescriptions.add("The count of published by " + statisticsKey.getPublisherClass() + " events");

            itemTypes.addAll(Arrays.asList(new OpenType[] { LONG }));
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
            case GET_PUBLISHED_EVENTS_CLASSES_OPERATION: {
                Collection<String> eventClasses = new TreeSet<>();
                for (PublishedEvent.Key key : statisticsProvider.getPublishedEventKeys()) {
                    eventClasses.add(key.getEventClass());
                }
                return eventClasses.toArray(new String[eventClasses.size()]);
            }
            case GET_INTEREST_EVENTS_CLASSES_OPERATION: {
                return interestPublishedEvents.toArray(new String[interestPublishedEvents.size()]);
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
        final SortedSet<String> interestPublishedEvents = new TreeSet<>();
        interestPublishedEvents.addAll(this.interestPublishedEvents);
        interestPublishedEvents.addAll(interestPublishedEventsZabbix);
        for (final String eventClass : interestPublishedEvents) {
            attributes.add(new MBeanAttributeInfo(eventClass, CompositeType.class.getName(), "The statistics of published events of type " + getSimpleClassName(eventClass), true, false, false));
        }
        return attributes.toArray(new MBeanAttributeInfo[attributes.size()]);
    }

    private MBeanOperationInfo[] collectOperations() {
        MBeanOperationInfo getPublishedEventClassesOperation = new MBeanOperationInfo(GET_PUBLISHED_EVENTS_CLASSES_OPERATION, "Returns all the published event classes.", null, "[Ljava/lang/String;", MBeanOperationInfo.INFO);
        MBeanOperationInfo getInterestEventClassesOperation = new MBeanOperationInfo(GET_INTEREST_EVENTS_CLASSES_OPERATION, "Returns the interest event classes.", null, "[Ljava/lang/String;", MBeanOperationInfo.INFO);
        return new MBeanOperationInfo[] { getPublishedEventClassesOperation, getInterestEventClassesOperation };
    }

}
