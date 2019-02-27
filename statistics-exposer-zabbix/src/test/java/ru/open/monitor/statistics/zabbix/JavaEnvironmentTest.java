package ru.open.monitor.statistics.zabbix;

import java.util.Map;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

public class JavaEnvironmentTest {

    @Test
    @Ignore
    public void testJavaEnvironment() throws Exception {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            System.out.format("%s=%s%n", envName, env.get(envName));
        }
    }

    @Test
    @Ignore
    public void testJavaProperties() throws Exception {
        Properties properties = System.getProperties();
        for (Object propertyKey : properties.keySet()) {
            System.out.format("%s=%s%n", propertyKey, properties.get(propertyKey));
        }
    }

}
