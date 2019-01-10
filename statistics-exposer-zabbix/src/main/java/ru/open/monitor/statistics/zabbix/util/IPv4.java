package ru.open.monitor.statistics.zabbix.util;

import java.util.ArrayList;
import java.util.List;

/**
 * IP/Subnet Calculator Lib.
 * @author <a href="http://www.codeproject.com/Members/saddam-abu-ghaida">Saddam Abu Ghaida</a>
 * @see <a href="http://www.codeproject.com/Tips/850531/IP-Subnet-Calculator-Lib">Source Publication</a>
 */
public class IPv4 {

    public static final String REGEXP_IPv4_HOST = "(?:25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}";
    public static final String REGEXP_IPv4_PORT = "6553[0-5]|655[0-2]\\d|65[0-4]\\d\\d|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}";

    private int baseIPnumeric;
    private int netmaskNumeric;

    /**
     * Specify IP address and netmask like: <code>new IPv4("10.1.0.25","255.255.255.16");</code>
     * @param ipAddress
     * @param netmask
     */
    public IPv4(String ipAddress, String netmask) throws NumberFormatException {
        /* IP */
        String[] ipAddressBytes = ipAddress.split("\\.");

        if (ipAddressBytes.length != 4) {
            throw new NumberFormatException("Invalid IP address: " + ipAddress);
        }

        int i = 24;
        baseIPnumeric = 0;
        for (int n = 0; n < ipAddressBytes.length; n++) {
            int value = Integer.parseInt(ipAddressBytes[n]);
            if (value != (value & 0xff)) {
                throw new NumberFormatException("Invalid IP address: " + ipAddress);
            }
            baseIPnumeric += value << i;
            i -= 8;
        }

        /* Netmask */
        ipAddressBytes = netmask.split("\\.");

        if (ipAddressBytes.length != 4) {
            throw new NumberFormatException("Invalid netmask address: " + netmask);
        }

        i = 24;
        netmaskNumeric = 0;
        if (Integer.parseInt(ipAddressBytes[0]) < 255) {
            throw new NumberFormatException("The first byte of netmask can not be less than 255");
        }
        for (int n = 0; n < ipAddressBytes.length; n++) {
            int value = Integer.parseInt(ipAddressBytes[n]);
            if (value != (value & 0xff)) {
                throw new NumberFormatException("Invalid netmask address: " + netmask);
            }
            netmaskNumeric += value << i;
            i -= 8;
        }

        /*
         * See if there are zeroes inside netmask, like: 1111111101111.
         * This is illegal, throw exception if encountered.
         * Netmask should always have only ones, then only zeroes, like: 11111111110000.
         */
        boolean encounteredOne = false;
        int ourMaskBitPattern = 1;
        for (i = 0; i < 32; i++) {
            if ((netmaskNumeric & ourMaskBitPattern) != 0) {
                encounteredOne = true; // the bit is 1
            } else { // the bit is 0
                if (encounteredOne == true) {
                    throw new NumberFormatException("Invalid netmask: " + netmask + " (bit " + (i + 1) + ")");
                }
            }
            ourMaskBitPattern = ourMaskBitPattern << 1;
        }
    }

    /**
     * Specify IP in CIDR format like: <code>new IPv4("10.1.0.25/16");</code>
     * @param ipAddressCIDR
     */
    public IPv4(String ipAddressCIDR) throws NumberFormatException {
        String[] ipAddressBytes = ipAddressCIDR.split("\\/");
        if (ipAddressBytes.length != 2) {
            throw new NumberFormatException("Invalid CIDR format '" + ipAddressCIDR + "', should be: XXX.XXX.XXX.XXX/XX");
        }

        String symbolicIP = ipAddressBytes[0];
        String symbolicCIDR = ipAddressBytes[1];

        Integer numericCIDR = new Integer(symbolicCIDR);
        if (numericCIDR > 32) {
            throw new NumberFormatException("CIDR can not be greater than 32");
        }

        /* IP */
        ipAddressBytes = symbolicIP.split("\\.");
        if (ipAddressBytes.length != 4) {
            throw new NumberFormatException("Invalid IP address: " + symbolicIP);
        }

        int i = 24;
        baseIPnumeric = 0;
        for (int n = 0; n < ipAddressBytes.length; n++) {
            int value = Integer.parseInt(ipAddressBytes[n]);
            if (value != (value & 0xff)) {
                throw new NumberFormatException("Invalid IP address: " + symbolicIP);
            }
            baseIPnumeric += value << i;
            i -= 8;
        }

        /* netmask from CIDR */
        if (numericCIDR < 8) {
            throw new NumberFormatException("Netmask CIDR can not be less than 8");
        }
        netmaskNumeric = 0xffffffff;
        netmaskNumeric = netmaskNumeric << (32 - numericCIDR);
    }

    /**
     * @return the IP in symbolic form, i.e. <code>XXX.XXX.XXX.XXX</code>.
     */
    public String getIP() {
        return convertNumericIpToSymbolic(baseIPnumeric);
    }

    private String convertNumericIpToSymbolic(Integer ipAddressNumeric) {
        StringBuffer ipAddress = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            // process 3 bytes, from high order byte down.
            ipAddress.append(Integer.toString((ipAddressNumeric >>> shift) & 0xff));
            ipAddress.append('.');
        }
        ipAddress.append(Integer.toString(ipAddressNumeric & 0xff));
        return ipAddress.toString();
    }

    /**
     * @return the net mask in symbolic form, i.e. <code>XXX.XXX.XXX.XXX</code>.
     */
    public String getNetmask() {
        StringBuffer netmask = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            // process 3 bytes, from high order byte down.
            netmask.append(Integer.toString((netmaskNumeric >>> shift) & 0xff));
            netmask.append('.');
        }
        netmask.append(Integer.toString(netmaskNumeric & 0xff));
        return netmask.toString();
    }

    /**
     * @return the IP address and netmask in CIDR form, i.e. <code>XXX.XXX.XXX.XXX/XX</code>.
     */
    public String getCIDR() {
        int netmaskCIDR;
        for (netmaskCIDR = 0; netmaskCIDR < 32; netmaskCIDR++) {
            if ((netmaskNumeric << netmaskCIDR) == 0) {
                break;
            }
        }
        return convertNumericIpToSymbolic(baseIPnumeric & netmaskNumeric) + "/" + netmaskCIDR;
    }

    /**
     * @return an array of all the IP addresses available for the IP and netmask/CIDR
     * given at initialization.
     */
    public List<String> getAvailableIPs(Integer numberofIPs) {
        ArrayList<String> result = new ArrayList<String>();

        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
            if ((netmaskNumeric << numberOfBits) == 0) {
                break;
            }
        }

        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {
            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;
        }

        Integer baseIP = baseIPnumeric & netmaskNumeric;
        for (int i = 1; i < (numberOfIPs) && i < numberofIPs; i++) {
            Integer ourIP = baseIP + i;
            String ip = convertNumericIpToSymbolic(ourIP);
            result.add(ip);
        }

        return result;
    }

    /**
     * @return a range of hosts available for the IP and netmask/CIDR
     * given at initialization.
     */
    public String getHostAddressRange() {
        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
            if ((netmaskNumeric << numberOfBits) == 0) {
                break;
            }
        }

        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {
            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;
        }
        Integer baseIP = baseIPnumeric & netmaskNumeric;

        String firstIP = convertNumericIpToSymbolic(baseIP + 1);
        String lastIP = convertNumericIpToSymbolic(baseIP + numberOfIPs - 1);

        return firstIP + " - " + lastIP;
    }

    /**
     * @return a number of hosts available in given range.
     */
    public Long getNumberOfHosts() {
        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
            if ((netmaskNumeric << numberOfBits) == 0) {
                break;
            }
        }

        Double numberOfHosts = Math.pow(2, (32 - numberOfBits));
        if (numberOfHosts == -1) {
            numberOfHosts = 1D;
        }

        return numberOfHosts.longValue();
    }

    /**
     * The XOR of the netmask.
     * @return wildcard mask in text form, i.e. <code>0.0.15.255</code>.
     */
    public String getWildcardMask() {
        Integer wildcardMask = netmaskNumeric ^ 0xffffffff;

        StringBuffer result = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            // process 3 bytes, from high order byte down.
            result.append(Integer.toString((wildcardMask >>> shift) & 0xff));
            result.append('.');
        }
        result.append(Integer.toString(wildcardMask & 0xff));
        return result.toString();
    }

    /**
     * @return the broadcast address for the IP and netmask/CIDR
     * given at initialization.
     */
    public String getBroadcastAddress() {
        if (netmaskNumeric == 0xffffffff) {
            return "0.0.0.0";
        }

        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
            if ((netmaskNumeric << numberOfBits) == 0) {
                break;
            }
        }

        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {
            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;
        }

        Integer baseIP = baseIPnumeric & netmaskNumeric;
        Integer ourIP = baseIP + numberOfIPs;

        String ip = convertNumericIpToSymbolic(ourIP);
        return ip;
    }

    /**
     * @return the netmask in the binary format.
     */
    public String getNetmaskInBinary() {
        return getBinary(netmaskNumeric);
    }

    private String getBinary(Integer number) {
        String result = "";

        Integer ourMaskBitPattern = 1;
        for (int i = 1; i <= 32; i++) {
            if ((number & ourMaskBitPattern) != 0) {
                result = "1" + result; // the bit is 1
            } else { // the bit is 0
                result = "0" + result;
            }
            if ((i % 8) == 0 && i != 0 && i != 32) {
                result = "." + result;
            }
            ourMaskBitPattern = ourMaskBitPattern << 1;
        }

        return result;
    }

    /**
     * Checks if the given IP address contains in subnet.
     */
    public boolean contains(String ipAddress) {
        Integer checkingIP = 0;
        String[] ipAddressBytes = ipAddress.split("\\.");

        if (ipAddressBytes.length != 4) {
            throw new NumberFormatException("Invalid IP address: " + ipAddress);
        }

        int i = 24;
        for (int n = 0; n < ipAddressBytes.length; n++) {
            int value = Integer.parseInt(ipAddressBytes[n]);
            if (value != (value & 0xff)) {
                throw new NumberFormatException("Invalid IP address: " + ipAddress);
            }
            checkingIP += value << i;
            i -= 8;
        }

        if ((baseIPnumeric & netmaskNumeric) == (checkingIP & netmaskNumeric)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the given IP address contains in subnet.
     */
    public boolean contains(IPv4 child) {
        Integer subnetID = child.baseIPnumeric;
        Integer subnetMask = child.netmaskNumeric;

        if ((subnetID & this.netmaskNumeric) == (this.baseIPnumeric & this.netmaskNumeric)) {
            if ((this.netmaskNumeric < subnetMask) == true && this.baseIPnumeric <= subnetID) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the IP address given at initialization.
     */
    public boolean validateIPAddress() {
        String ipAddress = getIP();
        if (ipAddress.startsWith("0")) {
            return false;
        }

        if (ipAddress.isEmpty()) {
            return false;
        }

        if (ipAddress.matches("^" + REGEXP_IPv4_HOST + "$")) {
            return true;
        }

        return false;
    }

}
