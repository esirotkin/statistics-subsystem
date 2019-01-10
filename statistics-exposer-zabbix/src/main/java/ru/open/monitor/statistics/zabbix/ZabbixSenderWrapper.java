package ru.open.monitor.statistics.zabbix;

import static ru.open.monitor.statistics.zabbix.config.ZabbixConfigurer.DEFAULT_SENDER_PORT;
import static ru.open.monitor.statistics.zabbix.util.IPv4.REGEXP_IPv4_HOST;
import static ru.open.monitor.statistics.zabbix.util.IPv4.REGEXP_IPv4_PORT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.SenderResult;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;

import ru.open.monitor.statistics.zabbix.util.UpdateableConcurrentNavigableSet;
import ru.open.monitor.statistics.zabbix.util.UpdateableConcurrentNavigableSet.Updateable;

public class ZabbixSenderWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixSenderWrapper.class);

    @Value("${statistics.monitor.zabbix.attemptCount:3}")
    private long attemptCount;
    @Value("${statistics.monitor.zabbix.attemptInterval:500}")
    private long attemptInterval;

    @Value("#{'${statistics.monitor.zabbix.proxy.list:}'.split(',')}")
    private List<String> zabbixProxyList;

    @Value("${statistics.monitor.zabbix.server.host}")
    private String zabbixServerHost;
    @Value("${statistics.monitor.zabbix.sender.port:" + DEFAULT_SENDER_PORT + "}")
    private Integer zabbixServerPort;

    private enum ZabbixSenderStatus {
        OK,
        UNDEFINED,
        FAILED;
    };

    private class ZabbixSenderExemplar implements Updateable<ZabbixSenderStatus>, Comparable<ZabbixSenderExemplar> {
        private final int priorityIndex;
        private final ZabbixSender zabbixSender;
        private volatile ZabbixSenderStatus zabbixStatus = ZabbixSenderStatus.UNDEFINED;
        private volatile int success, failed;

        private ZabbixSenderExemplar(final ZabbixSender zabbixSender, final int priorityIndex) {
            this.zabbixSender = zabbixSender;
            this.priorityIndex = priorityIndex;
        }

        @Override
        public void update(final ZabbixSenderStatus zabbixStatus) {
            if (this.zabbixStatus != zabbixStatus) {
                LOG.warn("The status of ZabbixSender on {}:{} changed: {} -> {}.", zabbixSender.getHost(), zabbixSender.getPort(), this.zabbixStatus, zabbixStatus);
                this.zabbixStatus = zabbixStatus;
            }
            switch (zabbixStatus) {
                case OK: success++; break;
                case FAILED: failed++; break;
            }
        }

        @Override
        public int compareTo(ZabbixSenderExemplar other) {
            int result = Integer.compare(priorityIndex, other.priorityIndex) * 100;
            result += Integer.compare(zabbixStatus.ordinal(), other.zabbixStatus.ordinal()) * 100;
            result += Integer.compare(success, other.success) * 100;
            result += Integer.compare(failed, other.failed) * 10;
            result += Integer.compare(zabbixSender.getHost().hashCode(), other.zabbixSender.getHost().hashCode());
            result += Integer.compare(zabbixSender.getPort(), other.zabbixSender.getPort());
            return result;
        }

        private boolean sendToZabbix(List<DataObject> zabbixData) throws IOException {
            boolean success = false;

            for (int attempt = 1; !success && attempt <= attemptCount; attempt++) {
                if (attempt > 1) {
                    try { Thread.sleep(attemptInterval); } catch (InterruptedException ie) { LOG.error("Ooops...", ie); }
                }

                success = send(zabbixData);

                if (attempt > 1) {
                    LOG.warn("Attempt #{} to send data to Zabbix -> {}", attempt, (success ? "OK" : "FAIL"));
                }
                if (attempt == attemptCount) {
                    LOG.warn("Failed to send: {}", zabbixData);
                }
            }

            return success;
        }

        private boolean send(List<DataObject> zabbixData) throws IOException {
            boolean success, failed;

            int indexFrom = 0, indexTo = zabbixData.size() - 1;
            do {
                LOG.trace("Sending: {}", zabbixData);
                SenderResult senderResult = send(zabbixData, indexFrom, indexTo);
                LOG.trace("Result: {}", senderResult);

                LOG.debug("Sent {} of {} items in {} seconds. Failed {} items",
                          senderResult.getProcessed(), senderResult.getTotal(), senderResult.getSpentSeconds(), senderResult.getFailed());

                success = senderResult.success();
                indexFrom += senderResult.getProcessed();
                failed = !success && senderResult.getProcessed() == 0;

                if (failed) {
                    LOG.warn("Failed to send statistics to Zabbix! Processed: {} of {} items, Failed: {} items, Spent: {} seconds.",
                             senderResult.getProcessed(), senderResult.getTotal(), senderResult.getFailed(), senderResult.getSpentSeconds());
                    break;
                }
            } while (!success);

            return success;
        }

        private SenderResult send(List<DataObject> zabbixData, int indexFrom, int indexTo) throws IOException {
            List<DataObject> zabbixDataToSend;

            if (indexFrom != 0 || indexTo != zabbixData.size() - 1) {
                zabbixDataToSend = new ArrayList<>();
                for (int i = indexFrom; i <= indexTo; i++) {
                    zabbixDataToSend.add(zabbixData.get(i));
                }
            } else {
                zabbixDataToSend = zabbixData;
            }

            LOG.debug("Sending {} data objects (from {} to {}) to Zabbix ...", zabbixDataToSend.size(), indexFrom, indexTo);
            return zabbixSender.send(zabbixDataToSend);
        }
    };

    private final UpdateableConcurrentNavigableSet<ZabbixSenderExemplar, ZabbixSenderStatus> zabbixSenders = new UpdateableConcurrentNavigableSet<>();

    @PostConstruct
    public void init() {
        for (int proxyIndex = 0; proxyIndex < zabbixProxyList.size(); proxyIndex++) {
            final String zabbixProxy = zabbixProxyList.get(proxyIndex);
            if (zabbixProxy.trim().matches("^" + REGEXP_IPv4_HOST + ":" + "(?:" + REGEXP_IPv4_PORT + ")" + "$")) {
                final String zabbixProxyHost = zabbixProxy.trim().replaceAll("^" + "(" + REGEXP_IPv4_HOST + ")" + ":" + "(?:" + REGEXP_IPv4_PORT + ")" + "$", "$1");
                final String zabbixProxyPort = zabbixProxy.trim().replaceAll("^" + "(?:" + REGEXP_IPv4_HOST + ")" + ":" + "(" + REGEXP_IPv4_PORT + ")" + "$", "$1");
                zabbixSenders.add(new ZabbixSenderExemplar(new ZabbixSender(zabbixProxyHost, Integer.parseInt(zabbixProxyPort)), proxyIndex));
            } else if (zabbixProxy.trim().matches("^" + REGEXP_IPv4_HOST + "$")) {
                final String zabbixProxyHost = zabbixProxy.trim();
                zabbixSenders.add(new ZabbixSenderExemplar(new ZabbixSender(zabbixProxyHost, DEFAULT_SENDER_PORT), proxyIndex));
            }
        }
        if (zabbixSenders.isEmpty()) {
            zabbixSenders.add(new ZabbixSenderExemplar(new ZabbixSender(zabbixServerHost, zabbixServerPort), 0));
        }
    }

    public boolean sendToZabbix(List<DataObject> zabbixData) throws Exception {
        Exception exception = null;

        for (int proxyIndex = 0; proxyIndex < zabbixSenders.size(); proxyIndex++) {
            ZabbixSenderExemplar senderExemplar = zabbixSenders.first();

            boolean status;
            try {
                status = senderExemplar.sendToZabbix(zabbixData);
            } catch (Exception e) {
                exception = e;
                LOG.warn("Failed to send data to Zabbix!", e);
                status = false;
            } catch (Throwable t) {
                LOG.warn("Failed to send data to Zabbix!", t);
                status = false;
            }

            switch (senderExemplar.zabbixStatus) {
                case OK: if (!status) zabbixSenders.update(senderExemplar, ZabbixSenderStatus.FAILED); break;
                case UNDEFINED: zabbixSenders.update(senderExemplar, status ? ZabbixSenderStatus.OK : ZabbixSenderStatus.FAILED); break;
                case FAILED: if (status) zabbixSenders.update(senderExemplar, ZabbixSenderStatus.OK); break;
            }

            if (!status) {
                continue;
            }

            return status;
        }

        if (exception != null) {
            throw exception;
        }

        return false;
    }

}
