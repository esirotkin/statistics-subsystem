package ru.open.monitor.statistics.zabbix.util;

import java.util.concurrent.ConcurrentSkipListSet;

import ru.open.monitor.statistics.zabbix.util.UpdateableConcurrentNavigableSet.Updateable;

public class UpdateableConcurrentNavigableSet<T extends Updateable<U>, U> extends ConcurrentSkipListSet<T> {

    public interface Updateable<U> { void update(U value); }

    public boolean update(T e, U value) {
        if (remove(e)) {
            e.update(value);
            return add(e);
        } else {
            return false;
        }
    }

}
