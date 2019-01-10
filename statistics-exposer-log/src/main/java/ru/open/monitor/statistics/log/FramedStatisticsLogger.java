package ru.open.monitor.statistics.log;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.open.monitor.statistics.event.frame.FramedEventStatisticsConsumer;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsProvider;
import ru.open.monitor.statistics.event.frame.FramedEventStatisticsSubscription;

@Component
public class FramedStatisticsLogger extends AbstractFramedStatisticsLogger<FramedEventStatisticsProvider> implements FramedEventStatisticsConsumer {

    @Autowired
    private FramedEventStatisticsSubscription statisticsSubscription;
    @Value("${statistics.monitor.timeFrame:15000}")
    private Long timeFrameMillis;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        statisticsSubscription.subscribeConsumer(this);
    }

    @Override
    protected String prepareDescription(final FramedEventStatisticsProvider statisticsProvider) {
        StringBuilder description = new StringBuilder();
        description.append("Collected Statistics on ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date()))
                   .append(" for the last ").append(String.format("%.3f", (double) timeFrameMillis / 1000.0)).append(" seconds");
        return description.toString();
    }

}
