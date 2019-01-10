package ru.open.monitor.statistics.zabbix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.github.hengyunabc.zabbix.sender.DataObject;

import ru.open.monitor.statistics.zabbix.config.ZabbixConfigurer;
import ru.open.monitor.statistics.zabbix.config.item.ExternalItem;

public class OnDemandZabbixSender {
    private static final Logger LOG = LoggerFactory.getLogger(OnDemandZabbixSender.class);

    private static final DataObject STOP = new DataObject();

    private final BlockingQueue<DataObject> senderQueue = new LinkedBlockingQueue<>();
    private final SenderThread senderThread = new SenderThread();
    private volatile boolean running = true;

    @Value("${statistics.monitor.zabbix.onDemandSendRhythm:500}")
    private long sendRhythm;

    @Autowired
    private ZabbixConfigurer zabbixConfigurer;
    @Autowired
    private ZabbixSenderWrapper zabbixSender;

    private class SenderThread extends Thread {
        private SenderThread() {
            super("onDemandSender");
        }

        @Override
        public void run() {
            LOG.info("Zabbix on-demand sender thread started.");
            try {
                while (running) {
                    Thread.sleep(sendRhythm);

                    final List<DataObject> zabbixData = new ArrayList<>();
                    if (senderQueue.drainTo(zabbixData) > 0) {
                        if (zabbixData.contains(STOP)) {
                            running = false;
                            zabbixData.remove(STOP);
                        }

                        if (!zabbixData.isEmpty()) {
                            try {
                                LOG.trace("Sending {} ...", zabbixData);
                                zabbixSender.sendToZabbix(zabbixData);
                            } catch (Throwable t) {
                                LOG.error("Failed to send data to Zabbix!", t);
                            }
                        }
                    }
                }
            } catch (InterruptedException ie) {
                LOG.error("Zabbix on-demand sender is interrupted!", ie);
            } catch (Throwable t) {
                LOG.error("Zabbix on-demand sender is crashed!", t);
            }
            LOG.info("Zabbix on-demand sender thread stopped.");
        }
    };

    @PostConstruct
    public void init() {
        if (zabbixConfigurer.isEnabled()) {
            zabbixConfigurer.configure();
            senderThread.start();
        } else {
            LOG.warn("Statistics Exposer Zabbix is DISABLED!");
        }
    }

    @PreDestroy
    public void destroy() {
        senderQueue.add(STOP);
        try {
            senderThread.join();
        } catch (InterruptedException ie) {
            LOG.error("Failed to stop on-demand sender thread!", ie);
        }
    }

    public void sendToZabbix(final ExternalItem item) {
        try {
            if (zabbixConfigurer.configureItem(item)) {
                senderQueue.add(convert(item));
            }
        } catch (Throwable t) {
            LOG.error("Failed to configure item {}!", item.getKey(), t);
        }
    }

    private DataObject convert(final ExternalItem item) {
        return DataObject.builder()
                         .host(zabbixConfigurer.getApplicationHost())
                         .key(item.getKey())
                         .value(item.getValue())
                         .build();
    }

}
