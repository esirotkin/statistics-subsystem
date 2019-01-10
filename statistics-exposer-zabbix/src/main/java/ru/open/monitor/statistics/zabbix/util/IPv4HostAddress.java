package ru.open.monitor.statistics.zabbix.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

public class IPv4HostAddress {
    private static final Logger LOG = LoggerFactory.getLogger(IPv4HostAddress.class);

    private static final IPv4 PRIVATE_SUBNET_10 = new IPv4("10.0.0.0/8");
    private static final IPv4 PRIVATE_SUBNET_172_16 = new IPv4("172.16.0.0/12");
    private static final IPv4 PRIVATE_SUBNET_192_168 = new IPv4("192.168.0.0/16");

    @Value("${java.rmi.server.hostname:}")
    private String bindAddress;
    @Value("${statistics.monitor.zabbix.agent.hostname:}")
    private String agentHostname;
    private final Inet4Address hostAddress;

    public IPv4HostAddress(final String subnet) throws SocketException {
        this.hostAddress = getAddressFromSubnet(new IPv4(subnet));
    }

    public String getHostIP() {
        LOG.info("Bind IP Address: {}", bindAddress);
        LOG.info("Host IP Address: {}", hostAddress.getHostAddress());
        return !StringUtils.isEmpty(bindAddress) ? bindAddress : hostAddress.getHostAddress();
    }

    public String getHostName() {
        LOG.info("Configured Zabbix Agent Hostname: {}", agentHostname);
        LOG.info("Current Hostname: {}", hostAddress.getHostName());
        return !StringUtils.isEmpty(agentHostname) ? agentHostname : hostAddress.getHostName();
    }

    public static Inet4Address getAddressFromSubnet(final IPv4 subnet) throws SocketException {
        for (InetAddress inetAddress : getInetAddresses()) {
            if (subnet.contains(inetAddress.getHostAddress())) {
                return (Inet4Address) inetAddress;
            }
        }
        return null;
    }

    public static List<Inet4Address> getPublicAddresses() throws SocketException {
        final List<Inet4Address> publicAddresses = new ArrayList<>();
        for (InetAddress inetAddress : getInetAddresses()) {
            if (!isPrivateAddress(inetAddress.getHostAddress())) {
                publicAddresses.add((Inet4Address) inetAddress);
            }
        }
        return publicAddresses;
    }

    public static List<Inet4Address> getPrivateAddresses() throws SocketException {
        final List<Inet4Address> privateAddresses = new ArrayList<>();
        for (InetAddress inetAddress : getInetAddresses()) {
            if (isPrivateAddress(inetAddress.getHostAddress())) {
                privateAddresses.add((Inet4Address) inetAddress);
            }
        }
        return privateAddresses;
    }

    private static List<InetAddress> getInetAddresses() throws SocketException {
        final List<InetAddress> hostAddresses = new ArrayList<>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (!networkInterface.isLoopback() && !networkInterface.isPointToPoint() && networkInterface.isUp()) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        hostAddresses.add(inetAddress);
                    }
                }
            }
        }
        return hostAddresses;
    }

    private static boolean isPrivateAddress(String ipAddress) {
        return PRIVATE_SUBNET_10.contains(ipAddress) || PRIVATE_SUBNET_172_16.contains(ipAddress) || PRIVATE_SUBNET_192_168.contains(ipAddress);
    }

}
