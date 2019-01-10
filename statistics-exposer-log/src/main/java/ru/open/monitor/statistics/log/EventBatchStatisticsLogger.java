package ru.open.monitor.statistics.log;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.frame.batch.EventBatchStatisticsConsumer;
import ru.open.monitor.statistics.event.frame.batch.EventBatchStatisticsProvider;
import ru.open.monitor.statistics.event.frame.batch.EventBatchStatisticsSubscription;

@Component
public class EventBatchStatisticsLogger extends AbstractFramedStatisticsLogger<EventBatchStatisticsProvider> implements EventBatchStatisticsConsumer {

    @Autowired
    private EventBatchStatisticsSubscription statisticsSubscription;
    @Value("${statistics.monitor.timeFrame:15000}")
    private Long timeFrameMillis;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        statisticsSubscription.subscribeConsumer(this);
    }

    @Override
    protected String prepareDescription(final EventBatchStatisticsProvider statisticsProvider) {
        StringBuilder description = new StringBuilder();
        description.append("Event Batch processing Statistics on ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date())).append(". ")
                   .append("Processing duration time: ").append(String.format("%.3f", (double) statisticsProvider.getBatchProcessingTime() / 1000.0)).append(" seconds");
        return description.toString();
    }

}
