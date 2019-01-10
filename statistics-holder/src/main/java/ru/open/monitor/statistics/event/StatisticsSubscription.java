package ru.open.monitor.statistics.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class StatisticsSubscription<C extends StatisticsConsumer<?>> {
    private final Set<C> consumers = new CopyOnWriteArraySet<>();

    public void subscribeConsumer(C consumer) {
        consumers.add(consumer);
    }

    public Set<C> getConsumers() {
        return consumers;
    }
}
